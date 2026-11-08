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
    val smartMoneyText: String?
)

class IranStockAnalysisService(private val context: Context) {
    private val gson = Gson()

    suspend fun analyzeStock(stockName: String): FullAnalysisResult? = withContext(Dispatchers.IO) {
        try {
            withTimeoutOrNull(25000L) {
                val jsonString = context.assets.open("market_data.json").bufferedReader().use { it.readText() }
                val mapType = object : TypeToken<Map<String, StockMarketData>>() {}.type
                val dataMap: Map<String, StockMarketData>? = gson.fromJson(jsonString, mapType)
                
                val stockData = dataMap?.get(stockName) ?: return@withTimeoutOrNull null
                if (stockData.prices.isEmpty()) return@withTimeoutOrNull null

                val candles = stockData.prices.map { price ->
                    Candle(
                        timestamp = price.dateRaw,
                        open = price.priceYesterday,
                        high = price.priceMax,
                        low = price.priceMin,
                        close = price.priceClose,
                        volume = price.volume.toDouble()
                    )
                }

                val currentPrice = candles.last().close
                
                // محاسبه حمایت و مقاومت
                val recent = candles.takeLast(30).map { it.close }.sorted()
                val minPrice = recent.firstOrNull() ?: currentPrice
                val maxPrice = recent.lastOrNull() ?: currentPrice
                val range = maxPrice - minPrice
                val support1 = minPrice + range * 0.25
                val resistance1 = minPrice + range * 0.75

                val technicalResult = TechnicalAnalysisEngine.analyze(
                    symbol = stockName,
                    currentPrice = currentPrice,
                    candles = candles,
                    provenance = MarketDataProvenance.CACHED_EXCHANGE,
                    sourceExchange = "TSETMC"
                )

                // برگرداندن نتیجه با حمایت/مقاومت
                FullAnalysisResult(
                    technical = technicalResult,
                    fundamental = null,
                    smartMoneyText = "حمایت نزدیک: ${support1.toInt()} | مقاومت نزدیک: ${resistance1.toInt()}"
                )
            }
        } catch (e: Exception) {
            Log.e("RIGOR_DEBUG", "Error: ${e.message}", e)
            null
        }
    }
}
