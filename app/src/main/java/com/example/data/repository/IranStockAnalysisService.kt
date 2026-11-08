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
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

data class FullAnalysisResult(
    val technical: TechnicalAnalysisResult,
    val fundamental: FundamentalData?,
    val smartMoneyText: String? = null,
    val lastUpdated: Long = 0L // زمان آخرین آپدیت کش
)

class IranStockAnalysisService(private val context: Context) {
    private val fundamentalService = IranStockFundamentalService()
    private val livePriceCache = ConcurrentHashMap<String, Pair<Double, Long>>()
    private val LIVE_PRICE_CACHE_MS = 60 * 1000L

    private suspend fun fetchLivePrice(insCode: String): Double? = withContext(Dispatchers.IO) {
        val cached = livePriceCache[insCode]
        if (cached != null && (System.currentTimeMillis() - cached.second) < LIVE_PRICE_CACHE_MS) return@withContext cached.first

        var currentDelay = 1000L
        repeat(3) { attempt ->
            try {
                val conn = URL("https://cdn.tsetmc.com/api/ClosingPrice/GetClosingPriceInfo/$insCode").openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                if (conn.responseCode == 200) {
                    val info = JsonParser.parseString(conn.inputStream.bufferedReader().use { it.readText() }).asJsonObject.getAsJsonObject("closingPriceInfo")
                    var p = info?.get("pDrCotVal")?.asDouble
                    if (p == null || p <= 0) p = info?.get("pClosing")?.asDouble
                    if (p != null && p > 0) {
                        livePriceCache[insCode] = Pair(p, System.currentTimeMillis())
                        return@withContext p
                    }
                }
            } catch (e: Exception) { Log.e("RIGOR_DEBUG", "Live fetch error: ${e.message}") }
            if (attempt < 2) { delay(currentDelay); currentDelay *= 2 }
        }
        return@withContext null
    }

    suspend fun analyzeStock(stockName: String): FullAnalysisResult? = withContext(Dispatchers.IO) {
        try {
            withTimeoutOrNull(35000L) {
                val stockData = MarketDataCacheManager.getStockData(stockName) ?: return@withTimeoutOrNull null
                if (stockData.prices.isEmpty()) return@withTimeoutOrNull null

                val candles = stockData.prices.map { Candle(it.dateRaw, it.priceYesterday, it.priceMax, it.priceMin, it.priceClose, it.volume) }
                    .sortedBy { it.timestamp }.filter { it.close > 0.0 }

                if (candles.isEmpty()) return@withTimeoutOrNull null

                return@withTimeoutOrNull coroutineScope {
                    val livePriceDeferred = async { fetchLivePrice(stockData.insCode) }
                    val fundamentalDeferred = async { fundamentalService.fetchFundamentalData(stockData.insCode) }
                    
                    val livePrice = livePriceDeferred.await()
                    val fundamentalData = fundamentalDeferred.await()

                    val currentPrice = livePrice ?: candles.last().close
                    val isLive = livePrice != null

                    val technicalResult = TechnicalAnalysisEngine.analyze(
                        symbol = stockName, currentPrice = currentPrice, candles = candles,
                        provenance = if (isLive) MarketDataProvenance.LIVE_EXCHANGE else MarketDataProvenance.CACHED_EXCHANGE,
                        sourceExchange = "TSETMC"
                    )

                    FullAnalysisResult(technicalResult, fundamentalData, null, stockData.lastUpdated)
                }
            }
        } catch (e: Exception) {
            Log.e("RIGOR_DEBUG", "Error: ${e.message}", e)
            null
        }
    }
}
