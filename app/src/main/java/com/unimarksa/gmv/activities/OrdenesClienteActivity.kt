package com.unimarksa.gmv.activities

import android.app.DatePickerDialog
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
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
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ordenes_cliente)

        if (Build.VERSION.SDK_INT < 35) {
            window.statusBarColor = ContextCompat.getColor(this, R.color.colorUmkPrimaryDark)
        }
        ViewCompat.getWindowInsetsController(window.decorView)?.isAppearanceLightStatusBars = false

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        setupDatePickers()

        val codCliente = intent.getStringExtra("codCliente")
        if (codCliente != null) {
            cargarOrdenesCliente(codCliente)
        } else {
            cargarTodas()
        }
    }

    private fun setupDatePickers() {
        val hoy = dateFmt.format(Calendar.getInstance().time)

        findViewById<EditText>(R.id.id_desde).setText(hoy)
        findViewById<EditText>(R.id.id_hasta).setText(hoy)

        findViewById<View>(R.id.id_desde).setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                val fecha = "$y-${(m+1).toString().padStart(2,'0')}-${d.toString().padStart(2,'0')}"
                findViewById<EditText>(R.id.id_desde).setText(fecha)
                filtrar()
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }

        findViewById<View>(R.id.id_hasta).setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                val fecha = "$y-${(m+1).toString().padStart(2,'0')}-${d.toString().padStart(2,'0')}"
                findViewById<EditText>(R.id.id_hasta).setText(fecha)
                filtrar()
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    private fun filtrar() {
        val desde = findViewById<EditText>(R.id.id_desde).text.toString()
        val hasta = findViewById<EditText>(R.id.id_hasta).text.toString()
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
        findViewById<View>(R.id.progress_bar).visibility = View.VISIBLE
        ApiService.fetchOrdenes(
            onResult = { items ->
                todasLasOrdenes.clear()
                todasLasOrdenes.addAll(items)
                findViewById<View>(R.id.progress_bar).visibility = View.GONE
                filtrar()
            },
            onError = {
                todasLasOrdenes.clear()
                findViewById<View>(R.id.progress_bar).visibility = View.GONE
                filtrar()
            }
        )
    }

    private fun cargarOrdenesCliente(codCliente: String) {
        findViewById<View>(R.id.progress_bar).visibility = View.VISIBLE
        ApiService.fetchOrdenesCliente(
            codCliente = codCliente,
            onResult = { items ->
                todasLasOrdenes.clear()
                todasLasOrdenes.addAll(items)
                findViewById<View>(R.id.progress_bar).visibility = View.GONE
                filtrar()
            },
            onError = {
                todasLasOrdenes.clear()
                findViewById<View>(R.id.progress_bar).visibility = View.GONE
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
            findViewById<View>(R.id.lyt_results).visibility = View.GONE
            return
        }

        findViewById<View>(R.id.lyt_empty).visibility = View.GONE
        findViewById<View>(R.id.lyt_results).visibility = View.VISIBLE
        findViewById<TextView>(R.id.tv_result_count).text = "$total resultado${if (total != 1) "s" else ""}"

        val rv = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_ordenes)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = OrdenesAdapter(items)
    }
}
