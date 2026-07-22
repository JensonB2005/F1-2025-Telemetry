package com.f1telemetry.viewer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.f1telemetry.viewer.data.LiveCar
import com.f1telemetry.viewer.telemetry.F1Constants

/**
 * Live track map. The outline is the accumulated world-position path of the
 * player's car over a lap; each car is a dot placed by its world (x, z),
 * coloured by team, with the player ringed.
 */
@Composable
fun LiveTrackMap(
    path: List<Pair<Float, Float>>,
    cars: List<LiveCar>,
    modifier: Modifier = Modifier,
    heightDp: Int = 280,
) {
    Box(modifier.fillMaxWidth().height(heightDp.dp)) {
        Canvas(Modifier.fillMaxWidth().height(heightDp.dp)) {
            if (path.size < 4) return@Canvas
            var minX = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE
            var minZ = Float.MAX_VALUE; var maxZ = -Float.MAX_VALUE
            for ((x, z) in path) {
                if (x < minX) minX = x; if (x > maxX) maxX = x
                if (z < minZ) minZ = z; if (z > maxZ) maxZ = z
            }
            val spanX = (maxX - minX).takeIf { it > 1f } ?: 1f
            val spanZ = (maxZ - minZ).takeIf { it > 1f } ?: 1f
            val pad = 26f
            val scale = minOf((size.width - 2 * pad) / spanX, (size.height - 2 * pad) / spanZ)
            val offX = (size.width - spanX * scale) / 2f
            val offZ = (size.height - spanZ * scale) / 2f
            fun map(x: Float, z: Float): Offset {
                val sx = offX + (x - minX) * scale
                // Flip Z so "north" is up.
                val sy = size.height - (offZ + (z - minZ) * scale)
                return Offset(sx, sy)
            }

            // Outline
            val outline = Path()
            path.forEachIndexed { i, (x, z) ->
                val p = map(x, z)
                if (i == 0) outline.moveTo(p.x, p.y) else outline.lineTo(p.x, p.y)
            }
            outline.close()
            drawPath(outline, Color(0xFF4A4A5A), style = Stroke(width = 10f))
            drawPath(outline, Color(0xFF20202C), style = Stroke(width = 5f))

            // Cars
            val paint = android.graphics.Paint().apply {
                textSize = 20f
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
            }
            for (car in cars) {
                if (car.worldX == 0f && car.worldZ == 0f) continue
                val p = map(car.worldX, car.worldZ)
                val col = Color(F1Constants.teamColor(car.teamId))
                if (car.isPlayer) {
                    drawCircle(Color.White, radius = 11f, center = p)
                }
                drawCircle(col, radius = 8f, center = p)
                paint.color = android.graphics.Color.WHITE
                drawContext.canvas.nativeCanvas.drawText(car.abbrev, p.x, p.y - 12f, paint)
            }
        }
    }
}
