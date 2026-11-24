package com.unimarksa.gmv.tools

import android.app.Activity
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.unimarksa.gmv.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Lógica compartida de las listas agrupadas por día (Facturación, Órdenes, Recibos, Solicitudes):
 * leer fechas del backend, títulos de día ("HOY", "AYER", "LUN 22 NOV"), rango legible
 * ("18 – 24 NOV") y el control de rangos rápidos fijo abajo (Hoy · 7 días · 30 días · Este mes).
 */
object FechasRango {

    private val PATRONES = listOf(
        "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd",
        "dd/MM/yyyy HH:mm", "dd/MM/yyyy", "dd-MM-yyyy"
    )
    private val DIAS = arrayOf("DOM", "LUN", "MAR", "MIÉ", "JUE", "VIE", "SÁB")
    private val MESES = arrayOf("ENE", "FEB", "MAR", "ABR", "MAY", "JUN", "JUL", "AGO", "SEP", "OCT", "NOV", "DIC")

    val ISO = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val FECHA_CORTA = SimpleDateFormat("dd/MM/yyyy", Locale.US)

    fun hoySinHora(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    /** Fecha (sin hora) de un texto del backend; null si no se puede leer. */
    fun diaDe(raw: String): Date? {
        val t = raw.trim()
        if (t.isEmpty()) return null
        for (p in PATRONES) {
            if (t.length < p.length) continue
            val d = runCatching {
                SimpleDateFormat(p, Locale.US).apply { isLenient = false }.parse(t.take(p.length))
            }.getOrNull() ?: continue
            return Calendar.getInstance().apply {
                time = d
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.time
        }
        return null
    }

    /** "yyyy-MM-dd" del texto del backend, o null. Útil para filtrar por rango. */
    fun isoDe(raw: String): String? = diaDe(raw)?.let { ISO.format(it) }

    /** Días desde hoy (negativo = pasado). */
    fun diasDesdeHoy(dia: Date): Int = ((dia.time - hoySinHora()) / 86_400_000L).toInt()

    fun tituloDia(dia: Date?): String {
        if (dia == null) return "SIN FECHA"
        val c = Calendar.getInstance().apply { time = dia }
        return when (-diasDesdeHoy(dia)) {
            0 -> "HOY"
            1 -> "AYER"
            else -> buildString {
                append(DIAS[c.get(Calendar.DAY_OF_WEEK) - 1]).append(' ')
                append(c.get(Calendar.DAY_OF_MONTH)).append(' ')
                append(MESES[c.get(Calendar.MONTH)])
                if (c.get(Calendar.YEAR) != Calendar.getInstance().get(Calendar.YEAR)) append(' ').append(c.get(Calendar.YEAR))
            }
        }
    }

    /** "18 – 24 NOV", "28 OCT – 3 NOV", "24 NOV"; con año si no es el actual. Entradas yyyy-MM-dd. */
    fun rangoLegible(desde: String, hasta: String): String {
        val d = runCatching { ISO.parse(desde) }.getOrNull() ?: return "$desde – $hasta"
        val h = runCatching { ISO.parse(hasta) }.getOrNull() ?: return "$desde – $hasta"
        val cd = Calendar.getInstance().apply { time = d }
        val ch = Calendar.getInstance().apply { time = h }
        val anioActual = Calendar.getInstance().get(Calendar.YEAR)
        fun dia(c: Calendar) = c.get(Calendar.DAY_OF_MONTH).toString()
        fun mes(c: Calendar) = MESES[c.get(Calendar.MONTH)]
        fun anio(c: Calendar) = if (c.get(Calendar.YEAR) != anioActual) " ${c.get(Calendar.YEAR)}" else ""
        val mismoAnio = cd.get(Calendar.YEAR) == ch.get(Calendar.YEAR)
        val mismoMes = mismoAnio && cd.get(Calendar.MONTH) == ch.get(Calendar.MONTH)
        return when {
            mismoMes && dia(cd) == dia(ch) -> "${dia(cd)} ${mes(cd)}${anio(cd)}"
            mismoMes -> "${dia(cd)} – ${dia(ch)} ${mes(ch)}${anio(ch)}"
            mismoAnio -> "${dia(cd)} ${mes(cd)} – ${dia(ch)} ${mes(ch)}${anio(ch)}"
            else -> "${dia(cd)} ${mes(cd)} ${cd.get(Calendar.YEAR)} – ${dia(ch)} ${mes(ch)} ${ch.get(Calendar.YEAR)}"
        }
    }

    /**
     * Agrupa [items] por día (más reciente primero). Devuelve pares (día, elementos del día)
     * manteniendo el orden original dentro de cada día.
     */
    fun <T> agruparPorDia(items: List<T>, fecha: (T) -> String): List<Pair<Date?, List<T>>> =
        items.groupBy { diaDe(fecha(it)) }
            .toList()
            .sortedByDescending { it.first?.time ?: Long.MIN_VALUE }

    // ── Rangos rápidos (panel inferior con ids rangoHoy / rango7 / rango30 / rangoMes) ──

    val RANGOS = listOf(
        R.id.rangoHoy to "HOY",
        R.id.rango7 to "ÚLTIMOS 7 DÍAS",
        R.id.rango30 to "ÚLTIMOS 30 DÍAS",
        R.id.rangoMes to "ESTE MES"
    )

    /** Fechas (desde, hasta) en yyyy-MM-dd para un rango rápido. */
    fun fechasDe(idRango: Int): Pair<String, String> {
        val hasta = Calendar.getInstance()
        val desde = Calendar.getInstance()
        when (idRango) {
            R.id.rango7 -> desde.add(Calendar.DAY_OF_YEAR, -6)
            R.id.rango30 -> desde.add(Calendar.DAY_OF_YEAR, -29)
            R.id.rangoMes -> desde.set(Calendar.DAY_OF_MONTH, 1)
        }
        return ISO.format(desde.time) to ISO.format(hasta.time)
    }

    /** Marca visualmente el rango activo (null = fechas a mano) en el control segmentado. */
    fun marcarRango(activity: Activity, idActivo: Int?) {
        RANGOS.forEach { (rid, _) ->
            val tv = activity.findViewById<TextView>(rid) ?: return@forEach
            val sel = rid == idActivo
            tv.setBackgroundResource(if (sel) R.drawable.bg_segmentado_indicador else 0)
            tv.setTextColor(ContextCompat.getColor(activity, if (sel) R.color.umk_ink else R.color.umk_ink_soft))
            tv.isSelected = sel
        }
    }

    /** Título de la línea de total: nombre del rango rápido o el rango legible. */
    fun tituloResumen(idActivo: Int?, desde: String, hasta: String): String =
        RANGOS.firstOrNull { it.first == idActivo }?.second ?: rangoLegible(desde, hasta)
}
