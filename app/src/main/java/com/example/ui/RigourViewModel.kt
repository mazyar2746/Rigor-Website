package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.RigourDatabase
import com.example.data.model.CoinSignalData
import com.example.data.model.CryptoCoin
import com.example.data.model.MarketDataProvenance
import com.example.data.model.SubscriptionPlan
import com.example.data.model.TechnicalAnalysisResult
import com.example.data.model.UserSubscription
import com.example.data.repository.CryptoRepository
import com.example.data.repository.SubscriptionRepository
import com.example.i18n.Language
import com.example.i18n.StringsDictionary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class Screen {
    object Welcome : Screen()
    object Markets : Screen()
    object Signals : Screen()
    data class CoinDetail(val coin: CryptoCoin) : Screen()
    object Subscription : Screen()
    object ContactUs : Screen()
    object Settings : Screen()
}

class RigourViewModel(application: Application) : AndroidViewModel(application) {

    private val database = RigourDatabase.getInstance(application)
    private val cryptoRepository = CryptoRepository(database.favoritesDao(), database.coinCacheDao(), context = application)
    private val subscriptionRepository = SubscriptionRepository(database.subscriptionDao())

    // App Navigation
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Markets)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // i18n & RTL State
    private val _currentLanguage = MutableStateFlow(Language.ENGLISH)
    val currentLanguage: StateFlow<Language> = _currentLanguage.asStateFlow()

    // Search and Filter State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _onlyFavorites = MutableStateFlow(false)
    val onlyFavorites: StateFlow<Boolean> = _onlyFavorites.asStateFlow()

    // Crypto Market Data & Pagination
    val coins: StateFlow<List<CryptoCoin>> = cryptoRepository.coins
    val isMarketLoading: StateFlow<Boolean> = cryptoRepository.isLoading
    val isLoadingMore: StateFlow<Boolean> = cryptoRepository.isLoadingMore
    val hasMore: StateFlow<Boolean> = cryptoRepository.hasMore
    val lastUpdated: StateFlow<Long> = cryptoRepository.lastUpdated
    val favoriteSymbols: StateFlow<List<String>> = cryptoRepository.favoriteSymbols
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredCoins: StateFlow<List<CryptoCoin>> = combine(
        coins,
        _searchQuery,
        _onlyFavorites,
        favoriteSymbols
    ) { coinList, query, onlyFavs, favs ->
        var list = coinList
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter {
                it.name.lowercase().contains(q) || it.symbol.lowercase().contains(q)
            }
        }
        if (onlyFavs) {
            list = list.filter { favs.contains(it.symbol) }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Coin & Technical Analysis
    private val _selectedCoin = MutableStateFlow<CryptoCoin?>(null)
    val selectedCoin: StateFlow<CryptoCoin?> = _selectedCoin.asStateFlow()

    private val _currentTimeframe = MutableStateFlow("1h")
    val currentTimeframe: StateFlow<String> = _currentTimeframe.asStateFlow()

    private val _analysisResult = MutableStateFlow<TechnicalAnalysisResult?>(null)
    val analysisResult: StateFlow<TechnicalAnalysisResult?> = _analysisResult.asStateFlow()

    private val _isAnalysisLoading = MutableStateFlow(false)
    val isAnalysisLoading: StateFlow<Boolean> = _isAnalysisLoading.asStateFlow()

    // Dedicated AI Trading Signals Stream
    private val _signals = MutableStateFlow<List<CoinSignalData>>(emptyList())
    val signals: StateFlow<List<CoinSignalData>> = _signals.asStateFlow()

    private val _isSignalsLoading = MutableStateFlow(false)
    val isSignalsLoading: StateFlow<Boolean> = _isSignalsLoading.asStateFlow()

    // Subscription & Paywall
    val subscription: StateFlow<UserSubscription> = subscriptionRepository.currentSubscriptionFlow
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            UserSubscription(false, null, 0L, "")
        )

    private val _isVerifyingTx = MutableStateFlow(false)
    val isVerifyingTx: StateFlow<Boolean> = _isVerifyingTx.asStateFlow()

    private val _verificationMessage = MutableStateFlow<String?>(null)
    val verificationMessage: StateFlow<String?> = _verificationMessage.asStateFlow()

    private val _isVerificationSuccess = MutableStateFlow(false)
    val isVerificationSuccess: StateFlow<Boolean> = _isVerificationSuccess.asStateFlow()

    // API Keys & Settings
    private val _cmcApiKey = MutableStateFlow("")
    val cmcApiKey: StateFlow<String> = _cmcApiKey.asStateFlow()

    private val _bscApiKey = MutableStateFlow("")
    val bscApiKey: StateFlow<String> = _bscApiKey.asStateFlow()

    private val _isSandboxMode = MutableStateFlow(false)
    val isSandboxMode: StateFlow<Boolean> = _isSandboxMode.asStateFlow()

    private var refreshJob: Job? = null

    init {
        viewModelScope.launch {
            cryptoRepository.initializeFromCache()
            cryptoRepository.refreshCoins(_cmcApiKey.value)
            computeSignals()
        }
        startPeriodicRefresh()
    }

    fun loadMoreCoins() {
        viewModelScope.launch {
            cryptoRepository.loadNextBatch(_cmcApiKey.value)
        }
    }

    fun computeSignals() {
        viewModelScope.launch {
            _isSignalsLoading.value = true
            val currentCoins = coins.value.take(30)
            val list = mutableListOf<CoinSignalData>()
            for (coin in currentCoins) {
                try {
                    val analysis = cryptoRepository.getTechnicalAnalysis(coin, "1h")
                    // CRITICAL: Real trading signals must NEVER be generated from synthetic or unavailable candles!
                    if (analysis.provenance == MarketDataProvenance.LIVE_EXCHANGE || analysis.provenance == MarketDataProvenance.CACHED_EXCHANGE) {
                        list.add(CoinSignalData(coin, analysis))
                    } else {
                        android.util.Log.d("RigourVM", "Skipping ${coin.symbol} from signals feed due to synthetic/unavailable provenance (${analysis.provenance})")
                    }
                } catch (e: Exception) {
                    android.util.Log.w("RigourVM", "Failed to compute signals for ${coin.symbol}: ${e.message}")
                }
            }
            _signals.value = list
            _isSignalsLoading.value = false
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
        if (screen is Screen.Signals && _signals.value.isEmpty()) {
            computeSignals()
        }
    }

    fun selectCoin(coin: CryptoCoin) {
        _selectedCoin.value = coin
        _currentScreen.value = Screen.CoinDetail(coin)
        loadTechnicalAnalysis(coin, _currentTimeframe.value)
    }

    fun setTimeframe(timeframe: String) {
        _currentTimeframe.value = timeframe
        _selectedCoin.value?.let { coin ->
            loadTechnicalAnalysis(coin, timeframe)
        }
    }

    fun setLanguage(language: Language) {
        _currentLanguage.value = language
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleFavoritesOnly() {
        _onlyFavorites.value = !_onlyFavorites.value
    }

    fun toggleFavorite(symbol: String) {
        viewModelScope.launch {
            cryptoRepository.toggleFavorite(symbol)
        }
    }

    fun setCmcApiKey(key: String) {
        _cmcApiKey.value = key
        loadCoins()
    }

    fun setBscApiKey(key: String) {
        _bscApiKey.value = key
    }

    fun setSandboxMode(enabled: Boolean) {
        _isSandboxMode.value = enabled
    }

    fun loadCoins() {
        viewModelScope.launch {
            cryptoRepository.refreshCoins(_cmcApiKey.value)
        }
    }

    fun loadTechnicalAnalysis(coin: CryptoCoin, timeframe: String) {
        viewModelScope.launch {
            _isAnalysisLoading.value = true
            try {
                val result = cryptoRepository.getTechnicalAnalysis(coin, timeframe)
                _analysisResult.value = result
            } catch (e: Exception) {
                // Return explicitly marked unavailable result, NOT disguised signals
                _analysisResult.value = com.example.engine.TechnicalAnalysisEngine.createUnavailableResult(
                    symbol = coin.symbol,
                    currentPrice = coin.priceUsd,
                    candles = emptyList(),
                    provenance = MarketDataProvenance.UNAVAILABLE,
                    reason = "Market data unavailable: ${e.message}"
                )
            } finally {
                _isAnalysisLoading.value = false
            }
        }
    }

    fun verifySubscription(txHash: String, plan: SubscriptionPlan) {
        viewModelScope.launch {
            _isVerifyingTx.value = true
            _verificationMessage.value = null
            _isVerificationSuccess.value = false

            val result = subscriptionRepository.verifyAndActivate(
                txHash = txHash,
                plan = plan,
                bscApiKey = _bscApiKey.value,
                isSandbox = _isSandboxMode.value
            )

            _isVerifyingTx.value = false
            if (result.isSuccess) {
                _isVerificationSuccess.value = true
                _verificationMessage.value = StringsDictionary.get("verification_success", _currentLanguage.value)
            } else {
                _isVerificationSuccess.value = false
                _verificationMessage.value = result.exceptionOrNull()?.message
                    ?: StringsDictionary.get("verification_failed", _currentLanguage.value)
            }
        }
    }

    fun activateInstantSandboxAccess(plan: SubscriptionPlan) {
        val testHash = "0x" + System.currentTimeMillis().toString(16).padStart(64, 'a')
        viewModelScope.launch {
            _isVerifyingTx.value = true
            val result = subscriptionRepository.verifyAndActivate(
                txHash = testHash,
                plan = plan,
                isSandbox = true
            )
            _isVerifyingTx.value = false
            if (result.isSuccess) {
                _isVerificationSuccess.value = true
                _verificationMessage.value = "Sandbox access granted for ${plan.durationDays} days!"
            }
        }
    }

    fun clearVerificationStatus() {
        _verificationMessage.value = null
        _isVerificationSuccess.value = false
    }

    private fun startPeriodicRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (true) {
                delay(60_000) // 60 seconds data refresh
                cryptoRepository.refreshCoins(_cmcApiKey.value)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        refreshJob?.cancel()
    }
}
