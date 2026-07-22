package com.f1telemetry.viewer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import com.f1telemetry.viewer.ui.theme.TextDim

data class Series(
    val label: String,
    val color: Color,
    val points: List<Pair<Float, Float>>, // x (e.g. distance), y (value)
    val yMin: Float,
    val yMax: Float,
)

/**
 * Multi-series line chart drawn on a shared x-axis (lap distance). Each series
 * is normalised to its own [Series.yMin]..[Series.yMax] so speed, throttle and
 * brake can share one plot.
 */
@Composable
fun MultiLineChart(
    series: List<Series>,
    modifier: Modifier = Modifier,
    heightDp: Int = 150,
    xMax: Float? = null,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .clip(RoundedCornerShape(10.dp)),
    ) {
        Canvas(Modifier.fillMaxWidth().height(heightDp.dp).padding(6.dp)) {
            val gridColor = Color(0xFF33333F)
            // horizontal gridlines
            for (i in 0..3) {
                val y = size.height * i / 3f
                drawLine(gridColor, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), 1f)
            }
            val maxX = xMax ?: series.mapNotNull { it.points.maxOfOrNull { p -> p.first } }.maxOrNull() ?: 1f
            if (maxX <= 0f) return@Canvas
            for (s in series) {
                if (s.points.size < 2) continue
                drawSeries(s, maxX)
            }
        }
    }
    Row(
        Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        series.forEach { s ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(Modifier.size(9.dp).clip(RoundedCornerShape(2.dp))) { drawRect(s.color) }
                Text("  ${s.label}", color = TextDim, fontSize = 10.sp)
            }
        }
    }
}

private fun DrawScope.drawSeries(s: Series, maxX: Float) {
    val range = (s.yMax - s.yMin).takeIf { it != 0f } ?: 1f
    val path = Path()
    var started = false
    for (p in s.points) {
        val x = (p.first / maxX).coerceIn(0f, 1f) * size.width
        val norm = ((p.second - s.yMin) / range).coerceIn(0f, 1f)
        val y = size.height - norm * size.height
        if (!started) { path.moveTo(x, y); started = true } else path.lineTo(x, y)
    }
    drawPath(path, s.color, style = Stroke(width = 2.2f))
}

/** A single-value trace vs an optional reference (e.g. current vs best lap). */
@Composable
fun ComparisonChart(
    label: String,
    current: List<Pair<Float, Float>>,
    reference: List<Pair<Float, Float>>?,
    yMin: Float,
    yMax: Float,
    currentColor: Color,
    referenceColor: Color,
    heightDp: Int = 140,
) {
    val list = buildList {
        if (reference != null) add(Series("Best", referenceColor, reference, yMin, yMax))
        add(Series(label, currentColor, current, yMin, yMax))
    }
    MultiLineChart(list, heightDp = heightDp)
}
