package com.example.data.repository

import android.util.Log
import com.example.data.local.CachedCoinEntity
import com.example.data.local.CoinCacheDao
import com.example.data.local.FavoriteCoinEntity
import com.example.data.local.FavoritesDao
import com.example.data.model.CryptoCoin
import com.example.data.model.TechnicalAnalysisResult
import com.example.data.remote.CryptoRemoteDataSource
import com.example.engine.TechnicalAnalysisEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class CryptoRepository(
    private val favoritesDao: FavoritesDao,
    private val coinCacheDao: CoinCacheDao,
    val remoteDataSource: CryptoRemoteDataSource = CryptoRemoteDataSource()
) {

    private val _coins = MutableStateFlow<List<CryptoCoin>>(emptyList())
    val coins = _coins.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore = _isLoadingMore.asStateFlow()

    private val _hasMore = MutableStateFlow(true)
    val hasMore = _hasMore.asStateFlow()

    private val _lastUpdated = MutableStateFlow(System.currentTimeMillis())
    val lastUpdated = _lastUpdated.asStateFlow()

    val favoriteSymbols: Flow<List<String>> = favoritesDao.getFavoriteSymbols()

    private var currentPage = 1
    private val maxPages = 5 // 4-5 pages gives 1000+ coins

    suspend fun initializeFromCache() = withContext(Dispatchers.IO) {
        try {
            val cached = coinCacheDao.getAllCachedCoins()
            if (cached.isNotEmpty()) {
                _coins.value = cached.map { it.toModel() }
            }
        } catch (e: Exception) {
            Log.d("CryptoRepo", "Cache read failed: ${e.message}")
        }
    }

    suspend fun refreshCoins(cmcApiKey: String = "") = withContext(Dispatchers.IO) {
        _isLoading.value = true
        currentPage = 1
        _hasMore.value = true

        try {
            // First load from local DB if in-memory list is empty
            if (_coins.value.isEmpty()) {
                val cached = coinCacheDao.getAllCachedCoins()
                if (cached.isNotEmpty()) {
                    _coins.value = cached.map { it.toModel() }
                }
            }

            // Step 1: CMC Listings (Top 100 coins)
            var cmcCoins: List<CryptoCoin>? = null
            if (cmcApiKey.isNotBlank()) {
                cmcCoins = remoteDataSource.fetchCmcListings(cmcApiKey, start = 1, limit = 100)
            }

            // Step 2: CoinGecko Page 1 (Top 250 coins)
            val geckoCoins = remoteDataSource.fetchCoinGeckoPage(page = 1, perPage = 250)

            // Step 3: Binance.US tickers as backup if both are unavailable
            val binanceCoins = if (cmcCoins.isNullOrEmpty() && geckoCoins.isNullOrEmpty()) {
                remoteDataSource.fetchBinanceUsAllTickers()
            } else null

            // Merge & Deduplicate
            val mergedList = mergeCoinLists(
                primary = cmcCoins ?: emptyList(),
                secondary = geckoCoins ?: emptyList(),
                tertiary = binanceCoins ?: emptyList()
            )

            if (mergedList.isNotEmpty()) {
                _coins.value = mergedList
                _lastUpdated.value = System.currentTimeMillis()
                saveToCache(mergedList)
            } else if (_coins.value.isEmpty()) {
                val fallback = remoteDataSource.getPresetFallbackCoins()
                _coins.value = fallback
            }
        } catch (e: Exception) {
            Log.w("CryptoRepo", "Refresh coins error: ${e.message}")
            if (_coins.value.isEmpty()) {
                _coins.value = remoteDataSource.getPresetFallbackCoins()
            }
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun loadNextBatch(cmcApiKey: String = "") = withContext(Dispatchers.IO) {
        if (_isLoadingMore.value || !_hasMore.value || currentPage >= maxPages) return@withContext
        _isLoadingMore.value = true

        try {
            val nextPage = currentPage + 1
            Log.d("CryptoRepo", "Loading coin page $nextPage...")

            val newCoins = remoteDataSource.fetchCoinGeckoPage(page = nextPage, perPage = 250)
                ?: if (nextPage == 2) remoteDataSource.fetchBinanceUsAllTickers() else null

            if (!newCoins.isNullOrEmpty()) {
                currentPage = nextPage
                val existingMap = _coins.value.associateBy { it.symbol.uppercase() }.toMutableMap()

                for (coin in newCoins) {
                    val key = coin.symbol.uppercase()
                    if (!existingMap.containsKey(key)) {
                        existingMap[key] = coin
                    }
                }

                // Re-rank sequentially
                val updatedList = existingMap.values.sortedBy { it.rank }.mapIndexed { index, coin ->
                    coin.copy(rank = index + 1)
                }

                _coins.value = updatedList
                saveToCache(updatedList)

                if (newCoins.size < 20 || currentPage >= maxPages) {
                    _hasMore.value = false
                }
            } else {
                _hasMore.value = false
            }
        } catch (e: Exception) {
            Log.w("CryptoRepo", "Load next batch error: ${e.message}")
        } finally {
            _isLoadingMore.value = false
        }
    }

    private fun mergeCoinLists(
        primary: List<CryptoCoin>,
        secondary: List<CryptoCoin>,
        tertiary: List<CryptoCoin>
    ): List<CryptoCoin> {
        val mergedMap = LinkedHashMap<String, CryptoCoin>()

        // 1. Insert primary (e.g. CMC)
        for (coin in primary) {
            mergedMap[coin.symbol.uppercase()] = coin
        }

        // 2. Insert secondary (CoinGecko page 1)
        for (coin in secondary) {
            val key = coin.symbol.uppercase()
            if (!mergedMap.containsKey(key)) {
                mergedMap[key] = coin
            } else {
                // If primary lacked icon, take it from secondary
                val existing = mergedMap[key]!!
                if (existing.iconUrl.isEmpty() && coin.iconUrl.isNotEmpty()) {
                    mergedMap[key] = existing.copy(iconUrl = coin.iconUrl)
                }
            }
        }

        // 3. Insert tertiary (Binance.US)
        for (coin in tertiary) {
            val key = coin.symbol.uppercase()
            if (!mergedMap.containsKey(key)) {
                mergedMap[key] = coin
            }
        }

        return mergedMap.values.mapIndexed { index, coin ->
            coin.copy(rank = index + 1)
        }
    }

    private suspend fun saveToCache(coins: List<CryptoCoin>) {
        try {
            val entities = coins.take(1000).map { coin ->
                CachedCoinEntity(
                    symbol = coin.symbol,
                    id = coin.id,
                    name = coin.name,
                    priceUsd = coin.priceUsd,
                    change24h = coin.change24h,
                    volume24h = coin.volume24h,
                    marketCap = coin.marketCap,
                    high24h = coin.high24h,
                    low24h = coin.low24h,
                    rank = coin.rank,
                    iconUrl = coin.iconUrl
                )
            }
            coinCacheDao.insertOrUpdateCoins(entities)
        } catch (e: Exception) {
            Log.d("CryptoRepo", "Error caching coins: ${e.message}")
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

    private fun CachedCoinEntity.toModel(): CryptoCoin {
        return CryptoCoin(
            id = id,
            name = name,
            symbol = symbol,
            priceUsd = priceUsd,
            change24h = change24h,
            volume24h = volume24h,
            marketCap = marketCap,
            high24h = high24h,
            low24h = low24h,
            rank = rank,
            iconUrl = iconUrl
        )
    }
}
