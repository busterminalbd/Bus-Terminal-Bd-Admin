package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.BengaliUtils

/**
 * Reusable "previous day / calendar-with-date-inside / next day" navigator row.
 *
 * This is a common feature shared across any tool screen that works on a
 * per-day basis (Medical Work Report today, and any future daily-report
 * style tool later) — build it once here and wire it into each screen
 * instead of re-implementing the same date navigation UI per tool.
 *
 * [selectedDateIso] must be in "yyyy-MM-dd" format.
 */
@Composable
fun DateNavigatorBar(
    selectedDateIso: String,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onPickDate: () -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val parts = selectedDateIso.split("-")
    val dayNumber = parts.getOrNull(2) ?: "--"
    val monthIndex = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val monthAbbrBn = listOf(
        "জানু", "ফেব্রু", "মার্চ", "এপ্রি", "মে", "জুন",
        "জুলাই", "আগস্ট", "সেপ্ট", "অক্টো", "নভে", "ডিসে"
    ).getOrElse(monthIndex - 1) { "" }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPreviousDay, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = "পূর্ববর্তী দিন",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // Calendar-style chip: the date itself is written inside the icon,
            // so there's no need for a separate "তারিখ: ..." label elsewhere.
            Box(
                modifier = Modifier
                    .size(width = 48.dp, height = 44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onPickDate() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = monthAbbrBn,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = BengaliUtils.toBengaliDigits(dayNumber),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            IconButton(onClick = onNextDay, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "পরবর্তী দিন",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        trailingContent?.invoke()
    }
}
