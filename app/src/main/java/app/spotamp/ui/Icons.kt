package app.spotamp.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Line icons drawn from the same SVG paths as the design. */
object Ic {
    private fun circle(cx: Float, cy: Float, r: Float) =
        "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0z"

    private fun icon(stroke: String? = null, fill: String? = null, width: Float = 1.8f): ImageVector {
        val b = ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        if (stroke != null) {
            b.addPath(
                pathData = addPathNodes(stroke),
                stroke = SolidColor(Color.White),
                strokeLineWidth = width,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        if (fill != null) b.addPath(pathData = addPathNodes(fill), fill = SolidColor(Color.White))
        return b.build()
    }

    val Search by lazy { icon(circle(11f, 11f, 6.5f) + "M16 16l4.5 4.5") }
    val Library by lazy { icon("M4 5v14M8.5 5v14M13 5.5l4.5 13.5") }
    val Playlists by lazy { icon("M4 6h11M4 11h11M4 16h7M19.5 4v11.5" + circle(17.2f, 16f, 2.3f)) }
    val Settings by lazy { icon("M4 7h10M18 7h2M4 17h3M11 17h9" + circle(16f, 7f, 2f) + circle(9f, 17f, 2f)) }
    val Shuffle by lazy { icon("M3 7h3c4.5 0 6.5 10 11 10h4M3 17h3c1.6 0 2.8-1.4 3.8-3.2M14.2 10.2C15.2 8.4 16.4 7 18 7h3M18 4l3 3-3 3M18 14l3 3-3 3") }
    val Repeat by lazy { icon("M4 11V9.5A3.5 3.5 0 0 1 7.5 6H20M17 3l3 3-3 3M20 13v1.5a3.5 3.5 0 0 1-3.5 3.5H4M7 21l-3-3 3-3") }
    val Prev by lazy { icon("M6 5v14", "M18.5 6.2v11.6a.8.8 0 0 1-1.2.7L9 12.7a.8.8 0 0 1 0-1.4l8.3-5.8a.8.8 0 0 1 1.2.7z") }
    val Next by lazy { icon("M18 5v14", "M5.5 6.2v11.6a.8.8 0 0 0 1.2.7l8.3-5.8a.8.8 0 0 0 0-1.4L6.7 5.5a.8.8 0 0 0-1.2.7z") }
    val Play by lazy { icon(fill = "M8 5.6v12.8a1 1 0 0 0 1.5.9l10.2-6.4a1 1 0 0 0 0-1.7L9.5 4.7A1 1 0 0 0 8 5.6z") }
    val Pause by lazy {
        icon(fill = "M7.7 5h1.6a1.2 1.2 0 0 1 1.2 1.2v11.6a1.2 1.2 0 0 1-1.2 1.2H7.7a1.2 1.2 0 0 1-1.2-1.2V6.2A1.2 1.2 0 0 1 7.7 5zM14.7 5h1.6a1.2 1.2 0 0 1 1.2 1.2v11.6a1.2 1.2 0 0 1-1.2 1.2h-1.6a1.2 1.2 0 0 1-1.2-1.2V6.2a1.2 1.2 0 0 1 1.2-1.2z")
    }
    val Stop by lazy { icon(fill = "M8 5h8a3 3 0 0 1 3 3v8a3 3 0 0 1-3 3H8a3 3 0 0 1-3-3V8a3 3 0 0 1 3-3z") }
    val More by lazy { icon(fill = circle(12f, 5f, 1.6f) + circle(12f, 12f, 1.6f) + circle(12f, 19f, 1.6f)) }
    val Back by lazy { icon("M15 5l-7 7 7 7") }
    val Chevron by lazy { icon("M9 5l7 7-7 7") }
    val Folder by lazy { icon("M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z") }
    val FolderAdd by lazy { icon("M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2zM12 10.5v5M9.5 13h5") }
    val Plus by lazy { icon("M12 5v14M5 12h14", width = 2.2f) }
    val Close by lazy { icon("M6 6l12 12M18 6L6 18") }
    val Check by lazy { icon("M5 12.5l4.5 4.5L19 7.5", width = 2.2f) }
    val Sort by lazy { icon("M7 4v16M3.5 16.5L7 20l3.5-3.5M13 6h8M13 12h6M13 18h3") }
    val AddToList by lazy { icon("M4 7h12M4 12h12M4 17h7M18 14v7M14.5 17.5h7") }
    val PlayNext by lazy { icon("M4 7h10M4 12h10M4 17h6", "M17 14l4 3-4 3z") }
    val Minus by lazy { icon("M5 12h14") }
    val Up by lazy { icon("M6 15l6-6 6 6") }
    val Down by lazy { icon("M6 9l6 6 6-6") }
    val Edit by lazy { icon("M4 20h4L19 9l-4-4L4 16zM13.5 6.5l4 4") }
    val Trash by lazy { icon("M4 7h16M9 7V4.5h6V7M6.5 7l1 13h9l1-13") }
    val Refresh by lazy { icon("M20 11a8 8 0 1 0-2.3 5.7M20 4.5V11h-6.5") }
    val Grip by lazy {
        icon(fill = circle(9f, 6f, 1.5f) + circle(15f, 6f, 1.5f) + circle(9f, 12f, 1.5f) + circle(15f, 12f, 1.5f) + circle(9f, 18f, 1.5f) + circle(15f, 18f, 1.5f))
    }
    val Info by lazy { icon(circle(12f, 12f, 9f) + "M12 11v6M12 7.5v.5") }
    val Heart by lazy { icon("M12 20s-7.5-4.6-7.5-10A4.3 4.3 0 0 1 12 7.4 4.3 4.3 0 0 1 19.5 10c0 5.4-7.5 10-7.5 10z") }
    val HeartFilled by lazy { icon(fill = "M12 20s-7.5-4.6-7.5-10A4.3 4.3 0 0 1 12 7.4 4.3 4.3 0 0 1 19.5 10c0 5.4-7.5 10-7.5 10z") }
    val Clock by lazy { icon(circle(12f, 12f, 8.5f) + "M12 7.5V12l3 2") }
    val Hamburger by lazy { icon("M4 7h16M4 12h16M4 17h16") }
    val Wave by lazy { icon("M3 12c2-6 4-6 6 0s4 6 6 0 4-6 6 0") }
    val Star by lazy { icon("M12 3.5l2.6 5.3 5.9.9-4.2 4.1 1 5.8-5.3-2.8-5.3 2.8 1-5.8-4.2-4.1 5.9-.9z") }
    val Update by lazy { icon("M12 19V6M6.5 11.5L12 6l5.5 5.5M5 20h14") }
    val Mail by lazy { icon("M4 6h16v12H4zM4.5 7l7.5 6 7.5-6") }
    val Globe by lazy { icon(circle(12f, 12f, 9f) + "M3 12h18M12 3c3.2 3 3.2 15 0 18M12 3c-3.2 3-3.2 15 0 18") }
    val Power by lazy { icon("M12 3v8M7 6.5a7 7 0 1 0 10 0") }
    val Save by lazy { icon("M5 4h11l3 3v13H5zM8 4v5h7V4M8 20v-6h8v6") }
}

@Composable
fun Icn(icon: ImageVector, color: Color, size: Dp = 20.dp, modifier: Modifier = Modifier) {
    Image(
        imageVector = icon,
        contentDescription = null,
        modifier = modifier.size(size),
        colorFilter = ColorFilter.tint(color),
    )
}
