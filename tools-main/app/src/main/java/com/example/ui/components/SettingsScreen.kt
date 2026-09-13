package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.example.BuildConfig
import com.example.update.AppUpdateManager
import com.example.update.UpdateDialog
import com.example.update.UpdateInfo
import com.example.util.BengaliUtils
import kotlinx.coroutines.launch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.CurrentBillState
import com.example.ui.theme.MaroonHeaderColor
import com.example.util.QuickPreset

@Composable
fun SettingsScreen(
    state: CurrentBillState,
    editingBillType: String = state.billType,
    getSavedSettingsForType: (type: String) -> Triple<String, String, String> = { _ ->
        Triple(state.centerName, state.subtitle, state.purchaserLabel)
    },
    quickPresets: List<QuickPreset> = emptyList(),
    onAddCustomPreset: (name: String, qty: String, rate: String, amount: String) -> Unit = { _, _, _, _ -> },
    onRemovePreset: (preset: QuickPreset) -> Unit = {},
    onResetPresetsDefault: () -> Unit = {},
    onSaveSettings: (billType: String, centerName: String, subtitle: String, purchaserLabel: String) -> Unit,
    onResetTemplate: () -> Unit,
    onResetAllData: () -> Unit,
    onOpenGlobalSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val focusCenterName = remember { FocusRequester() }
    val focusSubtitle = remember { FocusRequester() }
    val focusPurchaserLabel = remember { FocusRequester() }

    val signatureOptions = listOf("স্বাক্ষর", "ক্রয়কারীর স্বাক্ষর", "অনুমোদনকারীর স্বাক্ষর")
    val customOption = "কাস্টম..."

    // Loads the saved title/subtitle/signature for whichever bill type is currently
    // selected in the app bar dropdown, so "যাতায়াত ভাড়া" and "খাবার বিল" can each keep
    // their own separate header settings. Re-loaded whenever editingBillType changes.
    val savedForType = remember(editingBillType) { getSavedSettingsForType(editingBillType) }
    var centerNameField by remember(editingBillType) { mutableStateOf(savedForType.first) }
    var subtitleField by remember(editingBillType) { mutableStateOf(savedForType.second) }
    val purchaserLabelInitial = savedForType.third

    val initialIsPreset = purchaserLabelInitial in signatureOptions
    var selectedSignatureOption by remember(editingBillType) {
        mutableStateOf(if (initialIsPreset) purchaserLabelInitial else if (purchaserLabelInitial.isBlank()) "স্বাক্ষর" else customOption)
    }
    var customSignatureText by remember(editingBillType) {
        mutableStateOf(if (initialIsPreset) "" else purchaserLabelInitial)
    }
    var expandedSignatureDropdown by remember { mutableStateOf(false) }

    val editingTypeLabel = if (editingBillType == "transport") "যাতায়াত ভাড়া" else "খাবার বিল"

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "ক্যাশ মেমো হেডার সেটিংস — $editingTypeLabel",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "উপরের ড্রপডাউন থেকে বিলের ধরন পরিবর্তন করে আলাদাভাবে সেটিংস সংরক্ষণ করতে পারবেন",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Shortcut card to Main App Settings
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenGlobalSettings() },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "মেইন অ্যাপ সেটিংস (Main App Settings)",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "ডাটা ডাউনলোড ও ইনপুট, থিম, ভাষা ও অ্যাপ আপডেট",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "মেমোর হেডার তথ্য পরিবর্তন ও সংরক্ষণ:",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = centerNameField,
                    onValueChange = { centerNameField = it },
                    label = { Text("মেডিকেল বা প্রতিষ্ঠানের নাম") },
                    singleLine = true,
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusSubtitle.requestFocus() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusCenterName)
                        .testTag("center_name_setting_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = subtitleField,
                    onValueChange = { subtitleField = it },
                    label = { Text("মেমোর সাবটাইটেল / শিরোনাম") },
                    singleLine = true,
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { expandedSignatureDropdown = true }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusSubtitle)
                        .testTag("subtitle_setting_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "স্বাক্ষরের শিরোনাম নির্বাচন:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedSignatureOption,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("স্বাক্ষরের টাইটেল অপশন") },
                        trailingIcon = {
                            Text(
                                text = "▾",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        },
                        singleLine = true,
                        textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("signature_title_dropdown")
                    )

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { expandedSignatureDropdown = true }
                    )

                    DropdownMenu(
                        expanded = expandedSignatureDropdown,
                        onDismissRequest = { expandedSignatureDropdown = false },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        signatureOptions.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option,
                                        fontSize = 14.sp,
                                        fontWeight = if (selectedSignatureOption == option) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedSignatureOption == option) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    selectedSignatureOption = option
                                    expandedSignatureDropdown = false
                                }
                            )
                        }

                        Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = customOption,
                                    fontSize = 14.sp,
                                    fontWeight = if (selectedSignatureOption == customOption) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedSignatureOption == customOption) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                selectedSignatureOption = customOption
                                expandedSignatureDropdown = false
                            }
                        )
                    }
                }

                if (selectedSignatureOption == customOption) {
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = customSignatureText,
                        onValueChange = { newText -> customSignatureText = newText },
                        label = { Text("কাস্টম স্বাক্ষরের শিরোনাম লিখুন") },
                        singleLine = true,
                        textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusPurchaserLabel)
                            .testTag("purchaser_label_setting_input")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        val finalLabel = if (selectedSignatureOption == customOption) customSignatureText.trim() else selectedSignatureOption
                        onSaveSettings(editingBillType, centerNameField, subtitleField, finalLabel)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("save_settings_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("সেটিংস সংরক্ষণ করুন (Save)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    }
                }
            }
        }



        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "রিসেট অপশন (Reset Data):",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "রিসেট চাপলে সমস্ত সেভ করা তথ্য ও প্রিসেট মুছে গিয়ে অ্যাপ সম্পূর্ণ খালি অবস্থায় ফিরে যাবে। কোনো ডিফল্ট ডাটা আসবে না।",
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )

                Spacer(modifier = Modifier.height(12.dp))

                var showResetConfirmDialog by remember { mutableStateOf(false) }

                Button(
                    onClick = { showResetConfirmDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaroonHeaderColor, contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 46.dp)
                        .testTag("reset_template_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "সমস্ত তথ্য সম্পূর্ণ খালি অবস্থায় রিসেট করুন",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                }

                if (showResetConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = { showResetConfirmDialog = false },
                        icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaroonHeaderColor) },
                        title = { Text("নিশ্চিত করুন", fontWeight = FontWeight.Bold) },
                        text = {
                            Text("এই কাজটি করলে প্রতিষ্ঠানের নাম, সাব-টাইটেল, দ্রুত আইটেম প্রিসেট এবং অন্যান্য সব সেটিংস স্থায়ীভাবে মুছে যাবে। এটি ফিরিয়ে আনা যাবে না। (সংরক্ষিত পুরনো বিলের হিসাব এতে মুছবে না।) আপনি কি নিশ্চিত?")
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                showResetConfirmDialog = false
                                onResetAllData()
                            }) {
                                Text("হ্যাঁ, রিসেট করুন", color = MaroonHeaderColor, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showResetConfirmDialog = false }) {
                                Text("বাতিল")
                            }
                        }
                    )
                }
            }
        }
    }
}
