package app.spotamp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.spotamp.data.MusicFolder
import app.spotamp.data.ScanProgress
import app.spotamp.ui.ActionSheet
import app.spotamp.ui.AppActions
import app.spotamp.ui.AppViewModel
import app.spotamp.ui.Ic
import app.spotamp.ui.IconTap
import app.spotamp.ui.Icn
import app.spotamp.ui.LocalAccent
import app.spotamp.ui.MenuAction
import app.spotamp.ui.Pal
import app.spotamp.ui.PrimaryButton
import app.spotamp.ui.ScreenHeader
import app.spotamp.ui.SecondaryButton
import app.spotamp.ui.SettingSwitch
import app.spotamp.ui.Txt
import android.text.format.DateUtils

@Composable
fun FoldersScreen(vm: AppViewModel, actions: AppActions) {
    val folders by vm.library.folders.collectAsStateWithLifecycle()
    val scans by vm.library.scans.collectAsStateWithLifecycle()
    val tracks by vm.library.tracks.collectAsStateWithLifecycle()
    val settings by vm.library.prefs.settings.collectAsStateWithLifecycle()
    var confirmRemove by remember { mutableStateOf<MusicFolder?>(null) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Music folders",
            subtitle = "${folders.size} ${if (folders.size == 1) "folder" else "folders"} · ${tracks.size} tracks in library",
            onBack = actions.back,
        )
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            folders.forEach { f -> FolderCard(f, scans[f.id]) { confirmRemove = f } }

            SecondaryButton("Add folder", actions.addFolder, Modifier.fillMaxWidth().padding(top = 8.dp), height = 56.dp, icon = Ic.Plus, textSize = 15.sp)
            SecondaryButton(
                "Read folders again",
                { vm.library.rescanAll() },
                Modifier.fillMaxWidth(),
                height = 44.dp,
                icon = Ic.Refresh,
            )

            Spacer(Modifier.height(8.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Pal.Card)) {
                SettingSwitch(
                    "Include subfolders",
                    "Read files from every folder inside the picked one",
                    settings.includeSubfolders,
                ) { on -> vm.library.prefs.update { it.copy(includeSubfolders = on) } }
                SettingSwitch(
                    "Watch for new files",
                    "Add new tracks automatically when they appear in these folders",
                    settings.watchFolders,
                ) { on -> vm.library.prefs.update { it.copy(watchFolders = on) } }
            }
            Txt(
                "SpotAmp only reads the folders you pick. Files stay where they are. Removing a folder only takes its tracks out of the library, nothing is deleted from the phone.",
                Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
                size = 12.sp, color = Pal.Text3, maxLines = 6, lineHeight = 18.sp,
            )
        }
        Spacer(Modifier.navigationBarsPadding())
    }

    confirmRemove?.let { f ->
        ActionSheet(
            title = "Remove ${f.name}?",
            subtitle = "Its tracks leave the library. The files stay on the phone.",
            actions = listOf(MenuAction("Remove from library", Ic.Trash) { vm.library.removeFolder(f.id) }),
            onDismiss = { confirmRemove = null },
        )
    }
}

@Composable
private fun FolderCard(f: MusicFolder, scan: ScanProgress?, onRemove: () -> Unit) {
    val accent = LocalAccent.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Pal.Card)
            .padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (scan != null) accent.text.copy(alpha = 0.16f) else Pal.Raised),
            contentAlignment = Alignment.Center,
        ) {
            Icn(Ic.Folder, if (scan != null) accent.text else Pal.Text2, 20.dp)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Txt(f.name, size = 15.sp, weight = FontWeight.Medium)
            Txt(f.path, size = 12.sp, color = Pal.Text3)
            when {
                scan != null -> Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    val fraction = if (scan.total > 0) scan.done.toFloat() / scan.total else 0f
                    Box(Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(2.dp)).background(Pal.Line)) {
                        Box(
                            Modifier
                                .fillMaxWidth(fraction)
                                .height(3.dp)
                                .background(Brush.horizontalGradient(accent.colors.let { if (it.size == 1) it + it else it })),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Txt("${scan.done} / ${scan.total}", size = 12.sp, color = accent.text)
                }
                f.unavailable -> Txt("Not reachable. Check the SD card or pick the folder again.", size = 12.sp, color = Pal.Amber, maxLines = 2)
                else -> Txt(
                    "${f.trackCount} tracks" + if (f.lastScan > 0) {
                        " · updated " + DateUtils.getRelativeTimeSpanString(f.lastScan, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
                    } else "",
                    size = 12.sp, color = Pal.Text2,
                )
            }
        }
        IconTap(Ic.Close, "Remove ${f.name} from library", onRemove, tint = Pal.Text3)
    }
}
