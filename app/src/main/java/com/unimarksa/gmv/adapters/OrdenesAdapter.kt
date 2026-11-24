package com.unimarksa.gmv.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.unimarksa.gmv.R
import com.unimarksa.gmv.models.ItemPedidoCliente
import com.unimarksa.gmv.tools.FechasRango
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Órdenes agrupadas por día (mismo patrón que Facturación): un encabezado por fecha
 * (HOY / AYER / LUN 22 NOV) con la cantidad de órdenes y el Σ del día, seguido de sus
 * órdenes. Días más recientes primero.
 */
class OrdenesAdapter(
    items: List<ItemPedidoCliente>,
    private val onItemClick: (ItemPedidoCliente) -> Unit = {},
    private val onDeleteClick: (ItemPedidoCliente) -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    /** Agrupación de estados para el chip de la fila. */
    enum class CategoriaEstado { ENVIADA, PENDIENTE, ANULADA, SIN_ESTADO }

    private sealed class Fila {
        data class Dia(val titulo: String, val cantidad: Int, val total: Double) : Fila()
        data class Orden(val item: ItemPedidoCliente) : Fila()
    }

    private val fmt = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
    private val filas: List<Fila> = agrupar(items)

    class OrdenVH(view: View) : RecyclerView.ViewHolder(view) {
        val cardOrden: View = view.findViewById(R.id.cardOrden)
        val tvClientName: TextView = view.findViewById(R.id.tvClientName)
        val tvOrderNumber: TextView = view.findViewById(R.id.tvOrderNumber)
        val tvMonto: TextView = view.findViewById(R.id.tvMonto)
        val tvFecha: TextView = view.findViewById(R.id.tvFecha)
        val divOrden: View = view.findViewById(R.id.divOrden)
        val lytOrdenPie: View = view.findViewById(R.id.lytOrdenPie)
        val tvEstado: TextView = view.findViewById(R.id.tvEstado)
        val ivDelete: ImageView = view.findViewById(R.id.ivDelete)
    }

    class DiaVH(view: View) : RecyclerView.ViewHolder(view) {
        val titulo: TextView = view.findViewById(R.id.tvDiaTitulo)
        val conteo: TextView = view.findViewById(R.id.tvDiaConteo)
        val total: TextView = view.findViewById(R.id.tvDiaTotal)
    }

    override fun getItemViewType(position: Int) = if (filas[position] is Fila.Dia) TIPO_DIA else TIPO_ORDEN

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TIPO_DIA) DiaVH(inflater.inflate(R.layout.item_reporte_dia, parent, false))
        else OrdenVH(inflater.inflate(R.layout.item_orden_cliente, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val fila = filas[position]) {
            is Fila.Dia -> {
                holder as DiaVH
                holder.titulo.text = fila.titulo
                holder.conteo.text = "${fila.cantidad} orden${if (fila.cantidad == 1) "" else "es"}"
                holder.total.text = "Σ C$ ${fmt.format(fila.total)}"
                holder.itemView.contentDescription =
                    "${fila.titulo}, ${holder.conteo.text}, total C$ ${fmt.format(fila.total)}"
            }
            is Fila.Orden -> bindOrden(holder as OrdenVH, fila.item)
        }
    }

    private fun bindOrden(holder: OrdenVH, item: ItemPedidoCliente) {
        val ctx = holder.itemView.context

        holder.cardOrden.setOnClickListener { onItemClick(item) }

        holder.tvClientName.text = item.cliente.ifEmpty { "Cliente" }

        // Número de orden + cantidad de artículos (si viene)
        val articulos = item.articulos.trim().toIntOrNull() ?: 0
        val numero = "Ord. N° ${item.numero.ifEmpty { "—" }}"
        holder.tvOrderNumber.text = if (articulos > 0) {
            "$numero · $articulos artículo${if (articulos == 1) "" else "s"}"
        } else {
            numero
        }

        holder.tvMonto.text = "C$ ${fmt.format(monto(item.monto))}"

        // Fecha de la orden bajo el monto (en vez de la etiqueta "Total")
        val fecha = FechasRango.diaDe(item.fecha)?.let { FechasRango.FECHA_CORTA.format(it) } ?: item.fecha.trim()
        holder.tvFecha.text = fecha
        holder.tvFecha.visibility = if (fecha.isNotBlank()) View.VISIBLE else View.GONE

        // Chip de estado: backgroundTintList conserva la forma de píldora
        val cat = categoria(item.estado)
        val (bg, txt) = when (cat) {
            CategoriaEstado.PENDIENTE -> R.color.status_pending_bg to R.color.status_pending_text
            CategoriaEstado.ANULADA -> R.color.status_cancel_bg to R.color.status_cancel_text
            CategoriaEstado.ENVIADA -> R.color.status_active_bg to R.color.status_active_text
            CategoriaEstado.SIN_ESTADO -> R.color.status_active_bg to R.color.status_active_text
        }
        val hayEstado = cat != CategoriaEstado.SIN_ESTADO
        holder.tvEstado.visibility = if (hayEstado) View.VISIBLE else View.GONE
        holder.tvEstado.text = item.estado.trim().lowercase().replaceFirstChar { it.uppercase() }
        holder.tvEstado.backgroundTintList = ContextCompat.getColorStateList(ctx, bg)
        holder.tvEstado.setTextColor(ContextCompat.getColor(ctx, txt))

        // Eliminar solo en órdenes locales (id de Room)
        val esLocal = item.id != -1L
        if (esLocal) {
            holder.ivDelete.visibility = View.VISIBLE
            holder.ivDelete.setOnClickListener { onDeleteClick(item) }
        } else {
            holder.ivDelete.visibility = View.GONE
            holder.ivDelete.setOnClickListener(null)
        }

        // Sin estado ni acción de eliminar, la fila inferior (y su divisor) sobra
        val pieVisible = hayEstado || esLocal
        holder.divOrden.visibility = if (pieVisible) View.VISIBLE else View.GONE
        holder.lytOrdenPie.visibility = if (pieVisible) View.VISIBLE else View.GONE
    }

    override fun getItemCount() = filas.size

    // ── Agrupación ───────────────────────────────────────────────────────────

    private fun monto(raw: String) = raw.replace(",", "").trim().toDoubleOrNull() ?: 0.0

    private fun agrupar(items: List<ItemPedidoCliente>): List<Fila> {
        val salida = mutableListOf<Fila>()
        for ((dia, grupo) in FechasRango.agruparPorDia(items) { it.fecha }) {
            salida += Fila.Dia(FechasRango.tituloDia(dia), grupo.size, grupo.sumOf { monto(it.monto) })
            grupo.forEach { salida += Fila.Orden(it) }
        }
        return salida
    }

    companion object {
        private const val TIPO_DIA = 0
        private const val TIPO_ORDEN = 1

        fun categoria(estado: String): CategoriaEstado {
            val e = estado.trim()
            return when {
                e.isEmpty() -> CategoriaEstado.SIN_ESTADO
                e.contains("pend", true) -> CategoriaEstado.PENDIENTE
                e.contains("anul", true) || e.contains("cancel", true) ||
                    e.contains("rechaz", true) || e.contains("elimin", true) -> CategoriaEstado.ANULADA
                else -> CategoriaEstado.ENVIADA
            }
        }
    }
}
