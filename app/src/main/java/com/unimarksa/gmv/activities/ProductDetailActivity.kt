package com.unimarksa.gmv.activities

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.squareup.picasso.Picasso
import com.unimarksa.gmv.R
import com.unimarksa.gmv.adapters.BonificadosAdapter
import com.unimarksa.gmv.databinding.ActivityProductDetailBinding
import com.unimarksa.gmv.models.ItemProducto

class ProductDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityProductDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = ""

        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val producto = intent.getSerializableExtra("producto") as? ItemProducto
        producto?.let { bindData(it) }
    }

    private fun bindData(item: ItemProducto) {
        binding.productName.text = item.nombre
        binding.idSkuProduct.text = "SKU: ${item.codArticulo}"
        binding.productQuantity.text = item.cantidad
        binding.productPrice.text = "C$ ${item.precio}"

        Picasso.get()
            .load(item.imagenUrl)
            .placeholder(R.drawable.img_empty_history)
            .resize(600, 600)
            .centerCrop()
            .into(binding.productImage)

        val partes = if (!item.bonificado.isNullOrBlank()) {
            item.bonificado!!.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        } else emptyList()

        if (partes.isNotEmpty()) {
            binding.recyclerBonificado.layoutManager = GridLayoutManager(this, 6)
            binding.recyclerBonificado.adapter = BonificadosAdapter(partes)
            binding.recyclerBonificado.visibility = android.view.View.VISIBLE
        } else {
            binding.recyclerBonificado.visibility = android.view.View.GONE
        }

        val html = item.nombre.ifEmpty { "Sin descripción" }
        binding.productDescription.settings.javaScriptEnabled = true
        binding.productDescription.loadDataWithBaseURL(null,
            "<html><body style='color:#525252;font-size:14px'>$html</body></html>",
            "text/html", "utf-8", null)
    }
}
