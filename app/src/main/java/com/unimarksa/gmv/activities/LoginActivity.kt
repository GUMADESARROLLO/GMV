package com.unimarksa.gmv.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
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

        val root = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (Build.VERSION.SDK_INT >= 35) {
            val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
            val statusBarHeight = if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
            val scrim = View(this)
            scrim.setBackgroundColor(ContextCompat.getColor(this, R.color.colorUmkPrimaryDark))
            val content = findViewById<ViewGroup>(android.R.id.content)
            scrim.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, statusBarHeight)
            content.addView(scrim)
        } else {
            window.statusBarColor = ContextCompat.getColor(this, R.color.colorUmkPrimaryDark)
        }

        ViewCompat.getWindowInsetsController(window.decorView)?.isAppearanceLightStatusBars = false

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
