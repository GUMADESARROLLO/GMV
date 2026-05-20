package com.unimarksa.gmv.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.unimarksa.gmv.R
import com.unimarksa.gmv.activities.ProductDetailActivity
import com.unimarksa.gmv.models.ItemProducto

class ProductosAdapter(
    private val items: List<ItemProducto>
) : RecyclerView.Adapter<ProductosAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: CardView = view as CardView
        val productName: TextView = view.findViewById(R.id.product_name)
        val idSku: TextView = view.findViewById(R.id.id_cod_articulo)
        val idCantItem: TextView = view.findViewById(R.id.id_Cant_item)
        val productPrice: TextView = view.findViewById(R.id.product_price)
        val categoryImage: ImageView = view.findViewById(R.id.category_image)
        val rvBonificados: RecyclerView = view.findViewById(R.id.rv_bonificados)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_productos, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.productName.text = item.nombre
        holder.idSku.text = item.codArticulo
        holder.idCantItem.text = item.cantidad
        holder.productPrice.text = "C$ ${item.precio}"

        val partes = if (!item.bonificado.isNullOrBlank()) {
            item.bonificado!!.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        } else emptyList()

        if (partes.isNotEmpty()) {
            val ctx = holder.itemView.context
            holder.rvBonificados.layoutManager = GridLayoutManager(ctx, 6)
            holder.rvBonificados.adapter = BonificadosAdapter(partes)
            holder.rvBonificados.visibility = View.VISIBLE
        } else {
            holder.rvBonificados.visibility = View.GONE
        }

        Picasso.get()
            .load(item.imagenUrl)
            .placeholder(R.drawable.img_empty_history)
            .resize(250, 250)
            .centerCrop()
            .into(holder.categoryImage)

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, ProductDetailActivity::class.java)
            intent.putExtra("producto", item)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount() = items.size
}
