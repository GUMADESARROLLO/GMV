package com.unimarksa.gmv.activities

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.unimarksa.gmv.R
import com.unimarksa.gmv.adapters.OrdenesAdapter
import com.unimarksa.gmv.data.local.AppDatabase
import com.unimarksa.gmv.data.local.OrderEntity
import com.unimarksa.gmv.models.ItemPedidoCliente
import com.unimarksa.gmv.tools.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class OrdenesClienteActivity : AppCompatActivity() {

    private var todasLasOrdenes = mutableListOf<ItemPedidoCliente>()
    private val fmt = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ordenes_cliente)

        db = AppDatabase.getInstance(this)

        if (Build.VERSION.SDK_INT >= 35) {
            val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
            val h = if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
            val scrim = View(this)
            scrim.setBackgroundColor(ContextCompat.getColor(this, R.color.brand_primary_dark))
            val content = findViewById<ViewGroup>(android.R.id.content)
            scrim.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h)
            content.addView(scrim)
        } else {
            window.statusBarColor = ContextCompat.getColor(this, R.color.brand_primary_dark)
        }

        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        setupDatePickers()
        setupDeleteAll()

        val codCliente = intent.getStringExtra("codCliente")
        if (codCliente != null) {
            cargarOrdenesCliente(codCliente)
        } else {
            cargarTodas()
        }
    }

    private fun setupDatePickers() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val lunes = dateFmt.format(cal.time)
        val hoy = dateFmt.format(Calendar.getInstance().time)

        findViewById<TextView>(R.id.id_desde).text = lunes
        findViewById<TextView>(R.id.id_hasta).text = hoy

        findViewById<View>(R.id.cardDateFrom).setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                val fecha = "$y-${(m+1).toString().padStart(2,'0')}-${d.toString().padStart(2,'0')}"
                findViewById<TextView>(R.id.id_desde).text = fecha
                filtrar()
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }

        findViewById<View>(R.id.cardDateTo).setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                val fecha = "$y-${(m+1).toString().padStart(2,'0')}-${d.toString().padStart(2,'0')}"
                findViewById<TextView>(R.id.id_hasta).text = fecha
                filtrar()
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    private fun filtrar() {
        val desde = findViewById<TextView>(R.id.id_desde).text.toString()
        val hasta = findViewById<TextView>(R.id.id_hasta).text.toString()
        if (desde.isBlank() || hasta.isBlank()) return

        val filtradas = todasLasOrdenes.filter { orden ->
            try {
                val f = dateFmt.parse(orden.fecha) ?: return@filter false
                val d = dateFmt.parse(desde) ?: return@filter false
                val h = dateFmt.parse(hasta) ?: return@filter false
                !(f.before(d) || f.after(h))
            } catch (_: Exception) { true }
        }
        mostrarResultados(filtradas)
    }

    private fun cargarTodas() {
        findViewById<View>(R.id.lyt_loading).visibility = View.VISIBLE
        // Load local orders from Room
        scope.launch {
            val localOrdenes = db.orderDao().getAllOrders().map { orderEntityToItem(it) }
            // Load API orders
            ApiService.fetchOrdenes(
                onResult = { items ->
                    todasLasOrdenes.clear()
                    todasLasOrdenes.addAll(localOrdenes)
                    todasLasOrdenes.addAll(items)
                    findViewById<View>(R.id.lyt_loading).visibility = View.GONE
                    filtrar()
                },
                onError = {
                    todasLasOrdenes.clear()
                    todasLasOrdenes.addAll(localOrdenes)
                    findViewById<View>(R.id.lyt_loading).visibility = View.GONE
                    filtrar()
                }
            )
        }
    }

    private fun cargarOrdenesCliente(codCliente: String) {
        findViewById<View>(R.id.lyt_loading).visibility = View.VISIBLE
        // Load local orders from Room filtered by client
        scope.launch {
            val allOrders = db.orderDao().getAllOrders()
            val localOrdenes = allOrders
                .filter { it.clienteCodigo == codCliente }
                .map { orderEntityToItem(it) }

            ApiService.fetchOrdenesCliente(
                codCliente = codCliente,
                onResult = { items ->
                    todasLasOrdenes.clear()
                    todasLasOrdenes.addAll(localOrdenes)
                    todasLasOrdenes.addAll(items)
                    findViewById<View>(R.id.lyt_loading).visibility = View.GONE
                    filtrar()
                },
                onError = {
                    todasLasOrdenes.clear()
                    todasLasOrdenes.addAll(localOrdenes)
                    findViewById<View>(R.id.lyt_loading).visibility = View.GONE
                    filtrar()
                }
            )
        }
    }

    private fun mostrarResultados(items: List<ItemPedidoCliente>) {
        val total = items.size
        val monto = items.sumOf { it.monto.replace(",", "").toDoubleOrNull() ?: 0.0 }

        findViewById<TextView>(R.id.tv_total_ordenes).text = total.toString()
        findViewById<TextView>(R.id.tv_monto_total).text = "C$ ${fmt.format(monto)}"

        if (items.isEmpty()) {
            findViewById<View>(R.id.lyt_empty).visibility = View.VISIBLE
            findViewById<View>(R.id.rv_ordenes).visibility = View.GONE
            return
        }

        findViewById<View>(R.id.lyt_empty).visibility = View.GONE
        findViewById<View>(R.id.rv_ordenes).visibility = View.VISIBLE

        val rv = findViewById<RecyclerView>(R.id.rv_ordenes)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = OrdenesAdapter(
            items = items,
            onItemClick = { orden ->
                if (orden.id != -1L) {
                    startActivity(Intent(this, OrderDetailActivity::class.java)
                        .putExtra("orderId", orden.id))
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                }
            },
            onDeleteClick = { orden -> eliminarOrden(orden) }
        )
    }

    private fun setupDeleteAll() {
        findViewById<View>(R.id.ivDeleteAll).setOnClickListener {
            val locales = todasLasOrdenes.count { it.id != -1L }
            if (locales == 0) return@setOnClickListener
            MaterialAlertDialogBuilder(this)
                .setTitle("Eliminar todo")
                .setMessage("¿Eliminar $locales órdenes locales?")
                .setPositiveButton("Eliminar") { _, _ ->
                    scope.launch {
                        db.orderDao().deleteAllOrders()
                        todasLasOrdenes.removeAll { it.id != -1L }
                        withContext(Dispatchers.Main) { filtrar() }
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun eliminarOrden(orden: ItemPedidoCliente) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Eliminar orden")
            .setMessage("¿Eliminar ${orden.numero}?")
            .setPositiveButton("Eliminar") { _, _ ->
                scope.launch {
                    db.orderDao().deleteOrderById(orden.id)
                    todasLasOrdenes.removeAll { it.numero == orden.numero && it.id == orden.id }
                    withContext(Dispatchers.Main) { filtrar() }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private suspend fun orderEntityToItem(order: OrderEntity): ItemPedidoCliente {
        val fechaStr = try {
            displayFmt.format(Date(order.fechaCreacion))
        } catch (_: Exception) { "" }
        val displayNumero = order.codigo.ifEmpty { "LOC-${order.id}" }
        val estadoDisplay = when {
            order.estado == "enviado" -> "Completado"
            order.estado == "pendiente" -> "Pendiente"
            else -> order.estado
        }
        return ItemPedidoCliente(
            id = order.id,
            numero = displayNumero,
            estado = estadoDisplay,
            fecha = fechaStr,
            monto = String.format("%.2f", order.total),
            articulos = db.orderDao().getItemsByOrderId(order.id).size.toString(),
            cliente = order.clienteNombre
        )
    }
}