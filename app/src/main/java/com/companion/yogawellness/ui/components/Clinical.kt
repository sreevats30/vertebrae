package com.companion.yogawellness.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.companion.yogawellness.ui.theme.AppTheme
import com.companion.yogawellness.ui.theme.Brand
import com.companion.yogawellness.ui.theme.OverlineStyle

// ---------------------------------------------------------------------------------------------
// Structure
// ---------------------------------------------------------------------------------------------

/**
 * Brand top bar: navy→teal gradient with rounded lower corners, drawn behind the status bar.
 * [bottom] hosts extras such as a progress bar or a summary row.
 */
@Composable
fun ClinicalTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    bottom: (@Composable ColumnScope.() -> Unit)? = null
) {
    val c = AppTheme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(c.heroBrush)
            .border(
                1.dp,
                Brush.verticalGradient(0f to Color.Transparent, 0.7f to Color.Transparent, 1f to Brand.Mint.copy(alpha = 0.22f)),
                RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
            )
    ) {
        HeroDecor(Modifier.matchParentSize(), intensity = 0.7f)
        Column(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = if (bottom != null) 18.dp else 14.dp)
        ) {
            Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
                } else {
                    Spacer(Modifier.width(8.dp))
                }
                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text(
                        title, style = MaterialTheme.typography.titleLarge, color = Color.White,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    subtitle?.let {
                        Text(
                            it, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.72f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { actions() }
            }
            if (bottom != null) {
                Column(Modifier.padding(horizontal = 10.dp).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    bottom()
                }
            }
        }
    }
}

/** Translucent circular icon button for use on gradients and the camera feed. */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    container: Color = Color.White.copy(alpha = 0.14f),
    size: Dp = 42.dp
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .minimumInteractiveComponentSize()
            .pressScale(interaction, 0.92f)
            .size(size)
            .clip(CircleShape)
            .background(container)
            .border(
                1.dp,
                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.30f), Color.White.copy(alpha = 0.08f))),
                CircleShape
            )
            .clickable(interactionSource = interaction, indication = androidx.compose.material3.ripple(), role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(size * 0.52f))
    }
}

/** Gentle spring press-scale for anything tappable. */
fun Modifier.pressScale(interaction: InteractionSource, pressedScale: Float = 0.975f): Modifier = composed {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) pressedScale else 1f, spring(dampingRatio = 0.72f, stiffness = 700f), label = "press"
    )
    graphicsLayer { scaleX = scale; scaleY = scale }
}

/** Shadow + tonal surface + lit top edge: the card treatment, for rows that manage their own content. */
fun Modifier.cardSurface(shape: Shape, elevation: Dp = 6.dp): Modifier = composed {
    val c = AppTheme.colors
    this
        .shadow(elevation, shape, ambientColor = c.shadow.copy(alpha = if (c.isDark) 0.30f else 0.06f), spotColor = c.shadow.copy(alpha = if (c.isDark) 0.55f else 0.14f))
        .clip(shape)
        .background(c.cardBrush)
        .border(1.dp, c.cardEdgeBrush, shape)
}

/**
 * The app's card: a tonal surface with a lit top edge and a soft shadow. [brush] turns it into a
 * feature panel (brand gradient); [glow] adds a quiet corner light in an accent colour so
 * important cards get their own identity without changing the surface.
 */
@Composable
fun PanelCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = Color.Unspecified,
    brush: Brush? = null,
    shape: Shape = MaterialTheme.shapes.large,
    borderColor: Color? = AppTheme.colors.cardBorder,
    elevation: Dp = 10.dp,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    spacing: Dp = 12.dp,
    glow: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val c = AppTheme.colors
    val fill: Brush = brush ?: if (color.isSpecified) SolidColor(color) else c.cardBrush
    val base = if (color.isSpecified) color else c.cardBottom
    val shadowed = if (elevation > 0.dp) {
        modifier.shadow(
            elevation, shape,
            ambientColor = c.shadow.copy(alpha = if (c.isDark) 0.30f else 0.06f),
            spotColor = c.shadow.copy(alpha = if (c.isDark) 0.55f else 0.14f)
        )
    } else modifier
    val border = when {
        // Feature panels catch light on their top edge.
        brush != null && borderColor == null ->
            BorderStroke(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.20f), Color.White.copy(alpha = 0.03f))))
        borderColor == null -> null
        borderColor == c.cardBorder -> BorderStroke(1.dp, c.cardEdgeBrush)
        else -> BorderStroke(1.dp, borderColor)
    }
    val inner: @Composable () -> Unit = {
        Column(
            Modifier
                .background(fill)
                .then(
                    if (glow != null) Modifier.drawBehind {
                        val r = size.width * 0.62f
                        val centre = Offset(size.width * 0.96f, 0f)
                        drawCircle(
                            Brush.radialGradient(listOf(glow.copy(alpha = if (c.isDark) 0.20f else 0.10f), Color.Transparent), centre, r),
                            r, centre
                        )
                    } else Modifier
                )
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(spacing),
            content = content
        )
    }
    if (onClick != null) {
        val interaction = remember { MutableInteractionSource() }
        Surface(
            onClick = onClick, modifier = shadowed.pressScale(interaction, 0.985f), shape = shape, color = base,
            border = border, interactionSource = interaction
        ) { inner() }
    } else {
        Surface(modifier = shadowed, shape = shape, color = base, border = border) { inner() }
    }
}

