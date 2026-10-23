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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.spotamp.data.Track
import app.spotamp.ui.AppActions
import app.spotamp.ui.AppViewModel
import app.spotamp.ui.Chip
import app.spotamp.ui.Ic
import app.spotamp.ui.IconTap
import app.spotamp.ui.Icn
import app.spotamp.ui.LocalAccent
import app.spotamp.ui.Pal
import app.spotamp.ui.Screen
import app.spotamp.ui.SectionLabel
import app.spotamp.ui.TrackRow
import app.spotamp.ui.Txt
import app.spotamp.ui.formatTime
import app.spotamp.ui.highlight
import app.spotamp.ui.subtitle
import app.spotamp.ui.textStyle

private val FILTERS = listOf("All", "Tracks", "Artists", "Albums")

private class Named(val name: String, val count: Int, val detail: String)

@Composable
fun SearchScreen(vm: AppViewModel, actions: AppActions) {
    val tracks by vm.library.tracks.collectAsStateWithLifecycle()
    val state by vm.player.state.collectAsStateWithLifecycle()
    val recent by vm.library.prefs.recentSearches.collectAsStateWithLifecycle()
    val accent = LocalAccent.current
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    val q = query.trim()
    val matches = remember(tracks, q) {
        if (q.isEmpty()) emptyList()
        else tracks.filter {
            it.title.contains(q, true) || it.artist.contains(q, true) ||
                it.album.contains(q, true) || it.fileName.contains(q, true)
        }.sortedBy { it.title.lowercase() }
    }
    val artists = remember(tracks, q) {
        if (q.isEmpty()) emptyList()
        else tracks.filter { it.artist.contains(q, true) }.groupBy { it.artist }
            .map { (name, list) -> Named(name, list.size, "") }.sortedBy { it.name.lowercase() }
    }
    val albums = remember(tracks, q) {
        if (q.isEmpty()) emptyList()
        else tracks.filter { it.album.isNotEmpty() && it.album.contains(q, true) }.groupBy { it.album }
            .map { (name, list) -> Named(name, list.size, list.first().artist) }.sortedBy { it.name.lowercase() }
    }
    val ids = remember(matches) { matches.map { it.id } }
    val showTracks = filter == 0 || filter == 1
    val showArtists = filter == 0 || filter == 2
    val showAlbums = filter == 3
    val found = (if (showTracks) matches.size else 0) + (if (showArtists) artists.size else 0) + (if (showAlbums) albums.size else 0)

    Column(Modifier.fillMaxSize().imePadding()) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().height(68.dp).padding(start = 4.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTap(Ic.Back, "Back", actions.back, tint = Pal.Text)
            Row(
                Modifier
                    .weight(1f)
                    .height(48.dp)
                    .background(Brush.verticalGradient(listOf(Color(0xFF170E1C), Pal.Raised)), RoundedCornerShape(24.dp))
                    .border(1.dp, accent.text.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                    .padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icn(Ic.Search, Pal.Text2, 18.dp)
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Txt("Tracks, artists, albums", size = 15.sp, color = Pal.Text3)
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = textStyle(15.sp),
                        cursorBrush = SolidColor(accent.text),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    )
                }
                if (query.isNotEmpty()) IconTap(Ic.Close, "Clear search", { query = "" }, size = 40.dp, iconSize = 16.dp)
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FILTERS.forEachIndexed { i, name -> Chip(name, i == filter) { filter = i } }
            Spacer(Modifier.weight(1f))
            if (q.isNotEmpty()) Txt("$found found", size = 12.sp, color = Pal.Text3)
        }

        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
            if (q.isEmpty()) {
                if (recent.isNotEmpty()) item { SectionLabel("Recent searches") }
                items(recent) { r ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { query = r }
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icn(Ic.Clock, Pal.Text3, 18.dp)
                        Spacer(Modifier.width(14.dp))
                        Txt(r, size = 14.sp)
                    }
                }
                if (recent.isEmpty()) {
                    item {
                        Txt(
                            "Search covers titles, artists, albums and file names in your music folders.",
                            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            size = 13.sp, color = Pal.Text3, maxLines = 3, lineHeight = 19.sp,
                        )
                    }
                }
            } else if (found == 0) {
                item {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Txt("Nothing matches “$q”", size = 17.sp, weight = FontWeight.SemiBold)
                        Txt("Search covers titles, artists, albums and file names in your music folders.", size = 14.sp, color = Pal.Text2, maxLines = 3, lineHeight = 21.sp)
                    }
                }
            } else {
                if (showArtists && artists.isNotEmpty()) {
                    item { SectionLabel("Artists") }
                    items(artists, key = { "a:" + it.name }) { a ->
                        NamedRow(a, q) {
                            vm.library.prefs.addRecentSearch(q)
                            actions.go(Screen.Library(tab = 1, group = a.name))
                        }
                    }
                }
                if (showAlbums && albums.isNotEmpty()) {
                    item { SectionLabel("Albums") }
                    items(albums, key = { "b:" + it.name }) { a ->
                        NamedRow(a, q) {
                            vm.library.prefs.addRecentSearch(q)
                            actions.go(Screen.Library(tab = 2, group = a.name))
                        }
                    }
                }
                if (showTracks && matches.isNotEmpty()) {
                    item { SectionLabel("Tracks") }
                    itemsIndexed(matches, key = { _, t -> t.id }) { i, t ->
                        TrackRow(
                            title = t.title,
                            subtitle = t.subtitle(),
                            duration = formatTime(t.durationMs),
                            current = t.id == state.currentId,
                            onClick = {
                                vm.library.prefs.addRecentSearch(q)
                                vm.player.playQueue(ids, i, "Search: $q")
                            },
                            onMore = null,
                            query = q,
                            trailing = {
                                IconTap(Ic.AddToList, "Add ${t.title} to queue", { vm.player.addToQueue(listOf(t.id)) }, size = 44.dp, iconSize = 18.dp)
                            },
                        )
                    }
                }
            }
        }
        Spacer(Modifier.navigationBarsPadding())
    }
}

/** Artist or album row with the round gradient initial from the design. */
@Composable
private fun NamedRow(item: Named, query: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .shadow(4.dp, CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFF5A35A8), Color(0xFF9A2E62), Color(0xFFD9763F))), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Txt(item.name.take(1).uppercase(), size = 14.sp, weight = FontWeight.SemiBold)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Txt(highlight(item.name, query), size = 15.sp)
            if (item.detail.isNotEmpty()) Txt(item.detail, size = 12.sp, color = Pal.Text3)
        }
        Txt(if (item.count == 1) "1 track" else "${item.count} tracks", size = 12.sp, color = Pal.Text3)
    }
}
