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

        if (Build.VERSION.SDK_INT < 35) {
            window.statusBarColor = ContextCompat.getColor(this, R.color.brand_primary_dark)
        }

        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        val disponible = intent.getDoubleExtra("disponible", 0.0)
        val saldo = intent.getDoubleExtra("saldo", 0.0)
        val limite = intent.getDoubleExtra("limite", 0.0)

        val fmt = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
        findViewById<TextView>(R.id.tvDisponible).text = "C$ ${fmt.format(disponible)}"
        findViewById<TextView>(R.id.tvSaldo).text = "C$ ${fmt.format(saldo)}"
        findViewById<TextView>(R.id.tvLimite).text = "C$ ${fmt.format(limite)}"

        setupCalculator()

        val tvMotivo = findViewById<TextView>(R.id.tvMontoMotivo)
        tvMotivo.setOnClickListener {
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
                    tvMotivo.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
                } else {
                    tvMotivo.text = "Escribe el motivo del Incremento"
                    tvMotivo.setTextColor(ContextCompat.getColor(this, R.color.available))
                }
                dialog.dismiss()
            }
            dialog.setOnDismissListener {
                val text = etMotivo.text?.toString()?.trim()
                if (text.isNullOrEmpty()) {
                    motivoText = ""
                    tvMotivo.text = "Escribe el motivo del Incremento"
                    tvMotivo.setTextColor(ContextCompat.getColor(this, R.color.available))
                }
            }
            dialog.show()
        }

        findViewById<MaterialButton>(R.id.btnEnviar).setOnClickListener {
            if (currentInput.isEmpty() || currentInput == "0" || currentInput == "0.") {
                Snackbar.make(findViewById(R.id.btnEnviar),
                    "Ingresa un monto vAlido", Snackbar.LENGTH_SHORT).show()
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

    private fun setupCalculator() {
        val display = findViewById<TextView>(R.id.tvMontoDisplay)
        updateDisplay()

        val keys = listOf(
            R.id.btnK0, R.id.btnK1, R.id.btnK2, R.id.btnK3, R.id.btnK4,
            R.id.btnK5, R.id.btnK6, R.id.btnK7, R.id.btnK8, R.id.btnK9,
            R.id.btnKDot, R.id.btnKDel
        )

        for (id in keys) {
            findViewById<Button>(id).setOnClickListener {
                val text = (it as? Button)?.text?.toString() ?: return@setOnClickListener
                when (text) {
                    "<" -> {
                        if (currentInput.isNotEmpty()) {
                            if (currentInput.last() == '.') hasDecimal = false
                            currentInput = currentInput.dropLast(1)
                        }
                    }
                    "." -> {
                        if (!hasDecimal) {
                            if (currentInput.isEmpty()) currentInput = "0."
                            else currentInput += "."
                            hasDecimal = true
                        }
                    }
                    else -> {
                        if (text == "0" && currentInput.isEmpty()) return@setOnClickListener
                        if (hasDecimal) {
                            val parts = currentInput.split(".")
                            if (parts.size == 2 && parts[1].length >= 2) return@setOnClickListener
                        }
                        if (currentInput.length >= 12) return@setOnClickListener
                        currentInput += text
                    }
                }
                updateDisplay()
            }
        }
    }

    private fun updateDisplay() {
        val display = findViewById<TextView>(R.id.tvMontoDisplay)
        if (currentInput.isEmpty()) {
            display.text = "C$ 0.00"
        } else {
            val value = currentInput.toDoubleOrNull() ?: 0.0
            val fmt = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
            display.text = "C$ ${fmt.format(value)}"
        }
    }
}