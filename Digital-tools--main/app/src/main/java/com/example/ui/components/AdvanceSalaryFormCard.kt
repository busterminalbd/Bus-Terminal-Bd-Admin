package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AdvanceSalaryFormState
import com.example.ui.theme.HeadingFontFamily
import com.example.util.EnglishUtils

@Composable
fun AdvanceSalaryFormCard(
    state: AdvanceSalaryFormState,
    presetReasons: List<String>,
    presetDesignations: List<String>,
    onAddReasonPreset: (String) -> Unit,
    onEditReasonPreset: (String, String) -> Unit,
    onDeleteReasonPreset: (String) -> Unit,
    onAddDesignationPreset: (String) -> Unit,
    onEditDesignationPreset: (String, String) -> Unit,
    onDeleteDesignationPreset: (String) -> Unit,
    onCompanyNameChange: (String) -> Unit,
    onCompanySubtitleChange: (String) -> Unit,
    onApplicationNoChange: (String) -> Unit,
    onDateClick: () -> Unit,
    onApplicantNameChange: (String) -> Unit,
    onDesignationChange: (String) -> Unit,
    onContactNumberChange: (String) -> Unit,
    onMonthlySalaryChange: (String) -> Unit,
    onAdvanceAmountChange: (String) -> Unit,
    onAdvanceAmountInWordsChange: (String) -> Unit,
    onReasonChange: (String) -> Unit,
    onRepaymentTypeChange: (String) -> Unit,
    onInstallmentCountChange: (Int) -> Unit,
    onDeductionStartMonthChange: (String) -> Unit,
    onPreviousAdvancePendingChange: (String) -> Unit,
    onRemarksChange: (String) -> Unit,
    onStatusChange: (String) -> Unit,
    onShowSignaturesChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDesignationDropdown by remember { mutableStateOf(false) }
    var showReasonDropdown by remember { mutableStateOf(false) }

    var showManageDesignationsDialog by remember { mutableStateOf(false) }
    var showManageReasonsDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("advance_salary_form_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Banner: Official Executive Requisition Voucher Title
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.secondary,
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    )
                    .padding(vertical = 16.dp, horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "ADVANCE SALARY APPLICATION",
                        fontFamily = HeadingFontFamily,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Staff & Employee Advance Requisition Voucher",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Application Date Picker
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                    .clickable { onDateClick() }
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Select Date",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Application Date", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = state.dateString.ifBlank { "Select Date" },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(6.dp))

            // SECTION 1: Employee Details
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "1. Employee Details",
                    fontFamily = HeadingFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                IconButton(
                    onClick = { showManageDesignationsDialog = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = "Manage Designations",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = state.applicantName,
                onValueChange = onApplicantNameChange,
                label = { Text("Employee Name *") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_applicant_name"),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Editable Designation with Dropdown Menu
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = state.designation,
                    onValueChange = onDesignationChange,
                    label = { Text("Designation (Type or Select) *") },
                    leadingIcon = { Icon(Icons.Default.Work, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        IconButton(onClick = { showDesignationDropdown = true }) {
                            Icon(Icons.Default.ExpandMore, contentDescription = "Select Designation")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_designation"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                DropdownMenu(
                    expanded = showDesignationDropdown,
                    onDismissRequest = { showDesignationDropdown = false }
                ) {
                    presetDesignations.forEach { desig ->
                        DropdownMenuItem(
                            text = { Text(desig) },
                            onClick = {
                                onDesignationChange(desig)
                                showDesignationDropdown = false
                            }
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = {
                            Text(
                                "+ Manage / Add Designations",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        onClick = {
                            showDesignationDropdown = false
                            showManageDesignationsDialog = true
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = state.contactNumber,
                    onValueChange = onContactNumberChange,
                    label = { Text("Mobile No (Optional)") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    placeholder = { Text("Omit if not needed") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = state.monthlySalaryInput,
                    onValueChange = onMonthlySalaryChange,
                    label = { Text("Monthly Salary (৳) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f).testTag("input_monthly_salary"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Previous Advance Taken Input
            OutlinedTextField(
                value = state.previousAdvancePendingInput,
                onValueChange = onPreviousAdvancePendingChange,
                label = { Text("Previous Advance Taken (৳) / পূর্বে নেওয়া অগ্রিম") },
                placeholder = { Text("0 if no previous advance taken") },
                leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_previous_advance"),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            val maxEligibleAdvance = if (state.monthlySalary > 0.0) {
                (state.monthlySalary - state.previousAdvancePending).coerceAtLeast(0.0)
            } else 0.0
            val isOverLimit = state.monthlySalary > 0.0 && state.advanceAmount > maxEligibleAdvance

            // Salary & Advance Limit Calculation Box
            if (state.monthlySalary > 0.0) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isOverLimit) Color(0xFFFFEBEE) else Color(0xFFF1F8E9)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isOverLimit) Color(0xFFEF9A9A) else Color(0xFFC8E6C9)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "বেতন: ৳ ${EnglishUtils.formatEnglishCurrency(state.monthlySalary)}" +
                                        if (state.previousAdvancePending > 0.0) " • পূর্বের অগ্রিম: ৳ ${EnglishUtils.formatEnglishCurrency(state.previousAdvancePending)}" else "",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "সর্বোচ্চ সীমা: ৳ ${EnglishUtils.formatEnglishCurrency(maxEligibleAdvance)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOverLimit) Color(0xFFC62828) else Color(0xFF2E7D32)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(6.dp))

            // SECTION 2: Advance Details & Repayment
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "2. Advance Amount & Repayment Terms",
                    fontFamily = HeadingFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE57373)
                )

                IconButton(
                    onClick = { showManageReasonsDialog = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = "Manage Reasons",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))

            // Advance Amount Input
            OutlinedTextField(
                value = state.advanceAmountInput,
                onValueChange = onAdvanceAmountChange,
                label = { Text("Requested Advance Amount (৳) *") },
                leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null, tint = if (isOverLimit) Color(0xFFC62828) else Color(0xFFC62828)) },
                isError = isOverLimit,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_advance_amount"),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isOverLimit) Color(0xFFC62828) else Color(0xFFC62828),
                    unfocusedBorderColor = if (isOverLimit) Color(0xFFEF5350) else Color(0xFFE57373),
                    errorBorderColor = Color(0xFFC62828)
                )
            )

            // Over-limit Warning Card
            if (isOverLimit) {
                Spacer(modifier = Modifier.height(6.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFEBEE))
                        .border(1.dp, Color(0xFFEF5350), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "⚠️ সর্বোচ্চ সীমা অতিক্রম করেছে! আপনি সর্বোচ্চ ৳ ${EnglishUtils.formatEnglishCurrency(maxEligibleAdvance)}/- অ্যাডভান্স নিতে পারবেন।",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC62828)
                    )
                    Text(
                        text = "হিসাব: মাসিক বেতন (৳ ${EnglishUtils.formatEnglishCurrency(state.monthlySalary)}) - পূর্বে নেওয়া অগ্রিম (৳ ${EnglishUtils.formatEnglishCurrency(state.previousAdvancePending)}) = অবশিষ্ট ৳ ${EnglishUtils.formatEnglishCurrency(maxEligibleAdvance)}",
                        fontSize = 11.sp,
                        color = Color(0xFFB71C1C)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = {
                            val maxStr = if (maxEligibleAdvance > 0) maxEligibleAdvance.toLong().toString() else "0"
                            onAdvanceAmountChange(maxStr)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC62828))
                    ) {
                        Text(
                            text = "সর্বোচ্চ অনুমোদিত সীমা বসান (৳ ${EnglishUtils.formatEnglishCurrency(maxEligibleAdvance)})",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Live in-words display
            val inWords = if (state.advanceAmountInWords.isNotBlank()) {
                state.advanceAmountInWords
            } else {
                EnglishUtils.amountToEnglishWords(state.advanceAmount)
            }

            if (inWords.isNotBlank() && !isOverLimit) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFF3E0))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "In Words: $inWords",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE65100)
                    )
                }
            }

            // Net Payable Salary Display Box
            if (state.monthlySalary > 0.0 && !isOverLimit) {
                val remainingSalary = (state.monthlySalary - state.previousAdvancePending - state.advanceAmount).coerceAtLeast(0.0)
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE8F5E9))
                        .border(1.dp, Color(0xFFA5D6A7), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Remaining Salary / অবশিষ্ট বেতন:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF2E7D32)
                            )
                            if (state.advanceAmount > 0.0) {
                                Text(
                                    text = "বেতন (৳${EnglishUtils.formatEnglishCurrency(state.monthlySalary)}) - পূর্বের অগ্রিম (৳${EnglishUtils.formatEnglishCurrency(state.previousAdvancePending)}) - বর্তমান অগ্রিম (৳${EnglishUtils.formatEnglishCurrency(state.advanceAmount)})",
                                    fontSize = 10.5.sp,
                                    color = Color(0xFF388E3C)
                                )
                            }
                        }
                        Text(
                            text = "৳ ${EnglishUtils.formatEnglishCurrency(remainingSalary)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Reason for Advance (Editable + Dropdown)
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = state.reason,
                    onValueChange = onReasonChange,
                    label = { Text("Reason for Advance (Type or Select) *") },
                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        IconButton(onClick = { showReasonDropdown = true }) {
                            Icon(Icons.Default.ExpandMore, contentDescription = "Select Reason")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_reason"),
                    singleLine = false,
                    maxLines = 2,
                    shape = RoundedCornerShape(10.dp)
                )

                DropdownMenu(
                    expanded = showReasonDropdown,
                    onDismissRequest = { showReasonDropdown = false }
                ) {
                    presetReasons.forEach { reasonText ->
                        DropdownMenuItem(
                            text = { Text(reasonText) },
                            onClick = {
                                onReasonChange(reasonText)
                                showReasonDropdown = false
                            }
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = {
                            Text(
                                "+ Manage / Add Reasons",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        onClick = {
                            showReasonDropdown = false
                            showManageReasonsDialog = true
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = state.deductionStartMonth,
                onValueChange = onDeductionStartMonthChange,
                label = { Text("Deduction Starts") },
                placeholder = { Text("e.g. Next Month") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(14.dp))

            // SECTION 3: Signatures & Layout Options
            Text(
                text = "3. Signatures & Controls",
                fontFamily = HeadingFontFamily,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Include Signature Lines",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Applicant, Accounts, and Authorized signature lines",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = state.showSignatures,
                    onCheckedChange = onShowSignaturesChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF1B5E20),
                        checkedTrackColor = Color(0xFFA5D6A7)
                    )
                )
            }
        }
    }

    // Dialog: Manage Designation Presets
    if (showManageDesignationsDialog) {
        ManagePresetsDialog(
            title = "Manage Designation Presets",
            presets = presetDesignations,
            onAdd = onAddDesignationPreset,
            onEdit = onEditDesignationPreset,
            onDelete = onDeleteDesignationPreset,
            onDismiss = { showManageDesignationsDialog = false }
        )
    }

    // Dialog: Manage Reason Presets
    if (showManageReasonsDialog) {
        ManagePresetsDialog(
            title = "Manage Reason Presets",
            presets = presetReasons,
            onAdd = onAddReasonPreset,
            onEdit = onEditReasonPreset,
            onDelete = onDeleteReasonPreset,
            onDismiss = { showManageReasonsDialog = false }
        )
    }
}

@Composable
private fun ManagePresetsDialog(
    title: String,
    presets: List<String>,
    onAdd: (String) -> Unit,
    onEdit: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newPresetText by remember { mutableStateOf("") }
    var editingPreset by remember { mutableStateOf<String?>(null) }
    var editedText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                // Add New Field
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = newPresetText,
                        onValueChange = { newPresetText = it },
                        placeholder = { Text("Add new...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Button(
                        onClick = {
                            if (newPresetText.isNotBlank()) {
                                onAdd(newPresetText.trim())
                                newPresetText = ""
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
                    ) {
                        Text("Add")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                // Existing Presets List
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { item ->
                        if (editingPreset == item) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedTextField(
                                    value = editedText,
                                    onValueChange = { editedText = it },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Button(
                                    onClick = {
                                        if (editedText.isNotBlank()) {
                                            onEdit(item, editedText.trim())
                                            editingPreset = null
                                            editedText = ""
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Save")
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Row {
                                    IconButton(
                                        onClick = {
                                            editingPreset = item
                                            editedText = item
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { onDelete(item) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFD32F2F), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}
