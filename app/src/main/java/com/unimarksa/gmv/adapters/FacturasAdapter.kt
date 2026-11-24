package com.unimarksa.gmv.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.unimarksa.gmv.R
import com.unimarksa.gmv.models.ItemFacturaPendiente
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class FacturasAdapter(
    private val items: List<ItemFacturaPendiente>
) : RecyclerView.Adapter<FacturasAdapter.ViewHolder>() {

    var onItemClick: (ItemFacturaPendiente) -> Unit = {}

    private val selectedPositions = mutableSetOf<Int>()
    var onSelectionChanged: ((count: Int, total: Double) -> Unit)? = null

    val selectedItems: List<ItemFacturaPendiente>
        get() = selectedPositions.map { items[it] }

    val selectedCount: Int get() = selectedPositions.size

    val selectedTotal: Double
        get() = selectedPositions.sumOf {
            val saldo = items[it].saldo.replace(",", "").trim().toDoubleOrNull() ?: 0.0
            saldo
        }

    fun clearSelection() {
        val copy = selectedPositions.toSet()
        selectedPositions.clear()
        copy.forEach { notifyItemChanged(it) }
        onSelectionChanged?.invoke(0, 0.0)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val lytIconStatus: View = view.findViewById(R.id.lyt_icon_status)
        val ivStatusIcon: ImageView = view.findViewById(R.id.iv_status_icon)
        val tvNumero: TextView = view.findViewById(R.id.tv_factura_numero)
        val tvFacturado: TextView = view.findViewById(R.id.tv_factura_facturado)
        val tvSaldo: TextView = view.findViewById(R.id.tv_factura_saldo)
        val tvVencimiento: TextView = view.findViewById(R.id.tv_factura_vencimiento)
        val tvEstado: TextView = view.findViewById(R.id.tv_factura_estado)
    }

    private fun diasParaVencer(vencimiento: String): Long? {
        val fecha = runCatching {
            SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).apply { isLenient = false }.parse(vencimiento.trim())
        }.getOrNull() ?: return null
        val hoy = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return TimeUnit.MILLISECONDS.toDays(fecha.time - hoy)
    }

    private fun bindEstado(tv: TextView, vencimiento: String) {
        val ctx = tv.context
        val dias = diasParaVencer(vencimiento)
        if (dias == null) {
            tv.visibility = View.GONE
            return
        }
        val (texto, bg, fg) = when {
            dias < 0 -> Triple("Vencida", R.color.status_cancel_bg, R.color.status_cancel_text)
            dias == 0L -> Triple("Vence hoy", R.color.status_pending_bg, R.color.status_pending_text)
            dias <= 7 -> Triple("Vence en $dias días", R.color.status_pending_bg, R.color.status_pending_text)
            else -> Triple("Al día", R.color.status_active_bg, R.color.status_active_text)
        }
        tv.visibility = View.VISIBLE
        tv.text = texto
        tv.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, bg))
        tv.setTextColor(ContextCompat.getColor(ctx, fg))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_factura_pendiente, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val isSelected = position in selectedPositions
        val ctx = holder.itemView.context

        holder.tvNumero.text = item.numero
        val facturado = item.saldoActual.replace(",", "").trim().toDoubleOrNull() ?: 0.0
        val saldo = item.saldo.replace(",", "").trim().toDoubleOrNull() ?: 0.0
        holder.tvSaldo.text = "C$ ${"%,.2f".format(saldo)}"
        holder.tvFacturado.text = "Total C$ ${"%,.2f".format(facturado)}"
        holder.tvVencimiento.text = item.vencimiento
        bindEstado(holder.tvEstado, item.vencimiento)

        // setBackgroundColor reemplazaba la forma de la tarjeta (esquinas y borde); se usan drawables
        if (isSelected) {
            holder.itemView.setBackgroundResource(R.drawable.bg_tarjeta_seleccionada)
            holder.lytIconStatus.backgroundTintList = ContextCompat.getColorStateList(ctx, R.color.umk_royal)
            holder.ivStatusIcon.setImageResource(R.drawable.ic_check)
            holder.ivStatusIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.umk_paper))
        } else {
            holder.itemView.setBackgroundResource(R.drawable.bg_tarjeta_perfil)
            holder.lytIconStatus.backgroundTintList = ContextCompat.getColorStateList(ctx, R.color.umk_royal_tint)
            holder.ivStatusIcon.setImageResource(R.drawable.ic_receipt_long)
            holder.ivStatusIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.umk_royal))
        }
        holder.itemView.isSelected = isSelected
        holder.itemView.contentDescription =
            "Factura ${item.numero}, saldo C$ ${"%,.2f".format(saldo)}" + if (isSelected) ", seleccionada" else ""

        // En modo selección, tocar alterna; fuera de él, tocar abre el detalle
        holder.itemView.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
            if (selectedPositions.isNotEmpty()) toggleSelection(pos) else onItemClick(items[pos])
        }

        holder.itemView.setOnLongClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                it.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                toggleSelection(pos)
            }
            true
        }
    }

    private fun toggleSelection(position: Int) {
        if (position in selectedPositions) {
            selectedPositions.remove(position)
        } else {
            selectedPositions.add(position)
        }
        notifyItemChanged(position)
        onSelectionChanged?.invoke(selectedCount, selectedTotal)
    }

    override fun getItemCount() = items.size
}
