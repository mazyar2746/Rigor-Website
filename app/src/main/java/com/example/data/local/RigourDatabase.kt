package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [VerifiedTxEntity::class, FavoriteCoinEntity::class, CachedCoinEntity::class],
    version = 2,
    exportSchema = false
)
abstract class RigourDatabase : RoomDatabase() {
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun favoritesDao(): FavoritesDao
    abstract fun coinCacheDao(): CoinCacheDao

    companion object {
        @Volatile
        private var INSTANCE: RigourDatabase? = null

        fun getInstance(context: Context): RigourDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RigourDatabase::class.java,
                    "rigor_crypto.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
