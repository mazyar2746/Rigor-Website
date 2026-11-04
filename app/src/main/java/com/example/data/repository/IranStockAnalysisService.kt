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

data class FullAnalysisResult(
    val technical: TechnicalAnalysisResult,
    val fundamental: FundamentalData?,
    val smartMoneyText: String?
)

class IranStockAnalysisService(private val context: Context) {
    private val gson = Gson()
    private val fundamentalService = IranStockFundamentalService()
    private val smartMoneyService = IranStockSmartMoneyService()

    suspend fun analyzeStock(stockName: String): FullAnalysisResult? = withContext(Dispatchers.IO) {
        try {
            // ۱. خواندن دیتای قیمتی از assets
            val jsonString = context.assets.open("market_data.json").bufferedReader().use { it.readText() }
            val mapType = object : TypeToken<Map<String, StockMarketData>>() {}.type
            val dataMap: Map<String, StockMarketData>? = gson.fromJson(jsonString, mapType)

            // ۲. چک کردن وجود دیتای قیمتی برای این نماد
            val stockData = dataMap?.get(stockName)
            if (stockData == null || stockData.prices.isEmpty()) {
                Log.w("AnalysisService", "No price data for $stockName")
                return@withContext null
            }

            // ۳. تبدیل به Candle
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

            // ۴. تحلیل تکنیکال
            val currentPrice = candles.last().close
            val technicalResult = TechnicalAnalysisEngine.analyze(
                symbol = stockName,
                currentPrice = currentPrice,
                candles = candles,
                provenance = MarketDataProvenance.LIVE_EXCHANGE,
                sourceExchange = "TSETMC"
            )

            // ۵. دریافت اطلاعات بنیادی و پول هوشمند (با try-catch جداگانه)
            val fundamental = try {
                fundamentalService.fetchFundamentalData(stockData.insCode)
            } catch (e: Exception) {
                Log.e("AnalysisService", "Fundamental error: ${e.message}")
                null
            }

            val smartMoneyText = try {
                val smartMoneyList = smartMoneyService.fetchSmartMoneyData(stockData.insCode)
                smartMoneyService.analyzeSmartMoney(smartMoneyList)
            } catch (e: Exception) {
                Log.e("AnalysisService", "SmartMoney error: ${e.message}")
                null
            }

            return@withContext FullAnalysisResult(technicalResult, fundamental, smartMoneyText)
        } catch (e: Exception) {
            Log.e("AnalysisService", "Error analyzing $stockName: ${e.message}", e)
            return@withContext null
        }
    }
}
