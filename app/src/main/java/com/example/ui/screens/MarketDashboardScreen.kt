package com.example.ui.screens
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
 
import com.example.ui.components.CoinIcon

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.CryptoCoin
import com.example.i18n.Language
import com.example.i18n.StringsDictionary
import com.example.ui.theme.RigourBackground
import com.example.ui.theme.RigourBearish
import com.example.ui.theme.RigourBullish
import com.example.ui.theme.RigourCardBorder
import com.example.ui.theme.RigourCyan
import com.example.ui.theme.RigourGold
import com.example.ui.theme.RigourSurface
import com.example.ui.theme.RigourSurfaceVariant
import java.util.Locale

@Composable
fun MarketDashboardScreen(
    onNavigateToHub: () -> Unit,
    coins: List<CryptoCoin>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onlyFavorites: Boolean,
    onToggleFavorites: () -> Unit,
    favoriteSymbols: List<String>,
    onToggleFavoriteCoin: (String) -> Unit,
    onSelectCoin: (CryptoCoin) -> Unit,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    lang: Language
) {
    val listState = rememberLazyListState()

    // Infinite scroll detection: triggers loading next page when user scrolls near the end
    val shouldLoadMore by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisible >= totalItems - 6 && !isLoadingMore && !isLoading && hasMore && searchQuery.isEmpty() && !onlyFavorites
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            onLoadMore()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RigourBackground)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // دکمه برگشت به Hub
        TextButton(
            onClick = onNavigateToHub,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("← بازگشت به انتخاب بازار", color = Color(0xFF00BCD4), fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))

        
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("crypto_search_input"),
            placeholder = {
                Text(
                    text = StringsDictionary.get("search_placeholder", lang),
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = RigourCyan
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = Color.Gray
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = RigourSurfaceVariant,
                unfocusedContainerColor = RigourSurfaceVariant,
                focusedBorderColor = RigourCyan,
                unfocusedBorderColor = RigourCardBorder,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filters, Count Badge, and Refresh Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = !onlyFavorites,
                    onClick = { if (onlyFavorites) onToggleFavorites() },
                    label = { Text("${StringsDictionary.get("all_coins_label", lang)} (${coins.size})", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = RigourCyan.copy(alpha = 0.2f),
                        selectedLabelColor = RigourCyan
                    ),
                    modifier = Modifier.testTag("filter_all_coins")
                )
                FilterChip(
                    selected = onlyFavorites,
                    onClick = onToggleFavorites,
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (onlyFavorites) RigourGold else Color.Gray,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${StringsDictionary.get("watchlist_label", lang)} (${favoriteSymbols.size})", fontSize = 12.sp)
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = RigourGold.copy(alpha = 0.2f),
                        selectedLabelColor = RigourGold
                    ),
                    modifier = Modifier.testTag("filter_watchlist")
                )
            }

            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(RigourSurfaceVariant)
                    .testTag("refresh_market_btn")
            ) {
                if (isLoading) {
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
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Cryptocurrencies List with Infinite Scroll
        if (coins.isEmpty() && !isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = StringsDictionary.get("no_coins_found", lang),
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(coins, key = { it.symbol }) { coin ->
                    val isFav = favoriteSymbols.contains(coin.symbol)
                    CoinListItem(
                        coin = coin,
                        isFavorite = isFav,
                        onToggleFavorite = { onToggleFavoriteCoin(coin.symbol) },
                        onClick = { onSelectCoin(coin) }
                    )
                }

                // Infinite Scroll Loader Footer
                if (isLoadingMore) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = RigourCyan,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = StringsDictionary.get("loading_more_coins", lang),
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                } else if (hasMore && searchQuery.isEmpty() && !onlyFavorites && coins.size >= 25) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = StringsDictionary.get("scroll_down_load_more", lang),
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun CoinListItem(
    coin: CryptoCoin,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit
) {
    val isPositive = coin.change24h >= 0
    val changeColor = if (isPositive) RigourBullish else RigourBearish
    val changePrefix = if (isPositive) "+" else ""

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(RigourSurface)
            .border(1.dp, RigourCardBorder.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("coin_item_${coin.symbol}")
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank and Coin Icon/Monogram
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${coin.rank}",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(26.dp)
                )

                CoinIcon(
                    iconUrl = coin.iconUrl,
                    symbol = coin.symbol,
                    name = coin.name,
                    size = 38.dp
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = coin.name,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = coin.symbol,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }

            // Price and 24h Change Badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatPrice(coin.priceUsd),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(changeColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = String.format(Locale.US, "%s%.2f%%", changePrefix, coin.change24h),
                            color = changeColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) RigourGold else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

fun formatPrice(price: Double): String {
    return com.example.ui.util.PriceFormatter.formatPrice(price)
}
