package app.spotamp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalDensity
import app.spotamp.data.CROSSFADE_STEPS
import app.spotamp.ui.WaveLayer
import app.spotamp.ui.Accents
import app.spotamp.ui.AppActions
import app.spotamp.ui.AppViewModel
import app.spotamp.ui.Ic
import app.spotamp.ui.Icn
import app.spotamp.ui.LocalAccent
import app.spotamp.ui.Pal
import app.spotamp.ui.Screen
import app.spotamp.ui.ScreenHeader
import app.spotamp.ui.SecondaryButton
import app.spotamp.ui.SectionLabel
import app.spotamp.ui.SettingSwitch
import app.spotamp.ui.Txt

private val WAVE_STYLES = listOf("Flowing", "Calm", "Off")

@Composable
fun SettingsScreen(vm: AppViewModel, actions: AppActions) {
    val settings by vm.library.prefs.settings.collectAsStateWithLifecycle()
    val folders by vm.library.folders.collectAsStateWithLifecycle()
    val tracks by vm.library.tracks.collectAsStateWithLifecycle()
    val prefs = vm.library.prefs
    val accent = LocalAccent.current

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Settings", null, actions.back)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SectionLabel("Appearance")
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Txt("Accent color", size = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Accents.forEachIndexed { i, a ->
                            val selected = i == settings.accent
                            Box(
                                Modifier
                                    .size(44.dp)
                                    .border(2.dp, if (selected) Pal.Text else Color.Transparent, CircleShape)
                                    .clip(CircleShape)
                                    .clickable(role = Role.RadioButton) { prefs.update { it.copy(accent = i) } }
                                    .semantics { contentDescription = a.name },
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    Modifier
                                        .size(30.dp)
                                        .shadow(4.dp, CircleShape)
                                        .background(Brush.linearGradient(a.colors), CircleShape)
                                )
                            }
                        }
                    }
                    Txt("Sound wave", size = 14.sp, modifier = Modifier.padding(top = 6.dp))
                    Row(
                        Modifier.fillMaxWidth().background(Pal.Bg, RoundedCornerShape(14.dp)).padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        WAVE_STYLES.forEachIndexed { i, name ->
                            val selected = i == settings.waveStyle
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (selected) Brush.linearGradient(listOf(Pal.PillOnStart, Pal.PillOnEnd))
                                        else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                    )
                                    .clickable(role = Role.RadioButton) { prefs.update { it.copy(waveStyle = i) } },
                                contentAlignment = Alignment.Center,
                            ) {
                                Txt(name, size = 13.sp, color = if (selected) Pal.Text else Pal.Text2, weight = FontWeight.Medium)
                            }
                        }
                    }
                    val density = LocalDensity.current
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Pal.Bg),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (settings.waveStyle == 2) {
                            Txt("Wave hidden, saves a little battery", size = 12.sp, color = Pal.Text3)
                        } else {
                            // The 92 dp wave band, centred in the 64 dp preview.
                            WaveLayer(
                                Modifier.matchParentSize(),
                                waveTop = with(density) { (-14).dp.toPx() },
                                playing = true,
                                empty = false,
                                style = settings.waveStyle,
                            )
                        }
                    }
                }
            }

            SectionLabel("Library", Modifier.padding(top = 8.dp))
            Card {
                Column {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { actions.go(Screen.Folders) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Txt("Music folders", size = 14.sp)
                            Txt(
                                if (folders.isEmpty()) "None yet" else folders.joinToString(", ") { it.name },
                                size = 12.sp, color = Pal.Text2,
                            )
                        }
                        Icn(Ic.Chevron, Pal.Text3, 18.dp)
                    }
                    Divider()
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Txt("Read folders again", size = 14.sp)
                            Txt("${tracks.size} tracks in library", size = 12.sp, color = Pal.Text2)
                        }
                        SecondaryButton("Refresh", { vm.library.rescanAll() }, height = 36.dp)
                    }
                    Divider()
                    SettingSwitch(
                        "Skip short files",
                        "Ignore audio under 30 seconds, such as ringtones and voice notes. Applies on the next read.",
                        settings.skipShort,
                    ) { on -> prefs.update { it.copy(skipShort = on) } }
                }
            }

            SectionLabel("Playback", Modifier.padding(top = 8.dp))
            Card {
                Column {
                    SettingSwitch(
                        "Gapless playback",
                        "No silence between tracks of the same album",
                        settings.gapless,
                    ) { on -> prefs.update { it.copy(gapless = on) } }
                    Divider()
                    SettingSwitch(
                        "Pause when headphones disconnect",
                        "Wired and Bluetooth",
                        settings.pauseOnDisconnect,
                    ) { on -> prefs.update { it.copy(pauseOnDisconnect = on) } }
                    Divider()
                    SettingSwitch(
                        "Resume when headphones connect",
                        "Continues the last track",
                        settings.resumeOnConnect,
                    ) { on -> prefs.update { it.copy(resumeOnConnect = on) } }
                    Divider()
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                val i = CROSSFADE_STEPS.indexOf(settings.crossfadeSec)
                                prefs.update { it.copy(crossfadeSec = CROSSFADE_STEPS[(i + 1) % CROSSFADE_STEPS.size]) }
                            }
                            .padding(horizontal = 16.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Txt("Crossfade", Modifier.weight(1f), size = 14.sp)
                        Txt(if (settings.crossfadeSec == 0) "Off" else "${settings.crossfadeSec} s", size = 13.sp, color = Pal.Text2)
                    }
                }
            }

            Column(Modifier.padding(horizontal = 4.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row {
                    Txt("Spot", size = 14.sp, weight = FontWeight.SemiBold)
                    Txt("Amp", size = 14.sp, weight = FontWeight.SemiBold, color = accent.text)
                    Txt("  " + app.spotamp.BuildConfig.VERSION_NAME, size = 14.sp, color = Pal.Text3)
                }
                Txt(
                    "No ads, no tracking, no account. SpotAmp has no internet access and only reads the folders you pick. Everything is free and nothing is locked.",
                    size = 12.sp, color = Pal.Text2, maxLines = 4, lineHeight = 18.sp,
                )
            }
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Pal.Card)) { content() }
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp).background(Pal.Line))
}
