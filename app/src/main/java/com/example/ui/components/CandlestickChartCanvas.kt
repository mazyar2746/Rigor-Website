package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Candle
import com.example.ui.theme.ChartBollingerBand
import com.example.ui.theme.ChartBollingerLine
import com.example.ui.theme.ChartCandleGreen
import com.example.ui.theme.ChartCandleRed
import com.example.ui.theme.RigourSurfaceVariant
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

@Composable
fun CandlestickChartCanvas(
    candles: List<Candle>,
    bbUpper: Double,
    bbMiddle: Double,
    bbLower: Double,
    modifier: Modifier = Modifier
) {
    if (candles.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(RigourSurfaceVariant, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("Loading chart candles...", color = Color.Gray, fontSize = 13.sp)
        }
        return
    }

    val displayCandles = candles.takeLast(32)
    val highestPrice = max(displayCandles.maxOf { it.high }, bbUpper)
    val lowestPrice = min(displayCandles.minOf { it.low }, bbLower)
    val priceRange = (highestPrice - lowestPrice).coerceAtLeast(0.0001)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(RigourSurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .padding(vertical = 12.dp, horizontal = 8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val chartRightPadding = 56.dp.toPx()
            val usableWidth = width - chartRightPadding
            val candleCount = displayCandles.size
            val candleSpacing = usableWidth / candleCount
            val candleBodyWidth = (candleSpacing * 0.65f).coerceIn(4f, 22f)

            fun priceToY(price: Double): Float {
                val normalized = (highestPrice - price) / priceRange
                return (normalized * height * 0.88f + height * 0.06f).toFloat()
            }

            // Draw horizontal grid lines and price labels
            val gridSteps = 4
            for (i in 0..gridSteps) {
                val priceAtStep = lowestPrice + (priceRange * (i.toDouble() / gridSteps))
                val y = priceToY(priceAtStep)

                drawLine(
                    color = Color.White.copy(alpha = 0.08f),
                    start = Offset(0f, y),
                    end = Offset(usableWidth, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                )
            }

            // Draw Bollinger Bands Shading & Lines
            val upperY = priceToY(bbUpper)
            val middleY = priceToY(bbMiddle)
            val lowerY = priceToY(bbLower)

            // Shaded envelope
            drawRect(
                color = ChartBollingerBand,
                topLeft = Offset(0f, upperY),
                size = Size(usableWidth, (lowerY - upperY).coerceAtLeast(1f))
            )

            // Bollinger Lines
            drawLine(
                color = ChartBollingerLine.copy(alpha = 0.7f),
                start = Offset(0f, upperY),
                end = Offset(usableWidth, upperY),
                strokeWidth = 1.5.dp.toPx()
            )

            drawLine(
                color = ChartBollingerLine.copy(alpha = 0.45f),
                start = Offset(0f, middleY),
                end = Offset(usableWidth, middleY),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
            )

            drawLine(
                color = ChartBollingerLine.copy(alpha = 0.7f),
                start = Offset(0f, lowerY),
                end = Offset(usableWidth, lowerY),
                strokeWidth = 1.5.dp.toPx()
            )

            // Draw Candlesticks
            displayCandles.forEachIndexed { index, candle ->
                val x = index * candleSpacing + (candleSpacing / 2f)
                val isBullish = candle.close >= candle.open
                val candleColor = if (isBullish) ChartCandleGreen else ChartCandleRed

                val openY = priceToY(candle.open)
                val closeY = priceToY(candle.close)
                val highY = priceToY(candle.high)
                val lowY = priceToY(candle.low)

                // Wick (high to low)
                drawLine(
                    color = candleColor,
                    start = Offset(x, highY),
                    end = Offset(x, lowY),
                    strokeWidth = 1.5.dp.toPx()
                )

                // Real Body
                val bodyTop = min(openY, closeY)
                val bodyBottom = max(openY, closeY)
                val bodyHeight = (bodyBottom - bodyTop).coerceAtLeast(2f)

                drawRect(
                    color = candleColor,
                    topLeft = Offset(x - (candleBodyWidth / 2f), bodyTop),
                    size = Size(candleBodyWidth, bodyHeight)
                )
            }
        }

        // Overlay Current Price / Axis labels on the right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 4.dp, top = 2.dp)
        ) {
            Text(
                text = String.format(Locale.US, "$%.2f", highestPrice),
                color = Color.Gray,
                fontSize = 10.sp
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp)
        ) {
            Text(
                text = String.format(Locale.US, "$%.2f", bbMiddle),
                color = ChartBollingerLine,
                fontSize = 10.sp
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 4.dp, bottom = 2.dp)
        ) {
            Text(
                text = String.format(Locale.US, "$%.2f", lowestPrice),
                color = Color.Gray,
                fontSize = 10.sp
            )
        }
    }
}
