package ng.name.nsikak.kidquest.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import ng.name.nsikak.kidquest.ui.theme.Cream
import ng.name.nsikak.kidquest.ui.theme.CreamLip
import ng.name.nsikak.kidquest.ui.theme.Gold
import ng.name.nsikak.kidquest.ui.theme.GoldLip
import ng.name.nsikak.kidquest.ui.theme.Ink
import ng.name.nsikak.kidquest.ui.theme.Leather
import ng.name.nsikak.kidquest.ui.theme.LilitaOne
import ng.name.nsikak.kidquest.ui.theme.LockedFace
import ng.name.nsikak.kidquest.ui.theme.Night1
import ng.name.nsikak.kidquest.ui.theme.Night2
import ng.name.nsikak.kidquest.ui.theme.Pink
import ng.name.nsikak.kidquest.ui.theme.PinkLip
import ng.name.nsikak.kidquest.ui.theme.Sky
import ng.name.nsikak.kidquest.ui.theme.SkyLip
import ng.name.nsikak.kidquest.ui.theme.SwordOutline
import ng.name.nsikak.kidquest.ui.theme.SwordShade
import ng.name.nsikak.kidquest.ui.theme.SwordSteel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Night-sky backdrop with a few twinkling dots. Every screen sits on this. */
@Composable
fun GameBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Night1, Night2))),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val random = Random(7)
            repeat(40) {
                val x = random.nextFloat() * size.width
                val y = random.nextFloat() * size.height
                val radius = (1f + random.nextFloat() * 2.5f) * density
                drawCircle(Color.White.copy(alpha = 0.10f + random.nextFloat() * 0.25f), radius, Offset(x, y))
            }
        }
        content()
    }
}

/** Lilita One lettering with a soft drop shadow, the "game logo" look. */
@Composable
fun GameText(
    text: String,
    modifier: Modifier = Modifier,
    size: TextUnit = 24.sp,
    color: Color = Color.White,
    align: TextAlign = TextAlign.Center,
    maxLines: Int = Int.MAX_VALUE,
    shadow: Boolean = true,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = size,
        fontFamily = LilitaOne,
        textAlign = align,
        maxLines = maxLines,
        lineHeight = size * 1.12f,
        style = if (shadow) TextStyle(shadow = Shadow(Color(0x66000000), Offset(0f, 4f), 0f)) else TextStyle.Default,
    )
}

/**
 * The basic candy-style pressable shape: a coloured face sitting on a darker lip.
 * Pressing pushes the face down onto the lip.
 */
