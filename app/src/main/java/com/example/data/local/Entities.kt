package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "verified_transactions")
data class VerifiedTxEntity(
    @PrimaryKey val txHash: String,
    val amountUsdt: Double,
    val planId: String,
    val verifiedAt: Long,
    val expiryTimestamp: Long,
    val isSandbox: Boolean
)

@Entity(tableName = "favorite_coins")
data class FavoriteCoinEntity(
    @PrimaryKey val symbol: String,
    val addedAt: Long = System.currentTimeMillis()
)
