package com.unimarksa.gmv.adapters

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.unimarksa.gmv.R
import com.unimarksa.gmv.models.ItemReporteRecibo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReporteReciboAdapter(
    private val items: MutableList<ItemReporteRecibo>,
    private val onClick: (ItemReporteRecibo) -> Unit = {},
    private val onDelete: ((ItemReporteRecibo, Int) -> Unit)? = null,
    private val onAttach: ((ItemReporteRecibo) -> Unit)? = null
) : RecyclerView.Adapter<ReporteReciboAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardRecibo: MaterialCardView = view.findViewById(R.id.cardRecibo)
        val tvClientInitial: TextView = view.findViewById(R.id.tvClientInitial)
        val tvClientName: TextView = view.findViewById(R.id.tvClientName)
        val tvClientCode: TextView = view.findViewById(R.id.tvClientCode)
        val tvRuc: TextView = view.findViewById(R.id.tvRuc)
        val tvReceiptNumber: TextView = view.findViewById(R.id.tvReceiptNumber)
        val tvFooterAmount: TextView = view.findViewById(R.id.tvFooterAmount)
        val tvTimestamp: TextView = view.findViewById(R.id.tvTimestamp)
        val btnAttach: ImageButton = view.findViewById(R.id.btnAttach)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recibo, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        try {
            bind(holder, position)
        } catch (_: Exception) { }
    }

    private fun bind(holder: ViewHolder, position: Int) {
        val item = items[position]
        val ctx = holder.itemView.context
        val fmt = java.text.DecimalFormat("#,##0.00", java.text.DecimalFormatSymbols(Locale.US))

        val esAnulado = item.status == "4" || item.status.contains("Anul", ignoreCase = true)

        val name = item.nombreCliente.replace(Regex("^\\d+\\s*-\\s*"), "")
        val displayName = if (esAnulado) "ANULADO" else name.replace(Regex(" - RUC.*"), "").trim()
        val initial = if (esAnulado) "X" else (name.firstOrNull()?.uppercaseChar()?.toString() ?: "F")
        holder.tvClientInitial.text = initial
        holder.tvClientName.text = displayName
        holder.tvClientCode.text = item.codCliente.replace(Regex("[^\\d]"), "").ifEmpty { item.codCliente }
        val rucMatch = Regex("RUC[\\s\\-]*([\\dA-Z]+)").find(item.nombreCliente)
        holder.tvRuc.text = if (rucMatch != null) "RUC ${rucMatch.groupValues[1]}" else ""
        holder.tvReceiptNumber.text = "#${item.recibo}"

        val total = item.orderTotal.replace("C$", "").replace(",", "").trim().toDoubleOrNull() ?: 0.0
        holder.tvFooterAmount.text = "C$ ${fmt.format(total)}"

        if (esAnulado) {
            holder.cardRecibo.setCardBackgroundColor(android.graphics.Color.parseColor("#FFEBEE"))
            holder.cardRecibo.setStrokeColor(android.graphics.Color.parseColor("#EF9A9A"))
        } else {
            holder.cardRecibo.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.card_background))
            holder.cardRecibo.setStrokeColor(ContextCompat.getColor(ctx, R.color.card_stroke_new))
        }

        try {
            val dateStr = item.fecha.replace(" ", "T")
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            val date = sdf.parse(dateStr)
            if (date != null) {
                val now = Date()
                val diff = now.time - date.time
                val days = diff / (1000 * 60 * 60 * 24)
                holder.tvTimestamp.text = when {
                    days < 1 -> "hoy"
                    days < 2 -> "ayer"
                    days < 7 -> "hace $days días"
                    else -> DateFormat.format("dd/MM/yy", date).toString()
                }
            }
        } catch (_: Exception) {
            holder.tvTimestamp.text = item.fecha.take(10)
        }

        holder.btnDelete.setOnClickListener { onDelete?.invoke(item, position) }
        holder.btnAttach.setOnClickListener { onAttach?.invoke(item) }
        holder.cardRecibo.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size
}