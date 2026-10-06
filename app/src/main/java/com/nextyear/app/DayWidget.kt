package com.nextyear.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import java.util.Calendar

/**
 * Day widget: a clean 28dp card. Tap the left edge for an earlier day (yesterday, the day before...),
 * the right edge to come back toward today, the middle to capture (today) or open that day.
 */
class DayWidget : AppWidgetProvider() {

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { draw(ctx, mgr, it) }
    }

    override fun onAppWidgetOptionsChanged(ctx: Context, mgr: AppWidgetManager, id: Int, o: Bundle) {
        draw(ctx, mgr, id)
    }

    override fun onDeleted(ctx: Context, ids: IntArray) {
        val e = WidgetUtil.prefs(ctx).edit()
        ids.forEach { e.remove("dayoff$it") }
        e.apply()
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        val mgr = AppWidgetManager.getInstance(ctx)
        when (intent.action) {
            ACTION_PREV, ACTION_NEXT -> {
                val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                if (id == AppWidgetManager.INVALID_APPWIDGET_ID) return
                val cur = offset(ctx, id)
                val next = if (intent.action == ACTION_PREV) minOf(cur + 1, MAX_BACK) else maxOf(cur - 1, 0)
                if (next != cur) {
                    WidgetUtil.prefs(ctx).edit().putInt("dayoff$id", next).apply()
                    Haptics.init(ctx); Haptics.tick()
                    draw(ctx, mgr, id)
                }
            }
            Intent.ACTION_DATE_CHANGED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                val ids = mgr.getAppWidgetIds(ComponentName(ctx, DayWidget::class.java))
                val e = WidgetUtil.prefs(ctx).edit()
                ids.forEach { e.putInt("dayoff$it", 0) }   // a new day starts back on today
                e.apply()
                ids.forEach { draw(ctx, mgr, it) }
            }
        }
    }

    companion object {
        const val ACTION_PREV = "com.nextyear.app.DAY_PREV"
        const val ACTION_NEXT = "com.nextyear.app.DAY_NEXT"
        const val MAX_BACK = 30

        fun offset(ctx: Context, id: Int) = WidgetUtil.prefs(ctx).getInt("dayoff$id", 0)

        private fun step(ctx: Context, id: Int, action: String, code: Int) = PendingIntent.getBroadcast(
            ctx, id * 10 + code,
            Intent(ctx, DayWidget::class.java).setAction(action).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        fun draw(ctx: Context, mgr: AppWidgetManager, id: Int) {
            val (w, h) = WidgetUtil.dims(ctx, mgr, id, 146f, 146f)
            val side = minOf(w, h)
            val off = offset(ctx, id)
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -off) }

            val rv = RemoteViews(ctx.packageName, R.layout.widget_day)
            rv.setImageViewBitmap(R.id.clock_img, DayRenderer.render(ctx, side, side, cal, (off < MAX_BACK), (off > 0)))
            rv.setOnClickPendingIntent(R.id.zone_prev, step(ctx, id, ACTION_PREV, 1))
            rv.setOnClickPendingIntent(R.id.zone_next, step(ctx, id, ACTION_NEXT, 2))
            val open = PendingIntent.getActivity(
                ctx, id * 10 + 3,
                Intent(ctx, MainActivity::class.java).setAction("com.nextyear.app.OPEN_DAY").putExtra("daysAgo", off),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            rv.setOnClickPendingIntent(R.id.zone_open, open)
            mgr.updateAppWidget(id, rv)
        }
    }
}
