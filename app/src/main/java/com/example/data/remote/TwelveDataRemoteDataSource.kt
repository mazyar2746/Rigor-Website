package com.example.data.remote

import com.example.data.forex.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

class TwelveDataRemoteDataSource : ForexRepository {

    private val apiKey = "DTJqY832icm5SKjSfwPtJTd27oZH70s"
    private val baseUrl = "https://api-v4.fcsapi.com"

    private val majorPairs = listOf(
        "EUR/USD", "GBP/USD", "USD/JPY", "AUD/USD", "USD/CAD", "USD/CHF", "NZD/USD"
    )

    override suspend fun getForexPairs(): ForexResult<List<ForexPair>> = withContext(Dispatchers.IO) {
        try {
            val pairs = majorPairs.map { symbol ->
                ForexPair(
                    symbol = symbol,
                    baseCurrency = symbol.substringBefore("/"),
                    quoteCurrency = symbol.substringAfter("/"),
                    group = ForexPairGroup.MAJOR
                )
            }
            ForexResult.Success(pairs)
        } catch (e: Exception) {
            ForexResult.Failure(ForexError.Network(e.message ?: "Err"))
        }
    }

    override suspend fun getQuote(symbol: String): ForexResult<ForexQuote> = withContext(Dispatchers.IO) {
        try {
            val cleanSymbol = symbol.replace("/", "")
            val url = "$baseUrl/forex/latest?symbol=$cleanSymbol&access_key=$apiKey"
            val response = URL(url).readText()
            val json = JSONObject(response)
            
            if (json.getBoolean("status") == false) {
                return@withContext ForexResult.Failure(ForexError.Provider(json.optString("msg", "Unknown error")))
            }
            
            val data = json.getJSONArray("response").getJSONObject(0)
            val active = data.getJSONObject("active")
            
            val close = active.getDouble("c")
            
            ForexResult.Success(ForexQuote(
                symbol = symbol,
                name = data.optString("ticker", symbol),
                exchange = "FCS API",
                open = active.optDouble("o", close),
                high = active.optDouble("h", close),
                low = active.optDouble("l", close),
                close = close,
                previousClose = close,
                change = active.optDouble("ch", 0.0),
                percentChange = active.optDouble("chp", 0.0),
                timestamp = System.currentTimeMillis() / 1000L,
                isMarketOpen = true,
                fiftyTwoWeekLow = null,
                fiftyTwoWeekHigh = null
            ))
        } catch (e: Exception) {
            ForexResult.Failure(ForexError.Network(e.message ?: "Err"))
        }
    }

    override suspend fun getTimeSeries(symbol: String, interval: ForexInterval): ForexResult<ForexTimeSeries> = withContext(Dispatchers.IO) {
        try {
            val cleanSymbol = symbol.replace("/", "")
            val period = when (interval) {
                ForexInterval.M1 -> "1m"
                ForexInterval.M5 -> "5m"
                ForexInterval.M15 -> "15m"
                ForexInterval.M30 -> "30m"
                ForexInterval.H1 -> "1h"
                ForexInterval.H4 -> "4h"
                ForexInterval.D1 -> "1d"
                ForexInterval.W1 -> "1w"
            }
            
            val url = "$baseUrl/forex/history?symbol=$cleanSymbol&period=$period&access_key=$apiKey"
            val response = URL(url).readText()
            val json = JSONObject(response)
            
            if (json.getBoolean("status") == false) {
                return@withContext ForexResult.Failure(ForexError.Provider(json.optString("msg", "Unknown error")))
            }
            
            val rates = json.getJSONObject("response")
            val candles = mutableListOf<com.example.data.model.Candle>()
            
            val keys = rates.keys()
            while (keys.hasNext()) {
                val timestamp = keys.next()
                val candleData = rates.getJSONObject(timestamp)
                
                val o = candleData.optDouble("o", 0.0)
                val h = candleData.optDouble("h", 0.0)
                val l = candleData.optDouble("l", 0.0)
                val c = candleData.optDouble("c", 0.0)
                val v = candleData.optDouble("v", 0.0)
                
                if (o > 0.0 && c > 0.0) {
                    candles.add(com.example.data.model.Candle(
                        timestamp = timestamp.toLong() * 1000L,
                        open = o,
                        high = h,
                        low = l,
                        close = c,
                        volume = v
                    ))
                }
            }
            
            val sortedCandles = candles.sortedBy { it.timestamp }
            
            ForexResult.Success(ForexTimeSeries(
                symbol = symbol,
                interval = interval,
                candles = sortedCandles,
                currencyBase = symbol.substringBefore("/"),
                currencyQuote = symbol.substringAfter("/")
            ))
        } catch (e: Exception) {
            ForexResult.Failure(ForexError.Network(e.message ?: "Err"))
        }
    }
}
