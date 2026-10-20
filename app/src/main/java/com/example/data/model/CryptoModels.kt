package com.example.data.model

data class CryptoCoin(
    val id: String,
    val name: String,
    val symbol: String,
    val priceUsd: Double,
    val change24h: Double,
    val volume24h: Double,
    val marketCap: Double,
    val high24h: Double,
    val low24h: Double,
    val rank: Int = 1,
    val iconUrl: String = ""
)

data class Candle(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
)

enum class SignalType {
    STRONG_BUY,
    BUY,
    NEUTRAL,
    SELL,
    STRONG_SELL
}

data class TechnicalAnalysisResult(
    val coinSymbol: String,
    val currentPrice: Double,
    val rsi: Double,
    val rsiSignal: SignalType,
    val rsiDescription: String,
    val macdLine: Double,
    val macdSignalLine: Double,
    val macdHistogram: Double,
    val macdSignal: SignalType,
    val macdDescription: String,
    val bbUpper: Double,
    val bbMiddle: Double,
    val bbLower: Double,
    val bbSignal: SignalType,
    val bbDescription: String,
    val overallSignal: SignalType,
    val confidencePercent: Int,
    val shortTermTarget: Double,
    val mediumTermTarget: Double,
    val stopLoss: Double,
    val resistance2: Double,
    val resistance1: Double,
    val pivotPoint: Double,
    val support1: Double,
    val support2: Double,
    val candles: List<Candle>
)

enum class SubscriptionPlan(
    val id: String,
    val usdtAmount: Double,
    val durationDays: Int
) {
    MONTHLY("monthly", 9.99, 30),
    YEARLY("yearly", 85.0, 365)
}

data class UserSubscription(
    val isActive: Boolean,
    val plan: SubscriptionPlan?,
    val expiryTimestamp: Long,
    val txHash: String,
    val isSandbox: Boolean = false
) {
    val daysRemaining: Int
        get() {
            if (!isActive) return 0
            val diff = expiryTimestamp - System.currentTimeMillis()
            return if (diff > 0) (diff / (1000 * 60 * 60 * 24)).toInt() + 1 else 0
        }
}
