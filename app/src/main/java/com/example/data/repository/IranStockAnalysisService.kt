package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.Candle
import com.example.data.model.MarketDataProvenance
import com.example.data.model.StockMarketData
import com.example.data.model.TechnicalAnalysisResult
import com.example.engine.TechnicalAnalysisEngine
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
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
    private val gson = Gson()

    private fun normalizeSymbol(input: String): String {
        return input
            .replace('ي', 'ی')
            .replace('ك', 'ک')
            .replace('\u200c', ' ')
            .replace(Regex("\\s+"), "")
            .trim()
    }

    // تابع جدید برای گرفتن قیمت لحظه‌ای از TSETMC
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
                
                // اول قیمت آخرین معامله (pDrCotVal) رو چک می‌کنیم، اگه نبود قیمت پایانی (pClosing)
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
            withTimeoutOrNull(25000L) {
                val jsonString = context.assets.open("market_data.json").bufferedReader().use { it.readText() }
                val mapType = object : TypeToken<Map<String, StockMarketData>>() {}.type
                val rawDataMap: Map<String, StockMarketData>? = gson.fromJson(jsonString, mapType)

                val dataMap = rawDataMap?.entries?.associate { normalizeSymbol(it.key) to it.value }
                val stockData = dataMap?.get(normalizeSymbol(stockName)) ?: return@withTimeoutOrNull null
                if (stockData.prices.isEmpty()) return@withTimeoutOrNull null

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

                // قیمت از API گرفته می‌شه، اگه اینترنت قطع بود از فایل می‌خونه
                val livePrice = fetchLivePrice(stockData.insCode)
                val currentPrice = livePrice ?: candles.last().close

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

                val technicalResult = TechnicalAnalysisEngine.analyze(
                    symbol = stockName,
                    currentPrice = currentPrice, // حالا از قیمت زنده استفاده می‌شه
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
