package com.unimarksa.gmv.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.squareup.picasso.Picasso
import com.unimarksa.gmv.R
import com.unimarksa.gmv.databinding.ActivityOrderSummaryBinding
import com.unimarksa.gmv.tools.CartManager
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderSummaryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderSummaryBinding
    private val fmt = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityOrderSummaryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT < 35) {
            window.statusBarColor = ContextCompat.getColor(this, R.color.colorUmkPrimaryDark)
        }
        ViewCompat.getWindowInsetsController(window.decorView)?.isAppearanceLightStatusBars = false

        setupToolbar()
        setupClientInfo()
        setupOrderInfo()
        populateItems()
        setupListeners()
        updateTotals()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupClientInfo() {
        val cliente = CartManager.currentCliente
        if (cliente != null) {
            binding.cardClient.visibility = View.VISIBLE
            binding.tvClientName.text = "${cliente.codCliente} - ${cliente.nombre}"
            binding.tvClientCode.visibility = View.GONE
            binding.tvClientDireccion.text = cliente.direccion
        } else {
            binding.cardClient.visibility = View.GONE
        }
    }

    private fun setupOrderInfo() {
        binding.tvOrderCode.text = CartManager.generarCodigoPedido()

        val dateFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "NI"))
        binding.tvOrderDate.text = dateFmt.format(Date())
    }

    private fun populateItems() {
        val items = CartManager.currentItems
        binding.tvArticulosTitle.text = "ARTÍCULOS (${items.size})"
        binding.lytItems.removeAllViews()
        val inflater = LayoutInflater.from(this)
        for (item in items) {
            val row = inflater.inflate(R.layout.item_summary_product, binding.lytItems, false) as android.view.View
            row.findViewById<TextView>(R.id.tv_item_name).text = item.producto.nombre
            row.findViewById<TextView>(R.id.tv_item_sku).text = item.producto.codArticulo
            val qtyTxt = if (item.cantidad == item.cantidad.toLong().toDouble()) item.cantidad.toLong().toString() else item.cantidad.toString()
            row.findViewById<TextView>(R.id.tv_item_qty).text = "x$qtyTxt"
            row.findViewById<TextView>(R.id.tv_item_price).text = "C$ ${fmt.format(item.producto.precio.replace(",", "").toDoubleOrNull() ?: 0.0)}"
            row.findViewById<TextView>(R.id.tv_item_subtotal).text = "C$ ${fmt.format(item.subtotal)}"
            val tvBonif = row.findViewById<TextView>(R.id.tv_item_bonificado)
            if (item.selectedBonificado != null) {
                tvBonif.text = " ${item.selectedBonificado}"
                tvBonif.visibility = android.view.View.VISIBLE
            } else {
                tvBonif.visibility = android.view.View.GONE
            }
            val imgUrl = item.producto.imagenUrl
            val ivImage = row.findViewById<android.widget.ImageView>(R.id.tv_item_image)
            if (!imgUrl.isNullOrBlank()) {
                Picasso.get().load(imgUrl).placeholder(R.drawable.ic_placeholder_product).resize(150, 150).centerCrop().into(ivImage)
            }
            binding.lytItems.addView(row)
        }
    }

    private fun setupListeners() {
        binding.btnEdit.setOnClickListener { finish() }

        binding.btnConfirm.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Confirmar pedido")
                .setMessage("""
                    Código: ${binding.tvOrderCode.text}
                    Total: C$ ${fmt.format(CartManager.total)}
                    
                    ¿Deseas confirmar el pedido?
                """.trimIndent())
                .setPositiveButton("Confirmar") { _, _ ->
                    showSuccess()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun showSuccess() {
        MaterialAlertDialogBuilder(this)
            .setIcon(R.drawable.ic_check_circle_filled)
            .setTitle("¡Pedido confirmado!")
            .setMessage("Tu pedido ha sido registrado exitosamente.")
            .setPositiveButton("Ir al inicio") { _, _ ->
                CartManager.submitOrder(
                    codigo = binding.tvOrderCode.text.toString(),
                    observaciones = binding.etObservaciones.text.toString()
                )
                val intent = Intent(this, DashboardActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun updateTotals() {
        val pct = (CartManager.ivaPorcentaje * 100).toInt()
        binding.tvIvaLabel.text = "IVA (${pct}%)"
        binding.tvSubtotal.text = "C$ ${fmt.format(CartManager.subtotal)}"
        binding.tvIva.text = "C$ ${fmt.format(CartManager.iva)}"
        binding.tvDiscount.text = "C$ 0.00"
        binding.tvTotal.text = "C$ ${fmt.format(CartManager.total)}"
    }
}
