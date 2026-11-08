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
        sourceExchange: String? = null
    ): TechnicalAnalysisResult {
        if (currentPrice <= 0.0 || currentPrice.isNaN() || currentPrice.isInfinite()) {
            return createUnavailableResult(symbol, currentPrice, candles, MarketDataProvenance.UNAVAILABLE, "ارائه تحلیل در این زمان ممکن نیست")
        }

        if (candles.isEmpty()) {
            return createUnavailableResult(symbol, currentPrice, emptyList(), MarketDataProvenance.UNAVAILABLE, "تاریخچه قیمتی موجود نیست")
        }

        val normalizedCandles = normalizeCandles(symbol, currentPrice, candles)
        val closes = normalizedCandles.map { it.close }

        val rsiValue = calculateRSI(closes, period = 14)
        val (rsiSignal, rsiDesc) = evaluateRSI(rsiValue)

        val macdData = calculateMACD(closes, fastPeriod = 12, slowPeriod = 26, signalPeriod = 9)
        val (macdSignal, macdDesc) = evaluateMACD(macdData.macdLine, macdData.signalLine, macdData.histogram)

        val bbData = calculateBollingerBands(closes, period = 20, multiplier = 2.0)
        val (bbSignal, bbDesc) = evaluateBollinger(currentPrice, bbData.upper, bbData.middle, bbData.lower)

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

        val net = buyPoints - sellPoints
        var overallSignal = when {
            net >= 2.5 -> SignalType.STRONG_BUY
            net >= 0.8 -> SignalType.BUY
            net <= -2.5 -> SignalType.STRONG_SELL
            net <= -0.8 -> SignalType.SELL
            else -> SignalType.NEUTRAL
        }

        val confidence = min(98, max(52, (50 + abs(net) * 12).toInt()))

        // محاسبه سطوح پیوت (با هندل کردن NaN)
        val refCandle = if (normalizedCandles.size >= 2) normalizedCandles[normalizedCandles.size - 2] else normalizedCandles.last()
        val h = maxOf(refCandle.high, refCandle.low, refCandle.close)
        val l = minOf(refCandle.high, refCandle.low, refCandle.close).coerceAtLeast(1.0)
        val c = refCandle.close.coerceIn(l, h)

        val pivot = (h + l + c) / 3.0
        val r2 = max(pivot + (h - l), (2.0 * pivot) - l)
        val r1 = min(pivot + (h - l), (2.0 * pivot) - l).coerceAtLeast(pivot)
        val s1 = max(pivot - (h - l), (2.0 * pivot) - h).coerceAtMost(pivot)
        val s2 = min(pivot - (h - l), (2.0 * pivot) - h)

        // --- منطق فوق هوشمند تشخیص صف خرید و فروش ---
        // اگر قیمت فعلی برابر با بالاترین قیمت امروز باشد و حجم معاملات خیلی بالا باشد، یعنی صف خرید
        val isLimitUp = currentPrice >= refCandle.high && refCandle.volume > 0 && (currentPrice / refCandle.close > 1.01)
        val isLimitDown = currentPrice <= refCandle.low && refCandle.volume > 0 && (currentPrice / refCandle.close < 0.99)

        if (isLimitUp && overallSignal != SignalType.STRONG_SELL) {
            overallSignal = SignalType.STRONG_BUY
        } else if (isLimitDown && overallSignal != SignalType.STRONG_BUY) {
            overallSignal = SignalType.STRONG_SELL
        }

        val volatility = if (bbData.middle > 0.0) {
            ((bbData.upper - bbData.lower) / bbData.middle).coerceIn(0.005, 0.50)
        } else 0.02

        // محاسبه اهداف با هندل کردن حالت‌های خاص
        val shortTerm = if (overallSignal == SignalType.SELL || overallSignal == SignalType.STRONG_SELL) {
            currentPrice * (1.0 - (volatility * 0.5).coerceIn(0.01, 0.05))
        } else {
            currentPrice * (1.0 + (volatility * 0.5).coerceIn(0.01, 0.05))
        }

        val mediumTerm = if (overallSignal == SignalType.SELL || overallSignal == SignalType.STRONG_SELL) {
            currentPrice * (1.0 - (volatility * 1.2).coerceIn(0.03, 0.12))
        } else {
            currentPrice * (1.0 + (volatility * 1.2).coerceIn(0.03, 0.12))
        }

        val stopLoss = if (overallSignal == SignalType.SELL || overallSignal == SignalType.STRONG_SELL) {
            currentPrice * (1.0 + (volatility * 0.3).coerceIn(0.01, 0.03))
        } else {
            currentPrice * (1.0 - (volatility * 0.3).coerceIn(0.01, 0.03))
        }

        return TechnicalAnalysisResult(
            coinSymbol = symbol, currentPrice = currentPrice, rsi = rsiValue, rsiSignal = rsiSignal,
            rsiDescription = rsiDesc, macdLine = macdData.macdLine, macdSignalLine = macdData.signalLine,
            macdHistogram = macdData.histogram, macdSignal = macdSignal, macdDescription = macdDesc,
            bbUpper = bbData.upper, bbMiddle = bbData.middle, bbLower = bbData.lower, bbSignal = bbSignal,
            bbDescription = bbDesc, overallSignal = overallSignal, confidencePercent = confidence,
            shortTermTarget = shortTerm, mediumTermTarget = mediumTerm, stopLoss = stopLoss,
            resistance2 = r2, resistance1 = r1, pivotPoint = pivot, support1 = s1, support2 = s2,
            candles = normalizedCandles, provenance = provenance,
            dataSourceNotice = if (provenance == MarketDataProvenance.LIVE_EXCHANGE) "داده‌های زنده بازار" else "داده‌های ذخیره شده"
        )
    }

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
        for (i in 1..period) {
            val change = prices[i] - prices[i - 1]
            if (change > 0) gains += change else losses -= change
        }
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
            rsi >= 75.0 -> SignalType.STRONG_SELL to "اشباع خرید شدید (RSI > ۷۵). احتمال بالای اصلاح."
            rsi >= 70.0 -> SignalType.SELL to "اشباع خرید (RSI > ۷۰). مقاومت محتمل است."
            rsi <= 25.0 -> SignalType.STRONG_BUY to "اشباع فروش شدید (RSI < ۲۵). احتمال بازگشت."
            rsi <= 30.0 -> SignalType.BUY to "اشباع فروش (RSI < ۳۰). منطقه خرید."
            rsi in 45.0..55.0 -> SignalType.NEUTRAL to "روند خنثی (RSI ~ ۵۰). تعادل بازار."
            rsi > 55.0 -> SignalType.BUY to "تمایل صعودی ملایم."
            else -> SignalType.SELL to "تمایل نزولی ملایم."
        }
    }

    data class MacdOutput(val macdLine: Double, val signalLine: Double, val histogram: Double)

    fun calculateMACD(prices: List<Double>, fastPeriod: Int = 12, slowPeriod: Int = 26, signalPeriod: Int = 9): MacdOutput {
        if (prices.size < slowPeriod) return MacdOutput(0.0, 0.0, 0.0)
        val fastEma = calculateEMA(prices, fastPeriod)
        val slowEma = calculateEMA(prices, slowPeriod)
        val minSize = min(fastEma.size, slowEma.size)
        val macdSeries = mutableListOf<Double>()
        for (i in 0 until minSize) { macdSeries.add(fastEma[fastEma.size - minSize + i] - slowEma[slowEma.size - minSize + i]) }
        val signalSeries = calculateEMA(macdSeries, signalPeriod)
        val lastMacd = macdSeries.lastOrNull() ?: 0.0
        val lastSignal = signalSeries.lastOrNull() ?: 0.0
        return MacdOutput(lastMacd, lastSignal, lastMacd - lastSignal)
    }

    private fun evaluateMACD(macd: Double, signal: Double, hist: Double): Pair<SignalType, String> {
        val mag = maxOf(abs(macd), abs(signal), 1e-9)
        return when {
            hist > 0 && macd > signal && hist > mag * 0.1 -> SignalType.STRONG_BUY to "شکوفایی صعودی MACD."
            hist > 0 && macd > signal -> SignalType.BUY to "کراس صعودی MACD تایید شد."
            hist < 0 && macd < signal && abs(hist) > mag * 0.1 -> SignalType.STRONG_SELL to "شکوفایی نزولی MACD."
            hist < 0 && macd < signal -> SignalType.SELL to "کراس نزولی MACD شناسایی شد."
            else -> SignalType.NEUTRAL to "خطوط MACD همگرا هستند."
        }
    }

    private fun calculateEMA(values: List<Double>, period: Int): List<Double> {
        if (values.size < period) return emptyList()
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

    data class BollingerOutput(val upper: Double, val middle: Double, val lower: Double)

    fun calculateBollingerBands(prices: List<Double>, period: Int = 20, multiplier: Double = 2.0): BollingerOutput {
        if (prices.size < period) {
            val avg = prices.average()
            return BollingerOutput(avg * 1.05, avg, (avg * 0.95).coerceAtLeast(0.0))
        }
        val subset = prices.takeLast(period)
        val sma = subset.average()
        val stdDev = sqrt(max(0.0, subset.map { (it - sma).pow(2.0) }.average()))
        return BollingerOutput(sma + (multiplier * stdDev), sma, max(0.0, sma - (multiplier * stdDev)))
    }

    private fun evaluateBollinger(price: Double, upper: Double, middle: Double, lower: Double): Pair<SignalType, String> {
        return when {
            price <= lower -> SignalType.STRONG_BUY to "شکست باند پایین. پتانسیل بازگشت."
            price <= lower * 1.01 -> SignalType.BUY to "تست حمایت باند پایین."
            price >= upper -> SignalType.STRONG_SELL to "شکست باند بالا. اشباع خرید."
            price >= upper * 0.99 -> SignalType.SELL to "تست مقاومت باند بالا."
            else -> SignalType.NEUTRAL to "نوسان درون کانال بولینگر."
        }
    }
}
