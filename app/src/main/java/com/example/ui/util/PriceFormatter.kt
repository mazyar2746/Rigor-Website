package com.example.ui.util

import java.util.Locale
import kotlin.math.abs

/**
 * High-precision, locale-safe cryptocurrency price and percentage formatter.
 * Handles tokens across all price tiers: from high-value assets (BTC, ETH)
 * to micro-cap and meme tokens (SHIB, PEPE) down to 8 decimal places.
 */
object PriceFormatter {

    /**
     * Formats a USD/USDT cryptocurrency price with dynamic precision.
     * Prevents truncation to $0.00 for small tokens.
     */
    fun formatPrice(price: Double): String {
        if (price.isNaN() || price.isInfinite()) return "$--"
        val absVal = abs(price)
        val prefix = if (price < 0) "-$" else "$"

        val formattedNumber = when {
            absVal == 0.0 -> "0.00"
            absVal >= 1000.0 -> String.format(Locale.US, "%,.2f", absVal)
            absVal >= 1.0 -> String.format(Locale.US, "%,.2f", absVal)
            absVal >= 0.1 -> String.format(Locale.US, "%.4f", absVal)
            absVal >= 0.001 -> String.format(Locale.US, "%.5f", absVal)
            absVal >= 0.00001 -> String.format(Locale.US, "%.7f", absVal)
            else -> String.format(Locale.US, "%.8f", absVal)
        }
        return "$prefix$formattedNumber"
    }

    /**
     * Formats price without the leading dollar sign.
     */
    fun formatPriceWithoutSymbol(price: Double): String {
        val formatted = formatPrice(price)
        return formatted.replace("$", "")
    }

    /**
     * Formats percentage with sign and 2 decimal places.
     */
    fun formatPercentage(percent: Double, showSign: Boolean = true): String {
        if (percent.isNaN() || percent.isInfinite()) return "--%"
        val prefix = if (showSign && percent > 0) "+" else ""
        return String.format(Locale.US, "%s%.2f%%", prefix, percent)
    }

    /**
     * Formats technical indicator values (e.g. MACD, Histogram) adapting to the price scale.
     */
    fun formatIndicator(value: Double, referencePrice: Double = 1.0): String {
        if (value.isNaN() || value.isInfinite()) return "--"
        val absVal = abs(value)
        val sign = if (value > 0) "+" else if (value < 0) "-" else ""
        val ref = abs(referencePrice).coerceAtLeast(1e-8)

        return when {
            ref >= 1.0 -> String.format(Locale.US, "%s%.3f", sign, absVal)
            ref >= 0.01 -> String.format(Locale.US, "%s%.5f", sign, absVal)
            else -> String.format(Locale.US, "%s%.8f", sign, absVal)
        }
    }
}
