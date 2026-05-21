package com.unimarksa.gmv.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.unimarksa.gmv.R
import com.unimarksa.gmv.adapters.CartAdapter
import com.unimarksa.gmv.databinding.ActivityCartBinding
import com.unimarksa.gmv.models.ItemCarrito
import com.unimarksa.gmv.tools.CartManager
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class CartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCartBinding
    private lateinit var adapter: CartAdapter
    private val fmt = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityCartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT < 35) {
            window.statusBarColor = ContextCompat.getColor(this, R.color.colorUmkPrimaryDark)
        }
        ViewCompat.getWindowInsetsController(window.decorView)?.isAppearanceLightStatusBars = false

        setupToolbar()
        setupRecyclerView()
        setupListeners()
        observeCart()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupRecyclerView() {
        adapter = CartAdapter(
            items = CartManager.currentItems.toMutableList(),
            onQuantityChanged = { item, newQty ->
                CartManager.updateQuantity(item.producto.codArticulo, newQty, item.selectedBonificado)
                updateSummary()
            },
            onDelete = { item ->
                CartManager.remove(item.producto.codArticulo, item.selectedBonificado)
                Snackbar.make(binding.root, "${item.producto.nombre} eliminado", Snackbar.LENGTH_SHORT)
                    .setAction("Deshacer") {
                        CartManager.add(item.producto, item.cantidad, item.selectedBonificado)
                    }
                    .setAnchorView(binding.lytSummary)
                    .show()
            }
        )
        binding.rvCart.layoutManager = LinearLayoutManager(this)
        binding.rvCart.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnContinueShopping.setOnClickListener { finish() }
        binding.btnContinueShoppingBottom.setOnClickListener { finish() }
        binding.btnCheckout.setOnClickListener {
            if (CartManager.currentItems.isEmpty()) return@setOnClickListener
            startActivity(Intent(this, OrderSummaryActivity::class.java))
        }
        binding.btnClear.setOnClickListener {
            CartManager.clear()
            Snackbar.make(binding.root, "Carrito vaciado", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun observeCart() {
        CartManager.items.observe(this, Observer { items ->
            adapter.items.clear()
            adapter.items.addAll(items)
            adapter.notifyDataSetChanged()
            updateUI()
        })
        updateUI()
    }

    private fun updateUI() {
        val isEmpty = CartManager.currentItems.isEmpty()
        val count = CartManager.currentItems.size
        binding.tvCartTitle.text = if (count > 0) "Carrito ($count)" else "Carrito"
        binding.lytEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvCart.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.lytSummary.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.summaryDivider.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.btnClear.visibility = if (isEmpty) View.GONE else View.VISIBLE
        updateSummary()
    }

    private fun updateSummary() {
        binding.tvSubtotal.text = "C$ ${fmt.format(CartManager.subtotal)}"
        binding.tvIva.text = "C$ ${fmt.format(CartManager.iva)}"
        binding.tvTotal.text = "C$ ${fmt.format(CartManager.total)}"
    }
}
