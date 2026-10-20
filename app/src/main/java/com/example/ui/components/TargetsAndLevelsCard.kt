package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TechnicalAnalysisResult
import com.example.i18n.Language
import com.example.i18n.StringsDictionary
import com.example.ui.theme.RigourBearish
import com.example.ui.theme.RigourBullish
import com.example.ui.theme.RigourCardBorder
import com.example.ui.theme.RigourCyan
import com.example.ui.theme.RigourGold
import com.example.ui.theme.RigourSurfaceVariant
import java.util.Locale

@Composable
fun TargetsAndLevelsCard(
    result: TechnicalAnalysisResult,
    lang: Language,
    modifier: Modifier = Modifier
) {
    val currentPrice = result.currentPrice.coerceAtLeast(0.00001)
    val shortPct = ((result.shortTermTarget - currentPrice) / currentPrice) * 100.0
    val medPct = ((result.mediumTermTarget - currentPrice) / currentPrice) * 100.0
    val slPct = ((result.stopLoss - currentPrice) / currentPrice) * 100.0

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(RigourSurfaceVariant.copy(alpha = 0.7f))
            .border(1.dp, RigourCardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = StringsDictionary.get("price_targets", lang),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TargetBox(
                    label = StringsDictionary.get("short_term_target", lang),
                    price = result.shortTermTarget,
                    percentChange = shortPct,
                    color = if (shortPct >= 0) RigourBullish else RigourBearish
                )
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                TargetBox(
                    label = StringsDictionary.get("medium_term_target", lang),
                    price = result.mediumTermTarget,
                    percentChange = medPct,
                    color = if (medPct >= 0) RigourBullish else RigourBearish
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Stop-Loss
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF261922))
                    .border(1.dp, RigourBearish.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = StringsDictionary.get("stop_loss", lang),
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
                Text(
                    text = String.format(Locale.US, "$%.2f (%+.1f%%)", result.stopLoss, slPct),
                    color = RigourBearish,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFF334155))
            Spacer(modifier = Modifier.height(14.dp))

            // Classical Support and Resistance Pivot Matrix
            Text(
                text = StringsDictionary.get("support_resistance", lang),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            LevelRow(label = StringsDictionary.get("resistance_2", lang), price = result.resistance2, color = RigourBearish)
            LevelRow(label = StringsDictionary.get("resistance_1", lang), price = result.resistance1, color = RigourBearish.copy(alpha = 0.8f))
            LevelRow(label = StringsDictionary.get("pivot_point", lang), price = result.pivotPoint, color = RigourCyan, isBold = true)
            LevelRow(label = StringsDictionary.get("support_1", lang), price = result.support1, color = RigourBullish.copy(alpha = 0.8f))
            LevelRow(label = StringsDictionary.get("support_2", lang), price = result.support2, color = RigourBullish)
        }
    }
}

@Composable
private fun TargetBox(
    label: String,
    price: Double,
    percentChange: Double,
    color: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1E293B))
            .padding(10.dp)
    ) {
        Column {
            Text(text = label, color = Color.Gray, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = String.format(Locale.US, "$%.2f", price),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = String.format(Locale.US, "%+.2f%%", percentChange),
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun LevelRow(
    label: String,
    price: Double,
    color: Color,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (isBold) RigourCyan else Color.LightGray,
            fontSize = 12.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = String.format(Locale.US, "$%.2f", price),
            color = color,
            fontSize = 12.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium
        )
    }
}
