package com.unimarksa.gmv.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.unimarksa.gmv.R
import com.unimarksa.gmv.models.ItemPedidoCliente
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class OrdenesAdapter(
    private val items: List<ItemPedidoCliente>
) : RecyclerView.Adapter<OrdenesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNumero: TextView = view.findViewById(R.id.tv_lote)
        val tvEstado: TextView = view.findViewById(R.id.tv_estado)
        val tvArticulos: TextView = view.findViewById(R.id.tv_articulos)
        val tvMonto: TextView = view.findViewById(R.id.tv_monto)
        val tvFecha: TextView = view.findViewById(R.id.tv_fecha)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_orden_cliente, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val fmt = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
        val ctx = holder.itemView.context

        holder.tvNumero.text = "PED-${item.numero}"
        holder.tvArticulos.text = item.articulos
        holder.tvMonto.text = "C$ ${fmt.format(item.monto.replace(",", "").toDoubleOrNull() ?: 0.0)}"
        holder.tvFecha.text = item.fecha.ifEmpty { "--" }

        val estado = item.estado.lowercase()
        when {
            estado.contains("entreg") || estado.contains("complet") || estado.contains("pagado") -> {
                holder.tvEstado.text = "Completado"
                holder.tvEstado.setBackgroundResource(R.drawable.bg_badge_green)
                holder.tvEstado.setTextColor(ContextCompat.getColor(ctx, R.color.green_500))
            }
            estado.contains("proceso") || estado.contains("pendiente") || estado.contains("envi") -> {
                holder.tvEstado.text = "En proceso"
                holder.tvEstado.setBackgroundResource(R.drawable.bg_badge_amber)
                holder.tvEstado.setTextColor(ContextCompat.getColor(ctx, R.color.orange_500))
            }
            estado.contains("cancel") -> {
                holder.tvEstado.text = "Cancelado"
                holder.tvEstado.setBackgroundResource(R.drawable.bg_badge_red)
                holder.tvEstado.setTextColor(ContextCompat.getColor(ctx, R.color.red_400))
            }
            else -> {
                holder.tvEstado.text = item.estado.ifEmpty { "—" }
            }
        }
    }

    override fun getItemCount() = items.size
}
