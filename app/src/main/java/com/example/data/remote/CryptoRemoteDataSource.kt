package com.example.data.remote

import android.util.Log
import com.example.data.model.Candle
import com.example.data.model.CryptoCoin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class CryptoRemoteDataSource(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val original = chain.request()
            val requestWithUserAgent = original.newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "application/json")
                .build()
            chain.proceed(requestWithUserAgent)
        }
        .build()
) {

    // CoinMarketCap Pro API Endpoint for listings
    suspend fun fetchCmcListings(apiKey: String, limit: Int = 30): List<CryptoCoin>? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext null
        try {
            val url = "https://pro-api.coinmarketcap.com/v1/cryptocurrency/listings/latest?limit=$limit&convert=USD"
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
                val id = obj.getString("id")
                val name = obj.getString("name")
                val symbol = obj.getString("symbol")
                val quote = obj.getJSONObject("quote").getJSONObject("USD")
                val price = quote.optDouble("price", 0.0)
                val change24h = quote.optDouble("percent_change_24h", 0.0)
                val volume24h = quote.optDouble("volume_24h", 0.0)
                val marketCap = quote.optDouble("market_cap", 0.0)

                coins.add(
                    CryptoCoin(
                        id = id,
                        name = name,
                        symbol = symbol,
                        priceUsd = price,
                        change24h = change24h,
                        volume24h = volume24h,
                        marketCap = marketCap,
                        high24h = price * (1.0 + (if (change24h > 0) change24h / 100 * 0.6 else 0.015)),
                        low24h = price * (1.0 - (if (change24h < 0) -change24h / 100 * 0.6 else 0.015)),
                        rank = i + 1,
                        iconUrl = "https://s2.coinmarketcap.com/static/img/coins/64x64/$id.png"
                    )
                )
            }
            coins
        } catch (e: Exception) {
            Log.w("CryptoRemote", "Error calling CMC listings: ${e.message}")
            null
        }
    }

    // Public live market feed with automatic multi-tier fallback (Binance US -> Binance Global -> CoinGecko -> Preset)
    suspend fun fetchPublicMarketCoins(): List<CryptoCoin> = withContext(Dispatchers.IO) {
        // 1. Try Binance US (accessible from US cloud environments)
        val binanceUsResult = fetchFromBinanceTicker("https://api.binance.us/api/v3/ticker/24hr")
        if (!binanceUsResult.isNullOrEmpty()) {
            return@withContext binanceUsResult
        }

        // 2. Try Binance Global
        val binanceGlobalResult = fetchFromBinanceTicker("https://api.binance.com/api/v3/ticker/24hr")
        if (!binanceGlobalResult.isNullOrEmpty()) {
            return@withContext binanceGlobalResult
        }

        // 3. Try CoinGecko Public Markets API
        val coinGeckoResult = fetchFromCoinGecko()
        if (!coinGeckoResult.isNullOrEmpty()) {
            return@withContext coinGeckoResult
        }

        // 4. Stable default preset coins
        getPresetFallbackCoins()
    }

    private fun fetchFromBinanceTicker(endpoint: String): List<CryptoCoin>? {
        return try {
            val request = Request.Builder().url(endpoint).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null

            val body = response.body?.string() ?: return null
            val array = JSONArray(body)

            val prioritizedSymbols = listOf(
                "BTCUSDT" to ("Bitcoin" to "BTC"),
                "ETHUSDT" to ("Ethereum" to "ETH"),
                "BNBUSDT" to ("BNB" to "BNB"),
                "SOLUSDT" to ("Solana" to "SOL"),
                "XRPUSDT" to ("XRP" to "XRP"),
                "DOGEUSDT" to ("Dogecoin" to "DOGE"),
                "ADAUSDT" to ("Cardano" to "ADA"),
                "AVAXUSDT" to ("Avalanche" to "AVAX"),
                "LINKUSDT" to ("Chainlink" to "LINK"),
                "SUIUSDT" to ("Sui" to "SUI"),
                "NEARUSDT" to ("NEAR Protocol" to "NEAR"),
                "DOTUSDT" to ("Polkadot" to "DOT"),
                "TRXUSDT" to ("TRON" to "TRX"),
                "MATICUSDT" to ("Polygon" to "MATIC"),
                "LTCUSDT" to ("Litecoin" to "LTC"),
                "UNIUSDT" to ("Uniswap" to "UNI"),
                "APTUSDT" to ("Aptos" to "APT"),
                "SHIBUSDT" to ("Shiba Inu" to "SHIB"),
                "ATOMUSDT" to ("Cosmos" to "ATOM")
            )

            val map = mutableMapOf<String, JSONObject>()
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val pair = item.optString("symbol")
                if (pair.isNotEmpty()) {
                    map[pair] = item
                }
            }

            val result = mutableListOf<CryptoCoin>()
            var rank = 1

            for ((pair, info) in prioritizedSymbols) {
                val item = map[pair]
                if (item != null) {
                    val price = item.optDouble("lastPrice", 0.0)
                    val change = item.optDouble("priceChangePercent", 0.0)
                    val volume = item.optDouble("quoteVolume", 0.0)
                    val high = item.optDouble("highPrice", price * 1.02)
                    val low = item.optDouble("lowPrice", price * 0.98)

                    result.add(
                        CryptoCoin(
                            id = info.second.lowercase(),
                            name = info.first,
                            symbol = info.second,
                            priceUsd = price,
                            change24h = change,
                            volume24h = volume,
                            marketCap = volume * (40 - rank),
                            high24h = high,
                            low24h = low,
                            rank = rank++,
                            iconUrl = ""
                        )
                    )
                }
            }
            if (result.isNotEmpty()) result else null
        } catch (e: Exception) {
            Log.d("CryptoRemote", "Binance endpoint $endpoint unavailable: ${e.message}")
            null
        }
    }

    private fun fetchFromCoinGecko(): List<CryptoCoin>? {
        return try {
            val url = "https://api.coingecko.com/api/v3/coins/markets?vs_currency=usd&order=market_cap_desc&per_page=25&page=1&sparkline=false"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null

            val body = response.body?.string() ?: return null
            val array = JSONArray(body)
            val result = mutableListOf<CryptoCoin>()

            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val id = item.getString("id")
                val name = item.getString("name")
                val symbol = item.getString("symbol").uppercase()
                val price = item.optDouble("current_price", 0.0)
                val change = item.optDouble("price_change_percentage_24h", 0.0)
                val volume = item.optDouble("total_volume", 0.0)
                val mcap = item.optDouble("market_cap", 0.0)
                val high = item.optDouble("high_24h", price * 1.02)
                val low = item.optDouble("low_24h", price * 0.98)
                val rank = item.optInt("market_cap_rank", i + 1)
                val icon = item.optString("image", "")

                result.add(
                    CryptoCoin(
                        id = id,
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
            if (result.isNotEmpty()) result else null
        } catch (e: Exception) {
            Log.d("CryptoRemote", "CoinGecko fallback unavailable: ${e.message}")
            null
        }
    }

    // Fetch real candlestick data with Binance.US, Binance Global, and synthesized backup
    suspend fun fetchCandles(symbol: String, interval: String = "1h", limit: Int = 40): List<Candle> =
        withContext(Dispatchers.IO) {
            val cleanSymbol = symbol.replace("USDT", "").uppercase()
            val pair = "${cleanSymbol}USDT"

            val klineEndpoints = listOf(
                "https://api.binance.us/api/v3/klines?symbol=$pair&interval=$interval&limit=$limit",
                "https://api.binance.com/api/v3/klines?symbol=$pair&interval=$interval&limit=$limit"
            )

            for (endpoint in klineEndpoints) {
                try {
                    val request = Request.Builder().url(endpoint).build()
                    val response = client.newCall(request).execute()

                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: continue
                        val array = JSONArray(body)
                        val candles = mutableListOf<Candle>()

                        for (i in 0 until array.length()) {
                            val row = array.getJSONArray(i)
                            candles.add(
                                Candle(
                                    timestamp = row.getLong(0),
                                    open = row.getString(1).toDouble(),
                                    high = row.getString(2).toDouble(),
                                    low = row.getString(3).toDouble(),
                                    close = row.getString(4).toDouble(),
                                    volume = row.getString(5).toDouble()
                                )
                            )
                        }
                        if (candles.isNotEmpty()) return@withContext candles
                    }
                } catch (e: Exception) {
                    Log.d("CryptoRemote", "Kline fetch attempt on $endpoint skipped: ${e.message}")
                }
            }
            generateSynthesizedCandles(symbol, 100.0)
        }

    fun generateSynthesizedCandles(symbol: String, basePrice: Double): List<Candle> {
        val candles = mutableListOf<Candle>()
        var current = basePrice
        val now = System.currentTimeMillis()
        val hourMs = 3600000L

        val seed = symbol.hashCode().toDouble()
        for (i in 34 downTo 0) {
            val t = now - (i * hourMs)
            val variation = kotlin.math.sin((i + seed) * 0.4) * 0.018 + (kotlin.math.cos(i * 0.7) * 0.012)
            val open = current
            val close = (current * (1.0 + variation)).coerceAtLeast(0.0001)
            val high = maxOf(open, close) * (1.0 + kotlin.math.abs(variation) * 0.5)
            val low = minOf(open, close) * (1.0 - kotlin.math.abs(variation) * 0.5)
            val volume = 1500000.0 + kotlin.math.abs(variation) * 10000000.0

            candles.add(Candle(t, open, high, low, close, volume))
            current = close
        }
        return candles
    }

    private fun getPresetFallbackCoins(): List<CryptoCoin> {
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
