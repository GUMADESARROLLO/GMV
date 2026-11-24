package com.unimarksa.gmv.activities

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.unimarksa.gmv.R
import com.unimarksa.gmv.tools.BarrasSistema
import com.unimarksa.gmv.adapters.ReporteFacturaAdapter
import com.unimarksa.gmv.models.ItemFacturaPendiente
import com.unimarksa.gmv.models.ItemReporteFactura
import com.unimarksa.gmv.tools.ApiService
import com.unimarksa.gmv.tools.FechasRango
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ReporteFacturacionActivity : AppCompatActivity() {

    private var facturas = mutableListOf<ItemReporteFactura>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val fmt = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))

    /** Id de la última búsqueda: descarta respuestas viejas si se cambian las fechas rápido. */
    private var busquedaActual = 0

    /** Rango rápido activo (null = fechas elegidas a mano). */
    private var rangoActivo: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reporte_facturacion)

        BarrasSistema.aplicar(this, R.color.umk_mist)

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        findViewById<View>(R.id.cardDateFrom).setOnClickListener {
            showDatePicker(findViewById<TextView>(R.id.id_desde).text.toString()) { selectDesde(it) }
        }
        findViewById<View>(R.id.cardDateTo).setOnClickListener {
            showDatePicker(findViewById<TextView>(R.id.id_hasta).text.toString()) { selectHasta(it) }
        }

        FechasRango.RANGOS.forEach { (id, _) -> findViewById<View>(id).setOnClickListener { aplicarRango(id) } }
        aplicarRango(R.id.rango7)

        findViewById<SwipeRefreshLayout>(R.id.swipeRefresh).setOnRefreshListener {
            val d = findViewById<TextView>(R.id.id_desde).text.toString()
            val h = findViewById<TextView>(R.id.id_hasta).text.toString()
            buscar(d, h, true)
        }
    }

    /** Fija Desde/Hasta según el rango rápido y busca. */
    private fun aplicarRango(id: Int) {
        val (desde, hasta) = FechasRango.fechasDe(id)
        findViewById<TextView>(R.id.id_desde).text = desde
        findViewById<TextView>(R.id.id_hasta).text = hasta
        marcarRango(id)
        buscar(desde, hasta)
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

    private fun selectDesde(date: String) {
        marcarRango(null)
        findViewById<TextView>(R.id.id_desde).text = date
        val hasta = findViewById<TextView>(R.id.id_hasta).text?.toString()?.trim()
        if (!hasta.isNullOrEmpty()) buscar(date, hasta)
    }

    private fun selectHasta(date: String) {
        marcarRango(null)
        findViewById<TextView>(R.id.id_hasta).text = date
        val desde = findViewById<TextView>(R.id.id_desde).text?.toString()?.trim()
        if (!desde.isNullOrEmpty()) buscar(desde, date)
    }

    private fun buscar(desdeSel: String, hastaSel: String, isRefresh: Boolean = false) {
        // Rango invertido (Desde > Hasta): antes se enviaba tal cual y el backend devolvía vacío.
        val (desde, hasta) = if (desdeSel > hastaSel) hastaSel to desdeSel else desdeSel to hastaSel

        if (rangoActivo == null) {
            findViewById<TextView>(R.id.tvResumenTitulo).text = FechasRango.rangoLegible(desde, hasta)
        }

        val id = ++busquedaActual
        if (!isRefresh) {
            findViewById<View>(R.id.lyt_loading).visibility = View.VISIBLE
        }

        ApiService.fetchReporteFacturacion(
            desde = desde,
            hasta = hasta,
            onResult = { items ->
                if (id != busquedaActual || isFinishing || isDestroyed) return@fetchReporteFacturacion
                facturas.clear()
                facturas.addAll(items)
                mostrarResultados(null)
            },
            onError = { error ->
                if (id != busquedaActual || isFinishing || isDestroyed) return@fetchReporteFacturacion
                facturas.clear()
                mostrarResultados(error)
            }
        )
    }

    /** [error] no nulo = la búsqueda falló; se muestra en el estado vacío en vez de "Sin facturas". */
    private fun mostrarResultados(error: String?) {
        findViewById<SwipeRefreshLayout>(R.id.swipeRefresh).isRefreshing = false
        findViewById<View>(R.id.lyt_loading).visibility = View.GONE

        val rv = findViewById<RecyclerView>(R.id.recycler_view)
        val total = facturas.sumOf { it.total.replace(",", "").trim().toDoubleOrNull() ?: 0.0 }
        findViewById<TextView>(R.id.id_factura_total).text = "Σ C$ ${fmt.format(total)}"
        findViewById<TextView>(R.id.id_factura_count).text =
            "${facturas.size} factura${if (facturas.size == 1) "" else "s"}"

        if (facturas.isEmpty()) {
            findViewById<TextView>(R.id.tvEmptyTitulo).text =
                if (error != null) "No se pudo cargar" else "Sin facturas"
            findViewById<TextView>(R.id.tvEmptySubtitulo).text =
                if (error != null) "$error\nDesliza hacia abajo para reintentar"
                else "No hay facturas en este rango de fechas"
            findViewById<View>(R.id.lyt_empty_result).visibility = View.VISIBLE
            rv.visibility = View.GONE
            rv.adapter = null
            return
        }

        findViewById<View>(R.id.lyt_empty_result).visibility = View.GONE
        rv.visibility = View.VISIBLE
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = ReporteFacturaAdapter(facturas.toList()) { factura ->
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

    /** Abre el calendario en la fecha ya seleccionada (antes siempre abría en hoy). */
    private fun showDatePicker(actual: String, onDateSelected: (String) -> Unit) {
        val cal = Calendar.getInstance()
        runCatching { dateFormat.parse(actual) }.getOrNull()?.let { cal.time = it }
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
