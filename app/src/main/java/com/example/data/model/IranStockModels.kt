package com.example.data.model

import com.google.gson.annotations.SerializedName

data class StockInfo(
    @SerializedName("name") val name: String,
    @SerializedName("insCode") val insCode: String
)

data class DailyPrice(
    @SerializedName("pClosing") val priceClose: Double,
    @SerializedName("priceMin") val priceMin: Double,
    @SerializedName("priceMax") val priceMax: Double,
    @SerializedName("priceYesterday") val priceYesterday: Double,
    @SerializedName("qTotTran5J") val volume: Long,
    @SerializedName("dEven") val dateRaw: Long
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
    @SerializedName("prices") val prices: List<DailyPrice>
)
