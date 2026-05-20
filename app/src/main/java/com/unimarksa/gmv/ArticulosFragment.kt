package com.unimarksa.gmv

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ArticulosFragment : Fragment() {

    private val datos = listOf(
        ItemLista("Laptop HP 15", "Intel i5, 8GB RAM, 256GB SSD", "15:02", 3, Color.parseColor("#BBDEFB")),
        ItemLista("Monitor Samsung 24\"", "Full HD, IPS, 75Hz", "14:59", 1, Color.parseColor("#E1BEE7")),
        ItemLista("Teclado Mecánico", "Cherry MX Red, RGB", "14:42", 0, Color.parseColor("#C8E6C9")),
        ItemLista("Mouse Logitech MX", "Inalámbrico, ergonómico", "14:40", 0, Color.parseColor("#FFCCBC")),
        ItemLista("Webcam HD", "1080p, micrófono integrado", "13:15", 2, Color.parseColor("#B2EBF2")),
        ItemLista("Disco SSD 1TB", "Samsung 870 EVO", "12:45", 0, Color.parseColor("#BBDEFB")),
        ItemLista("Memoria RAM 16GB", "DDR4, 3200MHz", "12:00", 0, Color.parseColor("#E1BEE7")),
        ItemLista("Audífonos Sony", "WH-1000XM5, ANC", "11:30", 5, Color.parseColor("#C8E6C9")),
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_articulos, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = ListaAdapter(datos) { /* click handler */ }
    }
}
