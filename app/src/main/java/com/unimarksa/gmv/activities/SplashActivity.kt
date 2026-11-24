package com.unimarksa.gmv.activities

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.unimarksa.gmv.R
import com.unimarksa.gmv.data.local.AppDatabase
import com.unimarksa.gmv.tools.CartManager
import com.unimarksa.gmv.tools.PagoManager
import com.unimarksa.gmv.tools.SessionManager

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        SessionManager.init(this)
        val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        CartManager.init(AppDatabase.getInstance(this), androidId)
        PagoManager.init(AppDatabase.getInstance(this))
        PagoManager.loadFromDb()

        if (SessionManager.isLoggedIn()) {
            startActivity(Intent(this, SessionScreenActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
            return
        }

        window.navigationBarColor = androidx.core.content.ContextCompat.getColor(this, R.color.umk_royal)
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = false
        }

        // Entrada corta y desacelerada: la marca se asienta sin retrasar el acceso (antes 2.5 s de espera artificial)
        val logo = findViewById<android.widget.ImageView>(R.id.iv_splash_logo)
        logo.scaleX = 0.96f
        logo.scaleY = 0.96f
        logo.alpha = 0f

        val scaleX = ObjectAnimator.ofFloat(logo, "scaleX", 0.96f, 1.0f).apply {
            duration = 600
            interpolator = DecelerateInterpolator()
        }
        val scaleY = ObjectAnimator.ofFloat(logo, "scaleY", 0.96f, 1.0f).apply {
            duration = 600
            interpolator = DecelerateInterpolator()
        }
        val logoAlpha = ObjectAnimator.ofFloat(logo, "alpha", 0f, 1f).apply {
            duration = 400
            interpolator = DecelerateInterpolator()
        }
        val alpha = ObjectAnimator.ofFloat(
            findViewById(R.id.tv_splash_brand), "alpha", 0.0f, 1f
        ).apply {
            duration = 400
            startDelay = 200
            interpolator = DecelerateInterpolator()
        }

        AnimatorSet().apply {
            playTogether(scaleX, scaleY, logoAlpha, alpha)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    finish()
                }
            })
            start()
        }
    }
}
