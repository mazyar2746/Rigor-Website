package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.CoinSignalData
import com.example.data.model.CryptoCoin
import com.example.data.model.SignalType
import com.example.data.model.UserSubscription
import com.example.i18n.Language
import com.example.i18n.StringsDictionary
import com.example.ui.theme.RigourBackground
import com.example.ui.theme.RigourBearish
import com.example.ui.theme.RigourBullish
import com.example.ui.theme.RigourCardBorder
import com.example.ui.theme.RigourCyan
import com.example.ui.theme.RigourGold
import com.example.ui.theme.RigourSurface
import com.example.ui.theme.RigourSurfaceVariant

enum class SignalFilter {
    ALL,
    BUY_ONLY,
    SELL_ONLY,
    HIGH_CONFIDENCE
}

@Composable
fun SignalsScreen(
    signals: List<CoinSignalData>,
    isLoading: Boolean,
    subscription: UserSubscription,
    lang: Language,
    onRefresh: () -> Unit,
    onSelectCoin: (CryptoCoin) -> Unit,
    onNavigateToSubscription: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf(SignalFilter.ALL) }

    val filteredList = remember(signals, selectedFilter) {
        when (selectedFilter) {
            SignalFilter.ALL -> signals
            SignalFilter.BUY_ONLY -> signals.filter {
                it.analysis.overallSignal == SignalType.BUY || it.analysis.overallSignal == SignalType.STRONG_BUY
            }
            SignalFilter.SELL_ONLY -> signals.filter {
                it.analysis.overallSignal == SignalType.SELL || it.analysis.overallSignal == SignalType.STRONG_SELL
            }
            SignalFilter.HIGH_CONFIDENCE -> signals.filter {
                it.isProSignal || it.analysis.confidencePercent >= 75
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(RigourBackground)
            .testTag("signals_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header Banner & Pro Accuracy Explanation Card
        item {
            SignalsExplorationBanner(
                isPro = subscription.isActive,
                lang = lang,
                onUpgrade = onNavigateToSubscription
            )
        }

        // 2. Filter Chips & Refresh Action
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    FilterChip(
                        selected = selectedFilter == SignalFilter.ALL,
                        onClick = { selectedFilter = SignalFilter.ALL },
                        label = { Text(StringsDictionary.get("filter_all", lang), fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RigourCyan.copy(alpha = 0.2f),
                            selectedLabelColor = RigourCyan,
                            containerColor = RigourSurface,
                            labelColor = Color.Gray
                        ),
                        modifier = Modifier.testTag("filter_all_chip")
                    )

                    FilterChip(
                        selected = selectedFilter == SignalFilter.BUY_ONLY,
                        onClick = { selectedFilter = SignalFilter.BUY_ONLY },
                        label = { Text(StringsDictionary.get("filter_buy", lang), fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RigourBullish.copy(alpha = 0.2f),
                            selectedLabelColor = RigourBullish,
                            containerColor = RigourSurface,
                            labelColor = Color.Gray
                        ),
                        modifier = Modifier.testTag("filter_buy_chip")
                    )

                    FilterChip(
                        selected = selectedFilter == SignalFilter.SELL_ONLY,
                        onClick = { selectedFilter = SignalFilter.SELL_ONLY },
                        label = { Text(StringsDictionary.get("filter_sell", lang), fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RigourBearish.copy(alpha = 0.2f),
                            selectedLabelColor = RigourBearish,
                            containerColor = RigourSurface,
                            labelColor = Color.Gray
                        ),
                        modifier = Modifier.testTag("filter_sell_chip")
                    )
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.testTag("signals_refresh_btn")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = RigourCyan,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Signals",
                            tint = RigourCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 3. Signals List
        if (isLoading && signals.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = RigourCyan)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (lang == Language.PERSIAN) "در حال محاسبه سیگنال‌های همگرایی..." else "Calculating confluence signals...",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else if (filteredList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (lang == Language.PERSIAN) "هیچ سیگنالی برای فیلتر انتخاب شده یافت نشد." else "No signals found for selected filter.",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            items(filteredList, key = { it.coin.symbol }) { signalItem ->
                SignalItemCard(
                    signalItem = signalItem,
                    isProUser = subscription.isActive,
                    lang = lang,
                    onSelectCoin = { onSelectCoin(signalItem.coin) },
                    onUpgrade = onNavigateToSubscription
                )
            }
        }
    }
}

@Composable
fun SignalsExplorationBanner(
    isPro: Boolean,
    lang: Language,
    onUpgrade: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("signals_banner_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = RigourSurface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isPro) RigourGold.copy(alpha = 0.5f) else RigourCardBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = if (isPro) {
                            listOf(RigourGold.copy(alpha = 0.12f), RigourSurface)
                        } else {
                            listOf(Color(0xFF1E293B).copy(alpha = 0.5f), RigourSurface)
                        }
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isPro) RigourGold.copy(alpha = 0.2f) else RigourCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPro) Icons.Default.WorkspacePremium else Icons.Default.AutoGraph,
                            contentDescription = null,
                            tint = if (isPro) RigourGold else RigourCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = StringsDictionary.get("pro_signals_banner_title", lang),
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isPro) {
                                if (lang == Language.PERSIAN) "اشتراک فعال است • دقت بالا آنلاک شده" else "Pro Active • High Accuracy Unlocked"
                            } else {
                                if (lang == Language.PERSIAN) "تفاوت دقت سیگنال‌های عادی و پرو" else "Standard vs Pro Accuracy Comparison"
                            },
                            color = if (isPro) RigourGold else Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                }

                if (isPro) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(RigourBullish.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Verified,
                                contentDescription = null,
                                tint = RigourBullish,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ACTIVE",
                                color = RigourBullish,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Explanation Text
            Text(
                text = StringsDictionary.get("pro_signals_banner_desc", lang),
                color = Color(0xFFCBD5E1),
                fontSize = 12.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Comparison Matrix: Standard vs Pro
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF090D16))
                    .border(1.dp, RigourCardBorder.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Free/Normal Column
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color.Gray)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (lang == Language.PERSIAN) "سیگنال‌های عادی" else "Standard Signals",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (lang == Language.PERSIAN) "• دقت اولیه (~۵۵٪-۶۵٪)\n• تک اندیکاتور\n• بدون تارگت و حد ضرر" else "• Basic Accuracy (~60%)\n• Single Indicator\n• No Targets / Stop-Loss",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(55.dp)
                        .background(RigourCardBorder)
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Pro Column
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(RigourBullish)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (lang == Language.PERSIAN) "سیگنال‌های پرو (High-Confidence)" else "PRO High-Confidence",
                            color = RigourGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (lang == Language.PERSIAN) "• دقت فوق‌العاده (۸۵٪-۹۸٪)\n• تلاقی ۳ اندیکاتور همزمان\n• تارگت‌های دقیق و حد ضرر" else "• High Accuracy (85%-98%)\n• 3-Indicator Confluence\n• Exact Targets & Stop Loss",
                        color = RigourCyan,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
            }

            if (!isPro) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onUpgrade,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("signals_upgrade_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RigourCyan
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ElectricBolt,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = StringsDictionary.get("unlock_pro", lang),
                        color = Color.Black,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SignalItemCard(
    signalItem: CoinSignalData,
    isProUser: Boolean,
    lang: Language,
    onSelectCoin: () -> Unit,
    onUpgrade: () -> Unit
) {
    val coin = signalItem.coin
    val analysis = signalItem.analysis
    val isProSignal = signalItem.isProSignal
    val isLocked = isProSignal && !isProUser

    val (signalText, signalColor, signalBg) = when (analysis.overallSignal) {
        SignalType.STRONG_BUY -> Triple(
            StringsDictionary.get("signal_strong_buy", lang),
            Color(0xFF00FF88),
            Color(0xFF00FF88).copy(alpha = 0.15f)
        )
        SignalType.BUY -> Triple(
            StringsDictionary.get("signal_buy", lang),
            RigourBullish,
            RigourBullish.copy(alpha = 0.15f)
        )
        SignalType.SELL -> Triple(
            StringsDictionary.get("signal_sell", lang),
            RigourBearish,
            RigourBearish.copy(alpha = 0.15f)
        )
        SignalType.STRONG_SELL -> Triple(
            StringsDictionary.get("signal_strong_sell", lang),
            Color(0xFFFF3366),
            Color(0xFFFF3366).copy(alpha = 0.15f)
        )
        SignalType.NEUTRAL -> Triple(
            StringsDictionary.get("signal_neutral", lang),
            Color(0xFF94A3B8),
            Color(0xFF94A3B8).copy(alpha = 0.12f)
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectCoin() }
            .testTag("signal_card_${coin.symbol}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = RigourSurface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isProSignal && isProUser) RigourGold.copy(alpha = 0.4f) else RigourCardBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Row 1: Coin info and Signal badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (coin.iconUrl.isNotBlank()) {
                        AsyncImage(
                            model = coin.iconUrl,
                            contentDescription = coin.name,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(RigourSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = coin.symbol.take(3),
                                color = RigourCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = coin.symbol,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (isProSignal) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(RigourGold.copy(alpha = 0.2f))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "PRO HIGH ACCURACY",
                                        color = RigourGold,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Text(
                            text = coin.name,
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                }

                // Overall Signal Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(signalBg)
                        .border(1.dp, signalColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = signalText,
                        color = signalColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Price and Confidence meter
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (lang == Language.PERSIAN) "قیمت فعلی" else "Current Price",
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "$${formatPrice(coin.priceUsd)}",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Confidence pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1E293B))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (lang == Language.PERSIAN) "درجه اطمینان:" else "Conviction:",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${analysis.confidencePercent}%",
                            color = if (analysis.confidencePercent >= 75) RigourBullish else RigourCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Indicators summary pill flow
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // RSI pill
                IndicatorPill(
                    label = "RSI: ${analysis.rsi.toInt()}",
                    color = when (analysis.rsiSignal) {
                        SignalType.BUY, SignalType.STRONG_BUY -> RigourBullish
                        SignalType.SELL, SignalType.STRONG_SELL -> RigourBearish
                        else -> Color.Gray
                    }
                )

                // MACD pill
                IndicatorPill(
                    label = "MACD: ${if (analysis.macdHistogram >= 0) "+Bullish" else "-Bearish"}",
                    color = if (analysis.macdHistogram >= 0) RigourBullish else RigourBearish
                )

                // Bollinger pill
                IndicatorPill(
                    label = if (coin.priceUsd <= analysis.bbLower * 1.01) "BB Support"
                    else if (coin.priceUsd >= analysis.bbUpper * 0.99) "BB Resist"
                    else "BB Normal",
                    color = if (coin.priceUsd <= analysis.bbLower * 1.01) RigourBullish else Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Targets & Stop Loss (Unlocked for Pro, Locked for Free if High-Confidence)
            if (isLocked) {
                // Locked Pro Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF090D16))
                        .border(1.dp, RigourGold.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = RigourGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = StringsDictionary.get("pro_locked_signal", lang),
                                color = RigourGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (lang == Language.PERSIAN) "نقاط ورود، حد سود ۱ و ۲ و حد ضرر این سیگنال فوق‌العاده فقط برای کاربران پرو در دسترس است."
                            else "Entry points, Target 1 & 2, and stop-loss are exclusively available to Pro members.",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onUpgrade,
                            colors = ButtonDefaults.buttonColors(containerColor = RigourGold),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(
                                text = StringsDictionary.get("unlock_pro_signal_cta", lang),
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                // Unlocked Targets & Stop Loss
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF090D16))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = StringsDictionary.get("short_term_target", lang),
                            color = RigourBullish,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "$${formatPrice(analysis.shortTermTarget)}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column {
                        Text(
                            text = StringsDictionary.get("medium_term_target", lang),
                            color = RigourCyan,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "$${formatPrice(analysis.mediumTermTarget)}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column {
                        Text(
                            text = StringsDictionary.get("stop_loss", lang),
                            color = RigourBearish,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "$${formatPrice(analysis.stopLoss)}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IndicatorPill(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}


