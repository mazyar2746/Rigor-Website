package com.example.data.repository

import android.util.Log
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

data class MarketIndex(
    val indexLastValue: Double,
    val indexChange: Double,
    val indexEqualWeighted: Double,
    val marketState: String
)

class MarketIndexService {
    private val TAG = "MarketIndexService"

    suspend fun fetchMarketIndex(): MarketIndex? = withContext(Dispatchers.IO) {
        try {
            val url = "https://cdn.tsetmc.com/api/MarketData/GetMarketOverview/1"
            val json = URL(url).readText()
            val root = JsonParser.parseString(json).asJsonObject
            val overview = root.getAsJsonObject("marketOverview") ?: return@withContext null

            return@withContext MarketIndex(
                indexLastValue = overview.get("indexLastValue")?.asDouble ?: 0.0,
                indexChange = overview.get("indexChange")?.asDouble ?: 0.0,
                indexEqualWeighted = overview.get("indexEqualWeightedLastValue")?.asDouble ?: 0.0,
                marketState = overview.get("marketStateTitle")?.asString ?: ""
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}", e)
            return@withContext null
        }
    }
}
