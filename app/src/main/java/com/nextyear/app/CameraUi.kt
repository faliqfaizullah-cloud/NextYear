package com.nextyear.app

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.abs
import kotlin.math.sin

object CamGeo {
    /** Viewfinder rectangle (matches the video: wide frame under the top icons). */
    fun vf(w: Int, h: Int) = RectF(0.02f * w, 0.2f * h, 0.98f * w, 0.76f * h)
}

/** Transparent overlay: hand-drawn controls, wobbly viewfinder outline, review card. */
class CameraUi(ctx: Context) : View(ctx) {
    interface Cb {
        fun onClose(); fun onFlash(); fun onZoom(); fun onShutter()
        fun onFlip(); fun onRetake(); fun onConfirm()
    }

    var cb: Cb? = null
    var flash = false
    var flashOk = true
    var zoomLabel = "1x"

    private val d = resources.displayMetrics.density
    private var review = false
    private var p = 0f
    private var photo: Bitmap? = null
    private var down = false
    private var anim: ValueAnimator? = null
    private var outline: Path? = null
    private var outW = 0

    private val pen = Doodle.pen(Pal.INK, 2.1f * d)
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val txt = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Pal.INK; typeface = Typeface.MONOSPACE; textAlign = Paint.Align.CENTER
        textSkewX = -0.2f; isFakeBoldText = true; textSize = 15 * d
    }

    fun show(b: Bitmap) { photo = b; review = true; animateTo(1f, null) }
    fun hide() { review = false; animateTo(0f) { photo = null } }

    private fun animateTo(to: Float, end: (() -> Unit)?) {
        anim?.cancel()
        anim = ValueAnimator.ofFloat(p, to).apply {
            duration = 380
            interpolator = DecelerateInterpolator(1.6f)
            addUpdateListener { p = it.animatedValue as Float; invalidate() }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(a: Animator) { end?.invoke() }
            })
            start()
        }
    }

    private fun buildOutline(vf: RectF): Path {
        val rr = Path().apply { addRoundRect(vf, 26 * d, 26 * d, Path.Direction.CW) }
        val pm = PathMeasure(rr, true)
        val len = pm.length
        val n = (len / (22 * d)).toInt().coerceAtLeast(8)
        val pts = Array(n) { i ->
            val pos = FloatArray(2); val tan = FloatArray(2)
            pm.getPosTan(len * i / n, pos, tan)
            val off = d * (1.8f * sin(i * 0.9f) + 1.1f * sin(i * 2.3f + 1f))
            floatArrayOf(pos[0] + tan[1] * off, pos[1] - tan[0] * off)
        }
        val path = Path()
        val last = pts[n - 1]; val first = pts[0]
        path.moveTo((last[0] + first[0]) / 2, (last[1] + first[1]) / 2)
        for (i in 0 until n) {
            val a = pts[i]; val b = pts[(i + 1) % n]
            path.quadTo(a[0], a[1], (a[0] + b[0]) / 2, (a[1] + b[1]) / 2)
        }
        path.close()
        return path
    }

    override fun onDraw(c: Canvas) {
        if (width == 0) return
        val w = width.toFloat(); val h = height.toFloat()
        val vf = CamGeo.vf(width, height)
        val by = 0.875f * h
        if (outline == null || outW != width) { outline = buildOutline(vf); outW = width }

        if (p > 0f) { fill.shader = null; fill.color = Pal.BG; c.drawRect(0f, 0f, w, h, fill) }

        val ca = ((1f - p) * 255).toInt()
        if (ca > 0) {
            pen.alpha = ca; txt.alpha = ca
            drawClose(c, 0.07f * w, 0.115f * h)
            drawFlash(c, 0.91f * w, 0.115f * h)
            c.drawPath(outline!!, pen)
            c.drawText(zoomLabel, 0.14f * w, by + 5 * d, txt)
            drawShutter(c, 0.5f * w, by)
            drawFlip(c, 0.86f * w, by)
        }
        if (p > 0f) {
            photo?.let { drawCard(c, vf, it, p) }
            pen.alpha = (p * 255).toInt()
            drawClose(c, 0.31f * w, by)
            drawCheck(c, 0.69f * w, by)
        }
        pen.alpha = 255
    }

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

    private fun drawCard(c: Canvas, vf: RectF, b: Bitmap, t: Float) {
        val cw = 0.68f * width
        val ch = cw / (b.width.toFloat() / b.height)
        val tcx = 0.5f * width; val tcy = 0.46f * height
        val r = RectF(
            lerp(vf.left, tcx - cw / 2, t), lerp(vf.top, tcy - ch / 2, t),
            lerp(vf.right, tcx + cw / 2, t), lerp(vf.bottom, tcy + ch / 2, t)
        )
        val rad = (26 - 6 * t) * d
        c.save()
        c.rotate(2.2f * t, r.centerX(), r.centerY())
        fill.color = 0xFFDADADD.toInt()
        fill.setShadowLayer(14 * d, 0f, 5 * d, 0x40000000)
        c.drawRoundRect(r, rad, rad, fill)
        fill.clearShadowLayer()
        c.clipPath(Path().apply { addRoundRect(r, rad, rad, Path.Direction.CW) })
        val ra = r.width() / r.height(); val ba = b.width.toFloat() / b.height
        val src = if (ba > ra) {
            val sw = b.height * ra; Rect(((b.width - sw) / 2).toInt(), 0, ((b.width + sw) / 2).toInt(), b.height)
        } else {
            val sh = b.width / ra; Rect(0, ((b.height - sh) / 2).toInt(), b.width, ((b.height + sh) / 2).toInt())
        }
        c.drawBitmap(b, src, r, null)
        c.restore()
    }

    private fun drawClose(c: Canvas, x: Float, y: Float) {
        val a = 7 * d
        c.drawLine(x - a, y - a, x + a, y + a, pen); c.drawLine(x + a, y - a, x - a, y + a, pen)
    }

    private fun drawCheck(c: Canvas, x: Float, y: Float) {
        val a = 8 * d
        val q = Path().apply { moveTo(x - a, y); lineTo(x - a * 0.25f, y + a * 0.75f); lineTo(x + a, y - a * 0.7f) }
        c.drawPath(q, pen)
    }

    private fun drawFlash(c: Canvas, x: Float, y: Float) {
        val u = 10 * d
        val old = pen.alpha
        if (!flashOk) pen.alpha = old / 3
        val q = Path().apply {
            moveTo(x + 0.35f * u, y - u); lineTo(x - 0.5f * u, y + 0.1f * u)
            lineTo(x + 0.1f * u, y + 0.1f * u); lineTo(x - 0.35f * u, y + u)
        }
        c.drawPath(q, pen)
        if (!flash) c.drawLine(x - 0.95f * u, y - 0.95f * u, x + 0.95f * u, y + 0.95f * u, pen)
        pen.alpha = old
    }

    private fun drawFlip(c: Canvas, x: Float, y: Float) {
        val r = 9 * d
        val rect = RectF(x - r, y - r, x + r, y + r)
        c.drawArc(rect, 200f, 130f, false, pen)
        c.drawArc(rect, 20f, 130f, false, pen)
        c.drawLine(x + r * 0.5f, y - r * 0.95f, x + r * 0.95f, y - r * 0.95f, pen)
        c.drawLine(x + r * 0.95f, y - r * 0.95f, x + r * 0.95f, y - r * 0.5f, pen)
        c.drawLine(x - r * 0.5f, y + r * 0.95f, x - r * 0.95f, y + r * 0.95f, pen)
        c.drawLine(x - r * 0.95f, y + r * 0.95f, x - r * 0.95f, y + r * 0.5f, pen)
    }

    private fun drawShutter(c: Canvas, cx: Float, cy: Float) {
        val r = (if (down) 22 else 26) * d
        c.save()
        c.clipPath(Path().apply { addCircle(cx, cy, r, Path.Direction.CW) })
        val hp = Doodle.pen(Pal.INK, 1.8f * d)
        hp.alpha = if (down) 255 else 230
        var k = -2 * r
        val step = if (down) 3.2f * d else 4.6f * d
        while (k < 2 * r) { c.drawLine(cx + k - r, cy + r, cx + k + r, cy - r, hp); k += step }
        c.restore()
        val op = Doodle.pen(Pal.INK, 2.2f * d)
        c.drawCircle(cx, cy, r, op)
        c.drawCircle(cx + 0.8f * d, cy - 0.6f * d, r - 0.8f * d, op)
    }

    private fun hit(e: MotionEvent, x: Float, y: Float, r: Float) = abs(e.x - x) < r && abs(e.y - y) < r

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val w = width.toFloat(); val h = height.toFloat(); val by = 0.875f * h
        val tap = 32 * d
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                if (!review && p == 0f && hit(e, 0.5f * w, by, 40 * d)) { down = true; Haptics.tick(); invalidate() }
            }
            MotionEvent.ACTION_CANCEL -> { down = false; invalidate() }
            MotionEvent.ACTION_UP -> {
                if (down) {
                    down = false; invalidate()
                    if (hit(e, 0.5f * w, by, 48 * d)) cb?.onShutter()
                } else if (!review && p == 0f) {
                    when {
                        hit(e, 0.07f * w, 0.115f * h, tap) -> cb?.onClose()
                        hit(e, 0.91f * w, 0.115f * h, tap) -> cb?.onFlash()
                        hit(e, 0.14f * w, by, tap) -> cb?.onZoom()
                        hit(e, 0.86f * w, by, tap) -> cb?.onFlip()
                    }
                } else if (review && p > 0.95f) {
                    when {
                        hit(e, 0.31f * w, by, tap) -> cb?.onRetake()
                        hit(e, 0.69f * w, by, tap) -> cb?.onConfirm()
                    }
                }
            }
        }
        return true
    }
}
