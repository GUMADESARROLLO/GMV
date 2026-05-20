package com.unimarksa.gmv.activities

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.unimarksa.gmv.R
import com.unimarksa.gmv.models.ItemCliente

class ClienteDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cliente_detail)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.nested_content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<android.widget.ImageButton>(R.id.btn_back).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val cliente = intent.getSerializableExtra("cliente") as? ItemCliente
        cliente?.let { bindData(it) }
    }

    private fun bindData(cliente: ItemCliente) {
        findViewById<android.widget.TextView>(R.id.id_perfil_name_cliente).text = cliente.nombre
        findViewById<android.widget.TextView>(R.id.id_tele).text = "Tel. ${cliente.telefono}"
        findViewById<android.widget.TextView>(R.id.txt_direccion).text = cliente.direccion
        findViewById<android.widget.TextView>(R.id.id_perfil_disponible).text = "C$ ${cliente.disponible}"
        findViewById<android.widget.TextView>(R.id.id_perfil_saldo).text = "C$ ${cliente.saldo}"
        findViewById<android.widget.TextView>(R.id.id_perfil_limite).text = "C$ ${cliente.limite}"
    }
}
