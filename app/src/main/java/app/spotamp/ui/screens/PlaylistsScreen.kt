package app.spotamp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.spotamp.data.Playlist
import app.spotamp.data.Track
import app.spotamp.ui.ActionSheet
import app.spotamp.ui.AppActions
import app.spotamp.ui.AppViewModel
import app.spotamp.ui.DragHandle
import app.spotamp.ui.Ic
import app.spotamp.ui.IconTap
import app.spotamp.ui.Icn
import app.spotamp.ui.LocalAccent
import app.spotamp.ui.MenuAction
import app.spotamp.ui.MiniPlayerBar
import app.spotamp.ui.Pal
import app.spotamp.ui.PrimaryButton
import app.spotamp.ui.Screen
import app.spotamp.ui.ScreenHeader
import app.spotamp.ui.SectionLabel
import app.spotamp.ui.TextPrompt
import app.spotamp.ui.TrackRow
import app.spotamp.ui.Txt
import app.spotamp.ui.dragHandle
import app.spotamp.ui.formatTime
import app.spotamp.ui.rememberReorderState
import app.spotamp.ui.reorderable
import app.spotamp.ui.subtitle
import app.spotamp.ui.textStyle
import kotlinx.coroutines.delay

const val AUTO_RECENT = "auto:recent"
const val AUTO_FAVORITES = "auto:favorites"
private const val RECENT_DAYS = 30

/** Tracks of a playlist, or of one of the automatic lists. */
private fun autoTracks(id: String, tracks: List<Track>, favorites: Set<String>): List<Track>? = when (id) {
    AUTO_RECENT -> {
        val since = System.currentTimeMillis() - RECENT_DAYS * 24L * 3600 * 1000
        tracks.filter { it.added >= since }.sortedByDescending { it.added }
    }
    AUTO_FAVORITES -> tracks.filter { it.id in favorites }.sortedBy { it.title.lowercase() }
    else -> null
}

