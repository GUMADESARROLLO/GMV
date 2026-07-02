package com.unimarksa.gmv.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.unimarksa.gmv.R
import com.unimarksa.gmv.models.ItemFacturaPendiente

class FacturasAdapter(
    private val items: List<ItemFacturaPendiente>,
    private val onClick: (ItemFacturaPendiente) -> Unit
) : RecyclerView.Adapter<FacturasAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNumero: TextView = view.findViewById(R.id.tv_factura_numero)
        val tvFacturado: TextView = view.findViewById(R.id.tv_factura_facturado)
        val tvSaldo: TextView = view.findViewById(R.id.tv_factura_saldo)
        val tvVencimiento: TextView = view.findViewById(R.id.tv_factura_vencimiento)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_factura_pendiente, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvNumero.text = item.numero
        val facturado = item.saldoActual.replace(",", "").trim().toDoubleOrNull() ?: 0.0
        val saldo = item.saldo.replace(",", "").trim().toDoubleOrNull() ?: 0.0
        holder.tvFacturado.text = "C$ ${"%,.2f".format(facturado)}"
        holder.tvSaldo.text = "Pte: C$ ${"%,.2f".format(saldo)}"
        holder.tvVencimiento.text = "Vence: ${item.vencimiento}"
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size
}
