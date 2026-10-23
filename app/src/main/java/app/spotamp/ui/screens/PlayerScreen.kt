package app.spotamp.ui.screens

import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import app.spotamp.ui.TextPrompt
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import app.spotamp.data.Settings
import app.spotamp.ui.ActionSheet
import app.spotamp.ui.AppActions
import app.spotamp.ui.AppViewModel
import app.spotamp.ui.Ic
import app.spotamp.ui.IconTap
import app.spotamp.ui.Icn
import app.spotamp.ui.LocalAccent
import app.spotamp.ui.MenuAction
import app.spotamp.ui.Pal
import app.spotamp.ui.PlayButton
import app.spotamp.ui.PrimaryButton
import app.spotamp.ui.RaisedButton
import app.spotamp.ui.Screen
import app.spotamp.ui.SecondaryButton
import app.spotamp.ui.TrackRow
import app.spotamp.ui.Txt
import app.spotamp.ui.WaveLayer
import app.spotamp.ui.formatTime
import app.spotamp.ui.rememberPositionMs
import kotlin.math.max
import kotlin.math.sin

@Composable
fun PlayerScreen(vm: AppViewModel, actions: AppActions, settings: Settings) {
    val state by vm.player.state.collectAsStateWithLifecycle()
    val trackMap by vm.library.trackMap.collectAsStateWithLifecycle()
    val tracks by vm.library.tracks.collectAsStateWithLifecycle()
    val folders by vm.library.folders.collectAsStateWithLifecycle()
    val scans by vm.library.scans.collectAsStateWithLifecycle()
    val accent = LocalAccent.current

    val hasQueue = state.queue.isNotEmpty()
    val current = state.currentId?.let { trackMap[it] }
    val position = rememberPositionMs(vm.player, state.isPlaying)
    val duration = current?.durationMs?.takeIf { it > 0 } ?: state.durationMs
    var menuFor by remember { mutableIntStateOf(-1) }
    var queueMenu by remember { mutableStateOf(false) }
    var scrub by remember { mutableStateOf<Float?>(null) }
    var savingQueue by remember { mutableStateOf(false) }
    var infoFor by remember { mutableStateOf<app.spotamp.data.Track?>(null) }
    val favorites by vm.library.favorites.collectAsStateWithLifecycle()

    val density = LocalDensity.current
    val waveTop = WindowInsets.statusBars.getTop(density) + with(density) { 48.dp.toPx() }
    val panelShape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)

    Column(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxWidth()
                .shadow(18.dp, panelShape, ambientColor = Color.Black, spotColor = Color.Black)
                .clip(panelShape)
                .drawBehind {
                    drawRect(Brush.linearGradient(listOf(Color(0xFF1C1030), Color(0xFF220E26), Color(0xFF2A0F1F)), Offset.Zero, Offset(size.width * 0.3f, size.height)))
                    drawRect(Brush.radialGradient(listOf(Color(0x617846E6), Color.Transparent), Offset(size.width * 0.18f, size.height * 0.38f), size.width * 0.7f))
                    drawRect(Brush.radialGradient(listOf(Color(0x33F28C4B), Color.Transparent), Offset(size.width * 0.88f, size.height * 0.3f), size.width * 0.6f))
                    drawRect(Brush.radialGradient(listOf(Color(0x59961E50), Color.Transparent), Offset(size.width * 0.7f, size.height * 0.85f), size.width * 0.8f))
                }
        ) {
            WaveLayer(Modifier.matchParentSize(), waveTop, state.isPlaying, !hasQueue, settings.waveStyle)
            Column {
                Spacer(Modifier.statusBarsPadding())
                Row(
                    Modifier.fillMaxWidth().height(48.dp).padding(start = 8.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GlassIcon(Ic.Hamburger, "Menu") { actions.openMenu() }
                    Spacer(Modifier.width(10.dp))
                    Txt("Spot", size = 17.sp, weight = FontWeight.SemiBold)
                    Txt("Amp", size = 17.sp, weight = FontWeight.SemiBold, brush = Brush.horizontalGradient(listOf(Color(0xFFB794FF), Pal.Amber)))
                    Spacer(Modifier.weight(1f))
                    GlassIcon(Ic.Search, "Search") { actions.go(Screen.Search) }
                    GlassIcon(Ic.Library, "Library") { actions.go(Screen.Library()) }
                    GlassIcon(Ic.Playlists, "Playlists") { actions.go(Screen.Playlists) }
                    GlassIcon(Ic.FolderAdd, "Add music folder") { actions.addFolder() }
                }
                // The wave band; with the wave off the player moves up and the list gets the room.
                if (settings.waveStyle != 2) Spacer(Modifier.height(92.dp))
                val scrubbing = scrub
                // While the seek bar is dragged, a big target time replaces the title, away from the finger.
                if (scrubbing != null) {
                    Box(Modifier.fillMaxWidth().height(41.dp), contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Txt(formatTime((scrubbing * duration).toLong()), size = 30.sp, weight = FontWeight.SemiBold, brush = Brush.horizontalGradient(listOf(Color(0xFFF4ECFF), Pal.Gold)))
                            Txt("  / " + formatTime(duration), Modifier.padding(bottom = 4.dp), size = 15.sp, color = Pal.Text2)
                        }
                    }
                } else Column(Modifier.padding(horizontal = 22.dp)) {
                    Txt(current?.title ?: "Nothing playing", size = 18.sp, weight = FontWeight.SemiBold, color = if (current != null) Pal.Text else Pal.Text2)
                    Row {
                        val idle = when {
                            folders.isEmpty() -> "Add a music folder to start"
                            scans.isNotEmpty() -> "Reading your music..."
                            else -> "Pick a track to play"
                        }
                        Txt(current?.artist ?: idle, Modifier.weight(1f), size = 13.sp, color = Pal.Text2)
                        if (current != null) Txt(current.format, Modifier.padding(start = 8.dp), size = 13.sp, color = Pal.Text3)
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val shownMs = scrub?.let { (it * duration).toLong() } ?: position
                    Txt(if (hasQueue) formatTime(shownMs) else "-:--", Modifier.width(42.dp), size = 12.sp, color = if (hasQueue) Pal.Text else Pal.Disabled)
                    SeekBar(
                        fraction = if (duration > 0) position.toFloat() / duration else 0f,
                        enabled = hasQueue && duration > 0,
                        onSeek = { vm.player.seekTo((it * duration).toLong()) },
                        modifier = Modifier.weight(1f),
                        onScrub = { scrub = it },
                    )
                    Txt(if (hasQueue) formatTime(duration) else "-:--", Modifier.width(42.dp), size = 12.sp, color = Pal.Text3, align = androidx.compose.ui.text.style.TextAlign.End)
                }
                Row(
                    Modifier.fillMaxWidth().height(96.dp).padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RaisedButton(Ic.Shuffle, "Shuffle", { vm.player.toggleShuffle() }, size = 46.dp, iconSize = 19.dp, tint = if (state.shuffle) Pal.Gold else Pal.Text2, glow = state.shuffle, enabled = hasQueue)
                    RaisedButton(Ic.Prev, "Previous track", { vm.player.previous() }, size = 52.dp, iconSize = 22.dp, enabled = hasQueue)
                    PlayButton(state.isPlaying, hasQueue, { vm.player.togglePlay() })
                    RaisedButton(Ic.Next, "Next track", { vm.player.next() }, size = 52.dp, iconSize = 22.dp, enabled = hasQueue)
                    RaisedButton(Ic.Stop, "Stop", { vm.player.stop() }, size = 46.dp, iconSize = 16.dp, enabled = hasQueue)
                    Box {
                        val on = state.repeat != Player.REPEAT_MODE_OFF
                        val label = when (state.repeat) {
                            Player.REPEAT_MODE_ALL -> "Repeat all"
                            Player.REPEAT_MODE_ONE -> "Repeat one"
                            else -> "Repeat off"
                        }
                        RaisedButton(Ic.Repeat, label, { vm.player.cycleRepeat() }, size = 46.dp, iconSize = 19.dp, tint = if (on) Pal.Gold else Pal.Text2, glow = on, enabled = hasQueue)
                        if (state.repeat == Player.REPEAT_MODE_ONE) {
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .size(15.dp)
                                    .background(Brush.linearGradient(listOf(Pal.Amber, Color(0xFFC2456F))), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) { Txt("1", size = 9.sp, color = Color.White, weight = FontWeight.SemiBold) }
                        }
                    }
                }
            }
        }

        if (hasQueue || (folders.isNotEmpty() && scans.isNotEmpty())) {
            val total = remember(state.queue, trackMap) { state.queue.sumOf { trackMap[it]?.durationMs ?: 0L } }
            Row(
                Modifier.fillMaxWidth().height(56.dp).padding(start = 22.dp, end = 8.dp, top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Txt(state.queueName.ifEmpty { "Library" }, size = 15.sp, weight = FontWeight.SemiBold)
                    Txt("${state.queue.size} tracks · ${formatTime(total)}", size = 12.sp, color = Pal.Text3)
                }
                if (hasQueue) IconTap(Ic.More, "Playlist options", { queueMenu = true }, tint = Pal.Text2)
            }
            if (scans.isNotEmpty()) ScanProgressBar(scans.values.sumOf { it.done }, scans.values.sumOf { it.total })
            val listState = rememberLazyListState()
            LaunchedEffect(state.index) {
                if (state.index >= 0) listState.animateScrollToItem(max(0, state.index - 2))
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            ) {
                itemsIndexed(state.queue) { i, id ->
                    val t = trackMap[id]
                    val isCurrent = i == state.index
                    TrackRow(
                        title = t?.title ?: fileNameOf(id),
                        subtitle = t?.artist ?: "",
                        duration = t?.let { formatTime(it.durationMs) } ?: "",
                        current = isCurrent,
                        onClick = { vm.player.skipTo(i) },
                        onMore = { menuFor = i },
                        height = 48.dp,
                        leading = {
                            when {
                                isCurrent && state.isPlaying -> PlayingIndicator()
                                isCurrent -> Icn(Ic.Pause, accent.text, 12.dp)
                                else -> Txt("${i + 1}", size = 12.sp, color = Pal.Text3)
                            }
                        },
                    )
                }
            }
        } else {
            EmptyQueue(
                hasTracks = tracks.isNotEmpty(),
                onAddFolder = actions.addFolder,
                onPlayAll = {
                    val ids = tracks.sortedBy { it.title.lowercase() }.map { it.id }
                    vm.player.playQueue(ids, 0, "Library")
                },
                onOpenLibrary = { actions.go(Screen.Library()) },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.navigationBarsPadding())
    }

    val menuId = state.queue.getOrNull(menuFor)
    if (menuId != null) {
        val t = trackMap[menuId]
        ActionSheet(
            title = t?.title ?: fileNameOf(menuId),
            subtitle = t?.let { "${it.artist} · ${formatTime(it.durationMs)} · ${it.format}" } ?: "",
            actions = listOfNotNull(
                MenuAction("Play next", Ic.PlayNext) { vm.player.moveToNext(menuFor) },
                MenuAction("Add to playlist", Ic.AddToList) { actions.addToPlaylist(listOf(menuId)) },
                if (t != null) {
                    val fav = menuId in favorites
                    MenuAction(if (fav) "Remove from favorites" else "Add to favorites", if (fav) Ic.HeartFilled else Ic.Heart) { vm.library.toggleFavorite(menuId) }
                } else null,
                if (t != null) MenuAction("Track info", Ic.Info) { infoFor = t } else null,
                if (t != null) MenuAction("Show folder", Ic.Folder) { actions.go(Screen.Library(tab = 3, group = t.folderId)) } else null,
                MenuAction("Remove from playlist", Ic.Minus) { vm.player.removeAt(menuFor) },
            ),
            onDismiss = { menuFor = -1 },
        )
    }
    if (queueMenu) {
        ActionSheet(
            title = state.queueName.ifEmpty { "Queue" },
            subtitle = "${state.queue.size} tracks",
            actions = listOf(
                MenuAction("Save as playlist", Ic.Save) { savingQueue = true },
                MenuAction("Clear playlist", Ic.Trash) { vm.player.clearQueue() },
            ),
            onDismiss = { queueMenu = false },
        )
    }
    if (savingQueue) {
        TextPrompt(
            title = "Save as playlist",
            initial = state.queueName,
            confirm = "Save",
            onConfirm = { name ->
                vm.library.createPlaylist(name, state.queue)
                savingQueue = false
            },
            onDismiss = { savingQueue = false },
        )
    }
    infoFor?.let { t -> TrackInfoSheet(vm, t) { infoFor = null } }
}

