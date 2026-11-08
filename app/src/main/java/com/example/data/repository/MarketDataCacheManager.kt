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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap

object MarketDataCacheManager {
    private const val TAG = "CacheManager"
    private const val CACHE_EXPIRY_MS = 12 * 60 * 60 * 1000L // ۱۲ ساعت
    
    private val gson = Gson()
    private val fileMutex = Mutex()
    
    // نقشه‌ی درخواست‌های در حال انجام (جلوگیری از درخواست تکراری به API)
    private val activeRequests = ConcurrentHashMap<String, CompletableDeferred<StockMarketData?>>()
    
    private lateinit var contextRef: Context
    private var globalDataMap: MutableMap<String, StockMarketData>? = null

    fun init(context: Context) {
        contextRef = context.applicationContext
        // فقط بار اول فایل ۱۲ مگی رو توی حافظه لود می‌کنیم
        if (globalDataMap == null) {
            try {
                val jsonString = contextRef.assets.open("market_data.json").bufferedReader().use { it.readText() }
                val mapType = object : TypeToken<Map<String, StockMarketData>>() {}.type
                val parsed: Map<String, StockMarketData>? = gson.fromJson(jsonString, mapType)
                globalDataMap = parsed?.toMutableMap() ?: mutableMapOf()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load assets market_data.json", e)
                globalDataMap = mutableMapOf()
            }
        }
    }

    private fun normalizeSymbol(input: String): String {
        return input.replace('ي', 'ی').replace('ك', 'ک').replace('\u200c', ' ').replace(Regex("\\s+"), "").trim()
    }

    // بررسی اینکه آیا بازار بورس باز است یا خیر
    private fun isMarketOpen(): Boolean {
        val tehranTime = TimeZone.getTimeZone("Asia/Tehran")
        val now = Calendar.getInstance(tehranTime)
        val dayOfWeek = now.get(Calendar.DAY_OF_WEEK)
        
        // ۱ = یکشنبه، ۷ = شنبه. جمعه (۶) بازار تعطیله.
        if (dayOfWeek == Calendar.FRIDAY) return false 
        
        val hour = now.get(Calendar.HOUR_OF_DAY)
        val minute = now.get(Calendar.MINUTE)
        val timeInMinutes = hour * 60 + minute

        // بازار از ۹:۰۰ تا ۱۲:۳۰ بازه (با کمی حاشیه اطمینان)
        val marketOpen = 9 * 60
        val marketClose = 12 * 60 + 30

        return timeInMinutes in marketOpen..marketClose
    }

    suspend fun getStockData(symbol: String): StockMarketData? = withContext(Dispatchers.IO) {
        val normalizedSymbol = normalizeSymbol(symbol)
        val cacheFile = File(contextRef.filesDir, "cache_${normalizedSymbol.hashCode()}.json")
        
        // 1. خوندن از کشِ فایل (اگه وجود داشته باشه)
        var stockData: StockMarketData? = null
        if (cacheFile.exists()) {
            try {
                stockData = gson.fromJson(cacheFile.readText(), StockMarketData::class.java)
            } catch (e: Exception) { /* ignore corrupt cache */ }
        }
        
        // اگه کش نبود، از دیتای اولیه (assets) برمی‌داریم
        if (stockData == null) {
            stockData = globalDataMap?.entries?.find { normalizeSymbol(it.key) == normalizedSymbol }?.value
        }

        if (stockData == null) return@withContext null

        // 2. چک کردن انقضا و ساعت بازار
        val isExpired = System.currentTimeMillis() - stockData.lastUpdated > CACHE_EXPIRY_MS
        val shouldFetch = isExpired && isMarketOpen() 

        if (!shouldFetch) {
            return@withContext stockData
        }

        // 3. سیستم صف هوشمند (همزمانی)
        val existingRequest = activeRequests[normalizedSymbol]
        if (existingRequest != null) {
            return@withContext existingRequest.await()
        }

        val deferred = CompletableDeferred<StockMarketData?>()
        activeRequests[normalizedSymbol] = deferred

        try {
            val updatedPrices = fetchHistoryFromApi(stockData.insCode)
            if (updatedPrices != null && updatedPrices.isNotEmpty()) {
                val newData = StockMarketData(
                    insCode = stockData.insCode,
                    prices = updatedPrices.takeLast(30), // فقط ۳۰ روز آخر
                    lastUpdated = System.currentTimeMillis()
                )
                
                // ذخیره در فایل اختصاصی این سهم (بدون نوشتن کل ۱۲ مگ)
                fileMutex.withLock {
                    cacheFile.writeText(gson.toJson(newData))
                    // آپدیت دیتای حافظه اصلی
                    globalDataMap?.let { map ->
                        val key = map.keys.find { normalizeSymbol(it) == normalizedSymbol } ?: symbol
                        map[key] = newData
                    }
                }
                deferred.complete(newData)
                return@withContext newData
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating data for $symbol", e)
        } finally {
            activeRequests.remove(normalizedSymbol)
        }

        deferred.complete(stockData)
        return@withContext stockData
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
                    return list.sortedBy { it.dateRaw }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "API fetch error: ${e.message}")
        }
        return null
    }
}
