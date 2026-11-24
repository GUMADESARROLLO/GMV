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
import com.unimarksa.gmv.tools.BarrasSistema
import com.unimarksa.gmv.tools.MetricaTile
import com.unimarksa.gmv.views.DeslizarParaConfirmarView
import com.unimarksa.gmv.tools.ApiService
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class SolicitarIncrementoAutorizadoActivity : AppCompatActivity() {

    private var isLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_solicitar_incremento_autorizado)

        BarrasSistema.aplicar(this, R.color.umk_mist)

        val monto = intent.getDoubleExtra("monto", 0.0)
        val disponible = intent.getDoubleExtra("disponible", 0.0)
        val saldo = intent.getDoubleExtra("saldo", 0.0)
        val limite = intent.getDoubleExtra("limite", 0.0)
        val motivo = intent.getStringExtra("motivo") ?: ""
        val clienteNombre = intent.getStringExtra("clienteNombre") ?: ""
        val clienteCodigo = intent.getStringExtra("clienteCodigo") ?: ""

        val fmt = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))

        findViewById<TextView>(R.id.tvMonto).text = "C$ ${fmt.format(monto)}"
        val moneda = { v: Double -> "C$ ${fmt.format(v)}" }
        MetricaTile.llenarCredito(findViewById(android.R.id.content), limite, saldo, disponible, moneda)
        MetricaTile.llenarIncremento(findViewById(R.id.tileIncremento), monto, limite, moneda)

        if (motivo.isNotEmpty()) {
            findViewById<TextView>(R.id.tvNota).text = "$motivo"
        }

        val initial = clienteNombre.split(" ").filter { it.isNotBlank() && it.first().isLetter() }
            .take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "C" }
        findViewById<TextView>(R.id.tvAvatar).text = initial
        findViewById<TextView>(R.id.tvNombreCliente).text = clienteNombre.ifEmpty { "Cliente" }
        findViewById<TextView>(R.id.tvIdCliente).text = clienteCodigo

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        val deslizar = findViewById<DeslizarParaConfirmarView>(R.id.btnEnviar)

        // Desde el listado de solicitudes: solo consulta (no se puede reenviar)
        if (intent.getBooleanExtra("soloLectura", false)) {
            findViewById<TextView>(R.id.tvTituloConfirmar).text = "Detalle de solicitud"
            deslizar.visibility = View.GONE
            val estado = intent.getStringExtra("estado").orEmpty()
            val (txt, bg, fg) = when {
                estado.contains("Aprob", true) -> Triple("Aprobada", R.color.status_active_bg, R.color.status_active_text)
                estado.contains("Rechaz", true) -> Triple("Rechazada", R.color.status_cancel_bg, R.color.status_cancel_text)
                else -> Triple("Pendiente", R.color.status_pending_bg, R.color.status_pending_text)
            }
            findViewById<TextView>(R.id.tvEstadoSolicitud).apply {
                text = txt
                backgroundTintList = ContextCompat.getColorStateList(this@SolicitarIncrementoAutorizadoActivity, bg)
                setTextColor(ContextCompat.getColor(this@SolicitarIncrementoAutorizadoActivity, fg))
                visibility = View.VISIBLE
            }
            return
        }
        deslizar.texto = "Desliza para enviar"
        deslizar.onConfirmado = {
            if (!isLoading) {
                isLoading = true
                deslizar.cargando = true
                deslizar.texto = "Enviando…"

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
                            deslizar.cargando = false
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
                            deslizar.texto = "Desliza para enviar"
                            deslizar.reiniciar()
                            Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }
        }
    }
}