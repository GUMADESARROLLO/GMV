package com.unimarksa.gmv.activities

import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.unimarksa.gmv.R
import com.unimarksa.gmv.databinding.ActivityListadoPagosBinding
import com.unimarksa.gmv.models.ItemPagoFactura
import com.unimarksa.gmv.tools.PagoManager
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class ListadoPagosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListadoPagosBinding
    private val fmt = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityListadoPagosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT < 35) {
            window.statusBarColor = ContextCompat.getColor(this, R.color.colorUmkPrimaryDark)
        }
        ViewCompat.getWindowInsetsController(window.decorView)?.isAppearanceLightStatusBars = false

        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        binding.btnContinuar.setOnClickListener {
            startActivity(android.content.Intent(this, ReciboColectorActivity::class.java))
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }

        refreshPagos()
    }

    override fun onResume() {
        super.onResume()
        refreshPagos()
    }

    private fun refreshPagos() {
        val pagos = PagoManager.pagos
        binding.tvPagoCount.text = pagos.size.toString()
        binding.lytPagos.removeAllViews()

        if (pagos.isEmpty()) {
            binding.lytEmpty.visibility = View.VISIBLE
            binding.lytPagos.visibility = View.GONE
            binding.btnContinuar.isEnabled = false
            binding.btnContinuar.alpha = 0.5f
        } else {
            binding.lytEmpty.visibility = View.GONE
            binding.lytPagos.visibility = View.VISIBLE
            binding.btnContinuar.isEnabled = true
            binding.btnContinuar.alpha = 1.0f

            val inflater = LayoutInflater.from(this)
            for ((idx, pago) in pagos.withIndex()) {
                val row = inflater.inflate(R.layout.item_resumen_pago, binding.lytPagos, false)
                row.findViewById<TextView>(R.id.id_Factura).text = "Fact. ${pago.numero}"
                row.findViewById<TextView>(R.id.id_nc).text = "C$ ${fmt.format(pago.nc)}"
                row.findViewById<TextView>(R.id.id_retencion).text = "C$ ${fmt.format(pago.retencion)}"
                row.findViewById<TextView>(R.id.id_descuento).text = "C$ ${fmt.format(pago.descuento)}"
                row.findViewById<TextView>(R.id.id_valor_recibido).text = "C$ ${fmt.format(pago.valorRecibido)}"
                applyEstado(row, pago)
                row.setOnLongClickListener {
                    removePago(idx)
                    true
                }
                binding.lytPagos.addView(row)
            }
        }
    }

    private fun applyEstado(row: View, pago: ItemPagoFactura) {
        val color = estadoColor(pago.estado)
        row.findViewById<View>(R.id.view_estado_bar).setBackgroundColor(color)
        val badge = row.findViewById<TextView>(R.id.id_badge_estado)
        badge.text = when (pago.action) {
            "abono" -> "Bono"
            "cancelacion" -> "Cancelacion"
            "contado" -> "Contado"
            else -> pago.action.replaceFirstChar { it.uppercase() }
        }
        badge.backgroundTintList = ColorStateList.valueOf(color)
    }

    private fun estadoColor(estado: String): Int {
        return when (estado) {
            "pagado" -> ContextCompat.getColor(this, R.color.green_500)
            "parcial" -> ContextCompat.getColor(this, R.color.light_blue_400)
            else -> ContextCompat.getColor(this, R.color.red_400)
        }
    }

    private fun removePago(index: Int) {
        val pago = PagoManager.pagos.getOrNull(index) ?: return
        MaterialAlertDialogBuilder(this)
            .setTitle("Eliminar pago")
            .setMessage("Eliminar pago de Fact. ${pago.numero}?")
            .setPositiveButton("Eliminar") { _, _ ->
                PagoManager.removeAt(index)
                refreshPagos()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
