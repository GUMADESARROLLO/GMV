package com.unimarksa.gmv.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.unimarksa.gmv.R
import com.unimarksa.gmv.adapters.FacturaHistoricoAdapter
import com.unimarksa.gmv.models.ItemFacturaPendiente
import com.unimarksa.gmv.models.ItemReporteFactura
import com.unimarksa.gmv.tools.ApiService

class HistoricoFacturaActivity : AppCompatActivity() {

    private var todasLasFacturas = mutableListOf<ItemReporteFactura>()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historico_factura)

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

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar.navigationIcon?.setTint(ContextCompat.getColor(this, android.R.color.white))
        toolbar.setNavigationOnClickListener { finish() }

        val codCliente = intent.getStringExtra("codCliente") ?: return
        val nomCliente = intent.getStringExtra("nomCliente") ?: ""
        val codTexto = intent.getStringExtra("codTexto") ?: ""

        findViewById<TextView>(R.id.tv_client_name).text = nomCliente
        findViewById<TextView>(R.id.tv_client_code).text = codTexto

        findViewById<TextView>(R.id.tv_avatar).text = nomCliente.take(2).uppercase()

        setupSearch()
        cargarHistorico(codCliente)
    }

    private fun setupSearch() {
        val etBuscar = findViewById<EditText>(R.id.et_buscar)
        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filtrar(s.toString())
            }
        })
        etBuscar.setOnEditorActionListener { _, _, _ ->
            filtrar(etBuscar.text.toString()); true
        }
    }

    private fun filtrar(query: String) {
        val filtered = if (query.isBlank()) {
            todasLasFacturas
        } else {
            todasLasFacturas.filter {
                it.numero.contains(query, ignoreCase = true) ||
                it.total.contains(query) ||
                it.cliente.contains(query, ignoreCase = true)
            }
        }
        mostrarLista(filtered)
    }

    private fun cargarHistorico(codCliente: String) {
        findViewById<View>(R.id.lyt_loading).visibility = View.VISIBLE
        findViewById<View>(R.id.lyt_empty_result).visibility = View.GONE

        ApiService.fetchHistoricoFactura(
            codCliente = codCliente,
            onResult = { items ->
                findViewById<View>(R.id.lyt_loading).visibility = View.GONE
                todasLasFacturas.clear()
                todasLasFacturas.addAll(items)
                calcularResumen(items)
                mostrarLista(items)
            },
            onError = {
                findViewById<View>(R.id.lyt_loading).visibility = View.GONE
                findViewById<View>(R.id.lyt_empty_result).visibility = View.VISIBLE
            }
        )
    }

    private fun calcularResumen(items: List<ItemReporteFactura>) {
        var totalPagado = 0.0
        var totalPendiente = 0.0
        var totalPeriodo = 0.0

        for (f in items) {
            val monto = f.total.replace(",", "").trim().toDoubleOrNull() ?: 0.0
            val saldo = f.saldo.replace(",", "").trim().toDoubleOrNull() ?: 0.0
            totalPeriodo += monto
            if (saldo <= 0.0) {
                totalPagado += monto
            } else {
                totalPendiente += saldo
            }
        }

        val fmt = { v: Double -> "C$ ${"%,.2f".format(v)}" }
        findViewById<TextView>(R.id.tv_total_pagado).text = fmt(totalPagado)
        findViewById<TextView>(R.id.tv_total_pendiente).text = fmt(totalPendiente)
        findViewById<TextView>(R.id.tv_total_periodo).text = fmt(totalPeriodo)
    }

    private fun mostrarLista(items: List<ItemReporteFactura>) {
        if (items.isEmpty()) {
            findViewById<View>(R.id.lyt_empty_result).visibility = View.VISIBLE
            findViewById<View>(R.id.recycler_view).visibility = View.GONE
        } else {
            findViewById<View>(R.id.lyt_empty_result).visibility = View.GONE
            findViewById<View>(R.id.recycler_view).visibility = View.VISIBLE
        }

        findViewById<TextView>(R.id.tv_results_count).text = "${items.size} resultados"

        val rv = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recycler_view)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = FacturaHistoricoAdapter(items) { factura ->
            val intent = Intent(this, FacturaDetailActivity::class.java)
            intent.putExtra("factura", ItemFacturaPendiente(
                numero = factura.numero,
                saldo = factura.saldo.ifEmpty { factura.total },
                vencimiento = factura.vencimiento.ifEmpty { factura.fecha },
                saldoActual = factura.saldo.ifEmpty { factura.total }
            ))
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }
}
