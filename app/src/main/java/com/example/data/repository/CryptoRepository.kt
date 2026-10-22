package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.CachedCoinEntity
import com.example.data.local.CoinCacheDao
import com.example.data.local.FavoriteCoinEntity
import com.example.data.local.FavoritesDao
import com.example.data.model.CandleResult
import com.example.data.model.CryptoCoin
import com.example.data.model.MarketDataProvenance
import com.example.data.model.TechnicalAnalysisResult
import com.example.data.remote.CryptoRemoteDataSource
import com.example.engine.TechnicalAnalysisEngine
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class CryptoRepository(
    private val favoritesDao: FavoritesDao,
    private val coinCacheDao: CoinCacheDao,
    val remoteDataSource: CryptoRemoteDataSource = CryptoRemoteDataSource(),
    private val context: Context? = null
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
                val models = cached.map { it.toModel() }
                _coins.value = models
                Log.d("CryptoRepo", "Loaded ${models.size} coins from local Room cache")
            } else if (context != null) {
                // Initialize from bundled catalog of 1,214 coins
                val bundled = remoteDataSource.loadBundledCatalog(context)
                if (bundled.isNotEmpty()) {
                    _coins.value = bundled
                    saveToCache(bundled)
                    Log.d("CryptoRepo", "Pre-populated Room with ${bundled.size} bundled tokens")
                }
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
            // First load from local DB or bundled assets if in-memory list is empty
            if (_coins.value.isEmpty()) {
                val cached = coinCacheDao.getAllCachedCoins()
                if (cached.isNotEmpty()) {
                    _coins.value = cached.map { it.toModel() }
                } else if (context != null) {
                    val bundled = remoteDataSource.loadBundledCatalog(context)
                    if (bundled.isNotEmpty()) {
                        _coins.value = bundled
                        saveToCache(bundled)
                    }
                }
            }

            // Step 1: CMC Listings (Top 100 coins)
            var cmcCoins: List<CryptoCoin>? = null
            if (cmcApiKey.isNotBlank()) {
                cmcCoins = remoteDataSource.fetchCmcListings(cmcApiKey, start = 1, limit = 100)
            }

            // Step 2: CoinGecko Page 1 (Top 250 coins)
            val geckoCoins = remoteDataSource.fetchCoinGeckoPage(page = 1, perPage = 250)

            // Step 3: Binance.US tickers (600+ real-time tickers)
            val binanceCoins = remoteDataSource.fetchBinanceUsAllTickers()

            // Step 4: MEXC real-time tickers for high-volume market depth (1,400+ coins)
            val mexcCoins = if ((binanceCoins?.size ?: 0) < 500) {
                remoteDataSource.fetchMexcAllTickers()
            } else null

            // Merge & Deduplicate all live sources
            val freshCoins = mergeCoinLists(
                primary = cmcCoins ?: emptyList(),
                secondary = geckoCoins ?: emptyList(),
                tertiary = (binanceCoins ?: emptyList()) + (mexcCoins ?: emptyList())
            )

            if (freshCoins.isNotEmpty()) {
                // Preserves full 1214+ roster, updates live prices and stats without wiping list!
                val updatedList = updateOrMergeCoins(_coins.value, freshCoins)
                _coins.value = updatedList
                _lastUpdated.value = System.currentTimeMillis()
                saveToCache(updatedList)
            } else if (_coins.value.isEmpty()) {
                val fallback = if (context != null) remoteDataSource.loadBundledCatalog(context) else remoteDataSource.getPresetFallbackCoins()
                _coins.value = fallback
            }
        } catch (e: Exception) {
            Log.w("CryptoRepo", "Refresh coins error: ${e.message}")
            if (_coins.value.isEmpty()) {
                val fallback = if (context != null) remoteDataSource.loadBundledCatalog(context) else remoteDataSource.getPresetFallbackCoins()
                _coins.value = fallback
            }
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun loadNextBatch(cmcApiKey: String = "") = withContext(Dispatchers.IO) {
        if (_isLoadingMore.value || !_hasMore.value || currentPage >= maxPages || _coins.value.size >= 1214) {
            _hasMore.value = false
            return@withContext
        }
        _isLoadingMore.value = true

        try {
            val nextPage = currentPage + 1
            Log.d("CryptoRepo", "Loading coin page $nextPage...")

            val newCoins = remoteDataSource.fetchCoinGeckoPage(page = nextPage, perPage = 250)
                ?: remoteDataSource.fetchMexcAllTickers()

            if (!newCoins.isNullOrEmpty()) {
                currentPage = nextPage
                val updated = updateOrMergeCoins(_coins.value, newCoins)
                _coins.value = updated
                saveToCache(updated)

                if (newCoins.size < 20 || currentPage >= maxPages || updated.size >= 1214) {
                    _hasMore.value = false
                }
            } else {
                _hasMore.value = false
            }
        } catch (e: Exception) {
            Log.w("CryptoRepo", "Load next batch error: ${e.message}")
            _hasMore.value = false
        } finally {
            _isLoadingMore.value = false
        }
    }

    private fun updateOrMergeCoins(
        currentList: List<CryptoCoin>,
        freshList: List<CryptoCoin>
    ): List<CryptoCoin> {
        if (currentList.isEmpty()) return freshList
        if (freshList.isEmpty()) return currentList

        val freshMap = freshList.associateBy { it.symbol.trim().uppercase() }
        val updatedCurrent = currentList.map { coin ->
            val fresh = freshMap[coin.symbol.trim().uppercase()]
            if (fresh != null) {
                coin.copy(
                    priceUsd = fresh.priceUsd,
                    change24h = fresh.change24h,
                    volume24h = if (fresh.volume24h > 0) fresh.volume24h else coin.volume24h,
                    marketCap = if (fresh.marketCap > 0) fresh.marketCap else coin.marketCap,
                    high24h = if (fresh.high24h > 0) fresh.high24h else coin.high24h,
                    low24h = if (fresh.low24h > 0) fresh.low24h else coin.low24h,
                    iconUrl = if (fresh.iconUrl.isNotEmpty()) fresh.iconUrl else coin.iconUrl
                )
            } else {
                coin
            }
        }

        val existingSymbols = currentList.map { it.symbol.trim().uppercase() }.toSet()
        val newAdditions = freshList.filter { it.symbol.trim().uppercase() !in existingSymbols }

        val combined = updatedCurrent + newAdditions
        return combined.mapIndexed { index, coin ->
            coin.copy(rank = index + 1)
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
            val key = coin.symbol.trim().uppercase()
            if (key.isNotEmpty()) {
                mergedMap[key] = coin.copy(symbol = key)
            }
        }

        // 2. Insert secondary (CoinGecko)
        for (coin in secondary) {
            val key = coin.symbol.trim().uppercase()
            if (key.isEmpty()) continue
            if (!mergedMap.containsKey(key)) {
                mergedMap[key] = coin.copy(symbol = key)
            } else {
                val existing = mergedMap[key]!!
                if (existing.iconUrl.isEmpty() && coin.iconUrl.isNotEmpty()) {
                    mergedMap[key] = existing.copy(iconUrl = coin.iconUrl)
                }
            }
        }

        // 3. Insert tertiary (Binance.US / MEXC)
        for (coin in tertiary) {
            val key = coin.symbol.trim().uppercase()
            if (key.isEmpty()) continue
            if (!mergedMap.containsKey(key)) {
                mergedMap[key] = coin.copy(symbol = key)
            }
        }

        return mergedMap.values.mapIndexed { index, coin ->
            coin.copy(rank = index + 1)
        }
    }

    private suspend fun saveToCache(coins: List<CryptoCoin>) {
        try {
            val entities = coins.take(2500).map { coin ->
                CachedCoinEntity(
                    symbol = coin.symbol.trim().uppercase(),
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

    // Thread-safe in-memory cache for verified exchange candles (retained for up to 30 minutes)
    private val candleCache = ConcurrentHashMap<String, Pair<Long, CandleResult>>()

    suspend fun getTechnicalAnalysis(coin: CryptoCoin, timeframe: String = "1h"): TechnicalAnalysisResult {
        val cacheKey = "${coin.symbol.trim().uppercase()}_$timeframe"
        val liveResult = remoteDataSource.fetchCandlesWithProvenance(
            symbol = coin.symbol,
            interval = timeframe,
            limit = 40,
            basePrice = coin.priceUsd
        )

        val resolvedCandleResult = if (liveResult.provenance == MarketDataProvenance.LIVE_EXCHANGE) {
            candleCache[cacheKey] = Pair(System.currentTimeMillis(), liveResult)
            liveResult
        } else {
            // Live exchange failed. Check if we have recent cached exchange candles (within 30 mins)
            val cached = candleCache[cacheKey]
            val maxCacheAgeMs = 30 * 60 * 1000L
            if (cached != null && (System.currentTimeMillis() - cached.first) < maxCacheAgeMs && cached.second.candles.isNotEmpty()) {
                CandleResult(
                    candles = cached.second.candles,
                    provenance = MarketDataProvenance.CACHED_EXCHANGE,
                    sourceExchange = "${cached.second.sourceExchange ?: "Exchange"} (Cached)"
                )
            } else {
                liveResult
            }
        }

        return TechnicalAnalysisEngine.analyze(
            symbol = coin.symbol,
            currentPrice = coin.priceUsd,
            candles = resolvedCandleResult.candles,
            provenance = resolvedCandleResult.provenance,
            sourceExchange = resolvedCandleResult.sourceExchange
        )
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
