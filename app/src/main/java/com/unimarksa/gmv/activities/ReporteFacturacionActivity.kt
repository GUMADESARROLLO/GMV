package com.unimarksa.gmv.activities

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.unimarksa.gmv.R
import com.unimarksa.gmv.adapters.ReporteFacturaAdapter
import com.unimarksa.gmv.models.ItemFacturaPendiente
import com.unimarksa.gmv.models.ItemReporteFactura
import com.unimarksa.gmv.tools.ApiService
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ReporteFacturacionActivity : AppCompatActivity() {

    private var facturas = mutableListOf<ItemReporteFactura>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reporte_facturacion)

        if (Build.VERSION.SDK_INT >= 35) {
            val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
            val h = if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
            val scrim = View(this)
            scrim.setBackgroundColor(ContextCompat.getColor(this, R.color.colorUmkPrimaryDark))
            val content = findViewById<ViewGroup>(android.R.id.content)
            scrim.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h)
            content.addView(scrim)
        } else {
            window.statusBarColor = ContextCompat.getColor(this, R.color.colorUmkPrimaryDark)
        }

        ViewCompat.getWindowInsetsController(window.decorView)?.isAppearanceLightStatusBars = false

        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        findViewById<EditText>(R.id.id_desde).setOnClickListener { showDatePicker { selectDesde(it) } }
        findViewById<EditText>(R.id.id_hasta).setOnClickListener { showDatePicker { selectHasta(it) } }
        findViewById<View>(R.id.id_img_desde).setOnClickListener { showDatePicker { selectDesde(it) } }
        findViewById<View>(R.id.id_img_hasta).setOnClickListener { showDatePicker { selectHasta(it) } }

        val hoy = dateFormat.format(Calendar.getInstance().time)
        findViewById<EditText>(R.id.id_desde).setText(hoy)
        findViewById<EditText>(R.id.id_hasta).setText(hoy)
        buscar(hoy, hoy)
    }

    private fun selectDesde(date: String) {
        findViewById<EditText>(R.id.id_desde).setText(date)
        val hasta = findViewById<EditText>(R.id.id_hasta).text?.toString()?.trim()
        if (!hasta.isNullOrEmpty()) buscar(date, hasta)
    }

    private fun selectHasta(date: String) {
        findViewById<EditText>(R.id.id_hasta).setText(date)
        val desde = findViewById<EditText>(R.id.id_desde).text?.toString()?.trim()
        if (!desde.isNullOrEmpty()) buscar(desde, date)
    }

    private fun buscar(desde: String, hasta: String) {
        findViewById<View>(R.id.lyt_loading).visibility = View.VISIBLE
        findViewById<View>(R.id.lyt_empty_result).visibility = View.GONE
        findViewById<View>(R.id.card_recycler).visibility = View.GONE
        findViewById<View>(R.id.recycler_view).visibility = View.GONE

        ApiService.fetchReporteFacturacion(
            desde = desde,
            hasta = hasta,
            onResult = { items ->
                facturas.clear()
                facturas.addAll(items)
                mostrarResultados()
            },
            onError = {
                facturas.clear()
                mostrarResultados()
            }
        )
    }

    private fun mostrarResultados() {
        findViewById<View>(R.id.lyt_loading).visibility = View.GONE
        if (facturas.isEmpty()) {
            findViewById<View>(R.id.lyt_empty_result).visibility = View.VISIBLE
            findViewById<View>(R.id.card_recycler).visibility = View.GONE
            findViewById<TextView>(R.id.id_factura_total).text = "C$ 0.00"
            findViewById<TextView>(R.id.id_factura_count).text = "0"
            findViewById<TextView>(R.id.id_results_count_label).text = "0 resultados"
            return
        }

        findViewById<View>(R.id.lyt_empty_result).visibility = View.GONE
        findViewById<View>(R.id.card_recycler).visibility = View.VISIBLE
        findViewById<View>(R.id.recycler_view).visibility = View.VISIBLE

        val total = facturas.sumOf {
            it.total.replace(",", "").trim().toDoubleOrNull() ?: 0.0
        }
        findViewById<TextView>(R.id.id_factura_total).text = "C$ ${"%,.2f".format(total)}"
        findViewById<TextView>(R.id.id_factura_count).text = "${facturas.size}"
        findViewById<TextView>(R.id.id_results_count_label).text = "${facturas.size} resultados"

        val rv = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recycler_view)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = ReporteFacturaAdapter(facturas) { factura ->
            val intent = Intent(this, FacturaDetailActivity::class.java)
            intent.putExtra("factura", ItemFacturaPendiente(
                numero = factura.numero,
                saldo = factura.saldo.ifEmpty { factura.total },
                vencimiento = factura.fecha,
                saldoActual = factura.saldo.ifEmpty { factura.total }
            ))
            intent.putExtra("readOnly", true)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                val m = (month + 1).toString().padStart(2, '0')
                val d = day.toString().padStart(2, '0')
                onDateSelected("$year-$m-$d")
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}
