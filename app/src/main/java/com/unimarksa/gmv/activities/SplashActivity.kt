package com.unimarksa.gmv.activities

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.animation.AccelerateDecelerateInterpolator
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

        window?.decorView?.apply {
            systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
        }

        val logo = findViewById<android.widget.ImageView>(R.id.iv_splash_logo)
        logo.scaleX = 0.92f
        logo.scaleY = 0.92f

        val scaleX = ObjectAnimator.ofFloat(logo, "scaleX", 0.92f, 1.0f).apply {
            duration = 2500
            interpolator = AccelerateDecelerateInterpolator()
        }
        val scaleY = ObjectAnimator.ofFloat(logo, "scaleY", 0.92f, 1.0f).apply {
            duration = 2500
            interpolator = AccelerateDecelerateInterpolator()
        }
        val alpha = ObjectAnimator.ofFloat(
            findViewById(R.id.tv_splash_brand), "alpha", 0.0f, 0.8f
        ).apply {
            duration = 2000
            startDelay = 200
            interpolator = AccelerateDecelerateInterpolator()
        }

        AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
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
