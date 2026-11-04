package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.StockInfo
import com.example.data.repository.IranStockRepository
import kotlinx.coroutines.launch

// تابع کمکی برای نرمال‌سازی حروف عربی و فارسی
fun normalizePersian(text: String): String {
    return text
        .replace('ك', 'ک')  // کاف عربی به فارسی
        .replace('ي', 'ی')  // ی عربی به فارسی
        .replace('ى', 'ی')  // ی مقصوره به فارسی
        .replace('ة', 'ه')  // تاء مربوطه به ه
        .replace('\u200c', ' ') // نیم‌فاصله به فاصله
        .lowercase()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IranStockSearchScreen(
    onBack: () -> Unit,
    onStockSelected: (String) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var allSymbols by remember { mutableStateOf<List<StockInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val repository = remember { IranStockRepository(context) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                allSymbols = repository.fetchSymbolsList()
                if (allSymbols.isEmpty()) {
                    errorMsg = "لیست خالی است."
                }
            } catch (e: Exception) {
                errorMsg = "خطا: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    val filteredSymbols = if (searchQuery.isEmpty()) {
        allSymbols
    } else {
        val normalizedQuery = normalizePersian(searchQuery)
        allSymbols.filter { stock ->
            normalizePersian(stock.name).contains(normalizedQuery)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("بازار سهام ایران", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF00BCD4),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("جستجوی نماد (مثلاً فولاد)") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (errorMsg != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = errorMsg!!, color = Color.Red)
                }
            } else if (filteredSymbols.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("نمادی با این نام پیدا نشد.")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredSymbols) { stock ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onStockSelected(stock.name) },
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stock.name,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "کد: ${stock.insCode}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
