package app.onlymusic.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.onlymusic.data.Track
import app.onlymusic.ui.AppActions
import app.onlymusic.ui.AppViewModel
import app.onlymusic.ui.Chip
import app.onlymusic.ui.Ic
import app.onlymusic.ui.IconTap
import app.onlymusic.ui.Icn
import app.onlymusic.ui.LocalAccent
import app.onlymusic.ui.ActionSheet
import app.onlymusic.ui.MenuAction
import app.onlymusic.ui.MiniPlayerBar
import app.onlymusic.ui.Pal
import app.onlymusic.ui.PrimaryButton
import app.onlymusic.ui.Screen
import app.onlymusic.ui.ScreenHeader
import app.onlymusic.ui.TrackRow
import app.onlymusic.ui.Txt
import app.onlymusic.ui.formatTime
import app.onlymusic.ui.subtitle
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.Normalizer
import java.util.Date

private val SORTS = listOf("Title A-Z", "Artist A-Z", "Recently added", "Longest first")
private val TABS = listOf("Tracks", "Artists", "Albums", "Folders")
private const val RAIL = "#ABCDEFGHIJKLMNOPQRSTUVWXYZ"

private class Group(val key: String, val name: String, val detail: String, val tracks: List<Track>)

private sealed interface Item {
    class Header(val letter: Char) : Item
    class Entry(val track: Track, val index: Int) : Item
}

/** First letter for the A-Z index; Polish letters fall under their base letter, digits under #. */
private fun indexLetter(s: String): Char {
    val c = s.trimStart().firstOrNull()?.uppercaseChar() ?: return '#'
    if (c == '\u0141') return 'L' // Ł has no decomposed form
    val base = Normalizer.normalize(c.toString(), Normalizer.Form.NFD).first()
    return if (base in 'A'..'Z') base else '#'
}

@Composable
fun LibraryScreen(vm: AppViewModel, actions: AppActions, screen: Screen.Library) {
    val tracks by vm.library.tracks.collectAsStateWithLifecycle()
    val folders by vm.library.folders.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(screen.tab) }
    var sort by rememberSaveable { mutableIntStateOf(0) }
    var openKey by rememberSaveable { mutableStateOf(screen.group) }
    var menuFor by remember { mutableStateOf<Track?>(null) }
    var infoFor by remember { mutableStateOf<Track?>(null) }

    val goBack: () -> Unit = {
        if (openKey != null && openKey != screen.group) {
            openKey = null
        } else {
            actions.back()
        }
    }
    BackHandler(enabled = openKey != null && openKey != screen.group) { openKey = null }

    val sorted = remember(tracks, sort) {
        when (sort) {
            1 -> tracks.sortedWith(compareBy({ it.artist.lowercase() }, { it.album.lowercase() }, { it.title.lowercase() }))
            2 -> tracks.sortedByDescending { it.added }
            3 -> tracks.sortedByDescending { it.durationMs }
            else -> tracks.sortedBy { it.title.lowercase() }
        }
    }
    val groups = remember(tracks, folders, tab) {
        val byAlbum = compareBy<Track>({ it.album.lowercase() }, { it.title.lowercase() })
        when (tab) {
            1 -> tracks.groupBy { it.artist }.toSortedMap(String.CASE_INSENSITIVE_ORDER)
                .map { (k, v) -> Group(k, k, "${v.size} tracks", v.sortedWith(byAlbum)) }
            2 -> tracks.filter { it.album.isNotEmpty() }.groupBy { it.album }.toSortedMap(String.CASE_INSENSITIVE_ORDER)
                .map { (k, v) -> Group(k, k, "${v.first().artist} · ${v.size} tracks", v.sortedBy { it.title.lowercase() }) }
            3 -> folders.map { f ->
                val v = tracks.filter { it.folderId == f.id }
                Group(f.id, f.name, "${f.path} · ${v.size} tracks", v.sortedBy { it.title.lowercase() })
            }
            else -> emptyList()
        }
    }
    val open = openKey?.let { key -> groups.firstOrNull { it.key == key } }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = open?.name ?: "Library",
            subtitle = open?.detail ?: "${tracks.size} tracks from ${folders.size} ${if (folders.size == 1) "folder" else "folders"}",
            onBack = goBack,
        ) {
            IconTap(Ic.FolderAdd, "Music folders", { actions.go(Screen.Folders) })
            IconTap(Ic.Search, "Search", { actions.go(Screen.Search) })
        }

        when {
            tracks.isEmpty() -> Column(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            ) {
                Txt("No music yet", size = 20.sp, weight = FontWeight.SemiBold)
                Txt("Add a folder with music and OnlyMusic will read it into your library.", size = 14.sp, color = Pal.Text2, maxLines = 3, lineHeight = 21.sp)
                PrimaryButton("Add music folder", actions.addFolder, Modifier.fillMaxWidth(), height = 52.dp, icon = Ic.FolderAdd)
            }
            open != null -> TrackList(vm, open.tracks, open.name, null, {}, false, { menuFor = it }, Modifier.weight(1f))
            else -> {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TABS.forEachIndexed { i, name -> Chip(name, i == tab) { tab = i } }
                }
                if (tab == 0) {
                    TrackList(vm, sorted, "Library", SORTS[sort], { sort = (sort + 1) % SORTS.size }, sort <= 1, { menuFor = it }, Modifier.weight(1f), byArtist = sort == 1)
                } else {
                    LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                        items(groups, key = { it.key }) { g -> GroupRow(g) { openKey = g.key } }
                    }
                }
            }
        }

        MiniPlayerBar(vm, actions)
        Spacer(Modifier.navigationBarsPadding())
    }

    menuFor?.let { t ->
        TrackMenu(vm, actions, t, onInfo = { infoFor = t }) { menuFor = null }
    }
    infoFor?.let { t -> TrackInfoSheet(vm, t) { infoFor = null } }
}

