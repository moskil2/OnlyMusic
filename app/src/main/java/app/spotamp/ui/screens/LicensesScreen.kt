package app.spotamp.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.spotamp.R
import app.spotamp.ui.AppActions
import app.spotamp.ui.Ic
import app.spotamp.ui.Icn
import app.spotamp.ui.Pal
import app.spotamp.ui.ScreenHeader
import app.spotamp.ui.SectionLabel
import app.spotamp.ui.Txt

private class Component(val name: String, val owner: String, val license: String)

private val COMPONENTS = listOf(
    Component("Inter typeface", "The Inter Project Authors", "SIL Open Font License 1.1"),
    Component("Jetpack Compose, AndroidX", "The Android Open Source Project", "Apache License 2.0"),
    Component("Media3 (ExoPlayer, MediaSession)", "The Android Open Source Project", "Apache License 2.0"),
    Component("Kotlin, kotlinx.coroutines", "JetBrains s.r.o. and contributors", "Apache License 2.0"),
    Component("Guava", "The Guava Authors", "Apache License 2.0"),
)

/** Third-party components and the full texts of their licenses, as those licenses require. */
@Composable
fun LicensesScreen(actions: AppActions) {
    val context = LocalContext.current
    val ofl = remember { context.resources.openRawResource(R.raw.license_ofl).bufferedReader().use { it.readText() } }
    val apache = remember { context.resources.openRawResource(R.raw.license_apache2).bufferedReader().use { it.readText() } }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Open-source licenses", "SpotAmp is built with these components", actions.back)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Pal.Card)) {
                COMPONENTS.forEachIndexed { i, c ->
                    if (i > 0) Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp).background(Pal.Line))
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Txt(c.name, size = 14.sp, weight = FontWeight.Medium)
                        Txt("${c.owner} · ${c.license}", size = 12.sp, color = Pal.Text3, maxLines = 2)
                    }
                }
            }
            SectionLabel("License texts", Modifier.padding(top = 8.dp))
            LicenseTile("SIL Open Font License 1.1", ofl)
            LicenseTile("Apache License 2.0", apache)
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun LicenseTile(title: String, text: String) {
    var open by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Pal.Card)
            .clickable { open = !open }
            .animateContentSize()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Txt(title, Modifier.weight(1f), size = 14.sp, weight = FontWeight.Medium)
            Box(Modifier.size(20.dp).rotate(if (open) 90f else 0f), contentAlignment = Alignment.Center) {
                Icn(Ic.Chevron, Pal.Text3, 18.dp)
            }
        }
        if (open) Txt(text, size = 11.sp, color = Pal.Text2, maxLines = Int.MAX_VALUE, lineHeight = 16.sp)
    }
}
