package com.nextyear.app

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View

class TabPill(ctx: Context) : View(ctx) {
    private val d = resources.displayMetrics.density
    var index = 0
        set(v) { field = v; invalidate() }
    var onSelect: ((Int) -> Unit)? = null
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val r = RectF()

    override fun onMeasure(w: Int, h: Int) = setMeasuredDimension((124 * d).toInt(), (56 * d).toInt())

    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        fill.setShadowLayer(8 * d, 0f, 2 * d, 0x30000000)
        fill.color = 0xFFC4C0DC.toInt()
        r.set(2 * d, 2 * d, w - 2 * d, h - 4 * d)
        c.drawRoundRect(r, 20 * d, 20 * d, fill)
        fill.clearShadowLayer()
        for (i in 0..1) {
            val cx = w * (0.28f + 0.44f * i); val cy = (h - 2 * d) / 2
            val sel = i == index
            if (sel) {
                fill.color = Pal.INK
                c.drawRoundRect(cx - 24 * d, cy - 21 * d, cx + 24 * d, cy + 21 * d, 15 * d, 15 * d, fill)
            }
            val p = Doodle.pen(if (sel) Color.WHITE else Pal.INK, 2.2f * d)
            if (i == 0) {
                Doodle.mini(c, 2, cx - 6 * d, cy + 2 * d, 9 * d, p)
                Doodle.mini(c, 3, cx + 8 * d, cy - 3 * d, 8 * d, p)
            } else Doodle.mini(c, 0, cx, cy, 13 * d, p)
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action == MotionEvent.ACTION_DOWN) return true
        if (e.action == MotionEvent.ACTION_UP) {
            val i = if (e.x < width / 2f) 0 else 1
            if (i != index) { index = i; Haptics.click(); onSelect?.invoke(i) }
        }
        return true
    }
}
