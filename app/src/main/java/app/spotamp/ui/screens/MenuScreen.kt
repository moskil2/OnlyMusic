package app.spotamp.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.spotamp.BuildConfig
import app.spotamp.ui.AppActions
import app.spotamp.ui.Ic
import app.spotamp.ui.Icn
import app.spotamp.ui.LocalAccent
import app.spotamp.ui.Pal
import app.spotamp.ui.Screen
import app.spotamp.ui.ScreenHeader
import app.spotamp.ui.Txt

private const val CONTACT_EMAIL = "tomasz.pieczara@gazeta.pl"
private const val WEBSITE = "spotrobotics.app"
private const val SUPPORT_FORM_URL = "https://spotrobotics.app/support/"

/** GitHub address, empty until the repository exists; the row and the copyright sentence appear once it is set. */
private const val GITHUB_DISPLAY = ""

private const val PRIVACY =
    "SpotAmp does not collect, send, or store any personal data beyond your phone. The app has no internet " +
        "access: it only reads the music folders you pick, and your library, playlists and settings stay on " +
        "this device. No accounts, no ads, no analytics, no tracking."

private const val TERMS =
    "SpotAmp is a free player for audio files stored on your own device. You are responsible for having the " +
        "right to play the files you add. The app is provided \"as is\", without any warranty of uninterrupted " +
        "or error-free operation. The developer bears no responsibility whatsoever for damage resulting from " +
        "using the app - you use it at your own risk."

private const val CONTACT_BODY =
    "Found a bug or have an idea for a new feature? Just want to say hi? Fill out a short form - every " +
        "message is read personally."

private const val COPYRIGHT =
    "© 2026 Tomasz Pieczara. All rights reserved. The source code, assets, and documentation of this app " +
        "are the property of the developer. Copying, modifying, redistributing, or selling all or part of the " +
        "code, in source or compiled form, without the developer's written consent is prohibited."

private const val COPYRIGHT_GITHUB =
    " The GitHub repository is made available for viewing only - forking or copying the code does not grant " +
        "any rights to use it."

private const val COPYRIGHT_END = " The app is distributed exclusively through Google Play."

/** The About screen (EggSPEED's "Menu") in SpotAmp's look: about, Google Play, contact and legal texts. */
@Composable
fun MenuScreen(actions: AppActions) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("About", null, actions.back)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Wordmark(22)
                    Txt("Created by Tomasz Pieczara", size = 14.sp)
                }
            }
            Card {
                Column {
                    ActionRow(Ic.Star, "Rate the app on Google Play") { openPlayStore(context) }
                    Divider()
                    ActionRow(Ic.Update, "Check for updates") { openPlayStore(context) }
                }
            }
            Card {
                Column {
                    InfoRow("Version", BuildConfig.VERSION_NAME)
                    Divider()
                    InfoRow("Build", BuildConfig.BUILD_STAMP)
                }
            }
            Card {
                Column {
                    InfoRow("Contact", CONTACT_EMAIL, Ic.Mail) {
                        open(context, Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$CONTACT_EMAIL")))
                    }
                    Divider()
                    InfoRow("Website", WEBSITE, Ic.Globe) {
                        open(context, Intent(Intent.ACTION_VIEW, Uri.parse("https://$WEBSITE")))
                    }
                    if (GITHUB_DISPLAY.isNotEmpty()) {
                        Divider()
                        InfoRow("GitHub", GITHUB_DISPLAY, Ic.Globe) {
                            open(context, Intent(Intent.ACTION_VIEW, Uri.parse("https://$GITHUB_DISPLAY")))
                        }
                    }
                }
            }
            ExpandableTile("Privacy policy", PRIVACY)
            ExpandableTile("Terms of service", TERMS)
            ExpandableTile("Contact / Report bug / Feature request", CONTACT_BODY, "Open support form →") {
                open(context, Intent(Intent.ACTION_VIEW, Uri.parse(SUPPORT_FORM_URL)))
            }
            ExpandableTile(
                "Copyright",
                COPYRIGHT + (if (GITHUB_DISPLAY.isNotEmpty()) COPYRIGHT_GITHUB else "") + COPYRIGHT_END,
            )
            Card {
                ActionRow(Ic.Info, "Open-source licenses") { actions.go(Screen.Licenses) }
            }
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
fun Wordmark(sizeSp: Int) {
    Row {
        Txt("Spot", size = sizeSp.sp, weight = FontWeight.SemiBold)
        Txt("Amp", size = sizeSp.sp, weight = FontWeight.SemiBold, brush = Brush.horizontalGradient(listOf(Color(0xFFB794FF), Pal.Amber)))
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

@Composable
private fun ActionRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    val accent = LocalAccent.current
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icn(icon, accent.text, 20.dp)
        Spacer(Modifier.width(14.dp))
        Txt(label, Modifier.weight(1f), size = 14.sp)
        Icn(Ic.Chevron, Pal.Text3, 18.dp)
    }
}

@Composable
private fun InfoRow(label: String, value: String, icon: ImageVector? = null, onClick: (() -> Unit)? = null) {
    val accent = LocalAccent.current
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icn(icon, accent.text, 20.dp)
            Spacer(Modifier.width(14.dp))
        }
        Txt(label, Modifier.weight(1f), size = 14.sp, color = Pal.Text2)
        Txt(value, size = 14.sp, color = if (onClick != null) accent.text else Pal.Text)
    }
}

/** Tap to read: the text, and optionally a link, show only once the tile is opened. */
@Composable
private fun ExpandableTile(title: String, body: String, linkLabel: String? = null, onLink: (() -> Unit)? = null) {
    val accent = LocalAccent.current
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
        if (open) {
            Txt(body, size = 13.sp, color = Pal.Text2, maxLines = 30, lineHeight = 20.sp)
            if (linkLabel != null && onLink != null) {
                Txt(
                    linkLabel,
                    Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onLink).padding(vertical = 4.dp),
                    size = 14.sp, weight = FontWeight.SemiBold, color = accent.text,
                )
            }
        }
    }
}

private fun open(context: Context, intent: Intent) {
    runCatching { context.startActivity(intent) }
}

/** Opens SpotAmp in the Play Store app, or in the browser when the store app is missing. */
private fun openPlayStore(context: Context) {
    val pkg = context.packageName
    val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")).apply {
        setPackage("com.android.vending")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(market) }.onFailure {
        open(context, Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg")))
    }
}
