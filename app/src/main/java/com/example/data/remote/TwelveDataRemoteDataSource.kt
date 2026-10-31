package com.example.data.remote

import com.example.data.forex.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

class TwelveDataRemoteDataSource : ForexRepository {

    private val apiKey = "1V6FFX48KH6LXTF9"
    private val baseUrl = "https://www.alphavantage.co/query"

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
            val from = symbol.substringBefore("/")
            val to = symbol.substringAfter("/")
            val url = "$baseUrl?function=CURRENCY_EXCHANGE_RATE&from_currency=$from&to_currency=$to&apikey=$apiKey"
            
            val response = URL(url).readText()
            val json = JSONObject(response)
            
            if (!json.has("Realtime Currency Exchange Rate")) {
                return@withContext ForexResult.Failure(ForexError.Provider("Invalid response: " + response.take(100)))
            }
            
            val data = json.getJSONObject("Realtime Currency Exchange Rate")
            val rate = data.getString("5. Exchange Rate").toDouble()
            
            ForexResult.Success(ForexQuote(
                symbol = symbol,
                name = symbol,
                exchange = "Alpha Vantage",
                open = rate,
                high = rate,
                low = rate,
                close = rate,
                previousClose = rate,
                change = 0.0,
                percentChange = 0.0,
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
            val from = symbol.substringBefore("/")
            val to = symbol.substringAfter("/")
            
            val function = when (interval) {
                ForexInterval.M1, ForexInterval.M5, ForexInterval.M15, ForexInterval.M30 -> "FX_INTRADAY"
                ForexInterval.H1, ForexInterval.H4 -> "FX_INTRADAY"
                else -> "FX_DAILY"
            }
            
            val intervalParam = when (interval) {
                ForexInterval.M1 -> "&interval=1min"
                ForexInterval.M5 -> "&interval=5min"
                ForexInterval.M15 -> "&interval=15min"
                ForexInterval.M30 -> "&interval=30min"
                ForexInterval.H1 -> "&interval=60min"
                ForexInterval.H4 -> "&interval=60min"
                else -> ""
            }
            
            val url = "$baseUrl?function=$function&from_symbol=$from&to_symbol=$to&outputsize=compact&apikey=$apiKey$intervalParam"
            
            val response = URL(url).readText()
            val json = JSONObject(response)
            
            val timeSeriesKey = when (function) {
                "FX_INTRADAY" -> "Time Series FX (" + intervalParam.substringAfter("interval=") + ")"
                else -> "Time Series FX (Daily)"
            }
            
            if (!json.has(timeSeriesKey)) {
                return@withContext ForexResult.Failure(ForexError.Provider("Invalid response: " + response.take(100)))
            }
            
            val timeSeries = json.getJSONObject(timeSeriesKey)
            val candles = mutableListOf<com.example.data.model.Candle>()
            
            val keys = timeSeries.keys()
            while (keys.hasNext()) {
                val dateStr = keys.next()
                val data = timeSeries.getJSONObject(dateStr)
                
                val o = data.optString("1. open", "0").toDoubleOrNull() ?: 0.0
                val h = data.optString("2. high", "0").toDoubleOrNull() ?: 0.0
                val l = data.optString("3. low", "0").toDoubleOrNull() ?: 0.0
                val c = data.optString("4. close", "0").toDoubleOrNull() ?: 0.0
                
                val timestamp = try {
                    java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).parse(dateStr)?.time
                        ?: java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(dateStr)?.time
                        ?: 0L
                } catch (e: Exception) {
                    0L
                }
                
                if (o > 0.0 && c > 0.0) {
                    candles.add(com.example.data.model.Candle(
                        timestamp = timestamp,
                        open = o,
                        high = h,
                        low = l,
                        close = c,
                        volume = 0.0
                    ))
                }
            }
            
            val sortedCandles = candles.sortedBy { it.timestamp }
            
            ForexResult.Success(ForexTimeSeries(
                symbol = symbol,
                interval = interval,
                candles = sortedCandles,
                currencyBase = from,
                currencyQuote = to
            ))
        } catch (e: Exception) {
            ForexResult.Failure(ForexError.Network(e.message ?: "Err"))
        }
    }
}
