package app.spotamp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import app.spotamp.playback.LevelTap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * The misty music-staff wave with drifting light and notes.
 * Animates only while the screen is visible; with the screen off nothing is drawn or computed.
 *
 * @param waveTop top of the 92 dp wave band, in px from the top of this layer
 * @param style 0 flowing, 1 calm, 2 off
 */
@Composable
fun WaveLayer(modifier: Modifier, waveTop: Float, playing: Boolean, empty: Boolean, style: Int) {
    if (style == 2) return
    val anim = remember { WaveAnimation() }
    var frame by remember { mutableLongStateOf(0L) }
    val isPlaying by rememberUpdatedState(playing)
    val isEmpty by rememberUpdatedState(empty)
    val calm by rememberUpdatedState(style == 1)
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(owner) {
        owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            LevelTap.enabled = true
            try {
                var last = 0L
                while (true) {
                    withFrameNanos { now ->
                        val dt = if (last == 0L) 0.016f else ((now - last) / 1e9f).coerceIn(0f, 0.05f)
                        last = now
                        anim.step(dt, isPlaying, isEmpty, calm)
                        frame = now
                    }
                }
            } finally {
                LevelTap.enabled = false
            }
        }
    }
    Canvas(modifier) {
        if (frame >= 0L) anim.draw(this, waveTop)
    }
}

private class Particle(
    val x: Float, val y: Float, val vx: Float, val amp: Float, val freq: Float, val phase: Float,
    val size: Float, val big: Boolean, val alpha: Float, val color: Color,
)

private class Note(val u: Float, val scale: Float, val double: Boolean, val riding: Boolean, val y: Float, val color: Color)

private class WaveAnimation {
    private var t = 2.4f
    private var pt = 0f
    private var level = 0.3f
    private var amp = 1f
    private var fade = 1f

    private val lines = 15
    private val points = 48
    private val linePaths = List(lines) { Path() }
    private val core = Path()

    private val single = PathParser().parsePathString("M6.2 0.5v10.1a2.9 2.9 0 1 0 1.6 2.6V4.2c1.7.4 3 1.6 3.3 3.2.7-2.9-1.1-5.6-4.9-6.9z").toPath()
    private val double = PathParser().parsePathString("M4.5 3.2L14.5 1v10.4a2.6 2.6 0 1 1-1.5-2.3V4.9L6 6.4v7a2.6 2.6 0 1 1-1.5-2.3z").toPath()

    private val meshColors = listOf(Color(0x268A6CFF), Color(0xFFA57CF0), Color(0xFFD98AB0), Color(0xFFF59A5B), Color(0xFFFFC070))
    private val mistColors = listOf(Color(0x1A7A55E0), Color(0x8CB85FA8), Color(0x8CF59A5B), Color(0x8CFFB45A))
    private val dustColors = listOf(Color(0xFFFFB45A), Color(0xFFF59A5B), Color(0xFFFFD08A), Color(0xFFFFB45A), Color(0xFFC9A2FF))

    private val particles = List(36) { i ->
        val big = i % 9 == 0
        val mid = i % 3 == 0
        Particle(
            x = (i * 47 % 430).toFloat(), y = (40 + i * 29 % 250).toFloat(),
            vx = 4f + (i % 6) * 2.5f, amp = 3f + (i % 4) * 3f, freq = 0.5f + (i % 3) * 0.3f, phase = i * 1.3f,
            size = if (big) 12f + (i % 2) * 6f else if (mid) 4f else 1.6f + (i % 2),
            big = big, alpha = if (big) 0.3f else if (mid) 0.7f else 0.85f, color = dustColors[i % 5],
        )
    }

    private val notes = listOf(
        Note(0.12f, 1f, false, true, 0f, Color(0xFFE2C2FF)),
        Note(0.46f, 1.25f, false, true, 0f, Color(0xFFFFC27A)),
        Note(0.74f, 0.9f, true, true, 0f, Color(0xFFFFC27A)),
        Note(0.1f, 0.85f, true, false, 62f, Color(0xFFE9C4FF)),
        Note(0.56f, 0.75f, false, false, 60f, Color(0xFFFFC9A0)),
        Note(0.8f, 0.8f, false, false, 150f, Color(0xFFFFD08A)),
    )

    fun step(dt: Float, playing: Boolean, empty: Boolean, calm: Boolean) {
        val measured = (LevelTap.level * 3.2f).coerceIn(0f, 1f)
        val target = when {
            empty -> 0.06f
            playing -> 0.55f + 0.45f * measured
            else -> 0.22f
        }
        level += (target - level) * min(1f, dt * if (playing) 6f else 1.5f)
        amp = level * if (calm) 0.5f else 1f
        val speed = if (calm) 0.5f else 1f
        t += dt * speed * if (playing) 1f else 0.15f
        pt += dt * if (empty) 0.15f else if (playing) 1f else 0.3f
        val targetFade = if (empty) 0.35f else if (playing) 1f else 0.65f
        fade += (targetFade - fade) * min(1f, dt * 2f)
    }

