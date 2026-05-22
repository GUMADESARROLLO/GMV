package com.unimarksa.gmv.activities

import android.app.Dialog
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
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
import com.unimarksa.gmv.tools.CartManager
import com.unimarksa.gmv.tools.PagoManager
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
    private val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale("es", "NI"))

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityReciboColectorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT < 35) {
            window.statusBarColor = ContextCompat.getColor(this, R.color.colorUmkPrimaryDark)
        }
        ViewCompat.getWindowInsetsController(window.decorView)?.isAppearanceLightStatusBars = false

        setupToolbar()
        setupClientInfo()
        refreshPayments()
        setupDatePicker()
        setupReceiptNumber()
        updateTotals()
        setupListeners()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupClientInfo() {
        val cliente = CartManager.currentCliente
        if (cliente != null) {
            binding.cardClient.visibility = View.VISIBLE
            binding.tvClientName.text = "${cliente.codCliente} - ${cliente.nombre}"
            binding.tvClientDireccion.text = cliente.direccion
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

            val msg = buildString {
                appendLine("Recibo No.: $receiptNumber")
                appendLine("Fecha: $selectedDate")
                appendLine("Facturas: ${PagoManager.count}")
                appendLine("Total Recibido: C$ ${fmt.format(PagoManager.totalRecibido)}")
            }

            MaterialAlertDialogBuilder(this)
                .setTitle("Confirmar recibo")
                .setMessage(msg.trimEnd())
                .setPositiveButton("Confirmar") { _, _ ->
                    PagoManager.clear()
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
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }
}
