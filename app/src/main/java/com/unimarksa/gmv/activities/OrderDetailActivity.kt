package com.unimarksa.gmv.activities

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
import com.unimarksa.gmv.R
import com.unimarksa.gmv.data.local.AppDatabase
import com.unimarksa.gmv.data.local.OrderEntity
import com.unimarksa.gmv.databinding.ActivityOrderDetailBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderDetailBinding
    private val fmt = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
    private val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityOrderDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT < 35) {
            window.statusBarColor = ContextCompat.getColor(this, R.color.colorUmkPrimaryDark)
        }
        ViewCompat.getWindowInsetsController(window.decorView)?.isAppearanceLightStatusBars = false

        db = AppDatabase.getInstance(this)

        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val orderId = intent.getLongExtra("orderId", -1L)
        if (orderId != -1L) {
            cargarDetalle(orderId)
        } else {
            mostrarVacio()
        }
    }

    private fun cargarDetalle(orderId: Long) {
        scope.launch {
            val order = db.orderDao().getOrderById(orderId)
            val items = if (order != null) db.orderDao().getItemsByOrderId(orderId) else emptyList()
            withContext(Dispatchers.Main) {
                if (order != null) {
                    mostrarOrder(order, items)
                } else {
                    mostrarVacio()
                }
            }
        }
    }

    private fun mostrarOrder(order: OrderEntity, items: List<com.unimarksa.gmv.data.local.OrderItemEntity>) {
        binding.lytLoading.visibility = View.GONE
        binding.cardOrderInfo.visibility = View.VISIBLE
        binding.cardItems.visibility = View.VISIBLE
        binding.cardTotal.visibility = View.VISIBLE

        if (order.observaciones.isNotBlank()) {
            binding.cardObservaciones.visibility = View.VISIBLE
            binding.tvObservaciones.text = order.observaciones
        } else {
            binding.cardObservaciones.visibility = View.GONE
        }

        val dateFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        binding.tvOrderCode.text = order.codigo.ifEmpty { "LOC-${order.id}" }
        binding.tvClientName.text = "${order.clienteCodigo} - ${order.clienteNombre}"
        binding.tvOrderDate.text = dateFmt.format(Date(order.fechaCreacion))

        binding.tvItemsTitle.text = "ARTÍCULOS (${items.size})"
        binding.lytItems.removeAllViews()
        val inflater = LayoutInflater.from(this)

        for (item in items) {
            val row = inflater.inflate(R.layout.item_summary_product, binding.lytItems, false)
            row.findViewById<TextView>(R.id.tv_item_name).text = item.nombre
            row.findViewById<TextView>(R.id.tv_item_sku).text = item.codArticulo
            row.findViewById<TextView>(R.id.tv_item_qty).text = "x${item.cantidad}"
            row.findViewById<TextView>(R.id.tv_item_price).text = "C$ ${fmt.format(item.precio)}"
            row.findViewById<TextView>(R.id.tv_item_subtotal).text = "C$ ${fmt.format(item.lineaSubtotal)}"
            if (!item.bonificado.isNullOrBlank()) {
                row.findViewById<TextView>(R.id.tv_item_bonificado).text = "| ${item.bonificado}".also {
                    row.findViewById<TextView>(R.id.tv_item_bonificado).visibility = View.VISIBLE
                }
            } else {
                row.findViewById<TextView>(R.id.tv_item_bonificado).visibility = View.GONE
            }
            val imgCard = row.findViewById<View>(R.id.tv_item_image).parent as? View
            imgCard?.visibility = View.GONE
            binding.lytItems.addView(row)
        }

        binding.tvTotalAmount.text = "C$ ${fmt.format(order.total)}"
    }

    private fun mostrarVacio() {
        binding.lytLoading.visibility = View.GONE
        binding.lytEmpty.visibility = View.VISIBLE
    }
}
