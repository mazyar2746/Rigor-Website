package com.example.data.forex

import com.example.data.model.Candle

data class ForexPair(
    val symbol: String,
    val baseCurrency: String,
    val quoteCurrency: String,
    val group: ForexPairGroup = ForexPairGroup.OTHER
)

enum class ForexPairGroup {
    MAJOR, MINOR, EXOTIC, OTHER;

    companion object {
        fun fromApiValue(value: String?): ForexPairGroup {
            return when (value?.lowercase()) {
                "major" -> MAJOR
                "minor" -> MINOR
                "exotic" -> EXOTIC
                else -> OTHER
            }
        }
    }
}

data class ForexQuote(
    val symbol: String,
    val name: String,
    val exchange: String,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val previousClose: Double,
    val change: Double,
    val percentChange: Double,
    val timestamp: Long,
    val isMarketOpen: Boolean,
    val fiftyTwoWeekLow: Double?,
    val fiftyTwoWeekHigh: Double?
)

data class ForexTimeSeries(
    val symbol: String,
    val interval: ForexInterval,
    val candles: List<Candle>,
    val currencyBase: String?,
    val currencyQuote: String?
)

enum class ForexInterval(val apiValue: String) {
    M1("1min"),
    M5("5min"),
    M15("15min"),
    M30("30min"),
    H1("1h"),
    H4("4h"),
    D1("1day"),
    W1("1week");

    companion object {
        val DEFAULT = H1
        fun fromApiValue(value: String?): ForexInterval {
            return values().firstOrNull { it.apiValue == value } ?: DEFAULT
        }
    }
}

sealed class ForexError {
    data class InvalidApiKey(val detail: String) : ForexError()
    data class SymbolNotFound(val detail: String) : ForexError()
    data class RateLimited(val detail: String) : ForexError()
    data class Parsing(val detail: String) : ForexError()
    data class Network(val detail: String) : ForexError()
    data class Provider(val detail: String) : ForexError()
    data class Http(val code: Int, val detail: String) : ForexError()
}

sealed class ForexResult<T> {
    data class Success<T>(val data: T) : ForexResult<T>()
    data class Failure<T>(val error: ForexError) : ForexResult<T>()
}