@Composable
fun ChunkySurface(
    face: Color,
    lip: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    corner: Dp = 20.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val lipHeight = 6.dp
    val drop by animateDpAsState(if (pressed && onClick != null) lipHeight else 0.dp, label = "press")
    val shape = RoundedCornerShape(corner)
    val clickModifier = if (onClick != null) {
        Modifier.clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick)
    } else {
        Modifier
    }
    Box(modifier.then(clickModifier)) {
        Box(
            Modifier
                .matchParentSize()
                .padding(top = lipHeight)
                .background(lip, shape),
        )
        Box(
            Modifier
                .padding(bottom = lipHeight)
                .offset(y = drop)
                .background(face, shape)
                .drawBehind {
                    // The little shine across the top that makes it look like glossy candy.
                    drawRoundRect(
                        Color.White.copy(alpha = 0.22f),
                        topLeft = Offset(10.dp.toPx(), 5.dp.toPx()),
                        size = Size(size.width - 20.dp.toPx(), size.height * 0.26f),
                        cornerRadius = CornerRadius(12.dp.toPx()),
                    )
                },
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

@Composable
fun ChunkyButton(
    text: String,
    face: Color,
    lip: Color,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White,
    size: TextUnit = 24.sp,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    ChunkySurface(
        face = if (enabled) face else LockedFace,
        lip = if (enabled) lip else Color(0xFF47427F),
        modifier = modifier,
        enabled = enabled,
        onClick = onClick,
    ) {
        GameText(
            text = text,
            size = size,
            color = textColor,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        )
    }
}

/** A cream "paper" card with a lip, for text and information. */
@Composable
fun CandyCard(
    modifier: Modifier = Modifier,
    color: Color = Cream,
    lip: Color = CreamLip,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier) {
        Box(
            Modifier
                .matchParentSize()
                .padding(top = 6.dp)
                .background(lip, RoundedCornerShape(24.dp)),
        )
        Column(
            modifier = Modifier
                .padding(bottom = 6.dp)
                .fillMaxWidth()
                .background(color, RoundedCornerShape(24.dp))
                .padding(16.dp),
            content = content,
        )
    }
}

/** A selectable pill, used for choices like "Easy / Medium / Hard". */
@Composable
fun OptionChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedFace: Color = Gold,
    selectedLip: Color = GoldLip,
) {
    ChunkySurface(
        face = if (selected) selectedFace else Color(0xFF5B4FC4),
        lip = if (selected) selectedLip else Color(0xFF362C94),
        modifier = modifier,
        corner = 16.dp,
        onClick = onClick,
    ) {
        GameText(
            text = text,
            size = 18.sp,
            color = if (selected) Ink else Color.White,
            shadow = !selected,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

fun DrawScope.drawStar(center: Offset, outer: Float, color: Color) {
    val inner = outer * 0.45f
    val path = Path()
    for (i in 0 until 10) {
        val radius = if (i % 2 == 0) outer else inner
        val angle = -PI / 2 + i * PI / 5
        val x = center.x + (radius * cos(angle)).toFloat()
        val y = center.y + (radius * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
}

/** Up to [total] stars; the first [lit] are gold and pop in with a little bounce. */
@Composable
fun StarRow(lit: Int, modifier: Modifier = Modifier, total: Int = 3, starSize: Dp = 44.dp) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        for (i in 0 until total) {
            val on = i < lit
            val scale by animateFloatAsState(
                targetValue = if (on) 1f else 0.8f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "star",
            )
            Canvas(Modifier.size(starSize).scale(scale)) {
                val c = Offset(size.width / 2, size.height / 2 + size.height * 0.04f)
                if (on) {
                    drawStar(c.copy(y = c.y + 2.dp.toPx()), size.minDimension / 2f, GoldLip)
                    drawStar(c, size.minDimension / 2f, Gold)
                } else {
                    drawStar(c, size.minDimension / 2f, Color(0x40FFFFFF))
                }
            }
        }
    }
}

/** The sword, drawn upright. Rotate it with Modifier.rotate. */
@Composable
fun SwordGraphic(
    modifier: Modifier = Modifier,
    guard: Color = Gold,
    grip: Color = Leather,
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2
        val blade = Path().apply {
            moveTo(cx, h * 0.02f)
            lineTo(cx + w * 0.11f, h * 0.14f)
            lineTo(cx + w * 0.11f, h * 0.62f)
            lineTo(cx - w * 0.11f, h * 0.62f)
            lineTo(cx - w * 0.11f, h * 0.14f)
            close()
        }
        drawPath(blade, SwordSteel)
        drawPath(blade, SwordOutline, style = Stroke(width = w * 0.015f, join = StrokeJoin.Round))
        drawRect(SwordShade, Offset(cx - w * 0.025f, h * 0.14f), Size(w * 0.05f, h * 0.46f))
        drawRect(Color.White.copy(alpha = 0.7f), Offset(cx - w * 0.09f, h * 0.16f), Size(w * 0.03f, h * 0.44f))
        drawRoundRect(GoldLip, Offset(cx - w * 0.28f, h * 0.625f), Size(w * 0.56f, h * 0.075f), CornerRadius(w * 0.035f))
        drawRoundRect(guard, Offset(cx - w * 0.28f, h * 0.615f), Size(w * 0.56f, h * 0.07f), CornerRadius(w * 0.035f))
        drawCircle(Pink, w * 0.03f, Offset(cx, h * 0.65f))
        drawRoundRect(grip, Offset(cx - w * 0.05f, h * 0.69f), Size(w * 0.10f, h * 0.20f), CornerRadius(w * 0.03f))
        drawCircle(GoldLip, w * 0.08f, Offset(cx, h * 0.925f))
        drawCircle(guard, w * 0.07f, Offset(cx, h * 0.92f))
    }
}

/** The activity logo shown before every round. */
@Composable
fun SwordLogo(modifier: Modifier = Modifier, animateIn: Boolean = false, swordSize: Dp = 150.dp) {
    var entered by remember { mutableStateOf(!animateIn) }
    LaunchedEffect(Unit) { entered = true }
    val scale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.3f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "logo",
    )
    val tilt by animateFloatAsState(
        targetValue = if (entered) 35f else -60f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "tilt",
    )
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(swordSize * 1.3f).scale(scale), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(Color(0x33FFC93C), size.minDimension / 2f)
                drawCircle(Color(0x33FFFFFF), size.minDimension / 2.6f)
            }
            SwordGraphic(Modifier.size(swordSize).rotate(tilt))
        }
        GameText("Draw Your Sword", size = 38.sp, color = Gold, modifier = Modifier.scale(scale))
    }
}

@Composable
fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    ChunkySurface(face = Sky, lip = SkyLip, modifier = modifier.size(56.dp), corner = 28.dp, onClick = onClick) {
        Canvas(Modifier.size(22.dp)) {
            val p = Path().apply {
                moveTo(size.width * 0.68f, size.height * 0.08f)
                lineTo(size.width * 0.22f, size.height * 0.5f)
                lineTo(size.width * 0.68f, size.height * 0.92f)
            }
            drawPath(p, Color.White, style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/** Back button on the left, a big title in the middle. */
@Composable
fun ScreenTitleBar(title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        BackButton(onBack)
        GameText(title, size = 30.sp, modifier = Modifier.weight(1f).padding(end = 56.dp), maxLines = 1)
    }
}

@Composable
fun PadlockIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawArc(
            color = Color.White,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.25f, h * 0.05f),
            size = Size(w * 0.5f, h * 0.7f),
            style = Stroke(width = w * 0.1f),
        )
        drawRoundRect(Color.White, Offset(w * 0.12f, h * 0.42f), Size(w * 0.76f, h * 0.5f), CornerRadius(w * 0.12f))
        drawCircle(LockedFace, w * 0.07f, Offset(w * 0.5f, h * 0.63f))
    }
}

/** A pop-up question card with two buttons. */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    cancelText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        CandyCard(Modifier.fillMaxWidth()) {
            GameText(title, size = 28.sp, color = Ink, shadow = false, modifier = Modifier.fillMaxWidth())
            Text(
                text = message,
                color = Ink,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ChunkyButton(cancelText, Sky, SkyLip, Modifier.weight(1f), size = 20.sp, onClick = onDismiss)
                ChunkyButton(confirmText, Pink, PinkLip, Modifier.weight(1f), size = 20.sp, onClick = onConfirm)
            }
        }
    }
}

/** "12.3" - seconds with one decimal, built by hand so phone language settings can't change the digits. */
fun formatSeconds(ms: Long): String {
    val tenths = ms / 100
    return "${tenths / 10}.${tenths % 10}"
}
