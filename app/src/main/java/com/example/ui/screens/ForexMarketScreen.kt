package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.forex.ForexInterval
import com.example.ui.forex.ForexRow
import com.example.ui.forex.ForexViewModel

@Composable
fun ForexMarketScreen(viewModel: ForexViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.startIfNeeded() }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A)).padding(16.dp)) {
        OutlinedTextField(
            value = viewModel.searchQuery, onValueChange = { viewModel.updateSearchQuery(it) }, placeholder = { Text("Search pair...", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(focusedContainerColor = Color(0xFF1E293B), unfocusedContainerColor = Color(0xFF1E293B), focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { if (viewModel.majorsOnly) viewModel.toggleMajorsFilter() }, colors = ButtonDefaults.buttonColors(containerColor = if (!viewModel.majorsOnly) Color(0xFF06B6D4) else Color(0xFF1E293B))) { Text("All", color = Color.White) }
            Button(onClick = { if (!viewModel.majorsOnly) viewModel.toggleMajorsFilter() }, colors = ButtonDefaults.buttonColors(containerColor = if (viewModel.majorsOnly) Color(0xFFEAB308) else Color(0xFF1E293B))) { Text("Majors", color = Color.White) }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ForexInterval.values().forEach { intvl ->
                val isSelected = viewModel.interval == intvl
                Box(modifier = Modifier.weight(1f).background(if (isSelected) Color(0xFF06B6D4).copy(alpha = 0.2f) else Color(0xFF1E293B), RoundedCornerShape(8.dp)).clickable { viewModel.updateInterval(intvl) }.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Text(intvl.apiValue, color = if (isSelected) Color(0xFF06B6D4) else Color.Gray, fontSize = 10.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFF06B6D4)) }
        } else if (state.errorMessage != null) {
            Text("Error: ${state.errorMessage}", color = Color.Red)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.rows) { row -> ForexPairRow(row) }
            }
        }
    }
}

@Composable
fun ForexPairRow(row: ForexRow) {
    val isPositive = row.percentChange >= 0
    val signalColor = when {
        row.signal.contains("BUY") -> Color(0xFF10B981)
        row.signal.contains("SELL") -> Color(0xFFEF4444)
        row.signal.contains("PRO") -> Color(0xFFEAB308)
        else -> Color(0xFF94A3B8)
    }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(48.dp).background(Color(0xFF0F172A), CircleShape), contentAlignment = Alignment.Center) {
                    Text(row.pair.baseCurrency, color = Color(0xFFEAB308), fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(row.pair.symbol, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Confidence: ${row.confidence}%", color = Color.Gray, fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(row.price, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("${if (isPositive) "+" else ""}${String.format("%.2f", row.percentChange)}%", color = if (isPositive) Color(0xFF10B981) else Color(0xFFEF4444), fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Box(modifier = Modifier.background(signalColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(row.signal, color = signalColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color(0xFF334155))
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                InfoColumn("Entry", row.entry)
                InfoColumn("Target", row.target)
                InfoColumn("Stop", row.stopLoss)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                InfoColumn("Support", row.support1)
                InfoColumn("Resist", row.resistance1)
                InfoColumn("RSI", row.analysisDescription.take(15))
            }
        }
    }
}

@Composable
fun InfoColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.Gray, fontSize = 10.sp)
        Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
