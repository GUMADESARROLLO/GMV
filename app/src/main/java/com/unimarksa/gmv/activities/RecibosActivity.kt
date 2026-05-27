package com.unimarksa.gmv.activities

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.unimarksa.gmv.R
import com.unimarksa.gmv.adapters.ReporteReciboAdapter
import com.unimarksa.gmv.models.ItemReporteRecibo
import com.unimarksa.gmv.tools.ApiService
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RecibosActivity : AppCompatActivity() {

    private val items = mutableListOf<ItemReporteRecibo>()
    private lateinit var adapter: ReporteReciboAdapter
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recibos)

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

        // Date pickers
        findViewById<View>(R.id.cardDateFrom).setOnClickListener { showDatePicker { updateDesde(it) } }
        findViewById<View>(R.id.cardDateTo).setOnClickListener { showDatePicker { updateHasta(it) } }

        // RecyclerView
        val rv = findViewById<RecyclerView>(R.id.recyclerRecibos)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = ReporteReciboAdapter(
            items = items,
            onClick = { recibo ->
                mostrarDetalle(recibo)
            },
            onDelete = { recibo, position ->
                confirmarEliminar(recibo, position)
            }
        )
        rv.adapter = adapter

        val hoy = dateFormat.format(Calendar.getInstance().time)
        findViewById<TextView>(R.id.tvDateFrom).text = hoy
        findViewById<TextView>(R.id.tvDateTo).text = hoy
        buscar(hoy, hoy)
    }

    private fun updateDesde(date: String) {
        findViewById<TextView>(R.id.tvDateFrom).text = date
        val hasta = findViewById<TextView>(R.id.tvDateTo).text?.toString()?.trim()
        if (!hasta.isNullOrEmpty()) buscar(date, hasta)
    }

    private fun updateHasta(date: String) {
        findViewById<TextView>(R.id.tvDateTo).text = date
        val desde = findViewById<TextView>(R.id.tvDateFrom).text?.toString()?.trim()
        if (!desde.isNullOrEmpty()) buscar(desde, date)
    }

    private fun buscar(desde: String, hasta: String) {
        val loading = findViewById<View>(R.id.lyt_loading)
        val empty = findViewById<View>(R.id.lyt_empty_result)
        val rv = findViewById<RecyclerView>(R.id.recyclerRecibos)
        val banner = findViewById<View>(R.id.layout_summary_banner)

        loading.visibility = View.VISIBLE
        empty.visibility = View.GONE
        rv.visibility = View.GONE
        banner.visibility = View.GONE

        ApiService.fetchRecibos(
            desde = desde,
            hasta = hasta,
            onResult = { result ->
                items.clear()
                items.addAll(result)
                adapter.notifyDataSetChanged()
                mostrarResultados()
            },
            onError = {
                items.clear()
                adapter.notifyDataSetChanged()
                mostrarResultados()
            }
        )
    }

    private fun mostrarResultados() {
        val loading = findViewById<View>(R.id.lyt_loading)
        val empty = findViewById<View>(R.id.lyt_empty_result)
        val rv = findViewById<RecyclerView>(R.id.recyclerRecibos)
        val banner = findViewById<View>(R.id.layout_summary_banner)

        loading.visibility = View.GONE
        if (items.isEmpty()) {
            empty.visibility = View.VISIBLE
            rv.visibility = View.GONE
            banner.visibility = View.GONE
            return
        }

        empty.visibility = View.GONE
        rv.visibility = View.VISIBLE
        banner.visibility = View.VISIBLE

        val total = items.sumOf {
            it.orderTotal.replace("C$", "").replace(",", "").trim().toDoubleOrNull() ?: 0.0
        }
        val fmt = java.text.DecimalFormat("#,##0.00", java.text.DecimalFormatSymbols(Locale.US))
        findViewById<TextView>(R.id.tvTotalAmount).text = "C$ ${fmt.format(total)}"
        findViewById<TextView>(R.id.tvReceiptCount).text = "${items.size}"
    }

    private fun mostrarDetalle(recibo: ItemReporteRecibo) {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_recibo_detalle, null)
        dialog.setContentView(view)

        view.findViewById<TextView>(R.id.tv_recibo_numero).text = "Recibo N° ${recibo.recibo}"
        view.findViewById<TextView>(R.id.tv_recibo_cliente).text = recibo.nombreCliente
        view.findViewById<TextView>(R.id.tv_recibo_fecha).text = recibo.fecha
        view.findViewById<TextView>(R.id.tv_recibo_total).text = recibo.orderTotal

        val container = view.findViewById<ViewGroup>(R.id.lyt_detalles)
        container.removeAllViews()
        val inflater = LayoutInflater.from(this)
        val fmt = java.text.DecimalFormat("#,##0.00", java.text.DecimalFormatSymbols(Locale.US))

        for (det in recibo.detalles) {
            val row = inflater.inflate(R.layout.item_recibo_detalle_row, container, false)
            row.findViewById<TextView>(R.id.tv_factura).text = "Fact. ${det.numeroFactura}"

            val actionBg = when {
                det.tipoAccion.contains("Cancelacion", ignoreCase = true) -> R.color.status_cancel_bg
                det.tipoAccion.contains("Abono", ignoreCase = true) -> R.color.status_pending_bg
                else -> R.color.status_active_bg
            }
            val actionText = when {
                det.tipoAccion.contains("Cancelacion", ignoreCase = true) -> R.color.status_cancel_text
                det.tipoAccion.contains("Abono", ignoreCase = true) -> R.color.status_pending_text
                else -> R.color.status_active_text
            }
            val tvTipoAccion = row.findViewById<TextView>(R.id.tv_tipo_accion)
            tvTipoAccion.text = det.tipoAccion
            tvTipoAccion.setBackgroundColor(ContextCompat.getColor(this, actionBg))
            tvTipoAccion.setTextColor(ContextCompat.getColor(this, actionText))

            row.findViewById<TextView>(R.id.tv_valor_recibido).text = "C$ ${fmt.format(det.valorRecibido)}"
            row.findViewById<TextView>(R.id.tv_total).text = "C$ ${fmt.format(det.total)}"
            val tvSaldo = row.findViewById<TextView>(R.id.tv_saldo_pendiente)
            tvSaldo.text = "C$ ${fmt.format(det.saldoPendiente)}"
            tvSaldo.setTextColor(if (det.saldoPendiente > 0) ContextCompat.getColor(this, R.color.amount_negative) else ContextCompat.getColor(this, R.color.amount_zero))

            row.findViewById<View>(R.id.conceptRetention).findViewById<TextView>(R.id.tvConceptLabel).text = "Retención"
            row.findViewById<View>(R.id.conceptRetention).findViewById<TextView>(R.id.tvConceptAmount).text = "C$ ${fmt.format(det.retencion)}"
            row.findViewById<View>(R.id.conceptDiscount).findViewById<TextView>(R.id.tvConceptLabel).text = "Descuento"
            row.findViewById<View>(R.id.conceptDiscount).findViewById<TextView>(R.id.tvConceptAmount).text = "C$ ${fmt.format(det.descuento)}"
            row.findViewById<View>(R.id.conceptCreditNote).findViewById<TextView>(R.id.tvConceptLabel).text = "N/C"
            row.findViewById<View>(R.id.conceptCreditNote).findViewById<TextView>(R.id.tvConceptAmount).text = "C$ ${fmt.format(det.nc)}"

            container.addView(row)
        }

        view.findViewById<View>(R.id.btn_cerrar).setOnClickListener { dialog.dismiss() }

        view.findViewById<View>(R.id.btn_adjuntar).setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(this, AdjuntarComprobanteActivity::class.java))
        }

        view.findViewById<View>(R.id.btn_delete_recibo).setOnClickListener {
            dialog.dismiss()
            val pos = items.indexOfFirst { it.id == recibo.id }
            if (pos >= 0) confirmarEliminar(recibo, pos)
        }

        dialog.show()
    }

    private fun confirmarEliminar(recibo: ItemReporteRecibo, position: Int) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Eliminar recibo")
            .setMessage("¿Eliminar recibo #${recibo.recibo} de ${recibo.nombreCliente}?")
            .setIcon(R.drawable.ic_trash)
            .setPositiveButton("Eliminar") { _, _ ->
                items.removeAt(position)
                adapter.notifyItemRemoved(position)
                mostrarResultados()
                Snackbar.make(findViewById(R.id.recyclerRecibos),
                    "Recibo #${recibo.recibo} eliminado", Snackbar.LENGTH_LONG).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
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
