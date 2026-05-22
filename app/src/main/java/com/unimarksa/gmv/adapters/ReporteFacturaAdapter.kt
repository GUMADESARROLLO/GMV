package com.unimarksa.gmv.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.unimarksa.gmv.R
import com.unimarksa.gmv.models.ItemReporteFactura

class ReporteFacturaAdapter(
    private val items: List<ItemReporteFactura>,
    private val onClick: (ItemReporteFactura) -> Unit = {}
) : RecyclerView.Adapter<ReporteFacturaAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNumero: TextView = view.findViewById(R.id.tv_factura_numero)
        val tvCliente: TextView = view.findViewById(R.id.tv_factura_cliente)
        val tvFecha: TextView = view.findViewById(R.id.tv_factura_fecha)
        val tvTotal: TextView = view.findViewById(R.id.tv_factura_total)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reporte_factura, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvNumero.text = item.numero
        holder.tvCliente.text = item.cliente
        holder.tvFecha.text = item.fecha
        val total = item.total.replace(",", "").trim().toDoubleOrNull() ?: 0.0
        holder.tvTotal.text = "C$ ${"%,.2f".format(total)}"
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size
}
