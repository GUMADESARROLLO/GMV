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
import com.unimarksa.gmv.tools.BarrasSistema
import com.unimarksa.gmv.adapters.DetalleFacturaAdapter
import com.unimarksa.gmv.databinding.ActivityFacturaDetailBinding
import com.unimarksa.gmv.models.ItemFacturaPendiente
import com.unimarksa.gmv.models.ItemPagoFactura
import com.unimarksa.gmv.tools.ApiService
import com.unimarksa.gmv.tools.PagoManager
import com.unimarksa.gmv.tools.PagoFacturaDialog

class FacturaDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_READ_ONLY = "readOnly"
    }

    private lateinit var binding: ActivityFacturaDetailBinding
    private var factura: ItemFacturaPendiente? = null
    private var isReadOnly = false
    private var observaciones: List<Pair<String?, String>> = emptyList()

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

        BarrasSistema.aplicar(this, R.color.umk_mist)


        binding.btnObservaciones.setOnClickListener { mostrarObservaciones() }

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
        binding.tvToolbarTitle.text = "Fact. ${f.numero}"
        binding.tvFacturaSaldoBottom.text = fmt(f.saldo)
        binding.tvDetailVencimiento.text = "Vence ${f.vencimiento.replace("-", "/")}"
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
                    prepararObservaciones(items)
                    binding.tvConteoArticulos.text = "${items.size}"
                    binding.tvConteoArticulos.visibility = View.VISIBLE
                }
            },
            onError = {
                binding.lytEmpty.root.visibility = View.VISIBLE

            }
        )
    }

    /**
     * OBSERVACIONES llega en cada línea del detalle (normalmente el mismo comentario repetido).
     * Si todas coinciden se muestra una sola vez; si alguna línea trae uno distinto, se lista
     * con el artículo al que pertenece.
     */
    private fun prepararObservaciones(items: List<com.unimarksa.gmv.models.ItemDetalleFactura>) {
        val conTexto = items.filter { it.observaciones.isNotBlank() }
        val distintas = conTexto.map { it.observaciones.trim() }.distinct()
        observaciones = when {
            distintas.isEmpty() -> emptyList()
            distintas.size == 1 -> listOf(null to distintas.first())
            else -> conTexto.distinctBy { it.observaciones.trim() }
                .map { it.descripcion.ifBlank { it.articulo } to it.observaciones.trim() }
        }
        val hay = observaciones.isNotEmpty()
        binding.btnObservaciones.isEnabled = true
        binding.btnObservaciones.alpha = 1f
        binding.dotObservaciones.visibility = if (hay) View.VISIBLE else View.GONE
        binding.btnObservaciones.contentDescription =
            if (hay) "Ver observaciones de la factura" else "La factura no tiene observaciones"
    }

    private fun mostrarObservaciones() {
        val sheet = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_observaciones, null)
        view.findViewById<android.widget.TextView>(R.id.tvObsFactura).text = "Fact. ${factura?.numero.orEmpty()}"
        val contenedor = view.findViewById<android.widget.LinearLayout>(R.id.lytObservaciones)
        val dp = resources.displayMetrics.density

        fun bloque(titulo: String?, texto: String, italica: Boolean = false) {
            val card = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                setBackgroundResource(R.drawable.bg_tarjeta_perfil)
                val p = (14 * dp).toInt()
                setPadding(p, p, p, p)
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { if (contenedor.childCount > 0) topMargin = (8 * dp).toInt() }
            }
            if (titulo != null) {
                card.addView(android.widget.TextView(this).apply {
                    text = titulo
                    setTextColor(ContextCompat.getColor(context, R.color.umk_ink_muted))
                    textSize = 12f
                    maxLines = 2
                    setPadding(0, 0, 0, (4 * dp).toInt())
                })
            }
            card.addView(android.widget.TextView(this).apply {
                text = texto
                setTextColor(ContextCompat.getColor(context, if (italica) R.color.umk_ink_muted else R.color.umk_ink))
                textSize = 15f
                setLineSpacing(0f, 1.2f)
                setTextIsSelectable(!italica)
            })
            contenedor.addView(card)
        }

        if (observaciones.isEmpty()) bloque(null, "Esta factura no tiene observaciones.", italica = true)
        else observaciones.forEach { (titulo, texto) -> bloque(titulo, texto) }

        view.findViewById<View>(R.id.btnCerrarObs).setOnClickListener { sheet.dismiss() }
        sheet.setContentView(view)
        sheet.show()
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

            Snackbar.make(binding.root, "Pago agregado a la lista", Snackbar.LENGTH_LONG).show()
        }
        dialog.setOnActionListener(actionListener)
        dialog.show(supportFragmentManager, "PagoFacturaDialog")
    }
}
