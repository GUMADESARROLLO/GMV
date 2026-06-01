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
import com.unimarksa.gmv.R
import com.unimarksa.gmv.adapters.SolicitudesAdapter
import com.unimarksa.gmv.models.SolicitudItem
import com.unimarksa.gmv.tools.ApiService
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SolicitudesActivity : AppCompatActivity() {

    private val todasLasSolicitudes = mutableListOf<SolicitudItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_solicitudes)

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

        findViewById<View>(R.id.cardDateFrom).setOnClickListener { showDatePicker { updateDesde(it) } }
        findViewById<View>(R.id.cardDateTo).setOnClickListener { showDatePicker { updateHasta(it) } }

        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        findViewById<TextView>(R.id.tvDateFrom).text = dateFmt.format(cal.time)
        findViewById<TextView>(R.id.tvDateTo).text = dateFmt.format(Calendar.getInstance().time)

        cargarSolicitudes()

        findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefresh)
            .setOnRefreshListener { cargarSolicitudes() }
    }

    private fun cargarSolicitudes() {
        findViewById<View>(R.id.lyt_loading).visibility = View.VISIBLE
        ApiService.fetchSolicitudes(
            onResult = { items ->
                findViewById<View>(R.id.lyt_loading).visibility = View.GONE
                todasLasSolicitudes.clear()
                todasLasSolicitudes.addAll(items)
                mostrarResultados()
            },
            onError = {
                findViewById<View>(R.id.lyt_loading).visibility = View.GONE
                todasLasSolicitudes.clear()
                mostrarResultados()
            }
        )
    }

    private fun updateDesde(date: String) {
        findViewById<TextView>(R.id.tvDateFrom).text = date
        mostrarResultados()
    }

    private fun updateHasta(date: String) {
        findViewById<TextView>(R.id.tvDateTo).text = date
        mostrarResultados()
    }

    private fun mostrarResultados() {
        findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefresh).isRefreshing = false
        val fmt = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
        val total = todasLasSolicitudes.sumOf { it.monto.replace(",", "").toDoubleOrNull() ?: 0.0 }
        findViewById<TextView>(R.id.tvTotalMonto).text = "C$ ${fmt.format(total)}"
        findViewById<TextView>(R.id.tvCount).text = "${todasLasSolicitudes.size}"

        val rv = findViewById<RecyclerView>(R.id.rvSolicitudes)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = SolicitudesAdapter(todasLasSolicitudes) { solicitud ->
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
            }
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