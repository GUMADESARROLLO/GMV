package com.unimarksa.gmv.views

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.unimarksa.gmv.R
import kotlin.math.abs

/**
 * Envuelve una tarjeta para eliminarla deslizándola hacia la izquierda (patrón iOS / Gmail).
 *
 * - El gesto solo se toma cuando el movimiento es claramente horizontal (supera el touch slop y
 *   es mayor que el vertical), así el scroll vertical de la lista sigue funcionando.
 * - La tarjeta sigue al dedo 1:1 respetando dónde se agarró; a la derecha opone resistencia elástica.
 * - Debajo aparece un fondo rojo con la papelera, que crece a medida que avanza el gesto.
 * - Al soltar se proyecta la posición con la velocidad: un "flick" decidido elimina aunque no se
 *   haya llegado al umbral; si no, la tarjeta regresa. El regreso es interrumpible.
 * - Al eliminar: la tarjeta sale de pantalla, el hueco se cierra y se llama [onEliminar].
 * - Accesibilidad: acción "Eliminar" disponible para TalkBack.
 */
class FilaDeslizable(context: Context, val contenido: View) : FrameLayout(context) {

    var onEliminar: (() -> Unit)? = null

    private val dp = resources.displayMetrics.density
    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private val icono: ImageView
    private val fondo: FrameLayout

    private var xInicio = 0f
    private var yInicio = 0f
    private var offsetAgarre = 0f
    private var arrastrando = false
    private var eliminando = false
    private var tracker: VelocityTracker? = null
    private var anim: ValueAnimator? = null

