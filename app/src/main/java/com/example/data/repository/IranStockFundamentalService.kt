package com.example.data.repository

import android.util.Log
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

data class FundamentalData(
    val estimatedEPS: Double?,
    val sectorPE: Double?,
    val sectorName: String?,
    val totalShares: Double?,
    val weekHigh: Double?,
    val weekLow: Double?,
    val yearHigh: Double?,
    val yearLow: Double?
)

class IranStockFundamentalService {
    private val TAG = "FundamentalService"

    suspend fun fetchFundamentalData(insCode: String): FundamentalData? = withContext(Dispatchers.IO) {
        try {
            val url = "https://cdn.tsetmc.com/api/Instrument/GetInstrumentInfo/$insCode"
            val jsonString = URL(url).readText()
            val root = JsonParser.parseString(jsonString).asJsonObject

            val info = root.getAsJsonObject("instrumentInfo") ?: return@withContext null

            val epsObj = info.getAsJsonObject("eps")
            val sectorObj = info.getAsJsonObject("sector")

            return@withContext FundamentalData(
                estimatedEPS = epsObj?.get("estimatedEPS")?.asString?.toDoubleOrNull(),
                sectorPE = epsObj?.get("sectorPE")?.asDouble,
                sectorName = sectorObj?.get("lSecVal")?.asString,
                totalShares = info.get("zTitad")?.asDouble,
                weekHigh = info.get("maxWeek")?.asDouble,
                weekLow = info.get("minWeek")?.asDouble,
                yearHigh = info.get("maxYear")?.asDouble,
                yearLow = info.get("minYear")?.asDouble
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching fundamental data for $insCode: ${e.message}", e)
            return@withContext null
        }
    }
}
