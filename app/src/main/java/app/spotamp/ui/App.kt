package app.spotamp.ui

import android.app.Activity
import android.app.Application
import android.content.Intent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.spotamp.playback.PlaybackService
import app.spotamp.ui.screens.MenuScreen
import app.spotamp.ui.screens.Wordmark
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.spotamp.SpotAmpApp
import app.spotamp.data.Track
import app.spotamp.playback.PlayerConnection
import app.spotamp.ui.screens.FoldersScreen
import app.spotamp.ui.screens.LibraryScreen
import app.spotamp.ui.screens.PlayerScreen
import app.spotamp.ui.screens.PlaylistDetailScreen
import app.spotamp.ui.screens.PlaylistsScreen
import app.spotamp.ui.screens.SearchScreen
import app.spotamp.ui.screens.SettingsScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

class AppViewModel(app: Application) : AndroidViewModel(app) {
    val library = (app as SpotAmpApp).library
    val player = PlayerConnection(app, viewModelScope, library.prefs)

    init {
        // Tracks that appear while the app runs (a new folder, new files) go to the end of the
        // main list, in folder order, ready to play.
        viewModelScope.launch {
            var known = library.tracks.value.mapTo(HashSet()) { it.id }
            library.tracks.drop(1).collect { list ->
                val added = list.filter { it.id !in known }.map { it.id }
                known = list.mapTo(HashSet()) { it.id }
                if (added.isNotEmpty()) player.appendNewTracks(added, "Library")
            }
        }
        // Pick up files added or removed since the last start. Known files are not read again.
        library.rescanAll()
    }

    override fun onCleared() {
        player.release()
    }
}

sealed interface Screen {
    data object Player : Screen
    /** [tab] 0 tracks, 1 artists, 2 albums, 3 folders; [group] opens one artist, album or folder id. */
    data class Library(val tab: Int = 0, val group: String? = null) : Screen
    data object Folders : Screen
    data object Playlists : Screen
    data class PlaylistDetail(val id: String) : Screen
    data object Search : Screen
    data object Settings : Screen
    data object About : Screen
    data object Licenses : Screen
}

class AppActions(
    val go: (Screen) -> Unit,
    val back: () -> Unit,
    val addFolder: () -> Unit,
    val addToPlaylist: (List<String>) -> Unit,
    val openMenu: () -> Unit,
)

@Composable
fun SpotAmpRoot(vm: AppViewModel = viewModel()) {
    val settings by vm.library.prefs.settings.collectAsStateWithLifecycle()
    val playlists by vm.library.playlists.collectAsStateWithLifecycle()
    val stack = remember { mutableStateListOf<Screen>(Screen.Player) }
    var addTo by remember { mutableStateOf<List<String>?>(null) }
    var newPlaylistFor by remember { mutableStateOf<List<String>?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) vm.library.addFolder(uri)
    }
    val actions = remember {
        AppActions(
            go = { target ->
                if (target == Screen.Player) {
                    while (stack.size > 1) stack.removeAt(stack.lastIndex)
                } else {
                    stack.add(target)
                }
            },
            back = { if (stack.size > 1) stack.removeAt(stack.lastIndex) },
            addFolder = { pickFolder.launch(null) },
            addToPlaylist = { addTo = it },
            openMenu = { menuOpen = true },
        )
    }
    BackHandler(enabled = stack.size > 1) { actions.back() }

    CompositionLocalProvider(LocalAccent provides Accents[settings.accent.coerceIn(0, Accents.lastIndex)]) {
        Box(Modifier.fillMaxSize().background(Pal.Bg)) {
            val top = stack.last()
            key(stack.size, top) { when (val screen = top) {
                Screen.Player -> PlayerScreen(vm, actions, settings)
                is Screen.Library -> LibraryScreen(vm, actions, screen)
                Screen.Folders -> FoldersScreen(vm, actions)
                Screen.Playlists -> PlaylistsScreen(vm, actions)
                is Screen.PlaylistDetail -> PlaylistDetailScreen(vm, actions, screen.id)
                Screen.Search -> SearchScreen(vm, actions)
                Screen.Settings -> SettingsScreen(vm, actions)
                Screen.About -> MenuScreen(actions)
                Screen.Licenses -> app.spotamp.ui.screens.LicensesScreen(actions)
            } }

            SideMenu(
                open = menuOpen,
                current = top,
                onNavigate = { target ->
                    menuOpen = false
                    if (target != top) actions.go(target)
                },
                onDismiss = { menuOpen = false },
                onCloseApp = {
                    vm.player.shutdown()
                    context.stopService(Intent(context, PlaybackService::class.java))
                    (context as? Activity)?.finishAndRemoveTask()
                },
            )

            addTo?.let { ids ->
                ActionSheet(
                    title = "Add to playlist",
                    subtitle = if (ids.size == 1) "1 track" else "${ids.size} tracks",
                    actions = listOf(MenuAction("New playlist", Ic.Plus) { newPlaylistFor = ids }) +
                        playlists.map { p -> MenuAction(p.name, Ic.Playlists) { vm.library.addToPlaylist(p.id, ids) } },
                    onDismiss = { addTo = null },
                )
            }
            newPlaylistFor?.let { ids ->
                TextPrompt(
                    title = "New playlist",
                    initial = "",
                    confirm = "Create",
                    onConfirm = { name ->
                        vm.library.createPlaylist(name, ids)
                        newPlaylistFor = null
                    },
                    onDismiss = { newPlaylistFor = null },
                )
            }
        }
    }
}

