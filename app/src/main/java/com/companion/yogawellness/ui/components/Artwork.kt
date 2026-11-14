package com.companion.yogawellness.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import com.companion.yogawellness.ui.theme.AppTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.companion.yogawellness.R
import com.companion.yogawellness.scoring.Asana
import com.companion.yogawellness.ui.theme.Brand

/** Soft glows, concentric rings and a dotted grid layered on the hero gradient. */
@Composable
fun HeroDecor(modifier: Modifier = Modifier, intensity: Float = 1f) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val glowR = w * 0.75f
        drawCircle(
            Brush.radialGradient(listOf(Brand.Teal400.copy(alpha = 0.30f * intensity), Color.Transparent), Offset(w * 0.95f, 0f), glowR),
            radius = glowR, center = Offset(w * 0.95f, 0f)
        )
        val glow2 = w * 0.55f
        drawCircle(
            Brush.radialGradient(listOf(Brand.Cyan.copy(alpha = 0.12f * intensity), Color.Transparent), Offset(0f, h), glow2),
            radius = glow2, center = Offset(0f, h)
        )
        val ringCenter = Offset(w * 0.92f, h * 0.08f)
        for (i in 1..4) {
            drawCircle(
                Color.White.copy(alpha = 0.07f * intensity / i),
                radius = 52.dp.toPx() * i, center = ringCenter, style = Stroke(1.dp.toPx())
            )
        }
        // Dot grid, top-left — a quiet "instrument panel" texture
        val step = 14.dp.toPx()
        val dot = 1.1.dp.toPx()
        for (r in 0 until 5) for (c in 0 until 7) {
            drawCircle(Color.White.copy(alpha = 0.09f * intensity), dot, Offset(18.dp.toPx() + c * step, h * 0.55f + r * step))
        }
    }
}

/**
 * Electrocardiogram-style pulse line. A bright highlight travels along the trace when [animate]
 * is on (infinite animations are suspended in UI tests, leaving the static trace).
 */
