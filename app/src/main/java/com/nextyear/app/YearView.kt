package com.nextyear.app

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import java.util.Calendar

class YearView(ctx: Context) : View(ctx) {
    private val d = resources.displayMetrics.density
    val entries = HashMap<Int, Int>() // day-of-year -> mood index
    var currentMood = 3
    var onEntry: ((Int, Int?) -> Unit)? = null

    private val cal = Calendar.getInstance()
    private val year = cal.get(Calendar.YEAR)
    val today = cal.get(Calendar.DAY_OF_YEAR)
    private val days = if (cal.getActualMaximum(Calendar.DAY_OF_YEAR) > 365) 366 else 365
    private val cols = 19
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Pal.INK }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val txt = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD); textAlign = Paint.Align.CENTER; color = Color.WHITE
    }
    private var gx = 0f; private var gy = 0f; private var cell = 0f

    private fun geom() {
        val rows = (days + cols - 1) / cols
        cell = minOf(width * 0.94f / cols, (height - 220 * d) / rows)
        gx = (width - cell * cols) / 2f
        gy = 110 * d
    }

    override fun onDraw(c: Canvas) {
        c.drawColor(Pal.BG)
        geom()
        fill.color = Pal.INK
        val pill = RectF(width / 2f - 36 * d, 48 * d, width / 2f + 36 * d, 84 * d)
        c.drawRoundRect(pill, 18 * d, 18 * d, fill)
        txt.textSize = 14 * d
        c.drawText("$year", width / 2f, 72 * d, txt)

        for (day in 1..days) {
            val i = day - 1
            val cx = gx + (i % cols + 0.5f) * cell
            val cy = gy + (i / cols + 0.5f) * cell
            val m = entries[day]
            if (m != null) {
                val p = Doodle.pen(Pal.moods[m], maxOf(1.3f * d, cell * 0.075f))
                Doodle.mini(c, (day * 7 + 3) % 6, cx, cy, cell * 0.36f, p)
            } else {
                c.drawCircle(cx, cy, 1.1f * d, dot)
            }
            if (day == today) {
                val ring = Doodle.pen(Pal.INK, 1.4f * d); ring.alpha = 120
                c.drawCircle(cx, cy, cell * 0.46f, ring)
            }
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action == MotionEvent.ACTION_DOWN) return true
        if (e.action == MotionEvent.ACTION_UP) {
            geom()
            val col = ((e.x - gx) / cell).toInt(); val row = ((e.y - gy) / cell).toInt()
            if (col in 0 until cols && row >= 0) {
                val day = row * cols + col + 1
                if (day in 1..days) {
                    if (entries[day] == currentMood) { entries.remove(day); Haptics.soft(); onEntry?.invoke(day, null) }
                    else { entries[day] = currentMood; Haptics.click(); onEntry?.invoke(day, currentMood) }
                    invalidate()
                }
            }
        }
        return true
    }
}
