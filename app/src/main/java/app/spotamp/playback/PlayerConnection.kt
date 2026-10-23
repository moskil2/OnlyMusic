package app.spotamp.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import app.spotamp.data.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch

/** The screen's link to [PlaybackService]. All playback goes through the media session. */
class PlayerConnection(context: Context, scope: CoroutineScope, private val prefs: Prefs) {

    data class State(
        val connected: Boolean = false,
        val queue: List<String> = emptyList(),
        val queueName: String = "",
        val index: Int = -1,
        val isPlaying: Boolean = false,
        val shuffle: Boolean = false,
        val repeat: Int = Player.REPEAT_MODE_OFF,
        val durationMs: Long = 0L,
    ) {
        val currentId: String? get() = queue.getOrNull(index)
    }

    private val app = context.applicationContext
    private var controller: MediaController? = null
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            refresh(player, events.contains(Player.EVENT_TIMELINE_CHANGED))
        }
    }

    init {
        scope.launch {
            runCatching {
                val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
                val c = MediaController.Builder(app, token).buildAsync().await()
                controller = c
                c.addListener(listener)
                refresh(c, true)
                if (pendingIds.isNotEmpty()) {
                    val ids = pendingIds.toList()
                    pendingIds.clear()
                    appendNewTracks(ids, pendingName)
                }
            }
        }
    }

    private fun refresh(p: Player, queueChanged: Boolean) {
        val old = _state.value
        val queue = if (queueChanged || !old.connected) List(p.mediaItemCount) { p.getMediaItemAt(it).mediaId } else old.queue
        _state.value = State(
            connected = true,
            queue = queue,
            queueName = prefs.queueName,
            index = if (p.mediaItemCount == 0) -1 else p.currentMediaItemIndex,
            isPlaying = p.isPlaying,
            shuffle = p.shuffleModeEnabled,
            repeat = p.repeatMode,
            durationMs = p.duration.takeIf { it > 0 } ?: 0L,
        )
    }

    fun positionMs(): Long = controller?.currentPosition ?: 0L

    fun playQueue(ids: List<String>, start: Int, name: String, shuffle: Boolean = false) {
        val c = controller ?: return
        if (ids.isEmpty()) return
        prefs.queueName = name
        c.shuffleModeEnabled = shuffle
        c.setMediaItems(ids.map { MediaItem.Builder().setMediaId(it).build() }, start.coerceIn(0, ids.lastIndex), 0L)
        c.prepare()
        c.play()
    }

    fun togglePlay() {
        val c = controller ?: return
        if (c.isPlaying) {
            c.pause()
            return
        }
        if (c.playbackState == Player.STATE_IDLE) c.prepare()
        if (c.playbackState == Player.STATE_ENDED) c.seekTo(c.currentMediaItemIndex, 0L)
        c.play()
    }

    /** Winamp-style stop: pause and go back to the start of the track. */
    fun stop() {
        val c = controller ?: return
        c.pause()
        c.seekTo(0L)
    }

    fun next() {
        controller?.seekToNextMediaItem()
    }

    fun previous() {
        val c = controller ?: return
        if (c.currentPosition > 3_000) c.seekTo(0L) else c.seekToPreviousMediaItem()
    }

    fun seekTo(ms: Long) {
        controller?.seekTo(ms)
    }

    fun skipTo(index: Int) {
        val c = controller ?: return
        if (index !in 0 until c.mediaItemCount) return
        c.seekTo(index, 0L)
        if (c.playbackState == Player.STATE_IDLE) c.prepare()
        c.play()
    }

    fun toggleShuffle() {
        controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

    fun cycleRepeat() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun playNext(ids: List<String>) {
        val c = controller ?: return
        val items = ids.map { MediaItem.Builder().setMediaId(it).build() }
        if (c.mediaItemCount == 0) {
            playQueue(ids, 0, "Queue")
            return
        }
        c.addMediaItems((c.currentMediaItemIndex + 1).coerceAtMost(c.mediaItemCount), items)
    }

    private val pendingIds = ArrayList<String>()
    private var pendingName = ""

    /**
     * Newly read tracks go to the end of the main list, without starting playback.
     * Before the player is connected they wait and are added on connect.
     */
    fun appendNewTracks(ids: List<String>, name: String) {
        val c = controller
        if (c == null) {
            pendingIds += ids
            pendingName = name
            return
        }
        val items = ids.map { MediaItem.Builder().setMediaId(it).build() }
        if (c.mediaItemCount == 0) {
            prefs.queueName = name
            c.setMediaItems(items, 0, 0L)
            c.prepare()
        } else {
            c.addMediaItems(items)
        }
    }

    /** Appends tracks to the end of the queue (the search screen's add button). */
    fun addToQueue(ids: List<String>) {
        val c = controller ?: return
        if (c.mediaItemCount == 0) {
            playQueue(ids, 0, "Queue")
            return
        }
        c.addMediaItems(ids.map { MediaItem.Builder().setMediaId(it).build() })
    }

    /** Moves a queued track right after the current one. */
    fun moveToNext(index: Int) {
        val c = controller ?: return
        val current = c.currentMediaItemIndex
        if (index !in 0 until c.mediaItemCount || index == current) return
        val target = if (index < current) current else current + 1
        c.moveMediaItem(index, target)
    }

    fun clearQueue() {
        controller?.clearMediaItems()
    }

    fun removeAt(index: Int) {
        val c = controller ?: return
        if (index in 0 until c.mediaItemCount) c.removeMediaItem(index)
    }

    /** "Close app": stop the music for good; the queue stays saved for next time. */
    fun shutdown() {
        controller?.let {
            it.pause()
            it.stop()
        }
    }

    fun release() {
        controller?.removeListener(listener)
        controller?.release()
        controller = null
    }
}
