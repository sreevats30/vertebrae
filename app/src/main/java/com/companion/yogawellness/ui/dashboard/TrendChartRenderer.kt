package com.companion.yogawellness.ui.dashboard

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.graphics.toArgb
import com.companion.yogawellness.ui.theme.AppColors
import com.companion.yogawellness.wellness.WeeklyRow
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Chart colours (validated categorical slots 1–3 against this app's card surfaces in both modes).
 * Text always uses ink tokens; series colours are only used for marks and legend keys.
 */
data class ChartColors(
    val surface: Int,
    val inkPrimary: Int,
    val inkSecondary: Int,
    val muted: Int,
    val grid: Int,
    val axis: Int,
    val quality: Int,
    val stress: Int,
    val mood: Int
) {
    companion object {
        /** Printed PDF report (white paper). Not part of the app palettes; see COLOUR_GUIDE.md §7. */
        val Light = ChartColors(
            surface = 0xFFFFFFFF.toInt(), inkPrimary = 0xFF111418.toInt(), inkSecondary = 0xFF4A5260.toInt(),
            muted = 0xFF6B7380.toInt(), grid = 0xFFEDEFF2.toInt(), axis = 0xFFC9CED6.toInt(),
            quality = 0xFF2A6FD6.toInt(), stress = 0xFFE0612D.toInt(), mood = 0xFF14946B.toInt()
        )

        /**
         * On the app's page colour (the dashboard draws charts directly on the page, not in a card),
         * from the theme tokens. Read `AppTheme.colors` in composition and pass it here; never inside DrawScope.
         */
        fun onPage(c: AppColors) = ChartColors(
            surface = c.canvas.toArgb(), inkPrimary = c.text.toArgb(), inkSecondary = c.textSecondary.toArgb(),
            muted = c.textTertiary.toArgb(), grid = c.line.toArgb(), axis = c.lineStrong.toArgb(),
            quality = c.quality.toArgb(), stress = c.stress.toArgb(), mood = c.mood.toArgb()
        )
    }
}

/** Line pattern of a data series; lengths in dp. */
enum class StrokePattern { SOLID, DASHED, DOTTED, DASH_DOT }

/** Point marker of a data series. */
enum class MarkerShape { CIRCLE, DIAMOND, SQUARE, TRIANGLE }

/**
 * Every data series has its own colour, line pattern AND marker, so series stay distinguishable
 * without colour (red-green colour-vision differences, greyscale print). Sleep is reserved for when
 * it is plotted; the weekly trend currently shows quality, stress and mood.
 */
enum class ChartSeries(val label: String, val pattern: StrokePattern, val marker: MarkerShape) {
    MOOD("Mood", StrokePattern.SOLID, MarkerShape.CIRCLE),
    STRESS("Stress", StrokePattern.DASHED, MarkerShape.DIAMOND),
    SLEEP("Sleep", StrokePattern.DOTTED, MarkerShape.SQUARE),
    QUALITY("Quality", StrokePattern.DASH_DOT, MarkerShape.TRIANGLE)
}

/**
 * Weekly trend as two small-multiple panels on a shared week axis:
 *   top    — practice quality (0–100)
 *   bottom — self-rated stress and mood (1–10)
 * The blueprint asks for a "dual-axis" chart; two y-scales on one plot let the reader infer a
 * relationship from arbitrary axis ranges, so the same comparison is drawn as stacked panels.
 *
 * Drawn on a plain [Canvas] so the app (Compose nativeCanvas) and the PDF report share one renderer.
 * @param dp pixels per dp, @param sp pixels per sp (use ~0.8 for PDF points)
 */
class TrendChartRenderer(
    private val c: ChartColors,
    private val dp: Float,
    private val sp: Float,
    /** Optional app typeface for labels (the PDF keeps the platform default). */
    private val typeface: android.graphics.Typeface? = null
) {

    private val leftPad = 30 * dp
    private val rightPad = 40 * dp
    private val titleH = 22 * dp
    private val gap = 16 * dp
    private val xLabelH = 20 * dp
    private val legendH = 20 * dp

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = c.grid; strokeWidth = max(1f, 0.75f * dp) }
    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = c.axis; strokeWidth = max(1f, 0.75f * dp) }
    private val tickText = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = c.muted; textSize = 11 * sp; textAlign = Paint.Align.RIGHT }
    private val xText = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = c.muted; textSize = 11 * sp; textAlign = Paint.Align.CENTER }
    private val titleText = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = c.inkSecondary; textSize = 12 * sp }
    private val endText = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = c.inkPrimary; textSize = 12 * sp; isFakeBoldText = true }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 1.75f * dp; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val selPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = c.inkSecondary; strokeWidth = max(1f, dp); alpha = 140 }
    private val markerPath = Path()

    /**
     * Dash intervals per pattern. Round caps add half the stroke width to each end of every dash, so
     * the "on" lengths are shortened and the gaps lengthened by one stroke width; a near-zero dash
     * draws a round dot.
     */
    private val dashEffects: Map<StrokePattern, DashPathEffect?> = run {
        val cap = linePaint.strokeWidth
        fun on(v: Float) = max(0.01f, v * dp - cap)
        fun off(v: Float) = v * dp + cap
        mapOf(
            StrokePattern.SOLID to null,
            StrokePattern.DASHED to DashPathEffect(floatArrayOf(on(6f), off(4f)), 0f),
            StrokePattern.DOTTED to DashPathEffect(floatArrayOf(0.01f, off(3.5f)), 0f),
            StrokePattern.DASH_DOT to DashPathEffect(floatArrayOf(on(8f), off(3f), 0.01f, off(3f)), 0f)
        )
    }

    init {
        if (typeface != null) listOf(tickText, xText, titleText, endText).forEach { it.typeface = typeface }
        if (typeface != null) endText.isFakeBoldText = false
    }

    private class Series(val kind: ChartSeries, val values: List<Double?>)

    private fun colorOf(s: ChartSeries) = when (s) {
        ChartSeries.MOOD -> c.mood
        ChartSeries.STRESS -> c.stress
        ChartSeries.QUALITY -> c.quality
        // Not plotted yet; never reached by the weekly trend.
        ChartSeries.SLEEP -> c.inkSecondary
    }

    private fun strokeFor(s: ChartSeries): Paint = linePaint.apply {
        color = colorOf(s)
        pathEffect = dashEffects[s.pattern]
    }

    /**
     * Marker of [shape] centred on ([x], [y]). [r] is the radius of the equivalent circle; the other
     * shapes are scaled to the same area so no series looks heavier than another.
     */
    private fun drawMarker(canvas: Canvas, shape: MarkerShape, x: Float, y: Float, r: Float, paint: Paint) {
        when (shape) {
            MarkerShape.CIRCLE -> canvas.drawCircle(x, y, r, paint)
            MarkerShape.SQUARE -> {
                val h = r * 0.886f // side = r·√π
                canvas.drawRect(x - h, y - h, x + h, y + h, paint)
            }
            MarkerShape.DIAMOND -> {
                val h = r * 1.253f // half-diagonal = r·√(π/2)
                markerPath.reset()
                markerPath.moveTo(x, y - h); markerPath.lineTo(x + h, y); markerPath.lineTo(x, y + h); markerPath.lineTo(x - h, y)
                markerPath.close()
                canvas.drawPath(markerPath, paint)
            }
            MarkerShape.TRIANGLE -> {
                val R = r * 1.555f // circumradius of an equilateral triangle with the circle's area
                val half = R * sqrt(3f) / 2f
                markerPath.reset()
                markerPath.moveTo(x, y - R); markerPath.lineTo(x + half, y + R / 2f); markerPath.lineTo(x - half, y + R / 2f)
                markerPath.close()
                canvas.drawPath(markerPath, paint)
            }
        }
    }

    /** Marker with a ring in the surface colour, so it reads on top of its own and other lines. */
    private fun drawRingedMarker(canvas: Canvas, s: ChartSeries, x: Float, y: Float, r: Float) {
        fill.color = c.surface
        drawMarker(canvas, s.marker, x, y, r + 1.5f * dp, fill)
        fill.color = colorOf(s)
        drawMarker(canvas, s.marker, x, y, r, fill)
    }

    /** Index of the week column under [x], for tap-to-inspect. */
    fun indexAt(x: Float, width: Float, n: Int): Int? {
        if (n == 0) return null
        val plotW = width - leftPad - rightPad
        val i = floor((x - leftPad) / (plotW / n)).toInt()
        return i.takeIf { it in 0 until n }
    }

    fun draw(canvas: Canvas, width: Float, height: Float, weeks: List<WeeklyRow>, labels: List<String>, selected: Int? = null) {
        val n = weeks.size
        if (n == 0) return
        val plotW = width - leftPad - rightPad
        val plotH = (height - 2 * titleH - gap - xLabelH - legendH) / 2f
        val step = plotW / n
        fun x(i: Int) = leftPad + step * (i + 0.5f)

        // ---- Panel A: practice quality --------------------------------------------------------
        val aTop = titleH
        canvas.drawText("Practice quality (alignment %, 0–100)", leftPad, aTop - 7 * dp, titleText)
        drawPanel(
            canvas, aTop, plotH, plotW, 0.0, 100.0, listOf(0.0, 50.0, 100.0), ::x,
            listOf(Series(ChartSeries.QUALITY, weeks.map { it.practiceQuality })), selected, decimals = 0
        )

        // ---- Panel B: stress & mood -----------------------------------------------------------
        val bTop = aTop + plotH + gap + titleH
        canvas.drawText("Stress & mood (self-rated, 1–10)", leftPad, bTop - 7 * dp, titleText)
        drawPanel(
            canvas, bTop, plotH, plotW, 0.0, 10.0, listOf(0.0, 5.0, 10.0), ::x,
            listOf(
                Series(ChartSeries.STRESS, weeks.map { it.meanStress }),
                Series(ChartSeries.MOOD, weeks.map { it.meanMood })
            ),
            selected, decimals = 1
        )

        // ---- Shared week axis -----------------------------------------------------------------
        val xBase = bTop + plotH + 15 * dp
        // Thin the labels (every 2nd, 3rd…) when they would crowd the column width.
        val widest = labels.maxOfOrNull { xText.measureText(it) } ?: 0f
        val every = max(1, kotlin.math.ceil((widest + 8 * dp) / step).toInt())
        labels.forEachIndexed { i, l -> if ((n - 1 - i) % every == 0) canvas.drawText(l, x(i), xBase, xText) }

        // Legend on its own row so it never collides with a panel title at narrow widths.
        drawLegend(canvas, leftPad, xBase + legendH - 2 * dp)

        selected?.let { i ->
            canvas.drawLine(x(i), aTop, x(i), aTop + plotH, selPaint)
            canvas.drawLine(x(i), bTop, x(i), bTop + plotH, selPaint)
        }
    }

    /** Each key repeats the series' colour, line pattern and marker, so the legend works without colour. */
    private fun drawLegend(canvas: Canvas, left: Float, baseline: Float) {
        val key = 22 * dp
        var cursor = left
        for (s in LEGEND) {
            val y = baseline - 4 * dp
            canvas.drawLine(cursor, y, cursor + key, y, strokeFor(s))
            drawRingedMarker(canvas, s, cursor + key / 2f, y, 3f * dp)
            cursor += key + 5 * dp
            canvas.drawText(s.label, cursor, baseline, titleText)
            cursor += titleText.measureText(s.label) + 14 * dp
        }
    }

    private companion object {
        /** Legend order: top panel first, then the bottom panel's series. */
        val LEGEND = listOf(ChartSeries.QUALITY, ChartSeries.STRESS, ChartSeries.MOOD)
    }

    private fun drawPanel(
        canvas: Canvas,
        top: Float,
        h: Float,
        plotW: Float,
        min: Double,
        maxV: Double,
        ticks: List<Double>,
        x: (Int) -> Float,
        series: List<Series>,
        selected: Int?,
        decimals: Int
    ) {
        fun y(v: Double) = (top + h - (v - min) / (maxV - min) * h).toFloat()
        // Recessive gridlines + baseline
        ticks.forEach { t ->
            val yy = y(t)
            canvas.drawLine(leftPad, yy, leftPad + plotW, yy, if (t == min) axisPaint else gridPaint)
            canvas.drawText(t.toInt().toString(), leftPad - 6 * dp, yy + 4 * dp, tickText)
        }
        // Lines (broken at weeks with no data), each in its own pattern
        for (s in series) {
            val stroke = strokeFor(s.kind)
            val path = Path()
            var penDown = false
            s.values.forEachIndexed { i, v ->
                if (v == null) { penDown = false; return@forEachIndexed }
                if (!penDown) path.moveTo(x(i), y(v)) else path.lineTo(x(i), y(v))
                penDown = true
            }
            canvas.drawPath(path, stroke)
        }
        // Markers (one shape per series) with a 1.5dp surface ring
        for (s in series) {
            s.values.forEachIndexed { i, v ->
                if (v == null) return@forEachIndexed
                val r = if (i == selected) 4.5f * dp else 3f * dp
                drawRingedMarker(canvas, s.kind, x(i), y(v), r)
            }
        }
        // Direct end labels (skipped when they would collide; the legend + table carry identity then)
        val ends = series.mapNotNull { s ->
            val i = s.values.indexOfLast { it != null }
            if (i < 0) null else Triple(s, i, s.values[i]!!)
        }
        val collide = ends.size == 2 && abs(y(ends[0].third) - y(ends[1].third)) < 14 * dp
        if (!collide) {
            ends.forEach { (_, i, v) ->
                val txt = if (decimals == 0) v.toInt().toString() else String.format(java.util.Locale.US, "%.1f", v)
                canvas.drawText(txt, x(i) + 10 * dp, y(v) + 4 * dp, endText)
            }
        }
    }
}
