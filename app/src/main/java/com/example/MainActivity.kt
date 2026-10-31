package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.billing.BillingManager
import com.example.i18n.Language
import com.example.i18n.StringsDictionary
import com.example.ui.RigourViewModel
import com.example.ui.Screen
import com.example.ui.components.LanguageSelectorDialog
import com.example.ui.screens.CoinDetailScreen
import com.example.ui.screens.ContactUsScreen
import com.example.ui.screens.MarketDashboardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WelcomeScreen
import com.example.ui.screens.SignalsScreen
import com.example.ui.screens.SubscriptionScreen
import com.example.ui.theme.RigourBackground
import com.example.ui.theme.RigourBullish
import com.example.ui.theme.RigourCardBorder
import com.example.ui.theme.RigourCyan
import com.example.ui.theme.RigourGold
import com.example.ui.theme.RigourSurface
import com.example.ui.theme.RigourTheme

class MainActivity : ComponentActivity() {

    private val viewModel: RigourViewModel by viewModels()
    private var billingManager: BillingManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            val manager = BillingManager(this)
            billingManager = manager
            manager.connect(
                onConnected = {
                    manager.checkActiveSubscriptions { hasActiveSub, purchaseInfo ->
                        if (hasActiveSub && purchaseInfo != null) {
                            viewModel.activateBazaarSubscription(purchaseInfo.purchaseToken)
                        }
                    }
                },
                onFailed = { throwable ->
                    android.util.Log.w("MainActivity", "Cafe Bazaar billing connection: ${throwable.message}")
                }
            )
        } catch (t: Throwable) {
            android.util.Log.e("MainActivity", "Error setting up BillingManager", t)
        }

        setContent {
            RigourTheme {
                RigourApp(
                    viewModel = viewModel,
                    onBazaarSubscribe = {
                        val manager = billingManager
                        if (manager != null) {
                            manager.subscribe(
                                registry = activityResultRegistry,
                                sku = BillingManager.SKU_RIGOR_PRO,
                                onSuccess = { purchaseInfo ->
                                    viewModel.activateBazaarSubscription(purchaseInfo.purchaseToken)
                                },
                                onCanceled = {
                                    viewModel.onBazaarPurchaseCanceled()
                                },
                                onError = { error ->
                                    viewModel.onBazaarPurchaseError(error)
                                }
                            )
                        } else {
                            viewModel.onBazaarPurchaseError("Cafe Bazaar billing is unavailable on this device.")
                        }
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            billingManager?.release()
            billingManager = null
        } catch (t: Throwable) {
            android.util.Log.w("MainActivity", "Error releasing billingManager", t)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RigourApp(
    viewModel: RigourViewModel,
    onBazaarSubscribe: () -> Unit = {}
) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val currentLanguage by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val filteredCoins by viewModel.filteredCoins.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val onlyFavorites by viewModel.onlyFavorites.collectAsStateWithLifecycle()
    val favoriteSymbols by viewModel.favoriteSymbols.collectAsStateWithLifecycle()
    val isMarketLoading by viewModel.isMarketLoading.collectAsStateWithLifecycle()
    val isLoadingMore by viewModel.isLoadingMore.collectAsStateWithLifecycle()
    val hasMore by viewModel.hasMore.collectAsStateWithLifecycle()

    val selectedCoin by viewModel.selectedCoin.collectAsStateWithLifecycle()
    val currentTimeframe by viewModel.currentTimeframe.collectAsStateWithLifecycle()
    val analysisResult by viewModel.analysisResult.collectAsStateWithLifecycle()
    val isAnalysisLoading by viewModel.isAnalysisLoading.collectAsStateWithLifecycle()

    val signals by viewModel.signals.collectAsStateWithLifecycle()
    val isSignalsLoading by viewModel.isSignalsLoading.collectAsStateWithLifecycle()

    val subscription by viewModel.subscription.collectAsStateWithLifecycle()
    val isVerifyingTx by viewModel.isVerifyingTx.collectAsStateWithLifecycle()
    val verificationMessage by viewModel.verificationMessage.collectAsStateWithLifecycle()
    val isVerificationSuccess by viewModel.isVerificationSuccess.collectAsStateWithLifecycle()

    val cmcApiKey by viewModel.cmcApiKey.collectAsStateWithLifecycle()
    val bscApiKey by viewModel.bscApiKey.collectAsStateWithLifecycle()
    val isSandboxMode by viewModel.isSandboxMode.collectAsStateWithLifecycle()

        // Forex state
    LaunchedEffect(selectedMarket) {
        if (selectedMarket == MarketTab.FOREX) {
            forexViewModel.startIfNeeded()
        }
    }

    var showLanguageDialog by remember { mutableStateOf(false) }

    // RTL support for Persian and Arabic
    val layoutDirection = if (currentLanguage.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        // Back handler
        BackHandler(enabled = currentScreen !is Screen.Markets) {
            when (currentScreen) {
                is Screen.CoinDetail -> viewModel.navigateTo(Screen.Markets)
                is Screen.Welcome -> viewModel.navigateTo(Screen.Markets)
                is Screen.Signals -> viewModel.navigateTo(Screen.Markets)
                is Screen.Subscription -> viewModel.navigateTo(Screen.Markets)
                is Screen.ContactUs -> viewModel.navigateTo(Screen.Settings)
                is Screen.Settings -> viewModel.navigateTo(Screen.Markets)
                Screen.Markets -> {}
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.rigor_logo),
                                contentDescription = "Rigor Logo",
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, RigourCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = StringsDictionary.get("app_name", currentLanguage),
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (subscription.isActive) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(RigourBullish.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "PRO",
                                        color = RigourBullish,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        // Language switcher button with flag
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E293B))
                                .clickable { showLanguageDialog = true }
                                .testTag("topbar_lang_btn")
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = currentLanguage.flag, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = currentLanguage.code.uppercase(),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Refresh button
                        IconButton(
                            onClick = { viewModel.loadCoins() },
                            modifier = Modifier.testTag("topbar_refresh_btn")
                        ) {
                            if (isMarketLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = RigourCyan,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh",
                                    tint = RigourCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = RigourSurface,
                        titleContentColor = Color.White
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = RigourSurface,
                    contentColor = Color.White,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    val marketsSelected = currentScreen is Screen.Markets || currentScreen is Screen.CoinDetail
                    val signalsSelected = currentScreen is Screen.Signals
                    val subSelected = currentScreen is Screen.Subscription
                    val welcomeSelected = currentScreen is Screen.Welcome
                    val settingsSelected = currentScreen is Screen.Settings || currentScreen is Screen.ContactUs

                    val itemColors = NavigationBarItemDefaults.colors(
                        selectedIconColor = RigourCyan,
                        selectedTextColor = RigourCyan,
                        indicatorColor = RigourCyan.copy(alpha = 0.15f),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )

                    NavigationBarItem(
                        selected = marketsSelected,
                        onClick = { viewModel.navigateTo(Screen.Markets) },
                        icon = { Icon(Icons.Default.TrendingUp, contentDescription = "Markets") },
                        label = { Text(StringsDictionary.get("nav_markets", currentLanguage), fontSize = 10.sp) },
                        colors = itemColors,
                        modifier = Modifier.testTag("nav_item_markets")
                    )

                    NavigationBarItem(
                        selected = signalsSelected,
                        onClick = { viewModel.navigateTo(Screen.Signals) },
                        icon = { Icon(Icons.Default.AutoGraph, contentDescription = "Signals") },
                        label = { Text(StringsDictionary.get("nav_signals", currentLanguage), fontSize = 10.sp) },
                        colors = itemColors,
                        modifier = Modifier.testTag("nav_item_signals")
                    )

                    NavigationBarItem(
                        selected = subSelected,
                        onClick = { viewModel.navigateTo(Screen.Subscription) },
                        icon = {
                            Icon(
                                if (subscription.isActive) Icons.Default.Verified else Icons.Default.Lock,
                                contentDescription = "Pro Access",
                                tint = if (subscription.isActive) RigourBullish else if (subSelected) RigourCyan else Color.Gray
                            )
                        },
                        label = { Text(StringsDictionary.get("nav_subscription", currentLanguage), fontSize = 10.sp) },
                        colors = itemColors,
                        modifier = Modifier.testTag("nav_item_pro")
                    )

                    NavigationBarItem(
                        selected = welcomeSelected,
                        onClick = { viewModel.navigateTo(Screen.Welcome) },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Overview") },
                        label = { Text(StringsDictionary.get("nav_welcome", currentLanguage), fontSize = 10.sp) },
                        colors = itemColors,
                        modifier = Modifier.testTag("nav_item_overview")
                    )

                    NavigationBarItem(
                        selected = settingsSelected,
                        onClick = { viewModel.navigateTo(Screen.Settings) },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text(StringsDictionary.get("nav_settings", currentLanguage), fontSize = 10.sp) },
                        colors = itemColors,
                        modifier = Modifier.testTag("nav_item_settings")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(RigourBackground)
            ) {
                when (val screen = currentScreen) {
                    is Screen.Welcome -> {
                        WelcomeScreen(
                            lang = currentLanguage,
                            onExploreMarkets = { viewModel.navigateTo(Screen.Markets) },
                            onUnlockPro = { viewModel.navigateTo(Screen.Subscription) }
                        )
                    }

                    is Screen.Markets -> {
                        MarketDashboardScreen(
                            coins = filteredCoins,
                            searchQuery = searchQuery,
                            onSearchQueryChange = { viewModel.setSearchQuery(it) },
                            onlyFavorites = onlyFavorites,
                            onToggleFavorites = { viewModel.toggleFavoritesOnly() },
                            favoriteSymbols = favoriteSymbols,
                            onToggleFavoriteCoin = { viewModel.toggleFavorite(it) },
                            onSelectCoin = { viewModel.selectCoin(it) },
                            isLoading = isMarketLoading,
                            isLoadingMore = isLoadingMore,
                            hasMore = hasMore,
                            onLoadMore = { viewModel.loadMoreCoins() },
                            onRefresh = { viewModel.loadCoins() },
                            lang = currentLanguage
                        )
                    }

                    is Screen.Signals -> {
                        SignalsScreen(
                            signals = signals,
                            isLoading = isSignalsLoading,
                            subscription = subscription,
                            lang = currentLanguage,
                            onRefresh = { viewModel.computeSignals() },
                            onSelectCoin = { viewModel.selectCoin(it) },
                            onNavigateToSubscription = { viewModel.navigateTo(Screen.Subscription) }
                        )
                    }

                    is Screen.CoinDetail -> {
                        val currentCoin = selectedCoin ?: screen.coin
                        CoinDetailScreen(
                            coin = currentCoin,
                            analysisResult = analysisResult,
                            isLoadingAnalysis = isAnalysisLoading,
                            currentTimeframe = currentTimeframe,
                            onSelectTimeframe = { viewModel.setTimeframe(it) },
                            subscription = subscription,
                            isFavorite = favoriteSymbols.contains(currentCoin.symbol),
                            onToggleFavorite = { viewModel.toggleFavorite(currentCoin.symbol) },
                            onBack = { viewModel.navigateTo(Screen.Markets) },
                            onNavigateToSubscription = { viewModel.navigateTo(Screen.Subscription) },
                            lang = currentLanguage
                        )
                    }

                    is Screen.Subscription -> {
                        SubscriptionScreen(
                            subscription = subscription,
                            isVerifying = isVerifyingTx,
                            verificationMessage = verificationMessage,
                            isVerificationSuccess = isVerificationSuccess,
                            onVerifyTx = { txHash, plan -> viewModel.verifySubscription(txHash, plan) },
                            onActivateSandbox = { plan -> viewModel.activateInstantSandboxAccess(plan) },
                            onBazaarSubscribe = onBazaarSubscribe,
                            lang = currentLanguage
                        )
                    }

                    is Screen.ContactUs -> {
                        ContactUsScreen(
                            lang = currentLanguage,
                            onBack = { viewModel.navigateTo(Screen.Settings) }
                        )
                    }

                    is Screen.Settings -> {
                        SettingsScreen(
                            currentLanguage = currentLanguage,
                            onOpenLanguageDialog = { showLanguageDialog = true },
                            onNavigateToContact = { viewModel.navigateTo(Screen.ContactUs) },
                            cmcApiKey = cmcApiKey,
                            onSaveCmcApiKey = { viewModel.setCmcApiKey(it) },
                            bscApiKey = bscApiKey,
                            onSaveBscApiKey = { viewModel.setBscApiKey(it) },
                            isSandboxMode = isSandboxMode,
                            onToggleSandboxMode = { viewModel.setSandboxMode(it) },
                            subscription = subscription
                        )
                    }
                }

                // Modal Language Dialog
                if (showLanguageDialog) {
                    LanguageSelectorDialog(
                        currentLanguage = currentLanguage,
                        onLanguageSelected = { viewModel.setLanguage(it) },
                        onDismissRequest = { showLanguageDialog = false }
                    )
                }
            }
        }
    }
}
