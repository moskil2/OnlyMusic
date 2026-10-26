package app.onlymusic.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Raised round control with a soft top light, a deep shadow and a pressed state. */
@Composable
fun RaisedButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    size: Dp = 48.dp,
    iconSize: Dp = 20.dp,
    tint: Color = Pal.Text,
    glow: Boolean = false,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, tween(120), label = "press")
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .drawBehind {
                if (glow && enabled) {
                    val r = this.size.minDimension * 0.95f
                    drawCircle(Brush.radialGradient(listOf(Pal.Orange.copy(alpha = 0.38f), Color.Transparent), center, r), r)
                }
            }
            .shadow(if (pressed) 2.dp else 10.dp, CircleShape, ambientColor = Color.Black, spotColor = Color.Black)
            .background(Brush.linearGradient(listOf(Pal.ButtonTop, Pal.ButtonBottom)), CircleShape)
            .drawBehind {
                val r = this.size.minDimension / 2f
                if (pressed) {
                    drawCircle(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent)), r)
                } else {
                    drawCircle(Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.35f)), r)
                    drawCircle(
                        Brush.verticalGradient(0f to Color.White.copy(alpha = 0.16f), 0.45f to Color.Transparent),
                        r - 0.5.dp.toPx(),
                        style = Stroke(1.dp.toPx()),
                    )
                }
            }
            .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape)
            .clickable(interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icn(icon, if (enabled) tint else Pal.Disabled, iconSize)
    }
}

/** The gem-like play button: signature gradient, gloss on top, colored shadow and glow. */
@Composable
fun PlayButton(isPlaying: Boolean, enabled: Boolean, onClick: () -> Unit, size: Dp = 72.dp) {
    val accent = LocalAccent.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.93f else 1f, tween(120), label = "playPress")
    val gradient = if (enabled) {
        Brush.linearGradient(accent.colors.let { if (it.size == 1) it + it else it })
    } else {
        Brush.linearGradient(listOf(Color(0xFF2A1D33), Color(0xFF170F1D)))
    }
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .drawBehind {
                if (enabled) {
                    val r = this.size.minDimension * 0.9f
                    drawCircle(Brush.radialGradient(listOf(Pal.Orange.copy(alpha = 0.28f), Color.Transparent), center, r), r)
                    drawCircle(Color.White.copy(alpha = 0.035f), this.size.minDimension / 2f + 6.dp.toPx())
                }
            }
            .shadow(
                if (pressed || !enabled) 4.dp else 18.dp,
                CircleShape,
                ambientColor = Pal.Burgundy,
                spotColor = Pal.Burgundy,
            )
            .background(gradient, CircleShape)
            .drawBehind {
                if (!enabled) return@drawBehind
                val r = this.size.minDimension / 2f
                if (pressed) {
                    drawCircle(Brush.verticalGradient(listOf(Color(0x99460A28), Color.Transparent)), r)
                } else {
                    drawCircle(
                        Brush.radialGradient(
                            listOf(Color.White.copy(alpha = 0.42f), Color.Transparent),
                            center = Offset(this.size.width * 0.32f, this.size.height * 0.08f),
                            radius = this.size.width * 0.55f,
                        ),
                        r,
                    )
                    drawCircle(Brush.verticalGradient(0.6f to Color.Transparent, 1f to Color(0x80460A28)), r)
                }
            }
            .clickable(interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = if (isPlaying) "Pause" else "Play" },
        contentAlignment = Alignment.Center,
    ) {
        Icn(if (isPlaying) Ic.Pause else Ic.Play, if (enabled) Color.White else Pal.Disabled, size * 0.39f)
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, height: Dp = 48.dp, icon: ImageVector? = null) {
    val accent = LocalAccent.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, tween(120), label = "primaryPress")
    Row(
        modifier = modifier
            .height(height)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(if (pressed) 3.dp else 12.dp, RoundedCornerShape(height / 2), ambientColor = Pal.Burgundy, spotColor = Pal.Burgundy)
            .background(Brush.linearGradient(accent.colors.let { if (it.size == 1) it + it else it }), RoundedCornerShape(height / 2))
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.28f), Color.Transparent),
                        center = Offset(this.size.width * 0.3f, 0f),
                        radius = this.size.width * 0.6f,
                    )
                )
            }
            .clickable(interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icn(icon, Color.White, 16.dp)
            Spacer(Modifier.width(8.dp))
        }
        Txt(text, size = 15.sp, color = Color.White, weight = FontWeight.SemiBold)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 38.dp,
    icon: ImageVector? = null,
    textSize: androidx.compose.ui.unit.TextUnit = 13.sp,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = RoundedCornerShape(height / 2)
    Row(
        modifier = modifier
            .height(height)
            .shadow(if (pressed) 1.dp else 6.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
            .background(Brush.linearGradient(listOf(Pal.ButtonTop, Pal.ButtonBottom)), shape)
            .border(1.dp, Color.White.copy(alpha = if (pressed) 0.02f else 0.06f), shape)
            .clickable(interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icn(icon, Pal.Amber, 16.dp)
            Spacer(Modifier.width(8.dp))
        }
        Txt(text, size = textSize, weight = FontWeight.Medium)
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(17.dp)
    Box(
        modifier = Modifier
            .height(34.dp)
            .clip(shape)
            .background(
                if (selected) Brush.linearGradient(listOf(Pal.PillOnStart, Pal.PillOnEnd)) else SolidColor(Pal.Card),
                shape,
            )
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Txt(text, size = 13.sp, color = if (selected) Pal.Text else Pal.Text2, weight = FontWeight.Medium)
    }
}

@Composable
fun Toggle(checked: Boolean) {
    val accent = LocalAccent.current
    val x by animateFloatAsState(if (checked) 21f else 3f, tween(150), label = "toggle")
    Box(
        modifier = Modifier
            .size(44.dp, 26.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(if (checked) Brush.linearGradient(accent.colors.let { if (it.size == 1) it + it else it }) else SolidColor(Pal.Line))
    ) {
        Box(
            Modifier
                .offset(x.dp, 3.dp)
                .size(20.dp)
                .shadow(2.dp, CircleShape)
                .background(if (checked) Color.White else Pal.Text3, CircleShape)
        )
    }
}

@Composable
fun SettingSwitch(label: String, hint: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Switch) { onChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Txt(label, size = 14.sp)
            Txt(hint, size = 12.sp, color = Pal.Text2, maxLines = 2, lineHeight = 16.sp)
        }
        Spacer(Modifier.width(16.dp))
        Toggle(checked)
    }
}

