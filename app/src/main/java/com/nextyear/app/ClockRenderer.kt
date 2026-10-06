package com.nextyear.app

import android.content.Context
import android.graphics.*
import android.text.format.DateFormat
import java.util.Calendar
import java.util.Random
import kotlin.math.sqrt

/** Draws the time as digits packed with tiny hand-drawn plants, on a dotted paper panel. */
object ClockRenderer {
    private val MASK = arrayOf("abcdef", "bc", "abged", "abgcd", "fgbc", "afgcd", "afgedc", "abc", "abcdefg", "abcdfg")
    private val WEIGHTS = floatArrayOf(0.06f, 0.08f, 0.68f, 0.12f, 0.06f) // crimson, orange, indigo, blue, green

    fun render(ctx: Context, wDp: Float, hDp: Float): Bitmap {
        val d = ctx.resources.displayMetrics.density
        var u = d
        var wp = wDp * d
        var hp = hDp * d
        val maxPx = 600_000f
        if (wp * hp > maxPx) {
            val k = sqrt(maxPx / (wp * hp)); u = d * k; wp *= k; hp *= k
        }
        val bmp = Bitmap.createBitmap(wp.toInt().coerceAtLeast(2), hp.toInt().coerceAtLeast(2), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val w = bmp.width.toFloat(); val h = bmp.height.toFloat()


        // dotted paper grid
        val dotP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x551A00C8 }
        val step = 9 * u
        var yy = step / 2
        while (yy < h) { var xx = step / 2; while (xx < w) { c.drawCircle(xx, yy, 0.7f * u, dotP); xx += step }; yy += step }

        // time
        val cal = Calendar.getInstance()
        var hr = cal.get(Calendar.HOUR_OF_DAY)
        val is24 = DateFormat.is24HourFormat(ctx)
        var blankLead = false
        if (!is24) { hr %= 12; if (hr == 0) hr = 12; blankLead = hr < 10 }
        val mi = cal.get(Calendar.MINUTE)
        val digits = intArrayOf(hr / 10, hr % 10, mi / 10, mi % 10)

        val pens = Array(5) { Doodle.pen(Pal.moods[it], 1.25f * u) }

        val pad = 14 * u
        val inner = w - 2 * pad
        val dw = inner / 5.1f
        val g = dw * 0.15f
        val cw = dw * 0.5f
        val dh = h - 2 * pad
        val t = minOf(dw * 0.36f, dh * 0.22f)
        // center the whole time: 3 digits when the 12h hour has no leading zero, otherwise 4
        val count = if (blankLead) 3 else 4
        val total = count * dw + cw + count * g
        var x = (w - total) / 2f
        val y = pad

        for (i in 0..1) {
            if (!(i == 0 && blankLead)) {
                fill(c, glyph(digits[i], x, y, dw, dh, t), 10 + digits[i] * 31 + i, u, pens)
                x += dw + g
            }
        }
        val sq = t * 0.95f
        val cx = x + (cw - sq) / 2
        fill(c, listOf(RectF(cx, y + dh * 0.28f, cx + sq, y + dh * 0.28f + sq)), 501, u, pens)
        fill(c, listOf(RectF(cx, y + dh * 0.72f - sq, cx + sq, y + dh * 0.72f)), 502, u, pens)
        x += cw + g
        for (i in 2..3) {
            fill(c, glyph(digits[i], x, y, dw, dh, t), 100 + digits[i] * 37 + i, u, pens)
            x += dw + g
        }
        return bmp
    }

    private fun glyph(n: Int, x: Float, y: Float, dw: Float, dh: Float, t: Float): List<RectF> {
        val out = ArrayList<RectF>()
        val m = y + dh / 2
        for (s in MASK[n]) out.add(when (s) {
            'a' -> RectF(x, y, x + dw, y + t)
            'b' -> RectF(x + dw - t, y, x + dw, m + t / 2)
            'c' -> RectF(x + dw - t, m - t / 2, x + dw, y + dh)
            'd' -> RectF(x, y + dh - t, x + dw, y + dh)
            'e' -> RectF(x, m - t / 2, x + t, y + dh)
            'f' -> RectF(x, y, x + t, m + t / 2)
            else -> RectF(x, m - t / 2, x + dw, m + t / 2)
        })
        return out
    }

    private fun fill(c: Canvas, rects: List<RectF>, seed: Int, u: Float, pens: Array<Paint>) {
        val rnd = Random(seed.toLong())
        val cs = 7.2f * u
        val l = rects.minOf { it.left }; val tp = rects.minOf { it.top }
        val rt = rects.maxOf { it.right }; val bt = rects.maxOf { it.bottom }
        var row = 0
        var gy = tp + cs / 2
        while (gy < bt + cs / 2) {
            var gx = l + cs / 2 + if (row % 2 == 1) cs / 2 else 0f
            while (gx < rt + cs / 2) {
                val px = gx + (rnd.nextFloat() - 0.5f) * 0.45f * cs
                val py = gy + (rnd.nextFloat() - 0.5f) * 0.45f * cs
                val type = rnd.nextInt(6)
                val size = u * (4.2f + rnd.nextFloat() * 1.6f)
                val rot = (rnd.nextFloat() - 0.5f) * 50f
                var rr = rnd.nextFloat(); var ci = 2
                for (i in WEIGHTS.indices) { rr -= WEIGHTS[i]; if (rr <= 0) { ci = i; break } }
                if (rects.any { it.contains(px, py) }) {
                    c.save(); c.rotate(rot, px, py)
                    Doodle.mini(c, type, px, py, size, pens[ci])
                    c.restore()
                }
                gx += cs
            }
            gy += cs * 0.9f; row++
        }
    }
}
