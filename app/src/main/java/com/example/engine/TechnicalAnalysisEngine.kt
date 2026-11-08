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

    private fun logE(tag: String, msg: String) {
        try { Log.e(tag, msg) } catch (_: Throwable) { System.err.println("ERROR [$tag] $msg") }
    }

    private fun logW(tag: String, msg: String) {
        try { Log.w(tag, msg) } catch (_: Throwable) { println("WARN [$tag] $msg") }
    }

    fun analyze(
        symbol: String,
        currentPrice: Double,
        candles: List<Candle>,
        provenance: MarketDataProvenance = MarketDataProvenance.LIVE_EXCHANGE,
        sourceExchange: String? = null
    ): TechnicalAnalysisResult {
        if (currentPrice <= 0.0 || currentPrice.isNaN() || currentPrice.isInfinite()) {
            logE(TAG, "[$symbol] Invalid currentPrice: $currentPrice. Aborting analysis.")
            return createUnavailableResult(symbol, currentPrice, candles, MarketDataProvenance.UNAVAILABLE, "ارائه تحلیل در این زمان ممکن نیست")
        }

        if (candles.isEmpty()) {
            logW(TAG, "[$symbol] Candlestick history is empty. Aborting analysis.")
            return createUnavailableResult(symbol, currentPrice, emptyList(), MarketDataProvenance.UNAVAILABLE, "ارائه تحلیل در این زمان ممکن نیست")
        }

        val normalizedCandles = normalizeCandles(symbol, currentPrice, candles)
        if (normalizedCandles.isEmpty()) {
            logW(TAG, "[$symbol] Normalized candle series is empty. Aborting analysis.")
            return createUnavailableResult(symbol, currentPrice, emptyList(), MarketDataProvenance.UNAVAILABLE, "ارائه تحلیل در این زمان ممکن نیست")
        }

        if (provenance == MarketDataProvenance.SYNTHETIC) {
            logW(TAG, "[$symbol] Synthetic data provenance detected. Gating analysis: real signals locked.")
            val closes = normalizedCandles.map { it.close }
            val bbData = calculateBollingerBands(closes, period = 20, multiplier = 2.0)
            return TechnicalAnalysisResult(
                coinSymbol = symbol, currentPrice = currentPrice, rsi = 50.0, rsiSignal = SignalType.NEUTRAL,
                rsiDescription = "پیش‌نمایش شبیه‌سازی: اندیکاتورهای تکنیکال برای محافظت از سرمایه غیرفعال هستند.",
                macdLine = 0.0, macdSignalLine = 0.0, macdHistogram = 0.0, macdSignal = SignalType.NEUTRAL,
                macdDescription = "پیش‌نمایش شبیه‌سازی: اندیکاتورهای تکنیکال غیرفعال هستند.",
                bbUpper = bbData.upper, bbMiddle = bbData.middle, bbLower = bbData.lower, bbSignal = SignalType.NEUTRAL,
                bbDescription = "پیش‌نمایش شبیه‌سازی: پاکت بولینگر مصنوعی. سیگنال معاملاتی صادر نمی‌شود.",
                overallSignal = SignalType.NEUTRAL, confidencePercent = 0, shortTermTarget = Double.NaN,
                mediumTermTarget = Double.NaN, stopLoss = Double.NaN, resistance2 = Double.NaN,
                resistance1 = Double.NaN, pivotPoint = Double.NaN, support1 = Double.NaN, support2 = Double.NaN,
                candles = normalizedCandles, provenance = MarketDataProvenance.SYNTHETIC,
                dataSourceNotice = "⚠️ پیش‌نمایش شبیه‌سازی: تاریخچه واقعی بازار در دسترس نیست."
            )
        }

        if (provenance == MarketDataProvenance.UNAVAILABLE) {
            return createUnavailableResult(symbol, currentPrice, candles, MarketDataProvenance.UNAVAILABLE, "ارائه تحلیل در این زمان ممکن نیست")
        }

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
        var overallSignal = when {
            net >= 2.5 -> SignalType.STRONG_BUY
            net >= 0.8 -> SignalType.BUY
            net <= -2.5 -> SignalType.STRONG_SELL
            net <= -0.8 -> SignalType.SELL
            else -> SignalType.NEUTRAL
        }

        val confidence = min(98, max(52, (50 + abs(net) * 12).toInt()))

        val refCandle = if (normalizedCandles.size >= 2) {
            normalizedCandles[normalizedCandles.size - 2]
        } else {
            normalizedCandles.last()
        }

        val rawH = refCandle.high
        val rawL = refCandle.low
        val rawC = refCandle.close

        val h = maxOf(rawH, rawL, rawC)
        val l = minOf(rawH, rawL, rawC).coerceAtLeast(1e-12)
        val c = rawC.coerceIn(l, h)

        val pivot = (h + l + c) / 3.0
        val rawR1 = (2.0 * pivot) - l
        val rawS1 = (2.0 * pivot) - h
        val rawR2 = pivot + (h - l)
        val rawS2 = pivot - (h - l)

        val r2 = max(rawR2, rawR1)
        val r1 = min(rawR2, rawR1).coerceAtLeast(pivot)
        val s1 = max(rawS1, rawS2).coerceAtMost(pivot)
        val s2 = min(rawS1, rawS2)

        // --- تشخیص صف خرید و صف فروش ---
        val priceChangeFromYesterday = (currentPrice - refCandle.close) / refCandle.close
        val isLimitUp = priceChangeFromYesterday >= 0.025 // حدود ۳ درصد رشد (سقف مجاز)
        val isLimitDown = priceChangeFromYesterday <= -0.025 // حدود ۳ درصد افت

        if (isLimitUp) {
            overallSignal = SignalType.STRONG_BUY
            logW(TAG, "[$symbol] Limit up detected (Change: ${priceChangeFromYesterday * 100}%). Overriding signal to STRONG_BUY.")
        } else if (isLimitDown) {
            overallSignal = SignalType.STRONG_SELL
            logW(TAG, "[$symbol] Limit down detected (Change: ${priceChangeFromYesterday * 100}%). Overriding signal to STRONG_SELL.")
        }

        val volatility = if (bbData.middle > 0.0) {
            ((bbData.upper - bbData.lower) / bbData.middle).coerceIn(0.005, 0.50)
        } else {
            0.02
        }

        val shortTerm = when (overallSignal) {
            SignalType.STRONG_BUY, SignalType.BUY -> {
                val shortGain = (volatility * 0.5).coerceIn(RigorConfig.MIN_SHORT_TERM_GAIN_PCT, RigorConfig.MAX_SHORT_TERM_GAIN_PCT)
                val baseTarget = currentPrice * (1.0 + shortGain)
                if (r1 > currentPrice && r1 <= currentPrice * (1.0 + RigorConfig.MAX_SHORT_TERM_GAIN_PCT)) {
                    min(r1, baseTarget)
                } else {
                    baseTarget
                }.coerceIn(currentPrice * (1.0 + RigorConfig.MIN_SHORT_TERM_GAIN_PCT), currentPrice * (1.0 + RigorConfig.MAX_SHORT_TERM_GAIN_PCT))
            }
            SignalType.STRONG_SELL, SignalType.SELL -> {
                val shortDrop = (volatility * 0.5).coerceIn(RigorConfig.MIN_SHORT_TERM_GAIN_PCT, RigorConfig.MAX_SHORT_TERM_GAIN_PCT)
                val baseTarget = currentPrice * (1.0 - shortDrop)
                if (s1 < currentPrice && s1 >= currentPrice * (1.0 - RigorConfig.MAX_SHORT_TERM_GAIN_PCT) && s1 > 0.0) {
                    max(s1, baseTarget)
                } else {
                    baseTarget
                }.coerceIn(currentPrice * (1.0 - RigorConfig.MAX_SHORT_TERM_GAIN_PCT), currentPrice * (1.0 - RigorConfig.MIN_SHORT_TERM_GAIN_PCT))
            }
            SignalType.NEUTRAL -> {
                val neutralGain = (volatility * 0.25).coerceIn(0.01, 0.025)
                currentPrice * (1.0 + neutralGain)
            }
        }

        val mediumTerm = when (overallSignal) {
            SignalType.STRONG_BUY, SignalType.BUY -> {
                val medGain = (volatility * 1.2).coerceIn(RigorConfig.MIN_MEDIUM_TERM_GAIN_PCT, RigorConfig.MAX_MEDIUM_TERM_GAIN_PCT)
                val baseTarget = currentPrice * (1.0 + medGain)
                if (r2 > shortTerm && r2 <= currentPrice * (1.0 + RigorConfig.MAX_MEDIUM_TERM_GAIN_PCT)) {
                    max(r2, baseTarget)
                } else {
                    baseTarget
                }.coerceIn(shortTerm * 1.015, currentPrice * (1.0 + RigorConfig.MAX_MEDIUM_TERM_GAIN_PCT))
            }
            SignalType.STRONG_SELL, SignalType.SELL -> {
                val medDrop = (volatility * 1.2).coerceIn(RigorConfig.MIN_MEDIUM_TERM_GAIN_PCT, RigorConfig.MAX_MEDIUM_TERM_GAIN_PCT)
                val baseTarget = currentPrice * (1.0 - medDrop)
                if (s2 < shortTerm && s2 >= currentPrice * (1.0 - RigorConfig.MAX_MEDIUM_TERM_GAIN_PCT) && s2 > 0.0) {
                    min(s2, baseTarget)
                } else {
                    baseTarget
                }.coerceIn(currentPrice * (1.0 - RigorConfig.MAX_MEDIUM_TERM_GAIN_PCT), shortTerm * 0.985)
            }
            SignalType.NEUTRAL -> {
                val neutralMedGain = (volatility * 0.4).coerceIn(0.02, 0.05)
                shortTerm * (1.0 + neutralMedGain)
            }
        }

        val stopLoss = when (overallSignal) {
            SignalType.STRONG_BUY, SignalType.BUY, SignalType.NEUTRAL -> {
                val supportLevel = if (s1 > 0.0 && s1 < currentPrice) s1 else currentPrice * (1.0 - RigorConfig.MIN_STOP_LOSS_PCT)
                val lowerBand = if (bbData.lower > 0.0 && bbData.lower < currentPrice) bbData.lower else currentPrice * (1.0 - RigorConfig.MIN_STOP_LOSS_PCT)
                val structuralStop = min(supportLevel * 0.995, lowerBand * 0.995)
                structuralStop.coerceIn(
                    currentPrice * (1.0 - RigorConfig.MAX_STOP_LOSS_PCT),
                    currentPrice * (1.0 - RigorConfig.MIN_STOP_LOSS_PCT)
                )
            }
            SignalType.STRONG_SELL, SignalType.SELL -> {
                val resistLevel = if (r1 > currentPrice) r1 else currentPrice * (1.0 + RigorConfig.MIN_STOP_LOSS_PCT)
                val upperBand = if (bbData.upper > currentPrice) bbData.upper else currentPrice * (1.0 + RigorConfig.MIN_STOP_LOSS_PCT)
                val structuralStop = max(resistLevel * 1.005, upperBand * 1.005)
                structuralStop.coerceIn(
                    currentPrice * (1.0 + RigorConfig.MIN_STOP_LOSS_PCT),
                    currentPrice * (1.0 + RigorConfig.MAX_STOP_LOSS_PCT)
                )
            }
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
            dataSourceNotice = if (provenance == MarketDataProvenance.LIVE_EXCHANGE) {
                "داده‌های زنده بازار (${sourceExchange ?: "تایید شده"})"
            } else {
                "داده‌های ذخیره شده (${sourceExchange ?: "تاریخی"})"
            }
        )
    }

    private fun normalizeCandles(symbol: String, currentPrice: Double, candles: List<Candle>): List<Candle> {
        if (candles.isEmpty()) return emptyList()
        val lastClose = candles.last().close
        if (lastClose <= 0.0 || lastClose.isNaN()) {
            logW(TAG, "[$symbol] Invalid candle close: $lastClose. Rejecting candles.")
            return emptyList()
        }
        val ratio = lastClose / currentPrice
        if (ratio > 5.0 || ratio < 0.2) {
            val scaleFactor = currentPrice / lastClose
            logW(TAG, "[$symbol] Unit mismatch detected. Normalizing candles by scale factor $scaleFactor.")
            return candles.map { c -> Candle(timestamp = c.timestamp, open = c.open * scaleFactor, high = c.high * scaleFactor, low = c.low * scaleFactor, close = c.close * scaleFactor, volume = c.volume) }
        }
        return candles
    }

    fun createUnavailableResult(
        symbol: String, currentPrice: Double, candles: List<Candle>,
        provenance: MarketDataProvenance = MarketDataProvenance.UNAVAILABLE,
        reason: String = "ارائه تحلیل در این زمان ممکن نیست"
    ): TechnicalAnalysisResult {
        return TechnicalAnalysisResult(
            coinSymbol = symbol, currentPrice = currentPrice, rsi = 50.0, rsiSignal = SignalType.NEUTRAL,
            rsiDescription = reason, macdLine = 0.0, macdSignalLine = 0.0, macdHistogram = 0.0,
            macdSignal = SignalType.NEUTRAL, macdDescription = reason, bbUpper = Double.NaN,
            bbMiddle = Double.NaN, bbLower = Double.NaN, bbSignal = SignalType.NEUTRAL,
            bbDescription = reason, overallSignal = SignalType.NEUTRAL, confidencePercent = 0,
            shortTermTarget = Double.NaN, mediumTermTarget = Double.NaN, stopLoss = Double.NaN,
            resistance2 = Double.NaN, resistance1 = Double.NaN, pivotPoint = Double.NaN,
            support1 = Double.NaN, support2 = Double.NaN, candles = candles, provenance = provenance,
            dataSourceNotice = reason
        )
    }

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
        if (avgLoss == 0.0) return if (avgGain == 0.0) 50.0 else 100.0
        val rs = avgGain / avgLoss
        return (100.0 - (100.0 / (1.0 + rs))).coerceIn(0.0, 100.0)
    }

    private fun evaluateRSI(rsi: Double): Pair<SignalType, String> {
        return when {
            rsi >= 75.0 -> SignalType.STRONG_SELL to "اشباع خرید شدید (RSI > ۷۵). احتمال بالای اصلاح و فشار فروش سنگین."
            rsi >= 70.0 -> SignalType.SELL to "اشباع خرید (RSI > ۷۰). مقاومت و فشار فروش محتمل است."
            rsi <= 25.0 -> SignalType.STRONG_BUY to "اشباع فروش شدید (RSI < ۲۵). احتمال بالای بازگشت (Rebound)."
            rsi <= 30.0 -> SignalType.BUY to "اشباع فروش (RSI < ۳۰). منطقه مناسب برای تجمیع و خرید."
            rsi in 45.0..55.0 -> SignalType.NEUTRAL to "روند خنثی (RSI ~ ۵۰). تعادل نسبی بین خریداران و فروشندگان."
            rsi > 55.0 -> SignalType.BUY to "تمایل صعودی ملایم با مومنتوم مثبت."
            else -> SignalType.SELL to "تمایل نزولی ملایم با مومنتوم نزولی."
        }
    }

    data class MacdOutput(val macdLine: Double, val signalLine: Double, val histogram: Double)

    fun calculateMACD(prices: List<Double>, fastPeriod: Int = 12, slowPeriod: Int = 26, signalPeriod: Int = 9): MacdOutput {
        if (prices.size < slowPeriod) return MacdOutput(0.0, 0.0, 0.0)
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
        val magnitudeRef = maxOf(abs(macd), abs(signal), 1e-9)
        return when {
            hist > 0 && macd > signal && hist > magnitudeRef * 0.1 -> SignalType.STRONG_BUY to "شکوفایی صعودی: خط MACD با شتاب بالاتر از خط سیگنال قرار گرفت."
            hist > 0 && macd > signal -> SignalType.BUY to "کراس صعودی MACD تایید شد. روند صعودی در حال شکل‌گیری است."
            hist < 0 && macd < signal && abs(hist) > magnitudeRef * 0.1 -> SignalType.STRONG_SELL to "شکوفایی نزولی: خط MACD با شتاب پایین‌تر از خط سیگنال قرار گرفت."
            hist < 0 && macd < signal -> SignalType.SELL to "کراس نزولی MACD شناسایی شد. مومنتوم نزولی فعال است."
            else -> SignalType.NEUTRAL to "خطوط MACD در حال همگرایی با واگرایی کم هستند."
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

    data class BollingerOutput(val upper: Double, val middle: Double, val lower: Double)

    fun calculateBollingerBands(prices: List<Double>, period: Int = 20, multiplier: Double = 2.0): BollingerOutput {
        if (prices.size < period) {
            val avg = if (prices.isNotEmpty()) prices.average() else 0.0
            return BollingerOutput(avg * 1.05, avg, (avg * 0.95).coerceAtLeast(0.0))
        }
        val subset = prices.takeLast(period)
        val sma = subset.average()
        val variance = subset.map { (it - sma).pow(2.0) }.average()
        val stdDev = sqrt(max(0.0, variance))
        return BollingerOutput(
            upper = sma + (multiplier * stdDev),
            middle = sma,
            lower = max(0.0, sma - (multiplier * stdDev))
        )
    }

    private fun evaluateBollinger(price: Double, upper: Double, middle: Double, lower: Double): Pair<SignalType, String> {
        if (price <= 0.0 || upper <= 0.0 || lower < 0.0 || upper.isNaN() || lower.isNaN()) {
            return SignalType.NEUTRAL to "داده‌های پاکت بولینگر کافی نیست."
        }
        return when {
            price <= lower -> SignalType.STRONG_BUY to "قیمت باند پایین ۲σ را شکست. پتانسیل بازگشت به میانگین (حمایت)."
            price <= lower * 1.01 -> SignalType.BUY to "قیمت در حال تست حمایت باند پایین است. اشباع فروش داینامیک."
            price >= upper -> SignalType.STRONG_SELL to "قیمت باند بالای ۲σ را شکست. اشباع خرید نزدیک مقاومت."
            price >= upper * 0.99 -> SignalType.SELL to "قیمت در حال تست مقاومت باند بالا است. ریسک خستگی روند."
            else -> SignalType.NEUTRAL to "قیمت در حال نوسان درون کانال بولینگر است."
        }
    }
}
