package com.example.config

/**
 * Rigor Central Configuration.
 * Centralizes all external API endpoints, official contacts, risk management parameters,
 * and technical calculation boundaries to support modular multi-asset expansion.
 */
object RigorConfig {
    const val TWELVE_DATA_API_KEY = "YOUR_API_KEY_HERE"
    const val TWELVE_DATA_BASE_URL = "https://api.twelvedata.com/"

    // App & Identity
    const val APP_NAME = "Rigor"
    const val APP_VERSION = "1.0.0"
    const val OFFICIAL_SUPPORT_EMAIL = "h.nasiri3247@gmail.com"

    // Subscription & On-Chain Settlement
    const val USDT_RECEIVER_ADDRESS = "0x71C8A9b7B73F507F3b30C8009Eb423C47137f722"
    const val SUBSCRIPTION_CHAIN_NAME = "BNB Smart Chain (BEP20)"
    const val MONTHLY_PRICE_USDT = 9.99
    const val YEARLY_PRICE_USDT = 85.0

    // External API Endpoints
    const val CMC_PRO_LISTINGS_URL = "https://pro-api.coinmarketcap.com/v1/cryptocurrency/listings/latest"
    const val COINGECKO_MARKETS_URL = "https://api.coingecko.com/api/v3/coins/markets"
    const val BINANCE_US_TICKER_24HR_URL = "https://api.binance.us/api/v3/ticker/24hr"
    const val BINANCE_US_KLINES_URL = "https://api.binance.us/api/v3/klines"
    const val MEXC_TICKER_24HR_URL = "https://api.mexc.com/api/v3/ticker/24hr"
    const val MEXC_KLINES_URL = "https://api.mexc.com/api/v3/klines"
    const val KUCOIN_CANDLES_URL = "https://api.kucoin.com/api/v1/market/candles"
    const val BSCSCAN_API_URL = "https://api.bscscan.com/api"

    // Network & Timeouts (seconds)
    const val HTTP_CONNECT_TIMEOUT_SECONDS = 10L
    const val HTTP_READ_TIMEOUT_SECONDS = 12L

    // Cache Lifetimes (milliseconds)
    const val CANDLE_CACHE_TTL_MS = 30 * 60 * 1000L // 30 minutes

    // Risk Management & Technical Calculation Bounds (Realistic Momentum Constraints)
    // Ensures projected targets never produce anomalous or astronomical returns (e.g. 970M%)
    const val MIN_SHORT_TERM_GAIN_PCT = 0.015  // 1.5% min short-term target
    const val MAX_SHORT_TERM_GAIN_PCT = 0.080  // 8.0% max short-term target
    const val MIN_MEDIUM_TERM_GAIN_PCT = 0.035 // 3.5% min medium-term target
    const val MAX_MEDIUM_TERM_GAIN_PCT = 0.250 // 25.0% max realistic medium-term target cap
    const val MIN_STOP_LOSS_PCT = 0.015        // 1.5% minimum stop buffer
    const val MAX_STOP_LOSS_PCT = 0.060        // 6.0% maximum structural stop-loss risk
}
