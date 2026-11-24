package com.unimarksa.gmv.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.unimarksa.gmv.R
import com.unimarksa.gmv.tools.ApiService
import com.unimarksa.gmv.tools.SessionManager

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        SessionManager.init(this)

        // Arriba blanco (íconos oscuros); la franja azul inferior absorbe el inset de la barra de navegación
        val root = findViewById<View>(R.id.main)
        val band = findViewById<View>(R.id.loginBand)
        val bandPaddingBottom = band.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            band.setPadding(band.paddingLeft, band.paddingTop, band.paddingRight, bandPaddingBottom + systemBars.bottom)
            insets
        }

        if (Build.VERSION.SDK_INT < 35) {
            window.statusBarColor = ContextCompat.getColor(this, R.color.umk_paper)
            window.navigationBarColor = ContextCompat.getColor(this, R.color.umk_royal)
        }
        ViewCompat.getWindowInsetsController(window.decorView)?.apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = false
        }

        // "Listo" en el teclado envía el formulario
        findViewById<EditText>(R.id.edtPassword).setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                findViewById<MaterialButton>(R.id.btnLogin).performClick(); true
            } else false
        }

        findViewById<MaterialButton>(R.id.btnLogin).setOnClickListener {
            val usuario = findViewById<EditText>(R.id.edtUsername).text?.toString()?.trim() ?: ""
            val contrasena = findViewById<EditText>(R.id.edtPassword).text?.toString()?.trim() ?: ""

            if (usuario.isEmpty() || contrasena.isEmpty()) {
                findViewById<TextView>(R.id.tvError).visibility = View.VISIBLE
                findViewById<TextView>(R.id.tvError).text = "Ingresa usuario y contraseña"
                return@setOnClickListener
            }

            login(usuario, contrasena)
        }
    }

    private fun login(usuario: String, contrasena: String) {
        val btn = findViewById<MaterialButton>(R.id.btnLogin)
        val loading = findViewById<View>(R.id.lytLoading)
        val tvError = findViewById<TextView>(R.id.tvError)

        btn.isEnabled = false
        btn.text = ""
        loading.visibility = View.VISIBLE
        tvError.visibility = View.GONE

        ApiService.fetchLogin(
            usuario = usuario,
            contrasena = contrasena,
            onSuccess = { name, fullName, phone, email ->
                SessionManager.saveLogin(name, fullName, phone, email)
                SessionManager.savePassword(contrasena)
                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
            },
            onError = { error ->
                btn.isEnabled = true
                btn.text = "Entrar"
                loading.visibility = View.GONE
                tvError.visibility = View.VISIBLE
                tvError.text = error
            }
        )
    }
}
