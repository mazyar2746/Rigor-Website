package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.TechnicalAnalysisEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Rigour", appName)
    }

    @Test
    fun `technical analysis engine calculations`() {
        val prices = listOf(
            44.0, 44.5, 45.2, 45.8, 46.0, 45.5, 46.2, 46.8, 47.5, 48.0,
            47.8, 48.2, 48.9, 49.5, 50.1, 50.8, 51.2, 51.9, 52.5, 53.0
        )

        val rsi = TechnicalAnalysisEngine.calculateRSI(prices, period = 14)
        assertTrue("RSI should be between 0 and 100", rsi in 0.0..100.0)

        val bb = TechnicalAnalysisEngine.calculateBollingerBands(prices, period = 20, multiplier = 2.0)
        assertTrue("Upper band should be greater than middle", bb.upper >= bb.middle)
        assertTrue("Middle band should be greater than lower", bb.middle >= bb.lower)
    }
}
