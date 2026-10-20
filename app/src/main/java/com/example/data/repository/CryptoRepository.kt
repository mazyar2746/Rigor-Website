package com.example.data.repository

import com.example.data.local.FavoriteCoinEntity
import com.example.data.local.FavoritesDao
import com.example.data.model.CryptoCoin
import com.example.data.model.TechnicalAnalysisResult
import com.example.data.remote.CryptoRemoteDataSource
import com.example.engine.TechnicalAnalysisEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class CryptoRepository(
    private val favoritesDao: FavoritesDao,
    val remoteDataSource: CryptoRemoteDataSource = CryptoRemoteDataSource()
) {

    private val _coins = MutableStateFlow<List<CryptoCoin>>(emptyList())
    val coins = _coins.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _lastUpdated = MutableStateFlow(System.currentTimeMillis())
    val lastUpdated = _lastUpdated.asStateFlow()

    val favoriteSymbols: Flow<List<String>> = favoritesDao.getFavoriteSymbols()

    suspend fun refreshCoins(cmcApiKey: String = "") {
        _isLoading.value = true
        try {
            var fetched: List<CryptoCoin>? = null
            if (cmcApiKey.isNotBlank()) {
                fetched = remoteDataSource.fetchCmcListings(cmcApiKey)
            }
            if (fetched.isNullOrEmpty()) {
                fetched = remoteDataSource.fetchPublicMarketCoins()
            }
            _coins.value = fetched
            _lastUpdated.value = System.currentTimeMillis()
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun getTechnicalAnalysis(coin: CryptoCoin, timeframe: String = "1h"): TechnicalAnalysisResult {
        val candles = remoteDataSource.fetchCandles(coin.symbol, interval = timeframe, limit = 40)
        return TechnicalAnalysisEngine.analyze(coin.symbol, coin.priceUsd, candles)
    }

    suspend fun toggleFavorite(symbol: String) {
        val isFav = favoritesDao.isFavorite(symbol)
        if (isFav) {
            favoritesDao.removeFavorite(symbol)
        } else {
            favoritesDao.addFavorite(FavoriteCoinEntity(symbol))
        }
    }
}
