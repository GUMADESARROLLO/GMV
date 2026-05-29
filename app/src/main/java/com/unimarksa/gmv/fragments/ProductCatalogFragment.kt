package com.unimarksa.gmv.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.unimarksa.gmv.R
import com.unimarksa.gmv.adapters.ProductAdapter
import com.unimarksa.gmv.tools.ApiService

class ProductCatalogFragment : Fragment() {

    private val adapter = ProductAdapter()
    private var allProducts: List<com.unimarksa.gmv.models.ItemProducto> = listOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_product_catalog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        val rv = view.findViewById<RecyclerView>(R.id.rv_productos)
        rv.layoutManager = GridLayoutManager(requireContext(), 2)
        rv.adapter = adapter

        cargarProductos()

        view.findViewById<TextInputEditText>(R.id.et_buscar).addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim().orEmpty()
                val filtered = if (query.isEmpty()) allProducts
                else allProducts.filter {
                    it.nombre.contains(query, ignoreCase = true) ||
                    it.codArticulo.contains(query, ignoreCase = true)
                }
                adapter.submitList(filtered)
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun cargarProductos() {
        val loading = view?.findViewById<FrameLayout>(R.id.lyt_loading)
        val rv = view?.findViewById<RecyclerView>(R.id.rv_productos)
        loading?.visibility = View.VISIBLE
        rv?.visibility = View.GONE

        ApiService.fetchProductos(
            onResult = { productos ->
                allProducts = productos.take(25)
                adapter.submitList(allProducts)
                loading?.visibility = View.GONE
                rv?.visibility = View.VISIBLE
            },
            onError = { error ->
                loading?.visibility = View.GONE
                rv?.visibility = View.VISIBLE
                view?.let { Snackbar.make(it, error, Snackbar.LENGTH_LONG).show() }
            }
        )
    }
}
