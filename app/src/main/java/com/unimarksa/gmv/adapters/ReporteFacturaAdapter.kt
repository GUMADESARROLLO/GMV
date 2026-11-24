package com.unimarksa.gmv.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.unimarksa.gmv.R
import com.unimarksa.gmv.models.ItemReporteFactura
import com.unimarksa.gmv.tools.FechasRango
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Facturación agrupada por día: un encabezado por fecha (Hoy / Ayer / LUN 22 NOV) con la
 * cantidad de facturas y el Σ del día, seguido de sus facturas. Días más recientes primero.
 */
class ReporteFacturaAdapter(
    items: List<ItemReporteFactura>,
    private val onClick: (ItemReporteFactura) -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private sealed class Fila {
        data class Dia(val titulo: String, val cantidad: Int, val total: Double) : Fila()
        data class Factura(val item: ItemReporteFactura) : Fila()
    }

    private val fmt = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
    private val filas: List<Fila> = agrupar(items)

    class FacturaVH(view: View) : RecyclerView.ViewHolder(view) {
        val cardFactura: View = view.findViewById(R.id.cardFactura)
        val tvFechaFactura: TextView = view.findViewById(R.id.tvFechaFactura)
        val divFactura: View = view.findViewById(R.id.divFactura)
        val lytFacturaPie: View = view.findViewById(R.id.lytFacturaPie)
        val tvClientName: TextView = view.findViewById(R.id.tvClientName)
        val tvFacturaNumero: TextView = view.findViewById(R.id.tvFacturaNumero)
        val tvFacturaTotal: TextView = view.findViewById(R.id.tvFacturaTotal)
        val tvEstado: TextView = view.findViewById(R.id.tvEstado)
        val tvVencimiento: TextView = view.findViewById(R.id.tvVencimiento)
        val ivVencimiento: View = view.findViewById(R.id.ivVencimiento)
    }

    class DiaVH(view: View) : RecyclerView.ViewHolder(view) {
        val titulo: TextView = view.findViewById(R.id.tvDiaTitulo)
        val conteo: TextView = view.findViewById(R.id.tvDiaConteo)
        val total: TextView = view.findViewById(R.id.tvDiaTotal)
    }

    override fun getItemViewType(position: Int) = if (filas[position] is Fila.Dia) TIPO_DIA else TIPO_FACTURA

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TIPO_DIA) DiaVH(inflater.inflate(R.layout.item_reporte_dia, parent, false))
        else FacturaVH(inflater.inflate(R.layout.item_reporte_factura, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val fila = filas[position]) {
            is Fila.Dia -> {
                holder as DiaVH
                holder.titulo.text = fila.titulo
                holder.conteo.text = "${fila.cantidad} factura${if (fila.cantidad == 1) "" else "s"}"
                holder.total.text = "Σ C$ ${fmt.format(fila.total)}"
                holder.itemView.contentDescription =
                    "${fila.titulo}, ${holder.conteo.text}, total C$ ${fmt.format(fila.total)}"
            }
            is Fila.Factura -> bindFactura(holder as FacturaVH, fila.item)
        }
    }

    private fun bindFactura(holder: FacturaVH, item: ItemReporteFactura) {
        val ctx = holder.itemView.context
        val total = monto(item.total)

        holder.tvClientName.text = item.cliente.ifEmpty { "Cliente" }
        holder.tvFacturaNumero.text = "Fact. ${item.numero.ifEmpty { "—" }}"
        holder.tvFacturaTotal.text = "C$ ${fmt.format(total)}"
        // La fecha ya está en el encabezado del día: aquí solo el vencimiento
        // Fecha de la factura bajo el monto; abajo solo el vencimiento (si viene)
        val fechaFactura = FechasRango.diaDe(item.fecha)?.let { FechasRango.FECHA_CORTA.format(it) } ?: item.fecha.trim()
        holder.tvFechaFactura.text = fechaFactura
        holder.tvFechaFactura.visibility = if (fechaFactura.isNotBlank()) View.VISIBLE else View.GONE
        val hayVence = item.vencimiento.isNotBlank()
        holder.tvVencimiento.text = if (hayVence) "Vence ${item.vencimiento}" else ""
        holder.tvVencimiento.visibility = if (hayVence) View.VISIBLE else View.GONE
        holder.ivVencimiento.visibility = if (hayVence) View.VISIBLE else View.GONE
        if (hayVence) {
            val dias = FechasRango.diaDe(item.vencimiento)?.let { FechasRango.diasDesdeHoy(it) }
            val colorVence = when {
                dias == null -> R.color.umk_ink_muted
                dias < 0 -> R.color.umk_danger
                dias <= 7 -> R.color.status_pending_text
                else -> R.color.umk_ink_muted
            }
            holder.tvVencimiento.text = if (dias != null && dias < 0) "Vencida ${item.vencimiento}" else "Vence ${item.vencimiento}"
            holder.tvVencimiento.setTextColor(ContextCompat.getColor(ctx, colorVence))
            holder.ivVencimiento.backgroundTintList = null
            (holder.ivVencimiento as? android.widget.ImageView)?.imageTintList =
                ContextCompat.getColorStateList(ctx, colorVence)
        }

        // Sin saldo del backend no hay estado que mostrar (antes decía "Emitida")
        val saldo = item.saldo.replace(",", "").trim().toDoubleOrNull()
        holder.tvEstado.visibility = if (saldo == null) View.GONE else View.VISIBLE
        val (texto, bg, fg) = when {
            saldo == null -> Triple("", R.color.umk_royal_tint, R.color.umk_royal)
            saldo <= 0.005 -> Triple("Pagada", R.color.status_active_bg, R.color.status_active_text)
            else -> Triple("Saldo C$ ${fmt.format(saldo)}", R.color.status_pending_bg, R.color.status_pending_text)
        }
        holder.tvEstado.text = texto
        // Si no hay estado ni vencimiento, la fila inferior (y su divisor) sobra
        val pieVisible = saldo != null || hayVence
        holder.divFactura.visibility = if (pieVisible) View.VISIBLE else View.GONE
        holder.lytFacturaPie.visibility = if (pieVisible) View.VISIBLE else View.GONE
        holder.tvEstado.backgroundTintList = ContextCompat.getColorStateList(ctx, bg)
        holder.tvEstado.setTextColor(ContextCompat.getColor(ctx, fg))

        holder.cardFactura.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = filas.size

    // ── Agrupación ───────────────────────────────────────────────────────────

    private fun monto(raw: String) = raw.replace(",", "").trim().toDoubleOrNull() ?: 0.0

    private fun agrupar(items: List<ItemReporteFactura>): List<Fila> {
        val salida = mutableListOf<Fila>()
        for ((dia, grupo) in FechasRango.agruparPorDia(items) { it.fecha }) {
            salida += Fila.Dia(FechasRango.tituloDia(dia), grupo.size, grupo.sumOf { monto(it.total) })
            grupo.forEach { salida += Fila.Factura(it) }
        }
        return salida
    }

    companion object {
        private const val TIPO_DIA = 0
        private const val TIPO_FACTURA = 1
    }
}
