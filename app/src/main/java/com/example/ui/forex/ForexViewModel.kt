package com.example.ui.forex

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.forex.*
import com.example.data.remote.TwelveDataRemoteDataSource
import com.example.engine.TechnicalAnalysisEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class ForexRow(
    val pair: ForexPair,
    val price: String,
    val percentChange: Double,
    val signal: String,
    val confidence: Int,
    val entry: String,
    val target: String,
    val stopLoss: String,
    val resistance1: String,
    val support1: String,
    val analysisDescription: String
)

data class ForexUiState(
    val isLoading: Boolean = false,
    val rows: List<ForexRow> = emptyList(),
    val errorMessage: String? = null
)

class ForexViewModel(
    private val repository: ForexRepository = TwelveDataRemoteDataSource()
) : ViewModel() {

    var searchQuery by mutableStateOf("")
        private set
    var interval by mutableStateOf(ForexInterval.H1)
        private set
    var majorsOnly by mutableStateOf(false)
        private set

    private val _uiState = MutableStateFlow(ForexUiState())
    val uiState: StateFlow<ForexUiState> = _uiState.asStateFlow()

    private val majorPairs = listOf("EUR/USD", "GBP/USD", "USD/JPY", "AUD/USD", "USD/CAD", "USD/CHF")
    private var isStarted = false

    fun startIfNeeded() {
        if (!isStarted) {
            isStarted = true
            refresh(true)
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
        refresh()
    }

    fun updateInterval(newInterval: ForexInterval) {
        interval = newInterval
        refresh(true)
    }

    fun toggleMajorsFilter() {
        majorsOnly = !majorsOnly
        refresh()
    }

    fun refresh(force: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            val result = repository.getForexPairs()
            
            if (result is ForexResult.Success) {
                var filtered = result.data
                if (majorsOnly) filtered = filtered.filter { it.symbol in majorPairs }
                if (searchQuery.isNotBlank()) filtered = filtered.filter { it.symbol.contains(searchQuery, ignoreCase = true) }

                val newRows = mutableListOf<ForexRow>()
                for (pair in filtered) {
                    val timeSeriesResult = repository.getTimeSeries(pair.symbol, interval)
                    
                    if (timeSeriesResult is ForexResult.Failure) {
                        try {
                            val logFile = File("/storage/emulated/0/Download/forex_error.txt")
                            logFile.appendText("Failed for ${pair.symbol}: ${timeSeriesResult.error}\n")
                        } catch (e: Exception) { }
                    }
                    
                    if (timeSeriesResult is ForexResult.Success) {
                        val timeSeries = timeSeriesResult.data
                        val lastCandle = timeSeries.candles.lastOrNull()
                        val currentPrice = lastCandle?.close ?: 0.0
                        
                        val analysis = TechnicalAnalysisEngine.analyze(
                            symbol = pair.symbol,
                            currentPrice = currentPrice,
                            candles = timeSeries.candles
                        )
                        
                        val change = timeSeries.candles.firstOrNull()?.let { first ->
                            ((currentPrice - first.close) / first.close) * 100
                        } ?: 0.0

                        newRows.add(ForexRow(
                            pair = pair,
                            price = String.format("%.5f", currentPrice),
                            percentChange = change,
                            signal = analysis.overallSignal.name,
                            confidence = analysis.confidencePercent,
                            entry = String.format("%.5f", analysis.currentPrice),
                            target = String.format("%.5f", analysis.shortTermTarget),
                            stopLoss = String.format("%.5f", analysis.stopLoss),
                            resistance1 = String.format("%.5f", analysis.resistance1),
                            support1 = String.format("%.5f", analysis.support1),
                            analysisDescription = analysis.rsiDescription
                        ))
                    }
                    _uiState.value = _uiState.value.copy(rows = newRows.toList())
                }
                _uiState.value = _uiState.value.copy(isLoading = false, rows = newRows)
            } else if (result is ForexResult.Failure) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.error.toString())
            }
        }
    }
}
