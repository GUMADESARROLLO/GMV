package com.unimarksa.gmv.activities

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.unimarksa.gmv.R
import com.unimarksa.gmv.tools.BarrasSistema
import com.unimarksa.gmv.adapters.OrdenesAdapter
import com.unimarksa.gmv.data.local.AppDatabase
import com.unimarksa.gmv.data.local.OrderEntity
import com.unimarksa.gmv.models.ItemPedidoCliente
import com.unimarksa.gmv.tools.ApiService
import com.unimarksa.gmv.tools.FechasRango
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
    // Locale.US: dígitos ASCII para que FechasRango.diaDe pueda leer la fecha al filtrar/agrupar
    private val displayFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US)
    // Se cancela en onDestroy (antes el scope nunca se cancelaba)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var db: AppDatabase
    private var codCliente: String? = null

    /** Id de la última carga: descarta respuestas viejas (p. ej. swipe mientras carga). */
    private var cargaActual = 0

    /** Rango rápido activo (null = fechas elegidas a mano). */
    private var rangoActivo: Int? = null

    /** true tras la primera carga; antes no se filtra (la lista aún está vacía). */
    private var cargado = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ordenes_cliente)

        db = AppDatabase.getInstance(this)
        codCliente = intent.getStringExtra("codCliente")

        BarrasSistema.aplicar(this, R.color.umk_mist)

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        setupDatePickers()
        setupDeleteAll()

        FechasRango.RANGOS.forEach { (id, _) -> findViewById<View>(id).setOnClickListener { aplicarRango(id) } }
        aplicarRango(R.id.rango7)

        findViewById<SwipeRefreshLayout>(R.id.swipeRefresh).setOnRefreshListener { cargar(true) }

        cargar()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun cargar(isRefresh: Boolean = false) {
        val cod = codCliente
        if (cod != null) cargarOrdenesCliente(cod, isRefresh) else cargarTodas(isRefresh)
    }

    private fun setupDatePickers() {
        val tvDesde = findViewById<TextView>(R.id.id_desde)
        val tvHasta = findViewById<TextView>(R.id.id_hasta)

        // Elegir una fecha a mano deselecciona el rango rápido
        findViewById<View>(R.id.cardDateFrom).setOnClickListener {
            showDatePicker(tvDesde.text.toString()) { fecha ->
                tvDesde.text = fecha
                marcarRango(null)
                filtrar()
            }
        }

        findViewById<View>(R.id.cardDateTo).setOnClickListener {
            showDatePicker(tvHasta.text.toString()) { fecha ->
                tvHasta.text = fecha
                marcarRango(null)
                filtrar()
            }
        }
    }

    /** Fija Desde/Hasta según el rango rápido y filtra (el filtro es local). */
    private fun aplicarRango(id: Int) {
        val (desde, hasta) = FechasRango.fechasDe(id)
        findViewById<TextView>(R.id.id_desde).text = desde
        findViewById<TextView>(R.id.id_hasta).text = hasta
        marcarRango(id)
        filtrar()
    }

    private fun marcarRango(id: Int?) {
        rangoActivo = id
        FechasRango.marcarRango(this, id)
        findViewById<TextView>(R.id.tvResumenTitulo).text = FechasRango.tituloResumen(
            id,
            findViewById<TextView>(R.id.id_desde).text.toString(),
            findViewById<TextView>(R.id.id_hasta).text.toString()
        )
    }

    /** Abre el calendario en la fecha ya seleccionada (antes siempre abría en hoy). */
    private fun showDatePicker(actual: String, onDateSelected: (String) -> Unit) {
        val c = Calendar.getInstance()
        runCatching { dateFmt.parse(actual) }.getOrNull()?.let { c.time = it }
        DatePickerDialog(this, { _, y, m, d ->
            onDateSelected("$y-${(m + 1).toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}")
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    /**
     * Filtra por rango comparando "yyyy-MM-dd". Las órdenes del API llegan como
     * "yyyy-MM-dd[ HH:mm:ss]" y las locales como "dd/MM/yyyy HH:mm"; FechasRango.isoDe
     * normaliza ambas.
     */
    private fun filtrar() {
        var desde = findViewById<TextView>(R.id.id_desde).text.toString()
        var hasta = findViewById<TextView>(R.id.id_hasta).text.toString()
        if (desde.isBlank() || hasta.isBlank()) return
        if (desde > hasta) { val t = desde; desde = hasta; hasta = t }

        if (rangoActivo == null) {
            findViewById<TextView>(R.id.tvResumenTitulo).text = FechasRango.rangoLegible(desde, hasta)
        }
        if (!cargado) return

        val filtradas = todasLasOrdenes.filter { orden ->
            // Fecha desconocida: se muestra (mismo criterio que antes)
            val dia = FechasRango.isoDe(orden.fecha) ?: return@filter true
            dia >= desde && dia <= hasta
        }
        mostrarResultados(filtradas)
    }

    private fun cargarTodas(isRefresh: Boolean) {
        val id = ++cargaActual
        if (!isRefresh) findViewById<View>(R.id.lyt_loading).visibility = View.VISIBLE
        // Órdenes locales (Room) + órdenes del API
        scope.launch {
            val localOrdenes = db.orderDao().getAllOrders().map { orderEntityToItem(it) }
            ApiService.fetchOrdenes(
                onResult = { items -> aplicarCarga(id, localOrdenes, items) },
                onError = { aplicarCarga(id, localOrdenes, emptyList()) }
            )
        }
    }

    private fun cargarOrdenesCliente(codCliente: String, isRefresh: Boolean) {
        val id = ++cargaActual
        if (!isRefresh) findViewById<View>(R.id.lyt_loading).visibility = View.VISIBLE
        // Órdenes locales (Room) filtradas por cliente + órdenes del API
        scope.launch {
            val localOrdenes = db.orderDao().getAllOrders()
                .filter { it.clienteCodigo == codCliente }
                .map { orderEntityToItem(it) }

            ApiService.fetchOrdenesCliente(
                codCliente = codCliente,
                onResult = { items -> aplicarCarga(id, localOrdenes, items) },
                onError = { aplicarCarga(id, localOrdenes, emptyList()) }
            )
        }
    }

    /** Callbacks de Retrofit llegan en el hilo principal. */
    private fun aplicarCarga(id: Int, locales: List<ItemPedidoCliente>, remotas: List<ItemPedidoCliente>) {
        if (id != cargaActual || isFinishing || isDestroyed) return
        cargado = true
        todasLasOrdenes.clear()
        todasLasOrdenes.addAll(locales)
        todasLasOrdenes.addAll(remotas)
        findViewById<View>(R.id.lyt_loading).visibility = View.GONE
        actualizarBotonEliminarTodo()
        filtrar()
    }

    private fun mostrarResultados(items: List<ItemPedidoCliente>) {
        findViewById<SwipeRefreshLayout>(R.id.swipeRefresh).isRefreshing = false
        val total = items.size
        val monto = items.sumOf { it.monto.replace(",", "").trim().toDoubleOrNull() ?: 0.0 }

        // Línea fija de total: conteo bajo el título del rango y Σ en verde a la derecha
        findViewById<TextView>(R.id.tv_total_ordenes).text =
            "$total orden${if (total == 1) "" else "es"}"
        findViewById<TextView>(R.id.tv_monto_total).text = "Σ C$ ${fmt.format(monto)}"

        val rv = findViewById<RecyclerView>(R.id.rv_ordenes)
        if (items.isEmpty()) {
            findViewById<View>(R.id.lyt_empty).visibility = View.VISIBLE
            rv.visibility = View.GONE
            rv.adapter = null
            return
        }

        findViewById<View>(R.id.lyt_empty).visibility = View.GONE
        rv.visibility = View.VISIBLE

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

    /** El botón de eliminar todo solo aparece si hay órdenes locales. */
    private fun actualizarBotonEliminarTodo() {
        val hayLocales = todasLasOrdenes.any { it.id != -1L }
        findViewById<View>(R.id.ivDeleteAll).visibility = if (hayLocales) View.VISIBLE else View.GONE
    }

    private fun setupDeleteAll() {
        findViewById<View>(R.id.ivDeleteAll).setOnClickListener {
            val idsLocales = todasLasOrdenes.filter { it.id != -1L }.map { it.id }
            val locales = idsLocales.size
            if (locales == 0) return@setOnClickListener
            MaterialAlertDialogBuilder(this)
                .setTitle("Eliminar todo")
                .setMessage(
                    if (locales == 1) "¿Eliminar 1 orden local?"
                    else "¿Eliminar $locales órdenes locales?"
                )
                .setPositiveButton("Eliminar") { _, _ ->
                    scope.launch {
                        // Con codCliente solo se borran las órdenes de ese cliente;
                        // antes deleteAllOrders() borraba las de todos los clientes.
                        if (codCliente == null) db.orderDao().deleteAllOrders()
                        else idsLocales.forEach { db.orderDao().deleteOrderById(it) }
                        withContext(Dispatchers.Main) {
                            todasLasOrdenes.removeAll { it.id != -1L }
                            actualizarBotonEliminarTodo()
                            filtrar()
                        }
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
                    withContext(Dispatchers.Main) {
                        todasLasOrdenes.removeAll { it.numero == orden.numero && it.id == orden.id }
                        actualizarBotonEliminarTodo()
                        filtrar()
                    }
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
            // Locale.US: con un locale de coma decimal "%.2f" daba "1234,50" y el
            // replace(",", "") posterior lo convertía en 123450
            monto = String.format(Locale.US, "%.2f", order.total),
            articulos = db.orderDao().getItemsByOrderId(order.id).size.toString(),
            cliente = order.clienteNombre
        )
    }
}
