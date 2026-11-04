package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
    val scope = rememberCoroutineScope()
    val service = remember { IranStockAnalysisService(context) }

    LaunchedEffect(stockName) {
        isLoading = true
        errorMsg = null
        
        val timeoutJob = scope.launch {
            delay(10000) // ۱۰ ثانیه
            if (isLoading) {
                isLoading = false
                errorMsg = "زمان دریافت اطلاعات به پایان رسید. لطفاً دوباره تلاش کنید."
            }
        }
        
        scope.launch {
            try {
                val analysis = service.analyzeStock(stockName)
                if (analysis == null) {
                    errorMsg = "دیتای قیمتی این نماد در دسترس نیست. احتمالاً نماد متوقف بوده یا در صف مجمع/افزایش سرمایه قرار دارد."
                } else {
                    result = analysis
                }
            } catch (e: Exception) {
                errorMsg = "خطای غیرمنتظره: ${e.message}"
            } finally {
                isLoading = false
                timeoutJob.cancel()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${StringsDictionary.get("iran_stock_analysis", lang)} $stockName", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF00BCD4),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(RigourBackground)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF00BCD4))
                }
            } else if (errorMsg != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = errorMsg!!, color = Color.Red, fontSize = 16.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(24.dp))
                }
            } else if (result != null) {
                val tech = result!!.technical
                val fund = result!!.fundamental
                val finalSignal = SignalInterpreter.getFinalSignal(tech.rsiSignal, result!!.smartMoneyText)
                val reasoning = SignalInterpreter.getReasoningText(tech, result!!.smartMoneyText, lang)
                val signalLabel = SignalInterpreter.getSignalLabel(finalSignal, lang)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // قیمت و سیگنال
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${StringsDictionary.get("iran_stock_price", lang)}: ${String.format("%,.0f", tech.currentPrice)}",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            SignalBadge(signal = finalSignal, label = signalLabel)
                        }
                    }

                    // اطلاعات بنیادی
                    if (fund != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = "📊 اطلاعات بنیادی", color = Color(0xFF00BCD4), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                fund.sectorName?.let { InfoRow("صنعت", it) }
                                fund.estimatedEPS?.let { InfoRow("EPS تخمینی", "${String.format("%,.0f", it)} ریال") }
                                fund.sectorPE?.let { InfoRow("P/E صنعت", String.format("%.2f", it)) }
                                fund.weekLow?.let { low -> fund.weekHigh?.let { high -> InfoRow("محدوده هفته", "${String.format("%,.0f", low)} - ${String.format("%,.0f", high)}") } }
                                fund.yearLow?.let { low -> fund.yearHigh?.let { high -> InfoRow("محدوده سال", "${String.format("%,.0f", low)} - ${String.format("%,.0f", high)}") } }
                            }
                        }
                    }

                    // پول هوشمند
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "💰 تحلیل پول هوشمند (حقیقی/حقوقی)", color = Color(0xFF00BCD4), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            if (result!!.smartMoneyText != null) {
                                Text(text = result!!.smartMoneyText!!, color = Color(0xFFE2E8F0), fontSize = 14.sp, lineHeight = 22.sp)
                            } else {
                                Text(text = "دیتای پول هوشمند برای این نماد در دسترس نیست.", color = Color(0xFF94A3B8), fontSize = 14.sp)
                            }
                        }
                    }

                    // منطق تحلیل
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

@Composable
fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, color = Color(0xFF94A3B8), fontSize = 14.sp)
        Text(text = value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
