package app.spotamp.playback

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import kotlin.math.sqrt

/**
 * Pass-through audio processor that measures loudness of the decoded audio
 * for the sound wave. Audio is never recorded, stored or changed.
 * Analysis runs only while the player screen is visible ([enabled]).
 */
@OptIn(UnstableApi::class)
object LevelTap : BaseAudioProcessor() {
    @Volatile var enabled = false
    @Volatile var level = 0f
        private set
    @Volatile var bass = 0f
        private set
    @Volatile var treble = 0f
        private set

    private var lowPass = 0f

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat =
        if (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT) inputAudioFormat else AudioProcessor.AudioFormat.NOT_SET

    override fun queueInput(inputBuffer: ByteBuffer) {
        val start = inputBuffer.position()
        val end = inputBuffer.limit()
        val size = end - start
        if (size == 0) return
        if (enabled) analyze(inputBuffer, start, end)
        val out = replaceOutputBuffer(size)
        out.put(inputBuffer)
        out.flip()
    }

    private fun analyze(buf: ByteBuffer, start: Int, end: Int) {
        val channels = inputAudioFormat.channelCount.coerceAtLeast(1)
        val step = 2 * channels * 4 // every 4th frame, first channel only
        var sum = 0f
        var sumLow = 0f
        var sumHigh = 0f
        var n = 0
        var i = start
        while (i + 1 < end) {
            val x = buf.getShort(i) / 32768f
            lowPass += 0.06f * (x - lowPass)
            val high = x - lowPass
            sum += x * x
            sumLow += lowPass * lowPass
            sumHigh += high * high
            n++
            i += step
        }
        if (n > 0) {
            level = sqrt(sum / n)
            bass = sqrt(sumLow / n)
            treble = sqrt(sumHigh / n)
        }
    }

    override fun onReset() {
        lowPass = 0f
        level = 0f
        bass = 0f
        treble = 0f
    }
}
