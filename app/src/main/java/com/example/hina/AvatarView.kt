package com.example.hina

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class AvatarView(context: Context) : View(context) {

    private class Bubble(val text: String, val born: Float, val offset: Float)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rect = RectF()
    private val bubbles = ArrayList<Bubble>()

    private var t = 0f
    private var lastNanos = 0L
    private var blinkStart = -1f
    private var nextBlink = 2f
    private var waveUntil = -1f
    private var waveK = 0f
    private var flip = false

    private val hairColor = Color.parseColor("#6C3FC5")
    private val skinColor = Color.parseColor("#FFDFC4")
    private val dressColor = Color.parseColor("#00B8D9")
    private val eyeColor = Color.parseColor("#1B2A6B")

    fun wave() {
        waveUntil = t + 2.5f
    }

    fun say(text: String) {
        var shown = text
        if (shown.length > 60) shown = shown.substring(0, 60) + "..."
        flip = !flip
        bubbles.add(Bubble(shown, t, if (flip) -1f else 1f))
        while (bubbles.size > 3) bubbles.removeAt(0)
    }

    private fun wrap(text: String): List<String> {
        if (text.length <= 24) return listOf(text)
        val mid = text.length / 2
        var cut = text.lastIndexOf(' ', mid)
        val after = text.indexOf(' ', mid)
        if (cut < 0 || (after >= 0 && after - mid < mid - cut)) cut = after
        if (cut < 0) return listOf(text)
        return listOf(text.substring(0, cut).trim(), text.substring(cut).trim())
    }

    private fun drawArm(canvas: Canvas, sx: Float, sy: Float, deg: Float, len: Float, s: Float) {
        val rad = Math.toRadians(deg.toDouble())
        val ex = sx + (cos(rad) * len).toFloat()
        val ey = sy + (sin(rad) * len).toFloat()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 6f * s
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = skinColor
        canvas.drawLine(sx, sy, ex, ey, paint)
        paint.style = Paint.Style.FILL
        canvas.drawCircle(ex, ey, 4.2f * s, paint)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val now = System.nanoTime()
        if (lastNanos != 0L) {
            var dt = (now - lastNanos) / 1_000_000_000f
            if (dt > 0.1f) dt = 0.1f
            t += dt
        }
        lastNanos = now

        val w = width.toFloat()
        val h = height.toFloat()
        val s = min(w / 100f, h / 125f)
        val cx = w / 2f + sin(t * 1.1f) * s * 1.2f
        val headY = h / 2f - 4f * s + sin(t * 2f) * s * 1.5f
        val neckY = headY + 24f * s

        // palak jhapakna
        if (blinkStart < 0f && t >= nextBlink) blinkStart = t
        var blinking = false
        if (blinkStart >= 0f) {
            if (t - blinkStart < 0.16f) {
                blinking = true
            } else {
                blinkStart = -1f
                nextBlink = t + 2f + (Math.random() * 3.0).toFloat()
            }
        }

        // haath hilana (smooth)
        val target = if (t < waveUntil) 1f else 0f
        waveK += (target - waveK) * 0.15f

        // peeche halka glow
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#12234A")
        canvas.drawCircle(cx, headY + 20f * s, 46f * s, paint)

        // twintails (baal hilte hue)
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = 9f * s
        paint.color = hairColor
        for (side in intArrayOf(-1, 1)) {
            val sw1 = sin(t * 2f + side) * 5f * s
            val sw2 = sin(t * 2f + side + 1f) * 7f * s
            path.reset()
            path.moveTo(cx + side * 22f * s, headY - 2f * s)
            path.quadTo(
                cx + side * 38f * s + sw1, headY + 24f * s,
                cx + side * 30f * s + sw2, headY + 55f * s
            )
            canvas.drawPath(path, paint)
        }

        // dress / body
        paint.style = Paint.Style.FILL
        paint.color = dressColor
        rect.set(cx - 19f * s, neckY, cx + 19f * s, neckY + 40f * s)
        canvas.drawRoundRect(rect, 8f * s, 8f * s, paint)

        // haath
        val idleSway = sin(t * 1.5f) * 3f
        drawArm(canvas, cx - 19f * s, neckY + 8f * s, 110f + idleSway, 27f * s, s)
        val idleA = 70f - idleSway
        val waveA = -60f + sin(t * 12f) * 20f
        val rightA = idleA + (waveA - idleA) * waveK
        drawArm(canvas, cx + 19f * s, neckY + 8f * s, rightA, 27f * s, s)

        // sir
        paint.style = Paint.Style.FILL
        paint.color = skinColor
        canvas.drawCircle(cx, headY, 24f * s, paint)

        // baal upar
        paint.color = hairColor
        rect.set(cx - 26f * s, headY - 28f * s, cx + 26f * s, headY + 12f * s)
        canvas.drawArc(rect, 180f, 180f, true, paint)
        for (i in -1..1) {
            val bx = cx + i * 15f * s
            path.reset()
            path.moveTo(bx - 9f * s, headY - 9f * s)
            path.lineTo(bx, headY + 3f * s)
            path.lineTo(bx + 9f * s, headY - 9f * s)
            path.close()
            canvas.drawPath(path, paint)
        }

        // aankhein
        val look = sin(t * 0.7f) * 1.5f * s
        for (side in intArrayOf(-1, 1)) {
            val ex = cx + side * 10f * s
            val ey = headY + 8f * s
            if (blinking) {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1.6f * s
                paint.strokeCap = Paint.Cap.ROUND
                paint.color = eyeColor
                canvas.drawLine(ex - 5f * s, ey, ex + 5f * s, ey, paint)
            } else {
                paint.style = Paint.Style.FILL
                paint.color = eyeColor
                rect.set(ex - 4.5f * s + look, ey - 6f * s, ex + 4.5f * s + look, ey + 6f * s)
                canvas.drawOval(rect, paint)
                paint.color = Color.WHITE
                canvas.drawCircle(ex - 1.5f * s + look, ey - 2.5f * s, 1.6f * s, paint)
            }
        }

        // blush
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#66FF8FA3")
        canvas.drawCircle(cx - 17f * s, headY + 16f * s, 3.5f * s, paint)
        canvas.drawCircle(cx + 17f * s, headY + 16f * s, 3.5f * s, paint)

        // muskaan
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.3f * s
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = Color.parseColor("#C0505F")
        rect.set(cx - 4f * s, headY + 14f * s, cx + 4f * s, headY + 20f * s)
        canvas.drawArc(rect, 20f, 140f, false, paint)

        // ribbon
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#FF4D8D")
        canvas.drawCircle(cx + 18f * s, headY - 20f * s, 4f * s, paint)

        // floating text bubbles
        val iter = bubbles.iterator()
        while (iter.hasNext()) {
            val b = iter.next()
            val age = t - b.born
            if (age > 4f) {
                iter.remove()
                continue
            }
            val alpha = if (age > 3f) ((4f - age) * 255f).toInt() else 255
            val lines = wrap(b.text)
            paint.textSize = 4.6f * s
            var tw = 0f
            for (ln in lines) tw = max(tw, paint.measureText(ln))
            val lh = 6f * s
            val top = headY - 40f * s - age * 5f * s
            var x = cx + b.offset * 12f * s - tw / 2f
            x = max(5f * s, min(x, w - tw - 5f * s))
            rect.set(x - 3f * s, top, x + tw + 3f * s, top + lh * lines.size + 1.5f * s)

            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#1E3A6E")
            paint.alpha = alpha * 220 / 255
            canvas.drawRoundRect(rect, 3f * s, 3f * s, paint)

            paint.color = Color.WHITE
            paint.alpha = alpha
            for (i in lines.indices) {
                canvas.drawText(lines[i], x, top + lh * (i + 1) - 0.5f * s, paint)
            }
        }

        postInvalidateOnAnimation()
    }
}
