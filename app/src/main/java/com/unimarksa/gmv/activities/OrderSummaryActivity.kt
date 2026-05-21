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
            binding.tvClientName.text = cliente.nombre
            binding.tvClientCode.text = "Código: ${cliente.codCliente}"
        } else {
            binding.cardClient.visibility = View.GONE
        }
    }

    private fun setupOrderInfo() {
        val code = "PED-${System.currentTimeMillis().toString().takeLast(6)}"
        binding.tvOrderCode.text = code

        val dateFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "NI"))
        binding.tvOrderDate.text = dateFmt.format(Date())
    }

    private fun populateItems() {
        binding.lytItems.removeAllViews()
        val inflater = LayoutInflater.from(this)
        for (item in CartManager.currentItems) {
            val row = inflater.inflate(R.layout.item_summary_product, binding.lytItems, false) as LinearLayout
            row.findViewById<TextView>(R.id.tv_item_name).text = item.producto.nombre
            row.findViewById<TextView>(R.id.tv_item_sku).text = item.producto.codArticulo
            row.findViewById<TextView>(R.id.tv_item_qty).text = "x${item.cantidad}"
            row.findViewById<TextView>(R.id.tv_item_price).text = "C$ ${fmt.format(item.producto.precio.replace(",", "").toDoubleOrNull() ?: 0.0)}"
            row.findViewById<TextView>(R.id.tv_item_subtotal).text = "C$ ${fmt.format(item.subtotal)}"
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
                CartManager.clear()
                val intent = Intent(this, DashboardActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun updateTotals() {
        binding.tvSubtotal.text = "C$ ${fmt.format(CartManager.subtotal)}"
        binding.tvIva.text = "C$ ${fmt.format(CartManager.iva)}"
        binding.tvDiscount.text = "C$ 0.00"
        binding.tvTotal.text = "C$ ${fmt.format(CartManager.total)}"
    }
}
