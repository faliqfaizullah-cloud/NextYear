package com.nextyear.app

import android.app.Activity
import android.app.AlarmManager
import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import java.io.File

class MainActivity : Activity() {
    private lateinit var today: TodayView
    private lateinit var sheet: MoodSheetView
    private lateinit var year: YearView
    private lateinit var pill: TabPill
    private lateinit var addBtn: AddWidgetButton
    private val prefs by lazy { getSharedPreferences("nextyear", MODE_PRIVATE) }
    private val pick = 42
    private val camReq = 43
    private var photoSig = ""

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        Haptics.init(this)
        goFullscreen()

        today = TodayView(this); sheet = MoodSheetView(this); year = YearView(this); pill = TabPill(this); addBtn = AddWidgetButton(this)
        val d = resources.displayMetrics.density
        val root = FrameLayout(this).apply { setBackgroundColor(Pal.BG) }
        root.addView(today, match()); root.addView(year, match())
        root.addView(addBtn, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.END).apply { bottomMargin = (28 * d).toInt(); rightMargin = (20 * d).toInt() })
        root.addView(sheet, match())
        root.addView(pill, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { bottomMargin = (28 * d).toInt() })
        setContentView(root)
        year.alpha = 0f; year.visibility = View.GONE

        // restore data
        for (day in 1..366) { val m = prefs.getInt("d$day", -1); if (m >= 0) year.entries[day] = m }
        val mood = year.entries[year.today] ?: prefs.getInt("mood", 3)
        sheet.pos = mood.toFloat(); year.currentMood = mood
        today.moodColor = Pal.moods[mood]
        prefs.getString("caption", null)?.let { today.caption = it }
        loadPhotos(false)

        sheet.onColor = { today.moodColor = it }
        sheet.onMood = { i ->
            year.currentMood = i; year.entries[year.today] = i
            prefs.edit().putInt("mood", i).putInt("d${year.today}", i).apply(); year.invalidate()
        }
        sheet.onReset = {
            year.entries.remove(year.today); prefs.edit().remove("d${year.today}").apply()
            sheet.pos = 2f; year.currentMood = 2; year.invalidate()
        }
        today.onTile = { sheet.show() }
        today.onCard = { startActivityForResult(Intent(this, CameraActivity::class.java), camReq) }
        today.onCardLong = {
            val opts = if (today.currentFile() != null) arrayOf("Pick from gallery", "Delete this photo") else arrayOf("Pick from gallery")
            AlertDialog.Builder(this).setItems(opts) { _, i ->
                if (i == 0) startActivityForResult(Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }, pick)
                else {
                    today.currentFile()?.delete(); Haptics.heavy(); loadPhotos(true)
                }
            }.show()
        }
        today.onCaption = {
            val et = EditText(this).apply { setText(today.caption); typeface = android.graphics.Typeface.MONOSPACE }
            AlertDialog.Builder(this).setTitle("Today's note").setView(et)
                .setPositiveButton("Save") { _, _ ->
                    today.caption = et.text.toString(); prefs.edit().putString("caption", today.caption).apply(); Haptics.click()
                }.setNegativeButton("Cancel", null).show()
        }
        year.onEntry = { day, m -> prefs.edit().apply { if (m == null) remove("d$day") else putInt("d$day", m) }.apply() }
        pill.onSelect = { i -> switchPage(i) }
        addBtn.onTap = { addWidget() }
    }

    private fun addWidget() {
        val names = arrayOf("Clock  3x2", "Year of growth  4x4", "Day  2x2", "Camera  1x1")
        val classes = arrayOf<Class<*>>(ClockWidget::class.java, YearWidget::class.java, DayWidget::class.java, CameraWidget::class.java)
        AlertDialog.Builder(this).setTitle("Add widget")
            .setItems(names) { _, i -> pinWidget(classes[i]) }.show()
    }

    private fun pinWidget(cls: Class<*>) {
        val mgr = AppWidgetManager.getInstance(this)
        if (Build.VERSION.SDK_INT >= 26 && mgr.isRequestPinAppWidgetSupported) {
            mgr.requestPinAppWidget(ComponentName(this, cls), null, null)
        } else {
            Toast.makeText(this, "Long-press your home screen > Widgets > NextYear", Toast.LENGTH_LONG).show()
        }
    }

    private fun photosDir() = File(filesDir, "photos").apply { mkdirs() }

    private fun listPhotos(): List<File> {
        val old = File(filesDir, "photo.jpg")
        if (old.exists()) old.renameTo(File(photosDir(), "${old.lastModified()}.jpg"))
        return (photosDir().listFiles { f -> f.name.endsWith(".jpg") } ?: emptyArray())
            .sortedByDescending { it.name.removeSuffix(".jpg").toLongOrNull() ?: 0L }
    }

    private fun loadPhotos(keepIndex: Boolean) {
        val list = listPhotos()
        photoSig = list.joinToString { it.name }
        today.setPhotos(list, keepIndex)
    }

    override fun onPause() {
        super.onPause()
        WidgetUtil.refreshAll(this)
    }

    override fun onResume() {
        super.onResume()
        if (listPhotos().joinToString { it.name } != photoSig) loadPhotos(false)
        val ids = AppWidgetManager.getInstance(this).getAppWidgetIds(ComponentName(this, ClockWidget::class.java))
        if (ids.isNotEmpty()) {
            ClockWidget.updateAll(this); ClockWidget.schedule(this)
            if (Build.VERSION.SDK_INT >= 31 && !prefs.getBoolean("askedAlarm", false)) {
                prefs.edit().putBoolean("askedAlarm", true).apply()
                val am = getSystemService(ALARM_SERVICE) as AlarmManager
                if (!am.canScheduleExactAlarms())
                    startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName")))
            }
        }
    }

    private fun match() = FrameLayout.LayoutParams(-1, -1)

    private fun switchPage(i: Int) {
        val show = if (i == 0) today else year
        val hide = if (i == 0) year else today
        if (i == 1) sheet.hide()
        show.visibility = View.VISIBLE
        show.animate().alpha(1f).setDuration(220).start()
        hide.animate().alpha(0f).setDuration(220).withEndAction { hide.visibility = View.GONE }.start()
    }

    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)
        if (req == camReq) { if (res == RESULT_OK) loadPhotos(false); return }
        val uri = data?.data ?: return
        if (req != pick || res != RESULT_OK) return
        try {
            val o = BitmapFactory.Options().apply { inSampleSize = 2 }
            val bmp: Bitmap = contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, o) } ?: return
            File(photosDir(), "${System.currentTimeMillis()}.jpg").outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            Haptics.heavy(); loadPhotos(false)
        } catch (_: Exception) { }
    }

    @Suppress("DEPRECATION")
    private fun goFullscreen() {
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        if (Build.VERSION.SDK_INT >= 28) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) goFullscreen()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (sheet.isOpen) sheet.hide() else super.onBackPressed()
    }
}
