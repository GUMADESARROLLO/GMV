package com.unimarksa.gmv.fragments

import android.os.Bundle
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.chip.Chip
import com.unimarksa.gmv.R
import com.unimarksa.gmv.databinding.FragmentSellerMetricsBinding
import com.unimarksa.gmv.tools.ApiService
import com.unimarksa.gmv.views.ArcProgressView
import com.unimarksa.gmv.views.AnimatedProgressBar
import com.unimarksa.gmv.views.CircularProgressMiniView
import com.unimarksa.gmv.views.MetricPillView
import java.util.Calendar

class SellerMetricsFragment : Fragment() {

    private var _binding: FragmentSellerMetricsBinding? = null
    private val binding get() = _binding!!
    private var currentMes = Calendar.getInstance().get(Calendar.MONTH) + 1
    private var currentAnno = Calendar.getInstance().get(Calendar.YEAR)

    private lateinit var tvMetaVenta: TextView
    private lateinit var tvMetaVentaReal: TextView
    private lateinit var tvVentasPercent: TextView
    private lateinit var tvMetaFaltante: TextView
    private lateinit var arcVentas: ArcProgressView
    private lateinit var pillMetaUnits: MetricPillView
    private lateinit var pillRealUnits: MetricPillView
    private lateinit var pillDiffUnits: MetricPillView

    private lateinit var tvMetaRecup: TextView
    private lateinit var tvRecupTotal: TextView
    private lateinit var tvRecupCredito: TextView
    private lateinit var tvRecupContado: TextView
    private lateinit var circleRecup: CircularProgressMiniView
    private lateinit var barRecup: AnimatedProgressBar
    private lateinit var tvDifRecup: TextView

    private lateinit var tvSaldoVencido: TextView
    private lateinit var chipNoVencido: Chip
    private lateinit var chipD30: Chip
    private lateinit var chipD60: Chip
    private lateinit var chipD90: Chip
    private lateinit var chipD120: Chip
    private lateinit var chipM120: Chip


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSellerMetricsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        bindViews(view)
        cargarDatos()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        actualizarLabelPeriodo()