@Composable
private fun TrackList(
    vm: AppViewModel,
    list: List<Track>,
    queueName: String,
    sortLabel: String?,
    onSort: () -> Unit,
    letters: Boolean,
    onMore: (Track) -> Unit,
    modifier: Modifier,
    byArtist: Boolean = false,
) {
    val state by vm.player.state.collectAsStateWithLifecycle()
    val accent = LocalAccent.current
    val ids = remember(list) { list.map { it.id } }
    val rows = remember(list, letters, byArtist) {
        val out = ArrayList<Item>(list.size + 30)
        var last: Char? = null
        list.forEachIndexed { i, t ->
            if (letters) {
                val l = indexLetter(if (byArtist) t.artist else t.title)
                if (l != last) {
                    out.add(Item.Header(l))
                    last = l
                }
            }
            out.add(Item.Entry(t, i))
        }
        out
    }
    val headerPositions = remember(rows) {
        rows.withIndex().filter { it.value is Item.Header }.associate { (it.value as Item.Header).letter to it.index }
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().height(48.dp).padding(start = 6.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (sortLabel != null) {
                Row(
                    Modifier.clip(RoundedCornerShape(20.dp)).clickable(onClick = onSort).padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icn(Ic.Sort, Pal.Text2, 16.dp)
                    Spacer(Modifier.width(8.dp))
                    Txt(sortLabel, size = 13.sp, color = Pal.Text2)
                }
            }
            Spacer(Modifier.weight(1f))
            IconTap(Ic.Shuffle, "Shuffle all", {
                if (ids.isNotEmpty()) vm.player.playQueue(ids, ids.indices.random(), queueName, shuffle = true)
            })
            PrimaryButton("Play all", { vm.player.playQueue(ids, 0, queueName) }, height = 36.dp, icon = Ic.Play)
        }
        Row(Modifier.weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 10.dp, end = if (letters) 0.dp else 10.dp, top = 4.dp, bottom = 4.dp),
            ) {
                items(rows.size, key = { i -> rows[i].let { if (it is Item.Header) "h${it.letter}" else (it as Item.Entry).track.id } }) { i ->
                    when (val item = rows[i]) {
                        is Item.Header -> Txt(
                            item.letter.toString(),
                            Modifier.padding(start = 12.dp, top = 10.dp, bottom = 2.dp),
                            size = 12.sp, weight = FontWeight.SemiBold, color = accent.text,
                        )
                        is Item.Entry -> TrackRow(
                            title = item.track.title,
                            subtitle = item.track.subtitle(),
                            duration = formatTime(item.track.durationMs),
                            current = item.track.id == state.currentId,
                            onClick = { vm.player.playQueue(ids, item.index, queueName) },
                            onMore = { onMore(item.track) },
                            height = 50.dp,
                        )
                    }
                }
            }
            if (letters) {
                LetterRail(Modifier.fillMaxHeight().width(22.dp)) { letter ->
                    val target = RAIL.substring(RAIL.indexOf(letter)).firstNotNullOfOrNull { headerPositions[it] }
                        ?: headerPositions.values.maxOrNull()
                    if (target != null) scope.launch { listState.scrollToItem(target) }
                }
            }
        }
    }
}

