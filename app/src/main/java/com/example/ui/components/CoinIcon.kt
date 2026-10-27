package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.decode.SvgDecoder
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.R
import com.example.ui.theme.RigourCyan
import com.example.ui.theme.RigourSurfaceVariant

/**
 * Robust cryptocurrency icon component using Coil SubcomposeAsyncImage with multi-tier fallbacks.
 * Supports SVG decoding, persistent disk and memory caching, and progressive resolution:
 * 1. Primary remote iconUrl (CoinGecko / CoinMarketCap)
 * 2. SpotHQ cryptocurrency-icons GitHub repo (128px official PNG & SVG)
 * 3. CoinCap official assets CDN
 * 4. CryptoIcons API
 * 5. Monogram token badge with metallic border as final fallback
 */
@Composable
fun CoinIcon(
    iconUrl: String,
    symbol: String,
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp
) {
    val context = LocalContext.current
    val cleanSym = symbol.trim().lowercase()

    val candidates = remember(iconUrl, cleanSym) {
        val list = mutableListOf<String>()
        val primary = iconUrl.trim()
        if (primary.isNotEmpty()) {
            list.add(primary)
        }
        if (cleanSym.isNotEmpty()) {
            list.add("https://raw.githubusercontent.com/spothq/cryptocurrency-icons/master/128/color/$cleanSym.png")
            list.add("https://raw.githubusercontent.com/spothq/cryptocurrency-icons/master/svg/color/$cleanSym.svg")
            list.add("https://assets.coincap.io/assets/icons/$cleanSym@2x.png")
            list.add("https://cryptoicons.org/api/icon/$cleanSym/200")
        }
        list.distinct()
    }

    var currentCandidateIndex by remember(candidates) { mutableIntStateOf(0) }

    if (currentCandidateIndex < candidates.size) {
        val currentUrl = candidates[currentCandidateIndex]

        val imageRequest = remember(currentUrl) {
            ImageRequest.Builder(context)
                .data(currentUrl)
                .crossfade(true)
                .decoderFactory(SvgDecoder.Factory())
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build()
        }

        SubcomposeAsyncImage(
            model = imageRequest,
            contentDescription = "$name ($symbol)",
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(RigourSurfaceVariant),
            contentScale = ContentScale.Fit,
            loading = {
                GenericCoinBadge(symbol = symbol, size = size)
            },
            error = {
                LaunchedEffect(currentCandidateIndex) {
                    currentCandidateIndex++
                }
                GenericCoinBadge(symbol = symbol, size = size)
            }
        )
    } else {
        GenericCoinBadge(symbol = symbol, size = size, modifier = modifier)
    }
}

@Composable
fun GenericCoinBadge(
    symbol: String,
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(RigourCyan.copy(alpha = 0.12f))
            .border(1.dp, RigourCyan.copy(alpha = 0.35f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_generic_coin),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(0.6f),
            alpha = 0.4f
        )
        Text(
            text = symbol.take(4).uppercase(),
            color = Color.White,
            fontSize = if (size >= 38.dp) 11.sp else 9.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
