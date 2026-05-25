package com.unimarksa.gmv.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import com.unimarksa.gmv.R
import kotlin.math.min

class ArcProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var arcColor: Int = Color.parseColor("#4361EE")
    private var arcTrackColor: Int = Color.parseColor("#EEF1FF")
    private var arcProgress: Float = 0f
    private var arcStrokeWidth: Float = 36f

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
    }
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
    }
    private val oval = RectF()
    private var animatedProgress = 0f

    init {
        attrs?.let {
            val ta = context.obtainStyledAttributes(it, R.styleable.ArcProgressView)
            arcColor = ta.getColor(R.styleable.ArcProgressView_arcColor, arcColor)
            arcTrackColor = ta.getColor(R.styleable.ArcProgressView_arcTrackColor, arcTrackColor)
            arcProgress = ta.getFloat(R.styleable.ArcProgressView_arcProgress, 0f)
            arcStrokeWidth = ta.getDimension(R.styleable.ArcProgressView_arcStrokeWidth, arcStrokeWidth)
            ta.recycle()
        }
        trackPaint.color = arcTrackColor
        progressPaint.color = arcColor
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val stroke = arcStrokeWidth
        val diameter = min(w, h * 2).toFloat() - stroke * 2
        val left = (w - diameter) / 2f
        val top = stroke
        oval.set(left, top, left + diameter, top + diameter)

        progressPaint.shader = LinearGradient(
            oval.left, oval.top, oval.right, oval.bottom,
            intArrayOf(Color.parseColor("#6C8FF5"), arcColor),
            null, Shader.TileMode.CLAMP
        )
        trackPaint.strokeWidth = stroke
        progressPaint.strokeWidth = stroke
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawArc(oval, 180f, 180f, false, trackPaint)
        val sweep = animatedProgress / 100f * 180f
        if (sweep > 0f) canvas.drawArc(oval, 180f, sweep, false, progressPaint)
    }

    fun setProgress(target: Float, animate: Boolean = true) {
        if (!animate) {
            animatedProgress = target.coerceIn(0f, 100f); invalidate(); return
        }
        ValueAnimator.ofFloat(animatedProgress, target.coerceIn(0f, 100f)).apply {
            duration = 900; interpolator = DecelerateInterpolator(1.5f)
            addUpdateListener { animatedProgress = it.animatedValue as Float; invalidate() }
            start()
        }
    }
}
