package com.example.data.forex

interface ForexRepository {
    suspend fun getForexPairs(): ForexResult<List<ForexPair>>
    suspend fun getQuote(symbol: String): ForexResult<ForexQuote>
    suspend fun getTimeSeries(
        symbol: String,
        interval: ForexInterval = ForexInterval.DEFAULT
    ): ForexResult<ForexTimeSeries>
}
