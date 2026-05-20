package com.unimarksa.gmv.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.unimarksa.gmv.R
import com.unimarksa.gmv.adapters.ListaAdapter
import com.unimarksa.gmv.models.ItemLista

class ClientesFragment : Fragment() {

    private val datos = listOf(
        ItemLista("Roberto García", "Última compra: 15/05/2026", "15:02", 3, Color.parseColor("#BBDEFB")),
        ItemLista("Alicia Torreaux", "Cliente frecuente, 12 pedidos", "14:59", 1, Color.parseColor("#E1BEE7")),
        ItemLista("Karen Stanford", "Pendiente de pago: $1,250", "14:42", 0, Color.parseColor("#C8E6C9")),
        ItemLista("Cat Videos Inc.", "Empresa, plan corporativo", "14:40", 0, Color.parseColor("#FFCCBC")),
        ItemLista("Little Brother Co.", "Nuevo cliente, registro reciente", "13:15", 2, Color.parseColor("#B2EBF2")),
        ItemLista("Mónica Parker", "Devolución solicitada", "12:45", 0, Color.parseColor("#BBDEFB")),
        ItemLista("Angélica Ruiz", "Crédito aprobado", "12:00", 4, Color.parseColor("#E1BEE7")),
        ItemLista("David Chen", "Pedido mayorista pendiente", "11:30", 0, Color.parseColor("#C8E6C9")),
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_clientes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = ListaAdapter(datos) { /* click handler */ }
    }
}
