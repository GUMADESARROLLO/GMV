package com.unimarksa.gmv.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.unimarksa.gmv.R
import com.unimarksa.gmv.models.ItemDetalleFactura
import com.unimarksa.gmv.tools.Config

class DetalleFacturaAdapter(
    private val items: List<ItemDetalleFactura>
) : RecyclerView.Adapter<DetalleFacturaAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivImagen: ImageView = view.findViewById(R.id.iv_imagen)
        val tvArticulo: TextView = view.findViewById(R.id.tv_articulo)
        val tvDescripcion: TextView = view.findViewById(R.id.tv_descripcion)
        val tvCantidad: TextView = view.findViewById(R.id.tv_cantidad)
        val tvPrecioUnitario: TextView = view.findViewById(R.id.tv_precio_unitario)
        val tvTotalLinea: TextView = view.findViewById(R.id.tv_total_linea)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_detalle_factura, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val imgUrl = if (item.imagen.isNotEmpty()) "${Config.BASE_URL}/upload/product/${item.imagen}" else null
        if (imgUrl != null) {
            Picasso.get().load(imgUrl).placeholder(R.drawable.ic_placeholder_product).resize(120, 120).centerCrop().into(holder.ivImagen)
        } else {
            holder.ivImagen.setImageResource(R.drawable.ic_placeholder_product)
        }
        holder.tvArticulo.text = item.articulo
        holder.tvDescripcion.text = item.descripcion

        val ctx = holder.itemView.context
        val qty = item.cantidad.replace(",", "").trim().toDoubleOrNull() ?: 0.0
        holder.tvCantidad.text = "${"%,.0f".format(qty)} UND"

        // "venta" es el TOTAL de la línea (la suma de líneas da el saldo de la factura);
        // el unitario se obtiene dividiendo entre la cantidad.
        val totalLinea = item.ventaNumerica
        val pad = (ctx.resources.displayMetrics.density).let { (10 * it).toInt() to (3 * it).toInt() }

        if (item.esBonificado) {
            holder.tvTotalLinea.text = "Bonificado"
            holder.tvTotalLinea.setBackgroundResource(R.drawable.bg_chip_pill)
            holder.tvTotalLinea.backgroundTintList = ContextCompat.getColorStateList(ctx, R.color.umk_success_tint)
            holder.tvTotalLinea.setPadding(pad.first, pad.second, pad.first, pad.second)
            holder.tvTotalLinea.textSize = 12f
            holder.tvPrecioUnitario.visibility = View.GONE
        } else {
            holder.tvTotalLinea.text = "C$ ${"%,.2f".format(totalLinea)}"
            holder.tvTotalLinea.background = null
            holder.tvTotalLinea.setPadding(0, 0, 0, 0)
            holder.tvTotalLinea.textSize = 15f
            holder.tvPrecioUnitario.text = if (qty > 0) "C$ ${"%,.2f".format(totalLinea / qty)} c/u" else ""
            holder.tvPrecioUnitario.visibility = if (qty > 0) View.VISIBLE else View.GONE
        }
        holder.itemView.contentDescription =
            "${item.descripcion}, ${holder.tvCantidad.text}, ${holder.tvTotalLinea.text}"
    }

    override fun getItemCount() = items.size
}
