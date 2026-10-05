package com.nextyear.app

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.sqrt

/** A rounded (28dp), borderless bitmap panel the widgets draw into. */
class Panel(val bmp: Bitmap, val c: Canvas, val u: Float, val w: Float, val h: Float)

object WidgetUtil {
    fun prefs(ctx: Context) = ctx.getSharedPreferences("nextyear", Context.MODE_PRIVATE)
    fun mood(ctx: Context, day: Int) = prefs(ctx).getInt("d$day", -1)

    fun dims(ctx: Context, mgr: AppWidgetManager, id: Int, defW: Float, defH: Float): Pair<Float, Float> {
        val o = mgr.getAppWidgetOptions(id)
        val land = ctx.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        var w = (if (land) o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH) else o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)).toFloat()
        var h = (if (land) o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) else o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)).toFloat()
        if (w <= 0f) w = defW
        if (h <= 0f) h = defH
        return Pair(w, h)
    }

    fun panel(ctx: Context, wDp: Float, hDp: Float, color: Int): Panel {
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
        val r = 28 * u
        c.clipPath(Path().apply { addRoundRect(RectF(0f, 0f, bmp.width.toFloat(), bmp.height.toFloat()), r, r, Path.Direction.CW) })
        c.drawColor(color)
        return Panel(bmp, c, u, bmp.width.toFloat(), bmp.height.toFloat())
    }

    fun refreshAll(ctx: Context) {
        val mgr = AppWidgetManager.getInstance(ctx)
        val classes = listOf<Class<*>>(ClockWidget::class.java, YearWidget::class.java, DayWidget::class.java, CameraWidget::class.java)
        for (cls in classes) {
            val ids = mgr.getAppWidgetIds(ComponentName(ctx, cls))
            if (ids.isNotEmpty()) {
                ctx.sendBroadcast(
                    Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).setClass(ctx, cls)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                )
            }
        }
    }
}
