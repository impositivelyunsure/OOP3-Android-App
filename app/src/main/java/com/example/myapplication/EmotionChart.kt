package com.example.myapplication

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

// individual slice of the pie chart
private data class Slice(
    val emotion: Emotion,
    val count: Int,
    val percent: Float,   // 0..1
    val sweepDeg: Float   // 0..360
)

// building the pie chart
private fun buildSlices(entries: List<JournalEntry>): List<Slice> {
    if (entries.isEmpty()) return emptyList()
    val counts = entries.groupingBy { it.emotion }.eachCount()
    val total = counts.values.sum().coerceAtLeast(1)
    return counts.entries
        .sortedBy { it.key.name }
        .map { (emo, cnt) ->
            val p = cnt.toFloat() / total
            Slice(emo, cnt, p, p * 360f)
        }
}


@Composable
fun EmotionChartDialog(
    entries: List<JournalEntry>,
    onDismiss: () -> Unit
) {
    val slices = remember(entries) { buildSlices(entries) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Button(onClick = onDismiss, shape = RoundedCornerShape(24)) { Text("Close") } },
        title = { Text("Emotion Frequency Chart", style = MaterialTheme.typography.titleLarge) },
        text = {
            if (slices.isEmpty()) {
                Text("No entries yet.")
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    EmotionPieChart(
                        slices = slices,
                        modifier = Modifier
                            .size(220.dp)
                            .padding(bottom = 12.dp)
                    )
                    EmotionLegend(slices)
                }
            }
        }
    )
}

@Composable
private fun EmotionPieChart(
    slices: List<Slice>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val diameter = size.minDimension
        val radius = diameter / 2f
        val topLeft = Offset(center.x - radius, center.y - radius)
        var start = -90f // start at 12 o'clock

        slices.forEach { s ->
            val color = emotionColours[s.emotion] ?: Color.Gray
            drawArc(
                color = color,
                startAngle = start,
                sweepAngle = s.sweepDeg,
                useCenter = true,
                topLeft = topLeft,
                size = Size(diameter, diameter)
            )
            start += s.sweepDeg
        }
    }
}

@Composable
private fun EmotionLegend(slices: List<Slice>) {
    val total = slices.sumOf { it.count }.coerceAtLeast(1)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        slices.forEach { s ->
            val color = emotionColours[s.emotion] ?: Color.Gray
            val pct = ((s.count * 1000f) / total).roundToInt() / 10f
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(color, RoundedCornerShape(3.dp))
                        .border(1.dp, Color.Black.copy(alpha = 0.1f), RoundedCornerShape(3.dp))
                )
                Text("${s.emotion.name}: ${s.count} (${pct.toInt()}%)")
            }
        }
    }
}