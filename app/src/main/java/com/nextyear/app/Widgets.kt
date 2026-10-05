package com.nextyear.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.RemoteViews

abstract class BitmapWidget : AppWidgetProvider() {
    abstract val defW: Float
    abstract val defH: Float
    abstract fun render(ctx: Context, w: Float, h: Float): Bitmap
    open fun click(ctx: Context): Intent = Intent(ctx, MainActivity::class.java)

    private fun draw(ctx: Context, mgr: AppWidgetManager, id: Int) {
        val (w, h) = WidgetUtil.dims(ctx, mgr, id, defW, defH)
        val rv = RemoteViews(ctx.packageName, R.layout.widget_clock)
        rv.setImageViewBitmap(R.id.clock_img, render(ctx, w, h))
        val pi = PendingIntent.getActivity(
            ctx, id * 31 + javaClass.simpleName.hashCode(), click(ctx),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        rv.setOnClickPendingIntent(R.id.clock_img, pi)
        mgr.updateAppWidget(id, rv)
    }

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { draw(ctx, mgr, it) }
    }

    override fun onAppWidgetOptionsChanged(ctx: Context, mgr: AppWidgetManager, id: Int, o: Bundle) {
        draw(ctx, mgr, id)
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        when (intent.action) {
            Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                val mgr = AppWidgetManager.getInstance(ctx)
                mgr.getAppWidgetIds(ComponentName(ctx, javaClass)).forEach { draw(ctx, mgr, it) }
            }
        }
    }
}

class YearWidget : BitmapWidget() {
    override val defW = 300f
    override val defH = 300f
    override fun render(ctx: Context, w: Float, h: Float) = YearRenderer.render(ctx, w, h)
}

class DayWidget : BitmapWidget() {
    override val defW = 146f
    override val defH = 146f
    override fun render(ctx: Context, w: Float, h: Float) = DayRenderer.render(ctx, w, h)
}

class CameraWidget : BitmapWidget() {
    override val defW = 68f
    override val defH = 68f
    override fun render(ctx: Context, w: Float, h: Float) = CameraTileRenderer.render(ctx, w, h)
    override fun click(ctx: Context): Intent =
        Intent(ctx, CameraActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
