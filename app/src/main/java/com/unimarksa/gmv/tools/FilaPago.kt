package com.unimarksa.gmv.tools

import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.unimarksa.gmv.R
import com.unimarksa.gmv.models.ItemPagoFactura
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/** Llena una fila `item_resumen_pago` (Pagos registrados y Recibo colector). */
object FilaPago {

    private val fmt = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))

    fun moneda(v: Double) = "C$ ${fmt.format(v)}"

    /** Etiqueta legible del tipo de pago. "abono" se mostraba como "Bono" y se confundía con bonificación. */
    fun etiqueta(action: String) = when (action) {
        "abono" -> "Abono"
        "cancelacion" -> "Cancelación"
        "contado" -> "Contado"
        else -> action.replaceFirstChar { it.uppercase() }
    }

    fun llenar(row: View, pago: ItemPagoFactura) {
        val ctx = row.context
        row.findViewById<TextView>(R.id.id_Factura).text = "Fact. ${pago.numero}"
        row.findViewById<TextView>(R.id.id_nc).text = moneda(pago.nc)
        row.findViewById<TextView>(R.id.id_retencion).text = moneda(pago.retencion)
        row.findViewById<TextView>(R.id.id_descuento).text = moneda(pago.descuento)
        row.findViewById<TextView>(R.id.id_valor_recibido).text = moneda(pago.valorRecibido)

        // pagado = factura saldada (verde); parcial = abono que deja saldo (azul); resto = pendiente (ámbar)
        val (fondo, texto) = when (pago.estado) {
            "pagado" -> R.color.status_active_bg to R.color.status_active_text
            "parcial" -> R.color.umk_royal_tint to R.color.umk_royal
            else -> R.color.status_pending_bg to R.color.status_pending_text
        }
        row.findViewById<TextView>(R.id.id_badge_estado).apply {
            text = etiqueta(pago.action)
            backgroundTintList = ContextCompat.getColorStateList(ctx, fondo)
            setTextColor(ContextCompat.getColor(ctx, texto))
        }

        row.contentDescription =
            "Factura ${pago.numero}, ${etiqueta(pago.action)}, recibido ${moneda(pago.valorRecibido)}"
    }
}
