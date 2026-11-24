package com.unimarksa.gmv.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.unimarksa.gmv.R
import kotlin.math.min

class AnimatedProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var barColor: Int = Color.parseColor("#2B3B9E")
    private var trackColor: Int = Color.parseColor("#E9ECF8")
    private var progress: Float = 0f
    private var cornerRadius: Float = 8f
    private var animatedProgress = 0f

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val trackRect = RectF()
    private val barRect = RectF()

    init {
        attrs?.let {
            val ta = context.obtainStyledAttributes(it, R.styleable.AnimatedProgressBar)
            barColor = ta.getColor(R.styleable.AnimatedProgressBar_apbColor, barColor)
            trackColor = ta.getColor(R.styleable.AnimatedProgressBar_apbTrackColor, trackColor)
            progress = ta.getFloat(R.styleable.AnimatedProgressBar_apbProgress, 0f)
            cornerRadius = ta.getDimension(R.styleable.AnimatedProgressBar_apbCornerRadius, cornerRadius)
            ta.recycle()
        }
        trackPaint.color = trackColor
        barPaint.color = barColor
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        trackRect.set(0f, 0f, w.toFloat(), h.toFloat())
        barPaint.shader = LinearGradient(0f, 0f, w.toFloat(), 0f,
            intArrayOf(Color.parseColor("#29B6E8"), barColor), null, Shader.TileMode.CLAMP)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRoundRect(trackRect, cornerRadius, cornerRadius, trackPaint)
        val barWidth = width * (animatedProgress / 100f)
        if (barWidth > cornerRadius * 2) {
            barRect.set(0f, 0f, barWidth, height.toFloat())
            canvas.drawRoundRect(barRect, cornerRadius, cornerRadius, barPaint)
        }
    }

    fun setProgress(target: Float, animate: Boolean = true) {
        if (!animate) { animatedProgress = target.coerceIn(0f, 100f); invalidate(); return }
        ValueAnimator.ofFloat(animatedProgress, target.coerceIn(0f, 100f)).apply {
            duration = 800; interpolator = DecelerateInterpolator(1.5f)
            addUpdateListener { animatedProgress = it.animatedValue as Float; invalidate() }
            start()
        }
    }
}

class CircularProgressMiniView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var cpColor: Int = Color.parseColor("#22C55E")
    private var cpTrack: Int = Color.parseColor("#DCFCE7")
    private var cpProgress: Float = 0f
    private var cpStrokeWidth: Float = 20f
    private var animatedProgress = 0f

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER; color = Color.parseColor("#15204F")
    }
    private val oval = RectF()

    init {
        attrs?.let {
            val ta = context.obtainStyledAttributes(it, R.styleable.CircularProgressMiniView)
            cpColor = ta.getColor(R.styleable.CircularProgressMiniView_cpColor, cpColor)
            cpTrack = ta.getColor(R.styleable.CircularProgressMiniView_cpTrack, cpTrack)
            cpProgress = ta.getFloat(R.styleable.CircularProgressMiniView_cpProgress, 0f)
            cpStrokeWidth = ta.getDimension(R.styleable.CircularProgressMiniView_cpStrokeWidth, cpStrokeWidth)
            ta.recycle()
        }
        trackPaint.color = cpTrack; progressPaint.color = cpColor
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val s = cpStrokeWidth; val d = min(w, h).toFloat() - s * 2
        val l = (w - d) / 2f; val t = (h - d) / 2f
        oval.set(l, t, l + d, t + d)
        trackPaint.strokeWidth = s; progressPaint.strokeWidth = s
        textPaint.textSize = d * 0.28f
        progressPaint.shader = SweepGradient(w / 2f, h / 2f,
            intArrayOf(Color.parseColor("#86EFAC"), cpColor, cpColor),
            floatArrayOf(0f, 0.5f, 1f))
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawArc(oval, -90f, 360f, false, trackPaint)
        val sweep = animatedProgress / 100f * 360f
        if (sweep > 0f) canvas.drawArc(oval, -90f, sweep, false, progressPaint)
        val cx = width / 2f; val cy = height / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText("${animatedProgress.toInt()}%", cx, cy, textPaint)
    }

    fun setProgress(target: Float, animate: Boolean = true) {
        if (!animate) { animatedProgress = target.coerceIn(0f, 100f); invalidate(); return }
        ValueAnimator.ofFloat(animatedProgress, target.coerceIn(0f, 100f)).apply {
            duration = 900; interpolator = DecelerateInterpolator(1.5f)
            addUpdateListener { animatedProgress = it.animatedValue as Float; invalidate() }
            start()
        }
    }
}