/** Playback position, refreshed only while the screen is visible. */
@Composable
fun rememberPositionMs(player: PlayerConnection, playing: Boolean): Long {
    var pos by remember { mutableLongStateOf(player.positionMs()) }
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(owner, playing) {
        owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                pos = player.positionMs()
                delay(if (playing) 250L else 1000L)
            }
        }
    }
    return pos
}

fun Track.subtitle(): String = if (album.isNotEmpty()) "$artist · $album" else artist

private class MenuEntry(val label: String, val icon: ImageVector, val screen: Screen)

/** Side menu, laid out like EggSPEED's: wordmark, close-app button, Menu first, then the screens. */
@Composable
private fun SideMenu(open: Boolean, current: Screen, onNavigate: (Screen) -> Unit, onDismiss: () -> Unit, onCloseApp: () -> Unit) {
    val progress by animateFloatAsState(if (open) 1f else 0f, tween(220), label = "sideMenu")
    if (progress == 0f) return
    BackHandler(enabled = open, onBack = onDismiss)
    val accent = LocalAccent.current
    val entries = listOf(
        MenuEntry("Settings", Ic.Settings, Screen.Settings),
        MenuEntry("About", Ic.Info, Screen.About),
    )
    val shape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = progress }
                .background(Color(0xAD08040A))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
        )
        Column(
            Modifier
                .fillMaxHeight()
                .width(300.dp)
                .graphicsLayer { translationX = -(1f - progress) * size.width }
                .shadow(24.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
                .clip(shape)
                .drawBehind {
                    drawRect(Brush.verticalGradient(listOf(Color(0xFF1C1030), Color(0xFF220E26), Color(0xFF2A0F1F))))
                    drawRect(Brush.radialGradient(listOf(Color(0x557846E6), Color.Transparent), Offset(size.width * 0.2f, size.height * 0.12f), size.width * 0.9f))
                    drawRect(Brush.radialGradient(listOf(Color(0x40961E50), Color.Transparent), Offset(size.width * 0.8f, size.height * 0.9f), size.width))
                }
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f).padding(start = 6.dp)) {
                    Wordmark(19)
                    Txt("Local music player", size = 12.sp, color = Pal.Text3)
                }
                Row(
                    Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFFF08AAE), RoundedCornerShape(16.dp))
                        .clickable(onClick = onCloseApp)
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icn(Ic.Power, Color(0xFFF08AAE), 14.dp)
                    Spacer(Modifier.width(6.dp))
                    Txt("CLOSE APP", size = 12.sp, weight = FontWeight.SemiBold, color = Color(0xFFF08AAE))
                }
            }
            Spacer(Modifier.height(22.dp))
            entries.forEach { e ->
                val selected = e.screen == current
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (selected) Brush.horizontalGradient(listOf(Color(0x389B6CFF), Color(0x29A8326E), Color(0x1FF28C4B)))
                            else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                        )
                        .clickable { onNavigate(e.screen) }
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icn(e.icon, if (selected) accent.text else Pal.Text2, 20.dp)
                    Spacer(Modifier.width(14.dp))
                    Txt(e.label, size = 16.sp, weight = if (selected) FontWeight.SemiBold else FontWeight.Medium, color = if (selected) Pal.Text else Pal.Text2)
                }
            }
        }
    }
}

/** Mini player shown at the bottom of the secondary screens. */
@Composable
fun MiniPlayerBar(vm: AppViewModel, actions: AppActions) {
    val state by vm.player.state.collectAsStateWithLifecycle()
    val trackMap by vm.library.trackMap.collectAsStateWithLifecycle()
    val track = state.currentId?.let { trackMap[it] } ?: return
    val pos = rememberPositionMs(vm.player, state.isPlaying)
    MiniPlayer(
        title = track.title,
        subtitle = "${track.artist} · ${formatTime(pos)} / ${formatTime(track.durationMs)}",
        isPlaying = state.isPlaying,
        progress = if (track.durationMs > 0) pos.toFloat() / track.durationMs else 0f,
        onOpen = { actions.go(Screen.Player) },
        onToggle = { vm.player.togglePlay() },
    )
}
