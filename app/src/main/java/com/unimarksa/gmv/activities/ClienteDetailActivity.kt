package com.unimarksa.gmv.activities

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.unimarksa.gmv.R
import com.unimarksa.gmv.adapters.FacturasAdapter
import com.unimarksa.gmv.databinding.ActivityClienteDetailBinding
import com.unimarksa.gmv.models.ItemCliente
import com.unimarksa.gmv.models.ItemFacturaPendiente
import com.unimarksa.gmv.tools.ApiService
import com.unimarksa.gmv.tools.CartManager

class ClienteDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityClienteDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityClienteDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.nestedContent) { v, insets ->
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

        val cliente = intent.getSerializableExtra("cliente") as? ItemCliente
        cliente?.let { bindData(it) }

        val codClient = cliente?.codCliente ?: ""
        val namClient = cliente?.nombre ?: ""

        binding.btnListArticulos.setOnClickListener {
            cliente?.let {
                val esMoroso = it.moroso.equals("S", ignoreCase = true) || it.moroso == "1" || it.moroso.equals("SI", ignoreCase = true)
                if (esMoroso) {
                    val titleHtml = "<font color='#FF0000'>Cliente en Estado de Morosidad</font>"
                    AlertDialog.Builder(this)
                        .setTitle(Html.fromHtml(titleHtml, Html.FROM_HTML_MODE_LEGACY))
                        .setMessage("No se le permite crear pedido.")
                        .setPositiveButton("OK", null)
                        .show()
                    return@setOnClickListener
                }
                val saldo = it.saldo.replace(",", "").replace("C$", "").trim().toDoubleOrNull() ?: 0.0
                val limite = it.limite.replace(",", "").replace("C$", "").trim().toDoubleOrNull() ?: 1.0
                val disponibleReal = limite - saldo - CartManager.subtotal
                if (disponibleReal < 0.0) {
                    val titleHtml = "<font color='#FF0000'>Límite de crédito excedido</font>"
                    AlertDialog.Builder(this)
                        .setTitle(Html.fromHtml(titleHtml, Html.FROM_HTML_MODE_LEGACY))
                        .setMessage("El cliente no tiene disponible para realizar un pedido.")
                        .setPositiveButton("OK", null)
                        .show()
                    return@setOnClickListener
                }
                CartManager.switchToClient(it)
                startActivity(Intent(this, CatalogoActivity::class.java))
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        }

        binding.idSearchLotes.setOnClickListener {
            startActivity(Intent(this, HistorialLotesActivity::class.java)
                .putExtra("codCliente", cliente?.codCliente ?: ""))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        binding.btnReciboColector.setOnClickListener {
            startActivity(Intent(this, ListadoPagosActivity::class.java)
                .putExtra("codCliente", cliente?.codCliente ?: "")
                .putExtra("nomCliente", cliente?.nombre ?: ""))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

    

        binding.idHistoryLast3m.setOnClickListener {
            startActivity(Intent(this, HistoricoFacturaActivity::class.java)
                .putExtra("codCliente", cliente?.codCliente ?: "")
                .putExtra("nomCliente", cliente?.nombre ?: "")
                .putExtra("codTexto", "${cliente?.codCliente ?: ""} · ${cliente?.direccion?.take(30) ?: ""}"))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        setupTabs()
    }

    private fun bindData(cliente: ItemCliente) {
        binding.idPerfilNameCliente.text = "${cliente.codCliente} - ${cliente.nombre}"
        binding.idTele.text = "Tel. ${cliente.telefono}"
        binding.txtDireccion.text = cliente.direccion
        binding.idCondicionPago.text = cliente.condPago.ifEmpty { "Contado" }

        val saldo = cliente.saldo.replace(",", "").replace("C$", "").trim().toDoubleOrNull() ?: 0.0
        val limite = cliente.limite.replace(",", "").replace("C$", "").trim().toDoubleOrNull() ?: 1.0
        val pedido = CartManager.subtotal

        val disponible = (limite - saldo - pedido).coerceAtLeast(0.0)
        val fmtNum = { v: Double -> "C$ ${"%,.2f".format(v)}" }

        binding.idPerfilDisponible.text = fmtNum(disponible)
        binding.idPerfilSaldo.text = fmtNum(saldo)
        binding.idPerfilPedido.text = fmtNum(pedido)
        binding.idPerfilLimite.text = fmtNum(limite)
        binding.idProgressCredito.progress = ((saldo / limite) * 100).toInt().coerceIn(0, 100)
        binding.btnSolicitarIncremento.setOnClickListener {
            val intent = Intent(this, com.unimarksa.gmv.activities.SolicitarIncrementoActivity::class.java).apply {
                putExtra("disponible", disponible)
                putExtra("saldo", saldo)
                putExtra("limite", limite)
                putExtra("clienteNombre", cliente.nombre)
                putExtra("codCliente", cliente.codCliente)
            }
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        val esMoroso = cliente.moroso.equals("S", ignoreCase = true) || cliente.moroso == "1" || cliente.moroso.equals("SI", ignoreCase = true)
        if (esMoroso) {
            binding.idBadgeEstado.text = "MOROSO"
            binding.idBadgeEstado.setBackgroundResource(R.drawable.bg_badge_red)
            binding.idBadgeEstado.setTextColor(ContextCompat.getColor(this, R.color.white))
        }

        loadPerfilCliente(cliente.codCliente)
    }

    private fun loadPerfilCliente(codCliente: String) {
        ApiService.fetchPerfilCliente(
            codCliente = codCliente,
            onResult = { perfil ->
                setupMoraRows(perfil)
                setupFacturas(perfil)
            },
            onError = { }
        )
    }

    private fun setupMoraRows(perfil: com.unimarksa.gmv.models.ItemPerfilCliente) {
        val colors = listOf(
            Triple(
                ContextCompat.getColor(this, R.color.mora_ok),
                ContextCompat.getColor(this, R.color.mora_ok_bg),
                ContextCompat.getColor(this, R.color.mora_ok)
            ),
            Triple(
                ContextCompat.getColor(this, R.color.mora_warn),
                ContextCompat.getColor(this, R.color.mora_warn_bg),
                ContextCompat.getColor(this, R.color.mora_warn)
            ),
            Triple(
                ContextCompat.getColor(this, R.color.mora_60),
                ContextCompat.getColor(this, R.color.mora_60_bg),
                ContextCompat.getColor(this, R.color.mora_60)
            ),
            Triple(
                ContextCompat.getColor(this, R.color.mora_90),
                ContextCompat.getColor(this, R.color.mora_90_bg),
                ContextCompat.getColor(this, R.color.mora_90)
            ),
            Triple(
                ContextCompat.getColor(this, R.color.mora_danger),
                ContextCompat.getColor(this, R.color.mora_danger_bg),
                ContextCompat.getColor(this, R.color.mora_danger)
            ),
            Triple(
                ContextCompat.getColor(this, R.color.mora_danger),
                ContextCompat.getColor(this, R.color.mora_danger_bg),
                ContextCompat.getColor(this, R.color.mora_danger)
            )
        )

        fun parseAmount(raw: String): Double {
            return raw.replace(",", "").trim().toDoubleOrNull() ?: 0.0
        }

        val rows = listOf(
            Triple(binding.rowNoVencido.root, "No vencido", parseAmount(perfil.noVencidos)),
            Triple(binding.rowD30.root, "30 d\u00edas", parseAmount(perfil.dias30)),
            Triple(binding.rowD60.root, "60 d\u00edas", parseAmount(perfil.dias60)),
            Triple(binding.rowD90.root, "90 d\u00edas", parseAmount(perfil.dias90)),
            Triple(binding.rowD120.root, "120 d\u00edas", parseAmount(perfil.dias120)),
            Triple(binding.rowM120.root, "M\u00e1s de 120 d\u00edas", parseAmount(perfil.mas120))
        )

        rows.forEachIndexed { index, (view, label, amount) ->
            val (dot, chipBg, chipText) = colors[index]
            view.findViewById<View>(R.id.mora_dot).backgroundTintList = ColorStateList.valueOf(dot)
            view.findViewById<TextView>(R.id.tv_mora_label).text = label
            val chip = view.findViewById<TextView>(R.id.tv_mora_amount)
            chip.text = "C$ ${"%,.2f".format(amount)}"
            chip.backgroundTintList = ColorStateList.valueOf(chipBg)
            chip.setTextColor(chipText)
        }
    }

    private fun setupFacturas(perfil: com.unimarksa.gmv.models.ItemPerfilCliente) {
        val facturas = ApiService.parseFacturasPendientes(perfil.factPend)
        if (facturas.isEmpty()) {
            binding.lytEmptyResult.root.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        } else {
            binding.lytEmptyResult.root.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
            binding.recyclerView.layoutManager = LinearLayoutManager(this)
            binding.recyclerView.adapter = FacturasAdapter(facturas) { factura ->
                val intent = Intent(this, FacturaDetailActivity::class.java)
                intent.putExtra("factura", factura)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab) {
                when (tab.position) {
                    0 -> {
                        binding.lytFacturas.visibility = View.VISIBLE
                        binding.lytSaldos.visibility = View.GONE
                    }
                    1 -> {
                        binding.lytFacturas.visibility = View.GONE
                        binding.lytSaldos.visibility = View.VISIBLE
                    }
                }
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
        })
    }
}