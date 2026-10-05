package com.nextyear.app

import android.graphics.*

object Pal {
    val INK = 0xFF1A00C8.toInt()
    val BG = 0xFFD8D8D8.toInt()
    val moods = intArrayOf(
        0xFFD6104A.toInt(), // crimson
        0xFFF08000.toInt(), // orange
        0xFF1A00C8.toInt(), // indigo
        0xFF1E88E5.toInt(), // blue
        0xFF3E8E00.toInt()  // green
    )
    // petals that have fallen for each mood (wilted -> full bloom)
    val fallen = intArrayOf(3, 3, 2, 0, 0)
}

object Doodle {
    fun pen(color: Int, w: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = w
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        this.color = color
    }

    /** Big hand-drawn daisy with stem + leaf, used on the mood sheet. */
    fun bigFlower(c: Canvas, cx: Float, cy: Float, s: Float, p: Paint, fallen: Int) {
        val petals = 7
        for (i in 0 until petals) {
            if (i < fallen) {
                val gx = cx - s * 1.3f + i * s * 0.9f
                val gy = cy + s * 2.7f + (i % 2) * s * 0.15f
                val old = p.alpha
                p.alpha = 70
                c.drawOval(RectF(gx - s * 0.35f, gy - s * 0.14f, gx + s * 0.35f, gy + s * 0.14f), p)
                p.alpha = old
                continue
            }
            c.save()
            c.rotate(i * 360f / petals + 8f, cx, cy)
            c.drawOval(RectF(cx - s * 0.24f, cy - s * 1.05f, cx + s * 0.24f, cy - s * 0.3f), p)
            c.restore()
        }
        c.drawCircle(cx, cy, s * 0.24f, p)
        val stem = Path().apply {
            moveTo(cx, cy + s * 0.3f)
            quadTo(cx + s * 0.25f, cy + s * 1.3f, cx - s * 0.1f, cy + s * 2.3f)
        }
        c.drawPath(stem, p)
        val leaf = Path().apply {
            moveTo(cx + s * 0.05f, cy + s * 1.7f)
            quadTo(cx + s * 0.7f, cy + s * 1.0f, cx + s * 1.3f, cy + s * 1.05f)
        }
        c.drawPath(leaf, p)
        c.drawOval(RectF(cx + s * 0.9f, cy + s * 0.55f, cx + s * 1.5f, cy + s * 1.15f), p)
    }

