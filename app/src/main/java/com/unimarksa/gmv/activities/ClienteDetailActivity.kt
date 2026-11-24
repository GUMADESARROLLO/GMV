package com.unimarksa.gmv.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.unimarksa.gmv.R
import com.unimarksa.gmv.tools.BarrasSistema
import com.unimarksa.gmv.adapters.FacturasAdapter
import com.unimarksa.gmv.databinding.ActivityClienteDetailBinding
import com.unimarksa.gmv.models.ItemCliente
import com.unimarksa.gmv.models.ItemFacturaPendiente
import com.unimarksa.gmv.models.ItemPagoFactura
import com.unimarksa.gmv.tools.ApiService
import com.unimarksa.gmv.tools.CartManager
import com.unimarksa.gmv.tools.PagoManager

class ClienteDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityClienteDetailBinding
    private var facturasAdapter: FacturasAdapter? = null
    private var numeroTelefono: String? = null
    private var clienteActual: ItemCliente? = null

    // El botón/gesto "atrás" del sistema primero cancela la selección
    private val atrasSeleccion = object : androidx.activity.OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = clearSelection()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityClienteDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.nestedContent) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            // La barra de selección queda por encima de la barra de navegación
            val barra = binding.lytBarraSeleccion
            val base = (12 * resources.displayMetrics.density).toInt()
            barra.setPadding(barra.paddingLeft, base, barra.paddingRight, base + systemBars.bottom)
            insets
        }
        onBackPressedDispatcher.addCallback(this, atrasSeleccion)

        BarrasSistema.aplicar(this, R.color.umk_mist)


        binding.btnBack.setOnClickListener {
            if (facturasAdapter?.selectedCount ?: 0 > 0) {
                clearSelection()
            } else {
                onBackPressedDispatcher.onBackPressed()
            }
        }

        findViewById<ImageButton>(R.id.btn_close_selection).setOnClickListener {
            clearSelection()
        }

        binding.btnSendSelection.setOnClickListener {
            val selected = facturasAdapter?.selectedItems ?: emptyList()
            if (selected.isNotEmpty()) {
                enviarSeleccion(selected)
            }
        }

        val cliente = intent.getSerializableExtra("cliente") as? ItemCliente
        clienteActual = cliente
        cliente?.let { bindData(it) }

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
        binding.idPerfilNameCliente.text = cliente.nombre
        binding.tvCodCliente.text = cliente.codCliente
        binding.tvAvatarCliente.text = iniciales(cliente.nombre)
        binding.idTele.text = cliente.telefono.ifBlank { "Sin teléfono" }
        binding.txtDireccion.text = cliente.direccion
        binding.idCondicionPago.text = cliente.condPago.ifEmpty { "Contado" }
        val esMoroso = cliente.moroso.equals("S", true) || cliente.moroso == "1" || cliente.moroso.equals("SI", true)
        binding.tvChipMoroso.visibility = if (esMoroso) View.VISIBLE else View.GONE

        // Tocar el teléfono abre el marcador con el primer número válido
        val numero = Regex("[0-9][0-9 -]{6,}[0-9]").find(cliente.telefono)?.value?.filter { it.isDigit() }
        numeroTelefono = numero
        if (numero != null) {
            binding.rowTelefono.contentDescription = "Llamar al $numero"
            binding.rowTelefono.setOnClickListener {
                startActivity(Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:$numero")))
            }
        } else {
            binding.rowTelefono.visibility = View.GONE
        }

        val saldo = cliente.saldo.replace(",", "").replace("C$", "").trim().toDoubleOrNull() ?: 0.0
        val limite = cliente.limite.replace(",", "").replace("C$", "").trim().toDoubleOrNull() ?: 1.0
        val pedido = CartManager.subtotal

        val disponible = (limite - saldo - pedido).coerceAtLeast(0.0)
        val fmtNum = { v: Double -> "C$ ${"%,.2f".format(v)}" }

        binding.idPerfilDisponible.text = fmtNum(disponible)
        binding.idPerfilSaldo.text = fmtNum(saldo)
        binding.idPerfilPedido.text = fmtNum(pedido)
        binding.idPerfilLimite.text = fmtNum(limite)

        val base = if (limite > 0) limite else 1.0
        val pctUsado = (((saldo + pedido) / base) * 100).toInt().coerceIn(0, 100)
        val pctSaldo = ((saldo / base) * 100).toInt().coerceIn(0, 100)
        binding.tvSaldoPct.text = "$pctSaldo% del límite"
        binding.tvChipUso.text = "$pctUsado% usado"
        binding.tvCreditoUsado.text = "del límite"
        val (chipBg, chipTxt) = when {
            pctUsado >= 90 -> R.color.status_cancel_bg to R.color.status_cancel_text
            pctUsado >= 70 -> R.color.status_pending_bg to R.color.status_pending_text
            else -> R.color.status_active_bg to R.color.status_active_text
        }
        binding.tvChipUso.backgroundTintList = ContextCompat.getColorStateList(this, chipBg)
        binding.tvChipUso.setTextColor(ContextCompat.getColor(this, chipTxt))
        pintarBarraCredito(saldo.coerceAtLeast(0.0), pedido.coerceAtLeast(0.0), disponible)

        binding.tvAumentoDetalle.text = if (pctUsado >= 70)
            "Usa el $pctUsado% de su límite. Pide un límite mayor."
        else
            "Pide un límite mayor para este cliente."

        binding.btnSolicitarIncremento.setOnClickListener {
            val intent = Intent(this, SolicitarIncrementoActivity::class.java).apply {
                putExtra("disponible", disponible)
                putExtra("saldo", saldo)
                putExtra("limite", limite)
                putExtra("clienteNombre", cliente.nombre)
                putExtra("codCliente", cliente.codCliente)
            }
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
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

    /** Pesos de la barra segmentada; un segmento en cero se oculta para no dejar huecos. */
    private fun pintarBarraCredito(saldo: Double, pedido: Double, disponible: Double) {
        val total = (saldo + pedido + disponible).takeIf { it > 0 } ?: 1.0
        listOf(
            binding.segSaldo to saldo,
            binding.segPedido to pedido,
            binding.segDisponible to disponible
        ).forEach { (vista, valor) ->
            val lp = vista.layoutParams as android.widget.LinearLayout.LayoutParams
            lp.weight = (valor / total).toFloat()
            vista.visibility = if (valor > 0) View.VISIBLE else View.GONE
            vista.layoutParams = lp
        }
        // El primer segmento visible no lleva separación a la izquierda
        var primero = true
        listOf(binding.segSaldo, binding.segPedido, binding.segDisponible).forEach { v ->
            if (v.visibility == View.VISIBLE) {
                val lp = v.layoutParams as android.widget.LinearLayout.LayoutParams
                lp.marginStart = if (primero) 0 else (4 * resources.displayMetrics.density).toInt()
                v.layoutParams = lp
                primero = false
            }
        }
    }

    private fun iniciales(nombre: String): String =
        nombre.split(" ", "-", ".").filter { it.isNotBlank() && it.first().isLetter() }
            .take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "CL" }

    private fun setupMoraRows(perfil: com.unimarksa.gmv.models.ItemPerfilCliente) {
        val colors = listOf(
            Triple(ContextCompat.getColor(this, R.color.mora_ok), ContextCompat.getColor(this, R.color.mora_ok_bg), ContextCompat.getColor(this, R.color.mora_ok)),
            Triple(ContextCompat.getColor(this, R.color.mora_warn), ContextCompat.getColor(this, R.color.mora_warn_bg), ContextCompat.getColor(this, R.color.mora_warn)),
            Triple(ContextCompat.getColor(this, R.color.mora_60), ContextCompat.getColor(this, R.color.mora_60_bg), ContextCompat.getColor(this, R.color.mora_60)),
            Triple(ContextCompat.getColor(this, R.color.mora_90), ContextCompat.getColor(this, R.color.mora_90_bg), ContextCompat.getColor(this, R.color.mora_90)),
            Triple(ContextCompat.getColor(this, R.color.mora_danger), ContextCompat.getColor(this, R.color.mora_danger_bg), ContextCompat.getColor(this, R.color.mora_danger)),
            Triple(ContextCompat.getColor(this, R.color.mora_danger), ContextCompat.getColor(this, R.color.mora_danger_bg), ContextCompat.getColor(this, R.color.mora_danger))
        )
        fun parseAmount(raw: String): Double = raw.replace(",", "").trim().toDoubleOrNull() ?: 0.0
        val rows = listOf(
            Triple(binding.rowNoVencido.root, "No vencido", parseAmount(perfil.noVencidos)),
            Triple(binding.rowD30.root, "30 d\u00edas", parseAmount(perfil.dias30)),
            Triple(binding.rowD60.root, "60 d\u00edas", parseAmount(perfil.dias60)),
            Triple(binding.rowD90.root, "90 d\u00edas", parseAmount(perfil.dias90)),
            Triple(binding.rowD120.root, "120 d\u00edas", parseAmount(perfil.dias120)),
            Triple(binding.rowM120.root, "M\u00e1s de 120 d\u00edas", parseAmount(perfil.mas120))
        )
        val vencido = rows.drop(1).sumOf { it.third }
        binding.tvPerfilVencido.text = "C$ ${"%,.2f".format(vencido)}"
        binding.tvPerfilVencido.setTextColor(ContextCompat.getColor(this, if (vencido > 0) R.color.umk_danger else R.color.umk_ink))
        binding.tvVencidoSub.text = if (vencido > 0) "Más de 30 días" else "Sin mora"

        rows.forEachIndexed { index, (view, label, amount) ->
            val (dot, chipBg, chipText) = colors[index]
            view.findViewById<View>(R.id.mora_dot).backgroundTintList = android.content.res.ColorStateList.valueOf(dot)
            view.findViewById<TextView>(R.id.tv_mora_label).text = label
            val chip = view.findViewById<TextView>(R.id.tv_mora_amount)
            chip.text = "C$ ${"%,.2f".format(amount)}"
            chip.backgroundTintList = android.content.res.ColorStateList.valueOf(chipBg)
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
            binding.tvHintSeleccion.visibility = if (facturas.size > 1) View.VISIBLE else View.GONE
            binding.recyclerView.layoutManager = LinearLayoutManager(this)
            facturasAdapter = FacturasAdapter(facturas).apply {
                onItemClick = { factura ->
                    val intent = Intent(this@ClienteDetailActivity, FacturaDetailActivity::class.java)
                    intent.putExtra("factura", factura)
                    startActivity(intent)
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                }
                onSelectionChanged = { count, total ->
                    if (count > 0) {
                        showSelectionToolbar(count, total)
                    } else {
                        hideSelectionToolbar()
                    }
                }
            }
            binding.recyclerView.adapter = facturasAdapter
        }
    }

    private fun showSelectionToolbar(count: Int, total: Double) {
        binding.tvSelectionInfo.text = "$count factura${if (count == 1) "" else "s"} seleccionada${if (count == 1) "" else "s"}"
        binding.tvSelectionTotal.text = "C$ ${"%,.2f".format(total)}"
        atrasSeleccion.isEnabled = true
        val barra = binding.lytBarraSeleccion
        if (barra.visibility != View.VISIBLE) {
            barra.visibility = View.VISIBLE
            if (android.animation.ValueAnimator.areAnimatorsEnabled()) {
                barra.post {
                    barra.translationY = barra.height.toFloat()
                    barra.animate().translationY(0f).setDuration(220)
                        .setInterpolator(android.view.animation.DecelerateInterpolator()).start()
                }
            }
        }
        // Espacio extra al final del scroll para que la barra no tape la última factura
        binding.nestedContent.post {
            val extra = barra.height
            val sb = ViewCompat.getRootWindowInsets(binding.nestedContent)
                ?.getInsets(WindowInsetsCompat.Type.systemBars())?.bottom ?: 0
            binding.nestedContent.setPadding(
                binding.nestedContent.paddingLeft, binding.nestedContent.paddingTop,
                binding.nestedContent.paddingRight, maxOf(extra, sb)
            )
        }
    }

    private fun hideSelectionToolbar() {
        atrasSeleccion.isEnabled = false
        val barra = binding.lytBarraSeleccion
        if (barra.visibility == View.VISIBLE) {
            if (android.animation.ValueAnimator.areAnimatorsEnabled()) {
                barra.animate().translationY(barra.height.toFloat()).setDuration(180)
                    .withEndAction { barra.visibility = View.GONE; barra.translationY = 0f }.start()
            } else {
                barra.visibility = View.GONE
            }
        }
        val sb = ViewCompat.getRootWindowInsets(binding.nestedContent)
            ?.getInsets(WindowInsetsCompat.Type.systemBars())?.bottom ?: 0
        binding.nestedContent.setPadding(
            binding.nestedContent.paddingLeft, binding.nestedContent.paddingTop,
            binding.nestedContent.paddingRight, sb
        )
    }

    private fun clearSelection() {
        facturasAdapter?.clearSelection()
        hideSelectionToolbar()
    }

    /**
     * Cobra de contado las facturas seleccionadas: cada una entra como pago "contado" por su saldo.
     * Antes tomaba el cliente de extras que el perfil nunca recibe (codCliente/nomCliente), así que
     * los pagos llegaban sin cliente; y vaciaba en silencio los pagos ya registrados.
     */
    private fun enviarSeleccion(items: List<ItemFacturaPendiente>) {
        fun cargar(reemplazar: Boolean) {
            if (reemplazar) PagoManager.clear()
            val yaCargadas = PagoManager.pagos.map { it.numero }.toSet()
            items.filter { it.numero !in yaCargadas }.forEach { factura ->
                val saldo = factura.saldo.replace(",", "").trim().toDoubleOrNull() ?: 0.0
                PagoManager.add(ItemPagoFactura(
                    numero = factura.numero,
                    nc = 0.0,
                    retencion = 0.0,
                    descuento = 0.0,
                    valorRecibido = saldo,
                    saldo = saldo,
                    action = "contado",
                    estado = "pagado"
                ))
            }
            startActivity(Intent(this, ListadoPagosActivity::class.java)
                .putExtra("codCliente", clienteActual?.codCliente.orEmpty())
                .putExtra("nomCliente", clienteActual?.nombre.orEmpty()))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            clearSelection()
        }

        val previos = PagoManager.count
        if (previos == 0) { cargar(reemplazar = false); return }
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Ya hay pagos registrados")
            .setMessage("Tienes $previos pago${if (previos == 1) "" else "s"} sin enviar. ¿Agregar estas facturas o reemplazarlos?")
            .setPositiveButton("Agregar") { _, _ -> cargar(reemplazar = false) }
            .setNegativeButton("Reemplazar") { _, _ -> cargar(reemplazar = true) }
            .setNeutralButton("Cancelar", null)
            .show()
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                if (facturasAdapter?.selectedCount ?: 0 > 0) {
                    clearSelection()
                }
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
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }
}
