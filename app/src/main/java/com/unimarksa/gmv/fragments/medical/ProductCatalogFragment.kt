package com.unimarksa.gmv.fragments.medical

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.unimarksa.gmv.R
import com.unimarksa.gmv.adapters.medical.ProductAdapter
import com.unimarksa.gmv.data.model.Product
import com.unimarksa.gmv.data.repository.FakeMedicalRepository

class ProductCatalogFragment : Fragment() {

    private val adapter = ProductAdapter { }
    private var allProducts: List<Product> = listOf()
    private var selectedCategory: String = "Todos"

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

        val rv = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_productos)
        rv.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        rv.adapter = adapter

        allProducts = FakeMedicalRepository.getAllProducts()
        adapter.submitList(allProducts)

        view.findViewById<TextInputEditText>(R.id.et_buscar).addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilters(s?.toString(), selectedCategory)
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        view.findViewById<ChipGroup>(R.id.chip_group_categorias).setOnCheckedStateChangeListener { group, checkedIds ->
            val chip = group.findViewById<Chip>(checkedIds.firstOrNull() ?: 0)
            selectedCategory = chip?.text?.toString() ?: "Todos"
            val search = view.findViewById<TextInputEditText>(R.id.et_buscar).text?.toString()
            applyFilters(search, selectedCategory)
        }
    }

    private fun applyFilters(search: String?, category: String) {
        val filtered = allProducts.filter { p ->
            val matchSearch = search.isNullOrEmpty() ||
                p.nombre.contains(search, ignoreCase = true) ||
                p.molecula.contains(search, ignoreCase = true)
            val matchCategory = category == "Todos" || p.categoria == category
            matchSearch && matchCategory
        }
        adapter.submitList(filtered)
    }
}
