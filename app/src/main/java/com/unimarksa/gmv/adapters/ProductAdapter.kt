package com.unimarksa.gmv.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.squareup.picasso.Picasso
import com.unimarksa.gmv.R
import com.unimarksa.gmv.models.ItemProducto

class ProductAdapter : ListAdapter<ItemProducto, ProductAdapter.ViewHolder>(ProductDiffCallback()) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val lytParent: MaterialCardView = view.findViewById(R.id.lyt_parent)
        val ivProductImage: ImageView = view.findViewById(R.id.iv_product_image)
        val tvNombre: TextView = view.findViewById(R.id.tv_nombre)
        val tvSku: TextView = view.findViewById(R.id.tv_sku)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)

        Picasso.get()
            .load(item.imagenUrl)
            .placeholder(R.drawable.img_empty_history)
            .resize(300, 200)
            .centerCrop()
            .into(holder.ivProductImage)

        holder.tvNombre.text = item.nombre
        holder.tvSku.text = "SKU: ${item.codArticulo}"
    }

    class ProductDiffCallback : DiffUtil.ItemCallback<ItemProducto>() {
        override fun areItemsTheSame(oldItem: ItemProducto, newItem: ItemProducto) = oldItem.codArticulo == newItem.codArticulo
        override fun areContentsTheSame(oldItem: ItemProducto, newItem: ItemProducto) = oldItem == newItem
    }
}
