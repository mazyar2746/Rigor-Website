package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MarketDataProvenance
import com.example.data.model.SignalType
import com.example.data.model.TechnicalAnalysisResult
import com.example.i18n.Language
import com.example.i18n.StringsDictionary
import com.example.ui.theme.ChartCandleGreen
import com.example.ui.theme.ChartCandleRed
import com.example.ui.theme.ChartMacdLine
import com.example.ui.theme.ChartRsiLine
import com.example.ui.theme.ChartSignalLine
import com.example.ui.theme.RigourBearish
import com.example.ui.theme.RigourBullish
import com.example.ui.theme.RigourCardBorder
import com.example.ui.theme.RigourCyan
import com.example.ui.theme.RigourGold
import com.example.ui.theme.RigourNeutral
import com.example.ui.theme.RigourSurface
import com.example.ui.theme.RigourSurfaceVariant
import java.util.Locale

@Composable
fun SignalConfluenceCard(
    result: TechnicalAnalysisResult,
    lang: Language,
    modifier: Modifier = Modifier
) {
    val (signalText, signalColor, signalBg) = when {
        result.provenance == MarketDataProvenance.SYNTHETIC -> Triple(
            if (lang == Language.PERSIAN) "شبیه‌سازی نمایشی" else "SIMULATION PREVIEW",
            RigourGold,
            RigourGold.copy(alpha = 0.15f)
        )
        result.provenance == MarketDataProvenance.UNAVAILABLE -> Triple(
            if (lang == Language.PERSIAN) "داده‌ها در دسترس نیست" else "DATA UNAVAILABLE",
            Color.Gray,
            Color.Gray.copy(alpha = 0.15f)
        )
        else -> when (result.overallSignal) {
            SignalType.STRONG_BUY -> Triple(
                StringsDictionary.get("signal_strong_buy", lang),
                RigourBullish,
                RigourBullish.copy(alpha = 0.15f)
            )
            SignalType.BUY -> Triple(
                StringsDictionary.get("signal_buy", lang),
                RigourBullish,
                RigourBullish.copy(alpha = 0.12f)
            )
            SignalType.STRONG_SELL -> Triple(
                StringsDictionary.get("signal_strong_sell", lang),
                RigourBearish,
                RigourBearish.copy(alpha = 0.15f)
            )
            SignalType.SELL -> Triple(
                StringsDictionary.get("signal_sell", lang),
                RigourBearish,
                RigourBearish.copy(alpha = 0.12f)
            )
            SignalType.NEUTRAL -> Triple(
                StringsDictionary.get("signal_neutral", lang),
                RigourGold,
                RigourGold.copy(alpha = 0.12f)
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        RigourSurfaceVariant,
                        RigourSurface
                    )
                )
            )
            .border(1.dp, signalColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = StringsDictionary.get("overall_signal", lang),
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = signalText,
                        color = signalColor,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Confidence pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(signalBg)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(signalColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${result.confidencePercent}%",
                            color = signalColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Conviction meter bar
            LinearProgressIndicator(
                progress = { result.confidencePercent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = signalColor,
                trackColor = Color.DarkGray
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3-Indicator voting breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IndicatorVotePill(
                    name = "RSI",
                    signal = result.rsiSignal,
                    value = String.format(Locale.US, "%.1f", result.rsi)
                )
                IndicatorVotePill(
                    name = "MACD",
                    signal = result.macdSignal,
                    value = if (result.macdHistogram > 0) "+Bull" else "-Bear"
                )
                IndicatorVotePill(
                    name = "Bollinger",
                    signal = result.bbSignal,
                    value = "2σ Zone"
                )
            }
        }
    }
}

@Composable
fun IndicatorVotePill(
    name: String,
    signal: SignalType,
    value: String
) {
    val (color, icon) = when (signal) {
        SignalType.STRONG_BUY, SignalType.BUY -> RigourBullish to Icons.Default.ArrowUpward
        SignalType.STRONG_SELL, SignalType.SELL -> RigourBearish to Icons.Default.ArrowDownward
        SignalType.NEUTRAL -> RigourGold to Icons.Default.Remove
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E293B))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$name: $value",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun RsiVisualizerCard(
    rsiValue: Double,
    description: String,
    lang: Language,
    modifier: Modifier = Modifier
) {
    val rsiColor = when {
        rsiValue >= 70 -> RigourBearish
        rsiValue <= 30 -> RigourBullish
        else -> RigourCyan
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(RigourSurfaceVariant.copy(alpha = 0.6f))
            .border(1.dp, RigourCardBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = StringsDictionary.get("rsi_indicator", lang),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = String.format(Locale.US, "%.1f", rsiValue),
                    color = rsiColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // RSI Slider track with 30 & 70 thresholds
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
            ) {
                val trackWidth = size.width
                val trackHeight = 6.dp.toPx()
                val yCenter = size.height / 2f

                // Base track
                drawRoundRect(
                    color = Color(0xFF334155),
                    topLeft = Offset(0f, yCenter - trackHeight / 2f),
                    size = androidx.compose.ui.geometry.Size(trackWidth, trackHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                )

                // 30 threshold line
                val x30 = trackWidth * 0.30f
                drawLine(
                    color = RigourBullish,
                    start = Offset(x30, yCenter - 6.dp.toPx()),
                    end = Offset(x30, yCenter + 6.dp.toPx()),
                    strokeWidth = 2.dp.toPx()
                )

                // 70 threshold line
                val x70 = trackWidth * 0.70f
                drawLine(
                    color = RigourBearish,
                    start = Offset(x70, yCenter - 6.dp.toPx()),
                    end = Offset(x70, yCenter + 6.dp.toPx()),
                    strokeWidth = 2.dp.toPx()
                )

                // Pointer
                val xPointer = (trackWidth * (rsiValue / 100f)).toFloat().coerceIn(4f, trackWidth - 4f)
                drawCircle(
                    color = rsiColor,
                    radius = 5.dp.toPx(),
                    center = Offset(xPointer, yCenter)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("0 (Oversold <30)", color = RigourBullish, fontSize = 10.sp)
                Text("50", color = Color.Gray, fontSize = 10.sp)
                Text("100 (Overbought >70)", color = RigourBearish, fontSize = 10.sp)
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                color = Color.LightGray,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun MacdVisualizerCard(
    macd: Double,
    signal: Double,
    histogram: Double,
    description: String,
    lang: Language,
    modifier: Modifier = Modifier
) {
    val isBullish = histogram >= 0
    val histColor = if (isBullish) ChartCandleGreen else ChartCandleRed

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(RigourSurfaceVariant.copy(alpha = 0.6f))
            .border(1.dp, RigourCardBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = StringsDictionary.get("macd_indicator", lang),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row {
                    Text(
                        text = "MACD: " + com.example.ui.util.PriceFormatter.formatIndicator(macd),
                        color = ChartMacdLine,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sig: " + com.example.ui.util.PriceFormatter.formatIndicator(signal),
                        color = ChartSignalLine,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Histogram visualizer bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Histogram:", color = Color.Gray, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(histColor.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = com.example.ui.util.PriceFormatter.formatIndicator(histogram),
                        color = histColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                color = Color.LightGray,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun BollingerVisualizerCard(
    upper: Double,
    middle: Double,
    lower: Double,
    currentPrice: Double,
    description: String,
    lang: Language,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(RigourSurfaceVariant.copy(alpha = 0.6f))
            .border(1.dp, RigourCardBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = StringsDictionary.get("bollinger_indicator", lang),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "20 SMA, 2σ",
                    color = RigourCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Upper (Res)", color = Color.Gray, fontSize = 10.sp)
                    Text(com.example.ui.util.PriceFormatter.formatPrice(upper), color = RigourBearish, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Middle (SMA)", color = Color.Gray, fontSize = 10.sp)
                    Text(com.example.ui.util.PriceFormatter.formatPrice(middle), color = RigourCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Lower (Supp)", color = Color.Gray, fontSize = 10.sp)
                    Text(com.example.ui.util.PriceFormatter.formatPrice(lower), color = RigourBullish, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                color = Color.LightGray,
                fontSize = 12.sp
            )
        }
    }
}
