package com.unimarksa.gmv.adapters

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.unimarksa.gmv.R
import com.unimarksa.gmv.models.ItemReporteRecibo
import com.unimarksa.gmv.tools.FechasRango
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Recibos agrupados por día (mismo patrón que [ReporteFacturaAdapter]): un encabezado por fecha
 * (HOY / AYER / LUN 22 NOV) con la cantidad de recibos y el Σ del día sin anulados, seguido de sus
 * recibos. Días más recientes primero.
 *
 * [items] es la lista de la Activity; tras modificarla se llama [actualizar] para reagrupar
 * (eliminar un recibo puede dejar un día vacío y su encabezado debe desaparecer).
 */
class ReporteReciboAdapter(
    private val items: List<ItemReporteRecibo>,
    private val onClick: (ItemReporteRecibo) -> Unit = {},
    private val onDelete: ((ItemReporteRecibo) -> Unit)? = null,
    private val onAttach: ((ItemReporteRecibo) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private sealed class Fila {
        data class Dia(val titulo: String, val cantidad: Int, val total: Double) : Fila()
        data class Recibo(val item: ItemReporteRecibo) : Fila()
    }

    private val fmt = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
    private var filas: List<Fila> = agrupar(items)

    class ReciboVH(view: View) : RecyclerView.ViewHolder(view) {
        val cardRecibo: View = view.findViewById(R.id.cardRecibo)
        val tvClientName: TextView = view.findViewById(R.id.tvClientName)
        val tvClientCode: TextView = view.findViewById(R.id.tvClientCode)
        val tvMonto: TextView = view.findViewById(R.id.tvMonto)
        val tvFechaRecibo: TextView = view.findViewById(R.id.tvFechaRecibo)
        val tvEstado: TextView = view.findViewById(R.id.tvEstado)
        val btnAttach: ImageButton = view.findViewById(R.id.btnAttach)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    class DiaVH(view: View) : RecyclerView.ViewHolder(view) {
        val titulo: TextView = view.findViewById(R.id.tvDiaTitulo)
        val conteo: TextView = view.findViewById(R.id.tvDiaConteo)
        val total: TextView = view.findViewById(R.id.tvDiaTotal)
    }

    /** Reagrupa desde [items] (la lista de la Activity) y redibuja. */
    fun actualizar() {
        filas = agrupar(items)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int) = if (filas[position] is Fila.Dia) TIPO_DIA else TIPO_RECIBO

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TIPO_DIA) DiaVH(inflater.inflate(R.layout.item_reporte_dia, parent, false))
        else ReciboVH(inflater.inflate(R.layout.item_recibo, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val fila = filas[position]) {
            is Fila.Dia -> {
                holder as DiaVH
                holder.titulo.text = fila.titulo
                holder.conteo.text = "${fila.cantidad} recibo${if (fila.cantidad == 1) "" else "s"}"
                holder.total.text = "Σ C$ ${fmt.format(fila.total)}"
                holder.itemView.contentDescription =
                    "${fila.titulo}, ${holder.conteo.text}, total C$ ${fmt.format(fila.total)}"
            }
            is Fila.Recibo -> try {
                bindRecibo(holder as ReciboVH, fila.item)
            } catch (_: Exception) { }
        }
    }

    /** Recibo de la fila actual del holder (las posiciones incluyen encabezados de día). */
    private fun reciboEn(holder: RecyclerView.ViewHolder): ItemReporteRecibo? {
        val pos = holder.bindingAdapterPosition
        if (pos == RecyclerView.NO_POSITION) return null
        return (filas.getOrNull(pos) as? Fila.Recibo)?.item
    }

    private fun bindRecibo(holder: ReciboVH, item: ItemReporteRecibo) {
        val ctx = holder.itemView.context
        val anulado = esAnulado(item)

        // "00123 - NOMBRE - RUC J0310..." → "NOMBRE"
        val nombre = item.nombreCliente.replace(Regex("^\\d+\\s*-\\s*"), "")
            .replace(Regex("\\s+-\\s*RUC.*"), "").trim()
        holder.tvClientName.text = nombre.ifEmpty { "Cliente" }

        val codigo = item.codCliente.replace(Regex("[^\\d]"), "").ifEmpty { item.codCliente }
        val numero = item.recibo.replace("#", "").trim()
        holder.tvClientCode.text = listOfNotNull(
            codigo.takeIf { it.isNotBlank() }?.let { "Cód. $it" },
            numero.takeIf { it.isNotBlank() }?.let { "Rec. #$it" }
        ).joinToString(" · ")

        // Verde = dinero recibido; anulado no suma al total: gris y tachado
        holder.tvMonto.text = "C$ ${fmt.format(monto(item.orderTotal))}"
        holder.tvMonto.setTextColor(ContextCompat.getColor(ctx, if (anulado) R.color.umk_ink_muted else R.color.umk_success))
        holder.tvMonto.paintFlags = if (anulado) holder.tvMonto.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        else holder.tvMonto.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()

        // Fecha del recibo bajo el monto
        val fecha = FechasRango.diaDe(item.fecha)?.let { FechasRango.FECHA_CORTA.format(it) } ?: item.fecha.trim()
        holder.tvFechaRecibo.text = fecha
        holder.tvFechaRecibo.visibility = if (fecha.isNotBlank()) View.VISIBLE else View.GONE

        // Chip de estado solo para anulados (tint sobre la forma píldora, no setBackgroundColor)
        holder.tvEstado.visibility = if (anulado) View.VISIBLE else View.GONE
        if (anulado) {
            holder.tvEstado.text = "Anulado"
            holder.tvEstado.backgroundTintList = ContextCompat.getColorStateList(ctx, R.color.status_cancel_bg)
            holder.tvEstado.setTextColor(ContextCompat.getColor(ctx, R.color.status_cancel_text))
        }

        // Un recibo ya anulado no se puede volver a eliminar
        holder.btnDelete.visibility = if (anulado || onDelete == null) View.GONE else View.VISIBLE

        // Se resuelve el recibo al momento del click (la posición capturada en bind queda desfasada)
        holder.btnDelete.setOnClickListener { reciboEn(holder)?.let { onDelete?.invoke(it) } }
        holder.btnAttach.setOnClickListener { reciboEn(holder)?.let { onAttach?.invoke(it) } }
        holder.cardRecibo.setOnClickListener { reciboEn(holder)?.let(onClick) }
    }

    override fun getItemCount() = filas.size

    // ── Agrupación ───────────────────────────────────────────────────────────

    private fun agrupar(items: List<ItemReporteRecibo>): List<Fila> {
        val salida = mutableListOf<Fila>()
        for ((dia, grupo) in FechasRango.agruparPorDia(items) { it.fecha }) {
            val totalDia = grupo.filterNot { esAnulado(it) }.sumOf { monto(it.orderTotal) }
            salida += Fila.Dia(FechasRango.tituloDia(dia), grupo.size, totalDia)
            grupo.forEach { salida += Fila.Recibo(it) }
        }
        return salida
    }

    companion object {
        private const val TIPO_DIA = 0
        private const val TIPO_RECIBO = 1

        fun esAnulado(item: ItemReporteRecibo): Boolean =
            item.status == "4" || item.status.contains("Anul", ignoreCase = true)

        /** Monto del backend ("C$ 1,234.50") → Double; 0 si no se puede leer. */
        fun monto(raw: String): Double =
            raw.replace("C$", "").replace(",", "").trim().toDoubleOrNull() ?: 0.0
    }
}