    private fun center(u: Float, h: Float): Float {
        val taper = 0.35f + 0.65f * sin(PI.toFloat() * u).coerceAtLeast(0f).pow(1.1f)
        val env = 0.8f * exp(-(u - 0.3f).pow(2) / 0.03f) + 0.9f * exp(-(u - 0.75f).pow(2) / 0.03f) + 0.4f
        return h / 2f + (0.25f + 0.75f * amp) * h * 0.22f * taper * env * sin(u * 8.5f + t * 2f)
    }

    private fun spread(u: Float, h: Float): Float =
        (h * 0.055f + h * 0.15f * sin(PI.toFloat() * u)) * cos(u * 7f - t * 1.1f) * (0.6f + 0.4f * amp)

    fun draw(scope: DrawScope, waveTop: Float) = with(scope) {
        val w = size.width
        val dp = density
        val h = 92f * dp
        val meshBrush = Brush.horizontalGradient(meshColors, 0f, w)

        // Build the staff lines.
        for (l in 0 until lines) {
            val s = -1f + 2f * l / (lines - 1)
            val p = linePaths[l]
            p.reset()
            for (i in 0..points) {
                val u = i / points.toFloat()
                val y = waveTop + center(u, h) + spread(u, h) * s + 1.4f * dp * amp * sin(u * 22f + l * 0.8f + t * 3f)
                if (i == 0) p.moveTo(0f, y) else p.lineTo(u * w, y)
            }
        }
        core.reset()
        for (i in 0..points) {
            val u = i / points.toFloat()
            val y = waveTop + center(u, h)
            if (i == 0) core.moveTo(0f, y) else core.lineTo(u * w, y)
        }

        // Mist: wide soft strokes under the lines.
        val mistBrush = Brush.horizontalGradient(mistColors, 0f, w)
        val mistAlpha = 0.5f * fade
        for ((width, alpha) in listOf(30f to 0.10f, 20f to 0.16f, 11f to 0.22f)) {
            drawPath(core, mistBrush, alpha = alpha * mistAlpha * 2f, style = Stroke(width * dp, cap = StrokeCap.Round))
            drawPath(linePaths[0], mistBrush, alpha = alpha * mistAlpha, style = Stroke(width * 0.7f * dp, cap = StrokeCap.Round))
            drawPath(linePaths[lines - 1], mistBrush, alpha = alpha * mistAlpha, style = Stroke(width * 0.7f * dp, cap = StrokeCap.Round))
        }

        // Particles behind and around the wave.
        val panelH = size.height
        particles.forEach { q ->
            val x = ((q.x + q.vx * pt) % 440f - 14f) * dp
            val y = (q.y + q.amp * sin(q.freq * pt + q.phase)) * dp
            if (y > panelH) return@forEach
            val twinkle = 0.55f + 0.45f * sin(pt * 2.2f + q.phase)
            val a = (q.alpha * twinkle * fade).coerceIn(0f, 1f)
            val r = q.size * dp / 2f
            if (q.big) {
                drawCircle(Brush.radialGradient(listOf(q.color.copy(alpha = a), Color.Transparent), Offset(x, y), r * 1.6f), r * 1.6f, Offset(x, y))
            } else {
                drawCircle(Brush.radialGradient(listOf(q.color.copy(alpha = a * 0.45f), Color.Transparent), Offset(x, y), r * 3.5f), r * 3.5f, Offset(x, y))
                drawCircle(q.color.copy(alpha = a), r, Offset(x, y))
            }
        }

        // Lines: half thin and solid, half dotted, plus a bright core.
        val alpha = fade
        val dots = PathEffect.dashPathEffect(floatArrayOf(0.01f, 3.4f * dp))
        for (l in 0 until lines) {
            if (l % 2 == 1) {
                drawPath(linePaths[l], meshBrush, alpha = 0.7f * alpha, style = Stroke(0.6f * dp))
            } else {
                drawPath(linePaths[l], meshBrush, alpha = 0.9f * alpha, style = Stroke(1.4f * dp, cap = StrokeCap.Round, pathEffect = dots))
            }
        }
        drawPath(core, Color(0xFFFFE6C8), alpha = 0.5f * alpha, style = Stroke(0.8f * dp))

        // Notes: some ride the wave, some float.
        notes.forEachIndexed { i, n ->
            val x: Float
            val y: Float
            val edge: Float
            if (n.riding) {
                val u = (n.u + pt * 0.018f) % 1f
                x = u * w - 6f * dp
                y = waveTop + center(u, h) - 18f * dp + 3f * dp * sin(pt * 1.4f + i)
                edge = min(1f, min(u, 1f - u) / 0.12f)
            } else {
                val px = ((n.u * 412f + 5f * pt) % 440f) - 16f
                x = px * dp
                y = (n.y + 7f * sin(pt * 0.8f + i)) * dp
                edge = min(1f, min(px + 16f, 424f - px) / 40f) * 0.55f
            }
            val a = (max(0f, edge) * 0.9f * fade).coerceIn(0f, 1f)
            if (a <= 0.01f) return@forEachIndexed
            translate(x, y) {
                rotate(10f * sin(pt * 1.1f + i * 2f), pivot = Offset.Zero) {
                    scale(n.scale * dp, pivot = Offset.Zero) {
                        drawPath(if (n.double) double else single, n.color.copy(alpha = a))
                    }
                }
            }
        }
    }
}
