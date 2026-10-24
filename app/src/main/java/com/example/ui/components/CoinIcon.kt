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
import androidx.compose.runtime.remember
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
import coil.request.ImageRequest
import com.example.R
import com.example.ui.theme.RigourCyan
import com.example.ui.theme.RigourSurfaceVariant

/**
 * Robust cryptocurrency icon component using Coil SubcomposeAsyncImage.
 * Supports SVG decoding, crossfade, and a graceful fallback/placeholder badge with
 * generic coin icon and token monogram if image fails or is missing.
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
    val cleanUrl = iconUrl.trim()

    val imageRequest = remember(cleanUrl) {
        ImageRequest.Builder(context)
            .data(cleanUrl.ifEmpty { null })
            .crossfade(true)
            .decoderFactory(SvgDecoder.Factory())
            .build()
    }

    if (cleanUrl.isNotEmpty()) {
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
            text = symbol.take(3).uppercase(),
            color = Color.White,
            fontSize = if (size >= 38.dp) 11.sp else 9.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
