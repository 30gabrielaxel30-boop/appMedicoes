package com.keyhistory.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

data class ChartPoint(val label: String, val value: Int)

@Composable
private fun rememberProgress(key: Any): Float {
    val anim = remember(key) { Animatable(0f) }
    LaunchedEffect(key) { anim.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
    return anim.value
}

/** Gráfico de barras verticais (registros por dia). */
@Composable
fun BarChart(points: List<ChartPoint>, modifier: Modifier = Modifier, color: Color = KhColors.Primary) {
    val progress = rememberProgress(points)
    val max = (points.maxOfOrNull { it.value } ?: 0).coerceAtLeast(1)
    Column(modifier) {
        Row(Modifier.fillMaxWidth().height(130.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
            points.forEachIndexed { index, p ->
                Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                    val fraction = maxOf((p.value.toFloat() / max) * progress, 0.02f).coerceAtMost(1f)
                    val isLast = index == points.lastIndex
                    Spacer(Modifier.weight(1.0001f - fraction))
                    Text(if (p.value > 0) p.value.toString() else "", style = MaterialTheme.typography.labelSmall, color = KhColors.TextMuted)
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .weight(fraction)
                            .background(
                                Brush.verticalGradient(
                                    if (isLast) listOf(color, color.copy(alpha = 0.6f))
                                    else listOf(color.copy(alpha = 0.55f), color.copy(alpha = 0.25f))
                                ),
                                RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 2.dp, bottomEnd = 2.dp)
                            )
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            points.forEach {
                Text(it.label, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = KhColors.TextMuted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 1)
            }
        }
    }
}

/** Barras horizontais (atividade por aplicativo). */
@Composable
fun HorizontalBars(points: List<ChartPoint>, modifier: Modifier = Modifier) {
    val progress = rememberProgress(points)
    val max = (points.maxOfOrNull { it.value } ?: 0).coerceAtLeast(1)
    val total = points.sumOf { it.value }.coerceAtLeast(1)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        points.forEachIndexed { i, p ->
            val color = KhColors.Chart[i % KhColors.Chart.size]
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).background(color, CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text(p.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${p.value} · ${p.value * 100 / total}%", style = MaterialTheme.typography.labelMedium, color = KhColors.TextMuted)
                }
                Spacer(Modifier.height(6.dp))
                Box(Modifier.fillMaxWidth().height(8.dp).background(KhColors.Graphite, RoundedCornerShape(50))) {
                    Box(
                        Modifier.fillMaxWidth((p.value.toFloat() / max * progress).coerceIn(0.01f, 1f)).height(8.dp)
                            .background(color, RoundedCornerShape(50))
                    )
                }
            }
        }
    }
}

/** Linha com área preenchida (evolução semanal). */
@Composable
fun LineChart(points: List<ChartPoint>, modifier: Modifier = Modifier, color: Color = KhColors.Green) {
    val progress = rememberProgress(points)
    val max = (points.maxOfOrNull { it.value } ?: 0).coerceAtLeast(1)
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(120.dp).padding(horizontal = 6.dp, vertical = 8.dp)) {
            if (points.isEmpty()) return@Canvas
            val stepX = if (points.size > 1) size.width / (points.size - 1) else 0f
            val grid = KhColors.Outline.copy(alpha = 0.5f)
            for (i in 0..3) {
                val y = size.height * i / 3f
                drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            }
            val coords = points.mapIndexed { i, p ->
                Offset(i * stepX, size.height - (p.value.toFloat() / max) * size.height * progress)
            }
            val line = Path().apply {
                coords.forEachIndexed { i, o -> if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) }
            }
            val area = Path().apply {
                addPath(line)
                lineTo(coords.last().x, size.height)
                lineTo(coords.first().x, size.height)
                close()
            }
            drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.35f), Color.Transparent)))
            drawPath(line, color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            coords.forEach {
                drawCircle(KhColors.Background, radius = 5.dp.toPx(), center = it)
                drawCircle(color, radius = 3.5.dp.toPx(), center = it)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            points.forEach { Text(it.label, style = MaterialTheme.typography.labelSmall, color = KhColors.TextMuted) }
        }
    }
}

/** Mini indicador de proporção usado no card de armazenamento. */
@Composable
fun ProgressBar(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().height(6.dp)) {
        drawRoundRect(KhColors.Graphite, cornerRadius = CornerRadius(size.height))
        drawRoundRect(color, size = Size(size.width * fraction.coerceIn(0f, 1f), size.height), cornerRadius = CornerRadius(size.height))
    }
}

@Composable
fun ChartLegendValue(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = KhColors.TextMuted)
    }
}
