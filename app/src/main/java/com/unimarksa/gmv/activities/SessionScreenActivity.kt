package com.unimarksa.gmv.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.unimarksa.gmv.R
import com.unimarksa.gmv.tools.SessionManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SessionScreenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_session_screen)

        val fullName = SessionManager.getFullName()
        val initial = fullName.firstOrNull()?.uppercaseChar()?.toString() ?: "U"
        val firstName = fullName.split(" ").firstOrNull() ?: ""

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when {
            hour < 12 -> "Buenos días"
            hour < 18 -> "Buenas tardes"
            else -> "Buenas noches"
        }

        findViewById<TextView>(R.id.txtGreeting).text = greeting
        findViewById<TextView>(R.id.tvAvatar).text = initial
        findViewById<TextView>(R.id.txtWelcome).text = "¡Bienvenido $firstName!"

        findViewById<View>(R.id.cardSession).setOnClickListener {
            verificarBiometrico()
        }

        findViewById<View>(R.id.txtOtherUser).setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Cambiar usuario")
                .setMessage("Se cerrará la sesión actual")
                .setPositiveButton("Cerrar sesión") { _, _ ->
                    SessionManager.logout()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finishAffinity()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun verificarBiometrico() {
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                val executor = ContextCompat.getMainExecutor(this)
                val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        irAlDashboard()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        if (errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                            Toast.makeText(this@SessionScreenActivity, "Error: $errString", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        Toast.makeText(this@SessionScreenActivity, "No se reconoció la huella", Toast.LENGTH_SHORT).show()
                    }
                })

                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Verificación biométrica")
                    .setSubtitle("Confirma tu identidad para acceder")
                    .setNegativeButtonText("Cancelar")
                    .build()

                prompt.authenticate(promptInfo)
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Toast.makeText(this, "Este dispositivo no tiene sensor biométrico", Toast.LENGTH_LONG).show()
                irAlDashboard()
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Toast.makeText(this, "Sensor biométrico no disponible", Toast.LENGTH_LONG).show()
                irAlDashboard()
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Toast.makeText(this, "No hay huellas registradas en el dispositivo", Toast.LENGTH_LONG).show()
                irAlDashboard()
            }
        }
    }

    private fun irAlDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}