    /** Small doodles (types 0..5) that fit inside a box of +-s. */
    fun mini(c: Canvas, type: Int, cx: Float, cy: Float, s: Float, p: Paint) {
        when (type % 14) {
            0 -> { // flower
                for (i in 0 until 6) {
                    c.save(); c.rotate(i * 60f, cx, cy - s * 0.3f)
                    c.drawOval(RectF(cx - s * 0.14f, cy - s * 0.95f, cx + s * 0.14f, cy - s * 0.4f), p)
                    c.restore()
                }
                c.drawCircle(cx, cy - s * 0.3f, s * 0.12f, p)
                c.drawLine(cx, cy - s * 0.1f, cx, cy + s, p)
                c.drawLine(cx, cy + s * 0.6f, cx + s * 0.4f, cy + s * 0.3f, p)
            }
            1 -> { // tulip
                val q = Path().apply {
                    moveTo(cx - s * 0.45f, cy - s * 0.8f)
                    lineTo(cx - s * 0.45f, cy - s * 0.1f)
                    quadTo(cx, cy + s * 0.4f, cx + s * 0.45f, cy - s * 0.1f)
                    lineTo(cx + s * 0.45f, cy - s * 0.8f)
                    lineTo(cx + s * 0.2f, cy - s * 0.4f)
                    lineTo(cx, cy - s * 0.85f)
                    lineTo(cx - s * 0.2f, cy - s * 0.4f)
                    close()
                }
                c.drawPath(q, p)
                c.drawLine(cx, cy + s * 0.2f, cx, cy + s, p)
                c.drawLine(cx, cy + s * 0.7f, cx + s * 0.4f, cy + s * 0.4f, p)
            }
            2 -> { // mushroom
                c.drawArc(RectF(cx - s * 0.8f, cy - s * 0.9f, cx + s * 0.8f, cy + s * 0.3f), 180f, 180f, false, p)
                c.drawLine(cx - s * 0.8f, cy - s * 0.3f, cx + s * 0.8f, cy - s * 0.3f, p)
                c.drawLine(cx - s * 0.25f, cy - s * 0.3f, cx - s * 0.25f, cy + s * 0.9f, p)
                c.drawLine(cx + s * 0.25f, cy - s * 0.3f, cx + s * 0.25f, cy + s * 0.9f, p)
                c.drawPoint(cx - s * 0.3f, cy - s * 0.6f, p)
                c.drawPoint(cx + s * 0.25f, cy - s * 0.55f, p)
            }
            3 -> { // sprout
                c.drawLine(cx, cy + s, cx, cy - s * 0.1f, p)
                c.drawOval(RectF(cx - s * 0.8f, cy - s * 0.6f, cx - s * 0.05f, cy - s * 0.05f), p)
                c.drawOval(RectF(cx + s * 0.05f, cy - s * 0.9f, cx + s * 0.8f, cy - s * 0.35f), p)
            }
            4 -> { // tree
                c.drawCircle(cx, cy - s * 0.35f, s * 0.6f, p)
                c.drawLine(cx, cy + s * 0.25f, cx, cy + s, p)
                c.drawLine(cx, cy + s * 0.6f, cx + s * 0.3f, cy + s * 0.35f, p)
            }
            6 -> { // grass
                c.drawLine(cx, cy + s, cx, cy - s * 0.9f, p)
                c.drawLine(cx - s * 0.45f, cy + s, cx - s * 0.7f, cy - s * 0.3f, p)
                c.drawLine(cx + s * 0.45f, cy + s, cx + s * 0.7f, cy - s * 0.3f, p)
            }
            7 -> { // cactus
                c.drawRoundRect(RectF(cx - s * 0.28f, cy - s * 0.9f, cx + s * 0.28f, cy + s), s * 0.28f, s * 0.28f, p)
                c.drawLine(cx - s * 0.28f, cy + s * 0.1f, cx - s * 0.7f, cy + s * 0.1f, p)
                c.drawLine(cx - s * 0.7f, cy + s * 0.1f, cx - s * 0.7f, cy - s * 0.4f, p)
                c.drawLine(cx + s * 0.28f, cy - s * 0.1f, cx + s * 0.7f, cy - s * 0.1f, p)
                c.drawLine(cx + s * 0.7f, cy - s * 0.1f, cx + s * 0.7f, cy - s * 0.6f, p)
            }
            8 -> { // leaf
                val q = Path().apply {
                    moveTo(cx, cy + s); quadTo(cx - s * 0.9f, cy, cx, cy - s)
                    quadTo(cx + s * 0.9f, cy, cx, cy + s); close()
                }
                c.drawPath(q, p)
                c.drawLine(cx, cy + s, cx, cy - s * 0.4f, p)
            }
            9 -> { // spiral flower
                c.drawCircle(cx, cy - s * 0.2f, s * 0.7f, p)
                c.drawCircle(cx, cy - s * 0.2f, s * 0.35f, p)
                c.drawLine(cx, cy + s * 0.5f, cx, cy + s, p)
            }
            10 -> { // pumpkin
                c.drawOval(RectF(cx - s * 0.8f, cy - s * 0.4f, cx + s * 0.8f, cy + s * 0.7f), p)
                c.drawLine(cx, cy - s * 0.4f, cx, cy - s * 0.8f, p)
                c.drawLine(cx - s * 0.25f, cy - s * 0.3f, cx - s * 0.25f, cy + s * 0.6f, p)
                c.drawLine(cx + s * 0.25f, cy - s * 0.3f, cx + s * 0.25f, cy + s * 0.6f, p)
            }
            11 -> { // carrot
                c.drawLine(cx - s * 0.35f, cy - s * 0.3f, cx, cy + s, p)
                c.drawLine(cx + s * 0.35f, cy - s * 0.3f, cx, cy + s, p)
                c.drawLine(cx - s * 0.35f, cy - s * 0.3f, cx + s * 0.35f, cy - s * 0.3f, p)
                c.drawLine(cx, cy - s * 0.3f, cx - s * 0.3f, cy - s * 0.95f, p)
                c.drawLine(cx, cy - s * 0.3f, cx + s * 0.3f, cy - s * 0.95f, p)
                c.drawLine(cx, cy - s * 0.3f, cx, cy - s, p)
            }
            12 -> { // sailboat
                val q = Path().apply {
                    moveTo(cx - s * 0.05f, cy - s * 0.9f); lineTo(cx - s * 0.05f, cy + s * 0.3f)
                    lineTo(cx + s * 0.7f, cy + s * 0.3f); close()
                }
                c.drawPath(q, p)
                c.drawLine(cx - s * 0.7f, cy + s * 0.5f, cx + s * 0.7f, cy + s * 0.5f, p)
                c.drawLine(cx - s * 0.7f, cy + s * 0.5f, cx - s * 0.4f, cy + s * 0.85f, p)
                c.drawLine(cx + s * 0.7f, cy + s * 0.5f, cx + s * 0.4f, cy + s * 0.85f, p)
                c.drawLine(cx - s * 0.4f, cy + s * 0.85f, cx + s * 0.4f, cy + s * 0.85f, p)
            }
            13 -> { // bush
                c.drawCircle(cx - s * 0.45f, cy + s * 0.2f, s * 0.45f, p)
                c.drawCircle(cx + s * 0.45f, cy + s * 0.2f, s * 0.45f, p)
                c.drawCircle(cx, cy - s * 0.25f, s * 0.5f, p)
            }
            else -> { // apple
                c.drawCircle(cx, cy + s * 0.15f, s * 0.6f, p)
                c.drawLine(cx, cy - s * 0.45f, cx + s * 0.1f, cy - s * 0.8f, p)
                c.drawOval(RectF(cx + s * 0.1f, cy - s * 0.95f, cx + s * 0.5f, cy - s * 0.65f), p)
            }
        }
    }

