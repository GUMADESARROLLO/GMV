package com.unimarksa.gmv.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.unimarksa.gmv.R
import com.unimarksa.gmv.adapters.ClientesAdapter
import com.unimarksa.gmv.models.ItemCliente
import com.unimarksa.gmv.tools.ApiService
import com.unimarksa.gmv.tools.Searchable

class ClientesFragment : Fragment(), Searchable {

    companion object {
        private var cachedClientes: List<ItemCliente>? = null
    }

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var lytEmpty: View
    private lateinit var progressBar: View
    private lateinit var lytNoConnection: View
    private lateinit var btRetry: View
    private var fullList: List<ItemCliente> = emptyList()

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
        lytEmpty = view.findViewById(R.id.lyt_empty)
        progressBar = lytEmpty.findViewById(R.id.progress_bar)
        lytNoConnection = lytEmpty.findViewById(R.id.lyt_no_connection)
        btRetry = lytEmpty.findViewById(R.id.bt_retry)

        recyclerView.layoutManager = LinearLayoutManager(context)

        swipeRefresh.setOnRefreshListener {
            cachedClientes = null
            cargarClientes()
        }

        btRetry.setOnClickListener {
            cargarClientes()
        }

        if (cachedClientes == null) {
            mostrarCargando()
            cargarClientes()
        } else {
            fullList = cachedClientes!!
            mostrarClientes(fullList)
        }
    }

    override fun onSearchQuery(query: String) {
        if (!::recyclerView.isInitialized) return
        if (query.isEmpty()) {
            mostrarClientes(fullList)
            return
        }
        val filtrados = fullList.filter {
            it.nombre.lowercase().contains(query) ||
            it.codCliente.lowercase().contains(query)
        }
        if (filtrados.isEmpty()) {
            mostrarVacioConTexto()
        } else {
            mostrarClientes(filtrados)
        }
    }

    private fun mostrarCargando() {
        swipeRefresh.visibility = View.GONE
        lytEmpty.visibility = View.VISIBLE
        progressBar.visibility = View.VISIBLE
        lytNoConnection.visibility = View.GONE
    }

    private fun mostrarVacioConTexto() {
        swipeRefresh.visibility = View.GONE
        lytEmpty.visibility = View.VISIBLE
        progressBar.visibility = View.GONE
        lytNoConnection.visibility = View.VISIBLE
    }

    private fun cargarClientes() {
        mostrarCargando()
        ApiService.fetchClientes(
            onResult = { clientes ->
                cachedClientes = clientes
                fullList = clientes
                if (clientes.isEmpty()) {
                    mostrarVacioConTexto()
                } else {
                    mostrarClientes(clientes)
                }
            },
            onError = { _ ->
                mostrarVacioConTexto()
            }
        )
    }

    private fun mostrarClientes(clientes: List<ItemCliente>) {
        swipeRefresh.visibility = View.VISIBLE
        lytEmpty.visibility = View.GONE
        swipeRefresh.isRefreshing = false
        recyclerView.adapter = ClientesAdapter(clientes, requireContext())
    }
}
