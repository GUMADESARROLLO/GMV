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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SolicitudesActivity : AppCompatActivity() {

    private val items = mutableListOf(
        SolicitudItem("1", "FARMACIA SANTA FE", "00592", "39709.86", "26/05/2026", "Aprobada", "Incremento de limite para temporada"),
        SolicitudItem("2", "FARMACIA GUANUCA", "03359", "30545.30", "26/05/2026", "Pendiente", "Compra de inventario"),
        SolicitudItem("3", "FARMACIA SINAI", "03859", "13636.80", "25/05/2026", "Pendiente", "Aumento de credito"),
        SolicitudItem("4", "FARMACIA EL HOGAR", "03514", "2263.94", "25/05/2026", "Rechazada", "Solicitud fuera de plazo"),
        SolicitudItem("5", "FARMACIA LUNSA", "00561", "32709.52", "24/05/2026", "Aprobada", "Incremento estacional"),
        SolicitudItem("6", "FARMACIA RIJAS", "04787", "21533.10", "24/05/2026", "Pendiente", "Nuevo limite"),
        SolicitudItem("7", "FARMACIA MORAZAN", "04760", "2800.44", "23/05/2026", "Pendiente", "Aumento de credito"),
        SolicitudItem("8", "FARMACIA VIDA", "00572", "3546.98", "23/05/2026", "Aprobada", "Renovacion de credito")
    )

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

        val rv = findViewById<RecyclerView>(R.id.rvSolicitudes)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = SolicitudesAdapter(items) { solicitud ->
            val monto = solicitud.monto.replace(",", "").toDoubleOrNull() ?: 0.0
            val intent = Intent(this, SolicitarIncrementoAutorizadoActivity::class.java).apply {
                putExtra("monto", monto)
                putExtra("clienteNombre", solicitud.cliente)
                putExtra("clienteCodigo", solicitud.codCliente)
                putExtra("motivo", solicitud.motivo)
                putExtra("saldo", 0.0)
                putExtra("limite", 0.0)
                putExtra("disponible", 0.0)
            }
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        mostrarResultados()

        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        findViewById<TextView>(R.id.tvDateFrom).text = dateFmt.format(cal.time)
        findViewById<TextView>(R.id.tvDateTo).text = dateFmt.format(Calendar.getInstance().time)
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
        val fmt = java.text.DecimalFormat("#,##0.00", java.text.DecimalFormatSymbols(Locale.US))
        val total = items.sumOf { it.monto.replace(",", "").toDoubleOrNull() ?: 0.0 }
        findViewById<TextView>(R.id.tvTotalMonto).text = "C$ ${fmt.format(total)}"
        findViewById<TextView>(R.id.tvCount).text = "${items.size}"
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