@Composable
fun PlaylistsScreen(vm: AppViewModel, actions: AppActions) {
    val playlists by vm.library.playlists.collectAsStateWithLifecycle()
    val trackMap by vm.library.trackMap.collectAsStateWithLifecycle()
    val tracks by vm.library.tracks.collectAsStateWithLifecycle()
    val favorites by vm.library.favorites.collectAsStateWithLifecycle()
    val state by vm.player.state.collectAsStateWithLifecycle()
    val accent = LocalAccent.current
    var creating by remember { mutableStateOf(false) }
    var menuFor by remember { mutableIntStateOf(-1) }
    var renamingId by remember { mutableStateOf<String?>(null) }
    var deleted by remember { mutableStateOf<Pair<Playlist, Int>?>(null) }
    val reorder = rememberReorderState(64.dp)

    LaunchedEffect(deleted) {
        if (deleted != null) {
            delay(5_000)
            deleted = null
        }
    }
    val recent = remember(tracks) { autoTracks(AUTO_RECENT, tracks, favorites)!! }
    val favs = remember(tracks, favorites) { autoTracks(AUTO_FAVORITES, tracks, favorites)!! }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Playlists", "${playlists.size} playlists, saved on this phone", actions.back) {
            PrimaryButton("New", { creating = true }, height = 36.dp, icon = Ic.Plus)
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
            if (playlists.isNotEmpty()) {
                item { SectionLabel("Hold and drag to reorder") }
            }
            itemsIndexed(playlists, key = { _, p -> p.id }) { i, p ->
                if (p.id == renamingId) {
                    RenameRow(
                        initial = p.name,
                        onSave = { name ->
                            vm.library.renamePlaylist(p.id, name)
                            renamingId = null
                        },
                        onCancel = { renamingId = null },
                    )
                    return@itemsIndexed
                }
                val duration = p.trackIds.sumOf { trackMap[it]?.durationMs ?: 0L }
                val playing = state.queueName == p.name && state.isPlaying
                val dragging = reorder.dragging == i
                Row(
                    Modifier
                        .reorderable(reorder, i, playlists.size, accent.text)
                        .fillMaxWidth()
                        .height(64.dp)
                        .then(
                            when {
                                dragging -> Modifier
                                    .shadow(14.dp, RoundedCornerShape(14.dp))
                                    .background(Color(0xFF2A1D30), RoundedCornerShape(14.dp))
                                playing -> Modifier
                                    .shadow(6.dp, RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.horizontalGradient(listOf(Color(0x389B6CFF), Color(0x29A8326E), Color(0x1FF28C4B))),
                                        RoundedCornerShape(14.dp),
                                    )
                                else -> Modifier
                            }
                        )
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { actions.go(Screen.PlaylistDetail(p.id)) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DragHandle(
                        Modifier.dragHandle(reorder, i, playlists.size) { from, to -> vm.library.movePlaylist(from, to) },
                        active = dragging,
                    )
                    Column(Modifier.weight(1f)) {
                        Txt(p.name, size = 15.sp, weight = if (playing) FontWeight.SemiBold else FontWeight.Normal)
                        Txt(
                            (if (playing) "Playing · " else "") + "${p.trackIds.size} tracks · ${formatTime(duration)}",
                            size = 12.sp, color = if (playing) accent.text else Pal.Text3,
                        )
                    }
                    IconTap(Ic.More, "Options for ${p.name}", { menuFor = i }, tint = Pal.Text3)
                }
            }
            item { SectionLabel("Automatic", Modifier.padding(top = 12.dp)) }
            item {
                AutoRow(Ic.Clock, "Recently added", "Last $RECENT_DAYS days · ${recent.size} tracks") {
                    actions.go(Screen.PlaylistDetail(AUTO_RECENT))
                }
            }
            item {
                AutoRow(Ic.Heart, "Favorites", "${favs.size} tracks") {
                    actions.go(Screen.PlaylistDetail(AUTO_FAVORITES))
                }
            }
        }

        deleted?.let { (p, index) ->
            Row(
                Modifier
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .fillMaxWidth()
                    .height(52.dp)
                    .shadow(10.dp, RoundedCornerShape(14.dp))
                    .background(Pal.Raised, RoundedCornerShape(14.dp))
                    .padding(start = 16.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Txt("Playlist “${p.name}” deleted", Modifier.weight(1f), size = 14.sp)
                Box(
                    Modifier.clip(RoundedCornerShape(10.dp)).clickable {
                        vm.library.restorePlaylist(p, index)
                        deleted = null
                    }.padding(horizontal = 14.dp, vertical = 10.dp)
                ) { Txt("Undo", size = 14.sp, weight = FontWeight.SemiBold, color = accent.text) }
            }
        }
        MiniPlayerBar(vm, actions)
        Spacer(Modifier.navigationBarsPadding())
    }

    playlists.getOrNull(menuFor)?.let { p ->
        val index = menuFor
        ActionSheet(
            title = p.name,
            subtitle = "${p.trackIds.size} tracks",
            actions = listOf(
                MenuAction("Play", Ic.Play) { vm.player.playQueue(p.trackIds.filter { it in trackMap }, 0, p.name) },
                MenuAction("Rename", Ic.Edit) { renamingId = p.id },
                MenuAction("Delete", Ic.Trash) {
                    vm.library.deletePlaylist(p.id)
                    deleted = p to index
                },
            ),
            onDismiss = { menuFor = -1 },
        )
    }
    if (creating) {
        TextPrompt("New playlist", "", "Create", { name -> vm.library.createPlaylist(name); creating = false }, { creating = false })
    }
}

/** Rename in place, as in the design: text field, cancel and save. */
@Composable
private fun RenameRow(initial: String, onSave: (String) -> Unit, onCancel: () -> Unit) {
    val accent = LocalAccent.current
    var value by remember { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Row(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(Pal.Card, RoundedCornerShape(14.dp))
            .border(1.dp, Pal.Line, RoundedCornerShape(14.dp))
            .padding(start = 12.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = value,
            onValueChange = { value = it },
            singleLine = true,
            textStyle = textStyle(15.sp),
            cursorBrush = SolidColor(accent.text),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focus)
                .background(Pal.Bg, RoundedCornerShape(10.dp))
                .border(1.dp, accent.text, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
        )
        IconTap(Ic.Close, "Cancel rename", onCancel, tint = Pal.Text2)
        Box(
            Modifier
                .size(44.dp)
                .shadow(8.dp, RoundedCornerShape(12.dp), ambientColor = Pal.Burgundy, spotColor = Pal.Burgundy)
                .background(Brush.linearGradient(accent.brushColors), RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .clickable { onSave(value) },
            contentAlignment = Alignment.Center,
        ) { Icn(Ic.Check, Color.White, 18.dp) }
    }
}

@Composable
private fun AutoRow(icon: ImageVector, title: String, detail: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).background(Pal.Raised, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
            Icn(icon, Pal.Text2, 18.dp)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Txt(title, size = 15.sp)
            Txt(detail, size = 12.sp, color = Pal.Text3)
        }
    }
}

