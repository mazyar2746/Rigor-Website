package com.example

import com.example.data.model.Candle
import com.example.data.model.MarketDataProvenance
import com.example.data.model.SignalType
import com.example.engine.TechnicalAnalysisEngine
import com.example.ui.util.PriceFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TechnicalAnalysisIntegrityTest {

    @Test
    fun testStandardPivotPointFormulasKnownFixture() {
        // Known OHLC test fixture: H = 100.0, L = 80.0, C = 90.0
        // Expected Classical Floor Pivots:
        // PP = (H + L + C) / 3 = (100 + 80 + 90) / 3 = 90.0
        // R1 = (2 * PP) - L = 180 - 80 = 100.0
        // S1 = (2 * PP) - H = 180 - 100 = 80.0
        // R2 = PP + (H - L) = 90 + 20 = 110.0
        // S2 = PP - (H - L) = 90 - 20 = 70.0
        val candles = listOf(
            Candle(timestamp = 1000L, open = 85.0, high = 100.0, low = 80.0, close = 90.0, volume = 1000.0)
        )

        val result = TechnicalAnalysisEngine.analyze("TEST", 90.0, candles)

        assertEquals("PP must equal (H + L + C) / 3", 90.0, result.pivotPoint, 0.001)
        assertEquals("R1 must equal (2 * PP) - L", 100.0, result.resistance1, 0.001)
        assertEquals("S1 must equal (2 * PP) - H", 80.0, result.support1, 0.001)
        assertEquals("R2 must equal PP + (H - L)", 110.0, result.resistance2, 0.001)
        assertEquals("S2 must equal PP - (H - L)", 70.0, result.support2, 0.001)

        // Strict inequality validation: R2 > R1 >= PP >= S1 > S2
        assertTrue("R2 must be >= R1", result.resistance2 >= result.resistance1)
        assertTrue("R1 must be >= PP", result.resistance1 >= result.pivotPoint)
        assertTrue("PP must be >= S1", result.pivotPoint >= result.support1)
        assertTrue("S1 must be >= S2", result.support1 >= result.support2)
    }

    @Test
    fun testPivotOrderingAcrossVariedFixtures() {
        val fixtures = listOf(
            Triple(120.0, 95.0, 110.0),
            Triple(0.0000065, 0.0000048, 0.0000052),
            Triple(68500.0, 66200.0, 67800.0),
            Triple(1.05, 0.95, 1.02),
            Triple(50.0, 50.0, 50.0) // Flat market edge case
        )

        for ((h, l, c) in fixtures) {
            val candle = Candle(1L, l, h, l, c, 500.0)
            val result = TechnicalAnalysisEngine.analyze("FIX", c, listOf(candle))

            assertTrue("R2 >= R1 failed for ($h, $l, $c)", result.resistance2 >= result.resistance1 - 1e-9)
            assertTrue("R1 >= PP failed for ($h, $l, $c)", result.resistance1 >= result.pivotPoint - 1e-9)
            assertTrue("PP >= S1 failed for ($h, $l, $c)", result.pivotPoint >= result.support1 - 1e-9)
            assertTrue("S1 >= S2 failed for ($h, $l, $c)", result.support1 >= result.support2 - 1e-9)
        }
    }

    @Test
    fun testLowPricedTokenSHIBPrecision() {
        val shibPrice = 0.00000512
        val candles = mutableListOf<Candle>()
        for (i in 0 until 25) {
            val p = shibPrice * (1.0 + (i - 12) * 0.003)
            candles.add(Candle(i * 3600000L, p, p * 1.015, p * 0.985, p, 1000000.0))
        }

        val result = TechnicalAnalysisEngine.analyze("SHIB", shibPrice, candles)

        // Verify SHIB levels are strictly in the micro-cent unit scale
        assertTrue("SHIB current price should be ~0.000005", result.currentPrice < 0.0001)
        assertTrue("SHIB PP should be in micro-dollar unit", result.pivotPoint in 0.000004..0.000006)
        assertTrue("SHIB R1 should be in micro-dollar unit", result.resistance1 in 0.000004..0.000007)
        assertTrue("SHIB S1 should be in micro-dollar unit", result.support1 in 0.000003..0.000006)
        assertTrue("SHIB Short term target must be in micro-dollar unit", result.shortTermTarget in 0.000003..0.000008)
        assertTrue("SHIB Stop loss must be in micro-dollar unit", result.stopLoss in 0.000003..0.000007)

        // PriceFormatter must preserve 8 decimal places for SHIB, not "$0.00"
        val formatted = PriceFormatter.formatPrice(shibPrice)
        assertEquals("$0.00000512", formatted)
        assertFalse("PriceFormatter should never display $0.00 for valid micro-token", formatted == "$0.00")
    }

    @Test
    fun testScreenshotBugReproductionAndNormalization() {
        // In the defect shown in the screenshot:
        // Candles were generated around $100 (high=106.69, low=94.92), but currentPrice was SHIB (~0.000005)
        // With the old buggy formula:
        // PP = (106.69 + 94.92 + 0.0) / 3 = 67.20
        // R1 = 2 * 67.20 - 94.92 = 39.48 (< PP!)
        // S2 = 67.20 - 11.77 = 55.43 (> S1: 27.71!)
        // This produced the exact inverted numbers from the screenshot:
        // R2 = $78.97, R1 = $39.48, PP = $67.20, S1 = $27.71, S2 = $55.43
        val mismatchedCandles = listOf(
            Candle(1L, 100.0, 106.69, 94.92, 100.0, 1000.0),
            Candle(2L, 100.0, 106.69, 94.92, 100.0, 1000.0)
        )

        val shibPrice = 0.00000508
        // The fixed engine must detect the unit discrepancy and normalize the candles to shibPrice
        val result = TechnicalAnalysisEngine.analyze("SHIB", shibPrice, mismatchedCandles)

        // PP must NOT be 67.20
        assertFalse("PP must not be $67.20 from mismatched units", abs(result.pivotPoint - 67.20) < 1.0)
        assertTrue("PP must be normalized to SHIB price scale", result.pivotPoint < 0.0001)

        // Strict ordering must hold (R1 > PP, S1 > S2)
        assertTrue("R2 >= R1 must hold", result.resistance2 >= result.resistance1)
        assertTrue("R1 >= PP must hold", result.resistance1 >= result.pivotPoint)
        assertTrue("PP >= S1 must hold", result.pivotPoint >= result.support1)
        assertTrue("S1 >= S2 must hold", result.support1 >= result.support2)

        // R1 must NOT be less than PP
        assertTrue("R1 must be >= PP after normalization", result.resistance1 >= result.pivotPoint)
    }

    @Test
    fun testTargetPercentagesAndDirectionConsistency() {
        val price = 100.0
        val bullishCandles = mutableListOf<Candle>()
        for (i in 0 until 35) {
            val p = 70.0 + i * 1.0 // Strong upward trend
            bullishCandles.add(Candle(i * 1000L, p - 0.5, p + 1.0, p - 1.0, p, 1000.0))
        }

        val buyResult = TechnicalAnalysisEngine.analyze("BTC", price, bullishCandles)
        if (buyResult.overallSignal == SignalType.BUY || buyResult.overallSignal == SignalType.STRONG_BUY) {
            // Upside targets: shortTerm > price and mediumTerm > shortTerm
            assertTrue("Buy short target must be above current price", buyResult.shortTermTarget > price)
            assertTrue("Buy med target must be above short target", buyResult.mediumTermTarget > buyResult.shortTermTarget)
            val shortPct = ((buyResult.shortTermTarget - price) / price) * 100.0
            val medPct = ((buyResult.mediumTermTarget - price) / price) * 100.0
            assertTrue("Buy short % must be positive", shortPct > 0.0)
            assertTrue("Buy med % must be positive", medPct > shortPct)
            // Stop loss must be below price
            assertTrue("Buy stop loss must be below current price", buyResult.stopLoss < price)
            val slPct = ((buyResult.stopLoss - price) / price) * 100.0
            assertTrue("Buy stop loss % must be negative", slPct < 0.0)
        }

        // Bearish candles test
        val bearishCandles = mutableListOf<Candle>()
        for (i in 0 until 35) {
            val p = 130.0 - i * 1.0 // Strong downward trend
            bearishCandles.add(Candle(i * 1000L, p + 0.5, p + 1.0, p - 1.0, p, 1000.0))
        }

        val sellResult = TechnicalAnalysisEngine.analyze("ETH", price, bearishCandles)
        if (sellResult.overallSignal == SignalType.SELL || sellResult.overallSignal == SignalType.STRONG_SELL) {
            // Downside targets: shortTerm < price and mediumTerm < shortTerm
            assertTrue("Sell short target must be below current price", sellResult.shortTermTarget < price)
            assertTrue("Sell med target must be below short target", sellResult.mediumTermTarget < sellResult.shortTermTarget)
            val shortPct = ((sellResult.shortTermTarget - price) / price) * 100.0
            val medPct = ((sellResult.mediumTermTarget - price) / price) * 100.0
            assertTrue("Sell short % must be negative", shortPct < 0.0)
            assertTrue("Sell med % must be negative", medPct < shortPct)
            // Short trade stop loss must be placed above price
            assertTrue("Sell stop loss must be above current price", sellResult.stopLoss > price)
            // Direction-appropriate short loss percentage
            val shortSlPct = - ((sellResult.stopLoss - price) / price) * 100.0
            assertTrue("Direction-appropriate short stop loss % must be negative", shortSlPct < 0.0)
        }
    }

    @Test
    fun testBollingerBandsCalculationsAndMeanReversionMessage() {
        val prices = List(25) { 100.0 } // Flat series
        val bb = TechnicalAnalysisEngine.calculateBollingerBands(prices, 20, 2.0)
        assertEquals("SMA of flat series must equal 100", 100.0, bb.middle, 0.001)
        assertEquals("Upper band of zero-variance series must equal 100", 100.0, bb.upper, 0.001)
        assertEquals("Lower band of zero-variance series must equal 100", 100.0, bb.lower, 0.001)

        // Varied series
        val varied = listOf(
            10.0, 11.0, 12.0, 11.5, 10.5, 9.5, 10.0, 10.8, 11.2, 12.1,
            12.5, 11.8, 10.9, 10.2, 9.8, 10.4, 11.0, 11.6, 12.0, 12.4
        )
        val bbVaried = TechnicalAnalysisEngine.calculateBollingerBands(varied, 20, 2.0)
        assertTrue("Upper > Middle", bbVaried.upper > bbVaried.middle)
        assertTrue("Middle > Lower", bbVaried.middle > bbVaried.lower)
        assertTrue("Lower must be >= 0", bbVaried.lower >= 0.0)

        // When price is comfortably within bands, verify message is neutral
        val normalCandles = varied.mapIndexed { idx, p ->
            Candle(idx * 1000L, p, p * 1.02, p * 0.98, p, 100.0)
        }
        val normalResult = TechnicalAnalysisEngine.analyze("SOL", bbVaried.middle, normalCandles)
        assertFalse(
            "Should not claim mean reversion when within envelope",
            normalResult.bbDescription.contains("pierced lower 2σ")
        )
    }

    @Test
    fun testRsiWildersSmoothing() {
        // Flat prices: RSI must be 50.0 (neutral), not 100.0
        val flat = List(20) { 100.0 }
        val rsiFlat = TechnicalAnalysisEngine.calculateRSI(flat, 14)
        assertEquals("RSI for flat prices must be 50.0", 50.0, rsiFlat, 0.01)

        // Monotonically increasing: RSI must be 100.0
        val rising = List(20) { 10.0 + it * 2.0 }
        val rsiRising = TechnicalAnalysisEngine.calculateRSI(rising, 14)
        assertEquals("RSI for purely rising prices must be 100.0", 100.0, rsiRising, 0.01)

        // Monotonically decreasing: RSI must be 0.0
        val falling = List(20) { 50.0 - it * 2.0 }
        val rsiFalling = TechnicalAnalysisEngine.calculateRSI(falling, 14)
        assertEquals("RSI for purely falling prices must be 0.0", 0.0, rsiFalling, 0.01)
    }

    @Test
    fun testZeroNegativeNanMalformedInputs() {
        val zeroResult = TechnicalAnalysisEngine.analyze("ZERO", 0.0, emptyList())
        assertEquals(SignalType.NEUTRAL, zeroResult.overallSignal)
        assertEquals(0, zeroResult.confidencePercent)
        assertTrue(zeroResult.pivotPoint.isNaN())

        val negResult = TechnicalAnalysisEngine.analyze("NEG", -15.0, emptyList())
        assertEquals(SignalType.NEUTRAL, negResult.overallSignal)
        assertTrue(negResult.shortTermTarget.isNaN())

        val nanResult = TechnicalAnalysisEngine.analyze("NAN", Double.NaN, emptyList())
        assertEquals(SignalType.NEUTRAL, nanResult.overallSignal)

        val infResult = TechnicalAnalysisEngine.analyze("INF", Double.POSITIVE_INFINITY, emptyList())
        assertEquals(SignalType.NEUTRAL, infResult.overallSignal)
    }

    @Test
    fun testPriceFormatterTiers() {
        assertEquals("$68,420.50", PriceFormatter.formatPrice(68420.50))
        assertEquals("$2,640.20", PriceFormatter.formatPrice(2640.20))
        assertEquals("$156.40", PriceFormatter.formatPrice(156.40))
        assertEquals("$0.5420", PriceFormatter.formatPrice(0.542))
        assertEquals("$0.1420", PriceFormatter.formatPrice(0.142))
        assertEquals("$0.03550", PriceFormatter.formatPrice(0.0355))
        assertEquals("$0.00000512", PriceFormatter.formatPrice(0.00000512))
        assertEquals("$0.00000095", PriceFormatter.formatPrice(0.00000095))
        assertEquals("$0.00", PriceFormatter.formatPrice(0.0))
        assertEquals("-$50.25", PriceFormatter.formatPrice(-50.25))
        assertEquals("$--", PriceFormatter.formatPrice(Double.NaN))

        assertEquals("+5.25%", PriceFormatter.formatPercentage(5.25))
        assertEquals("-3.50%", PriceFormatter.formatPercentage(-3.50))
        assertEquals("--%", PriceFormatter.formatPercentage(Double.NaN))
    }

    @Test
    fun testSyntheticDataProvenanceBlocksTradingSignals() {
        // Critical requirement: Real trading signals must never be generated from synthetic candles.
        val candles = mutableListOf<Candle>()
        for (i in 0 until 35) {
            val p = 100.0 + i * 2.0 // Strong upward trend
            candles.add(Candle(i * 1000L, p - 0.5, p + 1.0, p - 1.0, p, 1000.0))
        }

        // When analysis is marked as SYNTHETIC provenance
        val syntheticResult = TechnicalAnalysisEngine.analyze(
            symbol = "SYNTH",
            currentPrice = 170.0,
            candles = candles,
            provenance = MarketDataProvenance.SYNTHETIC,
            sourceExchange = "Deterministic Simulation"
        )

        assertEquals("Synthetic data must ALWAYS produce NEUTRAL overall signal", SignalType.NEUTRAL, syntheticResult.overallSignal)
        assertEquals("Confidence must be 0 for synthetic data", 0, syntheticResult.confidencePercent)
        assertTrue("Short-term target must be NaN for synthetic data", syntheticResult.shortTermTarget.isNaN())
        assertTrue("Medium-term target must be NaN for synthetic data", syntheticResult.mediumTermTarget.isNaN())
        assertTrue("Stop-loss must be NaN for synthetic data", syntheticResult.stopLoss.isNaN())
        assertTrue("Pivot point must be NaN for synthetic data", syntheticResult.pivotPoint.isNaN())
        assertTrue("R1 must be NaN for synthetic data", syntheticResult.resistance1.isNaN())
        assertTrue("S1 must be NaN for synthetic data", syntheticResult.support1.isNaN())
        assertEquals(SignalType.NEUTRAL, syntheticResult.rsiSignal)
        assertEquals(SignalType.NEUTRAL, syntheticResult.macdSignal)
        assertEquals(SignalType.NEUTRAL, syntheticResult.bbSignal)
        assertEquals(MarketDataProvenance.SYNTHETIC, syntheticResult.provenance)
        assertTrue(syntheticResult.dataSourceNotice?.contains("SIMULATION PREVIEW") == true)
    }

    @Test
    fun testEmptyCandlesReturnsUnavailableResult() {
        val emptyResult = TechnicalAnalysisEngine.analyze(
            symbol = "EMPTY",
            currentPrice = 50.0,
            candles = emptyList(),
            provenance = MarketDataProvenance.UNAVAILABLE
        )

        assertEquals(SignalType.NEUTRAL, emptyResult.overallSignal)
        assertEquals(0, emptyResult.confidencePercent)
        assertTrue(emptyResult.shortTermTarget.isNaN())
        assertTrue(emptyResult.pivotPoint.isNaN())
        assertEquals(MarketDataProvenance.UNAVAILABLE, emptyResult.provenance)
    }

    @Test
    fun testCachedExchangeProvenancePreservesVerifiedSignals() {
        val candles = mutableListOf<Candle>()
        for (i in 0 until 35) {
            val p = 70.0 + i * 1.0
            candles.add(Candle(i * 1000L, p - 0.5, p + 1.0, p - 1.0, p, 1000.0))
        }

        val cachedResult = TechnicalAnalysisEngine.analyze(
            symbol = "BTC",
            currentPrice = 105.0,
            candles = candles,
            provenance = MarketDataProvenance.CACHED_EXCHANGE,
            sourceExchange = "MEXC (Cached)"
        )

        assertEquals(MarketDataProvenance.CACHED_EXCHANGE, cachedResult.provenance)
        assertTrue(cachedResult.dataSourceNotice?.contains("Cached") == true)
        // Verified exchange data (even when cached) produces valid calculated pivot points
        assertFalse(cachedResult.pivotPoint.isNaN())
        assertTrue(cachedResult.pivotPoint > 0.0)
    }

    @Test
    fun testShibAstronomicalTargetBugPrevented() {
        val shibPrice = 0.00000512
        // Extreme volatile wick candles that previously produced 970,000,000% targets
        val candles = mutableListOf<Candle>()
        for (i in 0 until 35) {
            val p = shibPrice * (1.0 + (i * 0.01))
            candles.add(Candle(i * 3600000L, p, p * 1.5, p * 0.8, p, 10000000.0))
        }

        val result = TechnicalAnalysisEngine.analyze("SHIB", shibPrice, candles)

        // Verify target gain is bounded within realistic limits (max 25%), never 970M%
        val shortGainPct = ((result.shortTermTarget - shibPrice) / shibPrice) * 100.0
        val medGainPct = ((result.mediumTermTarget - shibPrice) / shibPrice) * 100.0

        assertTrue("SHIB short gain must not exceed 8%: $shortGainPct%", shortGainPct <= 8.5)
        assertTrue("SHIB medium gain must not exceed 25%: $medGainPct%", medGainPct <= 25.5)
        assertTrue("SHIB medium gain must be positive", medGainPct > 0.0)
        assertFalse("SHIB medium gain must never be astronomical (e.g. 970M%)", medGainPct > 100.0)

        // Verify Support 1 is >= Support 2
        assertTrue("Support 1 (${result.support1}) must be >= Support 2 (${result.support2})", result.support1 >= result.support2)
        assertTrue("Pivot (${result.pivotPoint}) must be >= Support 1 (${result.support1})", result.pivotPoint >= result.support1)
        assertTrue("Resistance 1 (${result.resistance1}) must be >= Pivot (${result.pivotPoint})", result.resistance1 >= result.pivotPoint)
        assertTrue("Resistance 2 (${result.resistance2}) must be >= Resistance 1 (${result.resistance1})", result.resistance2 >= result.resistance1)
    }

    @Test
    fun testUnavailableCandlesProducesExactPersianMessage() {
        val unavailableResult = TechnicalAnalysisEngine.analyze(
            symbol = "TEST",
            currentPrice = 100.0,
            candles = emptyList(),
            provenance = MarketDataProvenance.UNAVAILABLE
        )

        assertEquals("ارائه تحلیل در این زمان ممکن نیست", unavailableResult.dataSourceNotice)
        assertEquals("ارائه تحلیل در این زمان ممکن نیست", unavailableResult.rsiDescription)
        assertEquals(MarketDataProvenance.UNAVAILABLE, unavailableResult.provenance)
    }
}
