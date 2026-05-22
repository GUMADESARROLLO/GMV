package com.unimarksa.gmv.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.unimarksa.gmv.R

class PagoFacturaDialog : BottomSheetDialogFragment() {

    private var values = mutableMapOf(
        FIELD_NC to "0.00",
        FIELD_RETENCION to "0.00",
        FIELD_DESCUENTO to "0.00",
        FIELD_RECIBIDO to "0.00"
    )
    private var selectedField = FIELD_NC
    private var listener: ((action: String, values: Map<String, String>) -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_pago_factura, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

        view.findViewById<Button>(R.id.btn_abono).setOnClickListener {
            listener?.invoke("abono", values.toMap())
            dismiss()
        }
        view.findViewById<Button>(R.id.btn_cancelacion).setOnClickListener {
            listener?.invoke("cancelacion", values.toMap())
            dismiss()
        }
        view.findViewById<Button>(R.id.btn_contado).setOnClickListener {
            listener?.invoke("contado", values.toMap())
            dismiss()
        }
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
