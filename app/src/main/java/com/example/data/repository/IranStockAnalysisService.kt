package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.Candle
import com.example.data.model.MarketDataProvenance
import com.example.data.model.StockMarketData
import com.example.data.model.TechnicalAnalysisResult
import com.example.engine.TechnicalAnalysisEngine
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

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

                val currentPrice = candles.last().close

                // استخراج P/E و EPS از آخرین کندل معتبر
                val latestValidPrice = stockData.prices.sortedBy { it.dateRaw }.lastOrNull { it.priceClose > 0.0 }
                
                // پر کردن هر ۸ فیلد کلاس FundamentalData
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
