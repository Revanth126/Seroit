package com.msu.mfalocker

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.min
import kotlin.math.sqrt

class PatternView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    interface PatternListener {
        fun onPatternComplete(dotSequence: List<Int>)
        fun onPatternProgress(dotSequence: List<Int>)
        fun onPatternTooShort()
    }

    var listener: PatternListener? = null

    // Dot positions (center x, center y) computed in onSizeChanged
    private val dotCenters = Array(9) { Pair(0f, 0f) }

    // Currently selected dot indices in draw order
    private val selectedDots = mutableListOf<Int>()

    // Current touch position (for drawing the in-progress line to finger)
    private var touchX = 0f
    private var touchY = 0f
    private var isTouching = false

    private val dotRadius get() = cellSize * 0.12f
    private val hitRadius get() = cellSize * 0.28f
    private var cellSize = 0f

    // Paints
    private val idleDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#AAAAAA")
        style = Paint.Style.FILL
    }

    private val selectedDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4A90E2")
        style = Paint.Style.FILL
    }

    private val selectedDotRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4A90E2")
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4A90E2")
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
    }

    private val activeLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#80B0D8")
        style = Paint.Style.STROKE
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val size = min(w, h).toFloat()
        cellSize = size / 3f
        val offsetX = (w - size) / 2f
        val offsetY = (h - size) / 2f
        for (i in 0..8) {
            val col = i % 3
            val row = i / 3
            dotCenters[i] = Pair(
                offsetX + col * cellSize + cellSize / 2f,
                offsetY + row * cellSize + cellSize / 2f
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw connecting lines between selected dots
        for (i in 0 until selectedDots.size - 1) {
            val (x1, y1) = dotCenters[selectedDots[i]]
            val (x2, y2) = dotCenters[selectedDots[i + 1]]
            canvas.drawLine(x1, y1, x2, y2, linePaint)
        }

        // Draw in-progress line from last selected dot to current touch
        if (isTouching && selectedDots.isNotEmpty()) {
            val (lx, ly) = dotCenters[selectedDots.last()]
            canvas.drawLine(lx, ly, touchX, touchY, activeLinePaint)
        }

        // Draw dots
        for (i in 0..8) {
            val (cx, cy) = dotCenters[i]
            if (selectedDots.contains(i)) {
                canvas.drawCircle(cx, cy, dotRadius * 1.4f, selectedDotPaint)
                canvas.drawCircle(cx, cy, dotRadius * 2.2f, selectedDotRingPaint)
            } else {
                canvas.drawCircle(cx, cy, dotRadius, idleDotPaint)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                isTouching = true
                touchX = event.x
                touchY = event.y
                hitTestDot(event.x, event.y)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isTouching = false
                onPatternFinished()
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun hitTestDot(x: Float, y: Float) {
        for (i in 0..8) {
            if (selectedDots.contains(i)) continue
            val (cx, cy) = dotCenters[i]
            val dx = x - cx
            val dy = y - cy
            val dist = sqrt(dx * dx + dy * dy)
            if (dist <= hitRadius) {
                selectedDots.add(i)
                listener?.onPatternProgress(selectedDots.toList())
                break
            }
        }
    }

    private fun onPatternFinished() {
        if (selectedDots.size < 4) {
            listener?.onPatternTooShort()
            reset()
        } else {
            listener?.onPatternComplete(selectedDots.toList())
        }
    }

    /** Clears the current drawn pattern and redraws the idle grid. */
    fun reset() {
        selectedDots.clear()
        isTouching = false
        invalidate()
    }
}
