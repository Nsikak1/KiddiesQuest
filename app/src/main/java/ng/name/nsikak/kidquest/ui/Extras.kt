package ng.name.nsikak.kidquest.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ng.name.nsikak.kidquest.data.Speaker
import ng.name.nsikak.kidquest.ui.theme.Cherry
import ng.name.nsikak.kidquest.ui.theme.CherryLip
import ng.name.nsikak.kidquest.ui.theme.Gold
import ng.name.nsikak.kidquest.ui.theme.Grape
import ng.name.nsikak.kidquest.ui.theme.LockedFace
import ng.name.nsikak.kidquest.ui.theme.LockedLip
import ng.name.nsikak.kidquest.ui.theme.Mint
import ng.name.nsikak.kidquest.ui.theme.MintLip
import ng.name.nsikak.kidquest.ui.theme.Orange
import ng.name.nsikak.kidquest.ui.theme.Pink
import ng.name.nsikak.kidquest.ui.theme.Sky
import kotlin.math.sin
import kotlin.random.Random

private class Piece(
    val x: Float,
    val speed: Float,
    val phase: Float,
    val wobble: Float,
    val color: Color,
)

/** A burst of falling confetti that plays once (about three seconds) and then disappears. */
@Composable
fun Confetti(modifier: Modifier = Modifier, durationMs: Int = 3200) {
    var t by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        animate(0f, 1f, animationSpec = tween(durationMs, easing = LinearEasing)) { value, _ -> t = value }
    }
    val colours = remember { listOf(Gold, Pink, Mint, Sky, Orange, Grape) }
    val pieces = remember {
        List(70) { i ->
            val r = Random(i + 11)
            Piece(r.nextFloat(), 0.45f + r.nextFloat() * 0.8f, r.nextFloat() * 6.28f, 1f + r.nextFloat() * 2f, colours[i % colours.size])
        }
    }
    if (t < 1f) {
        Canvas(modifier.fillMaxSize()) {
            for (p in pieces) {
                val y = (-0.08f + t * p.speed * 1.3f) * size.height
                if (y > size.height) continue
                val x = (p.x + sin(t * 6f * p.wobble + p.phase) * 0.03f) * size.width
                rotate(degrees = t * 360f * p.wobble + p.phase * 57f, pivot = Offset(x, y)) {
                    drawRect(p.color, Offset(x - 7.dp.toPx(), y - 4.dp.toPx()), Size(14.dp.toPx(), 8.dp.toPx()))
                }
            }
        }
    }
}

/**
 * "Listen" / "Stop" button for the phone's voice. Pass [parts] to read several pieces one after another
 * (e.g. every verse of a chapter); otherwise [text] is read.
 */
@Composable
fun ListenButton(
    speaker: Speaker,
    key: String,
    text: String,
    modifier: Modifier = Modifier,
    parts: List<String>? = null,
) {
    val speaking = speaker.speakingKey == key
    if (speaker.unavailable) {
        ChunkySurface(face = LockedFace, lip = LockedLip, modifier = modifier, corner = 16.dp) {
            GameText("No voice on this phone", size = 15.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
        }
    } else {
        ChunkyButton(
            text = if (speaking) "Stop" else "Listen",
            face = if (speaking) Cherry else Mint,
            lip = if (speaking) CherryLip else MintLip,
            modifier = modifier,
            size = 18.sp,
            enabled = speaker.ready,
        ) {
            if (speaking) speaker.stop() else if (parts != null) speaker.speakParts(key, parts) else speaker.speak(key, text)
        }
    }
}

/** Text that lights up the word being read aloud (on Android 8 and newer) while the voice speaks it. */
@Composable
fun SpokenText(
    text: String,
    key: String,
    speaker: Speaker,
    color: Color,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    modifier: Modifier = Modifier,
    align: TextAlign = TextAlign.Center,
) {
    val start = speaker.rangeStart
    val end = speaker.rangeEnd
    val lit = speaker.speakingKey == key && start >= 0 && start < text.length
    val styled: AnnotatedString = if (lit) {
        buildAnnotatedString {
            append(text)
            addStyle(SpanStyle(background = Color(0xFFFFE08A)), start, minOf(end, text.length).coerceAtLeast(start))
        }
    } else {
        AnnotatedString(text)
    }
    Text(
        text = styled,
        color = color,
        fontSize = fontSize,
        lineHeight = lineHeight,
        fontFamily = FontFamily.Serif,
        textAlign = align,
        modifier = modifier,
    )
}
