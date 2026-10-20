package com.example.engine

import com.example.data.model.Candle
import com.example.data.model.SignalType
import com.example.data.model.TechnicalAnalysisResult
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

object TechnicalAnalysisEngine {

    fun analyze(
        symbol: String,
        currentPrice: Double,
        candles: List<Candle>
    ): TechnicalAnalysisResult {
        val closes = candles.map { it.close }
        val highs = candles.map { it.high }
        val lows = candles.map { it.low }

        val rsiValue = calculateRSI(closes, period = 14)
        val (rsiSignal, rsiDesc) = evaluateRSI(rsiValue)

        val macdData = calculateMACD(closes, fastPeriod = 12, slowPeriod = 26, signalPeriod = 9)
        val (macdSignal, macdDesc) = evaluateMACD(macdData.macdLine, macdData.signalLine, macdData.histogram)

        val bbData = calculateBollingerBands(closes, period = 20, multiplier = 2.0)
        val (bbSignal, bbDesc) = evaluateBollinger(currentPrice, bbData.upper, bbData.lower)

        // Combine indicators for confluence
        var buyPoints = 0.0
        var sellPoints = 0.0

        when (rsiSignal) {
            SignalType.STRONG_BUY -> buyPoints += 2.0
            SignalType.BUY -> buyPoints += 1.0
            SignalType.SELL -> sellPoints += 1.0
            SignalType.STRONG_SELL -> sellPoints += 2.0
            SignalType.NEUTRAL -> {}
        }

        when (macdSignal) {
            SignalType.STRONG_BUY -> buyPoints += 2.0
            SignalType.BUY -> buyPoints += 1.2
            SignalType.SELL -> sellPoints += 1.2
            SignalType.STRONG_SELL -> sellPoints += 2.0
            SignalType.NEUTRAL -> {}
        }

        when (bbSignal) {
            SignalType.STRONG_BUY -> buyPoints += 1.8
            SignalType.BUY -> buyPoints += 1.0
            SignalType.SELL -> sellPoints += 1.0
            SignalType.STRONG_SELL -> sellPoints += 1.8
            SignalType.NEUTRAL -> {}
        }

        val net = buyPoints - sellPoints
        val overallSignal = when {
            net >= 2.5 -> SignalType.STRONG_BUY
            net >= 0.8 -> SignalType.BUY
            net <= -2.5 -> SignalType.STRONG_SELL
            net <= -0.8 -> SignalType.SELL
            else -> SignalType.NEUTRAL
        }

        val confidence = min(98, max(52, (50 + abs(net) * 12).toInt()))

        // Support and Resistance calculation using Classical Pivot Points
        val recentHigh = highs.takeLast(20).maxOrNull() ?: (currentPrice * 1.04)
        val recentLow = lows.takeLast(20).minOrNull() ?: (currentPrice * 0.96)
        val pivot = (recentHigh + recentLow + currentPrice) / 3.0
        val r1 = 2 * pivot - recentLow
        val s1 = 2 * pivot - recentHigh
        val r2 = pivot + (recentHigh - recentLow)
        val s2 = pivot - (recentHigh - recentLow)

        // Targets calculation based on momentum & volatility
        val volatility = (bbData.upper - bbData.lower) / (bbData.middle.coerceAtLeast(0.0001))
        val momentumMultiplier = when (overallSignal) {
            SignalType.STRONG_BUY -> 1.0 + (volatility * 0.6).coerceIn(0.02, 0.12)
            SignalType.BUY -> 1.0 + (volatility * 0.35).coerceIn(0.015, 0.08)
            SignalType.STRONG_SELL -> 1.0 - (volatility * 0.6).coerceIn(0.02, 0.12)
            SignalType.SELL -> 1.0 - (volatility * 0.35).coerceIn(0.015, 0.08)
            SignalType.NEUTRAL -> 1.008
        }

        val shortTerm = if (overallSignal == SignalType.BUY || overallSignal == SignalType.STRONG_BUY) {
            max(currentPrice * 1.015, min(r1, currentPrice * momentumMultiplier))
        } else if (overallSignal == SignalType.SELL || overallSignal == SignalType.STRONG_SELL) {
            min(currentPrice * 0.985, max(s1, currentPrice * momentumMultiplier))
        } else {
            currentPrice * 1.012
        }

        val mediumTerm = if (overallSignal == SignalType.BUY || overallSignal == SignalType.STRONG_BUY) {
            max(shortTerm * 1.025, r2)
        } else if (overallSignal == SignalType.SELL || overallSignal == SignalType.STRONG_SELL) {
            min(shortTerm * 0.975, s2)
        } else {
            currentPrice * 1.035
        }

        val stopLoss = if (overallSignal == SignalType.BUY || overallSignal == SignalType.STRONG_BUY) {
            min(currentPrice * 0.965, max(s1 * 0.99, bbData.lower * 0.985))
        } else {
            max(currentPrice * 1.035, r1 * 1.01)
        }

        return TechnicalAnalysisResult(
            coinSymbol = symbol,
            currentPrice = currentPrice,
            rsi = rsiValue,
            rsiSignal = rsiSignal,
            rsiDescription = rsiDesc,
            macdLine = macdData.macdLine,
            macdSignalLine = macdData.signalLine,
            macdHistogram = macdData.histogram,
            macdSignal = macdSignal,
            macdDescription = macdDesc,
            bbUpper = bbData.upper,
            bbMiddle = bbData.middle,
            bbLower = bbData.lower,
            bbSignal = bbSignal,
            bbDescription = bbDesc,
            overallSignal = overallSignal,
            confidencePercent = confidence,
            shortTermTarget = shortTerm,
            mediumTermTarget = mediumTerm,
            stopLoss = stopLoss,
            resistance2 = r2,
            resistance1 = r1,
            pivotPoint = pivot,
            support1 = s1,
            support2 = s2,
            candles = candles
        )
    }

