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
        recyclerView.layoutManager = LinearLayoutManager(context)

        swipeRefresh.setOnRefreshListener {
            cachedProductos = null
            cargarProductos()
        }

        if (cachedProductos == null) {
            cargarProductos()
        } else {
            fullList = cachedProductos!!
            mostrarProductos(fullList)
        }
    }

    override fun onSearchQuery(query: String) {
        if (query.isEmpty()) {
            mostrarProductos(fullList)
            return
        }
        val filtrados = fullList.filter {
            it.nombre.lowercase().contains(query) ||
            it.codArticulo.lowercase().contains(query)
        }
        mostrarProductos(filtrados)
    }

    private fun cargarProductos() {
        swipeRefresh.isRefreshing = true
        ApiService.fetchProductos(
            onResult = { productos ->
                cachedProductos = productos
                fullList = productos
                mostrarProductos(productos)
                swipeRefresh.isRefreshing = false
            },
            onError = { error ->
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                swipeRefresh.isRefreshing = false
            }
        )
    }

    private fun mostrarProductos(productos: List<ItemProducto>) {
        recyclerView.adapter = ProductosAdapter(productos)
    }
}