/** Header used by every screen except the player. */
@Composable
fun ScreenHeader(title: String, subtitle: String?, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(60.dp)
            .padding(start = 4.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTap(Ic.Back, "Back", onBack, tint = Pal.Text)
        Column(Modifier.weight(1f)) {
            Txt(title, size = 19.sp, weight = FontWeight.SemiBold)
            if (subtitle != null) Txt(subtitle, size = 12.sp, color = Pal.Text3)
        }
        actions()
    }
}

@Composable
fun IconTap(icon: ImageVector, label: String, onClick: () -> Unit, tint: Color = Pal.Text2, size: Dp = 44.dp, iconSize: Dp = 20.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icn(icon, tint, iconSize)
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Txt(text, modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp), size = 12.sp, color = Pal.Text3, weight = FontWeight.Medium)
}

/** Track row shared by the queue, library, search and playlists. */
@Composable
fun TrackRow(
    title: String,
    subtitle: String,
    duration: String,
    current: Boolean,
    onClick: () -> Unit,
    onMore: (() -> Unit)?,
    leading: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    query: String = "",
    trailing: (@Composable () -> Unit)? = null,
    height: Dp = 52.dp,
) {
    val accent = LocalAccent.current
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .then(
                if (current) {
                    Modifier
                        .shadow(6.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0x389B6CFF), Color(0x29A8326E), Color(0x1FF28C4B)),
                            ),
                            shape,
                        )
                } else Modifier
            )
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(start = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            Box(Modifier.width(32.dp), contentAlignment = Alignment.CenterStart) { leading() }
        }
        Column(Modifier.weight(1f)) {
            Txt(highlight(title, query), size = 14.sp, weight = if (current) FontWeight.SemiBold else FontWeight.Normal)
            Txt(highlight(subtitle, query, bold = false), size = 12.sp, color = Pal.Text2)
        }
        val hasEnd = trailing != null || onMore != null
        Txt(duration, modifier = Modifier.padding(start = 12.dp, end = if (hasEnd) 0.dp else 12.dp), size = 12.sp, color = if (current) accent.text else Pal.Text3)
        when {
            trailing != null -> trailing()
            onMore != null -> IconTap(Ic.More, "More options for $title", onMore, tint = Pal.Text3, size = 42.dp, iconSize = 16.dp)
        }
    }
}

