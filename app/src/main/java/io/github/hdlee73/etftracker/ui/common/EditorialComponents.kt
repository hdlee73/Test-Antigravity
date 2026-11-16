package io.github.hdlee73.etftracker.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.hdlee73.etftracker.ui.theme.WealthColors
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

fun formatWon(value: Long): String {
    val nf = NumberFormat.getNumberInstance(Locale.KOREA)
    return "${nf.format(value)}원"
}

fun formatSignedPct(value: Double, suffix: String = "%"): String {
    val sign = if (value > 0) "+" else ""
    return "$sign${String.format(Locale.US, "%.2f", value)}$suffix"
}

@Composable
fun EditorialCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun SectionEditorialHeader(
    eyebrow: String,
    title: String,
    subtitle: String? = null,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = eyebrow.uppercase(Locale.US),
            style = MaterialTheme.typography.labelMedium,
            color = WealthColors.ChampagneGold,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun StatusPill(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = containerColor,
    ) {
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun VennOverlapDiagram(
    etfAName: String,
    etfBName: String,
    overlapPct: Double,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(
                modifier = Modifier
                    .width(240.dp)
                    .height(130.dp),
            ) {
                val radius = size.minDimension * 0.42f
                val centerY = size.height / 2f
                val offset = radius * 0.56f
                drawCircle(
                    color = WealthColors.ClassicNavy.copy(alpha = 0.72f),
                    radius = radius,
                    center = Offset(size.width / 2f - offset, centerY),
                )
                drawCircle(
                    color = WealthColors.ForestEmerald.copy(alpha = 0.68f),
                    radius = radius,
                    center = Offset(size.width / 2f + offset, centerY),
                )
                drawCircle(
                    color = WealthColors.ChampagneGold,
                    radius = radius,
                    center = Offset(size.width / 2f - offset, centerY),
                    style = Stroke(width = 2.5f),
                )
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f),
                border = BorderStroke(1.dp, WealthColors.ChampagneGold),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "중복 비중",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${String.format(Locale.US, "%.1f", overlapPct)}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = WealthColors.ClassicNavy,
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            LegendDot(color = WealthColors.ClassicNavy, label = etfAName)
            LegendDot(color = WealthColors.ForestEmerald, label = etfBName)
        }
    }
}

@Composable
fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(10.dp)
                .height(10.dp)
                .background(color, RoundedCornerShape(50)),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun MultiLineChart(
    series: List<Pair<String, List<Double>>>,
    colors: List<Color>,
    modifier: Modifier = Modifier,
) {
    val validSeries = series.filter { it.second.size >= 2 }
    if (validSeries.isEmpty()) return

    val allValues = validSeries.flatMap { it.second }
    val minVal = allValues.minOrNull() ?: 0.0
    val maxVal = allValues.maxOrNull() ?: 1.0
    val span = (maxVal - minVal).takeIf { abs(it) > 1e-6 } ?: 1.0

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
        ) {
            // Subtle horizontal grid lines
            for (i in 0..3) {
                val y = size.height * (i / 3f)
                drawLine(
                    color = WealthColors.WarmBorder,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f,
                )
            }
            validSeries.forEachIndexed { idx, (_, pts) ->
                val strokeColor = colors.getOrElse(idx) { WealthColors.ClassicNavy }
                val path = Path()
                pts.forEachIndexed { pIdx, v ->
                    val x = if (pts.size == 1) 0f else (pIdx.toFloat() / (pts.size - 1)) * size.width
                    val norm = ((v - minVal) / span).toFloat()
                    val y = size.height - (norm * (size.height * 0.86f)) - (size.height * 0.07f)
                    if (pIdx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path = path,
                    color = strokeColor,
                    style = Stroke(width = 4.5f, cap = StrokeCap.Round),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            validSeries.forEachIndexed { idx, (label, _) ->
                LegendDot(
                    color = colors.getOrElse(idx) { WealthColors.ClassicNavy },
                    label = label,
                )
            }
        }
    }
}
