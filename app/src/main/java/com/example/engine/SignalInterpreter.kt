package com.example.engine

import com.example.data.model.SignalType
import com.example.data.model.TechnicalAnalysisResult
import com.example.i18n.Language
import com.example.i18n.StringsDictionary

object SignalInterpreter {

    /**
     * سیگنال نهایی را با در نظر گرفتن تحلیل تکنیکال و پول هوشمند محاسبه می‌کند.
     * @param technical سیگنال خام تکنیکال
     * @param smartMoneyText متن تحلیل پول هوشمند (اگر خالی یا null باشد، فقط تکنیکال در نظر گرفته می‌شود)
     */
    fun getFinalSignal(technical: SignalType, smartMoneyText: String?): SignalType {
        // اگر پول هوشمند در دسترس نبود، فقط به تکنیکال اعتماد کن
        if (smartMoneyText == null) return technical

        val isLegalBuying = smartMoneyText.contains("خریدار") || smartMoneyText.contains("ورود پول هوشمند")
        val isLegalSelling = smartMoneyText.contains("فروشنده") || smartMoneyText.contains("خروج")

        return when (technical) {
            SignalType.STRONG_BUY -> {
                if (isLegalSelling) SignalType.BUY // کاهش قدرت خرید به دلیل خروج پول هوشمند
                else SignalType.STRONG_BUY
            }
            SignalType.BUY -> {
                if (isLegalSelling) SignalType.NEUTRAL // سیگنال خنثی چون پول هوشمند همراه نیست
                else SignalType.BUY
            }
            SignalType.STRONG_SELL -> {
                if (isLegalBuying) SignalType.SELL // کاهش قدرت فروش به دلیل ورود پول هوشمند
                else SignalType.STRONG_SELL
            }
            SignalType.SELL -> {
                if (isLegalBuying) SignalType.NEUTRAL // سیگنال خنثی چون پول هوشمند در حال خرید است
                else SignalType.SELL
            }
            SignalType.NEUTRAL -> {
                if (isLegalBuying) SignalType.BUY // تقویت سیگنال خنثی به خرید
                else if (isLegalSelling) SignalType.SELL // تقویت سیگنال خنثی به فروش
                else SignalType.NEUTRAL
            }
        }
    }

    fun getSignalLabel(signal: SignalType, lang: Language): String {
        val key = when (signal) {
            SignalType.STRONG_BUY -> "iran_stock_strong_buy"
            SignalType.BUY -> "iran_stock_buy"
            SignalType.NEUTRAL -> "iran_stock_neutral"
            SignalType.SELL -> "iran_stock_sell"
            SignalType.STRONG_SELL -> "iran_stock_strong_sell"
        }
        return StringsDictionary.get(key, lang)
    }

    fun getReasoningText(technical: TechnicalAnalysisResult, smartMoneyText: String?, lang: Language): String {
        val isFa = lang == Language.PERSIAN
        val reasons = mutableListOf<String>()

        if (isFa) {
            // منطق تکنیکال
            when {
                technical.rsi < 30 -> reasons.add("شاخص RSI در ناحیه اشباع فروش (زیر ۳۰) قرار دارد.")
                technical.rsi > 70 -> reasons.add("شاخص RSI در ناحیه اشباع خرید (بالای ۷۰) است.")
                else -> reasons.add("شاخص RSI در محدوده خنثی قرار دارد.")
            }
            if (technical.macdHistogram > 0) reasons.add("هیستوگرام MACD مثبت است (مومنتوم صعودی).")
            else reasons.add("هیستوگرام MACD منفی است (فشار فروش).")

            // منطق پول هوشمند (اگر موجود باشد)
            if (smartMoneyText != null) {
                reasons.add(smartMoneyText)
            }
        } else {
            when {
                technical.rsi < 30 -> reasons.add("RSI is in the oversold zone (below 30).")
                technical.rsi > 70 -> reasons.add("RSI is in the overbought zone (above 70).")
                else -> reasons.add("RSI is in the neutral zone.")
            }
            if (technical.macdHistogram > 0) reasons.add("MACD histogram is positive (bullish momentum).")
            else reasons.add("MACD histogram is negative (selling pressure).")

            if (smartMoneyText != null) {
                reasons.add(smartMoneyText) // در نسخه فعلی، متن پول هوشمند فارسی است. برای انگلیسی نیاز به ترجمه دارد.
            }
        }

        // جمع‌بندی نهایی
        val finalSignal = getFinalSignal(getFinalSignal(technical.rsiSignal, null), smartMoneyText)
        val conclusion = if (isFa) {
            when (finalSignal) {
                SignalType.STRONG_BUY -> "نتیجه‌گیری: هم‌جهتی سیگنال تکنیکال و پول هوشمند، شرایط بسیار مناسبی برای خرید فراهم کرده است."
                SignalType.BUY -> "نتیجه‌گیری: شرایط کلی برای خرید مساعد است، اما با احتیاط وارد شوید."
                SignalType.STRONG_SELL -> "نتیجه‌گیری: فشار فروش و خروج پول هوشمند، احتمال نزول بیشتر را نشان می‌دهد."
                SignalType.SELL -> "نتیجه‌گیری: احتمال نزول قیمت وجود دارد. احتیاط کنید."
                SignalType.NEUTRAL -> "نتیجه‌گیری: سیگنال‌ها هم‌جهت نیستند (تناقض بین تکنیکال و پول هوشمند). بهتر است صبر کنید."
            }
        } else {
            when (finalSignal) {
                SignalType.STRONG_BUY -> "Conclusion: Technical and Smart Money signals align for a strong buy."
                SignalType.BUY -> "Conclusion: Overall conditions favor buying, but enter with caution."
                SignalType.STRONG_SELL -> "Conclusion: Selling pressure and Smart Money outflow suggest further downside."
                SignalType.SELL -> "Conclusion: Further downside is likely. Exercise caution."
                SignalType.NEUTRAL -> "Conclusion: Signals are conflicting (Technical vs Smart Money). Wait for clarity."
            }
        }

        return reasons.joinToString("\n\n") + "\n\n📌 " + conclusion
    }
}
