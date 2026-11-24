package com.unimarksa.gmv.fragments

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.unimarksa.gmv.R
import com.unimarksa.gmv.databinding.FragmentSellerMetricsBinding
import com.unimarksa.gmv.models.ItemStatArticulo
import com.unimarksa.gmv.models.ItemStatRecup
import com.unimarksa.gmv.models.ItemStatVenta
import com.unimarksa.gmv.tools.ApiService
import com.unimarksa.gmv.tools.SessionManager
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

/**
 * Mis estadísticas: cómo va la ruta en el período.
 * - Ventas: vendido vs meta (barra + cuánto falta / cuánto se superó) y unidades.
 * - Recuperación: recuperado vs meta, desglose crédito / contado.
 * - Saldo vencido: total y distribución por antigüedad (no depende del período: es al día de hoy).
 */
class SellerMetricsFragment : Fragment() {

    private var _binding: FragmentSellerMetricsBinding? = null
    private val binding get() = _binding!!
    private var currentMes = Calendar.getInstance().get(Calendar.MONTH) + 1
    private var currentAnno = Calendar.getInstance().get(Calendar.YEAR)

    /** Identifica la carga vigente: respuestas de un período anterior se descartan. */
    private var cargaId = 0
    private var pendientes = 0
    private var huboError = false

    private val meses = arrayOf("ENE", "FEB", "MAR", "ABR", "MAY", "JUN", "JUL", "AGO", "SEP", "OCT", "NOV", "DIC")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSellerMetricsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

        val ruta = SessionManager.getUsername().uppercase()
        binding.txtSellerZone.text = "Ruta $ruta"
        binding.txtSellerName.text = SessionManager.getFullName().ifEmpty { ruta }
        actualizarLabelPeriodo()

        binding.chipPeriod.setOnClickListener { showMonthPickerDialog() }
        binding.swipeRefresh.setColorSchemeColors(ContextCompat.getColor(requireContext(), R.color.umk_royal))
        binding.swipeRefresh.setOnRefreshListener { cargarDatos() }

