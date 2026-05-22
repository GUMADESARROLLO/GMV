package com.unimarksa.gmv.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.unimarksa.gmv.R
import com.unimarksa.gmv.adapters.ReporteFacturaAdapter
import com.unimarksa.gmv.models.ItemFacturaPendiente
import com.unimarksa.gmv.tools.ApiService

class HistoricoFacturaActivity : AppCompatActivity() {

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

        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        val codCliente = intent.getStringExtra("codCliente") ?: return
        cargarHistorico(codCliente)
    }

    private fun cargarHistorico(codCliente: String) {
        findViewById<View>(R.id.lyt_loading).visibility = View.VISIBLE
        findViewById<View>(R.id.lyt_empty).visibility = View.GONE

        ApiService.fetchHistoricoFactura(
            codCliente = codCliente,
            onResult = { items ->
                findViewById<View>(R.id.lyt_loading).visibility = View.GONE
                if (items.isEmpty()) {
                    findViewById<View>(R.id.lyt_empty).visibility = View.VISIBLE
                } else {
                    val rv = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recycler_historico)
                    rv.layoutManager = LinearLayoutManager(this)
                    rv.adapter = ReporteFacturaAdapter(items) { factura ->
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
            },
            onError = {
                findViewById<View>(R.id.lyt_loading).visibility = View.GONE
                findViewById<View>(R.id.lyt_empty).visibility = View.VISIBLE
            }
        )
    }
}
