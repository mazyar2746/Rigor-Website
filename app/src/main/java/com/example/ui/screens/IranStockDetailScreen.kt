package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SignalType
import com.example.data.repository.FullAnalysisResult
import com.example.data.repository.IranStockAnalysisService
import com.example.engine.SignalInterpreter
import com.example.i18n.Language
import com.example.i18n.StringsDictionary
import com.example.ui.theme.RigourBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IranStockDetailScreen(
    stockName: String,
    onBack: () -> Unit,
    lang: Language = Language.PERSIAN
) {
    val context = LocalContext.current
    var result by remember { mutableStateOf<FullAnalysisResult?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var retryTrigger by remember { mutableStateOf(0) }

    val scope = rememberCoroutineScope()
    val service = remember { IranStockAnalysisService(context) }

    LaunchedEffect(stockName, retryTrigger) {
        isLoading = true
        errorMsg = null

        val timeoutJob = scope.launch {
            delay(20000)
            if (isLoading) {
                isLoading = false
                errorMsg = "زمان دریافت اطلاعات به پایان رسید. لطفاً دوباره تلاش کنید."
            }
        }

        scope.launch {
            try {
                val analysis = service.analyzeStock(stockName)
                if (analysis == null) {
                    errorMsg = "دیتای این نماد در دسترس نیست."
                } else {
                    result = analysis
                }
            } catch (e: Exception) {
                errorMsg = "خطا در ارتباط با سرور: ${e.message}"
            } finally {
                isLoading = false
                timeoutJob.cancel()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "${StringsDictionary.get("iran_stock_analysis", lang)} $stockName", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF00BCD4), titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(RigourBackground)) {
            if (isLoading) {
                Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(color = Color(0xFF00BCD4))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("در حال دریافت اطلاعات از بازار...", color = Color.White, fontSize = 14.sp)
                }
            } else if (errorMsg != null) {
                Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(text = "⚠️", fontSize = 48.sp, modifier = Modifier.padding(bottom = 16.dp))
                    Text(text = errorMsg!!, color = Color(0xFFFCA5A5), fontSize = 16.sp, textAlign = TextAlign.Center, lineHeight = 24.sp)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { retryTrigger++ },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BCD4)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تلاش مجدد", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            } else if (result != null) {
                val tech = result!!.technical
                val fund = result!!.fundamental
                val finalSignal = SignalInterpreter.getFinalSignal(tech.rsiSignal, result!!.smartMoneyText)
                val reasoning = SignalInterpreter.getReasoningText(tech, result!!.smartMoneyText, lang)
                val signalLabel = SignalInterpreter.getSignalLabel(finalSignal, lang)

                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${StringsDictionary.get("iran_stock_price", lang)}: ${formatPrice(tech.currentPrice)}",
                                color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold
                            )
                            
                            // نمایش وضعیت و ساعت آپدیت
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                Text(
                                    text = tech.dataSourceNotice ?: "",
                                    color = if (tech.provenance.name == "LIVE_EXCHANGE") Color(0xFF22C55E) else Color(0xFFEAB308),
                                    fontSize = 10.sp, fontWeight = FontWeight.Medium
                                )
                                if (result!!.lastUpdated > 0) {
                                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                                    Text(
                                        text = " | بروزرسانی: ${timeFormat.format(Date(result!!.lastUpdated))}",
                                        color = Color(0xFF94A3B8), fontSize = 10.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            SignalBadge(signal = finalSignal, label = signalLabel)
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "📈 سطوح کلیدی", color = Color(0xFF00BCD4), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            SmartLevelRow("مقاومت ۲", tech.resistance2, tech.currentPrice, isResistance = true)
                            SmartLevelRow("مقاومت ۱", tech.resistance1, tech.currentPrice, isResistance = true)
                            InfoRow("پیوت", formatPrice(tech.pivotPoint), Color.White)
                            SmartLevelRow("حمایت ۱", tech.support1, tech.currentPrice, isResistance = false)
                            SmartLevelRow("حمایت ۲", tech.support2, tech.currentPrice, isResistance = false)
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "📊 اندیکاتورهای تکنیکال", color = Color(0xFF00BCD4), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            InfoRow("شاخص قدرت نسبی (RSI)", String.format("%.1f", tech.rsi), Color.White)
                            Text(tech.rsiDescription, color = Color(0xFF94A3B8), fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.padding(bottom = 12.dp, top = 2.dp))
                            InfoRow("هیستوگرام MACD", String.format("%.3f", tech.macdHistogram), Color.White)
                            Text(tech.macdDescription, color = Color(0xFF94A3B8), fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.padding(bottom = 12.dp, top = 2.dp))
                            InfoRow("وضعیت بولینگر", tech.bbSignal.name, Color.White)
                            Text(tech.bbDescription, color = Color(0xFF94A3B8), fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                    }

                    if (fund != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = "📊 " + StringsDictionary.get("iran_stock_basic_info", lang), color = Color(0xFF00BCD4), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                fund.sectorName?.let { InfoRow(StringsDictionary.get("iran_stock_sector", lang), it, Color.White) }
                                fund.estimatedEPS?.let { InfoRow(StringsDictionary.get("iran_stock_eps", lang), "${formatPrice(it)} ریال", Color.White) }
                                fund.sectorPE?.let { InfoRow(StringsDictionary.get("iran_stock_pe", lang), String.format("%.2f", it), Color.White) }
                                fund.totalShares?.let { InfoRow("تعداد کل سهام", "${formatPrice(it / 1_000_000_000)} میلیارد", Color.White) }
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "🧠 " + StringsDictionary.get("iran_stock_reasoning_title", lang), color = Color(0xFF00BCD4), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = reasoning, color = Color(0xFFE2E8F0), fontSize = 14.sp, lineHeight = 22.sp)
                        }
                    }
                }
            }
        }
    }
}

private fun formatPrice(value: Double?): String {
    if (value == null || value.isNaN() || value.isInfinite()) return "---"
    return DecimalFormat("#,###").format(value)
}

@Composable
fun SmartLevelRow(label: String, level: Double, currentPrice: Double, isResistance: Boolean) {
    val isBroken = if (isResistance) currentPrice > level else currentPrice < level
    val displayLabel = if (isBroken) "$label (شکسته شده)" else label
    val valueColor = if (isBroken) Color(0xFFFCA5A5) else Color.White
    
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = displayLabel, color = if (isBroken) Color(0xFFFCA5A5) else Color(0xFF94A3B8), fontSize = 14.sp)
        Text(text = formatPrice(level), color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun InfoRow(label: String, value: String, valueColor: Color = Color.White) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, color = Color(0xFF94A3B8), fontSize = 14.sp)
        Text(text = value, color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SignalBadge(signal: SignalType, label: String) {
    val color = when (signal) {
        SignalType.STRONG_BUY -> Color(0xFF22C55E)
        SignalType.BUY -> Color(0xFF86EFAC)
        SignalType.NEUTRAL -> Color(0xFFEAB308)
        SignalType.SELL -> Color(0xFFFCA5A5)
        SignalType.STRONG_SELL -> Color(0xFFEF4444)
    }
    Box(modifier = Modifier.background(color, RoundedCornerShape(20.dp)).padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text(text = label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}
