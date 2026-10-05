package com.nextyear.app

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.LruCache
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import java.io.File
import kotlin.math.abs
import kotlin.math.max

class TodayView(ctx: Context) : View(ctx) {
    private val d = resources.displayMetrics.density
    private val slop = ViewConfiguration.get(ctx).scaledTouchSlop

    var moodColor = Pal.INK
        set(v) { field = v; invalidate() }
    var caption = ""
        set(v) { field = v; invalidate() }
    var onCard: (() -> Unit)? = null
    var onCardLong: (() -> Unit)? = null
    var onCaption: (() -> Unit)? = null
    var onTile: (() -> Unit)? = null

    // ---- photo pile (index 0 = newest) ----
    var photos: List<File> = emptyList()
        private set
    var index = 0
        private set
    private val cache = LruCache<String, Bitmap>(10)
    private val pending = HashSet<String>()

    fun setPhotos(list: List<File>, keepIndex: Boolean) {
        photos = list
        index = if (keepIndex) index.coerceIn(0, max(0, list.size - 1)) else 0
        dragX = 0f; appear = 1f
        invalidate()
    }

    fun currentFile(): File? = photos.getOrNull(index)

    private fun bmp(i: Int): Bitmap? {
        val f = photos.getOrNull(i) ?: return null
        cache.get(f.path)?.let { return it }
        if (pending.add(f.path)) {
            Thread {
                try {
                    val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(f.path, o)
                    var s = 1
                    while (max(o.outWidth, o.outHeight) / (s * 2) >= 1000) s *= 2
                    val b = BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = s })
                    if (b != null) cache.put(f.path, b)
                } catch (e: Throwable) { }
                post { pending.remove(f.path); invalidate() }
            }.start()
        }
        return null
    }

    // ---- paints / geometry ----
    private val statusH = run {
        val id = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (id > 0) resources.getDimensionPixelSize(id).toFloat() else 24 * d
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.MONOSPACE; color = Pal.INK }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tileRect = RectF()
    private val cardRect = RectF()
    private val capRect = RectF()
    private val selected = 3

    private var dragX = 0f
    private var appear = 1f
    private var anim: ValueAnimator? = null

    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        c.drawColor(Pal.BG)
        val top = max(52 * d, statusH + 24 * d)

        // label + icon strip
        text.textSize = 12 * d; text.textAlign = Paint.Align.CENTER
        val x = { i: Int -> 38 * d + i * 58 * d }
        c.drawText("Today", x(selected), top, text)
        val types = intArrayOf(3, 2, 0, 1, 5)
        for (i in 0 until 5) {
            val cx = x(i); val cy = top + 40 * d
            if (i == selected) {
                tileRect.set(cx - 27 * d, cy - 27 * d, cx + 27 * d, cy + 27 * d)
                fill.color = moodColor; fill.alpha = 55
                c.drawRoundRect(tileRect, 14 * d, 14 * d, fill)
                Doodle.mini(c, types[i], cx, cy, 14 * d, Doodle.pen(moodColor, 2.6f * d))
            } else {
                val p = Doodle.pen(Pal.INK, 2.2f * d); p.alpha = if (i == 0) 90 else 255
                Doodle.mini(c, types[i], cx, cy, 13 * d, p)
            }
        }

        // photo pile
        val cx = w / 2; val cy = h * 0.355f
        val cw = w * 0.44f; val ch = cw * 1.38f
        cardRect.set(cx - cw / 2, cy - ch / 2, cx + cw / 2, cy + ch / 2)
        val n = photos.size
        val left = n - index - 1

        if (n == 0) {
            drawBacks(c, cx, cy)
            drawCard(c, null, 2f, 0f, 1f)
        } else {
            if (left <= 0) drawBacks(c, cx, cy)
            for (k in minOf(2, left) downTo 1) {
                drawCard(c, bmp(index + k), if (k == 1) 4f else -5f, 0f, 1f)
            }
            val rot = 2f + dragX / w * 14f
            drawCard(c, bmp(index), rot, dragX, 0.94f + 0.06f * appear)
        }

        if (n > 1) {
            text.textSize = 11 * d; text.textAlign = Paint.Align.CENTER
            text.color = 0xFF9C95E6.toInt()
            c.drawText("${index + 1} / $n", cx, cardRect.bottom + 0.045f * h, text)
            text.color = Pal.INK
        }

        // caption
        text.textAlign = Paint.Align.LEFT; text.textSize = 17 * d
        var y = cardRect.bottom + 0.1f * h
        val x0 = w * 0.1f
        val blank = caption.isBlank()
        text.color = if (blank) 0xFF9C95E6.toInt() else Pal.INK
        val lines = (if (blank) "Start writing" else caption).split("\n")
        capRect.set(x0, y - 22 * d, w - x0, y + lines.size * 24 * d)
        for (ln in lines) { c.drawText(ln, x0, y, text); y += 24 * d }
        text.color = Pal.INK
    }

    private fun drawBacks(c: Canvas, cx: Float, cy: Float) {
        for ((rot, col) in listOf(-5f to 0xFFB08A5A.toInt(), 4f to 0xFFC9A877.toInt())) {
            c.save(); c.rotate(rot, cx, cy)
            fill.shader = null; fill.color = col; fill.alpha = 255
            c.drawRoundRect(cardRect, 8 * d, 8 * d, fill)
            c.restore()
        }
    }

    private fun drawCard(c: Canvas, b: Bitmap?, rot: Float, dx: Float, scale: Float) {
        c.save()
        c.translate(dx, 0f)
        c.rotate(rot, cardRect.centerX(), cardRect.centerY())
        c.scale(scale, scale, cardRect.centerX(), cardRect.centerY())
        fill.shader = null; fill.color = 0xFFC9C6E8.toInt(); fill.alpha = 255
        fill.setShadowLayer(8 * d, 0f, 3 * d, 0x33000000)
        c.drawRoundRect(cardRect, 8 * d, 8 * d, fill)
        fill.clearShadowLayer()
        c.save()
        c.clipPath(Path().apply { addRoundRect(cardRect, 8 * d, 8 * d, Path.Direction.CW) })
        if (b != null) {
            val ra = cardRect.width() / cardRect.height(); val ba = b.width.toFloat() / b.height
            val src = if (ba > ra) {
                val sw = b.height * ra; Rect(((b.width - sw) / 2).toInt(), 0, ((b.width + sw) / 2).toInt(), b.height)
            } else {
                val sh = b.width / ra; Rect(0, ((b.height - sh) / 2).toInt(), b.width, ((b.height + sh) / 2).toInt())
            }
            c.drawBitmap(b, src, cardRect, null)
        } else if (photos.isEmpty()) drawEmpty(c)
        c.restore()
        c.restore()
    }

    private fun drawEmpty(c: Canvas) {
        val p = Doodle.pen(Pal.INK, 2.4f * d)
        val cx = cardRect.centerX(); val cy = cardRect.centerY()
        val r = RectF(cx - 18 * d, cy - 12 * d, cx + 18 * d, cy + 14 * d)
        c.drawRoundRect(r, 6 * d, 6 * d, p)
        c.drawCircle(cx, cy + 1 * d, 7 * d, p)
        c.drawLine(cx - 7 * d, cy - 12 * d, cx - 4 * d, cy - 17 * d, p)
        c.drawLine(cx - 4 * d, cy - 17 * d, cx + 4 * d, cy - 17 * d, p)
        c.drawLine(cx + 4 * d, cy - 17 * d, cx + 7 * d, cy - 12 * d, p)
    }

    // ---- swipe through the pile ----
    private var downX = 0f; private var downY = 0f; private var downT = 0L
    private var dragging = false
    private var onCardDown = false

    private fun fling(dir: Int) {
        // dir -1: swipe left -> next photo; +1: swipe right -> previous photo
        val target = if (dir < 0) index + 1 else index - 1
        if (target < 0 || target >= photos.size) { settle(); return }
        Haptics.soft()
        anim?.cancel()
        anim = ValueAnimator.ofFloat(dragX, dir * width * 1.1f).apply {
            duration = 200
            addUpdateListener { dragX = it.animatedValue as Float; invalidate() }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(a: Animator) {
                    index = target; dragX = 0f; appear = 0f
                    anim = ValueAnimator.ofFloat(0f, 1f).apply {
                        duration = 180; interpolator = DecelerateInterpolator()
                        addUpdateListener { appear = it.animatedValue as Float; invalidate() }
                        start()
                    }
                    invalidate()
                }
            })
            start()
        }
    }

    private fun settle() {
        anim?.cancel()
        anim = ValueAnimator.ofFloat(dragX, 0f).apply {
            duration = 180; interpolator = DecelerateInterpolator()
            addUpdateListener { dragX = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = e.x; downY = e.y; downT = e.eventTime; dragging = false
                onCardDown = cardRect.contains(e.x, e.y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (onCardDown && photos.size > 1) {
                    val dx = e.x - downX
                    if (!dragging && abs(dx) > slop && abs(dx) > abs(e.y - downY)) {
                        dragging = true; anim?.cancel(); parent?.requestDisallowInterceptTouchEvent(true)
                    }
                    if (dragging) {
                        val atEnd = (dx < 0 && index >= photos.size - 1) || (dx > 0 && index <= 0)
                        dragX = if (atEnd) dx * 0.3f else dx
                        invalidate()
                    }
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> { if (dragging) settle(); dragging = false; return true }
            MotionEvent.ACTION_UP -> {
                if (dragging) {
                    dragging = false
                    val dt = max(1L, e.eventTime - downT)
                    val fast = abs(e.x - downX) / dt > 0.8f
                    if (abs(dragX) > cardRect.width() * 0.22f || (fast && abs(dragX) > 24 * d)) fling(if (dragX < 0) -1 else 1)
                    else settle()
                    return true
                }
                when {
                    tileRect.contains(e.x, e.y) -> { Haptics.soft(); onTile?.invoke() }
                    capRect.contains(e.x, e.y) -> { Haptics.tick(); onCaption?.invoke() }
                    cardRect.contains(e.x, e.y) -> {
                        if (e.eventTime - downT > 500) { Haptics.click(); onCardLong?.invoke() }
                        else { Haptics.soft(); onCard?.invoke() }
                    }
                }
                return true
            }
        }
        return true
    }
}
