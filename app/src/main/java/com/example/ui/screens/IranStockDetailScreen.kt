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
import com.example.i18n.Language
import com.example.i18n.StringsDictionary
import com.example.ui.theme.RigourBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
        isLoading = true; errorMsg = null
        val timeoutJob = scope.launch { delay(20000); if (isLoading) { isLoading = false; errorMsg = "زمان دریافت اطلاعات به پایان رسید." } }
        scope.launch {
            try {
                val analysis = service.analyzeStock(stockName)
                if (analysis == null) errorMsg = "دیتای این نماد در دسترس نیست." else result = analysis
            } catch (e: Exception) { errorMsg = "خطا: ${e.message}" } finally { isLoading = false; timeoutJob.cancel() }
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
                    CircularProgressIndicator(color = Color(0xFF00BCD4)); Spacer(modifier = Modifier.height(16.dp)); Text("در حال دریافت اطلاعات...", color = Color.White, fontSize = 14.sp)
                }
            } else if (errorMsg != null) {
                Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("⚠️", fontSize = 48.sp, modifier = Modifier.padding(bottom = 16.dp))
                    Text(errorMsg!!, color = Color(0xFFFCA5A5), fontSize = 16.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { retryTrigger++ }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BCD4)), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("تلاش مجدد", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            } else if (result != null) {
                val tech = result!!.technical
                val fund = result!!.fundamental
                
                val finalSignal = tech.overallSignal 
                
                val reasoning = when (finalSignal) {
                    SignalType.STRONG_BUY -> "🚀 سیگنال خرید قوی صادر شد. قیمت از سطوح مقاومتی عبور کرده است (شکست مقاومت) و مومنتوم صعودی تایید می‌شود."
                    SignalType.BUY -> "📈 سیگنال خرید. اندیکاتورها تمایل صعودی ملایم دارند و شرایط برای ورود مناسب است."
                    SignalType.STRONG_SELL -> "⚠️ سیگنال فروش قوی صادر شد. قیمت از سطوح حمایتی به پایین شکسته است (شکست حمایت) و فشار فروش شدید است."
                    SignalType.SELL -> "📉 سیگنال فروش. اندیکاتورها تمایل نزولی دارند و بهتر است احتیاط کنید."
                    SignalType.NEUTRAL -> "⏸️ سیگنال خنثی. بازار در حال تعادل است و اندیکاتورها هم‌جهت نیستند. صبر کنید."
                } + "\n\n📊 وضعیت اندیکاتورها:\n" +
                    "• RSI: ${tech.rsiDescription}\n" +
                    "• MACD: ${tech.macdDescription}\n" +
                    "• بولینگر: ${tech.bbDescription}"

                val signalLabel = when (finalSignal) {
                    SignalType.STRONG_BUY -> "خرید قوی"
                    SignalType.BUY -> "خرید"
                    SignalType.NEUTRAL -> "خنثی / صبر کنید"
                    SignalType.SELL -> "فروش"
                    SignalType.STRONG_SELL -> "فروش قوی"
                }

                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            // استفاده مستقیم از toLong برای دور زدن هرگونه فرمت دلار
                            Text("${StringsDictionary.get("iran_stock_price", lang)}: ${tech.currentPrice.toLong()}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                Text(tech.dataSourceNotice ?: "", color = if (tech.provenance.name == "LIVE_EXCHANGE") Color(0xFF22C55E) else Color(0xFFEAB308), fontSize = 10.sp)
                                if (result!!.lastUpdated > 0) {
                                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                                    Text(" | بروزرسانی: ${timeFormat.format(Date(result!!.lastUpdated))}", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            SignalBadge(signal = finalSignal, label = signalLabel)
                        }
                    }

                    DynamicLevelsCard(currentPrice = tech.currentPrice, r2 = tech.resistance2, r1 = tech.resistance1, pivot = tech.pivotPoint, s1 = tech.support1, s2 = tech.support2)

                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("📊 اندیکاتورهای تکنیکال", color = Color(0xFF00BCD4), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            InfoRow("شاخص قدرت نسبی (RSI)", String.format(Locale.US, "%.1f", tech.rsi), Color.White)
                            Text(tech.rsiDescription, color = Color(0xFF94A3B8), fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.padding(bottom = 12.dp, top = 2.dp))
                            InfoRow("هیستوگرام MACD", String.format(Locale.US, "%.3f", tech.macdHistogram), Color.White)
                            Text(tech.macdDescription, color = Color(0xFF94A3B8), fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.padding(bottom = 12.dp, top = 2.dp))
                            InfoRow("وضعیت بولینگر", tech.bbSignal.name, Color.White)
                            Text(tech.bbDescription, color = Color(0xFF94A3B8), fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                    }

                    if (fund != null) {
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)), shape = RoundedCornerShape(12.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("📊 " + StringsDictionary.get("iran_stock_basic_info", lang), color = Color(0xFF00BCD4), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                fund.sectorName?.let { InfoRow(StringsDictionary.get("iran_stock_sector", lang), it, Color.White) }
                                // استفاده مستقیم از toLong
                                fund.estimatedEPS?.let { InfoRow(StringsDictionary.get("iran_stock_eps", lang), "${it.toLong()} ریال", Color.White) }
                                fund.sectorPE?.let { InfoRow(StringsDictionary.get("iran_stock_pe", lang), String.format(Locale.US, "%.2f", it), Color.White) }
                                fund.totalShares?.let { InfoRow("تعداد کل سهام", "${(it / 1_000_000_000).toLong()} میلیارد", Color.White) }
                            }
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("🧠 " + StringsDictionary.get("iran_stock_reasoning_title", lang), color = Color(0xFF00BCD4), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(reasoning, color = Color(0xFFE2E8F0), fontSize = 14.sp, lineHeight = 22.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DynamicLevelsCard(currentPrice: Double, r2: Double, r1: Double, pivot: Double, s1: Double, s2: Double) {
    val allLevels = listOf(r2, r1, s1, s2).filter { !it.isNaN() && it > 0.0 }.distinct()
    val resistances = allLevels.filter { it > currentPrice }.sorted()
    val supports = allLevels.filter { it < currentPrice }.sortedDescending()

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("📈 سطوح کلیدی", color = Color(0xFF00BCD4), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))
            // استفاده مستقیم از toLong برای همه سطوح
            if (resistances.isEmpty()) InfoRow("مقاومت نزدیک", "بدون مقاومت (شکست کامل)", Color(0xFFFCA5A5))
            else { InfoRow("مقاومت نزدیک", "${resistances.first().toLong()}", Color(0xFFFCA5A5)); if (resistances.size > 1) InfoRow("مقاومت دوم", "${resistances[1].toLong()}", Color(0xFFFCA5A5)) }
            if (!pivot.isNaN() && pivot > 0) InfoRow("پیوت (Pivot)", "${pivot.toLong()}", Color(0xFFE2E8F0))
            if (supports.isEmpty()) InfoRow("حمایت نزدیک", "بدون حمایت (ریزش کامل)", Color(0xFF86EFAC))
            else { InfoRow("حمایت نزدیک", "${supports.first().toLong()}", Color(0xFF86EFAC)); if (supports.size > 1) InfoRow("حمایت دوم", "${supports[1].toLong()}", Color(0xFF86EFAC)) }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, valueColor: Color = Color.White) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color(0xFF94A3B8), fontSize = 14.sp)
        Text(value, color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SignalBadge(signal: SignalType, label: String) {
    val color = when (signal) {
        SignalType.STRONG_BUY -> Color(0xFF22C55E); SignalType.BUY -> Color(0xFF86EFAC)
        SignalType.NEUTRAL -> Color(0xFFEAB308); SignalType.SELL -> Color(0xFFFCA5A5)
        SignalType.STRONG_SELL -> Color(0xFFEF4444)
    }
    Box(modifier = Modifier.background(color, RoundedCornerShape(20.dp)).padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}