@Composable
private fun GlassIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .padding(start = 6.dp)
            .size(40.dp)
            .shadow(6.dp, CircleShape, ambientColor = Color.Black, spotColor = Color.Black)
            .background(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.02f))), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        IconTap(icon, label, onClick, tint = Color(0xFFE2D8EA), size = 40.dp, iconSize = 18.dp)
    }
}

@Composable
private fun SeekBar(
    fraction: Float,
    enabled: Boolean,
    onSeek: (Float) -> Unit,
    modifier: Modifier,
    onScrub: (Float?) -> Unit = {},
) {
    val accent = LocalAccent.current
    var dragging by remember { mutableFloatStateOf(-1f) }
    // The gesture handlers below live across recompositions; always seek with the current track's length.
    val seek by rememberUpdatedState(onSeek)
    val scrub by rememberUpdatedState(onScrub)
    val shown = (if (dragging >= 0f) dragging else fraction).coerceIn(0f, 1f)
    Box(
        modifier
            .height(32.dp)
            .padding(horizontal = 6.dp)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures { seek((it.x / size.width).coerceIn(0f, 1f)) }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = {
                        dragging = (it.x / size.width).coerceIn(0f, 1f)
                        scrub(dragging)
                    },
                    onDragEnd = {
                        seek(dragging)
                        dragging = -1f
                        scrub(null)
                    },
                    onDragCancel = {
                        dragging = -1f
                        scrub(null)
                    },
                ) { change, _ ->
                    dragging = (change.position.x / size.width).coerceIn(0f, 1f)
                    scrub(dragging)
                }
            }
            .drawBehind {
                val cy = size.height / 2f
                val stroke = 4.dp.toPx()
                drawLine(Color(0x59000000), Offset(0f, cy), Offset(size.width, cy), stroke, StrokeCap.Round)
                val x = size.width * shown
                if (enabled) {
                    drawLine(
                        Brush.horizontalGradient(accent.colors.let { if (it.size == 1) it + it else it }, 0f, size.width),
                        Offset(0f, cy), Offset(max(x, 0.1f), cy), stroke, StrokeCap.Round,
                    )
                    val r = (if (dragging >= 0f) 12 else 9).dp.toPx()
                    drawCircle(Color(0x29F28C4B), r + 5.dp.toPx(), Offset(x, cy))
                    drawCircle(Color(0x8C000000), r, Offset(x, cy + 2.dp.toPx()))
                    drawCircle(
                        Brush.radialGradient(listOf(Color.White, Color(0xFFF3E3EC), Color(0xFFD9C2D2)), Offset(x - r * 0.3f, cy - r * 0.4f), r * 1.4f),
                        r, Offset(x, cy),
                    )
                }
            }
    )
}

