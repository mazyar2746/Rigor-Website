package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.config.RigorConfig
import com.example.i18n.Language
import com.example.i18n.StringsDictionary
import com.example.ui.theme.RigourBackground
import com.example.ui.theme.RigourCardBorder
import com.example.ui.theme.RigourCyan
import com.example.ui.theme.RigourSurface
import com.example.ui.theme.RigourSurfaceVariant

@Composable
fun ContactUsScreen(
    lang: Language,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val officialEmail = RigorConfig.OFFICIAL_SUPPORT_EMAIL

    var senderName by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var messageBody by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RigourBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("contact_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = RigourCyan,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = StringsDictionary.get("contact_title", lang),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (lang == Language.PERSIAN) {
                        "پاسخگویی رسمی و پشتیبانی مستقیم ریگور"
                    } else {
                        "Institutional support & official feedback resolution"
                    },
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Official Email Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(RigourSurface)
                .border(1.dp, RigourCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(RigourCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = RigourCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = StringsDictionary.get("contact_email_label", lang),
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                            Text(
                                text = officialEmail,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Support Email", officialEmail)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Email copied to clipboard!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Email",
                            tint = RigourCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:$officialEmail")
                            putExtra(Intent.EXTRA_SUBJECT, if (subject.isNotBlank()) subject else "Rigor Crypto Support Request")
                            putExtra(Intent.EXTRA_TEXT, "Hello Rigor Team,\n\n$messageBody\n\nFrom: $senderName")
                        }
                        try {
                            context.startActivity(Intent.createChooser(emailIntent, "Send Email via..."))
                        } catch (e: Exception) {
                            Toast.makeText(context, "No email app found. Please send to $officialEmail", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("send_email_intent_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = RigourCyan),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = null,
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = StringsDictionary.get("send_email", lang),
                        color = Color(0xFF0F172A),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Direct Message Form
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(RigourSurface)
                .border(1.dp, RigourCardBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "Direct Feedback Form",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = senderName,
                    onValueChange = { senderName = it },
                    placeholder = { Text(StringsDictionary.get("contact_name", lang), color = Color.Gray, fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
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

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    placeholder = { Text(StringsDictionary.get("contact_subject", lang), color = Color.Gray, fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
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

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = messageBody,
                    onValueChange = { messageBody = it },
                    placeholder = { Text(StringsDictionary.get("contact_message", lang), color = Color.Gray, fontSize = 13.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = RigourSurfaceVariant,
                        unfocusedContainerColor = RigourSurfaceVariant,
                        focusedBorderColor = RigourCyan,
                        unfocusedBorderColor = RigourCardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    maxLines = 4
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Frequently Asked Questions
        Text(
            text = StringsDictionary.get("faq_title", lang),
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        FaqItem(
            question = StringsDictionary.get("faq_q1", lang),
            answer = StringsDictionary.get("faq_a1", lang)
        )
        Spacer(modifier = Modifier.height(8.dp))
        FaqItem(
            question = StringsDictionary.get("faq_q2", lang),
            answer = StringsDictionary.get("faq_a2", lang)
        )
        Spacer(modifier = Modifier.height(8.dp))
        FaqItem(
            question = StringsDictionary.get("faq_q3", lang),
            answer = StringsDictionary.get("faq_a3", lang)
        )

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
fun FaqItem(
    question: String,
    answer: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = RigourSurfaceVariant.copy(alpha = 0.5f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, RigourCardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.HelpOutline,
                    contentDescription = null,
                    tint = RigourCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = question,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = answer,
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
    }
}
