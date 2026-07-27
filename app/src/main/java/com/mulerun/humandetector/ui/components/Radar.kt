package com.mulerun.humandetector.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.mulerun.humandetector.model.Target
import com.mulerun.humandetector.ui.theme.RadarGreen
import com.mulerun.humandetector.ui.theme.RadarGreenDim
import com.mulerun.humandetector.ui.theme.Warn
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Distance rings drawn on the radar, in metres. */
private val RANGE_RINGS = intArrayOf(5, 10, 25, 50, 100, 200)

@Composable
fun Radar(
    targets: List<Target>,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "radar")
    val sweep by infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
        label = "sweep"
    )
    val pulse by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "pulse"
    )
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(12.dp)
            .clip(CircleShape)
            .background(Color(0xFF04090C))
    ) {
        Canvas(Modifier.fillMaxWidth().aspectRatio(1f)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val rMax = min(cx, cy) - 4f

            // outer bezel
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFF0A1A15), Color(0xFF01060A)),
                    center = Offset(cx, cy), radius = rMax
                ), radius = rMax
            )

            // range rings
            val maxRangeM = RANGE_RINGS.last().toFloat()
            for (r in RANGE_RINGS) {
                val rr = rMax * (r / maxRangeM)
                drawCircle(
                    color = RadarGreenDim.copy(alpha = 0.35f),
                    radius = rr,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f)))
                )
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = 0xFF0FAE5D.toInt()
                        textSize = 22f
                        isAntiAlias = true
                        alpha = 160
                        typeface = android.graphics.Typeface.MONOSPACE
                    }
                    drawText("${r}m", cx + 4, cy - rr - 3, paint)
                }
            }

            // cross hair
            drawLine(RadarGreenDim.copy(alpha = 0.35f), Offset(cx, cy - rMax), Offset(cx, cy + rMax), 1f)
            drawLine(RadarGreenDim.copy(alpha = 0.35f), Offset(cx - rMax, cy), Offset(cx + rMax, cy), 1f)
            for (deg in 0 until 360 step 30) {
                val a = Math.toRadians(deg.toDouble()).toFloat()
                drawLine(
                    RadarGreenDim.copy(alpha = 0.18f),
                    Offset(cx + rMax * sin(a), cy - rMax * cos(a)),
                    Offset(cx + (rMax - 12f) * sin(a), cy - (rMax - 12f) * cos(a)),
                    1f
                )
            }

            // sweep beam — 40° arc with radial gradient
            val sweepRad = Math.toRadians(sweep.toDouble()).toFloat()
            for (i in 0..40) {
                val a = sweepRad - Math.toRadians(i.toDouble()).toFloat()
                val alpha = (0.4f - i / 100f).coerceAtLeast(0f)
                drawLine(
                    RadarGreen.copy(alpha = alpha),
                    Offset(cx, cy),
                    Offset(cx + rMax * sin(a), cy - rMax * cos(a)),
                    2f
                )
            }
            // leading edge
            drawLine(
                RadarGreen, Offset(cx, cy),
                Offset(cx + rMax * sin(sweepRad), cy - rMax * cos(sweepRad)),
                2.4f
            )

            // targets
            for (t in targets) {
                val r = (t.distanceM.toFloat() / maxRangeM).coerceIn(0.03f, 1f) * rMax
                val az = (t.azimuthDeg ?: 0.0).toFloat()
                val a = Math.toRadians(az.toDouble()).toFloat()
                val px = cx + r * sin(a)
                val py = cy - r * cos(a)
                // pulse halo
                drawCircle(Warn.copy(alpha = 0.25f * (1f - pulse)), 18f + 10f * pulse, Offset(px, py))
                drawCircle(Warn, 5f, Offset(px, py))
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = 0xFFFF3B4E.toInt()
                        textSize = 22f
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.MONOSPACE
                    }
                    drawText(
                        "#${t.id} ${"%.1f".format(t.distanceM)}m ${(t.confidence * 100).toInt()}%",
                        px + 8, py - 8, paint
                    )
                }
                if (t.azimuthDeg == null) {
                    drawCircle(
                        Warn.copy(alpha = 0.4f), r, Offset(cx, cy),
                        style = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f)))
                    )
                }
            }
        }
    }
}
