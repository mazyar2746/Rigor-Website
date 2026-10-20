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

@Entity(tableName = "cached_coins")
data class CachedCoinEntity(
    @PrimaryKey val symbol: String,
    val id: String,
    val name: String,
    val priceUsd: Double,
    val change24h: Double,
    val volume24h: Double,
    val marketCap: Double,
    val high24h: Double,
    val low24h: Double,
    val rank: Int,
    val iconUrl: String,
    val cachedAt: Long = System.currentTimeMillis()
)
