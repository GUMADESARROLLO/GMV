package com.unimarksa.gmv.activities

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.unimarksa.gmv.R
import com.unimarksa.gmv.adapters.DetalleFacturaAdapter
import com.unimarksa.gmv.databinding.ActivityFacturaDetailBinding
import com.unimarksa.gmv.models.ItemFacturaPendiente
import com.unimarksa.gmv.tools.ApiService
import com.unimarksa.gmv.ui.PagoFacturaDialog

class FacturaDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFacturaDetailBinding
    private var factura: ItemFacturaPendiente? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityFacturaDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.mainRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (Build.VERSION.SDK_INT >= 35) {
            val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
            val h = if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
            val scrim = View(this)
            scrim.setBackgroundColor(ContextCompat.getColor(this, R.color.colorUmkPrimaryDark))
            val content = findViewById<ViewGroup>(android.R.id.content)
            scrim.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h)
            content.addView(scrim)
        } else {
            window.statusBarColor = ContextCompat.getColor(this, R.color.colorUmkPrimaryDark)
        }

        ViewCompat.getWindowInsetsController(window.decorView)?.isAppearanceLightStatusBars = false

        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        factura = intent.getSerializableExtra("factura") as? ItemFacturaPendiente
        factura?.let { bindHeader(it) }

        binding.btnRegistrarPago.setOnClickListener {
            showPagoDialog()
        }
    }

    private fun bindHeader(f: ItemFacturaPendiente) {
        fun fmt(raw: String): String {
            val v = raw.replace(",", "").trim().toDoubleOrNull() ?: 0.0
            return "C$ ${"%,.2f".format(v)}"
        }
        binding.tvToolbarTitle.text = "Fact. ${f.numero}"
        binding.tvFacturaSaldoBottom.text = fmt(f.saldo)
        binding.tvDetailVencimiento.text = "Venc. ${f.vencimiento.replace("-", "/")}"
        loadDetalle(f.numero)
    }

    private fun loadDetalle(numero: String) {
        binding.lytEmpty.root.visibility = View.GONE
        ApiService.fetchDetalleFactura(
            numeroFactura = numero,
            onResult = { items ->
                if (items.isEmpty()) {
                    binding.lytEmpty.root.visibility = View.VISIBLE
                } else {
                    binding.rvArticulos.layoutManager = LinearLayoutManager(this)
                    binding.rvArticulos.adapter = DetalleFacturaAdapter(items)
                }
            },
            onError = {
                binding.lytEmpty.root.visibility = View.VISIBLE
            }
        )
    }

    private fun showPagoDialog() {
        val dialog = PagoFacturaDialog()
        val saldoStr = factura?.saldo ?: "0.00"
        dialog.setSaldo(saldoStr.replace(",", "").trim().toDoubleOrNull() ?: 0.0)
        val actionListener: (String, Map<String, String>) -> Unit = { action, values ->
            val nc = values[PagoFacturaDialog.FIELD_NC] ?: "0.00"
            val ret = values[PagoFacturaDialog.FIELD_RETENCION] ?: "0.00"
            val desc = values[PagoFacturaDialog.FIELD_DESCUENTO] ?: "0.00"
            val rec = values[PagoFacturaDialog.FIELD_RECIBIDO] ?: "0.00"

            val msg = buildString {
                appendLine("Acci\u00f3n: ${action.replaceFirstChar { it.uppercase() }}")
                if (nc != "0.00") appendLine("N/C: C$ $nc")
                if (ret != "0.00") appendLine("Retenci\u00f3n: C$ $ret")
                if (desc != "0.00") appendLine("Descuento: C$ $desc")
                appendLine("Valor Recibido: C$ $rec")
            }

            MaterialAlertDialogBuilder(this)
                .setTitle("Confirmar pago")
                .setMessage(msg.trimEnd())
                .setPositiveButton("OK") { _, _ -> }
                .show()
        }
        dialog.setOnActionListener(actionListener)
        dialog.show(supportFragmentManager, "PagoFacturaDialog")
    }
}
