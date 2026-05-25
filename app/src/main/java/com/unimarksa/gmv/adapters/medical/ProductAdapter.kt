package com.unimarksa.gmv.adapters.medical

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.unimarksa.gmv.R
import com.unimarksa.gmv.data.model.Product

class ProductAdapter(
    private val onVerDetalle: (Product) -> Unit
) : ListAdapter<Product, ProductAdapter.ViewHolder>(ProductDiffCallback()) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val lytParent: MaterialCardView = view.findViewById(R.id.lyt_parent)
        val viewColorBar: View = view.findViewById(R.id.view_color_bar)
        val ivIcon: ImageView = view.findViewById(R.id.iv_icon)
        val tvNombre: TextView = view.findViewById(R.id.tv_nombre)
        val tvMolecula: TextView = view.findViewById(R.id.tv_molecula)
        val chipCategoria: Chip = view.findViewById(R.id.chip_categoria)
        val btnVerDetalle: Button = view.findViewById(R.id.btn_ver_detalle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)

        val categoryColor = getCategoryColor(item.categoria)
        (holder.viewColorBar.background as? GradientDrawable)?.setColor(categoryColor)
        holder.ivIcon.setColorFilter(categoryColor)

        holder.tvNombre.text = item.nombre
        holder.tvMolecula.text = item.molecula
        holder.chipCategoria.text = item.categoria
        holder.chipCategoria.chipBackgroundColor = ColorStateList.valueOf(lightenColor(categoryColor))

        holder.btnVerDetalle.setOnClickListener { onVerDetalle(item) }
    }

    private fun getCategoryColor(categoria: String): Int {
        return when (categoria.lowercase()) {
            "cardiovascular" -> Color.parseColor("#EF4444")
            "neurológico", "neurologico" -> Color.parseColor("#8B5CF6")
            "pediátrico", "pediatrico" -> Color.parseColor("#22C55E")
            "oncológico", "oncologico" -> Color.parseColor("#F59E0B")
            "dermatológico", "dermatologico" -> Color.parseColor("#EC4899")
            "gastrointestinal" -> Color.parseColor("#06B6D4")
            "endocrinológico", "endocrinologico" -> Color.parseColor("#3B82F6")
            else -> Color.parseColor("#4361EE")
        }
    }

    private fun lightenColor(color: Int): Int {
        val alpha = 0xFF
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        return (alpha shl 24) or (r shl 16) or (g shl 8) or b
    }

    class ProductDiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Product, newItem: Product) = oldItem == newItem
    }
}
