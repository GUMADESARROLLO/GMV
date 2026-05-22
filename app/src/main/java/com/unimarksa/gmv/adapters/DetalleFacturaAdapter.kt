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

        val qty = item.cantidad.replace(",", "").trim().toDoubleOrNull() ?: 0.0
        holder.tvCantidad.text = "${"%,.0f".format(qty)} UND"

        val precio = item.ventaNumerica
        val total = qty * precio

        if (item.esBonificado) {
            holder.tvPrecioUnitario.visibility = View.VISIBLE
            holder.tvPrecioUnitario.text = "(BONO)"
            holder.tvPrecioUnitario.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.mora_ok))
            holder.tvTotalLinea.visibility = View.GONE
        } else {
            holder.tvPrecioUnitario.visibility = View.VISIBLE
            holder.tvPrecioUnitario.text = "C$ ${"%,.2f".format(precio)}"
            holder.tvPrecioUnitario.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.txt_price_color))
            holder.tvTotalLinea.visibility = View.VISIBLE
            holder.tvTotalLinea.text = "C$ ${"%,.2f".format(total)}"
        }
    }

    override fun getItemCount() = items.size
}
