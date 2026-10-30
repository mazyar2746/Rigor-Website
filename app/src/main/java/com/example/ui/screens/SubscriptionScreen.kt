package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SubscriptionPlan
import com.example.data.model.UserSubscription
import com.example.i18n.Language
import com.example.i18n.StringsDictionary
import com.example.ui.theme.RigourBackground
import com.example.ui.theme.RigourBullish
import com.example.ui.theme.RigourCyan
import com.example.ui.theme.RigourGold
import com.example.ui.theme.RigourSurface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SubscriptionScreen(
    subscription: UserSubscription,
    isVerifying: Boolean,
    verificationMessage: String?,
    isVerificationSuccess: Boolean,
    onVerifyTx: (String, SubscriptionPlan) -> Unit,
    onActivateSandbox: (SubscriptionPlan) -> Unit,
    onBazaarSubscribe: () -> Unit = {},
    lang: Language
) {
    val scrollState = rememberScrollState()
    val expiresText = subscription.expiresAt?.let {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(it))
    } ?: "N/A"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RigourBackground)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Verified, contentDescription = "Pro", tint = RigourGold, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(StringsDictionary.get("pro_title", lang), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = RigourSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (subscription.isActive) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Active", tint = RigourBullish, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(StringsDictionary.get("pro_active", lang), color = RigourBullish, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(StringsDictionary.get("pro_expires", lang) + ": " + expiresText, color = Color.Gray, fontSize = 14.sp)
                } else {
                    Text(StringsDictionary.get("pro_inactive", lang), color = Color.Gray, fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = RigourSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(StringsDictionary.get("pro_upgrade_title", lang), color = RigourGold, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(StringsDictionary.get("pro_upgrade_desc", lang), color = Color.Gray, fontSize = 14.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onBazaarSubscribe,
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("bazaar_subscribe_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = RigourCyan),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = "Buy", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(StringsDictionary.get("pro_buy_btn", lang), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(StringsDictionary.get("pro_features_title", lang), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        val features = listOf("pro_feature_1", "pro_feature_2", "pro_feature_3", "pro_feature_4", "pro_feature_5")
        features.forEach { featureKey ->
            Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Check", tint = RigourBullish, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(StringsDictionary.get(featureKey, lang), color = Color.White, fontSize = 14.sp)
            }
        }
    }
}
