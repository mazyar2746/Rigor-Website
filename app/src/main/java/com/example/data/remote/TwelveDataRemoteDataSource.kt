package com.example.data.remote

import com.example.config.RigorConfig
import com.example.data.forex.*
import com.example.data.model.Candle
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class TwelveDataRemoteDataSource(
    private val apiKey: String = RigorConfig.TWELVE_DATA_API_KEY,
    private val api: TwelveDataApi = createTwelveDataApi()
) : ForexRepository {

    private val majorSymbols = listOf(
        "EUR/USD", "GBP/USD", "USD/JPY", "AUD/USD", "USD/CAD", "USD/CHF", "NZD/USD"
    )

    override suspend fun getForexPairs(): ForexResult<List<ForexPair>> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext ForexResult.Failure(ForexError.InvalidApiKey("Missing Key"))
        try {
            val pairs = majorSymbols.map { symbol ->
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
        if (apiKey.isBlank()) return@withContext ForexResult.Failure(ForexError.InvalidApiKey("Missing Key"))
        try {
            val response = api.quote(apiKey, symbol)
            if (response.status == "error") return@withContext ForexResult.Failure(ForexError.Provider(response.message ?: "Error"))
            
            val close = response.close?.toDoubleOrNull() ?: return@withContext ForexResult.Failure(ForexError.Parsing("No close"))
            
            ForexResult.Success(ForexQuote(
                symbol = response.symbol ?: symbol,
                name = response.name ?: symbol,
                exchange = response.exchange ?: "Forex",
                open = response.open?.toDoubleOrNull() ?: close,
                high = response.high?.toDoubleOrNull() ?: close,
                low = response.low?.toDoubleOrNull() ?: close,
                close = close,
                previousClose = response.previousClose?.toDoubleOrNull() ?: close,
                change = response.change?.toDoubleOrNull() ?: 0.0,
                percentChange = response.percentChange?.toDoubleOrNull() ?: 0.0,
                timestamp = response.timestamp ?: (System.currentTimeMillis() / 1000L),
                isMarketOpen = response.isMarketOpen ?: false,
                fiftyTwoWeekLow = null,
                fiftyTwoWeekHigh = null
            ))
        } catch (e: Exception) {
            ForexResult.Failure(ForexError.Network(e.message ?: "Err"))
        }
    }

    override suspend fun getTimeSeries(symbol: String, interval: ForexInterval): ForexResult<ForexTimeSeries> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext ForexResult.Failure(ForexError.InvalidApiKey("Missing Key"))
        try {
            val response = api.timeSeries(apiKey, symbol, interval.apiValue, 100)
            if (response.status == "error") return@withContext ForexResult.Failure(ForexError.Provider(response.message ?: "Error"))
            
            val candles = response.values.orEmpty()
                .mapNotNull { it.toCandleOrNull() }
                .sortedBy { it.timestamp }
                
            ForexResult.Success(ForexTimeSeries(
                symbol = response.meta?.symbol ?: symbol,
                interval = interval,
                candles = candles,
                currencyBase = response.meta?.currencyBase,
                currencyQuote = response.meta?.currencyQuote
            ))
        } catch (e: Exception) {
            ForexResult.Failure(ForexError.Network(e.message ?: "Err"))
        }
    }

    private fun TwelveDataCandleDto.toCandleOrNull(): Candle? {
        val o = open?.toDoubleOrNull() ?: return null
        val h = high?.toDoubleOrNull() ?: return null
        val l = low?.toDoubleOrNull() ?: return null
        val c = close?.toDoubleOrNull() ?: return null
        val t = try {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(datetime ?: "")?.time
        } catch(e: Exception) {
            null
        } ?: return null
        return Candle(t, o, h, l, c, volume?.toDoubleOrNull() ?: 0.0)
    }

    companion object {
        internal fun createTwelveDataApi(): TwelveDataApi {
            val client = okhttp3.OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()
            return Retrofit.Builder().baseUrl(RigorConfig.TWELVE_DATA_BASE_URL).client(client).addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().add(KotlinJsonAdapterFactory()).build())).build().create(TwelveDataApi::class.java)
        }
    }
}
