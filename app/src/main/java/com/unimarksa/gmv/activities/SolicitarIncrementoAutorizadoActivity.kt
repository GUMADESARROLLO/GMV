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

        findViewById<TextView>(R.id.tv_monto).text = "C$ ${fmt.format(monto)}"
        findViewById<TextView>(R.id.tvDisponibleView).text = "C$ ${fmt.format(disponible)}"
        findViewById<TextView>(R.id.tvSaldoView).text = "C$ ${fmt.format(saldo)}"
        findViewById<TextView>(R.id.tvLimiteView).text = "C$ ${fmt.format(limite)}"

        if (motivo.isNotEmpty()) {
            findViewById<TextView>(R.id.tvMotivoView).text = "$motivo"
        }

        val initial = clienteNombre.firstOrNull()?.uppercaseChar()?.toString() ?: "C"
        findViewById<TextView>(R.id.tvAvatarCliente).text = initial
        findViewById<TextView>(R.id.tvClienteNombre).text = clienteNombre.ifEmpty { "Cliente" }
        findViewById<TextView>(R.id.tvClienteCodigo).text = clienteCodigo

        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        findViewById<MaterialButton>(R.id.btn_enviar).setOnClickListener {
            if (isLoading) return@setOnClickListener
            isLoading = true

            val btn = findViewById<MaterialButton>(R.id.btn_enviar)
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