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
import com.unimarksa.gmv.R
import com.unimarksa.gmv.tools.BarrasSistema
import com.unimarksa.gmv.adapters.SolicitudesAdapter
import com.unimarksa.gmv.models.SolicitudItem
import com.unimarksa.gmv.tools.ApiService
import com.unimarksa.gmv.tools.FechasRango
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Solicitudes de incremento: se descargan todas una vez y el rango de fechas filtra en local
 * (cambiar el rango no hace otra petición; deslizar hacia abajo recarga).
 */
class SolicitudesActivity : AppCompatActivity() {

    private val todasLasSolicitudes = mutableListOf<SolicitudItem>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val fmt = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))

    /** Error de la última carga (null = cargó bien); se muestra en el estado vacío. */
    private var errorCarga: String? = null

    /** Rango rápido activo (null = fechas elegidas a mano). */
    private var rangoActivo: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_solicitudes)

        BarrasSistema.aplicar(this, R.color.umk_mist)

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        findViewById<View>(R.id.cardDateFrom).setOnClickListener {
            showDatePicker(findViewById<TextView>(R.id.tvDateFrom).text.toString()) { updateDesde(it) }
        }
        findViewById<View>(R.id.cardDateTo).setOnClickListener {
            showDatePicker(findViewById<TextView>(R.id.tvDateTo).text.toString()) { updateHasta(it) }
        }

        FechasRango.RANGOS.forEach { (id, _) -> findViewById<View>(id).setOnClickListener { aplicarRango(id) } }
        aplicarRango(R.id.rango7)

        cargarSolicitudes()

        findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
            .setOnRefreshListener { cargarSolicitudes(true) }
    }

    private fun cargarSolicitudes(isRefresh: Boolean = false) {
        if (!isRefresh) findViewById<View>(R.id.lyt_loading).visibility = View.VISIBLE
        ApiService.fetchSolicitudes(
            onResult = { items ->
                if (isFinishing || isDestroyed) return@fetchSolicitudes
                findViewById<View>(R.id.lyt_loading).visibility = View.GONE
                errorCarga = null
                todasLasSolicitudes.clear()
                todasLasSolicitudes.addAll(items)
                mostrarResultados()
            },
            onError = { error ->
                if (isFinishing || isDestroyed) return@fetchSolicitudes
                findViewById<View>(R.id.lyt_loading).visibility = View.GONE
                errorCarga = error.ifBlank { "Error de conexión" }
                todasLasSolicitudes.clear()
                mostrarResultados()
            }
        )
    }

    /** Fija Desde/Hasta según el rango rápido y vuelve a filtrar (sin nueva petición). */
    private fun aplicarRango(id: Int) {
        val (desde, hasta) = FechasRango.fechasDe(id)
        findViewById<TextView>(R.id.tvDateFrom).text = desde
        findViewById<TextView>(R.id.tvDateTo).text = hasta
        marcarRango(id)
        mostrarResultados()
    }

    private fun marcarRango(id: Int?) {
        rangoActivo = id
        FechasRango.marcarRango(this, id)
        findViewById<TextView>(R.id.tvResumenTitulo).text = FechasRango.tituloResumen(
            id,
            findViewById<TextView>(R.id.tvDateFrom).text.toString(),
            findViewById<TextView>(R.id.tvDateTo).text.toString()
        )
    }

    private fun updateDesde(date: String) {
        findViewById<TextView>(R.id.tvDateFrom).text = date
        marcarRango(null)
        mostrarResultados()
    }

    private fun updateHasta(date: String) {
        findViewById<TextView>(R.id.tvDateTo).text = date
        marcarRango(null)
        mostrarResultados()
    }

    private fun mostrarResultados() {
        findViewById<SwipeRefreshLayout>(R.id.swipeRefresh).isRefreshing = false

        // Rango invertido (Desde > Hasta): se intercambia en vez de dejar la lista vacía.
        var desde = findViewById<TextView>(R.id.tvDateFrom).text.toString()
        var hasta = findViewById<TextView>(R.id.tvDateTo).text.toString()
        if (desde > hasta) { val t = desde; desde = hasta; hasta = t }

        if (rangoActivo == null) {
            findViewById<TextView>(R.id.tvResumenTitulo).text = FechasRango.rangoLegible(desde, hasta)
        }

        // La fecha viene como "yyyy-MM-dd HH:mm:ss"; si no se puede leer, la solicitud se muestra igual.
        val visibles = todasLasSolicitudes.filter {
            val dia = FechasRango.isoDe(it.fecha)
            dia == null || (dia >= desde && dia <= hasta)
        }

        val total = visibles.sumOf { SolicitudesAdapter.monto(it.monto) }
        findViewById<TextView>(R.id.tvTotalMonto).text = "Σ C$ ${fmt.format(total)}"

        val aprobadas = visibles.count { SolicitudesAdapter.estadoDe(it.estado) == SolicitudesAdapter.Estado.APROBADA }
        val pendientes = visibles.count { SolicitudesAdapter.estadoDe(it.estado) == SolicitudesAdapter.Estado.PENDIENTE }
        findViewById<TextView>(R.id.tvCount).text = buildList {
            add(SolicitudesAdapter.conteoSolicitudes(visibles.size))
            if (aprobadas > 0) add("$aprobadas aprobada${if (aprobadas == 1) "" else "s"}")
            if (pendientes > 0) add("$pendientes pendiente${if (pendientes == 1) "" else "s"}")
        }.joinToString(" · ")

        val rv = findViewById<RecyclerView>(R.id.rvSolicitudes)
        if (visibles.isEmpty()) {
            val error = errorCarga
            findViewById<TextView>(R.id.tvEmptyTitulo).text =
                if (error != null) "No se pudo cargar" else "Sin solicitudes"
            findViewById<TextView>(R.id.tvEmptySubtitulo).text =
                if (error != null) "$error\nDesliza hacia abajo para reintentar"
                else "No hay solicitudes en este rango de fechas"
            findViewById<View>(R.id.lyt_empty).visibility = View.VISIBLE
            rv.visibility = View.GONE
            rv.adapter = null
            return
        }

        findViewById<View>(R.id.lyt_empty).visibility = View.GONE
        rv.visibility = View.VISIBLE
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = SolicitudesAdapter(visibles) { solicitud ->
            val monto = solicitud.monto.replace(",", "").toDoubleOrNull() ?: 0.0
            val saldo = solicitud.saldoActual.replace(",", "").toDoubleOrNull() ?: 0.0
            val limite = solicitud.limiteActual.replace(",", "").toDoubleOrNull() ?: 0.0
            val disponible = solicitud.disponibleActual.replace(",", "").toDoubleOrNull() ?: 0.0
            val intent = Intent(this, SolicitarIncrementoAutorizadoActivity::class.java).apply {
                putExtra("monto", monto)
                putExtra("clienteNombre", solicitud.cliente)
                putExtra("clienteCodigo", solicitud.codCliente)
                putExtra("motivo", solicitud.motivo)
                putExtra("saldo", saldo)
                putExtra("limite", limite)
                putExtra("disponible", disponible)
                // Solicitud ya enviada: solo consulta, sin el control de enviar
                putExtra("soloLectura", true)
                putExtra("estado", solicitud.estado)
            }
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
