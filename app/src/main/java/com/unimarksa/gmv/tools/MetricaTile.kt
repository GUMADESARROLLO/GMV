package com.unimarksa.gmv.tools

import android.content.res.ColorStateList
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.unimarksa.gmv.R

/** Llena una tarjeta `item_metrica_tile` (ícono tintado + título / monto / subtítulo). */
object MetricaTile {

    fun llenar(
        tile: View,
        @DrawableRes icono: Int,
        @ColorRes colorIcono: Int,
        @ColorRes fondoIcono: Int,
        titulo: String,
        valor: String,
        subtitulo: String,
        @ColorRes colorValor: Int = R.color.umk_ink
    ) {
        val ctx = tile.context
        tile.findViewById<ImageView>(R.id.ivMetricaIcono).apply {
            setImageResource(icono)
            imageTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, colorIcono))
            backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, fondoIcono))
        }
        tile.findViewById<TextView>(R.id.tvMetricaTitulo).text = titulo
        tile.findViewById<TextView>(R.id.tvMetricaValor).apply {
            text = valor
            setTextColor(ContextCompat.getColor(ctx, colorValor))
        }
        tile.findViewById<TextView>(R.id.tvMetricaSub).text = subtitulo
        tile.contentDescription = "$titulo: $valor. $subtitulo"
    }

    /** Tarjetas Límite / Saldo / Disponible comunes a la solicitud y la confirmación. */
    fun llenarCredito(raiz: View, limite: Double, saldo: Double, disponible: Double, fmt: (Double) -> String) {
        val base = if (limite > 0) limite else 1.0
        val pctSaldo = ((saldo / base) * 100).toInt().coerceIn(0, 999)
        llenar(raiz.findViewById(R.id.tileLimite), R.drawable.ic_monetization_on,
            R.color.umk_royal, R.color.umk_royal_tint, "Límite actual", fmt(limite), "De crédito")
        llenar(raiz.findViewById(R.id.tileSaldo), R.drawable.ic_receipt_long,
            R.color.umk_danger, R.color.umk_danger_tint, "Saldo", fmt(saldo), "$pctSaldo% del límite",
            if (saldo > 0) R.color.umk_danger else R.color.umk_ink)
        llenar(raiz.findViewById(R.id.tileDisponible), R.drawable.ic_check_circle,
            R.color.umk_success, R.color.umk_success_tint, "Disponible", fmt(disponible), "Para pedidos",
            R.color.umk_success)
    }

    /** Tarjeta Incremento: diferencia entre el nuevo límite y el actual. */
    fun llenarIncremento(tile: View, nuevo: Double, limite: Double, fmt: (Double) -> String) {
        val exceso = nuevo - limite
        if (nuevo <= 0.0 || exceso <= 0.0) {
            llenar(tile, R.drawable.ic_arrow_up, R.color.umk_cyan_ink, R.color.umk_cyan_tint,
                "Incremento", "C$ 0.00", if (nuevo <= 0.0) "Sin monto" else "Menor al actual")
        } else {
            val pct = if (limite > 0) "+${((exceso / limite) * 100).toInt()}% sobre el actual" else "Nuevo crédito"
            llenar(tile, R.drawable.ic_arrow_up, R.color.umk_success, R.color.umk_success_tint,
                "Incremento", "+${fmt(exceso)}", pct, R.color.umk_success)
        }
    }
}