    // 14-period RSI using Wilder's Smoothing
    fun calculateRSI(prices: List<Double>, period: Int = 14): Double {
        if (prices.size < period + 1) return 50.0

        var gains = 0.0
        var losses = 0.0

        for (i in 1..period) {
            val change = prices[i] - prices[i - 1]
            if (change > 0) gains += change else losses += -change
        }

        var avgGain = gains / period
        var avgLoss = losses / period

        for (i in (period + 1) until prices.size) {
            val change = prices[i] - prices[i - 1]
            val currentGain = if (change > 0) change else 0.0
            val currentLoss = if (change < 0) -change else 0.0

            avgGain = (avgGain * (period - 1) + currentGain) / period
            avgLoss = (avgLoss * (period - 1) + currentLoss) / period
        }

        if (avgLoss == 0.0) return 100.0
        val rs = avgGain / avgLoss
        return (100.0 - (100.0 / (1.0 + rs))).coerceIn(0.0, 100.0)
    }

    private fun evaluateRSI(rsi: Double): Pair<SignalType, String> {
        return when {
            rsi >= 75.0 -> SignalType.STRONG_SELL to "Extremely Overbought (RSI > 75). Heavy profit taking expected."
            rsi >= 70.0 -> SignalType.SELL to "Overbought (RSI > 70). Resistance pressure likely."
            rsi <= 25.0 -> SignalType.STRONG_BUY to "Deeply Oversold (RSI < 25). High probability reversal bounce."
            rsi <= 30.0 -> SignalType.BUY to "Oversold (RSI < 30). Accumulation zone identified."
            rsi in 45.0..55.0 -> SignalType.NEUTRAL to "Neutral momentum (RSI ~ 50). Balanced order books."
            rsi > 55.0 -> SignalType.BUY to "Mild bullish bias with positive momentum."
            else -> SignalType.SELL to "Mild bearish bias with descending momentum."
        }
    }

    // MACD (12, 26, 9)
    data class MacdOutput(val macdLine: Double, val signalLine: Double, val histogram: Double)

