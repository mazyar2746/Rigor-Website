package com.example.engine

import android.util.Log
import com.example.config.RigorConfig
import com.example.data.model.Candle
import com.example.data.model.MarketDataProvenance
import com.example.data.model.SignalType
import com.example.data.model.TechnicalAnalysisResult
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

object TechnicalAnalysisEngine {

    private const val TAG = "TechAnalysisEngine"

    fun analyze(
        symbol: String,
        currentPrice: Double,
        candles: List<Candle>,
        provenance: MarketDataProvenance = MarketDataProvenance.LIVE_EXCHANGE,
        sourceExchange: String? = null,
        pe: Double? = null,
        eps: Double? = null
    ): TechnicalAnalysisResult {
        if (currentPrice <= 0.0 || currentPrice.isNaN() || currentPrice.isInfinite()) {
            return createUnavailableResult(symbol, currentPrice, candles, MarketDataProvenance.UNAVAILABLE, "قیمت نامعتبر")
        }
        if (candles.isEmpty()) {
            return createUnavailableResult(symbol, currentPrice, emptyList(), MarketDataProvenance.UNAVAILABLE, "تاریخچه قیمتی موجود نیست")
        }

        val normalizedCandles = normalizeCandles(symbol, currentPrice, candles)
        val closes = normalizedCandles.map { it.close }

        // --- اندیکاتورهای تکنیکال ---
        val rsiValue = calculateRSI(closes, 14)
        val (rsiSignal, rsiDesc) = evaluateRSI(rsiValue)

        val macdData = calculateMACD(closes, 12, 26, 9)
        val (macdSignal, macdDesc) = evaluateMACD(macdData.macdLine, macdData.signalLine, macdData.histogram)

        val bbData = calculateBollingerBands(closes, 20, 2.0)
        val (bbSignal, bbDesc) = evaluateBollinger(currentPrice, bbData.upper, bbData.middle, bbData.lower)

        // EMA و SMA برای تأیید روند
        val ema20 = calculateEMA(closes, 20).lastOrNull() ?: currentPrice
        val sma50 = closes.takeLast(50).average()
        val trendUp = currentPrice > ema20 && ema20 > sma50
        val trendDown = currentPrice < ema20 && ema20 < sma50

        // --- امتیازدهی تکنیکال ---
        var buyPoints = 0.0
        var sellPoints = 0.0

        when (rsiSignal) {
            SignalType.STRONG_BUY -> buyPoints += 2.0
            SignalType.BUY -> buyPoints += 1.0
            SignalType.SELL -> sellPoints += 1.0
            SignalType.STRONG_SELL -> sellPoints += 2.0
            else -> {}
        }
        when (macdSignal) {
            SignalType.STRONG_BUY -> buyPoints += 2.0
            SignalType.BUY -> buyPoints += 1.2
            SignalType.SELL -> sellPoints += 1.2
            SignalType.STRONG_SELL -> sellPoints += 2.0
            else -> {}
        }
        when (bbSignal) {
            SignalType.STRONG_BUY -> buyPoints += 1.8
            SignalType.BUY -> buyPoints += 1.0
            SignalType.SELL -> sellPoints += 1.0
            SignalType.STRONG_SELL -> sellPoints += 1.8
            else -> {}
        }
        // تأیید روند
        if (trendUp) buyPoints += 1.0
        if (trendDown) sellPoints += 1.0

        // --- امتیازدهی بنیادی (P/E و EPS) ---
        var fundamentalScore = 0.0
        if (pe != null && pe > 0) {
            // P/E پایین‌تر بهتر است (نسبت به میانگین بازار حدود ۷)
            if (pe < 5) fundamentalScore += 1.5
            else if (pe < 7) fundamentalScore += 0.8
            else if (pe > 15) fundamentalScore -= 1.0
        }
        if (eps != null && eps > 0) {
            fundamentalScore += 0.5 // EPS مثبت
        } else if (eps != null && eps < 0) {
            fundamentalScore -= 1.0 // EPS منفی
        }
        buyPoints += max(0.0, fundamentalScore)
        sellPoints += max(0.0, -fundamentalScore)

        val net = buyPoints - sellPoints
        var overallSignal = when {
            net >= 3.5 -> SignalType.STRONG_BUY
            net >= 1.5 -> SignalType.BUY
            net <= -3.5 -> SignalType.STRONG_SELL
            net <= -1.5 -> SignalType.SELL
            else -> SignalType.NEUTRAL
        }

        // --- محاسبه سطوح پیوت ---
        val refCandle = if (normalizedCandles.size >= 2) normalizedCandles[normalizedCandles.size - 2] else normalizedCandles.last()
        val h = maxOf(refCandle.high, refCandle.low, refCandle.close)
        val l = minOf(refCandle.high, refCandle.low, refCandle.close).coerceAtLeast(1.0)
        val c = refCandle.close.coerceIn(l, h)
        val pivot = (h + l + c) / 3.0
        val r2 = max(pivot + (h - l), (2.0 * pivot) - l)
        val r1 = min(pivot + (h - l), (2.0 * pivot) - l).coerceAtLeast(pivot)
        val s1 = max(pivot - (h - l), (2.0 * pivot) - h).coerceAtMost(pivot)
        val s2 = min(pivot - (h - l), (2.0 * pivot) - h)

        // --- تشخیص شکست سطوح کلیدی (Breakout) ---
        // اگر قیمت از R2 بالاتر رفت، سیگنال خرید قوی (مگر اینکه RSI اشباع خرید شدید باشد)
        if (currentPrice > r2 && rsiValue < 80) {
            overallSignal = SignalType.STRONG_BUY
        } else if (currentPrice < s2 && rsiValue > 20) {
            overallSignal = SignalType.STRONG_SELL
        }

        val confidence = min(98, max(52, (50 + abs(net) * 12).toInt()))

        // --- اهداف و حد ضرر ---
        val volatility = if (bbData.middle > 0.0) ((bbData.upper - bbData.lower) / bbData.middle).coerceIn(0.01, 0.50) else 0.02
        val shortTerm = if (overallSignal == SignalType.SELL || overallSignal == SignalType.STRONG_SELL) currentPrice * (1.0 - (volatility * 0.5).coerceIn(0.01, 0.05)) else currentPrice * (1.0 + (volatility * 0.5).coerceIn(0.01, 0.05))
        val mediumTerm = if (overallSignal == SignalType.SELL || overallSignal == SignalType.STRONG_SELL) currentPrice * (1.0 - (volatility * 1.2).coerceIn(0.03, 0.12)) else currentPrice * (1.0 + (volatility * 1.2).coerceIn(0.03, 0.12))
        val stopLoss = if (overallSignal == SignalType.SELL || overallSignal == SignalType.STRONG_SELL) currentPrice * (1.0 + (volatility * 0.3).coerceIn(0.01, 0.03)) else currentPrice * (1.0 - (volatility * 0.3).coerceIn(0.01, 0.03))

        return TechnicalAnalysisResult(
            coinSymbol = symbol, currentPrice = currentPrice, rsi = rsiValue, rsiSignal = rsiSignal,
            rsiDescription = rsiDesc, macdLine = macdData.macdLine, macdSignalLine = macdData.signalLine,
            macdHistogram = macdData.histogram, macdSignal = macdSignal, macdDescription = macdDesc,
            bbUpper = bbData.upper, bbMiddle = bbData.middle, bbLower = bbData.lower, bbSignal = bbSignal,
            bbDescription = bbDesc, overallSignal = overallSignal, confidencePercent = confidence,
            shortTermTarget = shortTerm, mediumTermTarget = mediumTerm, stopLoss = stopLoss,
            resistance2 = r2, resistance1 = r1, pivotPoint = pivot, support1 = s1, support2 = s2,
            candles = normalizedCandles, provenance = provenance,
            dataSourceNotice = if (provenance == MarketDataProvenance.LIVE_EXCHANGE) "داده‌های زنده" else "داده‌های ذخیره شده"
        )
    }

