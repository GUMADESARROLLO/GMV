package com.unimarksa.gmv.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.unimarksa.gmv.R
import com.unimarksa.gmv.models.SolicitudItem
import com.unimarksa.gmv.tools.FechasRango
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Solicitudes agrupadas por día (mismo patrón que Facturación): un encabezado por fecha
 * (Hoy / Ayer / LUN 22 NOV) con la cantidad de solicitudes y el Σ del día, seguido de sus
 * solicitudes. Días más recientes primero.
 */
class SolicitudesAdapter(
    items: List<SolicitudItem>,
    private val onClick: (SolicitudItem) -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private sealed class Fila {
        data class Dia(val titulo: String, val cantidad: Int, val total: Double) : Fila()
        data class Solicitud(val item: SolicitudItem) : Fila()
    }

    private val fmt = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
    private val filas: List<Fila> = agrupar(items)

    class SolicitudVH(view: View) : RecyclerView.ViewHolder(view) {
        val cardSolicitud: View = view.findViewById(R.id.cardSolicitud)
        val tvClientName: TextView = view.findViewById(R.id.tvClientName)
        val tvMotivo: TextView = view.findViewById(R.id.tvMotivo)
        val tvMonto: TextView = view.findViewById(R.id.tvMonto)
        val tvFecha: TextView = view.findViewById(R.id.tvFecha)
        val tvEstado: TextView = view.findViewById(R.id.tvEstado)
        val ivHora: View = view.findViewById(R.id.ivHora)
        val tvHora: TextView = view.findViewById(R.id.tvHora)
    }

    class DiaVH(view: View) : RecyclerView.ViewHolder(view) {
        val titulo: TextView = view.findViewById(R.id.tvDiaTitulo)
        val conteo: TextView = view.findViewById(R.id.tvDiaConteo)
        val total: TextView = view.findViewById(R.id.tvDiaTotal)
    }

    override fun getItemViewType(position: Int) = if (filas[position] is Fila.Dia) TIPO_DIA else TIPO_SOLICITUD

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TIPO_DIA) DiaVH(inflater.inflate(R.layout.item_reporte_dia, parent, false))
        else SolicitudVH(inflater.inflate(R.layout.item_solicitud, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val fila = filas[position]) {
            is Fila.Dia -> {
                holder as DiaVH
                holder.titulo.text = fila.titulo
                holder.conteo.text = conteoSolicitudes(fila.cantidad)
                holder.total.text = "Σ C$ ${fmt.format(fila.total)}"
                holder.itemView.contentDescription =
                    "${fila.titulo}, ${holder.conteo.text}, total C$ ${fmt.format(fila.total)}"
            }
            is Fila.Solicitud -> bindSolicitud(holder as SolicitudVH, fila.item)
        }
    }

    private fun bindSolicitud(holder: SolicitudVH, item: SolicitudItem) {
        val ctx = holder.itemView.context

        holder.tvClientName.text = item.cliente.ifEmpty { "Cliente" }
        holder.tvMotivo.text = item.motivo.ifEmpty { "Sin motivo" }
        holder.tvMonto.text = "C$ ${fmt.format(monto(item.monto))}"

        // Fecha de la solicitud bajo el monto; abajo solo la hora (si viene)
        val fecha = FechasRango.diaDe(item.fecha)?.let { FechasRango.FECHA_CORTA.format(it) } ?: item.fecha.trim()
        holder.tvFecha.text = fecha
        holder.tvFecha.visibility = if (fecha.isNotBlank()) View.VISIBLE else View.GONE

        val hora = horaDe(item.fecha)
        holder.tvHora.text = hora.orEmpty()
        holder.tvHora.visibility = if (hora != null) View.VISIBLE else View.GONE
        holder.ivHora.visibility = if (hora != null) View.VISIBLE else View.GONE

        val (texto, bg, fg) = when (estadoDe(item.estado)) {
            Estado.APROBADA -> Triple("Aprobada", R.color.status_active_bg, R.color.status_active_text)
            Estado.RECHAZADA -> Triple("Rechazada", R.color.status_cancel_bg, R.color.status_cancel_text)
            Estado.PENDIENTE -> Triple("Pendiente", R.color.status_pending_bg, R.color.status_pending_text)
        }
        holder.tvEstado.text = texto
        holder.tvEstado.backgroundTintList = ContextCompat.getColorStateList(ctx, bg)
        holder.tvEstado.setTextColor(ContextCompat.getColor(ctx, fg))

        holder.cardSolicitud.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = filas.size

    // ── Agrupación ───────────────────────────────────────────────────────────

    private fun agrupar(items: List<SolicitudItem>): List<Fila> {
        val salida = mutableListOf<Fila>()
        for ((dia, grupo) in FechasRango.agruparPorDia(items) { it.fecha }) {
            salida += Fila.Dia(FechasRango.tituloDia(dia), grupo.size, grupo.sumOf { monto(it.monto) })
            grupo.forEach { salida += Fila.Solicitud(it) }
        }
        return salida
    }

    enum class Estado { APROBADA, PENDIENTE, RECHAZADA }

    companion object {
        private const val TIPO_DIA = 0
        private const val TIPO_SOLICITUD = 1

        private val HORA = Regex("""\s(\d{1,2}):(\d{2})""")

        /** Misma clasificación en la lista y en la línea de resumen de la Activity. */
        fun estadoDe(raw: String): Estado = when {
            raw.contains("Aprob", ignoreCase = true) -> Estado.APROBADA
            raw.contains("Rechaz", ignoreCase = true) -> Estado.RECHAZADA
            else -> Estado.PENDIENTE
        }

        fun monto(raw: String) = raw.replace(",", "").trim().toDoubleOrNull() ?: 0.0

        fun conteoSolicitudes(n: Int) = "$n solicitud${if (n == 1) "" else "es"}"

        /** "HH:mm" de un texto "yyyy-MM-dd HH:mm:ss"; null si no trae hora. */
        fun horaDe(raw: String): String? =
            HORA.find(raw.trim())?.let { "${it.groupValues[1].padStart(2, '0')}:${it.groupValues[2]}" }
    }
}