    fun calculateMACD(
        prices: List<Double>,
        fastPeriod: Int = 12,
        slowPeriod: Int = 26,
        signalPeriod: Int = 9
    ): MacdOutput {
        if (prices.size < slowPeriod) {
            return MacdOutput(0.0, 0.0, 0.0)
        }

        val fastEma = calculateEMA(prices, fastPeriod)
        val slowEma = calculateEMA(prices, slowPeriod)

        val macdSeries = mutableListOf<Double>()
        val minSize = min(fastEma.size, slowEma.size)
        val fastOffset = fastEma.size - minSize
        val slowOffset = slowEma.size - minSize

        for (i in 0 until minSize) {
            macdSeries.add(fastEma[fastOffset + i] - slowEma[slowOffset + i])
        }

        val signalSeries = calculateEMA(macdSeries, signalPeriod)
        val lastMacd = macdSeries.lastOrNull() ?: 0.0
        val lastSignal = signalSeries.lastOrNull() ?: 0.0
        val hist = lastMacd - lastSignal

        return MacdOutput(lastMacd, lastSignal, hist)
    }

    private fun evaluateMACD(macd: Double, signal: Double, hist: Double): Pair<SignalType, String> {
        return when {
            hist > 0 && macd > signal && hist > abs(macd * 0.1) ->
                SignalType.STRONG_BUY to "Bullish expansion: MACD line accelerating above signal line."
            hist > 0 && macd > signal ->
                SignalType.BUY to "Bullish crossover confirmed. Upward trend developing."
            hist < 0 && macd < signal && abs(hist) > abs(macd * 0.1) ->
                SignalType.STRONG_SELL to "Bearish expansion: MACD plunging below signal line."
            hist < 0 && macd < signal ->
                SignalType.SELL to "Bearish crossover detected. Downward momentum active."
            else ->
                SignalType.NEUTRAL to "MACD lines converging with low divergence."
        }
    }

    private fun calculateEMA(values: List<Double>, period: Int): List<Double> {
        if (values.isEmpty()) return emptyList()
        val emaList = mutableListOf<Double>()
        val multiplier = 2.0 / (period + 1)

        var currentEma = values.take(period).average()
        emaList.add(currentEma)

        for (i in period until values.size) {
            currentEma = (values[i] - currentEma) * multiplier + currentEma
            emaList.add(currentEma)
        }
        return emaList
    }

    // Bollinger Bands (20-period SMA, 2 standard deviations)
    data class BollingerOutput(val upper: Double, val middle: Double, val lower: Double)

    fun calculateBollingerBands(
        prices: List<Double>,
        period: Int = 20,
        multiplier: Double = 2.0
    ): BollingerOutput {
        if (prices.size < period) {
            val avg = if (prices.isNotEmpty()) prices.average() else 0.0
            return BollingerOutput(avg * 1.05, avg, avg * 0.95)
        }

        val subset = prices.takeLast(period)
        val sma = subset.average()
        val variance = subset.map { (it - sma).pow(2.0) }.average()
        val stdDev = sqrt(variance)

        return BollingerOutput(
            upper = sma + (multiplier * stdDev),
            middle = sma,
            lower = sma - (multiplier * stdDev)
        )
    }

    private fun evaluateBollinger(price: Double, upper: Double, lower: Double): Pair<SignalType, String> {
        return when {
            price <= lower ->
                SignalType.STRONG_BUY to "Price pierced lower 2σ band. Strong mean reversion support."
            price <= lower * 1.01 ->
                SignalType.BUY to "Price testing lower band support. Oversold dynamic."
            price >= upper ->
                SignalType.STRONG_SELL to "Price pierced upper 2σ band. Overextended at resistance."
            price >= upper * 0.99 ->
                SignalType.SELL to "Price testing upper band resistance. Exhaustion risk."
            else ->
                SignalType.NEUTRAL to "Price oscillating comfortably within Bollinger envelope."
        }
    }
}
