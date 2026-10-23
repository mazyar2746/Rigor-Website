package com.example.data.market

import com.example.data.model.CandleResult
import com.example.data.model.CryptoCoin

/**
 * Supported financial market categories.
 * Designed for strategic multi-market expansion into Forex, Commodities, and DEX.
 */
enum class MarketCategory(val displayName: String) {
    CRYPTO("Cryptocurrency"),
    FOREX("Foreign Exchange"),
    COMMODITIES("Commodities & Precious Metals"),
    DEX("Decentralized Exchange")
}

/**
 * Base abstraction for financial market data providers.
 * Decouples market data ingestion from signal generation, allowing future
 * Forex and Commodity data sources to plug into the Rigor signal engine seamlessly.
 */
interface MarketDataSource {
    val category: MarketCategory

    /**
     * Retrieves primary market listings/tickers.
     */
    suspend fun fetchTickers(): List<CryptoCoin>?

    /**
     * Retrieves candlestick/OHLCV series with explicit data provenance.
     */
    suspend fun fetchCandles(
        symbol: String,
        interval: String = "1h",
        limit: Int = 40,
        basePrice: Double = 0.0
    ): CandleResult
}

/**
 * Extensibility contract for upcoming Forex data feeds (EUR/USD, GBP/USD, USD/JPY, etc.).
 */
interface ForexMarketDataSource : MarketDataSource {
    override val category: MarketCategory
        get() = MarketCategory.FOREX
}

/**
 * Extensibility contract for upcoming Commodities data feeds (Gold XAU, Silver XAG, Crude Oil WTI).
 */
interface CommoditiesMarketDataSource : MarketDataSource {
    override val category: MarketCategory
        get() = MarketCategory.COMMODITIES
}

/**
 * Extensibility contract for future Decentralized Exchange (DEX) liquidity and swap integration.
 */
interface DexMarketDataSource : MarketDataSource {
    override val category: MarketCategory
        get() = MarketCategory.DEX
}
