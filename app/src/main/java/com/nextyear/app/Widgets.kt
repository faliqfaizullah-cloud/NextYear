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
    open val bgRes: Int get() = R.drawable.widget_bg
    /** Content is drawn at its own aspect and centered, so it can never be stretched. */
    open fun contentSize(w: Float, h: Float): Pair<Float, Float> { val s = minOf(w, h); return Pair(s, s) }
    abstract fun render(ctx: Context, w: Float, h: Float): Bitmap
    open fun click(ctx: Context): Intent = Intent(ctx, MainActivity::class.java)

    private fun draw(ctx: Context, mgr: AppWidgetManager, id: Int) {
        val (w, h) = WidgetUtil.dims(ctx, mgr, id, defW, defH)
        val rv = RemoteViews(ctx.packageName, R.layout.widget_clock)
        val (cw, ch) = contentSize(w, h)
        rv.setInt(R.id.widget_root, "setBackgroundResource", bgRes)
        rv.setImageViewBitmap(R.id.clock_img, render(ctx, cw, ch))
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
    override val bgRes get() = R.drawable.widget_bg_year
    override fun render(ctx: Context, w: Float, h: Float) = YearRenderer.render(ctx, w, h)
}

class CameraWidget : BitmapWidget() {
    override val defW = 68f
    override val defH = 68f
    override val bgRes get() = R.drawable.widget_bg_camera
    override fun render(ctx: Context, w: Float, h: Float) = CameraTileRenderer.render(ctx, w, h)
    override fun click(ctx: Context): Intent =
        Intent(ctx, CameraActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
