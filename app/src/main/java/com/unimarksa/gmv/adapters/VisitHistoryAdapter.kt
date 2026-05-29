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
import com.unimarksa.gmv.models.MedicalVisit
import com.unimarksa.gmv.models.VisitResult

class VisitHistoryAdapter : ListAdapter<MedicalVisit, VisitHistoryAdapter.ViewHolder>(VisitDiffCallback()) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val lytParent: MaterialCardView = view.findViewById(R.id.lyt_parent)
        val viewLine: View = view.findViewById(R.id.view_line)
        val tvFecha: TextView = view.findViewById(R.id.tv_fecha)
        val tvEfectividad: TextView = view.findViewById(R.id.tv_efectividad)
        val tvDoctor: TextView = view.findViewById(R.id.tv_doctor)
        val tvEspecialidad: TextView = view.findViewById(R.id.tv_especialidad)
        val tvDetalle: TextView = view.findViewById(R.id.tv_detalle)
        val tvObservaciones: TextView = view.findViewById(R.id.tv_observaciones)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_visit_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val ctx = holder.itemView.context

        holder.tvFecha.text = item.fecha
        holder.tvDoctor.text = item.doctorNombre
        holder.tvEspecialidad.text = item.doctorEspecialidad

        if (item.efectiva) {
            holder.viewLine.setBackgroundColor(ContextCompat.getColor(ctx, R.color.accent_green))
            holder.tvEfectividad.text = "EFECTIVA"
            holder.tvEfectividad.background = ContextCompat.getDrawable(ctx, R.drawable.bg_badge_solid_green)

            val result = item.result as? VisitResult.Effective
            if (result != null && result.moleculas.isNotEmpty()) {
                holder.tvDetalle.text = result.moleculas.joinToString(", ")
                holder.tvDetalle.visibility = View.VISIBLE
            } else {
                holder.tvDetalle.visibility = View.GONE
            }

            if (!result?.observaciones.isNullOrEmpty()) {
                holder.tvObservaciones.text = result?.observaciones
                holder.tvObservaciones.visibility = View.VISIBLE
            } else {
                holder.tvObservaciones.visibility = View.GONE
            }
        } else {
            holder.viewLine.setBackgroundColor(ContextCompat.getColor(ctx, R.color.accent_red))
            holder.tvEfectividad.text = "NO EFECTIVA"
            holder.tvEfectividad.background = ContextCompat.getDrawable(ctx, R.drawable.bg_badge_solid_red)

            val result = item.result as? VisitResult.NotEffective
            if (result != null) {
                val motivo = if (result.motivo == "Otro" && result.otro.isNotEmpty()) result.otro else result.motivo
                holder.tvDetalle.text = "Motivo: $motivo"
                holder.tvDetalle.visibility = View.VISIBLE
            } else {
                holder.tvDetalle.visibility = View.GONE
            }
            holder.tvObservaciones.visibility = View.GONE
        }
    }

    class VisitDiffCallback : DiffUtil.ItemCallback<MedicalVisit>() {
        override fun areItemsTheSame(oldItem: MedicalVisit, newItem: MedicalVisit) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: MedicalVisit, newItem: MedicalVisit) = oldItem == newItem
    }
}
