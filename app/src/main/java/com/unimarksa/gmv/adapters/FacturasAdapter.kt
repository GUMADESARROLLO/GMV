package com.unimarksa.gmv.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.unimarksa.gmv.R
import com.unimarksa.gmv.models.ItemFacturaPendiente

class FacturasAdapter(
    private val items: List<ItemFacturaPendiente>
) : RecyclerView.Adapter<FacturasAdapter.ViewHolder>() {

    var onItemClick: (ItemFacturaPendiente) -> Unit = {}

    private val selectedPositions = mutableSetOf<Int>()
    var onSelectionChanged: ((count: Int, total: Double) -> Unit)? = null

    val selectedItems: List<ItemFacturaPendiente>
        get() = selectedPositions.map { items[it] }

    val selectedCount: Int get() = selectedPositions.size

    val selectedTotal: Double
        get() = selectedPositions.sumOf {
            val saldo = items[it].saldo.replace(",", "").trim().toDoubleOrNull() ?: 0.0
            saldo
        }

    fun clearSelection() {
        val copy = selectedPositions.toSet()
        selectedPositions.clear()
        copy.forEach { notifyItemChanged(it) }
        onSelectionChanged?.invoke(0, 0.0)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val lytIconStatus: View = view.findViewById(R.id.lyt_icon_status)
        val ivStatusIcon: ImageView = view.findViewById(R.id.iv_status_icon)
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
        val isSelected = position in selectedPositions
        val ctx = holder.itemView.context

        holder.tvNumero.text = item.numero
        val facturado = item.saldoActual.replace(",", "").trim().toDoubleOrNull() ?: 0.0
        val saldo = item.saldo.replace(",", "").trim().toDoubleOrNull() ?: 0.0
        holder.tvFacturado.text = "C$ ${"%,.2f".format(facturado)}"
        holder.tvSaldo.text = "Pte: C$ ${"%,.2f".format(saldo)}"
        holder.tvVencimiento.text = "Vence: ${item.vencimiento}"

        if (isSelected) {
            holder.lytIconStatus.setBackgroundResource(R.drawable.bg_icon_circle_green)
            holder.ivStatusIcon.setImageResource(R.drawable.ic_check)
            holder.ivStatusIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.white))
            holder.itemView.setBackgroundColor(ContextCompat.getColor(ctx, R.color.accent_green_light))
        } else {
            holder.lytIconStatus.setBackgroundResource(R.drawable.bg_icon_circle_blue)
            holder.ivStatusIcon.setImageResource(R.drawable.ic_receipt_long)
            holder.ivStatusIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.accent_primary))
            holder.itemView.setBackgroundColor(ContextCompat.getColor(ctx, android.R.color.white))
        }

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }

        holder.itemView.setOnLongClickListener {
            toggleSelection(position)
            true
        }
    }

    private fun toggleSelection(position: Int) {
        if (position in selectedPositions) {
            selectedPositions.remove(position)
        } else {
            selectedPositions.add(position)
        }
        notifyItemChanged(position)
        onSelectionChanged?.invoke(selectedCount, selectedTotal)
    }

    override fun getItemCount() = items.size
}
