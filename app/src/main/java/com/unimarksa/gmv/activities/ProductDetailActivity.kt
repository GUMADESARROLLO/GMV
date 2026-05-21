package com.unimarksa.gmv.activities

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.card.MaterialCardView
import com.squareup.picasso.Picasso
import com.unimarksa.gmv.R
import com.unimarksa.gmv.databinding.ActivityProductDetailBinding
import com.unimarksa.gmv.models.ItemProducto

class ProductDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductDetailBinding
    private var selectedBonificado: String? = null

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

        setupToolbar()
        setupFab()

        val producto = intent.getSerializableExtra("producto") as? ItemProducto
        producto?.let { bindData(it) }
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.btnShare.setOnClickListener {
            val sendIntent = android.content.Intent().apply {
                action = android.content.Intent.ACTION_SEND
                putExtra(android.content.Intent.EXTRA_TEXT,
                    "${binding.productName.text} - SKU: ${binding.productSku.text}")
                type = "text/plain"
            }
            startActivity(android.content.Intent.createChooser(sendIntent, null))
        }
    }

    private fun setupFab() {
        binding.fabCart.setOnClickListener {
            // TODO: agregar al carrito/pedido
        }
    }

    private fun bindData(item: ItemProducto) {
        binding.productName.text = item.nombre
        binding.productSku.text = "SKU: ${item.codArticulo}"
        binding.productQuantity.text = item.cantidad
        binding.productPrice.text = "C$ ${item.precio}"
        binding.productDescription.text = item.nombre.ifEmpty { "Sin descripción" }

        Picasso.get()
            .load(item.imagenUrl)
            .placeholder(R.drawable.img_empty_history)
            .resize(600, 600)
            .centerCrop()
            .into(binding.imgProduct)

        val partes = if (!item.bonificado.isNullOrBlank()) {
            item.bonificado!!.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        } else emptyList()

        if (partes.isNotEmpty()) {
            binding.bonusContainer.removeAllViews()
            for (texto in partes) {
                val card = layoutInflater.inflate(R.layout.item_bonificado_card, binding.bonusContainer, false) as MaterialCardView
                val label = card.findViewById<TextView>(R.id.cardText)
                label.text = texto
                card.setOnClickListener { selectBonificado(texto, card) }
                binding.bonusContainer.addView(card)
            }
        }
    }

    private fun selectBonificado(texto: String, selected: MaterialCardView) {
        val isSame = selectedBonificado == texto
        selectedBonificado = if (isSame) null else texto
        for (i in 0 until binding.bonusContainer.childCount) {
            val child = binding.bonusContainer.getChildAt(i) as MaterialCardView
            val isSelected = child == selected && !isSame
            if (isSelected) {
                child.setCardBackgroundColor(ContextCompat.getColor(this, R.color.mockup_btn_active))
                child.setStrokeColor(ContextCompat.getColor(this, R.color.mockup_btn_active))
                child.strokeWidth = 0
                val tv = child.findViewById<TextView>(R.id.cardText)
                tv.setTextColor(ContextCompat.getColor(this, R.color.white))
            } else {
                child.setCardBackgroundColor(ContextCompat.getColor(this, R.color.size_chip_available_bg))
                child.setStrokeColor(ContextCompat.getColor(this, R.color.size_chip_available_text))
                child.strokeWidth = resources.getDimensionPixelSize(R.dimen.size_chip_stroke)
                val tv = child.findViewById<TextView>(R.id.cardText)
                tv.setTextColor(ContextCompat.getColor(this, R.color.size_chip_available_text))
            }
        }
        if (isSame) {
            binding.btnAdd.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.mockup_btn_primary))
            binding.hintText.text = getString(R.string.hint_sin_seleccion)
        } else {
            binding.btnAdd.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.mockup_btn_active))
            binding.hintText.text = getString(R.string.hint_talla_seleccionada, texto)
        }
    }
}
