package com.unimarksa.gmv.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.unimarksa.gmv.R
import com.unimarksa.gmv.models.ItemProducto

class ProductosAdapter(
    private val items: List<ItemProducto>,
    private val onClick: (ItemProducto) -> Unit
) : RecyclerView.Adapter<ProductosAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: CardView = view as CardView
        val productName: TextView = view.findViewById(R.id.product_name)
        val idSku: TextView = view.findViewById(R.id.id_sku)
        val idCodArticulo: TextView = view.findViewById(R.id.id_sku)
        val idCantItem: TextView = view.findViewById(R.id.id_Cant_item)
        val productPrice: TextView = view.findViewById(R.id.product_price)
        val categoryImage: ImageView = view.findViewById(R.id.category_image)
        val txtOffer: TextView = view.findViewById(R.id.txt_offer)
        val iconStatus: ImageView = view.findViewById(R.id.icon_status)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_productos, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.productName.text = item.nombre
        holder.idSku.text = "SKU. "
        holder.idCodArticulo.text = item.codArticulo
        holder.idCantItem.text = item.cantidad
        holder.productPrice.text = item.precio

        if (item.oferta != null) {
            holder.txtOffer.text = item.oferta
            holder.txtOffer.visibility = View.VISIBLE
        } else {
            holder.txtOffer.visibility = View.GONE
        }

        if (item.imagenResource != null) {
            holder.categoryImage.setImageResource(item.imagenResource)
        }

        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size
}
