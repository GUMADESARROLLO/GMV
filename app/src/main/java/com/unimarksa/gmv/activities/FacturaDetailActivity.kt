package com.unimarksa.gmv.activities

import android.content.Intent
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
import com.google.android.material.snackbar.Snackbar
import com.unimarksa.gmv.R
import com.unimarksa.gmv.adapters.DetalleFacturaAdapter
import com.unimarksa.gmv.databinding.ActivityFacturaDetailBinding
import com.unimarksa.gmv.models.ItemFacturaPendiente
import com.unimarksa.gmv.models.ItemPagoFactura
import com.unimarksa.gmv.tools.ApiService
import com.unimarksa.gmv.tools.PagoManager
import com.unimarksa.gmv.ui.PagoFacturaDialog

class FacturaDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_READ_ONLY = "readOnly"
    }

    private lateinit var binding: ActivityFacturaDetailBinding
    private var factura: ItemFacturaPendiente? = null
    private var isReadOnly = false

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

        isReadOnly = intent.getBooleanExtra(EXTRA_READ_ONLY, false)
        if (isReadOnly) {
            binding.lytBottomActions.visibility = View.GONE
        } else {
            binding.btnRegistrarPago.setOnClickListener {
                showPagoDialog()
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }

    private fun bindHeader(f: ItemFacturaPendiente) {
        fun fmt(raw: String): String {
            val v = raw.replace(",", "").trim().toDoubleOrNull() ?: 0.0
            return "C$ ${"%,.2f".format(v)}"
        }
        binding.tvToolbarTitle.text = "${f.numero}"
        binding.tvFacturaSaldoBottom.text = fmt(f.saldo)
        binding.tvDetailVencimiento.text = "${f.vencimiento.replace("-", "/")}"
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

            val saldoVal = saldoStr.replace(",", "").toDoubleOrNull() ?: 0.0
            val recVal = rec.replace(",", "").toDoubleOrNull() ?: 0.0
            val estado = when {
                action == "cancelacion" || action == "contado" -> "pagado"
                recVal < saldoVal -> "parcial"
                else -> "pendiente"
            }
            val pago = ItemPagoFactura(
                numero = factura?.numero ?: "",
                nc = nc.replace(",", "").toDoubleOrNull() ?: 0.0,
                retencion = ret.replace(",", "").toDoubleOrNull() ?: 0.0,
                descuento = desc.replace(",", "").toDoubleOrNull() ?: 0.0,
                valorRecibido = recVal,
                saldo = saldoVal,
                action = action,
                estado = estado
            )
            PagoManager.add(pago)

            Snackbar.make(binding.root, "Pago agregado a la lista", Snackbar.LENGTH_LONG)
                .setAction("Ver listado") {
                    startActivity(Intent(this, ListadoPagosActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                }
                .show()
        }
        dialog.setOnActionListener(actionListener)
        dialog.show(supportFragmentManager, "PagoFacturaDialog")
    }
}
