package com.example.ui

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MedicalRecordEntity
import com.example.data.PresetMedicalCodeEntity
import com.example.ui.theme.DarkForestGreen
import com.example.ui.theme.HeadingFontFamily
import com.example.util.BengaliUtils
import com.example.util.MedicalPrintUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalWorkScreen(
    viewModel: MedicalWorkViewModel,
    onNavigateBack: () -> Unit,
    onOpenGlobalSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val records by viewModel.recordsForSelectedDate.collectAsStateWithLifecycle()
    val presetCodes by viewModel.presetCodes.collectAsStateWithLifecycle()
    val nextSuggestedId by viewModel.nextSuggestedPatientId.collectAsStateWithLifecycle()

    // Form states
    var inputPatientId by remember { mutableStateOf("") }
    var inputCode by remember { mutableStateOf("") }
    var inputPatientName by remember { mutableStateOf("") }
    var isCodeDropdownExpanded by remember { mutableStateOf(false) }
    var quickCodeInputText by remember { mutableStateOf("") }

    // Synchronize auto-suggested ID when user hasn't typed anything
    LaunchedEffect(nextSuggestedId, records.size) {
        if (inputPatientId.isBlank()) {
            inputPatientId = nextSuggestedId
        }
    }

    // Dialog states
    val clipboardManager = LocalClipboardManager.current
    var showAutoSequenceDialog by remember { mutableStateOf(false) }
    var showBulkPasteDialog by remember { mutableStateOf(false) }
    var showQuickCodePasteDialog by remember { mutableStateOf(false) }
    var showCodeManagerDialog by remember { mutableStateOf(false) }
    var showSummaryDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<MedicalRecordEntity?>(null) }
    var showTopMenu by remember { mutableStateOf(false) }

    BackHandler {
        onNavigateBack()
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // System Date Picker
    val openDatePicker = {
        try {
            val cal = Calendar.getInstance()
            val parts = selectedDate.split("-")
            val y = parts.getOrNull(0)?.toIntOrNull() ?: cal.get(Calendar.YEAR)
            val m = (parts.getOrNull(1)?.toIntOrNull() ?: (cal.get(Calendar.MONTH) + 1)) - 1
            val d = parts.getOrNull(2)?.toIntOrNull() ?: cal.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(context, { _, year, month, dayOfMonth ->
                val formatted = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                viewModel.setSelectedDate(formatted)
            }, y, m, d).show()
        } catch (e: Exception) {
            // fallback
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "মেডিকেল ওয়ার্ক রিপোর্ট",
                            fontFamily = HeadingFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        val formattedShort = MedicalPrintUtils.formatDateShort(selectedDate)
                        Text(
                            text = "তারিখ: $formattedShort (মোট: ${BengaliUtils.toBengaliDigits(records.size.toString())} টি)",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("medical_work_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "ফিরে যান",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Quick day switcher
                    IconButton(
                        onClick = { viewModel.shiftDate(-1) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "পূর্ববর্তী দিন", tint = Color.White)
                    }
                    IconButton(
                        onClick = openDatePicker,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "তারিখ নির্বাচন", tint = Color.White)
                    }
                    IconButton(
                        onClick = { viewModel.shiftDate(1) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "পরবর্তী দিন", tint = Color.White)
                    }

                    // Print / Share actions
                    IconButton(
                        onClick = {
                            if (records.isEmpty()) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("প্রিন্ট করার জন্য কোনো রেকর্ড নেই")
                                }
                            } else {
                                MedicalPrintUtils.printDailyReport(context, selectedDate, records)
                            }
                        },
                        modifier = Modifier.testTag("medical_print_top_button")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = "প্রিন্ট করুন", tint = Color.White)
                    }

                    IconButton(
                        onClick = {
                            if (records.isEmpty()) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("শেয়ার করার জন্য কোনো রেকর্ড নেই")
                                }
                            } else {
                                MedicalPrintUtils.shareDailyReportAsImage(context, selectedDate, records)
                            }
                        },
                        modifier = Modifier.testTag("medical_share_top_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "ছবি শেয়ার করুন", tint = Color.White)
                    }

                    Box {
                        IconButton(onClick = { showTopMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "মেনু", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = showTopMenu,
                            onDismissRequest = { showTopMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("🖼️ ছবি শেয়ার (Image)") },
                                onClick = {
                                    showTopMenu = false
                                    MedicalPrintUtils.shareDailyReportAsImage(context, selectedDate, records)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("📄 PDF শেয়ার (Document)") },
                                onClick = {
                                    showTopMenu = false
                                    MedicalPrintUtils.shareDailyReportAsPdf(context, selectedDate, records)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("📋 টেক্সট কপি করুন") },
                                onClick = {
                                    showTopMenu = false
                                    MedicalPrintUtils.copyTableAsText(context, selectedDate, records)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("{ } JSON কপি করুন") },
                                onClick = {
                                    showTopMenu = false
                                    MedicalPrintUtils.copyTableAsJson(context, selectedDate, records)
                                }
                            )
                            Divider()
                            DropdownMenuItem(
                                text = { Text("📅 আজকের তারিখে যান") },
                                onClick = {
                                    showTopMenu = false
                                    viewModel.setToday()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("📊 কোড বিশ্লেষণ ও পরিসংখ্যান") },
                                onClick = {
                                    showTopMenu = false
                                    showSummaryDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("🏷️ প্রিসেট কোডসমূহ পরিচালনা") },
                                onClick = {
                                    showTopMenu = false
                                    showCodeManagerDialog = true
                                }
                            )
                            if (records.isNotEmpty()) {
                                Divider()
                                DropdownMenuItem(
                                    text = { Text("🗑️ আজকের সব রেকর্ড মুছুন", color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showTopMenu = false
                                        showDeleteConfirmDialog = true
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkForestGreen,
                    titleContentColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (records.isNotEmpty()) {
                SurfaceBottomBar(
                    recordsCount = records.size,
                    onPrint = { MedicalPrintUtils.printDailyReport(context, selectedDate, records) },
                    onShareImage = { MedicalPrintUtils.shareDailyReportAsImage(context, selectedDate, records) },
                    onSharePdf = { MedicalPrintUtils.shareDailyReportAsPdf(context, selectedDate, records) },
                    onCopyText = { MedicalPrintUtils.copyTableAsText(context, selectedDate, records) }
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Fast Add Form Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("medical_add_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "নতুন এন্ট্রি ফর্ম",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "তারিখ: ${MedicalPrintUtils.formatDateShort(selectedDate)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Row 1: ID and Code
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = inputPatientId,
                                onValueChange = { inputPatientId = it.uppercase() },
                                label = { Text("ID (রোগী আইডি)") },
                                placeholder = { Text("যেমন AB260948") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Characters,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Right) }
                                ),
                                trailingIcon = {
                                    if (inputPatientId.isNotBlank()) {
                                        IconButton(onClick = { inputPatientId = "" }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Clear, contentDescription = "মুছুন", modifier = Modifier.size(16.dp))
                                        }
                                    } else {
                                        IconButton(
                                            onClick = {
                                                val clip = clipboardManager.getText()?.text?.trim() ?: ""
                                                if (clip.isNotBlank()) {
                                                    inputPatientId = clip.uppercase()
                                                }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.ContentPaste,
                                                contentDescription = "ক্লিপবোর্ড থেকে আইডি পেস্ট",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1.1f)
                                    .testTag("input_patient_id")
                            )

                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = inputCode,
                                    onValueChange = { inputCode = it.uppercase() },
                                    label = { Text("কোড") },
                                    placeholder = { Text("যেমন AF07") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Characters,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                    ),
                                    trailingIcon = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(end = 2.dp)
                                        ) {
                                            if (inputCode.isNotBlank()) {
                                                IconButton(onClick = { inputCode = "" }, modifier = Modifier.size(24.dp)) {
                                                    Icon(Icons.Default.Clear, contentDescription = "মুছুন", modifier = Modifier.size(16.dp))
                                                }
                                            }
                                            IconButton(
                                                onClick = { isCodeDropdownExpanded = !isCodeDropdownExpanded },
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .testTag("btn_toggle_code_dropdown")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ArrowDropDown,
                                                    contentDescription = "কোড তালিকা",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_code")
                                 )

                                DropdownMenu(
                                    expanded = isCodeDropdownExpanded,
                                    onDismissRequest = { isCodeDropdownExpanded = false },
                                    modifier = Modifier
                                        .widthIn(min = 250.dp, max = 320.dp)
                                        .heightIn(max = 400.dp)
                                ) {
                                    // Dropdown Menu Header: Quick Add Code with "+" Button
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "কোড নির্বাচন ও নতুন কোড যোগ",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            OutlinedTextField(
                                                value = quickCodeInputText,
                                                onValueChange = { quickCodeInputText = it.uppercase() },
                                                placeholder = { Text("নতুন কোড...", fontSize = 12.sp) },
                                                singleLine = true,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(48.dp),
                                                keyboardOptions = KeyboardOptions(
                                                    capitalization = KeyboardCapitalization.Characters,
                                                    imeAction = ImeAction.Done
                                                ),
                                                keyboardActions = KeyboardActions(
                                                    onDone = {
                                                        val trimmed = quickCodeInputText.trim()
                                                        if (trimmed.isNotBlank()) {
                                                            if (trimmed.contains(",") || trimmed.contains(" ") || trimmed.contains("\n")) {
                                                                viewModel.addMultiplePresetCodes(trimmed)
                                                            } else {
                                                                viewModel.addPresetCode(trimmed)
                                                            }
                                                            inputCode = trimmed.split(Regex("[,;\\s]+")).firstOrNull()?.uppercase() ?: trimmed.uppercase()
                                                            quickCodeInputText = ""
                                                        }
                                                    }
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            IconButton(
                                                onClick = {
                                                    val trimmed = quickCodeInputText.trim()
                                                    if (trimmed.isNotBlank()) {
                                                        if (trimmed.contains(",") || trimmed.contains(" ") || trimmed.contains("\n")) {
                                                            viewModel.addMultiplePresetCodes(trimmed)
                                                        } else {
                                                            viewModel.addPresetCode(trimmed)
                                                        }
                                                        inputCode = trimmed.split(Regex("[,;\\s]+")).firstOrNull()?.uppercase() ?: trimmed.uppercase()
                                                        quickCodeInputText = ""
                                                    }
                                                },
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.primary)
                                                    .testTag("dropdown_add_code_plus_btn")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "কোড যোগ করুন",
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }

                                    Divider()

                                    // List of preset codes
                                    if (presetCodes.isEmpty()) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    "কোনো কোড নেই। উপরে লিখে + চাপুন।",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            },
                                            onClick = { }
                                        )
                                    } else {
                                        presetCodes.forEach { preset ->
                                            val isSelected = inputCode.equals(preset.code, ignoreCase = true)
                                            DropdownMenuItem(
                                                text = {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            if (isSelected) {
                                                                Icon(
                                                                    Icons.Default.Check,
                                                                    contentDescription = "নির্বাচিত",
                                                                    tint = MaterialTheme.colorScheme.primary,
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                            }
                                                            Text(
                                                                text = preset.code,
                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                                fontSize = 14.sp
                                                            )
                                                        }
                                                        if (!preset.name.isNullOrBlank()) {
                                                            Text(
                                                                text = preset.name,
                                                                fontSize = 11.sp,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                },
                                                onClick = {
                                                    inputCode = preset.code
                                                    isCodeDropdownExpanded = false
                                                },
                                                trailingIcon = {
                                                    IconButton(
                                                        onClick = {
                                                            viewModel.deletePresetCode(preset.code)
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Delete,
                                                            contentDescription = "মুছুন",
                                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Row 2: Name and Add button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val onAddRecord = {
                                if (inputPatientId.isNotBlank() || inputCode.isNotBlank()) {
                                    val targetId = inputPatientId.ifBlank { nextSuggestedId }
                                    val codeCount = inputCode.split(Regex("[,;\\n\\r]+")).count { it.isNotBlank() }.coerceAtLeast(1)
                                    viewModel.addRecord(targetId, inputCode, inputPatientName)
                                    inputPatientId = viewModel.getNextIdAfter(targetId, codeCount)
                                    inputCode = ""
                                    inputPatientName = ""
                                    focusManager.clearFocus()
                                }
                            }

                            OutlinedTextField(
                                value = inputPatientName,
                                onValueChange = { inputPatientName = it.uppercase() },
                                label = { Text("নাম (Patient Name)") },
                                placeholder = { Text("যেমন TUHIN ALI") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Characters,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { onAddRecord() }
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_patient_name")
                            )

                            Button(
                                onClick = { onAddRecord() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .height(54.dp)
                                    .testTag("button_add_medical_record")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "যোগ করুন")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("যোগ করুন", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 2. Quick Utilities Toolbar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showAutoSequenceDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_auto_sequence")
                    ) {
                        Icon(Icons.Default.FormatListNumbered, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("অটো সিকোয়েন্স", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { showBulkPasteDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_bulk_paste")
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("বাল্ক পেস্ট", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { showSummaryDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_code_summary")
                    ) {
                        Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("কোড পরিসংখ্যান", fontSize = 12.sp)
                    }
                }
            }

            // 3. Daily Records Table (Matches sample document)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📋 আজকের রিপোর্ট টেবিল",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "মোট: ${BengaliUtils.toBengaliDigits(records.size.toString())} টি এন্ট্রি",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (records.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "এই তারিখে কোনো রেকর্ড নেই",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "উপরের ফর্ম ব্যবহার করে আইডি, কোড ও নাম যোগ করুন বা 'অটো সিকোয়েন্স' চাপুন।",
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            } else {
                // Table Header Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ক্রমিক",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(48.dp)
                            )
                            Divider(modifier = Modifier.height(20.dp).width(1.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                            Text(
                                text = "ID",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1.1f)
                            )
                            Divider(modifier = Modifier.height(20.dp).width(1.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                            Text(
                                text = "কোড",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Divider(modifier = Modifier.height(20.dp).width(1.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                            Text(
                                text = "নাম",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1.3f)
                            )
                            Divider(modifier = Modifier.height(20.dp).width(1.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                            Text(
                                text = "অ্যাকশন",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(60.dp)
                            )
                        }
                    }
                }

                // Table Rows
                itemsIndexed(records) { index, record ->
                    val isEven = index % 2 == 0
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editingRecord = record },
                        shape = RoundedCornerShape(0.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isEven) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. ক্রমিক
                            Text(
                                text = BengaliUtils.toBengaliDigits((index + 1).toString()),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(48.dp)
                            )
                            Divider(modifier = Modifier.height(24.dp).width(0.5.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                            // 2. ID
                            Text(
                                text = record.patientId.uppercase(),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1.1f)
                            )
                            Divider(modifier = Modifier.height(24.dp).width(0.5.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                            // 3. কোড
                            Text(
                                text = record.code.uppercase(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                            Divider(modifier = Modifier.height(24.dp).width(0.5.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                            // 4. নাম
                            Text(
                                text = record.patientName.uppercase().ifBlank { "-" },
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1.3f)
                            )
                            Divider(modifier = Modifier.height(24.dp).width(0.5.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                            // 5. Actions (Edit & Delete)
                            Row(
                                modifier = Modifier.width(60.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                IconButton(
                                    onClick = { editingRecord = record },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "সম্পাদনা",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.deleteRecord(record.id) },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "মুছুন",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Table Bottom space for comfortable scrolling
                item {
                    Spacer(modifier = Modifier.height(70.dp))
                }
            }
        }
    }

    // --- Dialogs ---

    // 1. Edit Record Dialog
    editingRecord?.let { record ->
        var editId by remember { mutableStateOf(record.patientId) }
        var editCode by remember { mutableStateOf(record.code) }
        var editName by remember { mutableStateOf(record.patientName) }

        AlertDialog(
            onDismissRequest = { editingRecord = null },
            title = { Text("রেকর্ড সম্পাদনা", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editId,
                        onValueChange = { editId = it.uppercase() },
                        label = { Text("ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editCode,
                        onValueChange = { editCode = it.uppercase() },
                        label = { Text("কোড") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it.uppercase() },
                        label = { Text("নাম") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateRecord(record.id, editId, editCode, editName)
                        editingRecord = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("সংরক্ষণ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingRecord = null }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // 2. Auto Sequence Dialog
    if (showAutoSequenceDialog) {
        var startIdInput by remember { mutableStateOf(nextSuggestedId) }
        var countInput by remember { mutableStateOf("10") }
        var defaultCodeInput by remember { mutableStateOf(inputCode) }

        AlertDialog(
            onDismissRequest = { showAutoSequenceDialog = false },
            title = { Text("অটো সিকোয়েন্স তৈরি", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "পরপর ক্রমিক আইডি অটোমেটিকভাবে তৈরি করুন।",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = startIdInput,
                        onValueChange = { startIdInput = it.uppercase() },
                        label = { Text("প্রারম্ভিক আইডি") },
                        placeholder = { Text("যেমন AB260948") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = countInput,
                        onValueChange = { countInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("মোট এন্ট্রি সংখ্যা") },
                        placeholder = { Text("যেমন 10") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = defaultCodeInput,
                        onValueChange = { defaultCodeInput = it.uppercase() },
                        label = { Text("ডিফল্ট কোড") },
                        placeholder = { Text("যেমন AF07") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val count = countInput.toIntOrNull() ?: 1
                        viewModel.generateAutoSequence(startIdInput, count, defaultCodeInput)
                        showAutoSequenceDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("তৈরি করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAutoSequenceDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // 3. Bulk Paste Dialog
    if (showBulkPasteDialog) {
        var rawTextInput by remember { mutableStateOf("") }
        val isJsonDetected = remember(rawTextInput) {
            val t = rawTextInput.trim()
            (t.contains("{") && t.contains("}")) || (t.contains("[") && t.contains("]"))
        }

        AlertDialog(
            onDismissRequest = { showBulkPasteDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ContentPaste, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("বাল্ক টেক্সট / কোড পেস্ট", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "যেকোনো ফরম্যাটে পেস্ট করুন:\n• JSON কোড (যেমন: {\"date\": \"...\", \"data\": [...]})\n• শুধুমাত্র কোড (যেমন: AF07, MD-01, J007, USG, CBC)\n• আইডি ও কোড (যেমন: AB260948 AF07 TUHIN)\nস্বয়ংক্রিয়ভাবে আইডি, কোড ও নাম টেবিলে যুক্ত হবে।",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            val clip = clipboardManager.getText()?.text?.trim() ?: ""
                            if (clip.isNotBlank()) {
                                rawTextInput = if (rawTextInput.isBlank()) clip else "$rawTextInput\n$clip"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("📋 ক্লিপবোর্ড থেকে সরাসরি পেস্ট করুন", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    if (isJsonDetected) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "JSON ডেটা সনাক্ত হয়েছে: তারিখ ও টেবিল স্বয়ংক্রিয়ভাবে লোড হবে",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = rawTextInput,
                        onValueChange = { rawTextInput = it },
                        placeholder = { Text("এখানে যেকোনো টেক্সট বা কোড পেস্ট করুন...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (rawTextInput.isNotBlank()) {
                            viewModel.bulkAddFromText(rawTextInput)
                            showBulkPasteDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("ইমপোর্ট করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkPasteDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // 3b. Quick Code Paste Dialog (Directly paste and register multiple codes)
    if (showQuickCodePasteDialog) {
        var rawCodesToPaste by remember { mutableStateOf("") }
        var alsoAddToReport by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showQuickCodePasteDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ContentPaste, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("যেকোনো কোড পেস্ট ও যোগ করুন", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "এখানে যেকোনো এক বা একাধিক কোড পেস্ট করুন (যেমন: AF07, MD-01, 101, USG, CBC)। কমা, স্পেস বা নতুন লাইনে যত কোডই থাকবে সবগুলো এক ক্লিকে কুইক তালিকায় যোগ হয়ে যাবে।",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            val clip = clipboardManager.getText()?.text?.trim() ?: ""
                            if (clip.isNotBlank()) {
                                rawCodesToPaste = if (rawCodesToPaste.isBlank()) clip else "$rawCodesToPaste, $clip"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("📋 ক্লিপবোর্ড থেকে পেস্ট করুন", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedTextField(
                        value = rawCodesToPaste,
                        onValueChange = { rawCodesToPaste = it.uppercase() },
                        placeholder = { Text("যেমন: AF07, MD-01, J007, USG, CBC, 101...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { alsoAddToReport = !alsoAddToReport }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = alsoAddToReport,
                            onCheckedChange = { alsoAddToReport = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("একই সাথে আজকের রিপোর্টেও রেকর্ড যোগ করুন (অটো আইডি সহ)", fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (rawCodesToPaste.isNotBlank()) {
                            viewModel.addMultiplePresetCodes(rawCodesToPaste)
                            if (alsoAddToReport) {
                                viewModel.bulkAddFromText(rawCodesToPaste)
                            }
                            showQuickCodePasteDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("যোগ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickCodePasteDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // 4. Code Summary Dialog
    if (showSummaryDialog) {
        val codeGroups = records.groupBy { it.code.trim().uppercase() }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }

        AlertDialog(
            onDismissRequest = { showSummaryDialog = false },
            title = { Text("📊 কোড সারসংক্ষেপ ও বিশ্লেষণ", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "তারিখ: ${MedicalPrintUtils.formatDateShort(selectedDate)}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "সর্বমোট রোগী/রেকর্ড সংখ্যা: ${BengaliUtils.toBengaliDigits(records.size.toString())} টি",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )
                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    if (codeGroups.isEmpty()) {
                        Text("কোনো রেকর্ড নেই", fontSize = 13.sp)
                    } else {
                        codeGroups.forEach { (code, count) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (code.isBlank()) "[খালি কোড]" else code,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "${BengaliUtils.toBengaliDigits(count.toString())} টি",
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSummaryDialog = false }) {
                    Text("ঠিক আছে")
                }
            }
        )
    }

    // 5. Preset Code Manager Dialog
    if (showCodeManagerDialog) {
        var newCodeToAdd by remember { mutableStateOf("") }
        var newCodeName by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCodeManagerDialog = false },
            title = { Text("🏷️ প্রিসেট কোডসমূহ পরিচালনা", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newCodeToAdd,
                            onValueChange = { newCodeToAdd = it.uppercase() },
                            placeholder = { Text("কোড বা কমা দিয়ে একাধিক কোড") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                if (newCodeToAdd.isNotBlank()) {
                                    if (newCodeToAdd.contains(",") || newCodeToAdd.contains(" ") || newCodeToAdd.contains("\n")) {
                                        viewModel.addMultiplePresetCodes(newCodeToAdd)
                                    } else {
                                        viewModel.addPresetCode(newCodeToAdd, newCodeName)
                                    }
                                    newCodeToAdd = ""
                                    newCodeName = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text("যোগ")
                        }
                    }

                    // Direct Paste from Clipboard Button
                    Button(
                        onClick = {
                            val clip = clipboardManager.getText()?.text?.trim() ?: ""
                            if (clip.isNotBlank()) {
                                viewModel.addMultiplePresetCodes(clip)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("📋 ক্লিপবোর্ড থেকে সব কোড যোগ করুন", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    Text("বিদ্যমান কোডের তালিকা (${presetCodes.size} টি):", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    presetCodes.forEach { preset ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(preset.code, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            IconButton(
                                onClick = { viewModel.deletePresetCode(preset.code) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "মুছুন", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCodeManagerDialog = false }) {
                    Text("সম্পন্ন")
                }
            }
        )
    }

    // 6. Delete Confirm Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("সব রেকর্ড মুছে ফেলা নিশ্চিত করুন") },
            text = { Text("আপনি কি নিশ্চিত যে আপনি ${MedicalPrintUtils.formatDateShort(selectedDate)} তারিখের সব রেকর্ড মুছে ফেলতে চান? এটি আর ফিরিয়ে আনা যাবে না।") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteRecordsForCurrentDate()
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("হ্যাঁ, সব মুছুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

@Composable
private fun SurfaceBottomBar(
    recordsCount: Int,
    onPrint: () -> Unit,
    onShareImage: () -> Unit,
    onSharePdf: () -> Unit,
    onCopyText: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Print button
            Button(
                onClick = onPrint,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1.2f)
                    .height(42.dp)
                    .testTag("btn_bottom_print")
            ) {
                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("প্রিন্ট", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            // Share Image
            OutlinedButton(
                onClick = onShareImage,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .testTag("btn_bottom_share_image")
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text("ছবি", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            // Share PDF
            OutlinedButton(
                onClick = onSharePdf,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .testTag("btn_bottom_share_pdf")
            ) {
                Text("PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            // Copy Text
            IconButton(
                onClick = onCopyText,
                modifier = Modifier.size(42.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "কপি", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
