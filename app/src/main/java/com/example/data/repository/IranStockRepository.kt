package com.example.data.repository

import android.util.Log
import com.example.data.model.DailyPrice
import com.example.data.model.StockInfo
import com.example.data.model.StockMarketData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

class IranStockRepository {
    private val gson = Gson()
    private val TAG = "IranStockRepo"

    // استفاده از jsDelivr برای دور زدن کش و تحریم
    private val symbolsUrl = "https://cdn.jsdelivr.net/gh/mazyar2746/Rigor@main/symbols_list.json"
    
    // آدرس دیتای قیمتی (فعلاً از Gist استفاده می‌کنیم)
    private val marketDataUrl = "https://gist.githubusercontent.com/mazyar2746/431540372b2fea6e9cd00cf3269b8633/raw/46ac3faa5657b71c1ce0c5c578e3dc960bfdb617/market_data.json"

    suspend fun fetchSymbolsList(): List<StockInfo> = withContext(Dispatchers.IO) {
        try {
            val jsonString = URL(symbolsUrl).readText()
            val listType = object : TypeToken<List<StockInfo>>() {}.type
            val result: List<StockInfo> = gson.fromJson(jsonString, listType) ?: emptyList()
            Log.d(TAG, "Loaded ${result.size} symbols from jsDelivr")
            return@withContext result
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}", e)
            return@withContext emptyList()
        }
    }

    suspend fun fetchTopStockData(stockName: String): List<DailyPrice> = withContext(Dispatchers.IO) {
        try {
            val jsonString = URL(marketDataUrl).readText()
            val mapType = object : TypeToken<Map<String, StockMarketData>>() {}.type
            val dataMap: Map<String, StockMarketData>? = gson.fromJson(jsonString, mapType)
            return@withContext dataMap?.get(stockName)?.prices ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}", e)
            return@withContext emptyList()
        }
    }
}
