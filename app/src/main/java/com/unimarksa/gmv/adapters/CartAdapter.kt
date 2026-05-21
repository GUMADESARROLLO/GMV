package com.unimarksa.gmv.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.unimarksa.gmv.R
import com.unimarksa.gmv.models.ItemCarrito
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class CartAdapter(
    val items: MutableList<ItemCarrito>,
    private val onQuantityChanged: (ItemCarrito, Double) -> Unit,
    private val onDelete: (ItemCarrito) -> Unit
) : RecyclerView.Adapter<CartAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivImage: ImageView = view.findViewById(R.id.iv_product_image)
        val tvName: TextView = view.findViewById(R.id.tv_product_name)
        val tvSku: TextView = view.findViewById(R.id.tv_product_sku)
        val tvBonificado: TextView = view.findViewById(R.id.tv_cart_bonificado)
        val tvQtyInfo: TextView = view.findViewById(R.id.tv_qty_info)
        val tvSubtotal: TextView = view.findViewById(R.id.tv_subtotal)
        val tvQuantity: TextView = view.findViewById(R.id.tv_quantity)
        val btnDecrease: ImageButton = view.findViewById(R.id.btn_decrease)
        val btnIncrease: ImageButton = view.findViewById(R.id.btn_increase)
        val btnDelete: ImageButton = view.findViewById(R.id.btn_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val ctx = holder.itemView.context
        val fmt = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
        val qtyFmt = if (item.cantidad == item.cantidad.toLong().toDouble())
            item.cantidad.toLong().toString() else item.cantidad.toString()

        holder.tvName.text = item.producto.nombre
        holder.tvSku.text = "SKU: ${item.producto.codArticulo}"

        if (item.selectedBonificado != null) {
            holder.tvBonificado.text = "| ${item.selectedBonificado}"
            holder.tvBonificado.visibility = android.view.View.VISIBLE
        } else {
            holder.tvBonificado.visibility = android.view.View.GONE
        }

        val unitPrice = item.producto.precio.replace(",", "").toDoubleOrNull() ?: 0.0
        holder.tvSubtotal.text = "C$ ${fmt.format(item.subtotal)}"
        holder.tvQtyInfo.text = "C$ ${fmt.format(unitPrice)} x $qtyFmt"

        val imgUrl = item.producto.imagenUrl
        if (!imgUrl.isNullOrBlank()) {
            Picasso.get()
                .load(imgUrl)
                .placeholder(R.drawable.ic_placeholder_product)
                .resize(200, 200)
                .centerCrop()
                .into(holder.ivImage)
        } else {
            holder.ivImage.setImageResource(R.drawable.ic_placeholder_product)
        }

        holder.btnDecrease.isEnabled = item.cantidad > 1.0
        holder.btnDecrease.alpha = if (item.cantidad > 1.0) 1f else 0.4f

        holder.btnDecrease.setOnClickListener {
            val newQty = item.cantidad - 1.0
            if (newQty >= 1.0) {
                animateQuantityChange(holder.tvQuantity, newQty)
                onQuantityChanged(item, newQty)
            }
        }

        holder.btnIncrease.setOnClickListener {
            val newQty = item.cantidad + 1.0
            val stock = item.producto.cantidad.replace(",", "").toDoubleOrNull() ?: 999999.0
            if (newQty <= stock) {
                animateQuantityChange(holder.tvQuantity, newQty)
                onQuantityChanged(item, newQty)
            } else {
                holder.tvQuantity.alpha = 0.5f
                holder.tvQuantity.postDelayed({ holder.tvQuantity.alpha = 1f }, 500)
            }
        }

        holder.btnDelete.setOnClickListener {
            onDelete(item)
        }
    }

    override fun getItemCount() = items.size

    private fun animateQuantityChange(view: TextView, newQty: Double) {
        val txt = if (newQty == newQty.toLong().toDouble()) newQty.toLong().toString() else newQty.toString()
        view.text = txt
        view.scaleX = 0.8f
        view.scaleY = 0.8f
        view.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(200)
            .start()
    }
}
