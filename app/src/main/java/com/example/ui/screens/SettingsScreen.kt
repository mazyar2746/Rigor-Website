package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserSubscription
import com.example.i18n.Language
import com.example.i18n.StringsDictionary
import com.example.ui.theme.RigourBackground
import com.example.ui.theme.RigourBullish
import com.example.ui.theme.RigourCardBorder
import com.example.ui.theme.RigourCyan
import com.example.ui.theme.RigourGold
import com.example.ui.theme.RigourSurface
import com.example.ui.theme.RigourSurfaceVariant

import androidx.compose.material.icons.filled.Email

@Composable
fun SettingsScreen(
    currentLanguage: Language,
    onOpenLanguageDialog: () -> Unit,
    onNavigateToContact: () -> Unit = {},
    cmcApiKey: String,
    onSaveCmcApiKey: (String) -> Unit,
    bscApiKey: String,
    onSaveBscApiKey: (String) -> Unit,
    isSandboxMode: Boolean,
    onToggleSandboxMode: (Boolean) -> Unit,
    subscription: UserSubscription
) {
    val scrollState = rememberScrollState()

    var cmcKeyInput by remember(cmcApiKey) { mutableStateOf(cmcApiKey) }
    var bscKeyInput by remember(bscApiKey) { mutableStateOf(bscApiKey) }
    var saveStatus by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RigourBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = StringsDictionary.get("nav_settings", currentLanguage),
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Contact Us & Official Support Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(RigourSurface)
                .border(1.dp, RigourCyan.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                .clickable(onClick = onNavigateToContact)
                .testTag("settings_contact_btn")
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = RigourCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = StringsDictionary.get("contact_us_btn", currentLanguage),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "h.nasiri3247@gmail.com",
                            color = RigourCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Language Switcher Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(RigourSurface)
                .border(1.dp, RigourCardBorder, RoundedCornerShape(14.dp))
                .clickable(onClick = onOpenLanguageDialog)
                .testTag("settings_language_btn")
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = RigourCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = StringsDictionary.get("select_language", currentLanguage),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${currentLanguage.flag} ${currentLanguage.nativeName} (${if (currentLanguage.isRtl) "RTL" else "LTR"})",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Subscription Status
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(RigourSurface)
                .border(1.dp, RigourCardBorder, RoundedCornerShape(14.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = if (subscription.isActive) RigourBullish else RigourGold,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = StringsDictionary.get("rigor_pro_status", currentLanguage),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (subscription.isActive) {
                                "${StringsDictionary.get("subscription_active", currentLanguage)} (${subscription.daysRemaining} ${StringsDictionary.get("days_remaining", currentLanguage)})"
                            } else {
                                if (currentLanguage == Language.PERSIAN) {
                                    "غیرفعال / نسخه پیش‌نمایش رایگان"
                                } else {
                                    "Inactive / Free Preview"
                                }
                            },
                            color = if (subscription.isActive) RigourBullish else Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sandbox Reviewer Mode Toggle
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(RigourSurface)
                .border(1.dp, RigourCardBorder, RoundedCornerShape(14.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Science,
                        contentDescription = null,
                        tint = RigourGold,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Sandbox / Testnet Mode",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Instant simulated verification for reviewer demo",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                }
                Switch(
                    checked = isSandboxMode,
                    onCheckedChange = onToggleSandboxMode,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = RigourGold,
                        checkedTrackColor = RigourGold.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.testTag("sandbox_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // API Key Configuration Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(RigourSurface)
                .border(1.dp, RigourCardBorder, RoundedCornerShape(14.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = RigourCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Data Provider API Keys",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Rigor features live fallback endpoints that work out of the box. You may also provide custom API keys below:",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("CoinMarketCap Pro API Key", color = Color.LightGray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = cmcKeyInput,
                    onValueChange = { cmcKeyInput = it },
                    placeholder = { Text("e.g. b54bcf4d-1bca-...", color = Color.DarkGray, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = RigourSurfaceVariant,
                        unfocusedContainerColor = RigourSurfaceVariant,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("BscScan API Key", color = Color.LightGray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = bscKeyInput,
                    onValueChange = { bscKeyInput = it },
                    placeholder = { Text("e.g. 9Y7X...", color = Color.DarkGray, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = RigourSurfaceVariant,
                        unfocusedContainerColor = RigourSurfaceVariant,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        onSaveCmcApiKey(cmcKeyInput.trim())
                        onSaveBscApiKey(bscKeyInput.trim())
                        saveStatus = "Settings updated successfully!"
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RigourCyan),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = StringsDictionary.get("settings_save", currentLanguage),
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                if (saveStatus != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = saveStatus ?: "",
                        color = RigourBullish,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Rigour Branding & Credits
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Rigor Institutional Analytics v1.0.0",
                color = Color.DarkGray,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Protected by BEP20 On-Chain Anti-Replay",
                color = Color(0xFF475569),
                fontSize = 10.sp
            )
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}
