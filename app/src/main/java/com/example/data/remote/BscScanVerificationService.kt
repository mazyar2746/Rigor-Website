package com.example.data.remote

import android.util.Log
import com.example.data.model.SubscriptionPlan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.math.BigDecimal
import java.math.BigInteger
import java.util.concurrent.TimeUnit

sealed class VerificationResult {
    data class Success(
        val txHash: String,
        val plan: SubscriptionPlan,
        val amount: Double,
        val expiryTimestamp: Long,
        val isSandbox: Boolean = false
    ) : VerificationResult()

    data class Error(val message: String) : VerificationResult()
}

class BscScanVerificationService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {

    companion object {
        const val OFFICIAL_RECEIVING_ADDRESS = "0x736CC22F2b19adcAE6Ef4e5e13eab6BbF35B1Aab"
        const val OFFICIAL_USDT_BEP20_CONTRACT = "0x55d398326f99059ff775485246999027b3197955"
        // USDT on BSC has 18 decimals
        val WEI_DECIMALS = BigDecimal("1000000000000000000") // 10^18
    }

    suspend fun verifyTransaction(
        txHash: String,
        selectedPlan: SubscriptionPlan,
        customBscApiKey: String = "",
        isSandbox: Boolean = false
    ): VerificationResult = withContext(Dispatchers.IO) {
        val cleanHash = txHash.trim()

        if (cleanHash.isBlank()) {
            return@withContext VerificationResult.Error("Please enter a valid Transaction Hash (TxID).")
        }

        if (!cleanHash.startsWith("0x") || cleanHash.length != 66) {
            return@withContext VerificationResult.Error("Invalid TxID format. A BSC transaction hash must start with 0x and be 66 characters.")
        }

        // Handle Sandbox / Demo test mode
        if (isSandbox) {
            delay(1200) // Simulate fast network roundtrip
            val expiry = System.currentTimeMillis() + (selectedPlan.durationDays.toLong() * 24 * 3600 * 1000)
            return@withContext VerificationResult.Success(
                txHash = cleanHash,
                plan = selectedPlan,
                amount = selectedPlan.usdtAmount,
                expiryTimestamp = expiry,
                isSandbox = true
            )
        }

        try {
            // First attempt: Check via BscScan API if API key provided or public endpoint
            val bscScanUrl = if (customBscApiKey.isNotBlank()) {
                "https://api.bscscan.com/api?module=proxy&action=eth_getTransactionReceipt&txhash=$cleanHash&apikey=$customBscApiKey"
            } else {
                "https://api.bscscan.com/api?module=proxy&action=eth_getTransactionReceipt&txhash=$cleanHash"
            }

            val request = Request.Builder().url(bscScanUrl).build()
            val response = client.newCall(request).execute()

            var receiptObj: JSONObject? = null
            if (response.isSuccessful) {
                val bodyStr = response.body?.string() ?: ""
                val root = JSONObject(bodyStr)
                if (root.has("result") && !root.isNull("result")) {
                    receiptObj = root.optJSONObject("result")
                }
            }

            // Fallback to public BSC JSON-RPC if BscScan rate-limited or unavailable
            if (receiptObj == null) {
                receiptObj = fetchReceiptFromBscRpc(cleanHash)
            }

            if (receiptObj == null) {
                return@withContext VerificationResult.Error(
                    "Transaction not yet indexed on BNB Smart Chain. Please wait 1-2 minutes for block confirmation."
                )
            }

            // 1. Check status is Success ("0x1")
            val status = receiptObj.optString("status", "")
            if (status != "0x1") {
                return@withContext VerificationResult.Error("Transaction failed on-chain or reverted.")
            }

            // 2. Validate token transfer logs (USDT BEP20)
            val logs = receiptObj.optJSONArray("logs")
            var matchedTransfer = false
            var transferredAmount = 0.0

            val transferTopic = "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef"
            val paddedRecipient = "0x000000000000000000000000" + OFFICIAL_RECEIVING_ADDRESS.removePrefix("0x").lowercase()

            if (logs != null) {
                for (i in 0 until logs.length()) {
                    val log = logs.getJSONObject(i)
                    val contractAddress = log.optString("address", "").lowercase()
                    val topics = log.optJSONArray("topics")

                    if (topics != null && topics.length() >= 3) {
                        val eventSig = topics.getString(0).lowercase()
                        val toTopic = topics.getString(2).lowercase()

                        if (eventSig == transferTopic &&
                            toTopic == paddedRecipient &&
                            contractAddress == OFFICIAL_USDT_BEP20_CONTRACT.lowercase()
                        ) {
                            val data = log.optString("data", "0x0")
                            val cleanData = data.removePrefix("0x")
                            val rawBigInt = if (cleanData.isNotBlank()) BigInteger(cleanData, 16) else BigInteger.ZERO
                            val amountDecimal = BigDecimal(rawBigInt).divide(WEI_DECIMALS)
                            transferredAmount = amountDecimal.toDouble()

                            if (kotlin.math.abs(transferredAmount - selectedPlan.usdtAmount) <= 0.05) {
                                matchedTransfer = true
                                break
                            }
                        }
                    }
                }
            }

            // Also check direct native/token transfer match if recipient matches directly
            val txTo = receiptObj.optString("to", "").lowercase()
            if (!matchedTransfer && txTo == OFFICIAL_RECEIVING_ADDRESS.lowercase()) {
                matchedTransfer = true
                transferredAmount = selectedPlan.usdtAmount
            }

            if (!matchedTransfer) {
                return@withContext VerificationResult.Error(
                    "Could not confirm ${selectedPlan.usdtAmount} USDT BEP20 transfer to $OFFICIAL_RECEIVING_ADDRESS in this TxID. Please verify the amount and recipient."
                )
            }

            val expiry = System.currentTimeMillis() + (selectedPlan.durationDays.toLong() * 24 * 3600 * 1000)
            return@withContext VerificationResult.Success(
                txHash = cleanHash,
                plan = selectedPlan,
                amount = transferredAmount,
                expiryTimestamp = expiry,
                isSandbox = false
            )
        } catch (e: Exception) {
            Log.e("BscScan", "Error verifying transaction", e)
            return@withContext VerificationResult.Error("Network error verifying transaction: ${e.message}")
        }
    }

    private fun fetchReceiptFromBscRpc(txHash: String): JSONObject? {
        val rpcEndpoints = listOf(
            "https://bsc-dataseed1.binance.org",
            "https://bsc-dataseed2.binance.org",
            "https://binance.llamarpc.com"
        )

        for (endpoint in rpcEndpoints) {
            try {
                val jsonPayload = JSONObject().apply {
                    put("jsonrpc", "2.0")
                    put("method", "eth_getTransactionReceipt")
                    put("params", org.json.JSONArray().put(txHash))
                    put("id", 1)
                }.toString()

                val request = Request.Builder()
                    .url(endpoint)
                    .post(jsonPayload.toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val root = JSONObject(body)
                    if (root.has("result") && !root.isNull("result")) {
                        return root.getJSONObject("result")
                    }
                }
            } catch (e: Exception) {
                Log.w("BscScan", "RPC $endpoint failed, trying next", e)
            }
        }
        return null
    }
}
