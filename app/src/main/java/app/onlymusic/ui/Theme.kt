package app.onlymusic.ui

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import app.onlymusic.R

object Pal {
    val Bg = Color(0xFF0C0810)
    val Card = Color(0xFF1A1120)
    val Raised = Color(0xFF22162A)
    val Line = Color(0xFF2E2236)
    val Text = Color(0xFFF0EAF2)
    val Text2 = Color(0xFFB9AEC4)
    val Text3 = Color(0xFF9C90A8)
    val Disabled = Color(0xFF4E4256)
    val Amber = Color(0xFFF59A5B)
    val Gold = Color(0xFFFFB067)
    val Violet = Color(0xFF9B6CFF)
    val Burgundy = Color(0xFFA8326E)
    val Orange = Color(0xFFF28C4B)
    val ButtonTop = Color(0xFF2E1F3A)
    val ButtonBottom = Color(0xFF180F20)
    val PillOnStart = Color(0xFF4B2C63)
    val PillOnEnd = Color(0xFF5E2442)
    val Highlight = Color(0xFFFFB98A)
}

/** Inter, cut down to Latin with Polish letters (about 50 KB per weight). */
val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

/** Accent choices from Settings: gradient colors plus the readable text tone. */
class Accent(val name: String, val colors: List<Color>, val text: Color) {
    /** Gradients need two stops; single-color accents are doubled. */
    val brushColors: List<Color> get() = if (colors.size == 1) colors + colors else colors
}

val Accents = listOf(
    Accent("Ember", listOf(Color(0xFF8A5CF0), Color(0xFFA8326E), Color(0xFFF28C4B)), Pal.Amber),
    Accent("Violet", listOf(Color(0xFF6A43D8), Color(0xFFA98AFF)), Color(0xFFC2AEFF)),
    Accent("Burgundy", listOf(Color(0xFF7A1A45), Color(0xFFC2456F)), Color(0xFFF08AAE)),
    Accent("Amber", listOf(Color(0xFFD9763F), Color(0xFFFFB067)), Pal.Gold),
)

val LocalAccent = staticCompositionLocalOf { Accents[0] }

fun textStyle(size: TextUnit, color: Color = Pal.Text, weight: FontWeight = FontWeight.Normal) =
    TextStyle(color = color, fontSize = size, fontWeight = weight, fontFamily = Inter, fontFeatureSettings = "tnum")

@Composable
fun Txt(
    text: String,
    modifier: Modifier = Modifier,
    size: TextUnit = 14.sp,
    color: Color = Pal.Text,
    weight: FontWeight = FontWeight.Normal,
    maxLines: Int = 1,
    lineHeight: TextUnit = TextUnit.Unspecified,
    align: TextAlign = TextAlign.Start,
    brush: Brush? = null,
) = Txt(AnnotatedString(text), modifier, size, color, weight, maxLines, lineHeight, align, brush)

@Composable
fun Txt(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    size: TextUnit = 14.sp,
    color: Color = Pal.Text,
    weight: FontWeight = FontWeight.Normal,
    maxLines: Int = 1,
    lineHeight: TextUnit = TextUnit.Unspecified,
    align: TextAlign = TextAlign.Start,
    brush: Brush? = null,
) {
    val base = textStyle(size, color, weight).copy(lineHeight = lineHeight, textAlign = align)
    BasicText(
        text = text,
        modifier = modifier,
        style = if (brush != null) base.copy(brush = brush) else base,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Marks every match of [query] in [text] with the highlight color, like the search design. */
fun highlight(text: String, query: String, bold: Boolean = true): AnnotatedString = buildAnnotatedString {
    if (query.isEmpty()) {
        append(text)
        return@buildAnnotatedString
    }
    var start = 0
    while (start < text.length) {
        val i = text.indexOf(query, start, ignoreCase = true)
        if (i < 0) break
        append(text.substring(start, i))
        withStyle(SpanStyle(color = Pal.Highlight, fontWeight = if (bold) FontWeight.SemiBold else null)) {
            append(text.substring(i, i + query.length))
        }
        start = i + query.length
    }
    append(text.substring(start))
}

fun formatTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
