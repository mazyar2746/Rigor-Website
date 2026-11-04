package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.DailyPrice
import com.example.data.model.StockInfo
import com.example.data.model.StockMarketData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class IranStockRepository(private val context: Context) {
    private val gson = Gson()
    private val TAG = "IranStockRepo"

    // خواندن از فایل داخلی اپ (بدون نیاز به اینترنت)
    suspend fun fetchSymbolsList(): List<StockInfo> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.assets.open("symbols_list.json").bufferedReader().use { it.readText() }
            val listType = object : TypeToken<List<StockInfo>>() {}.type
            val result: List<StockInfo> = gson.fromJson(jsonString, listType) ?: emptyList()
            Log.d(TAG, "Loaded ${result.size} symbols from assets")
            return@withContext result
        } catch (e: Exception) {
            Log.e(TAG, "Error reading assets: ${e.message}", e)
            return@withContext emptyList()
        }
    }

    suspend fun fetchTopStockData(stockName: String): List<DailyPrice> = withContext(Dispatchers.IO) {
        try {
            // فعلاً دیتای قیمتی رو از assets نمی‌خونیم، از Gist می‌خونیم (یا بعداً اضافه می‌کنیم)
            return@withContext emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}", e)
            return@withContext emptyList()
        }
    }
}
