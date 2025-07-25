package com.anton.candash.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BatteryGauge(
    modifier: Modifier = Modifier,
    percentage: Int
) {
    val clampedPercent = percentage.coerceIn(0, 100)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val radius = size.minDimension / 2f * 0.8f
            val center = Offset(width / 2, height / 2)

            val strokeWidth = radius * 0.2f

            // Define sectors and their ranges
            val sectors = listOf(
                Pair(Color(0xFFFF5722), 0f..25f),  // Red: 0-30%
                Pair(Color(0xFFFFEB3B), 25f..55f), // Yellow: 30-55%
                Pair(Color(0xFF4CAF50), 55f..80f), // Green: 55-80%
                Pair(Color(0xFF00BCD4), 80f..100f) // Blue-Green: 80-100%
            )

            val startAngle = 135f // Starting point on the gauge
            val sweepTotal = 270f // Total sweep angle for the gauge

            // Draw each sector
            for ((color, range) in sectors) {
                val sectorStartAngle = startAngle + (range.start / 100f) * sweepTotal
                val sectorSweepAngle = ((range.endInclusive - range.start) / 100f) * sweepTotal
                drawArc(
                    color = color,
                    startAngle = sectorStartAngle,
                    sweepAngle = sectorSweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    size = Size(radius * 2, radius * 2),
                    topLeft = Offset(center.x - radius, center.y - radius)
                )
            }

            // Draw needle
            val needleAngle = startAngle + (clampedPercent / 100f) * sweepTotal
            val needleLength = radius - strokeWidth
            val needleEnd = Offset(
                x = center.x + needleLength * cos(Math.toRadians(needleAngle.toDouble())).toFloat(),
                y = center.y + needleLength * sin(Math.toRadians(needleAngle.toDouble())).toFloat()
            )
            drawLine(
                color = Color.White,
                start = center,
                end = needleEnd,
                strokeWidth = strokeWidth * 0.2f,
                cap = StrokeCap.Round
            )

            // Draw center circle
            drawCircle(
                color = Color.Black,
                radius = strokeWidth * 0.3f,
                center = center
            )
        }

        // Draw percentage text
        Text(
            text = "$clampedPercent%",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.Center) // или TopCenter / BottomCenter и т.д.
                .offset(y = 20.dp)
        )
    }
}