        binding.chipPeriod.setOnClickListener { showMonthPickerDialog() }
    }

    private fun bindViews(root: View) {
        tvMetaVenta     = root.findViewById(R.id.id_Meta_venta)
        tvMetaVentaReal = root.findViewById(R.id.id_Meta_venta_real)
        tvVentasPercent = root.findViewById(R.id.txt_ventas_percent)
        tvMetaFaltante  = root.findViewById(R.id.id_meta_faltante)
        arcVentas       = root.findViewById(R.id.arc_progress_ventas)
        pillMetaUnits   = root.findViewById(R.id.pill_meta_units)
        pillRealUnits   = root.findViewById(R.id.pill_real_units)
        pillDiffUnits   = root.findViewById(R.id.pill_diff_units)

        tvMetaRecup    = root.findViewById(R.id.id_meta_recup)
        tvRecupTotal   = root.findViewById(R.id.id_recup_total)
        tvRecupCredito = root.findViewById(R.id.id_recup_credito)
        tvRecupContado = root.findViewById(R.id.id_recup_contado)
        circleRecup    = root.findViewById(R.id.circle_recup)
        barRecup       = root.findViewById(R.id.id_progres_recup)
        tvDifRecup     = root.findViewById(R.id.id_lbl_diferencia_recup)

        tvSaldoVencido = root.findViewById(R.id.id_saldo_vencido)
        val rowNoVencido = root.findViewById<View>(R.id.row_no_vencido)
        val rowD30       = root.findViewById<View>(R.id.row_d30)
        val rowD60       = root.findViewById<View>(R.id.row_d60)
        val rowD90       = root.findViewById<View>(R.id.row_d90)
        val rowD120      = root.findViewById<View>(R.id.row_d120)
        val rowM120      = root.findViewById<View>(R.id.row_m120)

        fun configChip(row: View, label: String): Chip {
            row.findViewById<TextView>(R.id.txt_aging_label).text = label
            return row.findViewById<Chip>(R.id.chip_aging_amount)
        }

        chipNoVencido = configChip(rowNoVencido, "No Vencido")
        chipD30 = configChip(rowD30, "30 Días")
        chipD60 = configChip(rowD60, "60 Días")
        chipD90 = configChip(rowD90, "90 Días")
        chipD120 = configChip(rowD120, "120 Días")
        chipM120 = configChip(rowM120, "+120 Días")
    }

    private fun showMonthPickerDialog() {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        dialog.setContentView(R.layout.dialog_month_picker)
        dialog.setCancelable(true)

        var year = currentAnno
        val tvYear = dialog.findViewById<TextView>(R.id.tv_year)!!
        val lytMonths = dialog.findViewById<android.widget.LinearLayout>(R.id.lyt_months)!!
        val names = arrayOf("ENE","FEB","MAR","ABR","MAY","JUN","JUL","AGO","SEP","OCT","NOV","DIC")
        val screenW = resources.displayMetrics.widthPixels
        tvYear.text = year.toString()

        fun buildMonths() {
            lytMonths.removeAllViews()
            for (row in 0..5) {
                val rowLay = android.widget.LinearLayout(requireContext()).apply {
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    orientation = android.widget.LinearLayout.HORIZONTAL
                }
                for (col in 0..1) {
                    val idx = row * 2 + col
                    if (idx >= 12) break
                    val tv = android.widget.TextView(requireContext()).apply {
                        text = names[idx]
                        textSize = 16f
                        gravity = android.view.Gravity.CENTER
                        layoutParams = android.widget.LinearLayout.LayoutParams(
                            0, 72, 1f
                        ).apply { setMargins(8, 8, 8, 8) }
                        setTextColor(android.graphics.Color.parseColor("#1A1A18"))
                        val bg = GradientDrawable().apply {
                            shape = GradientDrawable.RECTANGLE
                            cornerRadius = 10f
                            setColor(android.graphics.Color.parseColor("#EFF1F5"))
                        }
                        background = bg
                        setOnClickListener {
                            currentMes = idx + 1
                            currentAnno = year
                            actualizarLabelPeriodo()
                            cargarDatos()
                            dialog.dismiss()
                        }
                    }
                    if (idx + 1 == currentMes && year == currentAnno) {
                        val bgSel = GradientDrawable().apply {
                            shape = GradientDrawable.RECTANGLE
                            cornerRadius = 10f
                            setColor(android.graphics.Color.parseColor("#4361EE"))
                        }
                        tv.background = bgSel
                        tv.setTextColor(android.graphics.Color.WHITE)
                    }
                    rowLay.addView(tv)
                }
                lytMonths.addView(rowLay)
            }
        }

        buildMonths()

        dialog.findViewById<TextView>(R.id.btn_year_prev)!!.setOnClickListener {
            year--; tvYear.text = year.toString(); buildMonths()
        }
        dialog.findViewById<TextView>(R.id.btn_year_next)!!.setOnClickListener {
            year++; tvYear.text = year.toString(); buildMonths()
        }

        dialog.show()
    }

    private fun cargarDatos() {
        ApiService.fetchStatArticulo(
            onResult = { stat -> actualizarVencidos(stat) },
            onError = { }
        )

        ApiService.fetchStatRuta(
            mes = currentMes, anno = currentAnno,
            onResult = { stat -> actualizarVentas(stat) },
            onError = { }
        )

        ApiService.fetchStatRecup(
            mes = currentMes, anno = currentAnno,
            onResult = { stat -> actualizarRecuperacion(stat) },
            onError = { }
        )
    }

    private fun parseAmount(raw: String): Double =
        raw.replace(",", "").trim().toDoubleOrNull() ?: 0.0

    private fun actualizarVentas(stat: com.unimarksa.gmv.models.ItemStatVenta) {
        val meta    = stat.metaVenta
        val real    = stat.ventaReal
        val diff    = stat.ventaDif
        val metaU   = stat.vntCanti
        val realU   = stat.vntCantiReal
        val diffU   = stat.vntCantiDif

        val pct = if (meta > 0) (real / meta * 100).toFloat() else 0f
        val faltante = meta - real

        tvMetaVenta.text     = formatCurrency(meta)
        tvMetaVentaReal.text = formatCurrency(real)
        tvVentasPercent.text = "${pct.toInt()}%"
        tvMetaFaltante.text  = "Falta ${formatCurrency(faltante)}"
        arcVentas.setProgress(pct)
        pillMetaUnits.setValue("$metaU")
        pillRealUnits.setValue("$realU")
        pillDiffUnits.setValue("$diffU")
    }

    private fun actualizarRecuperacion(stat: com.unimarksa.gmv.models.ItemStatRecup) {
        val meta = stat.metaRecuperacion
        val credito = stat.recupCredito
        val contado = stat.recupContado
        val total = stat.recupTotal
        val pct = stat.recupCumple.toFloat()

        tvMetaRecup.text    = formatCurrency(meta)
        tvRecupTotal.text   = formatCurrency(total)
        tvRecupCredito.text = formatCurrency(credito)
        tvRecupContado.text = formatCurrency(contado)
        tvDifRecup.text     = "${String.format("%.1f", pct)}%"
        circleRecup.setProgress(pct.coerceIn(0f, 100f))
        barRecup.setProgress(pct.coerceIn(0f, 100f))
    }

    private fun actualizarVencidos(stat: com.unimarksa.gmv.models.ItemStatArticulo) {
        val amounts = listOf(
            stat.noVencidos, stat.dias30, stat.dias60,
            stat.dias90, stat.dias120, stat.mas120
        ).map { parseAmount(it) }

        val totalVencido = amounts.drop(1).sum()
        val chips = listOf(chipNoVencido, chipD30, chipD60, chipD90, chipD120, chipM120)

        for (i in amounts.indices) {
            chips[i].text = formatCurrency(amounts[i])
        }

        tvSaldoVencido.text = formatCurrency(totalVencido)
    }

    private fun formatCurrency(amount: Double): String = "C$ %,.2f".format(amount)

    private fun actualizarLabelPeriodo() {
        val months = arrayOf("ENE","FEB","MAR","ABR","MAY","JUN","JUL","AGO","SEP","OCT","NOV","DIC")
        binding.chipPeriod.text = "${months[currentMes - 1]} $currentAnno"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
