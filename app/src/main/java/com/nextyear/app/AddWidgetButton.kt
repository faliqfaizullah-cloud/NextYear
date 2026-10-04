package com.nextyear.app

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View

/** Small doodle tile (clock + plus) that asks Android to pin the clock widget. */
class AddWidgetButton(ctx: Context) : View(ctx) {
    private val d = resources.displayMetrics.density
    var onTap: (() -> Unit)? = null
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val r = RectF()
    private var pressedNow = false

    override fun onMeasure(w: Int, h: Int) = setMeasuredDimension((52 * d).toInt(), (52 * d).toInt())

    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        fill.setShadowLayer(8 * d, 0f, 2 * d, 0x30000000)
        fill.color = if (pressedNow) Pal.INK else 0xFFC4C0DC.toInt()
        r.set(3 * d, 3 * d, w - 3 * d, h - 5 * d)
        c.drawRoundRect(r, 18 * d, 18 * d, fill)
        fill.clearShadowLayer()
        val p = Doodle.pen(if (pressedNow) Color.WHITE else Pal.INK, 2.2f * d)
        val cx = w / 2 - 3 * d; val cy = h / 2 - 1 * d
        c.drawCircle(cx, cy, 11 * d, p)
        c.drawLine(cx, cy, cx, cy - 7 * d, p)
        c.drawLine(cx, cy, cx + 5 * d, cy + 2 * d, p)
        val px = w / 2 + 10 * d; val py = h / 2 - 12 * d
        c.drawLine(px - 4 * d, py, px + 4 * d, py, p)
        c.drawLine(px, py - 4 * d, px, py + 4 * d, p)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.action) {
            MotionEvent.ACTION_DOWN -> { pressedNow = true; invalidate(); Haptics.tick() }
            MotionEvent.ACTION_UP -> { pressedNow = false; invalidate(); Haptics.click(); onTap?.invoke() }
            MotionEvent.ACTION_CANCEL -> { pressedNow = false; invalidate() }
        }
        return true
    }
}
