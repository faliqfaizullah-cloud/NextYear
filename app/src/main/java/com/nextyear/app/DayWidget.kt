package com.nextyear.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import java.util.Calendar

/** Swipe the stack: today, yesterday, the day before... Tap today to capture a moment. */
class DayWidget : AppWidgetProvider() {

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { update(ctx, mgr, it) }
    }

    override fun onAppWidgetOptionsChanged(ctx: Context, mgr: AppWidgetManager, id: Int, o: Bundle) {
        mgr.notifyAppWidgetViewDataChanged(id, R.id.day_stack)
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        when (intent.action) {
            Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                val mgr = AppWidgetManager.getInstance(ctx)
                mgr.getAppWidgetIds(ComponentName(ctx, DayWidget::class.java)).forEach { update(ctx, mgr, it) }
            }
        }
    }

    companion object {
        fun update(ctx: Context, mgr: AppWidgetManager, id: Int) {
            val rv = RemoteViews(ctx.packageName, R.layout.widget_day)
            val svc = Intent(ctx, DayStackService::class.java).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            svc.data = Uri.parse(svc.toUri(Intent.URI_INTENT_SCHEME))
            rv.setRemoteAdapter(R.id.day_stack, svc)
            rv.setEmptyView(R.id.day_stack, R.id.day_empty)
            val open = Intent(ctx, MainActivity::class.java).setAction("com.nextyear.app.OPEN_DAY")
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
            rv.setPendingIntentTemplate(R.id.day_stack, PendingIntent.getActivity(ctx, id, open, flags))
            mgr.updateAppWidget(id, rv)
            mgr.notifyAppWidgetViewDataChanged(id, R.id.day_stack)
        }
    }
}

class DayStackService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory = DayFactory(applicationContext)
}

class DayFactory(private val ctx: Context) : RemoteViewsService.RemoteViewsFactory {
    private val days = 30

    override fun onCreate() {}
    override fun onDataSetChanged() {}
    override fun onDestroy() {}
    override fun getCount() = days
    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount() = 1
    override fun getItemId(position: Int) = position.toLong()
    override fun hasStableIds() = false

    override fun getViewAt(position: Int): RemoteViews {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -position) }
        val rv = RemoteViews(ctx.packageName, R.layout.widget_day_item)
        rv.setImageViewBitmap(R.id.item_img, DayRenderer.render(ctx, 150f, 150f, cal))
        rv.setOnClickFillInIntent(R.id.item_root, Intent().putExtra("daysAgo", position))
        return rv
    }
}
