package com.unimarksa.gmv.activities

import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.unimarksa.gmv.R
import com.unimarksa.gmv.tools.BarrasSistema
import com.unimarksa.gmv.databinding.ActivityReciboColectorBinding
import com.unimarksa.gmv.models.ItemPagoFactura
import com.unimarksa.gmv.tools.ApiService
import com.unimarksa.gmv.tools.CartManager
import com.unimarksa.gmv.tools.PagoManager
import com.unimarksa.gmv.tools.FilaPago
import com.unimarksa.gmv.tools.SessionManager
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ReciboColectorActivity : AppCompatActivity(), DatePickerDialog.OnDateSetListener {

    private lateinit var binding: ActivityReciboColectorBinding
    private var selectedDate: String = ""
    private var receiptNumber: String = ""
    private val fmt = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private var progressDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityReciboColectorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        BarrasSistema.aplicar(this, R.color.umk_mist)

        val codCliente = intent.getStringExtra("codCliente") ?: CartManager.currentCliente?.codCliente ?: ""
        val nomCliente = intent.getStringExtra("nomCliente") ?: CartManager.currentCliente?.nombre ?: ""

        PagoManager.loadFromDb {
            runOnUiThread {
                refreshPayments()
                updateTotals()
            }
        }
        setupToolbar()
        setupClientInfo(nomCliente, codCliente)
        refreshPayments()
        setupDatePicker()
        setupReceiptNumber()
        autoAsignarRecibo()
        updateTotals()
        setupListeners()
    }

    private var clienteCodigo = ""
    private var clienteNombre = ""

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupClientInfo(nomCliente: String, codCliente: String) {
        clienteCodigo = codCliente
        clienteNombre = nomCliente
        if (clienteNombre.isNotEmpty() && clienteCodigo.isNotEmpty()) {
            binding.cardClient.visibility = View.VISIBLE
            binding.tvClientName.text = clienteNombre
            binding.tvClientDireccion.text = "Código $clienteCodigo"
            binding.tvClientAvatar.text = clienteNombre.split(" ")
                .filter { it.isNotBlank() && it.first().isLetter() }
                .take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "CL" }
        }
        selectedDate = dateFmt.format(System.currentTimeMillis())
        binding.tvReciboFecha.text = selectedDate
    }

    private fun refreshPayments() {
        val pagos = PagoManager.pagos
        binding.tvFacturasCount.text = pagos.size.toString()
        binding.lytPagos.removeAllViews()
        val inflater = LayoutInflater.from(this)

        if (pagos.isEmpty()) {
            val empty = TextView(this)
            empty.text = "No hay pagos agregados"
            empty.setTextColor(ContextCompat.getColor(this, R.color.umk_ink_muted))
            empty.textSize = 14f
            empty.gravity = android.view.Gravity.CENTER
            val p = (24 * resources.displayMetrics.density).toInt()
            empty.setPadding(0, p, 0, p)
            binding.lytPagos.addView(empty)
        } else {
            for (pago in pagos) {
                val row = inflater.inflate(R.layout.item_resumen_pago, binding.lytPagos, false)
                FilaPago.llenar(row, pago)
                binding.lytPagos.addView(row)
            }
        }
        updateTotals()
    }

    private fun setupDatePicker() {
        binding.cardFechaRecibo.setOnClickListener {
            val now = Calendar.getInstance()
            runCatching { dateFmt.parse(selectedDate) }.getOrNull()?.let { now.time = it }
            DatePickerDialog.newInstance(
                this,
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)
            ).apply {
                accentColor = ContextCompat.getColor(this@ReciboColectorActivity, R.color.colorUmkPrimaryDark)
                autoDismiss(true)
                show(supportFragmentManager, "Datepickerdialog")
            }
        }
    }

    override fun onDateSet(view: DatePickerDialog?, year: Int, monthOfYear: Int, dayOfMonth: Int) {
        val cal = Calendar.getInstance()
        cal.set(year, monthOfYear, dayOfMonth)
        selectedDate = dateFmt.format(cal.time)
        binding.tvReciboFecha.text = selectedDate
    }

    override fun onResume() {
        super.onResume()
        val dpd = supportFragmentManager.findFragmentByTag("Datepickerdialog") as? DatePickerDialog
        dpd?.setOnDateSetListener(this)
    }

    private fun setupReceiptNumber() {
        binding.cardNumeroRecibo.setOnClickListener {
            val dialog = Dialog(this)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.dialog_add_vineta)
            dialog.setCancelable(true)

            val edt = dialog.findViewById<EditText>(R.id.edt_cantidad_vineta)
            edt.hint = "N\u00ba Recibo: "

            val title = dialog.findViewById<TextView>(R.id.id_title_frm)
            title.text = "Digite el N\u00ba de recibo."

            val lp = dialog.window?.attributes
            if (lp != null) {
                lp.width = (resources.displayMetrics.widthPixels * 0.85).toInt()
                lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
                dialog.window?.attributes = lp
            }

            dialog.findViewById<View>(R.id.bt_cancel).setOnClickListener { dialog.dismiss() }

            dialog.findViewById<View>(R.id.bt_add).setOnClickListener {
                val input = edt.text.toString()
                if (input.isEmpty()) {
                    Toast.makeText(this, "por favor, digite un valor", Toast.LENGTH_SHORT).show()
                } else {
                    receiptNumber = ("00000" + input).takeLast(5)
                    binding.tvReciboNumero.text = receiptNumber
                    dialog.dismiss()
                }
            }

            dialog.show()
        }
    }

    private fun autoAsignarRecibo() {
        val ruta = SessionManager.getUsername().uppercase()
        if (ruta.isEmpty()) return
        ApiService.fetchLastRecibo(
            ruta = ruta,
            onResult = { nextNum ->
                if (nextNum.isNotBlank()) {
                    receiptNumber = nextNum
                    binding.tvReciboNumero.text = nextNum
                }
            },
            onError = { /* silencio, se asigna manual */ }
        )
    }

    private fun updateTotals() {
        binding.tvTotalRecibido.text = "C$ ${fmt.format(PagoManager.totalRecibido)}"
    }

    private fun showProgressDialog() {
        progressDialog = Dialog(this).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.dialog_progress)
            setCancelable(false)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val lp = WindowManager.LayoutParams()
            lp.copyFrom(window?.attributes)
            lp.width = WindowManager.LayoutParams.WRAP_CONTENT
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT
            show()
            window?.attributes = lp
        }
    }

    private fun dismissProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    private fun showSuccessDialog() {
        MaterialAlertDialogBuilder(this)
            .setIcon(R.drawable.ic_check_circle_filled)
            .setTitle("\u00a1Recibo registrado!")
            .setMessage("El recibo colector se ha registrado exitosamente.")
            .setPositiveButton("Ir al inicio") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun setupListeners() {
        binding.btnEdit.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Cancelar recibo")
                .setMessage("Se perder\u00e1n todos los pagos registrados. \u00bfContinuar?")
                .setPositiveButton("S\u00ed, cancelar") { _, _ ->
                    PagoManager.clear()
                    onBackPressedDispatcher.onBackPressed()
                }
                .setNegativeButton("No", null)
                .show()
        }

        val deslizar = binding.btnConfirm
        deslizar.texto = "Desliza para enviar el recibo"

        fun aviso(titulo: String, mensaje: String) {
            deslizar.reiniciar()
            MaterialAlertDialogBuilder(this)
                .setTitle(titulo)
                .setMessage(mensaje)
                .setPositiveButton("OK", null)
                .show()
        }

        deslizar.onConfirmado = confirmar@{
            if (PagoManager.count == 0) {
                aviso("Sin pagos", "Agrega al menos un pago antes de enviar.")
                return@confirmar
            }
            if (receiptNumber.isBlank()) {
                aviso("Campo requerido", "Ingresa el número de recibo antes de enviar.")
                return@confirmar
            }
            if (selectedDate.isBlank()) {
                aviso("Campo requerido", "Selecciona la fecha del recibo antes de enviar.")
                return@confirmar
            }

            deslizar.cargando = true
            deslizar.texto = "Enviando…"
            binding.btnEdit.isEnabled = false

            fun restaurar() {
                deslizar.texto = "Desliza para enviar el recibo"
                deslizar.reiniciar()
                binding.btnEdit.isEnabled = true
            }

            val ruta = SessionManager.getUsername().uppercase()

            ApiService.enviarRecibo(
                ruta = ruta,
                codCliente = clienteCodigo,
                clienteNombre = clienteNombre,
                fechaRecibo = selectedDate,
                reciboNumero = receiptNumber,
                pagos = PagoManager.pagos,
                total = PagoManager.totalRecibido,
                observaciones = binding.etObservaciones.text.toString(),
                onResult = { serverResponse ->
                    when {
                        serverResponse == "Nuevo" -> {
                            deslizar.cargando = false
                            PagoManager.clear()
                            showSuccessDialog()
                        }
                        serverResponse == "Existe" -> {
                            restaurar()
                            aviso("Recibo duplicado", "El número de recibo $receiptNumber ya fue utilizado. Cámbialo e intenta de nuevo.")
                        }
                        serverResponse.startsWith("Error") -> {
                            restaurar()
                            aviso("Error al enviar", serverResponse)
                        }
                        else -> {
                            restaurar()
                            aviso("Respuesta inesperada", serverResponse)
                        }
                    }
                },
                onError = { error ->
                    android.util.Log.e("ReciboColector", "Error al enviar recibo: $error")
                    restaurar()
                    aviso("Error al enviar", error)
                }
            )
        }
    }
}
