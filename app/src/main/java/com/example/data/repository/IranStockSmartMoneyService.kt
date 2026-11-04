package com.example.data.repository

import android.util.Log
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

data class SmartMoneyData(
    val date: Long,
    val buyIndividualVolume: Double,
    val sellIndividualVolume: Double,
    val buyLegalVolume: Double,
    val sellLegalVolume: Double,
    val buyLegalCount: Double,
    val sellLegalCount: Double
) {
    val netLegalVolume: Double get() = buyLegalVolume - sellLegalVolume
    val legalDominance: Boolean get() = netLegalVolume > 0
}

class IranStockSmartMoneyService {
    private val TAG = "SmartMoneyService"

    suspend fun fetchSmartMoneyData(insCode: String): List<SmartMoneyData> = withContext(Dispatchers.IO) {
        try {
            val url = "https://cdn.tsetmc.com/api/ClientType/GetClientTypeHistory/$insCode"
            val jsonString = URL(url).readText()
            val root = JsonParser.parseString(jsonString).asJsonObject
            val clientTypeArray = root.getAsJsonArray("clientType") ?: return@withContext emptyList()

            val result = mutableListOf<SmartMoneyData>()
            for (i in 0 until minOf(clientTypeArray.size(), 30)) {
                val obj = clientTypeArray.get(i).asJsonObject
                result.add(
                    SmartMoneyData(
                        date = obj.get("recDate")?.asLong ?: 0,
                        buyIndividualVolume = obj.get("buy_I_Volume")?.asDouble ?: 0.0,
                        sellIndividualVolume = obj.get("sell_I_Volume")?.asDouble ?: 0.0,
                        buyLegalVolume = obj.get("buy_N_Volume")?.asDouble ?: 0.0,
                        sellLegalVolume = obj.get("sell_N_Volume")?.asDouble ?: 0.0,
                        buyLegalCount = obj.get("buy_N_Count")?.asDouble ?: 0.0,
                        sellLegalCount = obj.get("sell_N_Count")?.asDouble ?: 0.0
                    )
                )
            }
            return@withContext result
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}", e)
            return@withContext emptyList()
        }
    }

    fun analyzeSmartMoney(data: List<SmartMoneyData>): String? {
        if (data.size < 3) return null
        
        // بررسی ۳ روز اخیر
        val last3Days = data.take(3)
        val positiveDays = last3Days.count { it.legalDominance }
        val totalNetLegal = last3Days.sumOf { it.netLegalVolume }
        
        return when {
            positiveDays == 3 && totalNetLegal > 0 -> 
                "پول هوشمند (حقوقی‌ها) در ۳ روز اخیر به طور مداوم در حال خرید بوده‌اند. این یک سیگنال صعودی قوی است."
            positiveDays >= 2 -> 
                "حقوقی‌ها در ۲ روز از ۳ روز اخیر خریدار بوده‌اند. ورود پول هوشمند محتمل است."
            positiveDays == 0 -> 
                "حقوقی‌ها در ۳ روز اخیر مداوم فروشنده بوده‌اند. فشار فروش پول هوشمند وجود دارد."
            else -> null
        }
    }
}
