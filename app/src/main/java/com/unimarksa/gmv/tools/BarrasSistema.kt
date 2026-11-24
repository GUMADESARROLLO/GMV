package com.unimarksa.gmv.tools

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Pinta la barra de estado con el color del header de la pantalla y elige íconos
 * claros u oscuros según la luminancia de ese color.
 *
 * - API < 35: usa window.statusBarColor.
 * - API 35+: el edge-to-edge es obligatorio y statusBarColor se ignora, así que se
 *   superpone una vista del alto real del inset (no del dimen del sistema) en android.R.id.content.
 *
 * Llamar después de setContentView. Es idempotente: reutiliza la misma vista si se llama dos veces.
 */
object BarrasSistema {

    private const val TAG_SCRIM = "gmv_scrim_barra_estado"

    fun aplicar(activity: Activity, @ColorRes colorHeader: Int) {
        val color = ContextCompat.getColor(activity, colorHeader)
        val window = activity.window
        val iconosOscuros = ColorUtils.calculateLuminance(color) > 0.5

        if (Build.VERSION.SDK_INT < 35) {
            @Suppress("DEPRECATION")
            window.statusBarColor = color
        } else {
            val content = activity.findViewById<ViewGroup>(android.R.id.content)
            val scrim = content.findViewWithTag<View>(TAG_SCRIM) ?: View(activity).also { v ->
                v.tag = TAG_SCRIM
                v.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                content.addView(v, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, alturaInicial(activity)))
                ViewCompat.setOnApplyWindowInsetsListener(v) { vista, insets ->
                    val alto = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
                    if (vista.layoutParams.height != alto) {
                        vista.layoutParams.height = alto
                        vista.requestLayout()
                    }
                    insets
                }
                ViewCompat.requestApplyInsets(v)
            }
            scrim.setBackgroundColor(color)
            scrim.bringToFront()
        }

        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = iconosOscuros
    }

    private fun alturaInicial(activity: Activity): Int {
        val id = activity.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) activity.resources.getDimensionPixelSize(id) else 0
    }
}
