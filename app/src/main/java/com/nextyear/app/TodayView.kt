package com.nextyear.app

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View

class TodayView(ctx: Context) : View(ctx) {
    private val d = resources.displayMetrics.density
    var moodColor = Pal.INK
        set(v) { field = v; invalidate() }
    var photo: Bitmap? = null
        set(v) { field = v; invalidate() }
    var caption = ""
        set(v) { field = v; invalidate() }
    var onCard: (() -> Unit)? = null
    var onCaption: (() -> Unit)? = null
    var onTile: (() -> Unit)? = null
    var onCardLong: (() -> Unit)? = null
    private var downT = 0L

    private val mono = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = mono; color = Pal.INK }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tileRect = RectF()
    private val cardRect = RectF()
    private val capRect = RectF()
    private val selected = 3

    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        c.drawColor(Pal.BG)
        val top = 52 * d

        // label + icon strip
        text.textSize = 12 * d; text.textAlign = Paint.Align.CENTER
        val x = { i: Int -> 38 * d + i * 58 * d }
        c.drawText("Today", x(selected), top, text)
        val types = intArrayOf(3, 2, 0, 1, 5)
        for (i in 0 until 5) {
            val cx = x(i); val cy = top + 40 * d
            if (i == selected) {
                tileRect.set(cx - 27 * d, cy - 27 * d, cx + 27 * d, cy + 27 * d)
                fill.color = moodColor; fill.alpha = 55
                c.drawRoundRect(tileRect, 14 * d, 14 * d, fill)
                Doodle.mini(c, types[i], cx, cy, 14 * d, Doodle.pen(moodColor, 2.6f * d))
            } else {
                val p = Doodle.pen(Pal.INK, 2.2f * d); p.alpha = if (i == 0) 90 else 255
                Doodle.mini(c, types[i], cx, cy, 13 * d, p)
            }
        }

        // photo stack
        val cx = w / 2; val cy = h * 0.355f
        val cw = w * 0.42f; val ch = cw * 1.38f
        cardRect.set(cx - cw / 2, cy - ch / 2, cx + cw / 2, cy + ch / 2)
        fill.shader = null
        for ((rot, col) in listOf(-5f to 0xFFB08A5A.toInt(), 4f to 0xFFC9A877.toInt())) {
            c.save(); c.rotate(rot, cx, cy)
            fill.color = col; fill.alpha = 255
            c.drawRoundRect(cardRect, 8 * d, 8 * d, fill)
            c.restore()
        }
        c.save(); c.rotate(2f, cx, cy)
        c.clipPath(Path().apply { addRoundRect(cardRect, 8 * d, 8 * d, Path.Direction.CW) })
        val b = photo
        if (b != null) {
            val s = maxOf(cw / b.width, ch / b.height)
            val sw = cw / s; val sh = ch / s
            val sx = (b.width - sw) / 2; val sy = (b.height - sh) / 2
            c.drawBitmap(b, Rect(sx.toInt(), sy.toInt(), (sx + sw).toInt(), (sy + sh).toInt()), cardRect, null)
        } else drawEmpty(c)
        c.restore()

        // caption
        text.textAlign = Paint.Align.LEFT; text.textSize = 17 * d
        var y = cardRect.bottom + 0.1f * h
        val x0 = w * 0.1f
        val blank = caption.isBlank()
        text.color = if (blank) 0xFF9C95E6.toInt() else Pal.INK
        val lines = (if (blank) "Start writing" else caption).split("\n")
        capRect.set(x0, y - 22 * d, w - x0, y + lines.size * 24 * d)
        for (ln in lines) { c.drawText(ln, x0, y, text); y += 24 * d }
        text.color = Pal.INK
    }

    private fun drawEmpty(c: Canvas) {
        fill.shader = null; fill.color = 0xFFC9C6E8.toInt(); fill.alpha = 255
        c.drawRect(cardRect, fill)
        val p = Doodle.pen(Pal.INK, 2.4f * d)
        val cx = cardRect.centerX(); val cy = cardRect.centerY()
        val r = RectF(cx - 18 * d, cy - 12 * d, cx + 18 * d, cy + 14 * d)
        c.drawRoundRect(r, 6 * d, 6 * d, p)
        c.drawCircle(cx, cy + 1 * d, 7 * d, p)
        c.drawLine(cx - 7 * d, cy - 12 * d, cx - 4 * d, cy - 17 * d, p)
        c.drawLine(cx - 4 * d, cy - 17 * d, cx + 4 * d, cy - 17 * d, p)
        c.drawLine(cx + 4 * d, cy - 17 * d, cx + 7 * d, cy - 12 * d, p)
    }

    private fun drawLandscape(c: Canvas) {
        fill.shader = LinearGradient(0f, cardRect.top, 0f, cardRect.bottom, 0xFF3C7FD0.toInt(), 0xFFB9D6F2.toInt(), Shader.TileMode.CLAMP)
        c.drawRect(cardRect, fill)
        fill.shader = null
        fill.color = 0xFFC9792B.toInt()
        c.drawCircle(cardRect.right - 30 * d, cardRect.centerY(), 70 * d, fill)
        fill.color = 0xFF8A8F3A.toInt()
        val hill = Path().apply {
            moveTo(cardRect.left, cardRect.bottom - 90 * d)
            quadTo(cardRect.centerX(), cardRect.bottom - 150 * d, cardRect.right, cardRect.bottom - 80 * d)
            lineTo(cardRect.right, cardRect.bottom); lineTo(cardRect.left, cardRect.bottom); close()
        }
        c.drawPath(hill, fill)
        fill.color = 0xFFFFC21A.toInt()
        val sx = cardRect.left + cardRect.width() * 0.4f; val sy = cardRect.top + cardRect.height() * 0.3f
        c.save(); c.rotate(45f, sx, sy); c.drawRect(sx - 24 * d, sy - 24 * d, sx + 24 * d, sy + 24 * d, fill); c.restore()
        fill.color = 0xFF222222.toInt()
        c.drawRect(sx - 2 * d, sy + 34 * d, sx + 2 * d, cardRect.bottom - 60 * d, fill)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action == MotionEvent.ACTION_DOWN) { downT = e.eventTime; return true }
        if (e.action == MotionEvent.ACTION_UP) {
            when {
                tileRect.contains(e.x, e.y) -> { Haptics.soft(); onTile?.invoke() }
                capRect.contains(e.x, e.y) -> { Haptics.tick(); onCaption?.invoke() }
                cardRect.contains(e.x, e.y) -> {
                    if (e.eventTime - downT > 500) { Haptics.click(); onCardLong?.invoke() }
                    else { Haptics.soft(); onCard?.invoke() }
                }
            }
        }
        return true
    }
}
