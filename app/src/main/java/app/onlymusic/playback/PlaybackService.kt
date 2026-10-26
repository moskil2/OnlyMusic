package app.onlymusic.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import app.onlymusic.MainActivity
import app.onlymusic.OnlyMusicApp
import app.onlymusic.data.LibraryRepository
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import kotlin.math.PI
import kotlin.math.sin

/**
 * Plays music in the background with the screen off. Media3 shows the
 * notification and lock screen controls and handles headset buttons,
 * Bluetooth controls and audio focus (calls, other apps).
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null
    private lateinit var player: ExoPlayer
    private lateinit var library: LibraryRepository
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())
    private val crossfader = Crossfader()
    private var fallbackArt: ByteArray? = null

    private val musicAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .build()

    override fun onCreate() {
        super.onCreate()
        library = (application as OnlyMusicApp).library

        val renderers = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean,
            ): AudioSink = DefaultAudioSink.Builder(context)
                .setAudioProcessors(arrayOf(LevelTap))
                .build()
        }

        player = ExoPlayer.Builder(this, renderers)
            .setAudioAttributes(musicAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(library.prefs.settings.value.pauseOnDisconnect)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        player.addListener(PlayerEvents())
        restoreQueue()

        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        session = MediaSession.Builder(this, player)
            .setSessionActivity(openApp)
            .setCallback(SessionCallback())
            .build()
        session?.setMediaButtonPreferences(extraButtons())

        serviceScope.launch {
            library.prefs.settings.collect { player.setHandleAudioBecomingNoisy(it.pauseOnDisconnect) }
        }
        getSystemService(AudioManager::class.java).registerAudioDeviceCallback(headphoneWatcher, handler)
        crossfader.start()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        crossfader.stop()
        handler.removeCallbacksAndMessages(null)
        getSystemService(AudioManager::class.java).unregisterAudioDeviceCallback(headphoneWatcher)
        library.prefs.savePosition(player.currentMediaItemIndex, player.currentPosition)
        session?.release()
        player.release()
        session = null
        super.onDestroy()
    }

    // ---- Queue ----

    /** Builds a playable item from a media id (the file's document Uri). */
    private fun resolve(mediaId: String): MediaItem? {
        val track = library.track(mediaId) ?: return null
        val meta = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album.ifEmpty { null })
        if (!track.hasArt) meta.setArtworkData(waveArtwork(), MediaMetadata.PICTURE_TYPE_FRONT_COVER)
        return MediaItem.Builder()
            .setMediaId(mediaId)
            .setUri(Uri.parse(mediaId))
            .setMediaMetadata(meta.build())
            .build()
    }

    private fun savedItems(): MediaSession.MediaItemsWithStartPosition {
        val q = library.prefs.loadQueue()
        val items = ArrayList<MediaItem>()
        var startIndex = 0
        q.ids.forEachIndexed { i, id ->
            val item = resolve(id) ?: return@forEachIndexed
            if (i <= q.index) startIndex = items.size
            items.add(item)
        }
        val position = if (items.isEmpty()) 0L else q.positionMs
        return MediaSession.MediaItemsWithStartPosition(items, startIndex.coerceAtMost((items.size - 1).coerceAtLeast(0)), position)
    }

    private fun restoreQueue() {
        val saved = savedItems()
        if (saved.mediaItems.isEmpty()) return
        player.setMediaItems(saved.mediaItems, saved.startIndex, saved.startPositionMs)
        player.prepare()
    }

    // ---- Repeat and stop buttons in the notification and on the lock screen ----

    private fun extraButtons(): ImmutableList<CommandButton> {
        val repeatIcon = when (player.repeatMode) {
            Player.REPEAT_MODE_ONE -> CommandButton.ICON_REPEAT_ONE
            Player.REPEAT_MODE_ALL -> CommandButton.ICON_REPEAT_ALL
            else -> CommandButton.ICON_REPEAT_OFF
        }
        return ImmutableList.of(
            CommandButton.Builder(repeatIcon)
                .setDisplayName("Repeat")
                .setSessionCommand(CMD_REPEAT)
                .build(),
            CommandButton.Builder(CommandButton.ICON_STOP)
                .setDisplayName("Stop")
                .setSessionCommand(CMD_STOP)
                .build(),
        )
    }

    private inner class SessionCallback : MediaSession.Callback {
        override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
            val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(CMD_REPEAT)
                .add(CMD_STOP)
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(commands)
                .setMediaButtonPreferences(extraButtons())
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                CMD_REPEAT.customAction -> player.repeatMode = when (player.repeatMode) {
                    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                    else -> Player.REPEAT_MODE_OFF
                }
                CMD_STOP.customAction -> {
                    player.pause()
                    player.seekTo(0L)
                }
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> =
            Futures.immediateFuture(mediaItems.mapNotNull { resolve(it.mediaId) }.toMutableList())

        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> = Futures.immediateFuture(savedItems())
    }

    // ---- Player events: saving the queue, gaps, skipping unreadable files ----

    private inner class PlayerEvents : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            val prefs = library.prefs
            if (events.contains(Player.EVENT_TIMELINE_CHANGED)) {
                val ids = List(player.mediaItemCount) { player.getMediaItemAt(it).mediaId }
                prefs.saveQueue(app.onlymusic.data.Prefs.SavedQueue(prefs.queueName, ids, player.currentMediaItemIndex, player.currentPosition))
            } else if (events.containsAny(
                    Player.EVENT_MEDIA_ITEM_TRANSITION,
                    Player.EVENT_IS_PLAYING_CHANGED,
                    Player.EVENT_POSITION_DISCONTINUITY,
                )
            ) {
                prefs.savePosition(player.currentMediaItemIndex, player.currentPosition)
            }
            if (events.contains(Player.EVENT_REPEAT_MODE_CHANGED)) {
                session?.setMediaButtonPreferences(extraButtons())
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val handedOver = crossfader.onTransition(reason)
            val settings = library.prefs.settings.value
            // Gapless off: a short silence between tracks, like a classic player.
            if (!handedOver && !settings.gapless && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                this@PlaybackService.player.pause()
                handler.postDelayed({ this@PlaybackService.player.play() }, GAP_MS)
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            val p = this@PlaybackService.player
            if (p.hasNextMediaItem()) {
                p.seekToNextMediaItem()
                p.prepare()
                p.play()
            }
        }
    }

    // ---- Resume when headphones connect ----

    private val headphoneWatcher = object : AudioDeviceCallback() {
        private var initialListSeen = false

        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            // The first call lists devices that were already connected; only react to new ones.
            if (!initialListSeen) {
                initialListSeen = true
                return
            }
            val settings = library.prefs.settings.value
            if (!settings.resumeOnConnect || player.isPlaying || player.mediaItemCount == 0) return
            if (addedDevices.any { it.isSink && it.type in headphoneTypes() }) {
                if (player.playbackState == Player.STATE_IDLE) player.prepare()
                player.play()
            }
        }
    }

    private fun headphoneTypes(): Set<Int> {
        val types = mutableSetOf(
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_USB_HEADSET,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) types += AudioDeviceInfo.TYPE_BLE_HEADSET
        return types
    }

    // ---- Crossfade: a second, temporary player plays the start of the next track ----

    private inner class Crossfader {
        private var helper: ExoPlayer? = null
        private var targetIndex = C.INDEX_UNSET

        private val tick = object : Runnable {
            override fun run() {
                step()
                handler.postDelayed(this, 100)
            }
        }

        fun start() {
            handler.post(tick)
        }

        fun stop() {
            handler.removeCallbacks(tick)
            cancel()
        }

        private fun step() {
            val fadeMs = library.prefs.settings.value.crossfadeSec * 1000L
            val h = helper
            if (fadeMs <= 0 || !player.isPlaying) {
                if (h != null) cancel()
                return
            }
            val duration = player.duration
            if (duration == C.TIME_UNSET || duration < fadeMs * 3) return
            val remaining = duration - player.currentPosition
            if (h == null) {
                val canFade = remaining in 1..fadeMs &&
                    player.hasNextMediaItem() &&
                    player.repeatMode != Player.REPEAT_MODE_ONE
                if (!canFade) return
                targetIndex = player.nextMediaItemIndex
                helper = ExoPlayer.Builder(this@PlaybackService)
                    .setAudioAttributes(musicAttributes, false)
                    .build()
                    .apply {
                        setMediaItem(player.getMediaItemAt(targetIndex))
                        volume = 0f
                        prepare()
                        play()
                    }
                return
            }
            val f = (1f - remaining.toFloat() / fadeMs).coerceIn(0f, 1f)
            player.volume = 1f - f
            h.volume = f
        }

        /** Called on every track change. Returns true when the fade was handed over to the main player. */
        fun onTransition(reason: Int): Boolean {
            val h = helper ?: return false
            val handedOver = reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO && player.currentMediaItemIndex == targetIndex
            if (handedOver) player.seekTo(h.currentPosition)
            cancel()
            return handedOver
        }

        private fun cancel() {
            helper?.release()
            helper = null
            targetIndex = C.INDEX_UNSET
            player.volume = 1f
        }
    }

    // ---- Wave artwork for files without a cover ----

    private fun waveArtwork(): ByteArray {
        fallbackArt?.let { return it }
        val size = 512
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(0f, 0f, size.toFloat(), size.toFloat(), intArrayOf(0xFF1C1030.toInt(), 0xFF220E26.toInt(), 0xFF2A0F1F.toInt()), null, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        paint.shader = RadialGradient(size * 0.3f, size * 0.5f, size * 0.6f, 0x617846E6, 0x007846E6, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.shader = LinearGradient(0f, 0f, size.toFloat(), 0f, intArrayOf(0x40A57CF0, 0xFFD98AB0.toInt(), 0xFFF59A5B.toInt(), 0xFFFFC070.toInt()), null, Shader.TileMode.CLAMP)
        val path = android.graphics.Path()
        for (line in 0 until 11) {
            val s = -1f + 2f * line / 10f
            path.reset()
            for (i in 0..64) {
                val u = i / 64f
                val center = size * 0.5f + size * 0.12f * sin(u * 8.5f + 1.4f) * (0.35f + 0.65f * sin(PI.toFloat() * u))
                val spread = (size * 0.03f + size * 0.09f * sin(PI.toFloat() * u)) * kotlin.math.cos(u * 7f - 0.5f)
                val y = center + spread * s
                if (i == 0) path.moveTo(0f, y) else path.lineTo(u * size, y)
            }
            paint.strokeWidth = if (line % 2 == 0) 3f else 1.5f
            paint.alpha = if (line % 2 == 0) 230 else 170
            c.drawPath(path, paint)
        }

        // Golden light and notes, as on the player screen (a still image: the system draws this card).
        val dots = Paint(Paint.ANTI_ALIAS_FLAG)
        val rnd = java.util.Random(7)
        val dustColors = intArrayOf(0xFFFFB45A.toInt(), 0xFFF59A5B.toInt(), 0xFFFFD08A.toInt(), 0xFFFFB45A.toInt(), 0xFFC9A2FF.toInt())
        for (i in 0 until 46) {
            val x = rnd.nextFloat() * size
            val y = size * (0.12f + rnd.nextFloat() * 0.76f)
            val color = dustColors[i % dustColors.size]
            val big = i % 9 == 0
            val r = if (big) 14f + rnd.nextFloat() * 12f else 1.6f + rnd.nextFloat() * 2.6f
            val glow = if (big) r * 1.6f else r * 3.5f
            val coreAlpha = if (big) 0x55 else 0xD9
            dots.shader = RadialGradient(x, y, glow, (coreAlpha shl 24) or (color and 0xFFFFFF), color and 0xFFFFFF, Shader.TileMode.CLAMP)
            c.drawCircle(x, y, glow, dots)
            if (!big) {
                dots.shader = null
                dots.color = color
                c.drawCircle(x, y, r, dots)
            }
        }
        dots.shader = null
        val single = androidx.core.graphics.PathParser.createPathFromPathData("M6.2 0.5v10.1a2.9 2.9 0 1 0 1.6 2.6V4.2c1.7.4 3 1.6 3.3 3.2.7-2.9-1.1-5.6-4.9-6.9z")
        val double = androidx.core.graphics.PathParser.createPathFromPathData("M4.5 3.2L14.5 1v10.4a2.6 2.6 0 1 1-1.5-2.3V4.9L6 6.4v7a2.6 2.6 0 1 1-1.5-2.3z")
        val notes = listOf(
            ArtNote(70f, 150f, 3.2f, -12f, false, 0xFFE2C2FF.toInt()),
            ArtNote(210f, 190f, 3.8f, 8f, true, 0xFFFFC27A.toInt()),
            ArtNote(330f, 140f, 2.8f, -6f, false, 0xFFFFD08A.toInt()),
            ArtNote(430f, 205f, 3.4f, 14f, true, 0xFFFFC27A.toInt()),
            ArtNote(130f, 330f, 2.6f, 10f, true, 0xFFE9C4FF.toInt()),
            ArtNote(395f, 345f, 3.0f, -10f, false, 0xFFFFC9A0.toInt()),
        )
        notes.forEach { n ->
            val glyph = android.graphics.Path(if (n.double) double else single)
            val m = android.graphics.Matrix()
            m.postScale(n.scale, n.scale)
            m.postRotate(n.rotation)
            m.postTranslate(n.x, n.y)
            glyph.transform(m)
            dots.color = n.color
            dots.alpha = 200
            dots.setShadowLayer(10f, 0f, 0f, n.color)
            c.drawPath(glyph, dots)
        }
        dots.clearShadowLayer()

        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
        bmp.recycle()
        return out.toByteArray().also { fallbackArt = it }
    }

    private class ArtNote(val x: Float, val y: Float, val scale: Float, val rotation: Float, val double: Boolean, val color: Int)

    companion object {
        private val CMD_REPEAT = SessionCommand("app.onlymusic.REPEAT", Bundle.EMPTY)
        private val CMD_STOP = SessionCommand("app.onlymusic.STOP", Bundle.EMPTY)
        private const val GAP_MS = 1_000L
    }
}