@Composable
fun PulseLine(modifier: Modifier = Modifier, color: Color = Brand.Mint, animate: Boolean = true, periodDp: Dp = 150.dp) {
    val transition = rememberInfiniteTransition(label = "pulse")
    // Read only inside the draw lambda, so the trace redraws each frame without recomposing.
    val phase = transition.animateFloat(
        0.35f, 1.35f, infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart), label = "phase"
    )
    Canvas(modifier) {
        val path = ecgPath(size.width, size.height, periodDp.toPx())
        drawPath(path, color.copy(alpha = 0.22f), style = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        val x = size.width * (if (animate) phase.value % 1f else 0.35f)
        val band = size.width * 0.22f
        drawPath(
            path,
            Brush.horizontalGradient(
                0f to Color.Transparent,
                ((x - band) / size.width).coerceIn(0f, 1f) to Color.Transparent,
                (x / size.width).coerceIn(0f, 1f) to color,
                ((x + 2) / size.width).coerceIn(0f, 1f) to Color.Transparent,
                1f to Color.Transparent
            ),
            style = Stroke(2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

private fun ecgPath(w: Float, h: Float, period: Float): Path {
    val mid = h * 0.62f
    val amp = h * 0.9f
    val p = Path()
    p.moveTo(0f, mid)
    var x0 = 0f
    while (x0 < w) {
        fun at(f: Float) = x0 + period * f
        p.lineTo(at(0.30f), mid)
        p.quadraticTo(at(0.35f), mid - amp * 0.12f, at(0.40f), mid)          // P wave
        p.lineTo(at(0.46f), mid)
        p.lineTo(at(0.49f), mid + amp * 0.08f)                                 // Q
        p.lineTo(at(0.525f), mid - amp * 0.62f)                                // R
        p.lineTo(at(0.56f), mid + amp * 0.22f)                                 // S
        p.lineTo(at(0.59f), mid)
        p.lineTo(at(0.68f), mid)
        p.quadraticTo(at(0.75f), mid - amp * 0.20f, at(0.82f), mid)           // T wave
        p.lineTo(at(1f), mid)
        x0 += period
    }
    return p
}

/** The launcher artwork on its navy disc — used as the in-app brand mark. */
@Composable
fun BrandMark(modifier: Modifier = Modifier, size: Dp = 44.dp, ring: Boolean = true) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Color(0xFF0D2E35), Color(0xFF0A3B44), Color(0xFF061E23))))
            .then(if (ring) Modifier.border(1.dp, Brand.Mint.copy(alpha = 0.45f), CircleShape) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        // The foreground art occupies the middle ~45% of its 108-unit canvas; scale it up to fill the disc.
        Image(
            painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.requiredSize(size * 1.75f)
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Pose pictograms — keypoint-style stick figures, echoing the live skeleton overlay.
// Coordinates are in a 100 × 100 box.
// ---------------------------------------------------------------------------------------------

private class Figure(
    val head: Offset,
    val headR: Float,
    val bones: List<Pair<Offset, Offset>>,
    /** The joint this pose is "about" — limb end, vertex, limb end — drawn as a measured angle. */
    val angle: Triple<Offset, Offset, Offset>? = null
) {
    val joints: List<Offset> = bones.flatMap { listOf(it.first, it.second) }.distinct()
    val minX: Float = joints.minOf { it.x }
    val maxX: Float = joints.maxOf { it.x }
    val floorY: Float = joints.maxOf { it.y }
}

private fun o(x: Number, y: Number) = Offset(x.toFloat(), y.toFloat())

/** A limb: joint → joint → joint. */
private fun chain(a: Offset, b: Offset, c: Offset): List<Pair<Offset, Offset>> = listOf(a to b, b to c)

private fun figureFor(asana: Asana): Figure = when (asana) {
    Asana.TADASANA -> {
        val ls = o(41, 28); val rs = o(59, 28); val lh = o(45, 56); val rh = o(55, 56)
        Figure(
            o(50, 15), 7f,
            listOf(ls to rs, lh to rh, ls to lh, rs to rh) +
                chain(ls, o(38, 42), o(37, 55)) + chain(rs, o(62, 42), o(63, 55)) +
                chain(lh, o(45, 75), o(45, 94)) + chain(rh, o(55, 75), o(55, 94))
        )
    }
    Asana.VRIKSHASANA -> {
        val ls = o(42, 38); val rs = o(58, 38); val lh = o(45, 62); val rh = o(55, 62)
        Figure(
            o(50, 27), 6.5f,
            listOf(ls to rs, lh to rh, ls to lh, rs to rh) +
                chain(ls, o(38, 23), o(48, 8)) + chain(rs, o(62, 23), o(52, 8)) +
                chain(lh, o(45, 79), o(45, 96)) + chain(rh, o(68, 73), o(49, 72)),
            angle = Triple(rh, o(68, 73), o(49, 72))
        )
    }
    Asana.VIRABHADRASANA_II -> {
        val ls = o(46, 36); val rs = o(62, 36); val lh = o(49, 60); val rh = o(60, 60)
        Figure(
            o(54, 24), 6.5f,
            listOf(ls to rs, lh to rh, ls to lh, rs to rh) +
                chain(ls, o(29, 36), o(11, 36)) + chain(rs, o(78, 36), o(95, 36)) +
                chain(lh, o(28, 63), o(27, 94)) + chain(rh, o(74, 77), o(89, 94)),
            angle = Triple(lh, o(28, 63), o(27, 94))
        )
    }
    Asana.TRIKONASANA -> {
        val ls = o(31, 48); val rs = o(41, 36); val lh = o(50, 62); val rh = o(58, 54)
        Figure(
            o(27, 34), 6.5f,
            listOf(ls to rs, lh to rh, ls to lh, rs to rh) +
                chain(ls, o(29, 62), o(27, 76)) + chain(rs, o(42, 22), o(43, 7)) +
                chain(lh, o(36, 78), o(21, 94)) + chain(rh, o(70, 74), o(83, 94)),
            angle = Triple(ls, lh, o(36, 78))
        )
    }
    Asana.UTKATASANA -> {
        val s = o(48, 35); val hip = o(61, 62)
        Figure(
            o(37, 28), 6.5f,
            listOf(s to hip) + chain(s, o(44, 20), o(40, 5)) + chain(hip, o(37, 69), o(45, 94)) + listOf(o(45, 94) to o(34, 95)),
            angle = Triple(hip, o(37, 69), o(45, 94))
        )
    }
    Asana.VIRABHADRASANA_I -> {
        val s = o(46, 36); val hip = o(50, 60)
        Figure(
            o(46, 24), 6.5f,
            listOf(s to hip) + chain(s, o(44, 18), o(42, 4)) +
                chain(hip, o(32, 68), o(32, 94)) + chain(hip, o(70, 75), o(85, 94)),
            angle = Triple(hip, o(32, 68), o(32, 94))
        )
    }
    Asana.UTTHITA_PARSVAKONASANA -> {
        val ls = o(36, 45); val rs = o(48, 38); val lh = o(42, 62); val rh = o(56, 58)
        Figure(
            o(30, 35), 6.5f,
            listOf(ls to rs, lh to rh, ls to lh, rs to rh) +
                chain(rs, o(24, 25), o(12, 12)) + chain(ls, o(32, 65), o(30, 80)) +
                chain(lh, o(30, 66), o(30, 94)) + chain(rh, o(72, 75), o(86, 94)),
            angle = Triple(lh, o(30, 66), o(30, 94))
        )
    }
    Asana.ARDHA_CHANDRASANA -> {
        val s = o(35, 50); val hip = o(55, 50)
        Figure(
            o(26, 45), 6.5f,
            listOf(s to hip) + chain(s, o(35, 70), o(35, 92)) + chain(s, o(35, 30), o(35, 12)) +
                chain(hip, o(55, 72), o(55, 94)) + chain(hip, o(75, 50), o(92, 50)),
            angle = Triple(s, hip, o(55, 72))
        )
    }
    Asana.MALASANA -> {
        val s = o(50, 48); val lh = o(42, 70); val rh = o(58, 70)
        Figure(
            o(50, 36), 6.5f,
            listOf(s to lh, s to rh, lh to rh) +
                chain(s, o(42, 55), o(50, 58)) + chain(s, o(58, 55), o(50, 58)) +
                chain(lh, o(30, 74), o(42, 94)) + chain(rh, o(70, 74), o(58, 94)),
            angle = Triple(s, lh, o(30, 74))
        )
    }
    Asana.BADDHA_KONASANA -> {
        val s = o(50, 42); val hip = o(50, 72)
        Figure(
            o(50, 30), 6.5f,
            listOf(s to hip) + chain(s, o(40, 58), o(50, 72)) + chain(s, o(60, 58), o(50, 72)) +
                chain(hip, o(32, 78), o(50, 88)) + chain(hip, o(68, 78), o(50, 88)),
            angle = Triple(hip, o(32, 78), o(50, 88))
        )
    }
    Asana.SEATED_TWIST -> {
        val s = o(50, 40); val hip = o(50, 72)
        Figure(
            o(50, 28), 6.5f,
            listOf(s to hip) + chain(s, o(38, 55), o(35, 75)) + chain(s, o(62, 55), o(65, 75)) +
                chain(hip, o(36, 80), o(46, 88)) + chain(hip, o(64, 80), o(54, 88)),
            angle = Triple(s, hip, o(36, 80))
        )
    }
    Asana.NECK_RELEASES -> {
        val s = o(50, 45); val hip = o(50, 75)
        Figure(
            o(55, 32), 6.5f,
            listOf(s to hip) + chain(s, o(38, 60), o(38, 80)) + chain(s, o(62, 60), o(62, 80)) +
                chain(hip, o(40, 86), o(44, 94)) + chain(hip, o(60, 86), o(56, 94)),
            angle = Triple(hip, s, o(55, 32))
        )
    }
    Asana.ADHO_MUKHA_SVANASANA -> {
        val hip = o(52, 35); val s = o(36, 60); val feet = o(78, 88); val hands = o(22, 88)
        Figure(
            o(32, 67), 6.5f,
            listOf(hip to s) + chain(s, o(28, 75), hands) + chain(hip, o(65, 60), feet),
            angle = Triple(s, hip, o(65, 60))
        )
    }
    Asana.BALASANA -> {
        val hip = o(72, 72); val s = o(45, 70); val head = o(35, 78)
        Figure(
            head, 6.0f,
            listOf(hip to s, s to head) + chain(s, o(32, 80), o(22, 82)) + chain(hip, o(74, 82), o(68, 85)),
            angle = Triple(hip, s, head)
        )
    }
    Asana.VIPARITA_KARANI -> {
        val hip = o(32, 80); val s = o(60, 82); val feet = o(32, 28)
        Figure(
            o(72, 82), 6.5f,
            listOf(hip to s) + chain(s, o(55, 75), o(50, 75)) + chain(hip, o(32, 54), feet),
            angle = Triple(s, hip, feet)
        )
    }
    Asana.SAVASANA -> {
        val head = o(25, 78); val s = o(40, 78); val hip = o(60, 78); val feet = o(85, 78)
        Figure(
            head, 6.5f,
            listOf(head to s, s to hip, hip to feet) + chain(s, o(48, 84), o(55, 86)) + chain(hip, o(72, 80), feet)
        )
    }
}

private val FIGURES: Map<Asana, Figure> = Asana.entries.associateWith(::figureFor)

/**
 * Draws [asana] as a luminous biomechanical figure: glowing bones with a bright core, ring-shaped
 * joint markers and, with [ground], the floor it stands on. [measure] adds the gold angle arc at
 * the joint the pose is about; [breathing] lets the glow swell slowly (for one hero figure per screen).
 */
@Composable
fun PoseGlyph(
    asana: Asana,
    modifier: Modifier = Modifier,
    color: Color = Brand.Mint,
    jointColor: Color = Color.White,
    glow: Boolean = true,
    ground: Boolean = glow,
    measure: Boolean = false,
    breathing: Boolean = false
) {
    val fig = FIGURES.getValue(asana)
    val pulse = if (breathing) {
        rememberInfiniteTransition(label = "breath").animateFloat(
            0.75f, 1.25f, infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow"
        )
    } else null
    Canvas(modifier) {
        val s = minOf(size.width, size.height) / 100f
        val dx = (size.width - 100f * s) / 2f
        val dy = (size.height - 100f * s) / 2f
        fun m(p: Offset) = Offset(dx + p.x * s, dy + p.y * s)
        drawFigure(fig, ::m, s, color, jointColor, glow, ground, measure, pulse?.value ?: 1f)
    }
}

private val JointCore = Color(0xFF06182B)

private fun DrawScope.drawFigure(
    fig: Figure,
    m: (Offset) -> Offset,
    s: Float,
    color: Color,
    jointColor: Color,
    glow: Boolean,
    ground: Boolean,
    measure: Boolean,
    glowGain: Float
) {
    val bone = 3.2f * s
    if (ground) {
        // Floor: a soft contact shadow and a hairline that fades out at both ends.
        val y = m(Offset(0f, fig.floorY + 2.2f)).y
        val x0 = m(Offset(fig.minX - 12f, 0f)).x
        val x1 = m(Offset(fig.maxX + 12f, 0f)).x
        drawOval(
            Brush.radialGradient(listOf(color.copy(alpha = 0.22f), Color.Transparent), Offset((x0 + x1) / 2f, y), (x1 - x0) / 2f),
            Offset(x0, y - 3f * s), Size(x1 - x0, 6f * s)
        )
        drawLine(
            Brush.horizontalGradient(0f to Color.Transparent, 0.5f to color.copy(alpha = 0.55f), 1f to Color.Transparent, startX = x0, endX = x1),
            Offset(x0, y), Offset(x1, y), strokeWidth = 0.7f * s
        )
    }
    if (glow) {
        for ((a, b) in fig.bones) {
            drawLine(color.copy(alpha = (0.07f * glowGain).coerceAtMost(1f)), m(a), m(b), strokeWidth = bone * 4.6f, cap = StrokeCap.Round)
            drawLine(color.copy(alpha = (0.16f * glowGain).coerceAtMost(1f)), m(a), m(b), strokeWidth = bone * 2.3f, cap = StrokeCap.Round)
        }
        drawCircle(color.copy(alpha = (0.14f * glowGain).coerceAtMost(1f)), fig.headR * s * 1.7f, m(fig.head))
    }
    for ((a, b) in fig.bones) drawLine(color, m(a), m(b), strokeWidth = bone, cap = StrokeCap.Round)
    // A bright core down the middle of each bone makes the line read as lit, not flat.
    if (glow) for ((a, b) in fig.bones) drawLine(Color.White.copy(alpha = 0.50f), m(a), m(b), strokeWidth = bone * 0.26f, cap = StrokeCap.Round)

    if (measure) fig.angle?.let { (p, v, q) ->
        val c = m(v)
        val a0 = Math.toDegrees(kotlin.math.atan2((m(p).y - c.y).toDouble(), (m(p).x - c.x).toDouble())).toFloat()
        val a1 = Math.toDegrees(kotlin.math.atan2((m(q).y - c.y).toDouble(), (m(q).x - c.x).toDouble())).toFloat()
        var sweep = a1 - a0
        if (sweep > 180f) sweep -= 360f
        if (sweep < -180f) sweep += 360f
        val r = 10.5f * s
        drawArc(Brand.Gold.copy(alpha = 0.16f), a0, sweep, true, Offset(c.x - r, c.y - r), Size(2 * r, 2 * r))
        drawArc(Brand.Gold, a0, sweep, false, Offset(c.x - r, c.y - r), Size(2 * r, 2 * r), style = Stroke(1.1f * s, cap = StrokeCap.Round))
    }

    // Head: lit disc with a highlight toward the light.
    val head = m(fig.head)
    val hr = fig.headR * s
    drawCircle(
        Brush.radialGradient(listOf(Color.White.copy(alpha = 0.95f), color), Offset(head.x - hr * 0.35f, head.y - hr * 0.4f), hr * 1.5f),
        hr, head
    )
    // Joints: ring markers, like the live overlay's keypoints.
    for (j in fig.joints) {
        val c = m(j)
        drawCircle(JointCore, 2.9f * s, c)
        drawCircle(jointColor, 2.9f * s, c, style = Stroke(1.0f * s))
        drawCircle(color, 1.0f * s, c)
    }
}

/**
 * The illustration stage: a deep-navy panel with a pool of teal light and a faint perspective
 * floor. Every pose figure in the app stands on one of these, so the artwork reads as one set.
 */
@Composable
fun PoseStage(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    /** Where the light pools, as a fraction of the panel. */
    light: Offset = Offset(0.5f, 0.52f),
    floor: Boolean = true,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit
) {
    val c = AppTheme.colors
    Box(
        modifier
            .clip(shape)
            .background(c.stageBrush)
            .drawBehind {
                val centre = Offset(size.width * light.x, size.height * light.y)
                val r = maxOf(size.width, size.height) * 0.62f
                drawCircle(Brush.radialGradient(listOf(c.stageGlow.copy(alpha = 0.30f), c.stageGlow.copy(alpha = 0.06f), Color.Transparent), centre, r), r, centre)
                if (floor) {
                    // Perspective floor: lines closing up toward the horizon, rays fanning from the vanishing point.
                    val horizon = size.height * 0.70f
                    val line = Color.White.copy(alpha = 0.055f)
                    var gap = size.height * 0.035f
                    var y = horizon
                    while (y < size.height) {
                        drawLine(line, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                        y += gap
                        gap *= 1.55f
                    }
                    val vx = size.width / 2f
                    for (k in -4..4) {
                        drawLine(line, Offset(vx + k * size.width * 0.07f, horizon), Offset(vx + k * size.width * 0.34f, size.height), strokeWidth = 1f)
                    }
                    drawRect(
                        Brush.verticalGradient(listOf(c.stageBottom.copy(alpha = 0f), c.stageBottom.copy(alpha = 0.55f)), startY = horizon, endY = size.height),
                        topLeft = Offset(0f, horizon), size = Size(size.width, size.height - horizon)
                    )
                }
            }
            .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.03f))), shape),
        contentAlignment = contentAlignment,
        content = content
    )
}
