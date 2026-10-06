package com.nextyear.app

import android.content.Context
import android.graphics.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.min

private const val LAVENDER = 0xFF9C95E6.toInt()

/** 4x4 "year of growth": one doodle per day so far, dots for days to come. */
object YearRenderer {
    fun render(ctx: Context, wDp: Float, hDp: Float): Bitmap {
        val pn = WidgetUtil.panel(ctx, wDp, hDp, 0xFFDCDCDF.toInt())
        val c = pn.c; val w = pn.w; val h = pn.h; val u = pn.u
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val today = cal.get(Calendar.DAY_OF_YEAR)
        val days = cal.getActualMaximum(Calendar.DAY_OF_YEAR)

        val cols = 21
        val rows = ceil(days / cols.toFloat()).toInt()
        val cell = min(w * 0.917f / cols, h * 0.80f / rows)
        val gx = (w - cell * cols) / 2f
        val gy = h * 0.065f
        val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Pal.INK }
        val pens = Array(5) { Doodle.pen(Pal.moods[it], maxOf(1f, cell * 0.065f)) }
        val ink = Doodle.pen(Pal.INK, maxOf(1f, cell * 0.065f))

        for (day in 1..days) {
            val i = day - 1
            val cx = gx + (i % cols + 0.5f) * cell
            val cy = gy + (i / cols + 0.5f) * cell
            if (day <= today) {
                val m = WidgetUtil.mood(ctx, day)
                val pen = if (m in 0..4) pens[m] else ink
                val hsh = (day * 2654435761L % 1000).toInt()
                c.save(); c.rotate(((hsh % 25) - 12).toFloat(), cx, cy)
                Doodle.mini(c, (day * 5 + hsh) % 14, cx, cy, cell * 0.40f, pen)
                c.restore()
            } else {
                c.drawCircle(cx, cy, maxOf(0.7f * u, cell * 0.045f), dot)
            }
        }

        val ts = w * 0.035f
        val mono = Typeface.MONOSPACE
        val by = h * 0.935f
        val left = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Pal.INK; typeface = Typeface.create(mono, Typeface.BOLD); textSize = ts; textAlign = Paint.Align.LEFT
        }
        c.drawText("$year", w * 0.073f, by, left)

        val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = LAVENDER; typeface = mono; textSize = ts; textAlign = Paint.Align.RIGHT
        }
        val num = Paint(left).apply { textAlign = Paint.Align.RIGHT }
        val xr = w * 0.93f
        val tail = " days of growth"
        c.drawText(tail, xr, by, label)
        c.drawText("$today", xr - label.measureText(tail), by, num)
        return pn.bmp
    }
}

/** One card of the day stack: #day-of-year, flower with that day's mood, weekday and date. */
object DayRenderer {
    fun render(ctx: Context, wDp: Float, hDp: Float, cal: Calendar = Calendar.getInstance()): Bitmap {
        val pn = WidgetUtil.panel(ctx, wDp, hDp, 0)
        val c = pn.c; val w = pn.w; val h = pn.h
        val now = cal.time
        val doy = cal.get(Calendar.DAY_OF_YEAR)
        val sameYear = cal.get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.YEAR)
        val m = if (sameYear) WidgetUtil.mood(ctx, doy) else -1

        val side = min(w, h)
        val mono = Typeface.MONOSPACE
        val lav = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = LAVENDER; typeface = mono; textSize = side * 0.072f; textAlign = Paint.Align.RIGHT
        }
        c.drawText("#$doy", w * 0.885f, h * 0.175f, lav)
        c.drawText(SimpleDateFormat("MM.dd", Locale.US).format(now), w * 0.885f, h * 0.885f, lav)

        val day = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Pal.INK; typeface = Typeface.create("casual", Typeface.NORMAL); textSize = side * 0.09f
            isFakeBoldText = true; textAlign = Paint.Align.LEFT
        }
        val name = SimpleDateFormat("EEEE", Locale.getDefault()).format(now)
        val maxW = w * 0.45f
        val mw = day.measureText(name)
        if (mw > maxW) day.textSize = day.textSize * maxW / mw
        c.drawText(name, w * 0.095f, h * 0.885f, day)

        val col = if (m in 0..4) Pal.moods[m] else Pal.INK
        val s = side * 0.125f
        Doodle.sunflower(c, w * 0.49f, h * 0.40f, s, Doodle.pen(col, side * 0.026f), if (m in 0..4) m else 2)
        return pn.bmp
    }
}

/** 1x1 shortcut tile: opens the camera. */
object CameraTileRenderer {
    fun render(ctx: Context, wDp: Float, hDp: Float): Bitmap {
        val pn = WidgetUtil.panel(ctx, wDp, hDp, 0xFFEDEDF0.toInt())
        val side = min(pn.w, pn.h)
        val s = side * 0.15f
        Doodle.bigFlower(pn.c, pn.w / 2f, pn.h / 2f - 0.625f * s, s, Doodle.pen(Pal.INK, side * 0.035f), 0)
        return pn.bmp
    }
}
