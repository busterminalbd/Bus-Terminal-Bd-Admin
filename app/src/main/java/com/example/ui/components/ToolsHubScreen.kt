package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.ui.theme.DarkForestGreen
import com.example.ui.theme.ForestGreenText
import com.example.ui.theme.HeadingFontFamily
import com.example.ui.theme.LightForestGreen
import com.example.ui.theme.WarmBorderColor
import com.example.update.UpdateInfo
import com.example.util.BengaliUtils

data class AppToolItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val isAvailable: Boolean = true,
    val badgeText: String = if (isAvailable) "সক্রিয়" else "শীঘ্রই আসছে",
    val accentColor: Color = Color(0xFF1565C0)
)

@Composable
fun ToolsHubScreen(
    appLanguage: String = "bn",
    updateInfo: UpdateInfo? = null,
    isCheckingUpdate: Boolean = false,
    onCheckUpdate: () -> Unit = {},
    onOpenUpdateDialog: (UpdateInfo) -> Unit = {},
    onSelectFoodBillTool: () -> Unit,
    onSelectAdvanceSalaryTool: () -> Unit = {},
    onSelectMedicalWorkTool: () -> Unit = {},
    onSelectAppSettings: () -> Unit = {},
    onSelectUpcomingTool: (title: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isEn = appLanguage == "en"

    val toolsList = listOf(
        AppToolItem(
            id = "food_bill",
            title = if (isEn) "Food Bill Memo" else "খাবার বিল মেমো",
            subtitle = if (isEn) "Create, print and save daily grocery bill memos" else "দৈনিক খাবার বাজার বিল তৈরি, প্রিন্ট ও হিসাব সংরক্ষণ করুন",
            icon = Icons.Default.Restaurant,
            isAvailable = true,
            badgeText = if (isEn) "Active" else "চালু আছে",
            accentColor = MaterialTheme.colorScheme.primary
        ),
        AppToolItem(
            id = "advance_salary",
            title = if (isEn) "Advance Salary Application" else "অগ্রিম বেতন আবেদন",
            subtitle = if (isEn) "Create, save and print advance salary applications with installments" else "কর্মকর্তা-কর্মচারীদের অগ্রিম বেতন আবেদনপত্র তৈরি, কিস্তি হিসাব, প্রিন্ট ও সংরক্ষণ করুন",
            icon = Icons.AutoMirrored.Filled.ReceiptLong,
            isAvailable = true,
            badgeText = if (isEn) "Active" else "চালু আছে",
            accentColor = MaterialTheme.colorScheme.secondary
        ),
        AppToolItem(
            id = "medical_work",
            title = if (isEn) "Medical Work Report" else "মেডিকেল ওয়ার্ক রিপোর্ট",
            subtitle = if (isEn) "Daily patient ID, code & name entry, table report generation, print & share" else "আইডি, কোড ও নাম এন্ট্রি, ডেইলি রিপোর্ট টেবিল তৈরি, প্রিন্ট ও ইমেজ শেয়ার",
            icon = Icons.Default.Assignment,
            isAvailable = true,
            badgeText = if (isEn) "Active" else "চালু আছে",
            accentColor = Color(0xFF0D47A1)
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Hero Header Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.secondary,
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0x33FFFFFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Store,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isEn) "Digital Tool" else "ডিজিটাল টুল",
                            fontFamily = HeadingFontFamily,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isEn) "All your essential tools in one place" else "আপনার প্রয়োজনীয় সকল টুলস এক জায়গায়",
                            fontSize = 13.sp,
                            color = Color(0xFFE2D6C5)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0x33FFFFFF))
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Build & Auto Update Status Indicator
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x22FFFFFF))
                        .clickable { onCheckUpdate() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statusColor = when {
                        isCheckingUpdate -> Color(0xFFFFC107) // 🟡 Yellow (Processing)
                        updateInfo != null && updateInfo.errorMessage == null -> Color(0xFF4CAF50) // 🟢 Green (Success)
                        else -> Color(0xFFE53935) // 🔴 Red (Failed)
                    }

                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(statusColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    val statusText = when {
                        isCheckingUpdate -> if (isEn) "Build/Update Status: Processing..." else "আপডেট/বিল্ড স্ট্যাটাস: প্রসেসিং..."
                        updateInfo != null && updateInfo.errorMessage == null -> {
                            if (updateInfo.hasUpdate) {
                                if (isEn) "Update available: v${updateInfo.latestVersionName} (Tap to update)"
                                else "নতুন সংস্করণ উপলব্ধ: v${updateInfo.latestVersionName} (ট্যাপ করে আপডেট করুন)"
                            } else {
                                if (isEn) "Build Status: Up to date (v${BuildConfig.VERSION_NAME})"
                                else "বিল্ড স্ট্যাটাস: অ্যাপ সর্বশেষ সংস্করণে আছে (v${BengaliUtils.toBengaliDigits(BuildConfig.VERSION_NAME)})"
                            }
                        }
                        else -> {
                            val err = updateInfo?.errorMessage ?: if (isEn) "Connection failed" else "সংযোগ ব্যর্থ"
                            if (isEn) "Build/Update error: $err" else "আপডেট/বিল্ড ত্রুটি: $err"
                        }
                    }

                    Text(
                        text = statusText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )

                    if (updateInfo?.hasUpdate == true && !isCheckingUpdate) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFFC107))
                                .clickable { onOpenUpdateDialog(updateInfo) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isEn) "UPDATE" else "আপডেট",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (isEn) "Select any required tool to start:" else "প্রয়োজনীয় যেকোনো টুল নির্বাচন করে কাজ শুরু করুন:",
                    fontSize = 13.sp,
                    color = Color(0xFFF5EBE0),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = if (isEn) "Available Tools Index" else "উপলব্ধ টুলস সূচী",
            fontFamily = HeadingFontFamily,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )

        toolsList.forEach { tool ->
            ToolCardItem(
                tool = tool,
                onClick = {
                    when (tool.id) {
                        "food_bill" -> onSelectFoodBillTool()
                        "advance_salary" -> onSelectAdvanceSalaryTool()
                        "medical_work" -> onSelectMedicalWorkTool()
                        "app_settings" -> onSelectAppSettings()
                        else -> onSelectUpcomingTool(tool.title)
                    }
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Bottom Info Note
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isEn) "💡 More new daily tools will be added soon!" else "💡 আরও নতুন দৈনন্দিন টুলস খুব শীঘ্রই যোগ করা হবে!",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ToolCardItem(
    tool: AppToolItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (tool.isAvailable) 1.dp else 0.5.dp,
                color = if (tool.isAvailable) tool.accentColor.copy(alpha = 0.25f) else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .testTag("tool_card_${tool.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (tool.isAvailable) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (tool.isAvailable) 3.5.dp else 1.dp
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Badge
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (tool.isAvailable) tool.accentColor.copy(alpha = 0.12f)
                        else Color(0xFFE0E0E0)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = tool.title,
                    tint = if (tool.isAvailable) tool.accentColor else Color.Gray,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Text Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = tool.title,
                        fontFamily = HeadingFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (tool.isAvailable) MaterialTheme.colorScheme.onSurface else Color.DarkGray,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (tool.isAvailable) MaterialTheme.colorScheme.primary
                                else Color(0xFF757575)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = tool.badgeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = tool.subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Arrow
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "খুলুন",
                tint = if (tool.isAvailable) MaterialTheme.colorScheme.primary else Color.LightGray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
