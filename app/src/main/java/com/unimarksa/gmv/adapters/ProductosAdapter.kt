package com.unimarksa.gmv.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.unimarksa.gmv.R
import com.unimarksa.gmv.models.ItemProducto

class ProductosAdapter(
    private val items: List<ItemProducto>,
    private val onClick: (ItemProducto) -> Unit
) : RecyclerView.Adapter<ProductosAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: CardView = view as CardView
        val productName: TextView = view.findViewById(R.id.product_name)
        val idSku: TextView = view.findViewById(R.id.id_cod_articulo)
        val idCantItem: TextView = view.findViewById(R.id.id_Cant_item)
        val productPrice: TextView = view.findViewById(R.id.product_price)
        val categoryImage: ImageView = view.findViewById(R.id.category_image)

        val lytTallas: LinearLayout = view.findViewById(R.id.id_lyt_tallas)
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
        holder.productPrice.text = item.precio


        holder.lytTallas.removeAllViews()
        if (!item.bonificado.isNullOrBlank()) {
            val partes = item.bonificado!!.split(":")
            for (p in partes) {
                val trimmed = p.trim()
                if (trimmed.isNotEmpty()) {
                    val chip = TextView(holder.itemView.context).apply {
                        text = trimmed
                        textSize = 10f
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                        setTextColor(ContextCompat.getColor(context, R.color.grey_80))
                        setBackgroundResource(R.drawable.bg_size_chip)
                        gravity = android.view.Gravity.CENTER
                        minWidth = resources.getDimensionPixelSize(R.dimen.spacing_large)
                        setPadding(8, 0, 8, 0)
                        val lp = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            resources.getDimensionPixelSize(R.dimen.spacing_large)
                        )
                        lp.marginEnd = resources.getDimensionPixelSize(R.dimen.spacing_small)
                        layoutParams = lp
                    }
                    holder.lytTallas.addView(chip)
                }
            }
        }

        Picasso.get()
            .load(item.imagenUrl)
            .placeholder(R.drawable.img_empty_history)
            .resize(250, 250)
            .centerCrop()
            .into(holder.categoryImage)

        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size
}
