package com.unimarksa.gmv.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.unimarksa.gmv.R

class AdapterSuggestionSearch(
    private val items: MutableList<String>,
    private val onItemClick: (String) -> Unit,
    private val onItemDelete: (String) -> Unit
) : RecyclerView.Adapter<AdapterSuggestionSearch.ViewHolder>() {

    fun update(newItems: List<String>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(R.id.tv_suggestion_text)
        val btnDelete: ImageButton = view.findViewById(R.id.btn_delete_suggestion)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_suggestion_search, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.text.text = item
        holder.itemView.setOnClickListener { onItemClick(item) }
        holder.btnDelete.setOnClickListener { onItemDelete(item) }
    }

    override fun getItemCount() = items.size
}
