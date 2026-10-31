package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

interface TwelveDataApi {

    @GET("forex_pairs")
    suspend fun forexPairs(
        @Query("apikey") apiKey: String,
        @Query("currency_base") currencyBase: String? = null,
        @Query("currency_quote") currencyQuote: String? = null
    ): TwelveDataForexPairsResponse

    @GET("time_series")
    suspend fun timeSeries(
        @Query("apikey") apiKey: String,
        @Query("symbol") symbol: String,
        @Query("interval") interval: String,
        @Query("outputsize") outputSize: Int,
        @Query("timezone") timezone: String = "UTC"
    ): TwelveDataTimeSeriesResponse

    @GET("quote")
    suspend fun quote(
        @Query("apikey") apiKey: String,
        @Query("symbol") symbol: String
    ): TwelveDataQuoteResponse
}

@JsonClass(generateAdapter = true)
data class TwelveDataForexPairsResponse(
    val data: List<TwelveDataForexPairDto>?,
    val status: String?,
    val code: Int?,
    val message: String?
)

@JsonClass(generateAdapter = true)
data class TwelveDataForexPairDto(
    val symbol: String?,
    @Json(name = "currency_base") val currencyBase: String?,
    @Json(name = "currency_quote") val currencyQuote: String?,
    @Json(name = "currency_group") val currencyGroup: String?
)

@JsonClass(generateAdapter = true)
data class TwelveDataTimeSeriesResponse(
    val meta: TwelveDataMetaDto?,
    val values: List<TwelveDataCandleDto>?,
    val status: String?,
    val code: Int?,
    val message: String?
)

@JsonClass(generateAdapter = true)
data class TwelveDataMetaDto(
    val symbol: String?,
    val interval: String?,
    @Json(name = "currency_base") val currencyBase: String?,
    @Json(name = "currency_quote") val currencyQuote: String?
)

@JsonClass(generateAdapter = true)
data class TwelveDataCandleDto(
    val datetime: String?,
    val open: String?,
    val high: String?,
    val low: String?,
    val close: String?,
    val volume: String?
)

@JsonClass(generateAdapter = true)
data class TwelveDataQuoteResponse(
    val symbol: String?,
    val name: String?,
    val exchange: String?,
    val open: String?,
    val high: String?,
    val low: String?,
    val close: String?,
    @Json(name = "previous_close") val previousClose: String?,
    val change: String?,
    @Json(name = "percent_change") val percentChange: String?,
    val timestamp: Long?,
    @Json(name = "is_market_open") val isMarketOpen: Boolean?,
    val status: String?,
    val code: Int?,
    val message: String?
)
