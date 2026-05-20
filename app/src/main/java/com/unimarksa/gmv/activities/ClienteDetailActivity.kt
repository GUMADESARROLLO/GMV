package com.unimarksa.gmv.activities

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.unimarksa.gmv.R
import com.unimarksa.gmv.databinding.ActivityClienteDetailBinding
import com.unimarksa.gmv.models.ItemCliente

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

        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val cliente = intent.getSerializableExtra("cliente") as? ItemCliente
        cliente?.let { bindData(it) }
        setupMoraRows()
    }

    private fun bindData(cliente: ItemCliente) {
        binding.idPerfilNameCliente.text = cliente.nombre
        binding.idTele.text = "Tel. ${cliente.telefono}"
        binding.txtDireccion.text = cliente.direccion
        binding.idPerfilDisponible.text = "C$ ${cliente.disponible}"
        binding.idPerfilSaldo.text = "C$ ${cliente.saldo}"
        binding.idPerfilLimite.text = "C$ ${cliente.limite}"

        val saldo = cliente.saldo.replace(",", "").replace("C$", "").trim().toDoubleOrNull() ?: 0.0
        val limite = cliente.limite.replace(",", "").replace("C$", "").trim().toDoubleOrNull() ?: 1.0
        binding.idProgressCredito.progress = ((saldo / limite) * 100).toInt().coerceIn(0, 100)
    }

    private fun setupMoraRows() {
        val colorOk = ContextCompat.getColor(this, R.color.mora_ok)
        val colorWarn = ContextCompat.getColor(this, R.color.mora_warn)
        val colorDanger = ContextCompat.getColor(this, R.color.mora_danger)
        val colorOkBg = ContextCompat.getColor(this, R.color.mora_ok_bg)
        val colorWarnBg = ContextCompat.getColor(this, R.color.mora_warn_bg)
        val colorDangerBg = ContextCompat.getColor(this, R.color.mora_danger_bg)

        fun moraColor(amount: Double) = when {
            amount <= 0.0 -> Triple(colorOk, colorOkBg, colorOk)
            amount < 500.0 -> Triple(colorWarn, colorWarnBg, colorWarn)
            else -> Triple(colorDanger, colorDangerBg, colorDanger)
        }

        fun setMoraRow(include: View, label: String, amount: Double) {
            val (dot, chipBg, chipText) = moraColor(amount)
            val dotView = include.findViewById<View>(R.id.mora_dot)
            dotView.backgroundTintList = ColorStateList.valueOf(dot)
            include.findViewById<TextView>(R.id.tv_mora_label).text = label
            val chip = include.findViewById<TextView>(R.id.tv_mora_amount)
            chip.text = "C$ ${"%.2f".format(amount)}"
            chip.backgroundTintList = ColorStateList.valueOf(chipBg)
            chip.setTextColor(chipText)
        }

        val rows = listOf(
            binding.rowNoVencido to "No vencido",
            binding.rowD30 to "30 d\u00edas",
            binding.rowD60 to "60 d\u00edas",
            binding.rowD90 to "90 d\u00edas",
            binding.rowD120 to "120 d\u00edas",
            binding.rowM120 to "M\u00e1s de 120 d\u00edas"
        )

        rows.forEach { (view, label) -> setMoraRow(view.root, label, 0.0) }
    }
}
