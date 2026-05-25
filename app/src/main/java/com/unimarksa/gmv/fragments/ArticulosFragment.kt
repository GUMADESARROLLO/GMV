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
import com.unimarksa.gmv.adapters.ProductosAdapter
import com.unimarksa.gmv.models.ItemProducto
import com.unimarksa.gmv.tools.ApiService
import com.unimarksa.gmv.tools.Searchable

class ArticulosFragment : Fragment(), Searchable {

    companion object {
        private var cachedProductos: List<ItemProducto>? = null
    }

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var lytEmpty: View
    private lateinit var progressBar: View
    private lateinit var lytNoConnection: View
    private lateinit var btRetry: View
    private var fullList: List<ItemProducto> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_articulos, container, false)
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
            cachedProductos = null
            cargarProductos()
        }

        btRetry.setOnClickListener {
            cargarProductos()
        }

        if (cachedProductos == null) {
            mostrarCargando()
            cargarProductos()
        } else {
            fullList = cachedProductos!!
            mostrarProductos(fullList)
        }
    }

    override fun onSearchQuery(query: String) {
        if (!::recyclerView.isInitialized) return
        if (query.isEmpty()) {
            mostrarProductos(fullList)
            return
        }
        val filtrados = fullList.filter {
            it.nombre.lowercase().contains(query) ||
            it.codArticulo.lowercase().contains(query)
        }
        if (filtrados.isEmpty()) {
            mostrarVacioConTexto()
        } else {
            mostrarProductos(filtrados)
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

    private fun cargarProductos() {
        mostrarCargando()
        ApiService.fetchProductos(
            onResult = { productos ->
                cachedProductos = productos
                fullList = productos
                if (productos.isEmpty()) {
                    mostrarVacioConTexto()
                } else {
                    mostrarProductos(productos)
                }
            },
            onError = { _ ->
                mostrarVacioConTexto()
            }
        )
    }

    private fun mostrarProductos(productos: List<ItemProducto>) {
        swipeRefresh.visibility = View.VISIBLE
        lytEmpty.visibility = View.GONE
        swipeRefresh.isRefreshing = false
        recyclerView.adapter = ProductosAdapter(productos.toMutableList())
    }
}
