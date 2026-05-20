package com.unimarksa.gmv.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.unimarksa.gmv.R
import com.unimarksa.gmv.adapters.ClientesAdapter
import com.unimarksa.gmv.models.ItemCliente
import com.unimarksa.gmv.tools.ApiService

class ClientesFragment : Fragment() {

    companion object {
        private var cachedClientes: List<ItemCliente>? = null
    }

    private lateinit var swipeRefresh: SwipeRefreshLayout
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

        swipeRefresh = view.findViewById(R.id.swipe_refresh)
        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)

        swipeRefresh.setOnRefreshListener {
            cachedClientes = null
            cargarClientes()
        }

        if (cachedClientes == null) {
            cargarClientes()
        } else {
            mostrarClientes(cachedClientes!!)
        }
    }

    private fun cargarClientes() {
        swipeRefresh.isRefreshing = true
        ApiService.fetchClientes(
            onResult = { clientes ->
                cachedClientes = clientes
                mostrarClientes(clientes)
                swipeRefresh.isRefreshing = false
            },
            onError = { error ->
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                swipeRefresh.isRefreshing = false
            }
        )
    }

    private fun mostrarClientes(clientes: List<ItemCliente>) {
        recyclerView.adapter = ClientesAdapter(clientes) { /* click handler */ }
    }
}
