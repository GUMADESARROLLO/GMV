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
import com.unimarksa.gmv.databinding.ActivityReciboColectorBinding
import com.unimarksa.gmv.models.ItemPagoFactura
import com.unimarksa.gmv.tools.ApiService
import com.unimarksa.gmv.tools.CartManager
import com.unimarksa.gmv.tools.PagoManager
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
            binding.tvClientName.text = "${clienteCodigo} - ${clienteNombre}"
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
            empty.setTextColor(0xFF9A9890.toInt())
            empty.textSize = 13f
            empty.gravity = android.view.Gravity.CENTER
            empty.setPadding(0, 24, 0, 24)
            binding.lytPagos.addView(empty)
        } else {
            for (pago in pagos) {
                val row = inflater.inflate(R.layout.item_resumen_pago, binding.lytPagos, false)
                row.findViewById<TextView>(R.id.id_Factura).text = "Fact. ${pago.numero}"
                row.findViewById<TextView>(R.id.id_nc).text = "C$ ${fmt.format(pago.nc)}"
                row.findViewById<TextView>(R.id.id_retencion).text = "C$ ${fmt.format(pago.retencion)}"
                row.findViewById<TextView>(R.id.id_descuento).text = "C$ ${fmt.format(pago.descuento)}"
                row.findViewById<TextView>(R.id.id_valor_recibido).text = "C$ ${fmt.format(pago.valorRecibido)}"
                applyEstado(row, pago)
                binding.lytPagos.addView(row)
            }
        }
        updateTotals()
    }

    private fun applyEstado(row: View, pago: ItemPagoFactura) {
        val color = estadoColor(pago.estado)
        row.findViewById<View>(R.id.view_estado_bar).setBackgroundColor(color)
        val badge = row.findViewById<TextView>(R.id.id_badge_estado)
        badge.text = when (pago.action) {
            "abono" -> "Bono"
            "cancelacion" -> "Cancelacion"
            "contado" -> "Contado"
            else -> pago.action.replaceFirstChar { it.uppercase() }
        }
        badge.backgroundTintList = ColorStateList.valueOf(color)
    }

    private fun estadoColor(estado: String): Int {
        return when (estado) {
            "pagado" -> ContextCompat.getColor(this, R.color.green_500)
            "parcial" -> ContextCompat.getColor(this, R.color.light_blue_400)
            else -> ContextCompat.getColor(this, R.color.red_400)
        }
    }

    private fun setupDatePicker() {
        binding.tvReciboFecha.setOnClickListener {
            val now = Calendar.getInstance()
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
        binding.tvReciboNumero.setOnClickListener {
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

        binding.btnConfirm.setOnClickListener {
            if (PagoManager.count == 0) {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Sin pagos")
                    .setMessage("Agrega al menos un pago antes de confirmar.")
                    .setPositiveButton("OK", null)
                    .show()
                return@setOnClickListener
            }
            if (receiptNumber.isBlank()) {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Campo requerido")
                    .setMessage("Ingresa el n\u00famero de recibo antes de confirmar.")
                    .setPositiveButton("OK", null)
                    .show()
                return@setOnClickListener
            }
            if (selectedDate.isBlank()) {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Campo requerido")
                    .setMessage("Selecciona la fecha del recibo antes de confirmar.")
                    .setPositiveButton("OK", null)
                    .show()
                return@setOnClickListener
            }

            showProgressDialog()

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
                    dismissProgressDialog()
                    when {
                        serverResponse == "Nuevo" -> {
                            PagoManager.clear()
                            showSuccessDialog()
                        }
                        serverResponse == "Existe" -> Toast.makeText(this, "El n\u00famero de recibo ya fue utilizado", Toast.LENGTH_LONG).show()
                        serverResponse.startsWith("Error") -> Toast.makeText(this, "Error: $serverResponse", Toast.LENGTH_LONG).show()
                        else -> Toast.makeText(this, "Respuesta: $serverResponse", Toast.LENGTH_LONG).show()
                    }
                },
                onError = { error ->
                    dismissProgressDialog()
                    android.util.Log.e("ReciboColector", "Error al enviar recibo: $error")
                    MaterialAlertDialogBuilder(this)
                        .setTitle("Error al enviar")
                        .setMessage(error)
                        .setPositiveButton("OK", null)
                        .show()
                }
            )
        }
    }
}
