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
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class ProductosAdapter(
    private val items: MutableList<ItemProducto>,
    private val originalItems: List<ItemProducto> = items.toList()
) : RecyclerView.Adapter<ProductosAdapter.ViewHolder>() {

    fun filter(query: String) {
        val q = query.trim().lowercase(Locale.ROOT)
        items.clear()
        if (q.isEmpty()) {
            items.addAll(originalItems)
        } else {
            items.addAll(originalItems.filter {
                it.nombre.lowercase(Locale.ROOT).contains(q) ||
                it.codArticulo.lowercase(Locale.ROOT).contains(q)
            })
        }
        notifyDataSetChanged()
    }

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
        val cantidadFormateada = formatNumero(item.cantidad)
        holder.idCantItem.text = if (!item.und.isNullOrBlank()) "$cantidadFormateada [${item.und}]" else cantidadFormateada
        holder.productPrice.text = "C$ ${formatNumero(item.precio)}"

        val partes = if (!item.bonificado.isNullOrBlank()) {
            item.bonificado!!.split(",").map { it.trim() }.filter { it.isNotEmpty() && it != "0" && it != "0.00" }
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

    private fun formatNumero(valor: String): String {
        return try {
            val num = valor.replace(",", "").toDouble()
            DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US)).format(num)
        } catch (_: NumberFormatException) {
            valor
        }
    }
}
