package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private data class OnboardingTip(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val description: String
)

private val onboardingTips = listOf(
    OnboardingTip(
        icon = Icons.Default.Add,
        title = "এক ট্যাপে দ্রুত আইটেম যোগ করুন",
        description = "\"দ্রুত আইটেম যোগ করুন\" বাটনে আগে থেকে সেভ করা আইটেম (চাল, ডাল, তেল...) দেখতে পাবেন — ট্যাপ করলেই বিলে যোগ হয়ে যাবে, বারবার টাইপ করা লাগবে না।"
    ),
    OnboardingTip(
        icon = Icons.Default.DragHandle,
        title = "টেনে ধরে সাজান",
        description = "যেকোনো আইটেমের পাশের ⋮⋮ আইকনে চেপে ধরে (একটু বেশি সময় চেপে) টেনে উপরে-নিচে করে আইটেমের ক্রম পরিবর্তন করতে পারবেন।"
    ),
    OnboardingTip(
        icon = Icons.Default.Share,
        title = "সরাসরি প্রিন্ট বা শেয়ার করুন",
        description = "বিল লেখা শেষে \"সংরক্ষণ\" চাপুন, তারপর \"শেয়ার / প্রিভিউ\" থেকে সরাসরি প্রিন্ট করুন অথবা WhatsApp-এ PDF/ছবি হিসেবে পাঠান।"
    ),
    OnboardingTip(
        icon = Icons.Default.History,
        title = "পুরনো হিসাব সবসময় হাতের কাছে",
        description = "নিচের \"সংরক্ষিত হিসাব\" ট্যাব থেকে যেকোনো দিনের বিল আবার দেখতে, এডিট করতে বা আবার প্রিন্ট করতে পারবেন।"
    )
)

/**
 * First-launch walkthrough. Shown once (controlled by FoodBillViewModel.showOnboarding),
 * dismissible at any step via "এড়িয়ে যান" or after the last tip via "শুরু করুন".
 */
@Composable
fun OnboardingDialog(onDismiss: () -> Unit) {
    var currentStep by remember { mutableIntStateOf(0) }
    val tip = onboardingTips[currentStep]
    val isLastStep = currentStep == onboardingTips.lastIndex

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = tip.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = tip.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = tip.description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Step indicator dots
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    onboardingTips.indices.forEach { index ->
                        Box(
                            modifier = Modifier
                                .size(if (index == currentStep) 9.dp else 7.dp)
                                .background(
                                    color = if (index == currentStep) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant,
                                    shape = CircleShape
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            "এড়িয়ে যান",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            if (isLastStep) {
                                onDismiss()
                            } else {
                                currentStep += 1
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = if (isLastStep) "শুরু করুন" else "পরবর্তী",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
