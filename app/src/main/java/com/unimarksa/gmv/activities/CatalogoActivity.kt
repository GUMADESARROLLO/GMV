package com.unimarksa.gmv.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.unimarksa.gmv.R
import com.unimarksa.gmv.adapters.ProductosAdapter
import com.unimarksa.gmv.databinding.ActivityCatalogoBinding
import com.unimarksa.gmv.models.ItemProducto
import com.unimarksa.gmv.tools.ApiService
import com.unimarksa.gmv.tools.CartManager

class CatalogoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCatalogoBinding
    private var adapter: ProductosAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityCatalogoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT < 35) {
            window.statusBarColor = ContextCompat.getColor(this, R.color.colorUmkPrimaryDark)
        }
        ViewCompat.getWindowInsetsController(window.decorView)?.isAppearanceLightStatusBars = false

        setupToolbar()
        setupRecyclerView()
        observeCart()
        loadProductos()
    }

    override fun onResume() {
        super.onResume()
        updateCartBadge()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.btnCart.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    private fun setupRecyclerView() {
        binding.rvArticulos.layoutManager = LinearLayoutManager(this)
    }

    private fun loadProductos() {
        binding.lytEmpty.visibility = android.view.View.GONE
        ApiService.fetchProductos(
            onResult = { productos ->
                if (productos.isEmpty()) {
                    binding.lytEmpty.visibility = android.view.View.VISIBLE
                } else {
                    adapter = ProductosAdapter(productos)
                    binding.rvArticulos.adapter = adapter
                }
            },
            onError = {
                binding.lytEmpty.visibility = android.view.View.VISIBLE
            }
        )
    }

    private fun observeCart() {
        CartManager.cartCount.observe(this) { updateCartBadge() }
    }

    private fun updateCartBadge() {
        val count = CartManager.itemCount
        if (count > 0) {
            binding.tvCartBadge.text = if (count > 99) "99+" else count.toString()
            binding.tvCartBadge.visibility = android.view.View.VISIBLE
        } else {
            binding.tvCartBadge.visibility = android.view.View.GONE
        }
    }
}
