package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.Candle
import com.example.data.model.MarketDataProvenance
import com.example.data.model.StockMarketData
import com.example.data.model.TechnicalAnalysisResult
import com.example.engine.TechnicalAnalysisEngine
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.HttpURLConnection
import java.net.URL

data class FullAnalysisResult(
    val technical: TechnicalAnalysisResult,
    val fundamental: FundamentalData?,
    val smartMoneyText: String? = null
)

class IranStockAnalysisService(private val context: Context) {
    
    private fun normalizeSymbol(input: String): String {
        return input.replace('ي', 'ی').replace('ك', 'ک').replace('\u200c', ' ').replace(Regex("\\s+"), "").trim()
    }

    private suspend fun fetchLivePrice(insCode: String): Double? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://cdn.tsetmc.com/api/ClosingPrice/GetClosingPriceInfo/$insCode")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            if (conn.responseCode == 200) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JsonParser.parseString(text).asJsonObject
                val info = root.getAsJsonObject("closingPriceInfo")
                
                val livePrice = info?.get("pDrCotVal")?.asDouble
                if (livePrice != null && livePrice > 0) return@withContext livePrice
                val pClosing = info?.get("pClosing")?.asDouble
                if (pClosing != null && pClosing > 0) return@withContext pClosing
            }
        } catch (e: Exception) {
            Log.e("RIGOR_DEBUG", "Live price fetch failed for $insCode: ${e.message}")
        }
        return@withContext null
    }

    suspend fun analyzeStock(stockName: String): FullAnalysisResult? = withContext(Dispatchers.IO) {
        try {
            withTimeoutOrNull(30000L) { // افزایش تایم‌اوت چون ممکنه آپدیت انجام بشه
                // 1. گرفتن دیتا از مدیر کش (با پشتیبانی از همزمانی)
                val stockData = MarketDataCacheManager.getStockData(stockName) ?: return@withTimeoutOrNull null
                if (stockData.prices.isEmpty()) return@withTimeoutOrNull null

                // 2. ساخت کندل‌ها
                val candles = stockData.prices.map { price ->
                    Candle(
                        timestamp = price.dateRaw,
                        open = price.priceYesterday,
                        high = price.priceMax,
                        low = price.priceMin,
                        close = price.priceClose,
                        volume = price.volume
                    )
                }.sortedBy { it.timestamp }.filter { it.close > 0.0 }

                if (candles.isEmpty()) return@withTimeoutOrNull null

                // 3. گرفتن قیمت لحظه‌ای
                val livePrice = fetchLivePrice(stockData.insCode)
                val currentPrice = livePrice ?: candles.last().close

                // 4. استخراج P/E و EPS
                val latestValidPrice = stockData.prices.sortedBy { it.dateRaw }.lastOrNull { it.priceClose > 0.0 }
                
                val fundamental = FundamentalData(
                    estimatedEPS = latestValidPrice?.eps,
                    sectorPE = latestValidPrice?.pe,
                    sectorName = null,
                    totalShares = null,
                    weekHigh = null,
                    weekLow = null,
                    yearHigh = null,
                    yearLow = null
                )

                // 5. تحلیل
                val technicalResult = TechnicalAnalysisEngine.analyze(
                    symbol = stockName,
                    currentPrice = currentPrice,
                    candles = candles,
                    provenance = MarketDataProvenance.CACHED_EXCHANGE,
                    sourceExchange = "TSETMC"
                )

                FullAnalysisResult(
                    technical = technicalResult,
                    fundamental = fundamental,
                    smartMoneyText = null
                )
            }
        } catch (e: Exception) {
            Log.e("RIGOR_DEBUG", "Error: ${e.message}", e)
            null
        }
    }
}
