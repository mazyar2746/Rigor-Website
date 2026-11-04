package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.RigourBackground
import com.example.ui.theme.RigourCyan
import kotlinx.coroutines.delay

@Composable
fun MarketSelectorScreen(
    onNavigateToCrypto: () -> Unit,
    onNavigateToIranStock: () -> Unit,
    onNavigateToForex: () -> Unit,
    onNavigateToDex: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.8f,
        animationSpec = tween(1000),
        label = "scale"
    )

    LaunchedEffect(Unit) {
        visible = true
        delay(100)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RigourBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .scale(scale),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // لوگو
            Image(
                painter = painterResource(id = R.drawable.rigor_logo),
                contentDescription = "Rigor Logo",
                modifier = Modifier.size(100.dp).clip(RoundedCornerShape(20.dp))
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "RIGOR",
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 8.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // جمله تزئینی
            Text(
                text = "A comprehensive analytical and trading platform for financial markets.",
                color = Color(0xFF94A3B8),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                fontStyle = FontStyle.Italic,
                lineHeight = 18.sp
            )
            Text(
                text = "Rigor is the best of the best.",
                color = RigourCyan,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            Text(
                text = "انتخاب بازار",
                fontSize = 20.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassMarketCard(
                    title = "کریپتو",
                    icon = "₿",
                    gradient = listOf(Color(0xFF6C63FF).copy(alpha = 0.8f), Color(0xFF9D4EDD).copy(alpha = 0.6f)),
                    onClick = onNavigateToCrypto,
                    modifier = Modifier.weight(1f)
                )
                GlassMarketCard(
                    title = "بورس ایران",
                    icon = "📈",
                    gradient = listOf(Color(0xFF10B981).copy(alpha = 0.8f), Color(0xFF059669).copy(alpha = 0.6f)),
                    onClick = onNavigateToIranStock,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassMarketCard(
                    title = "فارکس",
                    icon = "💱",
                    gradient = listOf(Color(0xFF6366F1).copy(alpha = 0.8f), Color(0xFF8B5CF6).copy(alpha = 0.6f)),
                    onClick = onNavigateToForex,
                    modifier = Modifier.weight(1f),
                    disabled = true
                )
                GlassMarketCard(
                    title = "دکس",
                    icon = "🔗",
                    gradient = listOf(Color(0xFF06B6D4).copy(alpha = 0.8f), Color(0xFF0891B2).copy(alpha = 0.6f)),
                    onClick = onNavigateToDex,
                    modifier = Modifier.weight(1f),
                    disabled = true
                )
            }
        }
    }
}

@Composable
fun GlassMarketCard(
    title: String,
    icon: String,
    gradient: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    disabled: Boolean = false
) {
    Card(
        modifier = modifier.height(100.dp),
        onClick = onClick,
        enabled = !disabled,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(if (disabled) gradient.map { it.copy(alpha = 0.3f) } else gradient),
                    RoundedCornerShape(16.dp)
                )
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = icon, fontSize = 32.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                if (disabled) {
                    Text(text = "به‌زودی", color = Color(0xFF94A3B8), fontSize = 10.sp)
                }
            }
        }
    }
}
