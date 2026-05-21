package com.unimarksa.gmv.adapters

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.RippleDrawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.unimarksa.gmv.R
import com.unimarksa.gmv.activities.ClienteDetailActivity
import com.unimarksa.gmv.models.ItemCliente
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class ClientesAdapter(
    private val items: List<ItemCliente>,
    private val context: Context
) : RecyclerView.Adapter<ClientesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: CardView = view as CardView
        val clientName: TextView = view.findViewById(R.id.client_name)
        val clientDir: TextView = view.findViewById(R.id.client_dir)
        val tvDisponible: TextView = view.findViewById(R.id.id_cliente_disponible)
        val tvSaldo: TextView = view.findViewById(R.id.id_cliente_saldo)
        val tvLimite: TextView = view.findViewById(R.id.id_cliente_limite)
        val ivAvatar: ImageView = view.findViewById(R.id.id_moroso_img)
        val tvMoroso: TextView = view.findViewById(R.id.id_lyt_moroso)
        val ivPlan: ImageView = view.findViewById(R.id.id_img_plan)
        val lytPin: View = view.findViewById(R.id.id_lyt_pin)
        val btnVerificacion: ImageView = view.findViewById(R.id.btn_verificacion)
        val tvCodigo: TextView = view.findViewById(R.id.id_cliente_codigo)
        val lytParent: LinearLayout = view.findViewById(R.id.lyt_parent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cliente, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.clientName.text = "${item.codCliente} - ${item.nombre}"
        holder.clientDir.text = item.direccion
        holder.tvDisponible.text = "C$ ${formatNumero(item.disponible)}"
        holder.tvSaldo.text = "C$ ${formatNumero(item.saldo)}"
        holder.tvLimite.text = "C$ ${formatNumero(item.limite)}"
        holder.tvCodigo.text = item.codCliente

        holder.tvMoroso.visibility = if (item.moroso == "S") View.VISIBLE else View.GONE

        if (item.moroso == "S") setSelectableBackground(holder.lytParent, R.color.mora_danger_bg)

        holder.itemView.setOnClickListener {
            val intent = Intent(context, ClienteDetailActivity::class.java)
            intent.putExtra("cliente", item)
            context.startActivity(intent)
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

    private fun setSelectableBackground(view: View, baseColorResId: Int) {
        val baseColor = ContextCompat.getColor(context, baseColorResId)
        val typedValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.colorControlHighlight, typedValue, true)
        val rippleColor = ColorStateList.valueOf(typedValue.data)
        val rippleDrawable = RippleDrawable(rippleColor, ColorDrawable(baseColor), null)
        view.background = rippleDrawable
    }
}
