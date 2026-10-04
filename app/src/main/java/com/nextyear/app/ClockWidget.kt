package com.nextyear.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViews

class ClockWidget : AppWidgetProvider() {

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { draw(ctx, mgr, it) }
        schedule(ctx)
    }

    override fun onAppWidgetOptionsChanged(ctx: Context, mgr: AppWidgetManager, id: Int, o: Bundle) {
        draw(ctx, mgr, id)
    }

    override fun onEnabled(ctx: Context) = schedule(ctx)

    override fun onDisabled(ctx: Context) {
        (ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(tickIntent(ctx))
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        when (intent.action) {
            ACTION_TICK, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED, Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                updateAll(ctx); schedule(ctx)
            }
        }
    }

    companion object {
        const val ACTION_TICK = "com.nextyear.app.TICK"

        fun updateAll(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            mgr.getAppWidgetIds(ComponentName(ctx, ClockWidget::class.java)).forEach { draw(ctx, mgr, it) }
        }

        private fun draw(ctx: Context, mgr: AppWidgetManager, id: Int) {
            val o = mgr.getAppWidgetOptions(id)
            val land = ctx.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            var w = (if (land) o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH) else o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)).toFloat()
            var h = (if (land) o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) else o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)).toFloat()
            if (w <= 0f) w = 220f
            if (h <= 0f) h = 146f

            val rv = RemoteViews(ctx.packageName, R.layout.widget_clock)
            rv.setImageViewBitmap(R.id.clock_img, ClockRenderer.render(ctx, w, h))
            val open = PendingIntent.getActivity(
                ctx, 0, Intent(ctx, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            rv.setOnClickPendingIntent(R.id.clock_img, open)
            mgr.updateAppWidget(id, rv)
        }

        private fun tickIntent(ctx: Context) = PendingIntent.getBroadcast(
            ctx, 1, Intent(ctx, ClockWidget::class.java).setAction(ACTION_TICK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        /** Wakes the widget at the top of every minute. */
        fun schedule(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            if (mgr.getAppWidgetIds(ComponentName(ctx, ClockWidget::class.java)).isEmpty()) return
            val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val at = (System.currentTimeMillis() / 60000 + 1) * 60000
            val pi = tickIntent(ctx)
            try {
                if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms())
                    am.setAndAllowWhileIdle(AlarmManager.RTC, at, pi)
                else am.setExactAndAllowWhileIdle(AlarmManager.RTC, at, pi)
            } catch (e: SecurityException) {
                am.set(AlarmManager.RTC, at, pi)
            }
        }
    }
}
