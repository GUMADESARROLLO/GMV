package com.unimarksa.gmv.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.unimarksa.gmv.R

class PagoFacturaDialog : BottomSheetDialogFragment() {

    private var values = mutableMapOf(
        FIELD_NC to "0.00",
        FIELD_RETENCION to "0.00",
        FIELD_DESCUENTO to "0.00",
        FIELD_RECIBIDO to "0.00"
    )
    private var selectedField = FIELD_NC
    private var selectedAction = "contado"
    private var saldo: Double = 0.0
    private var listener: ((action: String, values: Map<String, String>) -> Unit)? = null

    fun setSaldo(s: Double) { saldo = s }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_pago_factura, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.post {
            (dialog as? BottomSheetDialog)?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
        }

        val tvSaldo = view.findViewById<TextView>(R.id.tv_saldo_dialog)
        tvSaldo.text = "C$ ${"%,.2f".format(saldo)}"

        val fieldViews = mapOf(
            FIELD_NC to FieldViews(
                view.findViewById(R.id.lyt_field_nc),
                view.findViewById(R.id.tv_field_nc)
            ),
            FIELD_RETENCION to FieldViews(
                view.findViewById(R.id.lyt_field_retencion),
                view.findViewById(R.id.tv_field_retencion)
            ),
            FIELD_DESCUENTO to FieldViews(
                view.findViewById(R.id.lyt_field_descuento),
                view.findViewById(R.id.tv_field_descuento)
            ),
            FIELD_RECIBIDO to FieldViews(
                view.findViewById(R.id.lyt_field_recibido),
                view.findViewById(R.id.tv_field_recibido)
            )
        )

        fun highlightField(field: String) {
            selectedField = field
            fieldViews.forEach { (key, fv) ->
                fv.container.setBackgroundResource(
                    if (key == field) R.drawable.bg_input_selected else R.drawable.bg_input
                )
            }
        }

        highlightField(FIELD_NC)

        fieldViews.forEach { (key, fv) ->
            fv.container.setOnClickListener { highlightField(key) }
        }

        fun updateDisplay() {
            fieldViews.forEach { (key, fv) ->
                fv.tvValue.text = values[key] ?: "0.00"
            }
        }

        val digits = mapOf(
            R.id.btn_0 to "0", R.id.btn_1 to "1", R.id.btn_2 to "2", R.id.btn_3 to "3",
            R.id.btn_4 to "4", R.id.btn_5 to "5", R.id.btn_6 to "6", R.id.btn_7 to "7",
            R.id.btn_8 to "8", R.id.btn_9 to "9", R.id.btn_00 to "00", R.id.btn_dot to "."
        )

        for ((id, digit) in digits) {
            view.findViewById<Button>(id).setOnClickListener {
                val current = values[selectedField] ?: "0.00"
                val candidate = if (current == "0.00") digit else current + digit
                if (candidate.length <= 14 && candidate.toDoubleOrNull() != null) {
                    values[selectedField] = candidate
                    updateDisplay()
                }
            }
        }

        view.findViewById<Button>(R.id.btn_delete).setOnClickListener {
            val current = values[selectedField] ?: "0.00"
            values[selectedField] = if (current.length <= 1) "0.00"
            else current.dropLast(1).ifEmpty { "0" }
            updateDisplay()
        }

        val btnAbono = view.findViewById<MaterialButton>(R.id.btn_abono)
        val btnCancelacion = view.findViewById<MaterialButton>(R.id.btn_cancelacion)
        val btnContado = view.findViewById<MaterialButton>(R.id.btn_contado)

        val primaryColor = ContextCompat.getColor(requireContext(), R.color.primary)
        val onPrimaryColor = ContextCompat.getColor(requireContext(), R.color.on_primary)
        val strokeColor = ContextCompat.getColor(requireContext(), R.color.outline)
        val transparent = ContextCompat.getColor(requireContext(), android.R.color.transparent)

        fun styleAsSelected(btn: MaterialButton) {
            btn.backgroundTintList = ColorStateList.valueOf(primaryColor)
            btn.setTextColor(onPrimaryColor)
            btn.strokeColor = ColorStateList.valueOf(primaryColor)
            btn.strokeWidth = 0
        }

        fun styleAsUnselected(btn: MaterialButton) {
            btn.backgroundTintList = ColorStateList.valueOf(transparent)
            btn.setTextColor(primaryColor)
            btn.strokeColor = ColorStateList.valueOf(strokeColor)
            btn.strokeWidth = 1
        }

        fun selectAction(action: String) {
            selectedAction = action
            styleAsUnselected(btnAbono)
            styleAsUnselected(btnCancelacion)
            styleAsUnselected(btnContado)
            when (action) {
                "abono" -> styleAsSelected(btnAbono)
                "cancelacion" -> styleAsSelected(btnCancelacion)
                "contado" -> styleAsSelected(btnContado)
            }
        }

        selectAction("contado")

        btnAbono.setOnClickListener { selectAction("abono") }
        btnCancelacion.setOnClickListener { selectAction("cancelacion") }
        btnContado.setOnClickListener { selectAction("contado") }

        fun parseValue(key: String): Double {
            return (values[key] ?: "0.00").replace(",", "").toDoubleOrNull() ?: 0.0
        }

        view.findViewById<MaterialButton>(R.id.btn_confirmar_accion).setOnClickListener {
            val nc = parseValue(FIELD_NC)
            val ret = parseValue(FIELD_RETENCION)
            val desc = parseValue(FIELD_DESCUENTO)
            val rec = parseValue(FIELD_RECIBIDO)
            val total = nc + ret + desc + rec
            if (total > saldo) {
                val errorMsg = "La suma de valores (C$ ${"%,.2f".format(total)}) excede el saldo de la factura (C$ ${"%,.2f".format(saldo)})"
                com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Error")
                    .setMessage(errorMsg)
                    .setPositiveButton("OK", null)
                    .show()
                return@setOnClickListener
            }
            listener?.invoke(selectedAction, values.toMap())
            dismiss()
        }

        view.findViewById<MaterialButton>(R.id.btn_cancelar_accion).setOnClickListener {
            resetValues()
            dismiss()
        }
    }

    private fun resetValues() {
        values[FIELD_NC] = "0.00"
        values[FIELD_RETENCION] = "0.00"
        values[FIELD_DESCUENTO] = "0.00"
        values[FIELD_RECIBIDO] = "0.00"
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        resetValues()
        super.onDismiss(dialog)
    }

    fun setOnActionListener(l: (action: String, values: Map<String, String>) -> Unit) {
        listener = l
    }

    private data class FieldViews(val container: View, val tvValue: TextView)

    companion object {
        const val FIELD_NC = "nc"
        const val FIELD_RETENCION = "retencion"
        const val FIELD_DESCUENTO = "descuento"
        const val FIELD_RECIBIDO = "recibido"
    }
}
