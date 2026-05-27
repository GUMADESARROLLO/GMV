package com.unimarksa.gmv.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.unimarksa.gmv.R
import com.unimarksa.gmv.models.ItemReporteFactura

class ReporteFacturaAdapter(
    private val items: List<ItemReporteFactura>,
    private val onClick: (ItemReporteFactura) -> Unit = {}
) : RecyclerView.Adapter<ReporteFacturaAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardFactura: MaterialCardView = view.findViewById(R.id.cardFactura)
        val tvClientInitial: TextView = view.findViewById(R.id.tvClientInitial)
        val tvClientName: TextView = view.findViewById(R.id.tvClientName)
        val tvClientCode: TextView = view.findViewById(R.id.tvClientCode)
        val tvFacturaNumero: TextView = view.findViewById(R.id.tvFacturaNumero)
        val tvFacturaTotal: TextView = view.findViewById(R.id.tvFacturaTotal)
        val tvVencimiento: TextView = view.findViewById(R.id.tvVencimiento)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reporte_factura, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val total = item.total.replace(",", "").trim().toDoubleOrNull() ?: 0.0
        val saldo = item.saldo.replace(",", "").trim().toDoubleOrNull() ?: 0.0

        val initial = item.cliente.firstOrNull()?.uppercaseChar()?.toString() ?: "C"
        holder.tvClientInitial.text = initial
        holder.tvClientName.text = item.cliente.ifEmpty { "Cliente" }
        holder.tvClientCode.text = item.numero
        holder.tvFacturaNumero.text = item.numero
        holder.tvFacturaTotal.text = "C$ ${"%,.2f".format(total)}"
        holder.tvVencimiento.text = item.vencimiento.ifEmpty { item.fecha }

        holder.cardFactura.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size
}
