package com.unimarksa.gmv.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.unimarksa.gmv.R
import com.unimarksa.gmv.models.ItemReporteFactura

class FacturaHistoricoAdapter(
    private val items: List<ItemReporteFactura>,
    private val onClick: (ItemReporteFactura) -> Unit = {}
) : RecyclerView.Adapter<FacturaHistoricoAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNumero: TextView = view.findViewById(R.id.tv_factura_numero)
        val tvMonto: TextView = view.findViewById(R.id.tv_factura_monto)
        val tvFecha: TextView = view.findViewById(R.id.tv_factura_fecha)
        val tvBadge: TextView = view.findViewById(R.id.tv_estado_badge)
        val ivIcon: ImageView = view.findViewById(R.id.iv_status_icon)
        val lytIcon: View = view.findViewById(R.id.lyt_icon_status)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_factura, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val ctx = holder.itemView.context

        holder.tvNumero.text = item.numero
        holder.tvFecha.text = item.fecha

        val monto = item.total.replace(",", "").trim().toDoubleOrNull() ?: 0.0
        holder.tvMonto.text = "C$ ${"%,.2f".format(monto)}"

        val saldo = item.saldo.replace(",", "").trim().toDoubleOrNull() ?: 0.0
        val esPagada = saldo <= 0.0

        if (esPagada) {
            holder.tvBadge.text = "Pagada"
            holder.tvBadge.setTextColor(ContextCompat.getColor(ctx, R.color.white))
            holder.tvBadge.setBackgroundResource(R.drawable.bg_badge_green)
            holder.ivIcon.setImageResource(R.drawable.ic_check)
            holder.ivIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.mora_ok))
            holder.lytIcon.setBackgroundResource(R.drawable.bg_icon_circle_green)
        } else {
            holder.tvBadge.text = "Pendiente"
            holder.tvBadge.setTextColor(ContextCompat.getColor(ctx, R.color.mora_warn))
            holder.tvBadge.setBackgroundResource(R.drawable.bg_badge_amber)
            holder.ivIcon.setImageResource(R.drawable.ic_schedule)
            holder.ivIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.mora_warn))
            holder.lytIcon.setBackgroundResource(R.drawable.bg_icon_circle_amber)
        }

        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size
}
