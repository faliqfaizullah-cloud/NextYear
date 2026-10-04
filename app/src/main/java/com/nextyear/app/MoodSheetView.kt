package com.nextyear.app

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import kotlin.math.roundToInt

class MoodSheetView(ctx: Context) : View(ctx) {
    private val d = resources.displayMetrics.density
    var pos = 3f
        set(v) { field = v; onColor?.invoke(currentColor()); invalidate() }
    var progress = 1f
        set(v) { field = v; invalidate() }
    var onMood: ((Int) -> Unit)? = null
    var onReset: (() -> Unit)? = null
    var onColor: ((Int) -> Unit)? = null

    private val mono = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = mono; textAlign = Paint.Align.CENTER; color = Pal.INK }
    private val rect = RectF()
    private val track = RectF()
    private val eval = ArgbEvaluator()
    private var mode = 0 // 1 sheet, 2 track, 3 reset
    private var downY = 0f
    private var lastIdx = -1
    private val sheetH get() = height * 0.62f

    fun currentColor(): Int {
        val i = pos.toInt().coerceIn(0, 3)
        return eval.evaluate((pos - i).coerceIn(0f, 1f), Pal.moods[i], Pal.moods[i + 1]) as Int
    }

    fun show() = animateProgress(1f)
    fun hide() = animateProgress(0f)
    val isOpen get() = progress > 0.5f

    private fun animateProgress(to: Float) {
        ValueAnimator.ofFloat(progress, to).apply {
            duration = 280
            addUpdateListener { progress = it.animatedValue as Float }
            start()
        }
    }

    private fun layoutRects() {
        val w = width.toFloat(); val h = height.toFloat()
        val t = h - (sheetH + 8 * d) * progress
        rect.set(8 * d, t, w - 8 * d, t + sheetH)
        track.set(rect.left + 20 * d, rect.bottom - 24 * d - 56 * d, rect.right - 20 * d, rect.bottom - 24 * d)
    }

    private fun dotX(i: Float): Float {
        val a = track.left + 36 * d; val b = track.right - 36 * d
        return a + (b - a) * i / 4f
    }

    override fun onDraw(c: Canvas) {
        if (progress <= 0.001f) return
        layoutRects()
        c.drawColor(Color.argb((90 * progress).toInt(), 0, 0, 0))
        fill.color = 0xFFD6D6D6.toInt()
        fill.setShadowLayer(18 * d, 0f, -2 * d, 0x33000000)
        c.drawRoundRect(rect, 34 * d, 34 * d, fill)
        fill.clearShadowLayer()
        // handle
        fill.color = 0xFFB5B5C0.toInt()
        c.drawRoundRect(rect.centerX() - 20 * d, rect.top + 8 * d, rect.centerX() + 20 * d, rect.top + 12 * d, 2 * d, 2 * d, fill)
        // title
        text.textSize = 13 * d; text.isFakeBoldText = true
        c.drawText("How are you", rect.centerX(), rect.top + 46 * d, text)
        c.drawText("feeling today?", rect.centerX(), rect.top + 64 * d, text)
        text.isFakeBoldText = false
        // flower
        val col = currentColor()
        val fallenN = Pal.fallen[pos.roundToInt().coerceIn(0, 4)]
        Doodle.bigFlower(c, rect.centerX() - 8 * d, rect.top + sheetH * 0.36f, rect.width() * 0.13f, Doodle.pen(col, 7 * d), fallenN)
        // reset
        text.textSize = 11 * d; text.alpha = 110
        c.drawText("Reset mood", rect.centerX(), track.top - 22 * d, text)
        text.alpha = 255
        // track
        fill.color = 0xFFC4C0DC.toInt()
        c.drawRoundRect(track, 28 * d, 28 * d, fill)
        for (i in 0..4) {
            fill.color = Pal.moods[i]
            c.drawCircle(dotX(i.toFloat()), track.centerY(), 6 * d, fill)
        }
        // knob
        val kx = dotX(pos); val ky = track.centerY()
        fill.setShadowLayer(10 * d, 0f, 3 * d, 0x40000000)
        fill.shader = null; fill.color = Color.WHITE
        c.drawCircle(kx, ky, 24 * d, fill)
        fill.clearShadowLayer()
        fill.shader = RadialGradient(kx, ky, 20 * d, intArrayOf(col, Color.argb(120, Color.red(col), Color.green(col), Color.blue(col)), Color.WHITE), floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP)
        c.drawCircle(kx, ky, 20 * d, fill)
        fill.shader = null
    }

    private fun posFromX(x: Float) {
        val p = ((x - dotX(0f)) / (dotX(4f) - dotX(0f)) * 4f).coerceIn(0f, 4f)
        val idx = p.roundToInt()
        if (idx != lastIdx) { lastIdx = idx; Haptics.tick() }
        pos = p
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (progress < 0.02f && e.action == MotionEvent.ACTION_DOWN) return false
        layoutRects()
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                downY = e.y
                mode = when {
                    e.y < rect.top -> { if (progress > 0.9f) { Haptics.soft(); hide() }; 0 }
                    e.y < rect.top + 60 * d -> 1
                    e.y > track.top - 14 * d && e.y < track.bottom + 14 * d -> { lastIdx = -1; posFromX(e.x); 2 }
                    e.y > track.top - 44 * d && e.y < track.top - 4 * d -> 3
                    else -> 0
                }
            }
            MotionEvent.ACTION_MOVE -> when (mode) {
                1 -> progress = (1f - (e.y - downY) / sheetH).coerceIn(0f, 1f)
                2 -> posFromX(e.x)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                when (mode) {
                    1 -> { Haptics.soft(); if (progress < 0.65f) hide() else show() }
                    2 -> {
                        val target = pos.roundToInt().toFloat()
                        ValueAnimator.ofFloat(pos, target).apply {
                            duration = 260; interpolator = OvershootInterpolator(2f)
                            addUpdateListener { pos = it.animatedValue as Float }
                            start()
                        }
                        Haptics.click()
                        onMood?.invoke(target.toInt())
                    }
                    3 -> if (e.y > track.top - 44 * d && e.y < track.top - 4 * d) { Haptics.heavy(); onReset?.invoke() }
                }
                mode = 0
            }
        }
        return true
    }
}