    init {
        // Fondo rojo con la papelera alineada a la derecha
        fondo = FrameLayout(context).apply {
            setBackgroundResource(R.drawable.bg_fondo_eliminar)
            alpha = 0f
        }
        icono = ImageView(context).apply {
            setImageResource(R.drawable.ic_trash)
            imageTintList = ContextCompat.getColorStateList(context, R.color.umk_paper)
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        val tam = (24 * dp).toInt()
        fondo.addView(icono, LayoutParams(tam, tam, Gravity.END or Gravity.CENTER_VERTICAL).apply {
            marginEnd = (24 * dp).toInt()
        })

        // La tarjeta conserva sus márgenes como margen del contenedor
        val lpOriginal = contenido.layoutParams
        layoutParams = lpOriginal ?: ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val margenSuperior = (lpOriginal as? ViewGroup.MarginLayoutParams)?.topMargin ?: 0

        addView(fondo, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).apply {
            topMargin = margenSuperior
        })
        addView(contenido, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = margenSuperior
        })
        (layoutParams as? ViewGroup.MarginLayoutParams)?.topMargin = 0
    }

    private val ancho get() = contenido.width.toFloat().coerceAtLeast(1f)

    private fun aplicar(tx: Float) {
        contenido.translationX = tx
        val progreso = (-tx / (ancho * 0.35f)).coerceIn(0f, 1f)
        fondo.alpha = if (tx < 0f) 1f else 0f
        // El ícono "crece hacia el dedo": anticipa que soltar ahí elimina
        val escala = 0.7f + 0.4f * progreso
        icono.scaleX = escala
        icono.scaleY = escala
        icono.alpha = 0.5f + 0.5f * progreso
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (eliminando) return true
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                xInicio = ev.x
                yInicio = ev.y
                anim?.cancel() // interrumpe un regreso en curso
                offsetAgarre = contenido.translationX
                tracker?.recycle()
                tracker = VelocityTracker.obtain().also { it.addMovement(ev) }
            }
            MotionEvent.ACTION_MOVE -> {
                tracker?.addMovement(ev)
                val dx = ev.x - xInicio
                val dy = ev.y - yInicio
                if (abs(dx) > slop && abs(dx) > abs(dy) * 1.2f) {
                    arrastrando = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                    // Descontar el slop para que no haya salto al empezar
                    xInicio += if (dx > 0) slop else -slop
                    return true
                }
            }
        }
        return false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (eliminando) return true
        tracker?.addMovement(ev)
        when (ev.actionMasked) {
            // Si la tarjeta no consumió el DOWN, lo tomamos para poder recibir el resto del gesto
            MotionEvent.ACTION_DOWN -> return true
            MotionEvent.ACTION_MOVE -> {
                if (!arrastrando) {
                    val dx = ev.x - xInicio
                    if (abs(dx) > slop && abs(dx) > abs(ev.y - yInicio) * 1.2f) {
                        arrastrando = true
                        parent?.requestDisallowInterceptTouchEvent(true)
                        xInicio += if (dx > 0) slop else -slop
                    } else return true
                }
                val crudo = offsetAgarre + (ev.x - xInicio)
                // A la derecha no hay nada: resistencia elástica
                val tx = if (crudo > 0f) rubberband(crudo, ancho) else crudo
                aplicar(tx)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (!arrastrando) return false
                arrastrando = false
                parent?.requestDisallowInterceptTouchEvent(false)
                tracker?.computeCurrentVelocity(1000)
                val vx = if (ev.actionMasked == MotionEvent.ACTION_CANCEL) 0f else tracker?.xVelocity ?: 0f
                tracker?.recycle()
                tracker = null

                val actual = contenido.translationX
                val proyectado = actual + (vx / 1000f) * 0.99f / (1f - 0.99f)
                val elimina = actual < -ancho * 0.15f && proyectado < -ancho * 0.45f || actual < -ancho * 0.5f
                if (elimina) eliminar() else animarA(0f, null)
                return true
            }
        }
        return arrastrando
    }

    private fun rubberband(exceso: Float, dimension: Float, c: Float = 0.55f) =
        (exceso * dimension * c) / (dimension + c * exceso)

    private fun animarA(destino: Float, alTerminar: (() -> Unit)?) {
        anim?.cancel()
        if (!ValueAnimator.areAnimatorsEnabled()) {
            aplicar(destino); alTerminar?.invoke(); return
        }
        val desde = contenido.translationX
        anim = ValueAnimator.ofFloat(desde, destino).apply {
            duration = (140 + 160 * (abs(destino - desde) / ancho)).toLong()
            interpolator = DecelerateInterpolator(1.6f)
            addUpdateListener { aplicar(it.animatedValue as Float) }
            addListener(object : AnimatorListenerAdapter() {
                private var cancelado = false
                override fun onAnimationCancel(animation: Animator) { cancelado = true }
                override fun onAnimationEnd(animation: Animator) { if (!cancelado) alTerminar?.invoke() }
            })
            start()
        }
    }

    /** Saca la tarjeta por la izquierda, cierra el hueco y avisa. */
    fun eliminar() {
        if (eliminando) return
        eliminando = true
        performHapticFeedback(
            if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS
        )
        animarA(-ancho - 24 * dp) {
            if (!ValueAnimator.areAnimatorsEnabled()) { onEliminar?.invoke(); return@animarA }
            val alto = height
            ValueAnimator.ofInt(alto, 0).apply {
                duration = 180
                interpolator = DecelerateInterpolator()
                addUpdateListener {
                    layoutParams = layoutParams.apply { height = it.animatedValue as Int }
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) { onEliminar?.invoke() }
                })
                start()
            }
        }
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.addAction(AccessibilityNodeInfo.AccessibilityAction(R.id.accion_eliminar_pago, "Eliminar pago"))
    }

    override fun performAccessibilityAction(action: Int, arguments: android.os.Bundle?): Boolean {
        if (action == R.id.accion_eliminar_pago) { eliminar(); return true }
        return super.performAccessibilityAction(action, arguments)
    }

    override fun onDetachedFromWindow() {
        anim?.cancel()
        tracker?.recycle()
        tracker = null
        super.onDetachedFromWindow()
    }
}
