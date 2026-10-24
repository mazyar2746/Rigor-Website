package com.example.data.repository

import com.example.data.local.SubscriptionDao
import com.example.data.local.VerifiedTxEntity
import com.example.data.model.SubscriptionPlan
import com.example.data.model.UserSubscription
import com.example.data.remote.BscScanVerificationService
import com.example.data.remote.VerificationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SubscriptionRepository(
    private val subscriptionDao: SubscriptionDao,
    private val verificationService: BscScanVerificationService = BscScanVerificationService()
) {

    val currentSubscriptionFlow: Flow<UserSubscription> = subscriptionDao.getLatestSubscriptionFlow().map { entity ->
        if (entity == null) {
            UserSubscription(isActive = false, plan = null, expiryTimestamp = 0L, txHash = "")
        } else {
            val now = System.currentTimeMillis()
            val isActive = entity.expiryTimestamp > now
            val plan = if (entity.planId == SubscriptionPlan.YEARLY.id) SubscriptionPlan.YEARLY else SubscriptionPlan.MONTHLY
            UserSubscription(
                isActive = isActive,
                plan = if (isActive) plan else null,
                expiryTimestamp = entity.expiryTimestamp,
                txHash = entity.txHash,
                isSandbox = entity.isSandbox
            )
        }
    }

    suspend fun verifyAndActivate(
        txHash: String,
        plan: SubscriptionPlan,
        bscApiKey: String = "",
        isSandbox: Boolean = false
    ): Result<UserSubscription> {
        val cleanHash = txHash.trim()

        // Anti-Bypass Security: Prevent replay attacks
        val existing = subscriptionDao.findByTxHash(cleanHash)
        if (existing != null) {
            return Result.failure(
                IllegalStateException("Replay Attack Prevented: This Transaction Hash (TxID) has already been used to redeem a subscription.")
            )
        }

        // Verify with BscScan / on-chain
        return when (val result = verificationService.verifyTransaction(cleanHash, plan, bscApiKey, isSandbox)) {
            is VerificationResult.Success -> {
                val entity = VerifiedTxEntity(
                    txHash = result.txHash,
                    amountUsdt = result.amount,
                    planId = result.plan.id,
                    verifiedAt = System.currentTimeMillis(),
                    expiryTimestamp = result.expiryTimestamp,
                    isSandbox = result.isSandbox
                )
                subscriptionDao.insertVerifiedTx(entity)

                val userSub = UserSubscription(
                    isActive = true,
                    plan = result.plan,
                    expiryTimestamp = result.expiryTimestamp,
                    txHash = result.txHash,
                    isSandbox = result.isSandbox
                )
                Result.success(userSub)
            }
            is VerificationResult.Error -> {
                Result.failure(Exception(result.message))
            }
        }
    }

    suspend fun activateBazaarSubscription(
        purchaseToken: String,
        productId: String = "rigor_pro_subscription",
        durationDays: Int = 30
    ): Result<UserSubscription> {
        val cleanToken = purchaseToken.trim()
        val expiry = System.currentTimeMillis() + (durationDays * 24L * 60L * 60L * 1000L)
        val entity = VerifiedTxEntity(
            txHash = "bazaar_$cleanToken",
            amountUsdt = 9.99,
            planId = SubscriptionPlan.MONTHLY.id,
            verifiedAt = System.currentTimeMillis(),
            expiryTimestamp = expiry,
            isSandbox = false
        )
        subscriptionDao.insertVerifiedTx(entity)
        val userSub = UserSubscription(
            isActive = true,
            plan = SubscriptionPlan.MONTHLY,
            expiryTimestamp = expiry,
            txHash = "bazaar_$cleanToken",
            isSandbox = false
        )
        return Result.success(userSub)
    }

    suspend fun clearSubscription() {
        subscriptionDao.clearAll()
    }
}
