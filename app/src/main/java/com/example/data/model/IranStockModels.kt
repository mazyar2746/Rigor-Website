package com.example.data.model

import com.google.gson.annotations.SerializedName

data class StockInfo(
    @SerializedName("name") val name: String,
    @SerializedName("insCode") val insCode: String
)

data class DailyPrice(
    @SerializedName(value = "pClosing", alternate = ["priceClose"]) val priceClose: Double,
    @SerializedName("priceMin") val priceMin: Double,
    @SerializedName("priceMax") val priceMax: Double,
    @SerializedName("priceYesterday") val priceYesterday: Double,
    @SerializedName(value = "qTotTran5J", alternate = ["volume"]) val volume: Double,
    @SerializedName(value = "dEven", alternate = ["dateRaw"]) val dateRaw: Long,
    @SerializedName("pe") val pe: Double? = null,
    @SerializedName("eps") val eps: Double? = null
) {
    val dateString: String
        get() {
            val s = dateRaw.toString()
            return if (s.length == 8) {
                "${s.substring(0, 4)}/${s.substring(4, 6)}/${s.substring(6, 8)}"
            } else {
                s
            }
        }
}

data class StockMarketData(
    @SerializedName("insCode") val insCode: String,
    @SerializedName("prices") val prices: List<DailyPrice>,
    @SerializedName("lastUpdated") var lastUpdated: Long = 0L // زمان آخرین آپدیت (میلی‌ثانیه)
)