    /** Thick sunflower with a mood face (mood 0..4). Used by the Day widget. */
    fun sunflower(c: Canvas, cx: Float, cy: Float, s: Float, p: Paint, mood: Int) {
        val n = 12
        for (i in 0 until n) {
            c.save(); c.rotate(i * 360f / n, cx, cy)
            c.drawOval(RectF(cx - s * 0.17f, cy - s * 1.32f, cx + s * 0.17f, cy - s * 0.82f), p)
            c.restore()
        }
        c.drawOval(RectF(cx - s * 0.82f, cy - s * 0.74f, cx + s * 0.82f, cy + s * 0.74f), p)
        val f = Paint(p).apply { style = Paint.Style.FILL }
        c.drawLine(cx - s * 0.4f, cy - s * 0.22f, cx - s * 0.2f, cy - s * 0.22f, p)
        c.drawLine(cx + s * 0.14f, cy - s * 0.22f, cx + s * 0.34f, cy - s * 0.22f, p)
        when (mood) {
            0 -> c.drawArc(RectF(cx - s * 0.28f, cy + s * 0.18f, cx + s * 0.28f, cy + s * 0.55f), 200f, 140f, false, p)
            1 -> c.drawLine(cx - s * 0.22f, cy + s * 0.3f, cx + s * 0.22f, cy + s * 0.3f, p)
            3 -> c.drawArc(RectF(cx - s * 0.3f, cy - s * 0.02f, cx + s * 0.3f, cy + s * 0.42f), 20f, 140f, false, p)
            4 -> c.drawArc(RectF(cx - s * 0.32f, cy - s * 0.05f, cx + s * 0.32f, cy + s * 0.45f), 10f, 160f, true, f)
            else -> c.drawOval(RectF(cx - s * 0.11f, cy + s * 0.08f, cx + s * 0.11f, cy + s * 0.4f), f)
        }
        c.drawLine(cx, cy + s * 0.74f, cx, cy + s * 2.35f, p)
        c.save(); c.rotate(-25f, cx - s * 0.65f, cy + s * 1.55f)
        c.drawOval(RectF(cx - s * 0.95f, cy + s * 1.4f, cx - s * 0.35f, cy + s * 1.7f), p)
        c.restore()
        c.save(); c.rotate(25f, cx + s * 0.6f, cy + s * 1.75f)
        c.drawOval(RectF(cx + s * 0.3f, cy + s * 1.6f, cx + s * 0.9f, cy + s * 1.95f), p)
        c.restore()
    }
}
