package com.unimarksa.gmv.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Picasso
import com.unimarksa.gmv.R
import com.unimarksa.gmv.databinding.ActivityProductDetailBinding
import com.unimarksa.gmv.models.ItemProducto
import com.unimarksa.gmv.tools.CartManager
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class ProductDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductDetailBinding
    private var selectedBonificado: String? = null
    private var producto: ItemProducto? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityProductDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT < 35) {
            window.statusBarColor = ContextCompat.getColor(this, R.color.colorUmkPrimaryDark)
        }

        ViewCompat.getWindowInsetsController(window.decorView)?.isAppearanceLightStatusBars = false

        setupToolbar()
        setupFab()
        setupAddButton()

        producto = intent.getSerializableExtra("producto") as? ItemProducto
        producto?.let { bindData(it) }
        updateCartBadge()
    }

    override fun onResume() {
        super.onResume()
        updateCartBadge()
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
            if (CartManager.currentCliente == null) {
                Snackbar.make(binding.main, "Primero seleccione un Cliente", Snackbar.LENGTH_LONG)
                    .setAnchorView(binding.btnAdd)
                    .show()
            } else {
                showCalculatorModal()
            }
        }
    }

    private fun showCalculatorModal() {
        val prod = producto ?: return
        val stock = prod.cantidad.replace(",", "").toDoubleOrNull() ?: 999999.0
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_calculator_quantity, null)
        dialog.setContentView(view)

        val tvDisplay = view.findViewById<TextView>(R.id.tv_display)
        val tvStock = view.findViewById<TextView>(R.id.tv_stock_calc)
        val tvPrice = view.findViewById<TextView>(R.id.tv_price_calc)
        val tvBonus = view.findViewById<TextView>(R.id.tv_bonus_calc)
        val tvName = view.findViewById<TextView>(R.id.tv_product_name_calc)
        val precioUni = prod.precio.replace(",", "").toDoubleOrNull() ?: 0.0
        val priceFmt = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
        tvName.text = prod.nombre
        tvStock.text = "Stock Disp. ${formatNumero(stock.toString())} UND"

        fun parseBonificado(b: String): Pair<Double, Double>? {
            val parts = b.split("+").map { it.trim().toDoubleOrNull() }
            return if (parts.size == 2 && parts[0] != null && parts[1] != null)
                Pair(parts[0]!!, parts[1]!!) else null
        }

        val bonif = selectedBonificado?.let { parseBonificado(it) }

        var input = if (bonif != null) bonif.first.toInt().toString() else "1"
        var userStarted = bonif != null
        tvDisplay.text = input

        fun updatePrice() {
            val qty = input.toDoubleOrNull() ?: 1.0
            val (paidQty, freeQty) = bonif ?: Pair(qty, 0.0)
            val cycleSize = paidQty + freeQty
            val effectivePaid = if (bonif != null && cycleSize > 0) {
                val fullCycles = (qty / cycleSize).toInt()
                val remaining = qty - fullCycles * cycleSize
                (fullCycles * paidQty) + remaining.coerceAtMost(paidQty)
            } else qty
            val total = effectivePaid * precioUni
            tvPrice.text = "C$ ${priceFmt.format(total)}"
            if (bonif != null && qty >= cycleSize) {
                tvBonus.visibility = android.view.View.VISIBLE
                tvBonus.text = "Bonif: ${paidQty.toInt()}+${freeQty.toInt()} (paga ${effectivePaid.toInt()} uds)"
            } else {
                tvBonus.visibility = android.view.View.GONE
            }
        }
        updatePrice()

        fun updateDisplay() {
            tvDisplay.text = input
            tvDisplay.scaleX = 0.85f
            tvDisplay.scaleY = 0.85f
            tvDisplay.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            tvStock.setTextColor(ContextCompat.getColor(this, R.color.available))
            tvStock.text = "Stock Disp. ${formatNumero(stock.toString())} UND"
            updatePrice()
        }

        fun currentValue(): Double = input.toDoubleOrNull() ?: 1.0

        val digits = mapOf(
            R.id.btn_0 to "0", R.id.btn_1 to "1", R.id.btn_2 to "2", R.id.btn_3 to "3",
            R.id.btn_4 to "4", R.id.btn_5 to "5", R.id.btn_6 to "6", R.id.btn_7 to "7",
            R.id.btn_8 to "8", R.id.btn_9 to "9"
        )

        for ((id, digit) in digits) {
            view.findViewById<Button>(id).setOnClickListener {
                val candidate = if (!userStarted) digit else input + digit
                userStarted = true
                if (candidate.toDoubleOrNull() != null && candidate.length <= 10) {
                    input = candidate
                    updateDisplay()
                }
            }
        }

        view.findViewById<Button>(R.id.btn_dot).setOnClickListener {
            if (!input.contains(".")) {
                input += "."
                updateDisplay()
            }
        }

        view.findViewById<Button>(R.id.btn_delete).setOnClickListener {
            if (input.length > 1) {
                input = input.dropLast(1)
                if (input == "." || input.isEmpty()) input = "1"
            } else {
                input = "1"
            }
            if (input == "1" && !input.contains(".")) userStarted = false
            updateDisplay()
        }

        view.findViewById<Button>(R.id.btn_clear).setOnClickListener {
            input = "1"
            userStarted = false
            updateDisplay()
        }

        view.findViewById<Button>(R.id.btn_confirm).setOnClickListener {
            val qty = currentValue()
            if (qty > stock) {
                tvStock.setTextColor(ContextCompat.getColor(this, R.color.error))
                tvStock.text = "Stock disp.: ${formatNumero(stock.toString())} UND — Excedido"
                tvStock.postDelayed({
                    tvStock.setTextColor(ContextCompat.getColor(this, R.color.available))
                    tvStock.text = "Stock disp.: ${formatNumero(stock.toString())} UND"
                }, 3000)
                return@setOnClickListener
            }
            CartManager.add(prod, qty, selectedBonificado)
            updateCartBadge()
            dialog.dismiss()
            val fmtQty = if (qty == qty.toLong().toDouble()) qty.toLong().toString() else qty.toString()
            Snackbar.make(binding.main, "$fmtQty x ${prod.nombre} agregado", Snackbar.LENGTH_LONG)
                .setAction("Ver carrito") {
                    startActivity(Intent(this, CartActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                }
                .setAnchorView(binding.btnAdd)
                .show()
        }

        dialog.show()
    }

    private fun setupAddButton() {
        binding.btnAdd.setOnClickListener {
            if (CartManager.currentCliente == null) {
                Snackbar.make(binding.main, "Primero seleccione un Cliente", Snackbar.LENGTH_LONG)
                    .setAnchorView(binding.btnAdd)
                    .show()
            } else {
                showCalculatorModal()
            }
        }
    }

    private fun updateCartBadge() {
        val count = CartManager.itemCount
        if (count > 0) {
            binding.cartBadge.text = if (count > 99) "99+" else count.toString()
            binding.cartBadge.visibility = android.view.View.VISIBLE
            binding.cartBadge.scaleX = 0f
            binding.cartBadge.scaleY = 0f
            binding.cartBadge.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(250)
                .start()
        } else {
            binding.cartBadge.visibility = android.view.View.GONE
        }
    }

    private fun bindData(item: ItemProducto) {
        producto = item
        binding.productName.text = item.nombre
        binding.productSku.text = "SKU: ${item.codArticulo}"
        val cantidadFmt = formatNumero(item.cantidad)
        binding.productQuantity.text = if (!item.und.isNullOrBlank()) "$cantidadFmt [${item.und}]" else cantidadFmt
        binding.productPrice.text = "C$ ${formatNumero(item.precio)}"
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

    private fun formatNumero(valor: String): String {
        return try {
            val num = valor.replace(",", "").toDouble()
            DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US)).format(num)
        } catch (_: NumberFormatException) {
            valor
        }
    }
}
