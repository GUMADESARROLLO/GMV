package com.unimarksa.gmv.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.unimarksa.gmv.R
import com.unimarksa.gmv.models.SolicitudItem
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class SolicitudesAdapter(
    private val items: List<SolicitudItem>,
    private val onClick: (SolicitudItem) -> Unit = {}
) : RecyclerView.Adapter<SolicitudesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardSolicitud: MaterialCardView = view.findViewById(R.id.cardSolicitud)
        val tvClientInitial: TextView = view.findViewById(R.id.tvClientInitial)
        val tvClientName: TextView = view.findViewById(R.id.tvClientName)
        val tvMotivo: TextView = view.findViewById(R.id.tvMotivo)
        val tvMonto: TextView = view.findViewById(R.id.tvMonto)
        val tvEstado: TextView = view.findViewById(R.id.tvEstado)
        val tvFecha: TextView = view.findViewById(R.id.tvFecha)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_solicitud, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val ctx = holder.itemView.context
        val fmt = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))

        val initial = item.cliente.firstOrNull()?.uppercaseChar()?.toString() ?: "S"
        holder.tvClientInitial.text = initial
        holder.tvClientName.text = item.cliente.ifEmpty { "Cliente" }
        holder.tvMotivo.text = item.motivo.ifEmpty { "Sin motivo" }
        holder.tvFecha.text = item.fecha
        holder.cardSolicitud.setOnClickListener { onClick(item) }

        val monto = item.monto.replace(",", "").toDoubleOrNull() ?: 0.0
        holder.tvMonto.text = "C$ ${fmt.format(monto)}"

        when {
            item.estado.contains("Aprob", ignoreCase = true) -> {
                holder.tvEstado.text = "Aprobada"
                holder.tvEstado.setBackgroundColor(ContextCompat.getColor(ctx, R.color.status_active_bg))
                holder.tvEstado.setTextColor(ContextCompat.getColor(ctx, R.color.status_active_text))
            }
            item.estado.contains("Rechaz", ignoreCase = true) -> {
                holder.tvEstado.text = "Rechazada"
                holder.tvEstado.setBackgroundColor(ContextCompat.getColor(ctx, R.color.status_cancel_bg))
                holder.tvEstado.setTextColor(ContextCompat.getColor(ctx, R.color.status_cancel_text))
            }
            else -> {
                holder.tvEstado.text = "Pendiente"
                holder.tvEstado.setBackgroundColor(ContextCompat.getColor(ctx, R.color.status_pending_bg))
                holder.tvEstado.setTextColor(ContextCompat.getColor(ctx, R.color.status_pending_text))
            }
        }
    }

    override fun getItemCount() = items.size
}