package com.unimarksa.gmv.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.unimarksa.gmv.R
import com.unimarksa.gmv.databinding.ItemLoteHistorialBinding
import com.unimarksa.gmv.models.ItemLoteHistorial

class HistorialLotesAdapter(
    private val items: MutableList<ItemLoteHistorial>
) : RecyclerView.Adapter<HistorialLotesAdapter.ViewHolder>() {

    fun update(newItems: List<ItemLoteHistorial>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: ItemLoteHistorialBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLoteHistorialBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.tvLote.text = "Lote. ${item.lote}"
        holder.binding.tvFactura.text = "Fact. ${item.factura}"
        holder.binding.tvDescripcion.text = item.descripcion.ifEmpty { "—" }
        holder.binding.tvFecha.text = "Fecha Fact. ${item.fecha}"
        holder.binding.tvArticulo.text = item.articulo
    }

    override fun getItemCount() = items.size

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = items[position].hashCode().toLong()
}