    // توابع کمکی (RSI, MACD, Bollinger, EMA, ...)
    private fun normalizeCandles(symbol: String, currentPrice: Double, candles: List<Candle>): List<Candle> {
        if (candles.isEmpty()) return emptyList()
        val lastClose = candles.last().close
        if (lastClose <= 0.0 || lastClose.isNaN()) return emptyList()
        val ratio = lastClose / currentPrice
        if (ratio > 5.0 || ratio < 0.2) {
            val scaleFactor = currentPrice / lastClose
            return candles.map { c -> Candle(c.timestamp, c.open * scaleFactor, c.high * scaleFactor, c.low * scaleFactor, c.close * scaleFactor, c.volume) }
        }
        return candles
    }

    fun createUnavailableResult(symbol: String, currentPrice: Double, candles: List<Candle>, provenance: MarketDataProvenance = MarketDataProvenance.UNAVAILABLE, reason: String = "داده‌ای موجود نیست"): TechnicalAnalysisResult {
        return TechnicalAnalysisResult(
            coinSymbol = symbol, currentPrice = currentPrice, rsi = 50.0, rsiSignal = SignalType.NEUTRAL, rsiDescription = reason,
            macdLine = 0.0, macdSignalLine = 0.0, macdHistogram = 0.0, macdSignal = SignalType.NEUTRAL, macdDescription = reason,
            bbUpper = currentPrice, bbMiddle = currentPrice, bbLower = currentPrice, bbSignal = SignalType.NEUTRAL, bbDescription = reason,
            overallSignal = SignalType.NEUTRAL, confidencePercent = 0, shortTermTarget = currentPrice, mediumTermTarget = currentPrice, stopLoss = currentPrice,
            resistance2 = currentPrice, resistance1 = currentPrice, pivotPoint = currentPrice, support1 = currentPrice, support2 = currentPrice,
            candles = candles, provenance = provenance, dataSourceNotice = reason
        )
    }

