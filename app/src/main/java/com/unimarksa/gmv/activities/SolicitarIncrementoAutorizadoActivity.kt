package com.unimarksa.gmv.activities

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.unimarksa.gmv.R
import com.unimarksa.gmv.tools.ApiService
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class SolicitarIncrementoAutorizadoActivity : AppCompatActivity() {

    private var isLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_solicitar_incremento_autorizado)

        if (Build.VERSION.SDK_INT < 35) {
            window.statusBarColor = ContextCompat.getColor(this, R.color.brand_primary_dark)
        }

        val monto = intent.getDoubleExtra("monto", 0.0)
        val disponible = intent.getDoubleExtra("disponible", 0.0)
        val saldo = intent.getDoubleExtra("saldo", 0.0)
        val limite = intent.getDoubleExtra("limite", 0.0)
        val motivo = intent.getStringExtra("motivo") ?: ""
        val clienteNombre = intent.getStringExtra("clienteNombre") ?: ""
        val clienteCodigo = intent.getStringExtra("clienteCodigo") ?: ""

        val fmt = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))

        findViewById<TextView>(R.id.tvMonto).text = "C$ ${fmt.format(monto)}"
        findViewById<TextView>(R.id.tvDisponible).text = "C$ ${fmt.format(disponible)}"
        findViewById<TextView>(R.id.tvSaldo).text = "C$ ${fmt.format(saldo)}"
        findViewById<TextView>(R.id.tvLimite).text = "C$ ${fmt.format(limite)}"

        if (motivo.isNotEmpty()) {
            findViewById<TextView>(R.id.tvNota).text = "$motivo"
        }

        val initial = clienteNombre.firstOrNull()?.uppercaseChar()?.toString() ?: "C"
        findViewById<TextView>(R.id.tvAvatar).text = initial
        findViewById<TextView>(R.id.tvNombreCliente).text = clienteNombre.ifEmpty { "Cliente" }
        findViewById<TextView>(R.id.tvIdCliente).text = clienteCodigo

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<MaterialButton>(R.id.btnEnviar).setOnClickListener {
            if (isLoading) return@setOnClickListener
            isLoading = true

            val btn = findViewById<MaterialButton>(R.id.btnEnviar)
            btn.text = "Enviando..."
            btn.isEnabled = false

            ApiService.enviarIncremento(
                monto = monto,
                disponible = disponible,
                saldo = saldo,
                limite = limite,
                motivo = motivo,
                clienteNombre = clienteNombre,
                clienteCodigo = clienteCodigo,
                onSuccess = {
                    runOnUiThread {
                        AlertDialog.Builder(this)
                            .setTitle("Solicitud enviada")
                            .setMessage("La solicitud de incremento fue enviada correctamente.")
                            .setPositiveButton("OK") { _, _ -> finish() }
                            .setCancelable(false)
                            .show()
                    }
                },
                onError = { error ->
                    runOnUiThread {
                        isLoading = false
                        btn.text = "Enviar solicitud"
                        btn.isEnabled = true
                        Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show()
                    }
                }
            )
        }
    }
}