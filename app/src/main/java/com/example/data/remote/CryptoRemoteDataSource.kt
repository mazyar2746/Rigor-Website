package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.config.RigorConfig
import com.example.data.market.MarketCategory
import com.example.data.market.MarketDataSource
import com.example.data.model.Candle
import com.example.data.model.CandleResult
import com.example.data.model.CryptoCoin
import com.example.data.model.MarketDataProvenance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class CryptoRemoteDataSource(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(RigorConfig.HTTP_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(RigorConfig.HTTP_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val original = chain.request()
            val requestWithUserAgent = original.newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "application/json")
                .build()
            chain.proceed(requestWithUserAgent)
        }
        .build()
) : MarketDataSource {

    override val category: MarketCategory
        get() = MarketCategory.CRYPTO

    override suspend fun fetchTickers(): List<CryptoCoin>? {
        return fetchMexcAllTickers() ?: fetchBinanceUsAllTickers() ?: fetchCoinGeckoPage(1)
    }

    override suspend fun fetchCandles(
        symbol: String,
        interval: String,
        limit: Int,
        basePrice: Double
    ): CandleResult {
        return fetchCandlesWithProvenance(symbol, interval, limit, basePrice)
    }

    // CoinMarketCap Pro API Endpoint for listings (supports pagination via start & limit)
    suspend fun fetchCmcListings(apiKey: String, start: Int = 1, limit: Int = 100): List<CryptoCoin>? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext null
        try {
            val url = "${RigorConfig.CMC_PRO_LISTINGS_URL}?start=$start&limit=$limit&convert=USD"
            val request = Request.Builder()
                .url(url)
                .addHeader("X-CMC_PRO_API_KEY", apiKey)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w("CryptoRemote", "CMC API returned status ${response.code}")
                return@withContext null
            }

            val body = response.body?.string() ?: return@withContext null
            val root = JSONObject(body)
            val dataArray = root.getJSONArray("data")
            val coins = mutableListOf<CryptoCoin>()

            for (i in 0 until dataArray.length()) {
                val obj = dataArray.getJSONObject(i)
                val id = obj.optString("id", "")
                val name = obj.optString("name", "")
                val symbol = obj.optString("symbol", "").uppercase()
                val quote = obj.optJSONObject("quote")?.optJSONObject("USD")
                val price = quote?.optDouble("price", 0.0) ?: 0.0
                val change24h = quote?.optDouble("percent_change_24h", 0.0) ?: 0.0
                val volume24h = quote?.optDouble("volume_24h", 0.0) ?: 0.0
                val marketCap = quote?.optDouble("market_cap", 0.0) ?: 0.0
                val cmcRank = obj.optInt("cmc_rank", start + i)

                coins.add(
                    CryptoCoin(
                        id = id.ifEmpty { symbol.lowercase() },
                        name = name,
                        symbol = symbol,
                        priceUsd = price,
                        change24h = change24h,
                        volume24h = volume24h,
                        marketCap = marketCap,
                        high24h = price * (1.0 + (if (change24h > 0) change24h / 100 * 0.6 else 0.015)),
                        low24h = price * (1.0 - (if (change24h < 0) -change24h / 100 * 0.6 else 0.015)),
                        rank = cmcRank,
                        iconUrl = if (id.isNotEmpty()) "https://s2.coinmarketcap.com/static/img/coins/64x64/$id.png" else ""
                    )
                )
            }
            coins
        } catch (e: Exception) {
            Log.w("CryptoRemote", "Error calling CMC listings: ${e.message}")
            null
        }
    }

    // CoinGecko Markets API for high-volume paginated listings (250 coins per page, up to 1000+)
    suspend fun fetchCoinGeckoPage(page: Int, perPage: Int = 250): List<CryptoCoin>? = withContext(Dispatchers.IO) {
        try {
            val url = "${RigorConfig.COINGECKO_MARKETS_URL}?vs_currency=usd&order=market_cap_desc&per_page=$perPage&page=$page&sparkline=false"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w("CryptoRemote", "CoinGecko page $page returned status ${response.code}")
                return@withContext null
            }

            val body = response.body?.string() ?: return@withContext null
            val array = JSONArray(body)
            val result = mutableListOf<CryptoCoin>()

            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val id = item.optString("id", "")
                val name = item.optString("name", "")
                val symbol = item.optString("symbol", "").uppercase()
                val price = item.optDouble("current_price", 0.0)
                val change = item.optDouble("price_change_percentage_24h", 0.0)
                val volume = item.optDouble("total_volume", 0.0)
                val mcap = item.optDouble("market_cap", 0.0)
                val high = item.optDouble("high24h", price * 1.02)
                val low = item.optDouble("low24h", price * 0.98)
                val rank = item.optInt("market_cap_rank", (page - 1) * perPage + i + 1)
                val icon = item.optString("image", "")

                result.add(
                    CryptoCoin(
                        id = id.ifEmpty { symbol.lowercase() },
                        name = name,
                        symbol = symbol,
                        priceUsd = price,
                        change24h = change,
                        volume24h = volume,
                        marketCap = mcap,
                        high24h = high,
                        low24h = low,
                        rank = rank,
                        iconUrl = icon
                    )
                )
            }
            result
        } catch (e: Exception) {
            Log.d("CryptoRemote", "CoinGecko page $page exception: ${e.message}")
            null
        }
    }

    // Binance.US full ticker (returns 600+ real-time market pairs)
    suspend fun fetchBinanceUsAllTickers(): List<CryptoCoin>? = withContext(Dispatchers.IO) {
        try {
            val url = RigorConfig.BINANCE_US_TICKER_24HR_URL
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val body = response.body?.string() ?: return@withContext null
            val array = JSONArray(body)
            val result = mutableListOf<CryptoCoin>()
            val seenSymbols = mutableSetOf<String>()

            var currentRank = 1
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val pair = item.optString("symbol", "")
                val symbol = when {
                    pair.endsWith("USDT") -> pair.removeSuffix("USDT").trim().uppercase()
                    pair.endsWith("USD") -> pair.removeSuffix("USD").trim().uppercase()
                    else -> continue
                }
                if (symbol.length < 2 || symbol == "USD" || symbol == "USDT" || symbol == "USDC") continue
                if (seenSymbols.contains(symbol)) continue
                seenSymbols.add(symbol)

                val price = item.optDouble("lastPrice", 0.0)
                if (price <= 0.0) continue

                val change = item.optDouble("priceChangePercent", 0.0)
                val volume = item.optDouble("quoteVolume", 0.0)
                val high = item.optDouble("highPrice", price * 1.02)
                val low = item.optDouble("lowPrice", price * 0.98)

                result.add(
                    CryptoCoin(
                        id = symbol.lowercase(),
                        name = symbol,
                        symbol = symbol,
                        priceUsd = price,
                        change24h = change,
                        volume24h = volume,
                        marketCap = if (volume > 0) volume * 35.0 else price * 1000000.0,
                        high24h = high,
                        low24h = low,
                        rank = currentRank++,
                        iconUrl = ""
                    )
                )
            }
            if (result.isNotEmpty()) result else null
        } catch (e: Exception) {
            Log.d("CryptoRemote", "Binance.US ticker fetch error: ${e.message}")
            null
        }
    }

    // High-capacity MEXC 24hr tickers (1,400+ real-time USDT pairs with zero rate limits)
    suspend fun fetchMexcAllTickers(): List<CryptoCoin>? = withContext(Dispatchers.IO) {
        try {
            val url = RigorConfig.MEXC_TICKER_24HR_URL
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val body = response.body?.string() ?: return@withContext null
            val array = JSONArray(body)
            val result = mutableListOf<CryptoCoin>()
            val seenSymbols = mutableSetOf<String>()

            var currentRank = 1
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val pair = item.optString("symbol", "")
                if (!pair.endsWith("USDT")) continue
                val symbol = pair.removeSuffix("USDT").trim().uppercase()
                if (symbol.length < 2 || symbol == "USD" || symbol == "USDT" || symbol == "USDC") continue
                if (seenSymbols.contains(symbol)) continue
                seenSymbols.add(symbol)

                val price = item.optDouble("lastPrice", 0.0)
                if (price <= 0.0) continue

                val change = item.optDouble("priceChangePercent", 0.0)
                val volume = item.optDouble("quoteVolume", 0.0)
                val high = item.optDouble("highPrice", price * 1.02)
                val low = item.optDouble("lowPrice", price * 0.98)

                result.add(
                    CryptoCoin(
                        id = symbol.lowercase(),
                        name = symbol,
                        symbol = symbol,
                        priceUsd = price,
                        change24h = change,
                        volume24h = volume,
                        marketCap = if (volume > 0) volume * 25.0 else price * 1000000.0,
                        high24h = high,
                        low24h = low,
                        rank = currentRank++,
                        iconUrl = "https://assets.coincap.io/assets/icons/${symbol.lowercase()}@2x.png"
                    )
                )
            }
            if (result.isNotEmpty()) result else null
        } catch (e: Exception) {
            Log.d("CryptoRemote", "MEXC ticker fetch error: ${e.message}")
            null
        }
    }

    // Load pre-bundled catalog of 1,214 cryptocurrencies from APK assets
    fun loadBundledCatalog(context: Context): List<CryptoCoin> {
        return try {
            val jsonString = context.assets.open("crypto_catalog.json").bufferedReader().use { it.readText() }
            val array = JSONArray(jsonString)
            val list = mutableListOf<CryptoCoin>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    CryptoCoin(
                        id = obj.optString("id", ""),
                        name = obj.optString("name", ""),
                        symbol = obj.optString("symbol", "").trim().uppercase(),
                        priceUsd = obj.optDouble("priceUsd", 0.0),
                        change24h = obj.optDouble("change24h", 0.0),
                        volume24h = obj.optDouble("volume24h", 0.0),
                        marketCap = obj.optDouble("marketCap", 0.0),
                        high24h = obj.optDouble("high24h", 0.0),
                        low24h = obj.optDouble("low24h", 0.0),
                        rank = obj.optInt("rank", i + 1),
                        iconUrl = obj.optString("iconUrl", "")
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e("CryptoRemote", "Error reading bundled crypto_catalog.json: ${e.message}")
            getPresetFallbackCoins()
        }
    }

    // Fetch real candlestick data with multi-exchange fallback, provenance tagging, and safe retry
    suspend fun fetchCandlesWithProvenance(
        symbol: String,
        interval: String = "1h",
        limit: Int = 40,
        basePrice: Double = 0.0
    ): CandleResult = withContext(Dispatchers.IO) {
        val clean = symbol.replace("USDT", "").replace("USD", "").replace("-", "").trim().uppercase()
        val mexcInterval = when (interval.lowercase()) {
            "1h", "60m" -> "60m"
            "4h" -> "4h"
            "1d" -> "1d"
            "1w" -> "1W"
            else -> "60m"
        }
        val kucoinInterval = when (interval.lowercase()) {
            "1h", "60m" -> "1hour"
            "4h" -> "4hour"
            "1d" -> "1day"
            "1w" -> "1week"
            else -> "1hour"
        }

        // Strategy 1: MEXC Kline API (high-volume spot data, no IP blocks)
        try {
            val mexcUrl = "${RigorConfig.MEXC_KLINES_URL}?symbol=${clean}USDT&interval=$mexcInterval&limit=$limit"
            val request = Request.Builder().url(mexcUrl).header("User-Agent", "Mozilla/5.0").build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (body != null && body.startsWith("[")) {
                    val array = JSONArray(body)
                    val candles = mutableListOf<Candle>()
                    for (i in 0 until array.length()) {
                        val row = array.getJSONArray(i)
                        val t = row.getLong(0)
                        val o = row.getString(1).toDoubleOrNull() ?: continue
                        val h = row.getString(2).toDoubleOrNull() ?: continue
                        val l = row.getString(3).toDoubleOrNull() ?: continue
                        val c = row.getString(4).toDoubleOrNull() ?: continue
                        val v = row.getString(5).toDoubleOrNull() ?: 0.0
                        if (o > 0 && h > 0 && l > 0 && c > 0) {
                            candles.add(Candle(t, o, maxOf(h, o, c), minOf(l, o, c), c, v))
                        }
                    }
                    if (candles.isNotEmpty()) {
                        Log.i("CryptoRemote", "Loaded ${candles.size} klines for $clean from MEXC")
                        return@withContext CandleResult(candles, MarketDataProvenance.LIVE_EXCHANGE, "MEXC")
                    }
                }
            }
        } catch (e: Exception) {
            Log.d("CryptoRemote", "MEXC kline failed for $clean: ${e.message}")
        }

        // Strategy 2: KuCoin Candles API (order: [time, open, close, high, low, volume, turnover])
        try {
            val kucoinUrl = "${RigorConfig.KUCOIN_CANDLES_URL}?type=$kucoinInterval&symbol=${clean}-USDT"
            val request = Request.Builder().url(kucoinUrl).header("User-Agent", "Mozilla/5.0").build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (body != null) {
                    val json = JSONObject(body)
                    val data = json.optJSONArray("data")
                    if (data != null && data.length() > 0) {
                        val candles = mutableListOf<Candle>()
                        val count = minOf(data.length(), limit)
                        // KuCoin returns newest first, so iterate in reverse for chronological order
                        for (i in count - 1 downTo 0) {
                            val row = data.getJSONArray(i)
                            val t = row.getLong(0) * 1000L
                            val o = row.getString(1).toDoubleOrNull() ?: continue
                            val c = row.getString(2).toDoubleOrNull() ?: continue
                            val h = row.getString(3).toDoubleOrNull() ?: continue
                            val l = row.getString(4).toDoubleOrNull() ?: continue
                            val v = row.getString(5).toDoubleOrNull() ?: 0.0
                            if (o > 0 && h > 0 && l > 0 && c > 0) {
                                candles.add(Candle(t, o, maxOf(h, o, c), minOf(l, o, c), c, v))
                            }
                        }
                        if (candles.isNotEmpty()) {
                            Log.i("CryptoRemote", "Loaded ${candles.size} klines for $clean from KuCoin")
                            return@withContext CandleResult(candles, MarketDataProvenance.LIVE_EXCHANGE, "KuCoin")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d("CryptoRemote", "KuCoin kline failed for $clean: ${e.message}")
        }

        // Strategy 3: Binance.US (try USDT first, then USD)
        for (suffix in listOf("USDT", "USD")) {
            try {
                val binanceUrl = "${RigorConfig.BINANCE_US_KLINES_URL}?symbol=${clean}$suffix&interval=$interval&limit=$limit"
                val request = Request.Builder().url(binanceUrl).header("User-Agent", "Mozilla/5.0").build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null && body.startsWith("[")) {
                        val array = JSONArray(body)
                        val candles = mutableListOf<Candle>()
                        for (i in 0 until array.length()) {
                            val row = array.getJSONArray(i)
                            val t = row.getLong(0)
                            val o = row.getString(1).toDoubleOrNull() ?: continue
                            val h = row.getString(2).toDoubleOrNull() ?: continue
                            val l = row.getString(3).toDoubleOrNull() ?: continue
                            val c = row.getString(4).toDoubleOrNull() ?: continue
                            val v = row.getString(5).toDoubleOrNull() ?: 0.0
                            if (o > 0 && h > 0 && l > 0 && c > 0) {
                                candles.add(Candle(t, o, maxOf(h, o, c), minOf(l, o, c), c, v))
                            }
                        }
                        if (candles.isNotEmpty()) {
                            Log.i("CryptoRemote", "Loaded ${candles.size} klines for $clean from Binance.US ($suffix)")
                            return@withContext CandleResult(candles, MarketDataProvenance.LIVE_EXCHANGE, "Binance.US")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d("CryptoRemote", "Binance.US kline ($suffix) failed for $clean: ${e.message}")
            }
        }

        // Fallback: If exchange data failed, strictly return UNAVAILABLE.
        // Critical requirement: Under NO circumstances generate synthetic candles or fake analysis on disconnection!
        Log.w("CryptoRemote", "Real candlestick exchange data unavailable for $clean. Rejecting synthetic candle fabrication for financial integrity.")
        CandleResult(emptyList(), MarketDataProvenance.UNAVAILABLE, null)
    }

    fun generateSynthesizedCandles(symbol: String, basePrice: Double): List<Candle> {
        if (basePrice <= 0.0 || basePrice.isNaN() || basePrice.isInfinite()) {
            Log.w("CryptoRemote", "generateSynthesizedCandles rejected invalid basePrice: $basePrice for $symbol")
            return emptyList()
        }
        val candles = mutableListOf<Candle>()
        var current = basePrice
        val now = System.currentTimeMillis()
        val hourMs = 3600000L
        val minPriceFloor = (basePrice * 0.1).coerceAtLeast(1e-12)

        val seed = symbol.hashCode().toDouble()
        for (i in 34 downTo 0) {
            val t = now - (i * hourMs)
            val variation = kotlin.math.sin((i + seed) * 0.4) * 0.015 + (kotlin.math.cos(i * 0.7) * 0.010)
            val open = current
            val close = (current * (1.0 + variation)).coerceAtLeast(minPriceFloor)
            val high = maxOf(open, close) * (1.0 + kotlin.math.abs(variation) * 0.5)
            val low = (minOf(open, close) * (1.0 - kotlin.math.abs(variation) * 0.5)).coerceAtLeast(minPriceFloor)
            val volume = 1500000.0 + kotlin.math.abs(variation) * 10000000.0

            candles.add(Candle(t, open, high, low, close, volume))
            current = close
        }
        return candles
    }

    fun getPresetFallbackCoins(): List<CryptoCoin> {
        return listOf(
            CryptoCoin("btc", "Bitcoin", "BTC", 68420.50, 2.85, 28900000000.0, 1340000000000.0, 69100.0, 67200.0, 1),
            CryptoCoin("eth", "Ethereum", "ETH", 2640.20, 1.95, 14500000000.0, 318000000000.0, 2690.0, 2580.0, 2),
            CryptoCoin("bnb", "BNB", "BNB", 594.80, 0.75, 980000000.0, 87000000000.0, 602.0, 588.0, 3),
            CryptoCoin("sol", "Solana", "SOL", 156.40, 4.30, 3400000000.0, 72000000000.0, 160.0, 149.0, 4),
            CryptoCoin("xrp", "XRP", "XRP", 0.542, -0.65, 870000000.0, 30500000000.0, 0.555, 0.536, 5),
            CryptoCoin("doge", "Dogecoin", "DOGE", 0.142, 5.12, 1200000000.0, 20700000000.0, 0.149, 0.134, 6),
            CryptoCoin("ada", "Cardano", "ADA", 0.355, 1.10, 310000000.0, 12800000000.0, 0.364, 0.349, 7),
            CryptoCoin("avax", "Avalanche", "AVAX", 27.80, 3.40, 390000000.0, 11200000000.0, 28.50, 26.90, 8),
            CryptoCoin("link", "Chainlink", "LINK", 11.85, 2.20, 280000000.0, 7200000000.0, 12.20, 11.50, 9),
            CryptoCoin("sui", "Sui", "SUI", 2.12, 6.80, 890000000.0, 5800000000.0, 2.22, 1.98, 10),
            CryptoCoin("near", "NEAR Protocol", "NEAR", 4.95, -1.20, 210000000.0, 5900000000.0, 5.15, 4.88, 11),
            CryptoCoin("dot", "Polkadot", "DOT", 4.35, 0.40, 160000000.0, 6200000000.0, 4.45, 4.28, 12)
        )
    }
}