@Composable
fun Overline(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    Text(text.uppercase(), style = OverlineStyle, color = color, modifier = modifier)
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    overline: String? = null,
    action: (@Composable () -> Unit)? = null
) {
    Row(modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            overline?.let { Overline(it, color = AppTheme.colors.inkMuted) }
            Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        }
        action?.invoke()
    }
}

// ---------------------------------------------------------------------------------------------
// Atoms
// ---------------------------------------------------------------------------------------------

@Composable
fun IconBadge(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = size * 0.52f,
    shape: Shape = RoundedCornerShape(size * 0.32f),
    filled: Boolean = false
) {
    Box(
        modifier
            .size(size)
            .clip(shape)
            .background(
                if (filled) Brush.linearGradient(listOf(lighten(tint, 0.18f), tint, tint.copy(alpha = 0.82f)))
                else Brush.linearGradient(listOf(tint.copy(alpha = 0.22f), tint.copy(alpha = 0.09f)))
            )
            .border(
                1.dp,
                if (filled) Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.38f), Color.White.copy(alpha = 0.04f)))
                else Brush.verticalGradient(listOf(tint.copy(alpha = 0.34f), tint.copy(alpha = 0.06f))),
                shape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = if (filled) Color.White else tint, modifier = Modifier.size(iconSize))
    }
}

private fun lighten(c: Color, by: Float) = Color(
    red = c.red + (1f - c.red) * by, green = c.green + (1f - c.green) * by, blue = c.blue + (1f - c.blue) * by, alpha = c.alpha
)

@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    color: Color = MaterialTheme.colorScheme.primary,
    container: Color = color.copy(alpha = 0.12f),
    border: Color? = null,
    iconTint: Color = color
) {
    Row(
        modifier
            .clip(CircleShape)
            .background(container)
            .then(if (border != null) Modifier.border(1.dp, border, CircleShape) else Modifier)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        icon?.let { Icon(it, contentDescription = null, tint = iconTint, modifier = Modifier.size(14.dp)) }
        Text(text, style = MaterialTheme.typography.labelMedium, color = color, maxLines = 1)
    }
}

/** Frosted pill for text on the hero gradient or camera feed. */
@Composable
fun GlassPill(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, iconTint: Color = Brand.Mint) {
    Pill(
        text, modifier, icon,
        color = Color.White,
        container = Color.White.copy(alpha = 0.10f),
        border = Color.White.copy(alpha = 0.18f),
        iconTint = iconTint
    )
}

/**
 * Primary call to action: brand gradient with a lit top edge and a coloured shadow. Pressing
 * sinks it slightly and dims the sheen; the disabled state still reads clearly.
 */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    brush: Brush = AppTheme.colors.ctaBrush,
    contentColor: Color = Color.White,
    height: Dp = 54.dp
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val sheen by animateFloatAsState(if (pressed) 0.04f else 0.20f, label = "sheen")
    val shape = RoundedCornerShape(17.dp)
    val disabledBg = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    val fg = if (enabled) contentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f)
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        color = Color.Transparent,
        interactionSource = interaction,
        modifier = modifier
            .pressScale(interaction, 0.97f)
            .then(
                if (enabled) Modifier.shadow(14.dp, shape, ambientColor = Brand.Teal500.copy(alpha = 0.30f), spotColor = Brand.Teal500.copy(alpha = 0.55f))
                else Modifier
            )
            .height(height)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(if (enabled) brush else SolidColor(disabledBg))
                .then(
                    if (enabled) Modifier
                        .drawBehind {
                            // Sheen: light falling on the upper half.
                            drawRect(
                                Brush.verticalGradient(0f to Color.White.copy(alpha = sheen), 0.55f to Color.Transparent),
                                size = Size(size.width, size.height * 0.55f)
                            )
                        }
                        .border(
                            1.dp,
                            Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.42f), Color.White.copy(alpha = 0.04f))),
                            shape
                        )
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                Modifier.padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                icon?.let { Icon(it, contentDescription = null, tint = fg, modifier = Modifier.size(21.dp)) }
                Text(
                    text, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp)
                )
                trailingIcon?.let { Icon(it, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp)) }
            }
        }
    }
}

/** Secondary action: tinted fill in the accent colour. */
@Composable
fun SoftButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    color: Color = MaterialTheme.colorScheme.primary,
    container: Color = color.copy(alpha = 0.11f),
    enabled: Boolean = true,
    height: Dp = 48.dp
) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        color = container,
        contentColor = color,
        border = BorderStroke(1.dp, Brush.verticalGradient(listOf(color.copy(alpha = 0.30f), color.copy(alpha = 0.10f)))),
        interactionSource = interaction,
        modifier = modifier.pressScale(interaction).height(height)
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            icon?.let { Icon(it, contentDescription = null, modifier = Modifier.size(20.dp)) }
            Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Data display