/** Drag-to-reorder for lists with rows of one fixed height. */
class ReorderState(private val itemHeightPx: Float) {
    var dragging by mutableIntStateOf(-1)
    var offset by mutableFloatStateOf(0f)

    fun target(count: Int): Int =
        (dragging + (offset / itemHeightPx).roundToInt()).coerceIn(0, (count - 1).coerceAtLeast(0))

    fun shiftFor(index: Int, count: Int): Float {
        if (dragging < 0 || index == dragging) return 0f
        val t = target(count)
        return when {
            index in (dragging + 1)..t -> -itemHeightPx
            index in t until dragging -> itemHeightPx
            else -> 0f
        }
    }
}

@Composable
fun rememberReorderState(itemHeight: Dp): ReorderState {
    val px = with(LocalDensity.current) { itemHeight.toPx() }
    return remember(px) { ReorderState(px) }
}

/** Moves the row with the finger while it is dragged and makes room in the other rows. */
fun Modifier.reorderable(state: ReorderState, index: Int, count: Int, lineColor: Color): Modifier = this
    .zIndex(if (state.dragging == index) 1f else 0f)
    .graphicsLayer {
        if (state.dragging == index) {
            translationY = state.offset
            rotationZ = -0.8f
            scaleX = 1.02f
            scaleY = 1.02f
        } else {
            translationY = state.shiftFor(index, count)
        }
    }
    .drawBehind {
        if (state.dragging == index) {
            val y = -4.dp.toPx()
            drawLine(lineColor, Offset(14.dp.toPx(), y), Offset(size.width - 14.dp.toPx(), y), 2.dp.toPx())
        }
    }

/** Hold the handle, then drag. [onMove] gets the old and the new position. */
fun Modifier.dragHandle(state: ReorderState, index: Int, count: Int, onMove: (Int, Int) -> Unit): Modifier =
    this.pointerInput(index, count) {
        detectDragGesturesAfterLongPress(
            onDragStart = {
                state.dragging = index
                state.offset = 0f
            },
            onDragEnd = {
                val to = state.target(count)
                val from = state.dragging
                state.dragging = -1
                state.offset = 0f
                if (from >= 0 && to != from) onMove(from, to)
            },
            onDragCancel = {
                state.dragging = -1
                state.offset = 0f
            },
        ) { change, amount ->
            change.consume()
            state.offset += amount.y
        }
    }

@Composable
fun DragHandle(modifier: Modifier, active: Boolean) {
    Box(modifier.size(40.dp, 52.dp), contentAlignment = Alignment.Center) {
        Icn(Ic.Grip, if (active) Pal.Text else Pal.Disabled, 16.dp)
    }
}

class MenuAction(val label: String, val icon: ImageVector, val onClick: () -> Unit)