@Composable
fun PlaylistDetailScreen(vm: AppViewModel, actions: AppActions, id: String) {
    val playlists by vm.library.playlists.collectAsStateWithLifecycle()
    val trackMap by vm.library.trackMap.collectAsStateWithLifecycle()
    val tracks by vm.library.tracks.collectAsStateWithLifecycle()
    val favorites by vm.library.favorites.collectAsStateWithLifecycle()
    val state by vm.player.state.collectAsStateWithLifecycle()
    val accent = LocalAccent.current
    var menuFor by remember { mutableIntStateOf(-1) }
    var infoFor by remember { mutableStateOf<Track?>(null) }
    val reorder = rememberReorderState(52.dp)

    val auto = autoTracks(id, tracks, favorites)
    val p = playlists.firstOrNull { it.id == id }
    val name = when (id) {
        AUTO_RECENT -> "Recently added"
        AUTO_FAVORITES -> "Favorites"
        else -> p?.name
    }
    if (name == null) {
        LaunchedEffect(Unit) { actions.back() }
        return
    }
    val trackIds = auto?.map { it.id } ?: p!!.trackIds
    val editable = auto == null
    val playable = trackIds.filter { it in trackMap }
    val duration = playable.sumOf { trackMap[it]?.durationMs ?: 0L }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(name, "${trackIds.size} tracks · ${formatTime(duration)}", actions.back)
        Row(
            Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (editable && trackIds.isNotEmpty()) {
                Txt("Hold and drag to reorder", Modifier.weight(1f).padding(start = 8.dp), size = 12.sp, color = Pal.Text3)
            }
            IconTap(Ic.Shuffle, "Shuffle", {
                if (playable.isNotEmpty()) vm.player.playQueue(playable, playable.indices.random(), name, shuffle = true)
            })
            PrimaryButton("Play", { vm.player.playQueue(playable, 0, name) }, height = 36.dp, icon = Ic.Play)
        }
        if (trackIds.isEmpty()) {
            Column(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Txt(
                    when (id) {
                        AUTO_FAVORITES -> "No favorites yet. Use Add to favorites on any track."
                        AUTO_RECENT -> "Nothing added in the last $RECENT_DAYS days."
                        else -> "This playlist is empty. Use Add to playlist on any track in the library."
                    },
                    size = 14.sp, color = Pal.Text2, maxLines = 3, lineHeight = 21.sp,
                )
            }
        } else {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                itemsIndexed(trackIds, key = { i, tid -> "$i:$tid" }) { i, trackId ->
                    val t = trackMap[trackId]
                    val handle: (@Composable () -> Unit)? = if (editable) ({
                        DragHandle(
                            Modifier.dragHandle(reorder, i, trackIds.size) { from, to -> vm.library.moveInPlaylist(id, from, to) },
                            active = reorder.dragging == i,
                        )
                    }) else null
                    TrackRow(
                        title = t?.title ?: fileNameOf(trackId),
                        subtitle = t?.subtitle() ?: "File not in library",
                        duration = t?.let { formatTime(it.durationMs) } ?: "",
                        current = state.queueName == name && trackId == state.currentId,
                        onClick = {
                            if (t != null) vm.player.playQueue(playable, playable.indexOf(trackId).coerceAtLeast(0), name)
                        },
                        onMore = { menuFor = i },
                        leading = handle,
                        modifier = if (editable) Modifier.reorderable(reorder, i, trackIds.size, accent.text) else Modifier,
                    )
                }
            }
        }
        MiniPlayerBar(vm, actions)
        Spacer(Modifier.navigationBarsPadding())
    }

    trackIds.getOrNull(menuFor)?.let { trackId ->
        val index = menuFor
        val t = trackMap[trackId]
        if (t != null) {
            TrackMenu(
                vm, actions, t,
                onInfo = { infoFor = t },
                extra = if (editable) listOf(MenuAction("Remove from playlist", Ic.Minus) { vm.library.removeFromPlaylist(id, index) }) else emptyList(),
            ) { menuFor = -1 }
        } else if (editable) {
            ActionSheet(
                title = fileNameOf(trackId),
                subtitle = "File not in library",
                actions = listOf(MenuAction("Remove from playlist", Ic.Minus) { vm.library.removeFromPlaylist(id, index) }),
                onDismiss = { menuFor = -1 },
            )
        } else {
            menuFor = -1
        }
    }
    infoFor?.let { t -> TrackInfoSheet(vm, t) { infoFor = null } }
}
