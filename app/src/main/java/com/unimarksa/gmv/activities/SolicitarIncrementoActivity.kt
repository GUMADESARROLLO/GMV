package com.unimarksa.gmv.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.unimarksa.gmv.R
import com.unimarksa.gmv.tools.BarrasSistema
import com.unimarksa.gmv.tools.MetricaTile
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class SolicitarIncrementoActivity : AppCompatActivity() {

    private var currentInput = ""
    private var hasDecimal = false
    private var motivoText = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_solicitar_incremento)

        BarrasSistema.aplicar(this, R.color.umk_mist)

        findViewById<android.view.View>(R.id.btn_back).setOnClickListener { finish() }
        findViewById<TextView>(R.id.tvClienteSub).text =
            listOf(intent.getStringExtra("codCliente"), intent.getStringExtra("clienteNombre"))
                .filter { !it.isNullOrBlank() }.joinToString(" · ").ifEmpty { "Cliente" }

        val disponible = intent.getDoubleExtra("disponible", 0.0)
        val saldo = intent.getDoubleExtra("saldo", 0.0)
        val limite = intent.getDoubleExtra("limite", 0.0)

        val fmt = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
        MetricaTile.llenarCredito(findViewById(android.R.id.content), limite, saldo, disponible) { "C$ ${fmt.format(it)}" }

        val tvBadge = findViewById<TextView>(R.id.tvBadgeExceso)

        setupCalculator(limite, tvBadge)

        val tvMotivo = findViewById<TextView>(R.id.tvMontoMotivo)
        findViewById<android.view.View>(R.id.rowMotivo).setOnClickListener {
            val dialog = BottomSheetDialog(this)
            val view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_motivo, null)
            dialog.setContentView(view)

            val etMotivo = view.findViewById<EditText>(R.id.etMotivo)
            etMotivo.setText(motivoText)
            etMotivo.requestFocus()

            view.findViewById<TextView>(R.id.btnAceptar).setOnClickListener {
                motivoText = etMotivo.text?.toString()?.trim() ?: ""
                if (motivoText.isNotEmpty()) {
                    tvMotivo.text = motivoText
                    tvMotivo.setTextColor(ContextCompat.getColor(this, R.color.umk_ink))
                    findViewById<TextView>(R.id.tvMotivoAccion).text = "Editar"
                } else {
                    tvMotivo.text = PLACEHOLDER_MOTIVO
                    tvMotivo.setTextColor(ContextCompat.getColor(this, R.color.umk_ink_muted))
                    findViewById<TextView>(R.id.tvMotivoAccion).text = "Escribir"
                }
                dialog.dismiss()
            }
            dialog.setOnDismissListener {
                val text = etMotivo.text?.toString()?.trim()
                if (text.isNullOrEmpty()) {
                    motivoText = ""
                    tvMotivo.text = PLACEHOLDER_MOTIVO
                    tvMotivo.setTextColor(ContextCompat.getColor(this, R.color.umk_ink_muted))
                    findViewById<TextView>(R.id.tvMotivoAccion).text = "Escribir"
                }
            }
            dialog.show()
        }

        findViewById<MaterialButton>(R.id.btnEnviar).setOnClickListener {
            if (currentInput.isEmpty() || currentInput == "0" || currentInput == "0.") {
                Snackbar.make(findViewById(R.id.btnEnviar),
                    "Ingresa un monto válido", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val montoNum = currentInput.toDoubleOrNull() ?: 0.0
            if (montoNum <= limite) {
                Snackbar.make(findViewById(R.id.btnEnviar),
                    "El nuevo límite debe ser mayor al actual", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (motivoText.isEmpty()) {
                Snackbar.make(findViewById(R.id.btnEnviar),
                    "Indica el motivo del incremento", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(this, SolicitarIncrementoAutorizadoActivity::class.java).apply {
                putExtra("monto", currentInput.toDoubleOrNull() ?: 0.0)
                putExtra("disponible", disponible)
                putExtra("saldo", saldo)
                putExtra("limite", limite)
                putExtra("motivo", motivoText)
                putExtra("clienteNombre", intent.getStringExtra("clienteNombre") ?: "")
                putExtra("clienteCodigo", intent.getStringExtra("codCliente") ?: "")
            }
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    private fun setupCalculator(limite: Double, tvBadge: TextView) {
        val display = findViewById<TextView>(R.id.tvMontoDisplay)
        val fmt = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
        updateDisplay(limite, tvBadge, fmt)

        val keys = mapOf(
            R.id.btnK0 to "0", R.id.btnK1 to "1", R.id.btnK2 to "2", R.id.btnK3 to "3",
            R.id.btnK4 to "4", R.id.btnK5 to "5", R.id.btnK6 to "6", R.id.btnK7 to "7",
            R.id.btnK8 to "8", R.id.btnK9 to "9", R.id.btnKDot to "."
        )

        for ((id, digit) in keys) {
            findViewById<Button>(id).setOnClickListener {
                when (digit) {
                    "." -> {
                        if (!hasDecimal) {
                            if (currentInput.isEmpty()) currentInput = "0."
                            else currentInput += "."
                            hasDecimal = true
                        }
                    }
                    else -> {
                        if (digit == "0" && currentInput.isEmpty()) return@setOnClickListener
                        if (hasDecimal) {
                            val parts = currentInput.split(".")
                            if (parts.size == 2 && parts[1].length >= 2) return@setOnClickListener
                        }
                        if (currentInput.length >= 12) return@setOnClickListener
                        currentInput += digit
                    }
                }
                updateDisplay(limite, tvBadge, fmt)
            }
        }

        findViewById<Button>(R.id.btnKDel).setOnClickListener {
            if (currentInput.isNotEmpty()) {
                if (currentInput.last() == '.') hasDecimal = false
                currentInput = currentInput.dropLast(1)
                updateDisplay(limite, tvBadge, fmt)
            }
        }
    }

    private fun updateDisplay(limite: Double, tvBadge: TextView, fmt: DecimalFormat) {
        val display = findViewById<TextView>(R.id.tvMontoDisplay)
        val badge = findViewById<android.view.View>(R.id.badge)
        val icono = findViewById<android.view.View>(R.id.ivBadge)
        val value = currentInput.toDoubleOrNull() ?: 0.0
        display.text = "C$ ${fmt.format(value)}"
        val exceso = value - limite
        val (texto, fondo, color, conIcono) = when {
            currentInput.isEmpty() -> Quad("Ingresa el nuevo límite", R.color.umk_mist, R.color.umk_ink_muted, false)
            exceso > 0 -> Quad("Nuevo límite válido", R.color.umk_success_tint, R.color.umk_success, true)
            else -> Quad("Debe ser mayor a C$ ${fmt.format(limite)}", R.color.status_pending_bg, R.color.status_pending_text, false)
        }
        MetricaTile.llenarIncremento(findViewById(R.id.tileIncremento), value, limite) { "C$ ${fmt.format(it)}" }
        tvBadge.text = texto
        tvBadge.setTextColor(ContextCompat.getColor(this, color))
        badge.backgroundTintList = ContextCompat.getColorStateList(this, fondo)
        icono.visibility = if (conIcono) android.view.View.VISIBLE else android.view.View.GONE
    }

    private data class Quad(val texto: String, val fondo: Int, val color: Int, val icono: Boolean)

    private companion object {
        const val PLACEHOLDER_MOTIVO = "Toca para escribir el motivo"
    }
}
