package com.unimarksa.gmv.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.unimarksa.gmv.R
import com.unimarksa.gmv.models.Doctor
import com.unimarksa.gmv.models.DoctorStatus

class DoctorAdapter(
    private val onClick: (Doctor) -> Unit
) : ListAdapter<Doctor, DoctorAdapter.ViewHolder>(DoctorDiffCallback()) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val lytParent: MaterialCardView = view.findViewById(R.id.lyt_parent)
        val tvAvatar: TextView = view.findViewById(R.id.tv_avatar)
        val tvNombre: TextView = view.findViewById(R.id.tv_nombre)
        val tvEspecialidad: TextView = view.findViewById(R.id.tv_especialidad)
        val tvTelefono: TextView = view.findViewById(R.id.tv_telefono)
        val tvEstado: TextView = view.findViewById(R.id.tv_estado)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_doctor_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val ctx = holder.itemView.context

        val initials = item.nombre.replace(Regex("^(Dr|Dra)\\.?\\s*"), "")
            .split(" ")
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }

        holder.tvAvatar.text = initials
        holder.tvAvatar.background.setTint(getAvatarColor(item.nombre))
        holder.tvNombre.text = item.nombre
        holder.tvEspecialidad.text = item.especialidad
        holder.tvTelefono.text = item.telefono

        holder.tvEstado.text = item.estado.label
        holder.tvEstado.background = ContextCompat.getDrawable(ctx, when (item.estado) {
            DoctorStatus.ACTIVO -> R.drawable.bg_badge_solid_green
            DoctorStatus.PENDIENTE_ALTA -> R.drawable.bg_badge_solid_amber
            DoctorStatus.PENDIENTE_BAJA -> R.drawable.bg_badge_solid_red
        })

        holder.lytParent.setOnClickListener { onClick(item) }
    }

    private fun getAvatarColor(name: String): Int {
        val colors = listOf(
            Color.parseColor("#2B3B9E"),
            Color.parseColor("#22C55E"),
            Color.parseColor("#C42B3A"),
            Color.parseColor("#F59E0B"),
            Color.parseColor("#1B2766"),
            Color.parseColor("#29B6E8")
        )
        return colors[(name.hashCode() and Int.MAX_VALUE) % colors.size]
    }

    class DoctorDiffCallback : DiffUtil.ItemCallback<Doctor>() {
        override fun areItemsTheSame(oldItem: Doctor, newItem: Doctor) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Doctor, newItem: Doctor) = oldItem == newItem
    }
}
