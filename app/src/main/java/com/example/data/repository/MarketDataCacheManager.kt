package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.DailyPrice
import com.example.data.model.StockMarketData
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

object MarketDataCacheManager {
    private const val TAG = "CacheManager"
    private const val CACHE_FILE_NAME = "market_data_cache.json"
    private const val CACHE_EXPIRY_MS = 12 * 60 * 60 * 1000L // ۱۲ ساعت
    
    private val gson = Gson()
    private val fileMutex = Mutex() // قفل برای جلوگیری از نوشتن همزمان
    private val ongoingFetches = ConcurrentHashMap<String, Any>() // جلوگیری از درخواست تکراری برای یک نماد
    
    private var inMemoryData: MutableMap<String, StockMarketData>? = null
    private lateinit var cacheFile: File

    fun init(context: Context) {
        cacheFile = File(context.filesDir, CACHE_FILE_NAME)
        if (!cacheFile.exists()) {
            try {
                context.assets.open("market_data.json").use { input ->
                    cacheFile.outputStream().use { output -> input.copyTo(output) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to copy initial data from assets", e)
            }
        }
    }

    private fun normalizeSymbol(input: String): String {
        return input.replace('ي', 'ی').replace('ك', 'ک').replace('\u200c', ' ').replace(Regex("\\s+"), "").trim()
    }

    suspend fun getStockData(symbol: String): StockMarketData? = withContext(Dispatchers.IO) {
        val normalizedSymbol = normalizeSymbol(symbol)
        
        // 1. لود کردن دیتا از فایل به حافظه (فقط یک بار)
        if (inMemoryData == null) {
            fileMutex.withLock {
                if (inMemoryData == null) {
                    val jsonString = if (cacheFile.exists()) cacheFile.readText() else "{}"
                    val mapType = object : TypeToken<Map<String, StockMarketData>>() {}.type
                    val parsed: Map<String, StockMarketData>? = try { gson.fromJson(jsonString, mapType) } catch (e: Exception) { null }
                    inMemoryData = parsed?.toMutableMap() ?: mutableMapOf()
                }
            }
        }

        val currentStockData = inMemoryData?.entries?.find { normalizeSymbol(it.key) == normalizedSymbol }?.value
            ?: return@withContext null

        // 2. چک کردن اینکه آیا دیتا منقضی شده یا نه
        val isExpired = System.currentTimeMillis() - currentStockData.lastUpdated > CACHE_EXPIRY_MS

        if (isExpired) {
            // اگر همین الان داره برای این نماد درخواست می‌ره، صبر می‌کنیم همون نتیجه رو بگیریم
            if (ongoingFetches.containsKey(normalizedSymbol)) {
                while (ongoingFetches.containsKey(normalizedSymbol)) {
                    Thread.sleep(200)
                }
                return@withContext inMemoryData?.entries?.find { normalizeSymbol(it.key) == normalizedSymbol }?.value
            }

            ongoingFetches[normalizedSymbol] = Any()
            try {
                val updatedPrices = fetchHistoryFromApi(currentStockData.insCode)
                if (updatedPrices != null && updatedPrices.isNotEmpty()) {
                    // محدود کردن به ۳۰ روز آخر
                    val trimmedPrices = updatedPrices.takeLast(30)
                    val newData = StockMarketData(
                        insCode = currentStockData.insCode,
                        prices = trimmedPrices,
                        lastUpdated = System.currentTimeMillis()
                    )
                    
                    // ذخیره توی حافظه و فایل با قفل
                    fileMutex.withLock {
                        inMemoryData?.let { map ->
                            val key = map.keys.find { normalizeSymbol(it) == normalizedSymbol } ?: symbol
                            map[key] = newData
                            cacheFile.writeText(gson.toJson(map))
                        }
                    }
                    return@withContext newData
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating data for $symbol", e)
            } finally {
                ongoingFetches.remove(normalizedSymbol)
            }
        }
        return@withContext currentStockData
    }

    private fun fetchHistoryFromApi(insCode: String): List<DailyPrice>? {
        try {
            val url = URL("https://cdn.tsetmc.com/api/ClosingPrice/GetClosingPriceDailyList/$insCode/35")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            if (conn.responseCode == 200) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JsonParser.parseString(text).asJsonObject
                val array = root.getAsJsonArray("closingPriceDaily")
                
                if (array != null) {
                    val list = mutableListOf<DailyPrice>()
                    for (element in array) {
                        val obj = element.asJsonObject
                        val pClose = obj.get("pClosing")?.asDouble ?: 0.0
                        // فیلتر کردن کندل‌های صفر
                        if (pClose > 0) {
                            list.add(
                                DailyPrice(
                                    priceClose = pClose,
                                    priceMin = obj.get("priceMin")?.asDouble ?: 0.0,
                                    priceMax = obj.get("priceMax")?.asDouble ?: 0.0,
                                    priceYesterday = obj.get("priceYesterday")?.asDouble ?: 0.0,
                                    volume = obj.get("qTotTran5J")?.asDouble ?: 0.0,
                                    dateRaw = obj.get("dEven")?.asLong ?: 0L,
                                    pe = obj.get("pe")?.asDouble,
                                    eps = obj.get("eps")?.asDouble
                                )
                            )
                        }
                    }
                    return list.sortedBy { it.dateRaw } // مرتب‌سازی از قدیم به جدید
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "API fetch error: ${e.message}")
        }
        return null
    }
}