    fun calculateRSI(prices: List<Double>, period: Int = 14): Double {
        if (prices.size < period + 1) return 50.0
        var gains = 0.0; var losses = 0.0
        for (i in 1..period) { val change = prices[i] - prices[i - 1]; if (change > 0) gains += change else losses -= change }
        var avgGain = gains / period; var avgLoss = losses / period
        for (i in (period + 1) until prices.size) {
            val change = prices[i] - prices[i - 1]
            avgGain = (avgGain * (period - 1) + if (change > 0) change else 0.0) / period
            avgLoss = (avgLoss * (period - 1) + if (change < 0) -change else 0.0) / period
        }
        if (avgLoss == 0.0) return if (avgGain == 0.0) 50.0 else 100.0
        return (100.0 - (100.0 / (1.0 + (avgGain / avgLoss)))).coerceIn(0.0, 100.0)
    }

    private fun evaluateRSI(rsi: Double): Pair<SignalType, String> {
        return when {
            rsi >= 75.0 -> SignalType.STRONG_SELL to "اشباع خرید شدید (RSI > 75)"
            rsi >= 70.0 -> SignalType.SELL to "اشباع خرید (RSI > 70)"
            rsi <= 25.0 -> SignalType.STRONG_BUY to "اشباع فروش شدید (RSI < 25)"
            rsi <= 30.0 -> SignalType.BUY to "اشباع فروش (RSI < 30)"
            rsi in 45.0..55.0 -> SignalType.NEUTRAL to "روند خنثی (RSI ~ 50)"
            rsi > 55.0 -> SignalType.BUY to "تمایل صعودی ملایم"
            else -> SignalType.SELL to "تمایل نزولی ملایم"
        }
    }

    data class MacdOutput(val macdLine: Double, val signalLine: Double, val histogram: Double)

    fun calculateMACD(prices: List<Double>, fastPeriod: Int = 12, slowPeriod: Int = 26, signalPeriod: Int = 9): MacdOutput {
        if (prices.size < slowPeriod) return MacdOutput(0.0, 0.0, 0.0)
        val fastEma = calculateEMA(prices, fastPeriod); val slowEma = calculateEMA(prices, slowPeriod)
        val minSize = min(fastEma.size, slowEma.size)
        val macdSeries = mutableListOf<Double>()
        for (i in 0 until minSize) { macdSeries.add(fastEma[fastEma.size - minSize + i] - slowEma[slowEma.size - minSize + i]) }
        val signalSeries = calculateEMA(macdSeries, signalPeriod)
        val lastMacd = macdSeries.lastOrNull() ?: 0.0; val lastSignal = signalSeries.lastOrNull() ?: 0.0
        return MacdOutput(lastMacd, lastSignal, lastMacd - lastSignal)
    }

    private fun evaluateMACD(macd: Double, signal: Double, hist: Double): Pair<SignalType, String> {
        val mag = maxOf(abs(macd), abs(signal), 1e-9)
        return when {
            hist > 0 && macd > signal && hist > mag * 0.1 -> SignalType.STRONG_BUY to "شکوفایی صعودی MACD"
            hist > 0 && macd > signal -> SignalType.BUY to "کراس صعودی MACD"
            hist < 0 && macd < signal && abs(hist) > mag * 0.1 -> SignalType.STRONG_SELL to "شکوفایی نزولی MACD"
            hist < 0 && macd < signal -> SignalType.SELL to "کراس نزولی MACD"
            else -> SignalType.NEUTRAL to "خطوط MACD همگرا"
        }
    }

    private fun calculateEMA(values: List<Double>, period: Int): List<Double> {
        if (values.size < period) return emptyList()
        val emaList = mutableListOf<Double>()
        val multiplier = 2.0 / (period + 1)
        var currentEma = values.take(period).average()
        emaList.add(currentEma)
        for (i in period until values.size) { currentEma = (values[i] - currentEma) * multiplier + currentEma; emaList.add(currentEma) }
        return emaList
    }

    data class BollingerOutput(val upper: Double, val middle: Double, val lower: Double)

    fun calculateBollingerBands(prices: List<Double>, period: Int = 20, multiplier: Double = 2.0): BollingerOutput {
        if (prices.size < period) { val avg = prices.average(); return BollingerOutput(avg * 1.05, avg, (avg * 0.95).coerceAtLeast(0.0)) }
        val subset = prices.takeLast(period); val sma = subset.average()
        val stdDev = sqrt(max(0.0, subset.map { (it - sma).pow(2.0) }.average()))
        return BollingerOutput(sma + (multiplier * stdDev), sma, max(0.0, sma - (multiplier * stdDev)))
    }

    private fun evaluateBollinger(price: Double, upper: Double, middle: Double, lower: Double): Pair<SignalType, String> {
        return when {
            price <= lower -> SignalType.STRONG_BUY to "شکست باند پایین"
            price <= lower * 1.01 -> SignalType.BUY to "تست حمایت باند پایین"
            price >= upper -> SignalType.STRONG_SELL to "شکست باند بالا"
            price >= upper * 0.99 -> SignalType.SELL to "تست مقاومت باند بالا"
            else -> SignalType.NEUTRAL to "نوسان درون کانال"
        }
    }
}
