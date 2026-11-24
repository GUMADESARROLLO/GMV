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
import com.unimarksa.gmv.tools.BarrasSistema
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

        BarrasSistema.aplicar(this, R.color.umk_royal)


        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar.navigationIcon?.setTint(ContextCompat.getColor(this, android.R.color.white))
        toolbar.setNavigationOnClickListener { finish() }

        val codCliente = intent.getStringExtra("codCliente") ?: return

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
                mostrarLista(items)
            },
            onError = {
                findViewById<View>(R.id.lyt_loading).visibility = View.GONE
                findViewById<View>(R.id.lyt_empty_result).visibility = View.VISIBLE
            }
        )
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
