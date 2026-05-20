package com.unimarksa.gmv.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.unimarksa.gmv.R
import com.unimarksa.gmv.activities.ClienteDetailActivity
import com.unimarksa.gmv.models.ItemCliente

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
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cliente, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.clientName.text = item.nombre
        holder.clientDir.text = item.direccion
        holder.tvDisponible.text = "C$ ${item.disponible}"
        holder.tvSaldo.text = "C$ ${item.saldo}"
        holder.tvLimite.text = "C$ ${item.limite}"

        holder.tvMoroso.visibility = if (item.moroso == "S") View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener {
            val intent = Intent(context, ClienteDetailActivity::class.java)
            intent.putExtra("cliente", item)
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = items.size
}