// ---------------------------------------------------------------------------------------------

/**
 * Circular progress ring that sweeps in from zero, with a soft halo under the arc and a bright
 * marker at its leading end. [content] sits in the middle.
 */
@Composable
fun RingGauge(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    trackColor: Color = color.copy(alpha = 0.14f),
    stroke: Dp = 8.dp,
    brush: Brush? = null,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        anim.animateTo(progress.coerceIn(0f, 1f), tween(900, easing = FastOutSlowInEasing))
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val s = stroke.toPx()
            // Inset so the halo and the end marker stay inside the bounds.
            val inset = s * 0.9f
            val topLeft = Offset(inset, inset)
            val arc = Size(size.width - 2 * inset, size.height - 2 * inset)
            drawArc(trackColor, 0f, 360f, false, topLeft, arc, style = Stroke(s))
            val sweep = 360f * anim.value
            if (sweep > 0.5f) {
                rotate(-90f) {
                    drawArc(color.copy(alpha = 0.16f), 0f, sweep, false, topLeft, arc, style = Stroke(s * 1.8f, cap = StrokeCap.Round))
                    if (brush != null) drawArc(brush, 0f, sweep, false, topLeft, arc, style = Stroke(s, cap = StrokeCap.Round))
                    else drawArc(color, 0f, sweep, false, topLeft, arc, style = Stroke(s, cap = StrokeCap.Round))
                    val a = Math.toRadians(sweep.toDouble())
                    val r = arc.width / 2f
                    val head = Offset(center.x + r * kotlin.math.cos(a).toFloat(), center.y + r * kotlin.math.sin(a).toFloat())
                    drawCircle(Color.White, s * 0.30f, head)
                }
            }
        }
        content()
    }
}

/** Rounded progress bar with an animated gradient fill and a bright leading edge. */
@Composable
fun ProgressTrack(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    trackColor: Color = color.copy(alpha = 0.14f),
    height: Dp = 8.dp,
    brush: Brush? = null
) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(progress) { anim.animateTo(progress.coerceIn(0f, 1f), tween(700, easing = FastOutSlowInEasing)) }
    Canvas(modifier.fillMaxWidth().height(height)) {
        val r = CornerRadius(size.height / 2, size.height / 2)
        drawRoundRect(trackColor, cornerRadius = r)
        val w = size.width * anim.value
        if (w > 0f) {
            val fillSize = Size(maxOf(w, size.height), size.height)
            drawRoundRect(
                brush ?: Brush.horizontalGradient(listOf(color.copy(alpha = 0.55f), color), endX = fillSize.width),
                size = fillSize, cornerRadius = r
            )
            drawCircle(Color.White.copy(alpha = 0.9f), size.height * 0.22f, Offset(fillSize.width - size.height / 2, size.height / 2))
        }
    }
}

/** KPI tile: icon badge, big value, label and an optional supporting line or delta pill. */
@Composable
fun MetricTile(
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
    sub: String? = null,
    delta: String? = null,
    deltaPositive: Boolean? = null
) {
    PanelCard(modifier, contentPadding = PaddingValues(14.dp), spacing = 10.dp, elevation = 6.dp, glow = accent) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, accent, size = 36.dp)
            Spacer(Modifier.weight(1f))
            delta?.let {
                val c = when (deltaPositive) {
                    true -> AppTheme.colors.success
                    false -> AppTheme.colors.warning
                    null -> AppTheme.colors.inkMuted
                }
                Pill(it, color = c)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                value, style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurface, maxLines = 1
            )
            Text(label, style = MaterialTheme.typography.labelMedium, color = AppTheme.colors.inkMuted, maxLines = 1)
            sub?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.inkMuted, maxLines = 2) }
        }
    }
}

/** Icon + text row used for safety / privacy statements. */
@Composable
fun FeatureRow(icon: ImageVector, text: String, tint: Color, modifier: Modifier = Modifier, title: String? = null) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
        IconBadge(icon, tint, size = 34.dp)
        Column(Modifier.weight(1f).padding(top = if (title == null) 7.dp else 0.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            title?.let { Text(it, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface) }
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Callout with a coloured accent edge (info / warning / success). */
@Composable
fun Callout(
    text: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.14f), color.copy(alpha = 0.05f))))
            .border(1.dp, color.copy(alpha = 0.24f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            title?.let { Text(it, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface) }
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.86f))
            content()
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Motion
// ---------------------------------------------------------------------------------------------

/** Staggered fade + rise entrance for stacked sections. */
@Composable
fun Reveal(index: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val state = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = state,
        modifier = modifier,
        enter = fadeIn(tween(420, delayMillis = 55 * index)) +
            slideInVertically(tween(480, delayMillis = 55 * index, easing = FastOutSlowInEasing)) { it / 8 }
    ) { content() }
}
