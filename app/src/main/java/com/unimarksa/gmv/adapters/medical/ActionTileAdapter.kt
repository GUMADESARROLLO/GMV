package com.unimarksa.gmv.adapters.medical

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.unimarksa.gmv.R

data class ActionTile(
    val iconRes: Int,
    val label: String,
    val onClick: () -> Unit
)

class ActionTileAdapter(
    private val tiles: List<ActionTile>
) : RecyclerView.Adapter<ActionTileAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val lytParent: MaterialCardView = view.findViewById(R.id.lyt_parent)
        val ivIcon: ImageView = view.findViewById(R.id.iv_icon)
        val tvLabel: TextView = view.findViewById(R.id.tv_label)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_action_tile, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val tile = tiles[position]
        holder.tvLabel.text = tile.label
        holder.ivIcon.setImageResource(tile.iconRes)
        holder.lytParent.setOnClickListener { tile.onClick() }
    }

    override fun getItemCount() = tiles.size
}