/** A-Z index: tap or slide a finger along it to jump. */
@Composable
private fun LetterRail(modifier: Modifier, onLetter: (Char) -> Unit) {
    var heightPx by remember { mutableStateOf(1f) }
    // The gesture handlers outlive recompositions; always jump within the current list.
    val jump by rememberUpdatedState(onLetter)
    fun pick(y: Float) {
        val i = ((y / heightPx) * RAIL.length).toInt().coerceIn(0, RAIL.length - 1)
        jump(RAIL[i])
    }
    Column(
        modifier
            .padding(vertical = 8.dp)
            .pointerInput(Unit) {
                heightPx = size.height.toFloat()
                detectTapGestures { pick(it.y) }
            }
            .pointerInput(Unit) {
                heightPx = size.height.toFloat()
                detectVerticalDragGestures { change, _ -> pick(change.position.y) }
            },
        verticalArrangement = Arrangement.SpaceAround,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RAIL.forEach { Txt(it.toString(), size = 9.sp, color = Color(0xFF6A5E76), weight = FontWeight.Medium) }
    }
}

@Composable
private fun GroupRow(g: Group, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Txt(g.name.ifEmpty { "Unknown" }, size = 15.sp)
            Txt(g.detail, size = 12.sp, color = Pal.Text3)
        }
        Icn(Ic.Chevron, Pal.Text3, 18.dp)
    }
}

/**
 * Menu for a track outside the queue; shared by library, search and playlists.
 * [extra] adds screen-specific actions such as removing from a playlist.
 */
@Composable
fun TrackMenu(
    vm: AppViewModel,
    actions: AppActions,
    t: Track,
    onInfo: () -> Unit,
    extra: List<MenuAction> = emptyList(),
    onDismiss: () -> Unit,
) {
    val favorites by vm.library.favorites.collectAsStateWithLifecycle()
    val fav = t.id in favorites
    ActionSheet(
        title = t.title,
        subtitle = "${t.artist} · ${formatTime(t.durationMs)} · ${t.format}",
        actions = listOf(
            MenuAction("Play next", Ic.PlayNext) { vm.player.playNext(listOf(t.id)) },
            MenuAction("Add to playlist", Ic.AddToList) { actions.addToPlaylist(listOf(t.id)) },
            MenuAction(if (fav) "Remove from favorites" else "Add to favorites", if (fav) Ic.HeartFilled else Ic.Heart) { vm.library.toggleFavorite(t.id) },
            MenuAction("Track info", Ic.Info, onInfo),
            MenuAction("Show folder", Ic.Folder) { actions.go(Screen.Library(tab = 3, group = t.folderId)) },
        ) + extra,
        onDismiss = onDismiss,
    )
}

/** Details of one file: tags, format, file name and where it lives. */
@Composable
fun TrackInfoSheet(vm: AppViewModel, t: Track, onDismiss: () -> Unit) {
    val folders by vm.library.folders.collectAsStateWithLifecycle()
    val folder = folders.firstOrNull { it.id == t.folderId }
    BackHandler(onBack = onDismiss)
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xAD08040A))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier
                .padding(8.dp)
                .navigationBarsPadding()
                .fillMaxWidth()
                .background(Pal.Raised, RoundedCornerShape(28.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Txt("Track info", size = 17.sp, weight = FontWeight.SemiBold)
            InfoLine("Title", t.title)
            InfoLine("Artist", t.artist)
            InfoLine("Album", t.album.ifEmpty { "-" })
            InfoLine("Length", formatTime(t.durationMs))
            InfoLine("Format", t.format)
            InfoLine("File", t.fileName)
            InfoLine("Folder", folder?.path ?: "-")
            InfoLine("Added", DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(t.added)))
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row {
        Txt(label, Modifier.width(72.dp), size = 13.sp, color = Pal.Text3)
        Txt(value, Modifier.weight(1f), size = 13.sp, maxLines = 3)
    }
}
