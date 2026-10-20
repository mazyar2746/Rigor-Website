package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {

    @Query("SELECT * FROM verified_transactions ORDER BY expiryTimestamp DESC LIMIT 1")
    fun getLatestSubscriptionFlow(): Flow<VerifiedTxEntity?>

    @Query("SELECT * FROM verified_transactions ORDER BY expiryTimestamp DESC LIMIT 1")
    suspend fun getLatestSubscription(): VerifiedTxEntity?

    @Query("SELECT * FROM verified_transactions WHERE LOWER(txHash) = LOWER(:txHash) LIMIT 1")
    suspend fun findByTxHash(txHash: String): VerifiedTxEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertVerifiedTx(tx: VerifiedTxEntity)

    @Query("DELETE FROM verified_transactions")
    suspend fun clearAll()
}

@Dao
interface FavoritesDao {

    @Query("SELECT symbol FROM favorite_coins")
    fun getFavoriteSymbols(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteCoinEntity)

    @Query("DELETE FROM favorite_coins WHERE symbol = :symbol")
    suspend fun removeFavorite(symbol: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_coins WHERE symbol = :symbol)")
    suspend fun isFavorite(symbol: String): Boolean
}

@Dao
interface CoinCacheDao {

    @Query("SELECT * FROM cached_coins ORDER BY rank ASC")
    suspend fun getAllCachedCoins(): List<CachedCoinEntity>

    @Query("SELECT * FROM cached_coins ORDER BY rank ASC")
    fun getAllCachedCoinsFlow(): Flow<List<CachedCoinEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCoins(coins: List<CachedCoinEntity>)

    @Query("SELECT COUNT(*) FROM cached_coins")
    suspend fun getCount(): Int

    @Query("DELETE FROM cached_coins")
    suspend fun clearCache()
}
