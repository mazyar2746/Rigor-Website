package com.example.ui.screens

import com.example.ui.components.CoinIcon

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CryptoCoin
import com.example.data.model.MarketDataProvenance
import com.example.data.model.TechnicalAnalysisResult
import com.example.data.model.UserSubscription
import com.example.i18n.Language
import com.example.i18n.StringsDictionary
import com.example.ui.components.BollingerVisualizerCard
import com.example.ui.components.CandlestickChartCanvas
import com.example.ui.components.MacdVisualizerCard
import com.example.ui.components.RsiVisualizerCard
import com.example.ui.components.SignalConfluenceCard
import com.example.ui.components.TargetsAndLevelsCard
import com.example.ui.theme.RigourBackground
import com.example.ui.theme.RigourBearish
import com.example.ui.theme.RigourBullish
import com.example.ui.theme.RigourCardBorder
import com.example.ui.theme.RigourCyan
import com.example.ui.theme.RigourGold
import com.example.ui.theme.RigourSurface
import com.example.ui.theme.RigourSurfaceVariant
import java.util.Locale

@Composable
fun CoinDetailScreen(
    coin: CryptoCoin,
    analysisResult: TechnicalAnalysisResult?,
    isLoadingAnalysis: Boolean,
    currentTimeframe: String,
    onSelectTimeframe: (String) -> Unit,
    subscription: UserSubscription,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onBack: () -> Unit,
    onNavigateToSubscription: () -> Unit,
    lang: Language
) {
    val scrollState = rememberScrollState()
    val isPositive = coin.change24h >= 0
    val changeColor = if (isPositive) RigourBullish else RigourBearish
    val changePrefix = if (isPositive) "+" else ""

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RigourBackground)
    ) {
        // Top Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("detail_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                CoinIcon(
                    iconUrl = coin.iconUrl,
                    symbol = coin.symbol,
                    name = coin.name,
                    size = 34.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "${coin.name} (${coin.symbol})",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Rank #${coin.rank}",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            }

            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) RigourGold else Color.Gray
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Price & 24h Stats Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = formatPrice(coin.priceUsd),
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(changeColor.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = String.format(Locale.US, "%s%.2f%% 24h", changePrefix, coin.change24h),
                                color = changeColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 24h High / Low Stats
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "24h High: " + formatPrice(coin.high24h),
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                    Text(
                        text = "24h Low: " + formatPrice(coin.low24h),
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Timeframe Selector Tabs (1H, 4H, 1D, 1W)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(RigourSurfaceVariant)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(
                    "1h" to StringsDictionary.get("timeframe_1h", lang),
                    "4h" to StringsDictionary.get("timeframe_4h", lang),
                    "1d" to StringsDictionary.get("timeframe_1d", lang),
                    "1w" to StringsDictionary.get("timeframe_1w", lang)
                ).forEach { (tfKey, tfLabel) ->
                    val isSelected = currentTimeframe == tfKey
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) RigourCyan else Color.Transparent)
                            .clickable { onSelectTimeframe(tfKey) }
                            .testTag("tf_tab_$tfKey")
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tfLabel,
                            color = if (isSelected) Color(0xFF0F172A) else Color.LightGray,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Market Data Provenance Status Badge
            if (analysisResult != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val (badgeBg, badgeBorder, badgeText, badgeIcon, label) = when (analysisResult.provenance) {
                        MarketDataProvenance.LIVE_EXCHANGE -> {
                            Triple(
                                RigourBullish.copy(alpha = 0.15f),
                                RigourBullish.copy(alpha = 0.4f),
                                RigourBullish
                            ) to (Icons.Default.CheckCircle to (analysisResult.dataSourceNotice ?: "Live Market Data"))
                        }
                        MarketDataProvenance.CACHED_EXCHANGE -> {
                            Triple(
                                RigourCyan.copy(alpha = 0.15f),
                                RigourCyan.copy(alpha = 0.4f),
                                RigourCyan
                            ) to (Icons.Default.History to (analysisResult.dataSourceNotice ?: "Cached Market Data"))
                        }
                        MarketDataProvenance.SYNTHETIC -> {
                            Triple(
                                RigourGold.copy(alpha = 0.15f),
                                RigourGold.copy(alpha = 0.5f),
                                RigourGold
                            ) to (Icons.Default.Warning to "SIMULATION PREVIEW (Signals Locked)")
                        }
                        MarketDataProvenance.UNAVAILABLE -> {
                            Triple(
                                Color.Gray.copy(alpha = 0.15f),
                                Color.Gray.copy(alpha = 0.4f),
                                Color.LightGray
                            ) to (Icons.Default.Info to "MARKET DATA UNAVAILABLE")
                        }
                    }.let { (colors, iconAndLabel) ->
                        arrayOf(colors.first, colors.second, colors.third, iconAndLabel.first, iconAndLabel.second)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeBg as Color)
                            .border(1.dp, badgeBorder as Color, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = badgeIcon as androidx.compose.ui.graphics.vector.ImageVector,
                                contentDescription = null,
                                tint = badgeText as Color,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = label as String,
                                color = badgeText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = "TF: $currentTimeframe",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Explicit simulation warning banner: Never mislead user about synthetic data
                if (analysisResult.provenance == MarketDataProvenance.SYNTHETIC) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(RigourGold.copy(alpha = 0.10f))
                            .border(1.dp, RigourGold.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = RigourGold,
                                modifier = Modifier.size(16.dp).padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (lang == Language.PERSIAN) {
                                    "توجه: داده‌های تاریخی صرافی برای جفت‌ارز ${coin.symbol} موقتاً در دسترس نیست. این نمودار صرفاً شبیه‌سازی نمایشی است. جهت حفاظت از سرمایه شما، سیگنال‌ها و تارگت‌های قیمتی قفل شده‌اند."
                                } else {
                                    "Notice: Live exchange candlestick history is currently unavailable for ${coin.symbol}. Displaying synthetic simulation for chart preview only. Trading signals and targets are locked to protect trading capital."
                                },
                                color = RigourGold,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // Disconnection / Market Data Unavailable banner
                if (analysisResult.provenance == MarketDataProvenance.UNAVAILABLE || analysisResult.candles.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF3B1D28))
                            .border(1.dp, RigourBearish.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = RigourBearish,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (lang == Language.PERSIAN) {
                                    "ارائه تحلیل در این زمان ممکن نیست. ارتباط با صرافی‌ها برقرار نشد؛ جهت حفظ سلامت مالی، از ارائه تحلیل فیک خودداری می‌شود."
                                } else {
                                    "Analysis is not available at this time. Live exchange feeds disconnected; fake or synthetic data is strictly prohibited."
                                },
                                color = Color.White,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // Candlestick Chart with Bollinger Bands
            if (analysisResult != null) {
                CandlestickChartCanvas(
                    candles = analysisResult.candles,
                    bbUpper = analysisResult.bbUpper,
                    bbMiddle = analysisResult.bbMiddle,
                    bbLower = analysisResult.bbLower
                )
            } else if (isLoadingAnalysis) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .background(RigourSurfaceVariant, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = RigourCyan)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Technical Analysis & Paywall Section
            Text(
                text = StringsDictionary.get("technical_analysis", lang),
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (analysisResult != null && (subscription.isActive || analysisResult.confidencePercent < 85)) {
                // UNLOCKED PRO EXPERIENCE
                SignalConfluenceCard(result = analysisResult, lang = lang)

                Spacer(modifier = Modifier.height(12.dp))

                RsiVisualizerCard(
                    rsiValue = analysisResult.rsi,
                    description = analysisResult.rsiDescription,
                    lang = lang
                )

                Spacer(modifier = Modifier.height(10.dp))

                MacdVisualizerCard(
                    macd = analysisResult.macdLine,
                    signal = analysisResult.macdSignalLine,
                    histogram = analysisResult.macdHistogram,
                    description = analysisResult.macdDescription,
                    lang = lang
                )

                Spacer(modifier = Modifier.height(10.dp))

                BollingerVisualizerCard(
                    upper = analysisResult.bbUpper,
                    middle = analysisResult.bbMiddle,
                    lower = analysisResult.bbLower,
                    currentPrice = analysisResult.currentPrice,
                    description = analysisResult.bbDescription,
                    lang = lang
                )

                Spacer(modifier = Modifier.height(12.dp))

                TargetsAndLevelsCard(result = analysisResult, lang = lang)

            } else {
                // LOCKED PAYWALL CARD
                PaywallGateCard(
                    onUpgrade = onNavigateToSubscription,
                    lang = lang
                )

                // Teaser preview of indicators
                if (analysisResult != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(RigourSurfaceVariant.copy(alpha = 0.4f))
                            .border(1.dp, RigourCardBorder.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("RSI Indicator (14)", color = Color.Gray, fontSize = 12.sp)
                                Text("🔒 Pro Locked", color = RigourGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("MACD Trend Confluence (12, 26, 9) • 🔒 Pro Locked", color = Color.Gray, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Bollinger Bands 2σ Envelope • 🔒 Pro Locked", color = Color.Gray, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Automated Short & Medium Price Targets • 🔒 Pro Locked", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

@Composable
fun PaywallGateCard(
    onUpgrade: () -> Unit,
    lang: Language
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E293B),
                        Color(0xFF0F172A)
                    )
                )
            )
            .border(1.5.dp, RigourGold.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(RigourGold.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = RigourGold,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = StringsDictionary.get("paywall_title", lang),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = StringsDictionary.get("paywall_desc", lang),
                color = Color(0xFF94A3B8),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onUpgrade,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("paywall_upgrade_cta_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = RigourGold),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = StringsDictionary.get("upgrade_cta", lang),
                    color = Color(0xFF0F172A),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
