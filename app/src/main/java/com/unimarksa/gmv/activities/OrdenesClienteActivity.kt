package com.unimarksa.gmv.activities

import android.app.DatePickerDialog
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.unimarksa.gmv.R
import com.unimarksa.gmv.adapters.OrdenesAdapter
import com.unimarksa.gmv.models.ItemPedidoCliente
import com.unimarksa.gmv.tools.ApiService
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class OrdenesClienteActivity : AppCompatActivity() {

    private var todasLasOrdenes = mutableListOf<ItemPedidoCliente>()
    private val fmt = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ordenes_cliente)

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
        ApiService.fetchOrdenes(
            onResult = { items ->
                todasLasOrdenes.clear()
                todasLasOrdenes.addAll(items)
                findViewById<View>(R.id.lyt_loading).visibility = View.GONE
                filtrar()
            },
            onError = {
                todasLasOrdenes.clear()
                findViewById<View>(R.id.lyt_loading).visibility = View.GONE
                filtrar()
            }
        )
    }

    private fun cargarOrdenesCliente(codCliente: String) {
        findViewById<View>(R.id.lyt_loading).visibility = View.VISIBLE
        ApiService.fetchOrdenesCliente(
            codCliente = codCliente,
            onResult = { items ->
                todasLasOrdenes.clear()
                todasLasOrdenes.addAll(items)
                findViewById<View>(R.id.lyt_loading).visibility = View.GONE
                filtrar()
            },
            onError = {
                todasLasOrdenes.clear()
                findViewById<View>(R.id.lyt_loading).visibility = View.GONE
                filtrar()
            }
        )
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
        rv.adapter = OrdenesAdapter(items)
    }
}