/** Bottom sheet with a title and a list of actions. */
@Composable
fun ActionSheet(title: String, subtitle: String, actions: List<MenuAction>, onDismiss: () -> Unit) {
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
                .shadow(24.dp, RoundedCornerShape(28.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF221630), Color(0xFF1A0F1E))), RoundedCornerShape(28.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .padding(top = 8.dp, bottom = 16.dp),
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).size(36.dp, 4.dp).background(Pal.Disabled, RoundedCornerShape(2.dp)))
            Column(Modifier.padding(start = 22.dp, end = 22.dp, top = 12.dp, bottom = 10.dp)) {
                Txt(title, size = 15.sp, weight = FontWeight.SemiBold)
                if (subtitle.isNotEmpty()) Txt(subtitle, size = 12.sp, color = Pal.Text2)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.06f)))
            actions.forEach { a ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clickable { a.onClick(); onDismiss() }
                        .padding(horizontal = 22.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icn(a.icon, Pal.Amber, 18.dp)
                    Spacer(Modifier.width(16.dp))
                    Txt(a.label, size = 14.sp)
                }
            }
        }
    }
}

/** Simple text prompt, used for new and renamed playlists. */
@Composable
fun TextPrompt(title: String, initial: String, confirm: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    BackHandler(onBack = onDismiss)
    var value by remember { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xAD08040A))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .shadow(24.dp, RoundedCornerShape(24.dp))
                .background(Pal.Raised, RoundedCornerShape(24.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Txt(title, size = 17.sp, weight = FontWeight.SemiBold)
            BasicTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                textStyle = TextStyle(color = Pal.Text, fontSize = 15.sp),
                cursorBrush = SolidColor(Pal.Amber),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus)
                    .background(Pal.Bg, RoundedCornerShape(12.dp))
                    .border(1.dp, Pal.Amber.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
                SecondaryButton("Cancel", onDismiss)
                PrimaryButton(confirm, { onConfirm(value) }, height = 38.dp)
            }
        }
    }
}

/** Small bar at the bottom of secondary screens that leads back to the player. */
@Composable
fun MiniPlayer(title: String, subtitle: String, isPlaying: Boolean, progress: Float, onOpen: () -> Unit, onToggle: () -> Unit) {
    val accent = LocalAccent.current
    val shape = RoundedCornerShape(18.dp)
    Box(
        Modifier
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .fillMaxWidth()
            .height(64.dp)
            .shadow(14.dp, shape)
            .clip(shape)
            .background(Pal.Raised)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        listOf(Color(0x477046E6), Color.Transparent),
                        center = Offset(size.width * 0.15f, size.height * 0.5f),
                        radius = size.width * 0.5f,
                    )
                )
                val y = size.height - 1.dp.toPx()
                drawLine(Pal.Line, Offset(14.dp.toPx(), y), Offset(size.width - 14.dp.toPx(), y), 2.dp.toPx())
                val w = (size.width - 28.dp.toPx()) * progress.coerceIn(0f, 1f)
                drawLine(
                    Brush.horizontalGradient(accent.colors.let { if (it.size == 1) it + it else it }),
                    Offset(14.dp.toPx(), y),
                    Offset(14.dp.toPx() + w, y),
                    2.dp.toPx(),
                )
            }
            .clickable(onClick = onOpen)
            .padding(start = 16.dp, end = 10.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(40.dp, 24.dp)) {
                val p1 = Path()
                val p2 = Path()
                for (k in 0..20) {
                    val u = k / 20f
                    val x = size.width * u
                    val env = sin(u * PI.toFloat())
                    val y1 = size.height / 2f - size.height * 0.38f * env * sin(u * 13f)
                    val y2 = size.height / 2f - size.height * 0.26f * env * sin(u * 13f + 1.2f)
                    if (k == 0) { p1.moveTo(x, y1); p2.moveTo(x, y2) } else { p1.lineTo(x, y1); p2.lineTo(x, y2) }
                }
                drawPath(p1, Color(0xFFB794FF), style = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round))
                drawPath(p2, Pal.Orange, alpha = 0.7f, style = Stroke(1.dp.toPx(), cap = StrokeCap.Round))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Txt(title, size = 14.sp, weight = FontWeight.SemiBold)
                Txt(subtitle, size = 12.sp, color = Pal.Text2)
            }
            RaisedButton(if (isPlaying) Ic.Pause else Ic.Play, if (isPlaying) "Pause" else "Play", onToggle, size = 44.dp, iconSize = 18.dp)
        }
    }
}
