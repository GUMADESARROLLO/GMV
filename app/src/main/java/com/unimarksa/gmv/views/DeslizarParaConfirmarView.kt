package com.unimarksa.gmv.views

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.unimarksa.gmv.R
import kotlin.math.abs

/**
 * "Desliza para enviar": la acción solo se dispara al arrastrar el círculo hasta el otro extremo.
 *
 * - Seguimiento 1:1 respetando el punto donde se agarró el círculo (no salta al dedo).
 * - Al soltar se proyecta la posición final con la velocidad del gesto: un "flick" decidido confirma
 *   aunque no se llegue al final; soltar a medio camino regresa el círculo con desaceleración.
 * - La animación de regreso es interrumpible: se puede volver a agarrar el círculo en movimiento.
 * - Resistencia elástica en los bordes en vez de un tope seco.
 * - Vibración háptica al confirmar.
 * - Accesibilidad: con TalkBack se activa con doble toque (acción "Enviar").
 * - Respeta "Quitar animaciones" del sistema.
 */
class DeslizarParaConfirmarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class Direccion { IZQUIERDA_A_DERECHA, DERECHA_A_IZQUIERDA }

    /** Sentido del gesto. Por defecto de izquierda a derecha. */
    var direccion: Direccion = Direccion.IZQUIERDA_A_DERECHA
        set(value) { field = value; invalidate() }

    var texto: String = "Desliza para enviar"
        set(value) { field = value; contentDescription = value; invalidate() }

    /** Se invoca una sola vez cuando el gesto se completa. Llamar [reiniciar] para volver a habilitarlo. */
    var onConfirmado: (() -> Unit)? = null

    /** Muestra un indicador girando dentro del círculo mientras se envía. */
    var cargando: Boolean = false
        set(value) {
            field = value
            if (value) iniciarGiro() else detenerGiro()
            invalidate()
        }

    private val dp = resources.displayMetrics.density

    private val colorPista = ContextCompat.getColor(context, R.color.umk_royal)
    private val colorRastro = ContextCompat.getColor(context, R.color.umk_royal_2)
    private val colorPulgar = ContextCompat.getColor(context, R.color.umk_paper)
    private val colorIcono = ContextCompat.getColor(context, R.color.umk_royal)
    private val colorTexto = ContextCompat.getColor(context, R.color.umk_paper)
    private val colorDeshabilitado = ContextCompat.getColor(context, R.color.umk_silver)

    private val pPista = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pRastro = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colorRastro }
    private val pPulgar = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorPulgar
        setShadowLayer(4 * dp, 0f, 1 * dp, 0x33000000)
    }
    private val pTexto = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorTexto
        textSize = 16 * resources.displayMetrics.scaledDensity
        textAlign = Paint.Align.CENTER
        typeface = ResourcesCompat.getFont(context, R.font.archivo)?.let { Typeface.create(it, 700, false) }
            ?: Typeface.DEFAULT_BOLD
    }
    private val pChevron = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorTexto
        style = Paint.Style.STROKE
        strokeWidth = 2 * dp
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val pCarga = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorIcono
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * dp
        strokeCap = Paint.Cap.ROUND
    }
    private val icono = ContextCompat.getDrawable(context, R.drawable.ic_send)?.mutate()?.apply { setTint(colorIcono) }
    private val iconoListo = ContextCompat.getDrawable(context, R.drawable.ic_check)?.mutate()?.apply { setTint(colorIcono) }

    private val rect = RectF()
    private val pathChevron = Path()

    private val margen = 4 * dp
    private var desplazamiento = 0f          // distancia recorrida desde el inicio (0..recorrido)
    private var offsetAgarre = 0f            // dónde se agarró el círculo respecto a su centro
    private var arrastrando = false
    private var confirmado = false
    private var anguloCarga = 0f
    private var animMovimiento: ValueAnimator? = null
    private var animGiro: ValueAnimator? = null
    private var tracker: VelocityTracker? = null

    init {
        isClickable = true
        isFocusable = true
        contentDescription = texto
    }

    // ── Medidas ──────────────────────────────────────────────────────────────

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val alto = resolveSize((56 * dp).toInt(), heightMeasureSpec)
        val ancho = resolveSize((280 * dp).toInt(), widthMeasureSpec)
        setMeasuredDimension(ancho, alto)
    }

    private val diametro get() = height - 2 * margen
    private val radio get() = diametro / 2f
    private val recorrido get() = (width - 2 * margen - diametro).coerceAtLeast(0f)
    private val progreso get() = if (recorrido > 0f) (desplazamiento / recorrido).coerceIn(0f, 1f) else 0f

    private fun centroPulgarX(): Float =
        if (direccion == Direccion.IZQUIERDA_A_DERECHA) margen + radio + desplazamiento
        else width - margen - radio - desplazamiento

    /** Convierte una X de pantalla a "distancia recorrida" según la dirección. */
    private fun distanciaDesdeX(x: Float): Float =
        if (direccion == Direccion.IZQUIERDA_A_DERECHA) x - (margen + radio)
        else (width - margen - radio) - x

    // ── Dibujo ───────────────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        val h = height.toFloat()
        val w = width.toFloat()
        val r = h / 2f
        val cx = centroPulgarX()
        val cy = h / 2f

        // Pista
        pPista.color = if (isEnabled) colorPista else colorDeshabilitado
        rect.set(0f, 0f, w, h)
        canvas.drawRoundRect(rect, r, r, pPista)

        // Rastro desde el inicio hasta el círculo: anticipa hacia dónde va el gesto
        if (desplazamiento > 0f) {
            if (direccion == Direccion.IZQUIERDA_A_DERECHA) rect.set(0f, 0f, cx + radio + margen, h)
            else rect.set(cx - radio - margen, 0f, w, h)
            pRastro.alpha = (90 + 165 * progreso).toInt()
            canvas.drawRoundRect(rect, r, r, pRastro)
        }

        // Texto + chevrons: se desvanecen a medida que avanza el círculo
        val alfaTexto = ((1f - progreso * 1.6f).coerceIn(0f, 1f) * 255).toInt()
        if (alfaTexto > 0) {
            pTexto.alpha = alfaTexto
            pChevron.alpha = (alfaTexto * 0.7f).toInt()
            val inicioLibre = if (direccion == Direccion.IZQUIERDA_A_DERECHA) diametro + margen * 2 else 0f
            val anchoLibre = w - diametro - margen * 2
            val centroTexto = inicioLibre + anchoLibre / 2f
            val textoVisible = TextUtils.ellipsize(texto, pTexto, anchoLibre - 48 * dp, TextUtils.TruncateAt.END).toString()
            val baseY = cy - (pTexto.descent() + pTexto.ascent()) / 2f
            canvas.drawText(textoVisible, centroTexto, baseY, pTexto)

            // Dos chevrons del lado hacia donde hay que deslizar
            val anchoTexto = pTexto.measureText(textoVisible)
            val s = 4.5f * dp
            val hacia = if (direccion == Direccion.IZQUIERDA_A_DERECHA) 1f else -1f
            val baseX = centroTexto + hacia * (anchoTexto / 2f + 14 * dp)
            for (i in 0..1) {
                val x = baseX + hacia * i * 8 * dp
                pathChevron.reset()
                pathChevron.moveTo(x - hacia * s, cy - s * 1.3f)
                pathChevron.lineTo(x, cy)
                pathChevron.lineTo(x - hacia * s, cy + s * 1.3f)
                canvas.drawPath(pathChevron, pChevron)
            }
        }

        // Círculo
        canvas.drawCircle(cx, cy, radio, pPulgar)

        if (cargando) {
            val rc = radio * 0.45f
            rect.set(cx - rc, cy - rc, cx + rc, cy + rc)
            canvas.drawArc(rect, anguloCarga, 270f, false, pCarga)
        } else {
            val d = if (confirmado) iconoListo else icono
            d?.let {
                val t = (22 * dp).toInt() / 2
                it.setBounds((cx - t).toInt(), (cy - t).toInt(), (cx + t).toInt(), (cy + t).toInt())
                canvas.save()
                // El ícono de enviar apunta a la derecha: se refleja cuando el gesto va a la izquierda
                if (!confirmado && direccion == Direccion.DERECHA_A_IZQUIERDA) canvas.scale(-1f, 1f, cx, cy)
                it.draw(canvas)
                canvas.restore()
            }
        }
    }

    // ── Gesto ────────────────────────────────────────────────────────────────

    @SuppressLint("ClickableViewAccessibility") // la alternativa accesible es performClick()
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled || confirmado || cargando) return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Solo se agarra el círculo (con holgura táctil de 12dp)
                if (abs(event.x - centroPulgarX()) > radio + 12 * dp) return false
                animMovimiento?.cancel() // interrumpe el regreso y continúa desde la posición actual
                arrastrando = true
                parent?.requestDisallowInterceptTouchEvent(true)
                offsetAgarre = distanciaDesdeX(event.x) - desplazamiento
                tracker?.recycle()
                tracker = VelocityTracker.obtain().also { it.addMovement(event) }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!arrastrando) return false
                tracker?.addMovement(event)
                desplazamiento = conResistencia(distanciaDesdeX(event.x) - offsetAgarre)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (!arrastrando) return false
                tracker?.addMovement(event)
                tracker?.computeCurrentVelocity(1000)
                val vx = tracker?.xVelocity ?: 0f
                val velocidad = if (direccion == Direccion.IZQUIERDA_A_DERECHA) vx else -vx
                soltar(velocidad)
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                if (!arrastrando) return false
                soltar(0f)
                return true
            }
        }
        return false
    }

    /** Resistencia elástica fuera de [0, recorrido]: el círculo sigue al dedo cada vez menos. */
    private fun conResistencia(valor: Float): Float {
        val max = recorrido
        val dim = diametro
        return when {
            valor < 0f -> -rubberband(-valor, dim)
            valor > max -> max + rubberband(valor - max, dim)
            else -> valor
        }
    }

    private fun rubberband(exceso: Float, dimension: Float, c: Float = 0.55f): Float =
        (exceso * dimension * c) / (dimension + c * exceso)

    private fun soltar(velocidad: Float) {
        arrastrando = false
        parent?.requestDisallowInterceptTouchEvent(false)
        tracker?.recycle()
        tracker = null

        // Proyección del punto de reposo según la velocidad (desaceleración 0.99)
        val proyectado = desplazamiento + (velocidad / 1000f) * 0.99f / (1f - 0.99f)
        val confirma = desplazamiento >= recorrido * 0.5f && proyectado >= recorrido * 0.9f ||
            desplazamiento >= recorrido * 0.9f

        if (confirma) animarA(recorrido) { completar() }
        else animarA(0f, null)
    }

    private fun animarA(destino: Float, alTerminar: (() -> Unit)?) {
        animMovimiento?.cancel()
        if (!ValueAnimator.areAnimatorsEnabled()) {
            desplazamiento = destino
            invalidate()
            alTerminar?.invoke()
            return
        }
        val distancia = abs(destino - desplazamiento)
        animMovimiento = ValueAnimator.ofFloat(desplazamiento, destino).apply {
            duration = (120 + 160 * (distancia / recorrido.coerceAtLeast(1f))).toLong()
            interpolator = DecelerateInterpolator(1.6f)
            addUpdateListener {
                desplazamiento = it.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                private var cancelado = false
                override fun onAnimationCancel(animation: Animator) { cancelado = true }
                override fun onAnimationEnd(animation: Animator) { if (!cancelado) alTerminar?.invoke() }
            })
            start()
        }
    }

    private fun completar() {
        if (confirmado) return
        confirmado = true
        performHapticFeedback(
            if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM
            else HapticFeedbackConstants.LONG_PRESS
        )
        announceForAccessibility("Enviando")
        invalidate()
        onConfirmado?.invoke()
    }

    /** Devuelve el círculo al inicio y vuelve a habilitar el gesto (p. ej. tras un error). */
    fun reiniciar() {
        confirmado = false
        cargando = false
        animarA(0f, null)
    }

    // ── Accesibilidad: doble toque con TalkBack ──────────────────────────────

    override fun performClick(): Boolean {
        super.performClick()
        if (!isEnabled || confirmado || cargando) return false
        animarA(recorrido) { completar() }
        return true
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = android.widget.Button::class.java.name
        info.addAction(AccessibilityNodeInfo.AccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK, "Enviar"))
    }

    // ── Indicador de carga ───────────────────────────────────────────────────

    private fun iniciarGiro() {
        if (animGiro != null) return
        animGiro = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 900
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                anguloCarga = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun detenerGiro() {
        animGiro?.cancel()
        animGiro = null
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        invalidate()
    }

    override fun onDetachedFromWindow() {
        animMovimiento?.cancel()
        detenerGiro()
        tracker?.recycle()
        tracker = null
        super.onDetachedFromWindow()
    }
}
