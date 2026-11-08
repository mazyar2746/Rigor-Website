package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.DailyPrice
import com.example.data.model.StockMarketData
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap

object MarketDataCacheManager {
    private const val TAG = "CacheManager"
    private const val CACHE_EXPIRY_MS = 12 * 60 * 60 * 1000L // ۱۲ ساعت
    
    private val gson = Gson()
    private val fileMutex = Mutex()
    private val activeRequests = ConcurrentHashMap<String, CompletableDeferred<StockMarketData?>>()
    
    private lateinit var contextRef: Context
    private var globalDataMap: MutableMap<String, StockMarketData>? = null

    fun init(context: Context) {
        contextRef = context.applicationContext
        if (globalDataMap == null) {
            try {
                val jsonString = contextRef.assets.open("market_data.json").bufferedReader().use { it.readText() }
                val mapType = object : TypeToken<Map<String, StockMarketData>>() {}.type
                val parsed: Map<String, StockMarketData>? = gson.fromJson(jsonString, mapType)
                globalDataMap = parsed?.toMutableMap() ?: mutableMapOf()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load assets", e)
                globalDataMap = mutableMapOf()
            }
        }
    }

    private fun normalizeSymbol(input: String): String {
        return input.replace('ي', 'ی').replace('ك', 'ک').replace('\u200c', ' ').replace(Regex("\\s+"), "").trim()
    }

    private fun isMarketOpen(): Boolean {
        val now = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tehran"))
        if (now.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) return false
        val time = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        return time in (9 * 60)..(12 * 60 + 30)
    }

    suspend fun getStockData(symbol: String): StockMarketData? = withContext(Dispatchers.IO) {
        val normalizedSymbol = normalizeSymbol(symbol)
        val cacheFile = File(contextRef.filesDir, "cache_${normalizedSymbol.hashCode()}.json")
        
        var stockData: StockMarketData? = null
        if (cacheFile.exists()) {
            try { stockData = gson.fromJson(cacheFile.readText(), StockMarketData::class.java) } catch (e: Exception) { }
        }
        if (stockData == null) {
            stockData = globalDataMap?.entries?.find { normalizeSymbol(it.key) == normalizedSymbol }?.value
        }
        if (stockData == null) return@withContext null

        val isExpired = System.currentTimeMillis() - stockData.lastUpdated > CACHE_EXPIRY_MS
        if (!isExpired || !isMarketOpen()) return@withContext stockData

        val existing = activeRequests[normalizedSymbol]
        if (existing != null) return@withContext existing.await()

        val deferred = CompletableDeferred<StockMarketData?>()
        activeRequests[normalizedSymbol] = deferred

        try {
            // استفاده از Retry با Exponential Backoff
            val updatedPrices = fetchWithRetry(stockData.insCode)
            if (updatedPrices != null && updatedPrices.isNotEmpty()) {
                val newData = StockMarketData(stockData.insCode, updatedPrices.takeLast(30), System.currentTimeMillis())
                fileMutex.withLock {
                    cacheFile.writeText(gson.toJson(newData))
                    globalDataMap?.let { map ->
                        val key = map.keys.find { normalizeSymbol(it) == normalizedSymbol } ?: symbol
                        map[key] = newData
                    }
                }
                deferred.complete(newData)
                return@withContext newData
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating $symbol", e)
        } finally {
            activeRequests.remove(normalizedSymbol)
        }

        deferred.complete(stockData)
        return@withContext stockData
    }

    // تابع جدید: تلاش مجدد با فاصله زمانی
    private suspend fun fetchWithRetry(insCode: String, maxRetries: Int = 3): List<DailyPrice>? {
        var currentDelay = 1000L
        repeat(maxRetries) { attempt ->
            val result = fetchHistoryFromApi(insCode)
            if (result != null) return result
            if (attempt < maxRetries - 1) {
                delay(currentDelay)
                currentDelay *= 2 // ۱ ثانیه، ۲ ثانیه، ۴ ثانیه
            }
        }
        return null
    }

    private fun fetchHistoryFromApi(insCode: String): List<DailyPrice>? {
        try {
            val conn = URL("https://cdn.tsetmc.com/api/ClosingPrice/GetClosingPriceDailyList/$insCode/35").openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")
            conn.connectTimeout = 8000
            conn.readTimeout = 8000

            if (conn.responseCode == 200) {
                val root = JsonParser.parseString(conn.inputStream.bufferedReader().use { it.readText() }).asJsonObject
                val array = root.getAsJsonArray("closingPriceDaily")
                if (array != null) {
                    val list = mutableListOf<DailyPrice>()
                    for (element in array) {
                        val obj = element.asJsonObject
                        val pClose = obj.get("pClosing")?.asDouble ?: 0.0
                        if (pClose > 0) {
                            list.add(DailyPrice(
                                priceClose = pClose, priceMin = obj.get("priceMin")?.asDouble ?: 0.0,
                                priceMax = obj.get("priceMax")?.asDouble ?: 0.0, priceYesterday = obj.get("priceYesterday")?.asDouble ?: 0.0,
                                volume = obj.get("qTotTran5J")?.asDouble ?: 0.0, dateRaw = obj.get("dEven")?.asLong ?: 0L,
                                pe = obj.get("pe")?.asDouble, eps = obj.get("eps")?.asDouble
                            ))
                        }
                    }
                    return list.sortedBy { it.dateRaw }
                }
            }
        } catch (e: Exception) { Log.e(TAG, "API error: ${e.message}") }
        return null
    }
}
