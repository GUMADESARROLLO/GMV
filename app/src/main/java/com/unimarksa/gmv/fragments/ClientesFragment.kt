package com.unimarksa.gmv.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.unimarksa.gmv.R
import com.unimarksa.gmv.adapters.ClientesAdapter
import com.unimarksa.gmv.tools.ApiService

class ClientesFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_clientes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)

        cargarClientes()
    }

    private fun cargarClientes() {
        ApiService.fetchClientes(
            onResult = { clientes ->
                recyclerView.adapter = ClientesAdapter(clientes) { /* click handler */ }
            },
            onError = { error ->
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            }
        )
    }
}