/** Thin bar under the list name while folders are being read. */
@Composable
private fun ScanProgressBar(done: Int, total: Int) {
    val accent = LocalAccent.current
    Row(
        Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .weight(1f)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Pal.Line)
        ) {
            if (total > 0) {
                Box(
                    Modifier
                        .fillMaxWidth(done.toFloat() / total)
                        .height(3.dp)
                        .background(Brush.horizontalGradient(accent.brushColors))
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Txt(if (total == 0) "Looking for music files..." else "Reading $done / $total", size = 12.sp, color = accent.text)
    }
}

/** Small animated wave next to the playing track. */
@Composable
private fun PlayingIndicator() {
    val accent = LocalAccent.current
    val phase by rememberInfiniteTransition(label = "eq").animateFloat(
        initialValue = 0f, targetValue = 6.283f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "eqPhase",
    )
    Canvas(Modifier.size(18.dp, 12.dp)) {
        val p = Path()
        for (k in 0..15) {
            val x = size.width * k / 15f
            val y = size.height / 2f + size.height * 0.42f * sin(k * 0.9f + phase) * sin(k / 15f * Math.PI.toFloat())
            if (k == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        drawPath(p, accent.text, style = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
private fun EmptyQueue(
    hasTracks: Boolean,
    onAddFolder: () -> Unit,
    onPlayAll: () -> Unit,
    onOpenLibrary: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        when {
            hasTracks -> {
                Txt("Nothing in the queue", size = 22.sp, weight = FontWeight.SemiBold)
                Txt("Play your whole library, or pick an album, artist or playlist.", size = 14.sp, color = Pal.Text2, maxLines = 3, lineHeight = 21.sp)
                PrimaryButton("Play whole library", onPlayAll, Modifier.fillMaxWidth().padding(top = 8.dp), height = 54.dp, icon = Ic.Play)
                SecondaryButton("Open library", onOpenLibrary, Modifier.fillMaxWidth(), height = 46.dp)
            }
            else -> {
                Box(
                    Modifier
                        .size(52.dp)
                        .shadow(10.dp, RoundedCornerShape(16.dp), ambientColor = Color.Black, spotColor = Color.Black)
                        .background(Brush.linearGradient(listOf(Pal.ButtonTop, Pal.ButtonBottom)), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) { Icn(Ic.FolderAdd, Pal.Highlight, 24.dp) }
                Txt("Your library is empty", size = 22.sp, weight = FontWeight.SemiBold)
                Txt(
                    "Pick a folder with music on this phone. SpotAmp reads the files from it into your library. You can add more folders at any time.",
                    size = 14.sp, color = Pal.Text2, maxLines = 4, lineHeight = 21.sp,
                )
                PrimaryButton("Add music folder", onAddFolder, Modifier.fillMaxWidth().padding(top = 8.dp), height = 54.dp, icon = Ic.FolderAdd)
                Txt("No account, no ads, no tracking. Works fully offline.", Modifier.padding(top = 12.dp), size = 12.sp, color = Pal.Text3, maxLines = 2)
            }
        }
    }
}

fun fileNameOf(id: String): String =
    Uri.decode(id).substringAfterLast('/').substringAfterLast(':').ifEmpty { "Unknown file" }
