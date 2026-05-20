package com.unimarksa.gmv.adapters

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import com.unimarksa.gmv.R
import com.unimarksa.gmv.models.ItemLista

class ListaAdapter(
    private val items: List<ItemLista>,
    private val onClick: (ItemLista) -> Unit
) : RecyclerView.Adapter<ListaAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatar: View = view.findViewById(R.id.iv_avatar)
        val nombre: MaterialTextView = view.findViewById(R.id.tv_nombre)
        val subtitulo: MaterialTextView = view.findViewById(R.id.tv_subtitulo)
        val hora: MaterialTextView = view.findViewById(R.id.tv_hora)
        val badge: MaterialTextView = view.findViewById(R.id.badge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lista, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.nombre.text = item.nombre
        holder.subtitulo.text = item.subtitulo
        holder.hora.text = item.hora

        if (item.badge > 0) {
            holder.badge.text = item.badge.toString()
            holder.badge.visibility = View.VISIBLE
            holder.badge.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(ContextCompat.getColor(holder.itemView.context, R.color.badge_red))
            }
        } else {
            holder.badge.visibility = View.GONE
        }

        holder.avatar.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(item.avatarBgColor.takeIf { it != 0 } ?: Color.parseColor("#BBDEFB"))
        }

        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size
}
