package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.Candle
import com.example.data.model.MarketDataProvenance
import com.example.data.model.TechnicalAnalysisResult
import com.example.engine.TechnicalAnalysisEngine
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

data class FullAnalysisResult(
    val technical: TechnicalAnalysisResult,
    val fundamental: FundamentalData?,
    val smartMoneyText: String? = null
)

class IranStockAnalysisService(private val context: Context) {
    
    private val fundamentalService = IranStockFundamentalService()
    
    // کش ۶۰ ثانیه‌ای برای قیمت لحظه‌ای (جلوگیری از درخواست تکراری)
    private val livePriceCache = ConcurrentHashMap<String, Pair<Double, Long>>()
    private val LIVE_PRICE_CACHE_MS = 60 * 1000L // ۱ دقیقه

    private suspend fun fetchLivePrice(insCode: String): Double? = withContext(Dispatchers.IO) {
        // 1. چک کردن کش
        val cached = livePriceCache[insCode]
        if (cached != null && (System.currentTimeMillis() - cached.second) < LIVE_PRICE_CACHE_MS) {
            return@withContext cached.first
        }

        // 2. درخواست از API
        try {
            val url = URL("https://cdn.tsetmc.com/api/ClosingPrice/GetClosingPriceInfo/$insCode")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            if (conn.responseCode == 200) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val info = JsonParser.parseString(text).asJsonObject.getAsJsonObject("closingPriceInfo")
                var livePrice = info?.get("pDrCotVal")?.asDouble
                if (livePrice == null || livePrice <= 0) {
                    livePrice = info?.get("pClosing")?.asDouble
                }
                if (livePrice != null && livePrice > 0) {
                    livePriceCache[insCode] = Pair(livePrice, System.currentTimeMillis())
                    return@withContext livePrice
                }
            }
        } catch (e: Exception) {
            Log.e("RIGOR_DEBUG", "Live price fetch failed: ${e.message}")
        }
        return@withContext null
    }

    suspend fun analyzeStock(stockName: String): FullAnalysisResult? = withContext(Dispatchers.IO) {
        try {
            withTimeoutOrNull(30000L) {
                val stockData = MarketDataCacheManager.getStockData(stockName) ?: return@withTimeoutOrNull null
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

                return@withTimeoutOrNull coroutineScope {
                    val livePriceDeferred = async { fetchLivePrice(stockData.insCode) }
                    val fundamentalDeferred = async { fundamentalService.fetchFundamentalData(stockData.insCode) }
                    
                    val livePrice = livePriceDeferred.await()
                    val fundamentalData = fundamentalDeferred.await()

                    val currentPrice = livePrice ?: candles.last().close
                    val isLive = livePrice != null

                    val technicalResult = TechnicalAnalysisEngine.analyze(
                        symbol = stockName,
                        currentPrice = currentPrice,
                        candles = candles,
                        provenance = if (isLive) MarketDataProvenance.LIVE_EXCHANGE else MarketDataProvenance.CACHED_EXCHANGE,
                        sourceExchange = "TSETMC"
                    )

                    FullAnalysisResult(
                        technical = technicalResult,
                        fundamental = fundamentalData,
                        smartMoneyText = null
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("RIGOR_DEBUG", "Error: ${e.message}", e)
            null
        }
    }
}
