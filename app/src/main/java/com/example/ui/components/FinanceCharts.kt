package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.CurrencyFormatter
import kotlin.math.cos
import kotlin.math.sin

data class CategorySpendItem(
    val categoryName: String,
    val amount: Double,
    val color: Color,
    val iconName: String
)

@Composable
fun SpendingDonutChart(
    items: List<CategorySpendItem>,
    currencyCode: String = "USD",
    modifier: Modifier = Modifier
) {
    val totalAmount = remember(items) { items.sumOf { it.amount } }
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(items) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(1f, animationSpec = tween(1000))
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(220.dp)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 32.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val center = Offset(size.width / 2, size.height / 2)

                if (totalAmount <= 0.0 || items.isEmpty()) {
                    drawCircle(
                        color = Color.LightGray.copy(alpha = 0.3f),
                        radius = radius,
                        center = center,
                        style = Stroke(width = strokeWidth)
                    )
                } else {
                    var startAngle = -90f
                    for (item in items) {
                        val sweepAngle = ((item.amount / totalAmount) * 360f * animatedProgress.value).toFloat()
                        if (sweepAngle > 0.5f) {
                            drawArc(
                                color = item.color,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle - 2f, // subtle gap
                                useCenter = false,
                                topLeft = Offset(center.x - radius, center.y - radius),
                                size = Size(radius * 2, radius * 2),
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                        startAngle += sweepAngle
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Total Spent",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = CurrencyFormatter.format(totalAmount, currencyCode),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Legend Breakdown
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.take(5).forEach { item ->
                val pct = if (totalAmount > 0) ((item.amount / totalAmount) * 100).toInt() else 0
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(item.color)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.categoryName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "${CurrencyFormatter.format(item.amount, currencyCode)} ($pct%)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

data class MonthlyBarData(
    val monthLabel: String,
    val income: Double,
    val expense: Double
)

@Composable
fun MonthlyComparisonBarChart(
    dataList: List<MonthlyBarData>,
    currencyCode: String = "USD",
    modifier: Modifier = Modifier
) {
    val maxVal = remember(dataList) {
        val maxInc = dataList.maxOfOrNull { it.income } ?: 100.0
        val maxExp = dataList.maxOfOrNull { it.expense } ?: 100.0
        Math.max(maxInc, maxExp).coerceAtLeast(100.0)
    }

    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(dataList) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(1f, animationSpec = tween(900))
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF10B981)))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Income", style = MaterialTheme.typography.labelSmall)
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFF43F5E)))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Expense", style = MaterialTheme.typography.labelSmall)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val barWidth = 14.dp.toPx()
                val groupSpacing = size.width / dataList.size
                val chartHeight = size.height - 30.dp.toPx()

                // Draw Grid Lines
                for (i in 0..3) {
                    val y = chartHeight * (i / 3f)
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.25f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                dataList.forEachIndexed { index, item ->
                    val groupCenterX = (index * groupSpacing) + (groupSpacing / 2)

                    // Income Bar
                    val incHeight = ((item.income / maxVal) * chartHeight * animatedProgress.value).toFloat()
                    val incLeft = groupCenterX - barWidth - 2.dp.toPx()
                    drawRoundRect(
                        color = Color(0xFF10B981),
                        topLeft = Offset(incLeft, chartHeight - incHeight),
                        size = Size(barWidth, incHeight),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )

                    // Expense Bar
                    val expHeight = ((item.expense / maxVal) * chartHeight * animatedProgress.value).toFloat()
                    val expLeft = groupCenterX + 2.dp.toPx()
                    drawRoundRect(
                        color = Color(0xFFF43F5E),
                        topLeft = Offset(expLeft, chartHeight - expHeight),
                        size = Size(barWidth, expHeight),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                }
            }

            // Month Labels below chart
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                dataList.forEach { item ->
                    Text(
                        text = item.monthLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

data class TrendPoint(
    val label: String,
    val amount: Double
)

@Composable
fun SpendingTrendLineChart(
    points: List<TrendPoint>,
    currencyCode: String = "USD",
    modifier: Modifier = Modifier
) {
    val maxVal = remember(points) { (points.maxOfOrNull { it.amount } ?: 100.0).coerceAtLeast(50.0) }
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(points) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(1f, animationSpec = tween(900))
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (points.size < 2) return@Canvas

                val chartHeight = size.height - 24.dp.toPx()
                val stepX = size.width / (points.size - 1)

                val path = Path()
                val fillPath = Path()

                points.forEachIndexed { i, pt ->
                    val x = i * stepX
                    val y = chartHeight - ((pt.amount / maxVal) * chartHeight * animatedProgress.value).toFloat()

                    if (i == 0) {
                        path.moveTo(x, y)
                        fillPath.moveTo(x, chartHeight)
                        fillPath.lineTo(x, y)
                    } else {
                        val prevX = (i - 1) * stepX
                        val prevY = chartHeight - ((points[i - 1].amount / maxVal) * chartHeight * animatedProgress.value).toFloat()
                        val cx = (prevX + x) / 2
                        path.cubicTo(cx, prevY, cx, y, x, y)
                        fillPath.cubicTo(cx, prevY, cx, y, x, y)
                    }
                }

                fillPath.lineTo(size.width, chartHeight)
                fillPath.close()

                // Draw Gradient Fill
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F766E).copy(alpha = 0.35f),
                            Color(0xFF0F766E).copy(alpha = 0.0f)
                        ),
                        startY = 0f,
                        endY = chartHeight
                    )
                )

                // Draw Line
                drawPath(
                    path = path,
                    color = Color(0xFF0F766E),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw Dots
                points.forEachIndexed { i, pt ->
                    val x = i * stepX
                    val y = chartHeight - ((pt.amount / maxVal) * chartHeight * animatedProgress.value).toFloat()
                    drawCircle(
                        color = Color.White,
                        radius = 4.dp.toPx(),
                        center = Offset(x, y)
                    )
                    drawCircle(
                        color = Color(0xFF0F766E),
                        radius = 4.dp.toPx(),
                        center = Offset(x, y),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }

            // Labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                points.forEach { pt ->
                    Text(
                        text = pt.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