        cargarDatos()
    }

    // ── Carga ────────────────────────────────────────────────────────────────

    private fun cargarDatos() {
        val id = ++cargaId
        pendientes = 3
        huboError = false
        binding.swipeRefresh.isRefreshing = true
        // Atenuar mientras carga: deja claro que los números aún no son del período elegido
        listOf(binding.cardVentas, binding.cardRecup, binding.cardVencidos).forEach { it.alpha = 0.5f }

        fun terminar(error: Boolean) {
            if (id != cargaId || _binding == null) return
            if (error) huboError = true
            if (--pendientes > 0) return
            binding.swipeRefresh.isRefreshing = false
            if (huboError) {
                Snackbar.make(binding.root, "No se pudieron cargar todas las estadísticas", Snackbar.LENGTH_LONG)
                    .setAction("Reintentar") { cargarDatos() }
                    .setActionTextColor(ContextCompat.getColor(requireContext(), R.color.umk_cyan))
                    .show()
            }
        }

        ApiService.fetchStatRuta(
            mes = currentMes, anno = currentAnno,
            onResult = { stat ->
                if (id == cargaId && _binding != null) actualizarVentas(stat)
                terminar(false)
            },
            onError = { terminar(true) }
        )
        ApiService.fetchStatRecup(
            mes = currentMes, anno = currentAnno,
            onResult = { stat ->
                if (id == cargaId && _binding != null) actualizarRecuperacion(stat)
                terminar(false)
            },
            onError = { terminar(true) }
        )
        ApiService.fetchStatArticulo(
            onResult = { stat ->
                if (id == cargaId && _binding != null) actualizarVencidos(stat)
                terminar(false)
            },
            onError = { terminar(true) }
        )
    }

    // ── Ventas ───────────────────────────────────────────────────────────────

    private fun actualizarVentas(stat: ItemStatVenta) {
        binding.cardVentas.alpha = 1f
        val meta = stat.metaVenta
        val real = stat.ventaReal

        binding.idMetaVentaReal.text = moneda(real)
        binding.idMetaVenta.text = if (meta > 0) moneda(meta) else "Sin meta"

        val pct = if (meta > 0) real / meta * 100 else 0.0
        pintarChipAvance(binding.txtVentasPercent, pct, meta > 0)
        binding.barVentas.setProgress(pct.toFloat().coerceIn(0f, 100f))

        val falta = meta - real
        binding.idMetaFaltante.apply {
            when {
                meta <= 0 -> { text = ""; color(R.color.umk_ink_soft) }
                falta > 0 -> { text = "Faltan ${moneda(falta)}"; color(R.color.umk_ink_soft) }
                else -> { text = "Meta superada por ${moneda(-falta)}"; color(R.color.umk_success) }
            }
        }

        binding.txtMetaUnits.text = "%,d".format(stat.vntCanti)
        binding.txtRealUnits.text = "%,d".format(stat.vntCantiReal)
        val faltanU = stat.vntCanti - stat.vntCantiReal
        if (faltanU > 0) {
            binding.lblDiffUnits.text = "Faltan"
            binding.txtDiffUnits.text = "%,d".format(faltanU)
            binding.txtDiffUnits.color(R.color.umk_ink)
        } else {
            binding.lblDiffUnits.text = "Excedente"
            binding.txtDiffUnits.text = "+%,d".format(abs(faltanU))
            binding.txtDiffUnits.color(R.color.umk_success)
        }
    }

    // ── Recuperación ─────────────────────────────────────────────────────────

    private fun actualizarRecuperacion(stat: ItemStatRecup) {
        binding.cardRecup.alpha = 1f
        val meta = stat.metaRecuperacion
        val total = stat.recupTotal

        binding.idRecupTotal.text = moneda(total)
        binding.idMetaRecup.text = if (meta > 0) moneda(meta) else "Sin meta"
        binding.idRecupCredito.text = moneda(stat.recupCredito)
        binding.idRecupContado.text = moneda(stat.recupContado)

        // Con meta 0 el backend devuelve Recup_cumple = 100: antes se mostraba "100%" con todo en cero
        val pct = if (meta > 0) total / meta * 100 else 0.0
        pintarChipAvance(binding.idLblDiferenciaRecup, pct, meta > 0)
        binding.idProgresRecup.setProgress(pct.toFloat().coerceIn(0f, 100f))

        val falta = meta - total
        binding.txtRecupFaltante.apply {
            when {
                meta <= 0 -> { text = ""; color(R.color.umk_ink_soft) }
                falta > 0 -> { text = "Faltan ${moneda(falta)}"; color(R.color.umk_ink_soft) }
                else -> { text = "Meta superada por ${moneda(-falta)}"; color(R.color.umk_success) }
            }
        }
    }

    // ── Saldo vencido ────────────────────────────────────────────────────────

    private fun actualizarVencidos(stat: ItemStatArticulo) {
        binding.cardVencidos.alpha = 1f
        fun n(raw: String) = raw.replace(",", "").trim().toDoubleOrNull() ?: 0.0

        data class Tramo(val etiqueta: String, val monto: Double, @ColorRes val color: Int)
        val tramos = listOf(
            Tramo("No vencido", n(stat.noVencidos), R.color.umk_success),
            Tramo("1 – 30 días", n(stat.dias30), R.color.mora_warn),
            Tramo("31 – 60 días", n(stat.dias60), R.color.mora_60),
            Tramo("61 – 90 días", n(stat.dias90), R.color.mora_90),
            Tramo("91 – 120 días", n(stat.dias120), R.color.mora_danger),
            Tramo("Más de 120 días", n(stat.mas120), R.color.umk_danger)
        )
        val vencido = tramos.drop(1).sumOf { it.monto }
        val cartera = tramos.sumOf { it.monto }

        binding.idSaldoVencido.text = moneda(vencido)

        // Barra por antigüedad
        val dp = resources.displayMetrics.density
        val barra = binding.lytBarraAging
        barra.removeAllViews()
        barra.visibility = if (cartera > 0) View.VISIBLE else View.GONE
        tramos.filter { it.monto > 0 }.forEachIndexed { i, t ->
            barra.addView(View(requireContext()).apply {
                setBackgroundResource(R.drawable.bg_segmento)
                backgroundTintList = ContextCompat.getColorStateList(requireContext(), t.color)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, t.monto.toFloat()).apply {
                    if (i > 0) marginStart = (3 * dp).toInt()
                }
            })
        }

        // Filas: punto de color · etiqueta · monto · % de la cartera
        val filas = binding.lytAging
        filas.removeAllViews()
        tramos.forEachIndexed { i, t ->
            val fila = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                minimumHeight = (36 * dp).toInt()
            }
            fila.addView(View(requireContext()).apply {
                setBackgroundResource(R.drawable.bg_chip_pill)
                backgroundTintList = ContextCompat.getColorStateList(requireContext(), t.color)
                layoutParams = LinearLayout.LayoutParams((10 * dp).toInt(), (10 * dp).toInt())
            })
            fila.addView(TextView(requireContext()).apply {
                text = t.etiqueta
                textSize = 14f
                setTextColor(ContextCompat.getColor(context, R.color.umk_ink_soft))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = (10 * dp).toInt()
                }
            })
            fila.addView(TextView(requireContext()).apply {
                text = moneda(t.monto)
                textSize = 14f
                fontFeatureSettings = "tnum"
                setTextColor(ContextCompat.getColor(context,
                    if (t.monto > 0 && i > 0) R.color.umk_ink else R.color.umk_ink_muted))
                typeface = android.graphics.Typeface.create(typeface, if (t.monto > 0) 700 else 400, false)
            })
            fila.contentDescription = "${t.etiqueta}: ${moneda(t.monto)}"
            if (i > 0) filas.addView(View(requireContext()).apply {
                setBackgroundColor(ContextCompat.getColor(context, R.color.umk_line))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (1 * dp).toInt())
            })
            filas.addView(fila)
        }
    }

    // ── Utilidades ───────────────────────────────────────────────────────────

    /** Chip de avance: ≥100 verde, ≥70 azul, ≥40 ámbar, menor rojo; sin meta, neutro. */
    private fun pintarChipAvance(tv: TextView, pct: Double, hayMeta: Boolean) {
        val (texto, fondo, color) = when {
            !hayMeta -> Triple("Sin meta", R.color.umk_mist, R.color.umk_ink_muted)
            pct >= 100 -> Triple(pctTxt(pct) + " ✓", R.color.status_active_bg, R.color.status_active_text)
            pct >= 70 -> Triple(pctTxt(pct), R.color.umk_royal_tint, R.color.umk_royal)
            pct >= 40 -> Triple(pctTxt(pct), R.color.status_pending_bg, R.color.status_pending_text)
            else -> Triple(pctTxt(pct), R.color.status_cancel_bg, R.color.status_cancel_text)
        }
        tv.text = texto
        tv.backgroundTintList = ContextCompat.getColorStateList(requireContext(), fondo)
        tv.color(color)
    }

    private fun pctTxt(p: Double) = String.format(Locale.US, "%.1f%%", p)

    private fun TextView.color(@ColorRes res: Int) = setTextColor(ContextCompat.getColor(context, res))

    private fun moneda(v: Double) = String.format(Locale.US, "C$ %,.2f", v)

    private fun actualizarLabelPeriodo() {
        binding.txtPeriod.text = "${meses[currentMes - 1]} $currentAnno"
        binding.chipPeriod.contentDescription = "Período ${meses[currentMes - 1]} $currentAnno. Cambiar período"
    }

    /** Selector de mes: cuadrícula 3×4 con botones de 48dp (antes 72px, muy pequeños). */
    private fun showMonthPickerDialog() {
        val ctx = requireContext()
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(ctx)
        dialog.setContentView(R.layout.dialog_month_picker)

        var year = currentAnno
        val tvYear = dialog.findViewById<TextView>(R.id.tv_year)!!
        val lytMonths = dialog.findViewById<LinearLayout>(R.id.lyt_months)!!
        val dp = resources.displayMetrics.density
        val hoy = Calendar.getInstance()
        tvYear.text = year.toString()

        fun buildMonths() {
            lytMonths.removeAllViews()
            for (fila in 0 until 4) {
                val rowLay = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
                for (col in 0 until 3) {
                    val idx = fila * 3 + col
                    val seleccionado = idx + 1 == currentMes && year == currentAnno
                    val futuro = year > hoy.get(Calendar.YEAR) ||
                        (year == hoy.get(Calendar.YEAR) && idx > hoy.get(Calendar.MONTH))
                    rowLay.addView(TextView(ctx).apply {
                        text = meses[idx]
                        textSize = 15f
                        gravity = Gravity.CENTER
                        isEnabled = !futuro
                        alpha = if (futuro) 0.35f else 1f
                        typeface = android.graphics.Typeface.create(typeface, if (seleccionado) 800 else 600, false)
                        setBackgroundResource(if (seleccionado) R.drawable.bg_tarjeta_seleccionada else R.drawable.bg_tarjeta_perfil)
                        setTextColor(ContextCompat.getColor(ctx, if (seleccionado) R.color.umk_royal else R.color.umk_ink))
                        layoutParams = LinearLayout.LayoutParams(0, (48 * dp).toInt(), 1f).apply {
                            val m = (4 * dp).toInt(); setMargins(m, m, m, m)
                        }
                        setOnClickListener {
                            currentMes = idx + 1
                            currentAnno = year
                            actualizarLabelPeriodo()
                            cargarDatos()
                            dialog.dismiss()
                        }
                    })
                }
                lytMonths.addView(rowLay)
            }
        }

        buildMonths()
        dialog.findViewById<TextView>(R.id.btn_year_prev)!!.setOnClickListener {
            year--; tvYear.text = year.toString(); buildMonths()
        }
        dialog.findViewById<TextView>(R.id.btn_year_next)!!.setOnClickListener {
            if (year < hoy.get(Calendar.YEAR)) { year++; tvYear.text = year.toString(); buildMonths() }
        }
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cargaId++ // invalida respuestas pendientes
        _binding = null
    }
}
