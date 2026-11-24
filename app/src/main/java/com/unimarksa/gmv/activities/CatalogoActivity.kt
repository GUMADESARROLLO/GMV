package com.unimarksa.gmv.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.unimarksa.gmv.R
import com.unimarksa.gmv.tools.BarrasSistema
import com.unimarksa.gmv.adapters.ProductosAdapter
import com.unimarksa.gmv.databinding.ActivityCatalogoBinding


class CatalogoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCatalogoBinding
    private var adapter: ProductosAdapter? = null
    private var menuCartItem: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityCatalogoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        BarrasSistema.aplicar(this, R.color.umk_royal)

        setSupportActionBar(binding.toolbar)

        setupToolbar()
        setupRecyclerView()
        setupSearch()
        loadProductos()
        updateCartBadge()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_catalogo, menu)
        menuCartItem = menu.findItem(R.id.action_cart)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_cart -> {
                startActivity(Intent(this, CartActivity::class.java))
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        updateCartBadge()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupRecyclerView() {
        binding.rvArticulos.layoutManager = LinearLayoutManager(this)
    }

    private fun loadProductos() {
        binding.lytEmpty.visibility = View.GONE
        com.unimarksa.gmv.tools.ApiService.fetchProductos(
            onResult = { productos ->
                if (productos.isEmpty()) {
                    binding.lytEmpty.visibility = View.VISIBLE
                } else {
                    adapter = ProductosAdapter(productos.toMutableList())
                    binding.rvArticulos.adapter = adapter
                }
            },
            onError = {
                binding.lytEmpty.visibility = View.VISIBLE
            }
        )
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter?.filter(s?.toString() ?: "")
                binding.btnClearSearch.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        binding.btnClearSearch.setOnClickListener {
            binding.etSearch.text.clear()
            binding.btnClearSearch.visibility = View.GONE
        }
    }

    private fun updateCartBadge() {
        val count = com.unimarksa.gmv.tools.CartManager.itemCount
        menuCartItem?.let {
            if (count > 0) it.setTitle("Carrito ($count)") else it.setTitle("Carrito")
        }
    }
}
