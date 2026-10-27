package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SubscriptionPlan
import com.example.data.model.UserSubscription
import com.example.data.remote.BscScanVerificationService
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
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var selectedPlan by remember { mutableStateOf(SubscriptionPlan.MONTHLY) }
    var txInput by remember { mutableStateOf("") }
    var showQrDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RigourBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Active Subscription Status Banner
        if (subscription.isActive) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF064E3B),
                                Color(0xFF0F172A)
                            )
                        )
                    )
                    .border(1.5.dp, RigourBullish, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(RigourBullish.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = RigourBullish,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = StringsDictionary.get("subscription_active", lang),
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        val dateFormatted = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            .format(Date(subscription.expiryTimestamp))
                        Text(
                            text = "${subscription.daysRemaining} ${StringsDictionary.get("days_remaining", lang)} (Expires: $dateFormatted)",
                            color = RigourBullish,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (subscription.isSandbox) {
                            Text(
                                text = "🧪 Sandbox Test Access Active",
                                color = RigourGold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
        }

        // Section Title
        Text(
            text = StringsDictionary.get("unlock_pro", lang),
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = StringsDictionary.get("plan_selection_subtitle", lang),
            color = Color(0xFF94A3B8),
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Official Cafe Bazaar Poolakey In-App Purchase Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF064E3B),
                            Color(0xFF0F172A)
                        )
                    )
                )
                .border(1.5.dp, Color(0xFF10B981).copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = StringsDictionary.get("bazaar_purchase_title", lang),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = StringsDictionary.get("bazaar_purchase_desc", lang),
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onBazaarSubscribe,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("bazaar_subscribe_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = StringsDictionary.get("bazaar_purchase_btn", lang),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Section Title: Crypto On-Chain Option
        Text(
            text = StringsDictionary.get("crypto_transfer_bep20", lang),
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = StringsDictionary.get("crypto_transfer_desc", lang),
            color = Color(0xFF94A3B8),
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Plan Selector Cards (Monthly 9.99 USDT vs Yearly 85 USDT)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PlanCard(
                title = StringsDictionary.get("pricing_monthly", lang),
                price = StringsDictionary.get("pricing_monthly_price", lang),
                period = StringsDictionary.get("plan_period_monthly", lang),
                badge = null,
                isSelected = selectedPlan == SubscriptionPlan.MONTHLY,
                onClick = { selectedPlan = SubscriptionPlan.MONTHLY },
                modifier = Modifier.weight(1f)
            )

            PlanCard(
                title = StringsDictionary.get("pricing_yearly", lang),
                price = StringsDictionary.get("pricing_yearly_price", lang),
                period = StringsDictionary.get("plan_period_yearly", lang),
                badge = StringsDictionary.get("pricing_yearly_badge", lang),
                isSelected = selectedPlan == SubscriptionPlan.YEARLY,
                onClick = { selectedPlan = SubscriptionPlan.YEARLY },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Payment Details Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(RigourSurface)
                .border(1.dp, RigourCardBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                // Network Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = StringsDictionary.get("network_required", lang),
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(RigourGold.copy(alpha = 0.15f))
                            .border(1.dp, RigourGold.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = StringsDictionary.get("network_bep20", lang),
                            color = RigourGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Wallet Address Label & Copy
                Text(
                    text = StringsDictionary.get("wallet_address_label", lang),
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(RigourSurfaceVariant)
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = BscScanVerificationService.OFFICIAL_RECEIVING_ADDRESS,
                            color = RigourCyan,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("USDT BEP20 Address", BscScanVerificationService.OFFICIAL_RECEIVING_ADDRESS)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(
                                    context,
                                    StringsDictionary.get("address_copied", lang),
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Address",
                                tint = RigourCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // High-Contrast Procedural QR Code
                Text(
                    text = StringsDictionary.get("deposit_qr_code", lang),
                    color = Color.Gray,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(10.dp)
                ) {
                    BscAddressQrCanvas(address = BscScanVerificationService.OFFICIAL_RECEIVING_ADDRESS)
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFF334155))
                Spacer(modifier = Modifier.height(14.dp))

                // Transaction Hash Input
                Text(
                    text = StringsDictionary.get("tx_hash_label", lang),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = txInput,
                    onValueChange = { txInput = it },
                    placeholder = {
                        Text(
                            text = StringsDictionary.get("tx_hash_placeholder", lang),
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_hash_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = RigourSurfaceVariant,
                        unfocusedContainerColor = RigourSurfaceVariant,
                        focusedBorderColor = RigourCyan,
                        unfocusedBorderColor = RigourCardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Verification Button
                Button(
                    onClick = { onVerifyTx(txInput, selectedPlan) },
                    enabled = !isVerifying,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("verify_subscription_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = RigourCyan),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isVerifying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color(0xFF0F172A),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = StringsDictionary.get("verifying_tx", lang),
                            color = Color(0xFF0F172A),
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = StringsDictionary.get("verify_transaction", lang),
                            color = Color(0xFF0F172A),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Verification Feedback Message
                if (verificationMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val msgColor = if (isVerificationSuccess) RigourBullish else RigourBearish
                    val icon = if (isVerificationSuccess) Icons.Default.CheckCircle else Icons.Default.Error

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(msgColor.copy(alpha = 0.12f))
                            .border(1.dp, msgColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = msgColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = verificationMessage,
                                color = msgColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Anti-Replay Security Notice
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(RigourSurfaceVariant.copy(alpha = 0.4f))
                .border(1.dp, RigourCardBorder.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = RigourCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = StringsDictionary.get("anti_replay_secured", lang),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = StringsDictionary.get("anti_replay_desc", lang),
                        color = Color.Gray,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Sandbox / Demonstration Mode Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E293B).copy(alpha = 0.6f))
                .border(1.dp, RigourGold.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = StringsDictionary.get("sandbox_mode", lang),
                        color = RigourGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(RigourGold.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = StringsDictionary.get("demo_reviewer_tool", lang),
                            color = RigourGold,
                            fontSize = 10.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = StringsDictionary.get("sandbox_mode_desc", lang),
                    color = Color.LightGray,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { onActivateSandbox(selectedPlan) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("sandbox_activate_btn"),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RigourGold),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = StringsDictionary.get("sandbox_activate", lang),
                        color = RigourGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
fun PlanCard(
    title: String,
    price: String,
    period: String,
    badge: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) RigourCyan else RigourCardBorder
    val bgColor = if (isSelected) RigourCyan.copy(alpha = 0.08f) else RigourSurface

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                if (badge != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(RigourBullish)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badge,
                            color = Color(0xFF0F172A),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = price,
                color = if (isSelected) RigourCyan else Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = period,
                color = Color.Gray,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun BscAddressQrCanvas(address: String) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val totalSize = size.width
        val gridCount = 21
        val cellSize = totalSize / gridCount

        // Clean white background
        drawRect(Color.White)

        // Draw 3 Corner Position Markers (Finder Patterns)
        drawFinderPattern(0, 0, cellSize)
        drawFinderPattern(gridCount - 7, 0, cellSize)
        drawFinderPattern(0, gridCount - 7, cellSize)

        // Draw deterministic matrix modules based on address hash
        val bytes = address.toByteArray()
        for (r in 0 until gridCount) {
            for (c in 0 until gridCount) {
                // Skip finder pattern zones
                if ((r < 7 && c < 7) || (r >= gridCount - 7 && c < 7) || (r < 7 && c >= gridCount - 7)) {
                    continue
                }
                val index = (r * gridCount + c) % bytes.size
                val bit = (bytes[index].toInt() + (r * 31) + (c * 17)) % 2 == 0
                if (bit) {
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(c * cellSize, r * cellSize),
                        size = Size(cellSize, cellSize)
                    )
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFinderPattern(
    startCol: Int,
    startRow: Int,
    cellSize: Float
) {
    val x = startCol * cellSize
    val y = startRow * cellSize

    // Outer 7x7 black box
    drawRect(
        color = Color.Black,
        topLeft = Offset(x, y),
        size = Size(cellSize * 7, cellSize * 7)
    )
    // Inner 5x5 white box
    drawRect(
        color = Color.White,
        topLeft = Offset(x + cellSize, y + cellSize),
        size = Size(cellSize * 5, cellSize * 5)
    )
    // Core 3x3 black box
    drawRect(
        color = Color.Black,
        topLeft = Offset(x + cellSize * 2, y + cellSize * 2),
        size = Size(cellSize * 3, cellSize * 3)
    )
}
