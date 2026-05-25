package com.unimarksa.gmv.views

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.TextView
import com.unimarksa.gmv.R

class MetricPillView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val tvLabel: TextView
    private val tvValue: TextView

    init {
        orientation = VERTICAL
        gravity = android.view.Gravity.CENTER

        inflate(context, R.layout.view_metric_pill, this)
        tvLabel = findViewById(R.id.pill_label)
        tvValue = findViewById(R.id.pill_value)

        attrs?.let {
            val ta = context.obtainStyledAttributes(it, R.styleable.MetricPillView)
            val label = ta.getString(R.styleable.MetricPillView_pillLabel) ?: ""
            val value = ta.getString(R.styleable.MetricPillView_pillValue) ?: "0"
            val color = ta.getColor(R.styleable.MetricPillView_pillColor, Color.parseColor("#4361EE"))
            ta.recycle()

            tvLabel.text = label
            tvValue.text = value
            tvValue.setTextColor(color)
        }
    }

    fun setLabel(label: String) { tvLabel.text = label }
    fun setValue(value: String) { tvValue.text = value }
    fun setValueColor(color: Int) { tvValue.setTextColor(color) }
}
