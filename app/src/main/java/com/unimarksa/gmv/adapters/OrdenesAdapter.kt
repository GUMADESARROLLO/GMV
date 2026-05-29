package com.unimarksa.gmv.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.unimarksa.gmv.R
import com.unimarksa.gmv.models.ItemPedidoCliente
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class OrdenesAdapter(
    private val items: List<ItemPedidoCliente>,
    private val onItemClick: (ItemPedidoCliente) -> Unit = {},
    private val onDeleteClick: (ItemPedidoCliente) -> Unit = {}
) : RecyclerView.Adapter<OrdenesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardOrden: MaterialCardView = view.findViewById(R.id.cardOrden)
        val tvClientInitial: TextView = view.findViewById(R.id.tvClientInitial)
        val tvClientName: TextView = view.findViewById(R.id.tvClientName)
        val tvOrderNumber: TextView = view.findViewById(R.id.tvOrderNumber)
        val tvEstado: TextView = view.findViewById(R.id.tvEstado)
        val tvMonto: TextView = view.findViewById(R.id.tvMonto)
        val ivDelete: ImageView = view.findViewById(R.id.ivDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_orden_cliente, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val ctx = holder.itemView.context
        val fmt = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))

        holder.cardOrden.setOnClickListener { onItemClick(item) }
        if (item.id != -1L) {
            holder.ivDelete.visibility = View.VISIBLE
            holder.ivDelete.setOnClickListener { onDeleteClick(item) }
        } else {
            holder.ivDelete.visibility = View.GONE
        }

        val initial = item.cliente.firstOrNull()?.uppercaseChar()?.toString() ?: "O"
        holder.tvClientInitial.text = initial
        holder.tvClientName.text = item.cliente.ifEmpty { "Cliente" }
        holder.tvOrderNumber.text = item.numero

        val monto = item.monto.replace(",", "").toDoubleOrNull() ?: 0.0
        holder.tvMonto.text = "C$ ${fmt.format(monto)}"

        val estado = item.estado.lowercase()
        when {
            estado.contains("entreg") || estado.contains("complet") || estado.contains("pagado") -> {
                holder.tvEstado.text = "Completado"
                holder.tvEstado.setBackgroundColor(ContextCompat.getColor(ctx, R.color.status_active_bg))
                holder.tvEstado.setTextColor(ContextCompat.getColor(ctx, R.color.status_active_text))
            }
            estado.contains("proceso") || estado.contains("pendiente") || estado.contains("envi") -> {
                holder.tvEstado.text = "En proceso"
                holder.tvEstado.setBackgroundColor(ContextCompat.getColor(ctx, R.color.status_pending_bg))
                holder.tvEstado.setTextColor(ContextCompat.getColor(ctx, R.color.status_pending_text))
            }
            estado.contains("cancel") -> {
                holder.tvEstado.text = "Cancelado"
                holder.tvEstado.setBackgroundColor(ContextCompat.getColor(ctx, R.color.status_cancel_bg))
                holder.tvEstado.setTextColor(ContextCompat.getColor(ctx, R.color.status_cancel_text))
            }
            else -> {
                holder.tvEstado.text = item.estado.ifEmpty { "—" }
                holder.tvEstado.setBackgroundColor(ContextCompat.getColor(ctx, R.color.chip_bg_neutral))
                holder.tvEstado.setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary_new))
            }
        }
    }

    override fun getItemCount() = items.size
}