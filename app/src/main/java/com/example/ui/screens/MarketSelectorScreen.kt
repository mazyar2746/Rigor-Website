package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.RigourBackground
import com.example.ui.theme.RigourCyan

@Composable
fun MarketSelectorScreen(
    onNavigateToCrypto: () -> Unit,
    onNavigateToIranStock: () -> Unit,
    onNavigateToForex: () -> Unit,
    onNavigateToDex: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RigourBackground)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // جمله تزئینی
        Text(
            text = "A comprehensive analytical and trading platform for financial markets.",
            color = Color(0xFF94A3B8),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            fontStyle = FontStyle.Italic,
            lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Rigor is the best of the best.",
            color = RigourCyan,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "انتخاب بازار",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // دکمه‌ها
        MarketCard("📈 بازار سهام ایران", Color(0xFF10B981), onNavigateToIranStock)
        Spacer(modifier = Modifier.height(12.dp))
        MarketCard("₿ بازار کریپتو", Color(0xFF6C63FF), onNavigateToCrypto)
        Spacer(modifier = Modifier.height(12.dp))
        MarketCard("💱 فارکس (به‌زودی)", Color(0xFF6366F1), {}, enabled = false)
        Spacer(modifier = Modifier.height(12.dp))
        MarketCard("🔗 دکس (به‌زودی)", Color(0xFF06B6D4), {}, enabled = false)
    }
}

@Composable
fun MarketCard(text: String, color: Color, onClick: () -> Unit, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(16.dp),
        enabled = enabled
    ) {
        Text(text = text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}
