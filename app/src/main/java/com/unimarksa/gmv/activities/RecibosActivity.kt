package com.unimarksa.gmv.activities

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.unimarksa.gmv.R
import com.unimarksa.gmv.adapters.ReporteReciboAdapter
import com.unimarksa.gmv.models.ItemReporteRecibo
import com.unimarksa.gmv.tools.ApiService
import com.unimarksa.gmv.tools.SessionManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RecibosActivity : AppCompatActivity() {

    private val items = mutableListOf<ItemReporteRecibo>()
    private lateinit var adapter: ReporteReciboAdapter
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recibos)

        if (Build.VERSION.SDK_INT >= 35) {
            val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
            val h = if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
            val scrim = View(this)
            scrim.setBackgroundColor(ContextCompat.getColor(this, R.color.brand_primary_dark))
            val content = findViewById<ViewGroup>(android.R.id.content)
            scrim.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h)
            content.addView(scrim)
        } else {
            window.statusBarColor = ContextCompat.getColor(this, R.color.brand_primary_dark)
        }

        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
            .apply {
                setNavigationOnClickListener { finish() }
                setOnMenuItemClickListener { item ->
                    if (item.itemId == R.id.action_anular_recibo) {
                        mostrarDialogoAnular()
                        true
                    } else false
                }
            }

        findViewById<View>(R.id.cardDateFrom).setOnClickListener { showDatePicker { updateDesde(it) } }
        findViewById<View>(R.id.cardDateTo).setOnClickListener { showDatePicker { updateHasta(it) } }

        val rv = findViewById<RecyclerView>(R.id.recyclerRecibos)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = ReporteReciboAdapter(
            items = items,
            onClick = { recibo -> mostrarDetalle(recibo) },
            onDelete = { recibo, position -> confirmarEliminar(recibo, position) },
            onAttach = { recibo ->
                startActivity(Intent(this, AdjuntarComprobanteActivity::class.java)
                    .putExtra("id_recibo", recibo.recibo.replace("#", "")))
            }
        )
        rv.adapter = adapter

        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val lunes = dateFormat.format(cal.time)
        val hoy = dateFormat.format(Calendar.getInstance().time)
        findViewById<TextView>(R.id.tvDateFrom).text = lunes
        findViewById<TextView>(R.id.tvDateTo).text = hoy
        buscar(lunes, hoy)

        val swipeRefresh = findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener {
            val d = findViewById<TextView>(R.id.tvDateFrom).text.toString()
            val h = findViewById<TextView>(R.id.tvDateTo).text.toString()
            buscar(d, h, true)
        }
    }

    private fun updateDesde(date: String) {
        findViewById<TextView>(R.id.tvDateFrom).text = date
        val hasta = findViewById<TextView>(R.id.tvDateTo).text?.toString()?.trim()
        if (!hasta.isNullOrEmpty()) buscar(date, hasta)
    }

    private fun updateHasta(date: String) {
        findViewById<TextView>(R.id.tvDateTo).text = date
        val desde = findViewById<TextView>(R.id.tvDateFrom).text?.toString()?.trim()
        if (!desde.isNullOrEmpty()) buscar(desde, date)
    }

    private fun buscar(desde: String, hasta: String, isRefresh: Boolean = false) {
        val loading = findViewById<View>(R.id.lyt_loading)
        val empty = findViewById<View>(R.id.lyt_empty_result)
        val rv = findViewById<RecyclerView>(R.id.recyclerRecibos)

        if (!isRefresh) {
            loading.visibility = View.VISIBLE
        }
        empty.visibility = View.GONE
        rv.visibility = View.GONE

        ApiService.fetchRecibos(
            desde = desde,
            hasta = hasta,
            onResult = { result ->
                items.clear()
                items.addAll(result)
                adapter.notifyDataSetChanged()
                mostrarResultados()
            },
            onError = {
                items.clear()
                adapter.notifyDataSetChanged()
                mostrarResultados()
            }
        )
    }

    private fun mostrarResultados() {
        val loading = findViewById<View>(R.id.lyt_loading)
        val empty = findViewById<View>(R.id.lyt_empty_result)
        val rv = findViewById<RecyclerView>(R.id.recyclerRecibos)
        val swipeRefresh = findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefresh)
        swipeRefresh.isRefreshing = false

        loading.visibility = View.GONE

        val fmt = java.text.DecimalFormat("#,##0.00", java.text.DecimalFormatSymbols(Locale.US))
        val total = items.sumOf {
            it.orderTotal.replace("C$", "").replace(",", "").trim().toDoubleOrNull() ?: 0.0
        }
        findViewById<TextView>(R.id.tvTotalAmount).text = "C$ ${fmt.format(total)}"
        findViewById<TextView>(R.id.tvReceiptCount).text = "${items.size}"

        if (items.isEmpty()) {
            empty.visibility = View.VISIBLE
            rv.visibility = View.GONE
            return
        }

        empty.visibility = View.GONE
        rv.visibility = View.VISIBLE
    }

    private fun mostrarDetalle(recibo: ItemReporteRecibo) {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_recibo_detalle, null)
        dialog.setContentView(view)

        view.findViewById<TextView>(R.id.tv_recibo_numero).text = "Recibo N° ${recibo.recibo}"
        view.findViewById<TextView>(R.id.tv_recibo_cliente).text = recibo.nombreCliente
        view.findViewById<TextView>(R.id.tv_recibo_fecha).text = recibo.fecha
        val fmtTotal = java.text.DecimalFormat("#,##0.00", java.text.DecimalFormatSymbols(Locale.US))
        val totalVal = recibo.orderTotal.replace("C$", "").replace(",", "").trim().toDoubleOrNull() ?: 0.0
        view.findViewById<TextView>(R.id.tv_recibo_total).text = "C$ ${fmtTotal.format(totalVal)}"

        val tvComentario = view.findViewById<TextView>(R.id.tv_recibo_comentario)
        if (recibo.comentario.isNotBlank()) {
            tvComentario.text = recibo.comentario
            tvComentario.visibility = View.VISIBLE
        } else {
            tvComentario.visibility = View.GONE
        }

        val container = view.findViewById<ViewGroup>(R.id.lyt_detalles)
        container.removeAllViews()
        val inflater = LayoutInflater.from(this)
        val fmt = java.text.DecimalFormat("#,##0.00", java.text.DecimalFormatSymbols(Locale.US))

        for (det in recibo.detalles) {
            val row = inflater.inflate(R.layout.item_recibo_detalle_row, container, false)
            row.findViewById<TextView>(R.id.tv_factura).text = "Fact. ${det.numeroFactura}"

            val actionBg = when {
                det.tipoAccion.contains("Cancelacion", ignoreCase = true) -> R.color.status_cancel_bg
                det.tipoAccion.contains("Abono", ignoreCase = true) -> R.color.status_pending_bg
                else -> R.color.status_active_bg
            }
            val actionText = when {
                det.tipoAccion.contains("Cancelacion", ignoreCase = true) -> R.color.status_cancel_text
                det.tipoAccion.contains("Abono", ignoreCase = true) -> R.color.status_pending_text
                else -> R.color.status_active_text
            }
            val tvTipoAccion = row.findViewById<TextView>(R.id.tv_tipo_accion)
            tvTipoAccion.text = det.tipoAccion
            tvTipoAccion.setBackgroundColor(ContextCompat.getColor(this, actionBg))
            tvTipoAccion.setTextColor(ContextCompat.getColor(this, actionText))

            row.findViewById<TextView>(R.id.tv_valor_recibido).text = "C$ ${fmt.format(det.valorRecibido)}"
            row.findViewById<TextView>(R.id.tv_total).text = "C$ ${fmt.format(det.total)}"
            val tvSaldo = row.findViewById<TextView>(R.id.tv_saldo_pendiente)
            tvSaldo.text = "C$ ${fmt.format(det.saldoPendiente)}"
            tvSaldo.setTextColor(if (det.saldoPendiente > 0) ContextCompat.getColor(this, R.color.amount_negative) else ContextCompat.getColor(this, R.color.amount_zero))

            row.findViewById<View>(R.id.conceptRetention).findViewById<TextView>(R.id.tvConceptLabel).text = "Retención"
            row.findViewById<View>(R.id.conceptRetention).findViewById<TextView>(R.id.tvConceptAmount).text = "C$ ${fmt.format(det.retencion)}"
            row.findViewById<View>(R.id.conceptDiscount).findViewById<TextView>(R.id.tvConceptLabel).text = "Descuento"
            row.findViewById<View>(R.id.conceptDiscount).findViewById<TextView>(R.id.tvConceptAmount).text = "C$ ${fmt.format(det.descuento)}"
            row.findViewById<View>(R.id.conceptCreditNote).findViewById<TextView>(R.id.tvConceptLabel).text = "N/C"
            row.findViewById<View>(R.id.conceptCreditNote).findViewById<TextView>(R.id.tvConceptAmount).text = "C$ ${fmt.format(det.nc)}"

            container.addView(row)
        }

        view.findViewById<View>(R.id.btn_cerrar).setOnClickListener { dialog.dismiss() }

        view.findViewById<View>(R.id.btn_adjuntar).setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(this, AdjuntarComprobanteActivity::class.java)
                .putExtra("id_recibo", recibo.recibo.replace("#", "")))
        }

        ApiService.fetchAdjuntos(
            idRecibo = recibo.recibo.replace("#", ""),
            onResult = { adjuntos ->
                android.util.Log.d("Adjuntos", "Cargados ${adjuntos.size} adjuntos")
                for (a in adjuntos) {
                    android.util.Log.d("Adjuntos", "  URL: ${a.imagenUrl.take(80)}...")
                }

                val btnAdjuntar = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_adjuntar)
                btnAdjuntar.text = if (adjuntos.isNotEmpty()) "Ver comprobantes (${adjuntos.size})" else "Adjuntar comprobante"

                val rvAdjuntos = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_adjuntos)
                val tvTitle = view.findViewById<TextView>(R.id.tv_adjuntos_title)
                if (adjuntos.isEmpty()) {
                    rvAdjuntos.visibility = View.GONE
                    tvTitle.visibility = View.GONE
                    return@fetchAdjuntos
                }
                tvTitle.visibility = View.VISIBLE
                rvAdjuntos.visibility = View.VISIBLE
                rvAdjuntos.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false)
                rvAdjuntos.adapter = object : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
                    override fun getItemCount() = adjuntos.size
                    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): androidx.recyclerview.widget.RecyclerView.ViewHolder {
                        val v = LayoutInflater.from(this@RecibosActivity).inflate(R.layout.item_photo_attachment, parent, false)
                        return object : androidx.recyclerview.widget.RecyclerView.ViewHolder(v) {}
                    }
                    override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {
                        val img = holder.itemView.findViewById<android.widget.ImageView>(R.id.ivPhoto)
                        val btnRemove = holder.itemView.findViewById<android.widget.ImageButton>(R.id.btnRemove)
                        btnRemove.visibility = View.GONE
                        val item = adjuntos[position]
                        com.squareup.picasso.Picasso.get().load(item.imagenUrl)
                            .placeholder(R.drawable.ic_placeholder_product)
                            .resize(200, 150).centerCrop()
                            .into(img, object : com.squareup.picasso.Callback {
                                override fun onSuccess() {}
                                override fun onError(e: Exception?) {
                                    android.util.Log.e("Adjuntos", "Picasso error: ${e?.message} para URL: ${item.imagenUrl.take(80)}")
                                }
                            })
                        holder.itemView.setOnClickListener {
                            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(item.imagenUrl))
                            startActivity(intent)
                        }
                    }
                }
            },
            onError = { error ->
                android.util.Log.e("Adjuntos", "Error al cargar adjuntos: $error")
                Toast.makeText(this, "Error al cargar adjuntos: $error", Toast.LENGTH_LONG).show()
            }
        )

        view.findViewById<View>(R.id.btn_delete_recibo).setOnClickListener {
            dialog.dismiss()
            val pos = items.indexOfFirst { it.id == recibo.id }
            if (pos >= 0) confirmarEliminar(recibo, pos)
        }

        dialog.show()
    }

    private fun mostrarDialogoAnular() {
        val dialog = android.app.Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_add_recibo_anular)
        dialog.setCancelable(true)

        val edtRecibo = dialog.findViewById<EditText>(R.id.edt_cantidad_vineta)
        edtRecibo.hint = "Nº Recibo: "

        dialog.findViewById<TextView>(R.id.id_title_frm).text = "Digite Numero de Recibo a Anular"

        val tvFecha = dialog.findViewById<TextView>(R.id.id_text_recibo)
        tvFecha.text = "0000-00-00"

        val lp = WindowManager.LayoutParams()
        lp.copyFrom(dialog.window?.attributes)
        lp.width = WindowManager.LayoutParams.WRAP_CONTENT
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT

        tvFecha.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                tvFecha.text = "$y-${(m+1).toString().padStart(2,'0')}-${d.toString().padStart(2,'0')}"
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        dialog.findViewById<View>(R.id.bt_cancel).setOnClickListener { dialog.dismiss() }

        dialog.findViewById<View>(R.id.bt_add).setOnClickListener {
            val reciboStr = edtRecibo.text.toString()
            val fechaStr = tvFecha.text.toString()

            if (reciboStr.isEmpty() || fechaStr == "0000-00-00") {
                Toast.makeText(this, "Falta datos por completar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val numRec = try {
                val n = reciboStr.toInt()
                ("00000" + n.toString()).takeLast(5)
            } catch (_: Exception) { reciboStr.padStart(5, '0') }

            dialog.dismiss()

            val ruta = SessionManager.getUsername().uppercase()
            ApiService.anularReciboPorNumero(
                recibo = numRec,
                fechaRecibo = fechaStr,
                ruta = ruta,
                onResult = { response ->
                    runOnUiThread {
                        if (response.contains("Nuevo", ignoreCase = true)) {
                            Toast.makeText(this, "Recibo #$numRec anulado", Toast.LENGTH_LONG).show()
                            val desde = findViewById<TextView>(R.id.tvDateFrom).text.toString()
                            val hasta = findViewById<TextView>(R.id.tvDateTo).text.toString()
                            buscar(desde, hasta)
                        } else {
                            Toast.makeText(this, "Error: $response", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onError = { error ->
                    runOnUiThread {
                        Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show()
                    }
                }
            )
        }

        dialog.show()
        dialog.window?.attributes = lp
    }

    private fun confirmarEliminar(recibo: ItemReporteRecibo, position: Int) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Eliminar recibo")
            .setMessage("Eliminar recibo #${recibo.recibo} de ${recibo.nombreCliente}?")
            .setIcon(R.drawable.ic_trash)
            .setPositiveButton("Eliminar") { _, _ ->
                val numRecibo = recibo.id.replace("#", "")
                ApiService.eliminarRecibo(
                    reciboNumero = numRecibo,
                    onResult = { response ->
                        runOnUiThread {
                            if (response.contains("Anulado", ignoreCase = true)) {
                                items.removeAt(position)
                                adapter.notifyItemRemoved(position)
                                mostrarResultados()
                                Snackbar.make(findViewById(R.id.recyclerRecibos),
                                    "Recibo #${recibo.recibo} eliminado", Snackbar.LENGTH_LONG).show()
                            } else {
                                Snackbar.make(findViewById(R.id.recyclerRecibos),
                                    "Error al eliminar: $response", Snackbar.LENGTH_LONG).show()
                            }
                        }
                    },
                    onError = { error ->
                        runOnUiThread {
                            Snackbar.make(findViewById(R.id.recyclerRecibos),
                                "Error de conexión: $error", Snackbar.LENGTH_LONG).show()
                        }
                    }
                )
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                val m = (month + 1).toString().padStart(2, '0')
                val d = day.toString().padStart(2, '0')
                onDateSelected("$year-$m-$d")
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}