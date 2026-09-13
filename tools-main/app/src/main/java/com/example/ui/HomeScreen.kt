package com.example.ui

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AppSplashScreen
import com.example.ui.components.BillHistoryList
import com.example.ui.components.GlobalSettingsScreen
import com.example.ui.components.MemoVoucherCard
import com.example.ui.components.OnboardingDialog
import com.example.ui.components.QuickPresetChips
import com.example.ui.components.SettingsScreen
import com.example.ui.components.ToolTopAppBar
import com.example.ui.components.ToolsHubScreen
import com.example.ui.components.VoucherPreviewDialog
import com.example.ui.theme.DarkForestGreen
import com.example.ui.theme.HeadingFontFamily
import com.example.ui.theme.LightForestGreen
import com.example.update.AppUpdateManager
import com.example.update.UpdateDialog
import com.example.update.UpdateInfo
import com.example.util.PrintMemoData
import com.example.util.PrintUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: FoodBillViewModel
) {
    val context = LocalContext.current
    val currentBillState by viewModel.currentBillState.collectAsStateWithLifecycle()
    val historyBills by viewModel.historyBills.collectAsStateWithLifecycle()
    val quickPresets by viewModel.quickPresets.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val themeColor by viewModel.themeColor.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val fontScale by viewModel.fontScale.collectAsStateWithLifecycle()
    val showOnboarding by viewModel.showOnboarding.collectAsStateWithLifecycle()

    val isEn = appLanguage == "en"

    var selectedTool by remember { mutableStateOf<String?>("food_bill") }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showGlobalSettings by remember { mutableStateOf(false) }
    var showMemoSettings by remember { mutableStateOf(false) }
    // Which bill type's header (title/subtitle/signature) is being edited on the memo
    // settings screen. Resets to whatever the live bill's type is each time the settings
    // screen is opened, then can be switched independently via the dropdown in the app bar
    // so the user can edit and save "যাতায়াত ভাড়া" and "খাবার বিল" settings separately.
    var memoSettingsBillType by remember(showMemoSettings) { mutableStateOf(currentBillState.billType) }
    var showPreviewDialog by remember { mutableStateOf(false) }
    var previewTopMemo by remember { mutableStateOf<PrintMemoData?>(null) }
    var previewBottomMemo by remember { mutableStateOf<PrintMemoData?>(null) }
    var isSplashLoading by remember { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val updateManager = remember { AppUpdateManager() }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateDialogInfoToShow by remember { mutableStateOf<UpdateInfo?>(null) }

    LaunchedEffect(Unit) {
        try {
            isCheckingUpdate = true
            val info = updateManager.checkForUpdate(context, forceCheck = false)
            updateInfo = info
            if (info.hasUpdate) {
                updateDialogInfoToShow = info
            }
        } catch (e: Exception) {
            // Keep app safe from network/parsing exceptions
        } finally {
            isCheckingUpdate = false
        }
    }

    BackHandler(enabled = showGlobalSettings || showMemoSettings || selectedTool != null) {
        if (showMemoSettings) {
            showMemoSettings = false
        } else if (showGlobalSettings) {
            showGlobalSettings = false
        } else if (selectedTool != null) {
            selectedTool = null
        }
    }

    LaunchedEffect(Unit) {
        delay(1200)
        isSplashLoading = false
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Android System Date Picker Dialog
    val showDatePicker = {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val formattedDay = String.format("%02d", dayOfMonth)
                val formattedMonth = String.format("%02d", month + 1)
                viewModel.updateDate("$formattedDay/$formattedMonth/$year")
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Crossfade(
        targetState = isSplashLoading,
        animationSpec = tween(600),
        label = "splash_fade"
    ) { loading ->
        if (loading) {
            AppSplashScreen(
                appName = "Digital Tool",
                subtitle = "স্মার্ট বিলিং সহকারী"
            )
        } else {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = baseDensity.density,
                    fontScale = baseDensity.fontScale * fontScale
                )
            ) {
            // Whether the title area should act as a quick "title type menu" — lets the user
            // jump between বাজার লিস্ট (market) and যাতায়াত ভাড়া (transport) mode right from
            // the app bar, without losing their place or needing to open the memo card first.
            val titleMenuEnabled = !showGlobalSettings && !showMemoSettings &&
                selectedTool == "food_bill" && selectedTab == 0
            var showTitleTypeMenu by remember { mutableStateOf(false) }

            if (showGlobalSettings) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = if (isEn) "Main App Settings" else "মেইন অ্যাপ সেটিংস",
                                    fontFamily = HeadingFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 19.sp
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = { showGlobalSettings = false }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "ফিরে যান",
                                        tint = Color.White
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                titleContentColor = Color.White
                            )
                        )
                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    GlobalSettingsScreen(
                        themeMode = themeMode,
                        themeColor = themeColor,
                        appLanguage = appLanguage,
                        fontScale = fontScale,
                        onThemeModeChange = { mode -> viewModel.setThemeMode(mode) },
                        onThemeColorChange = { color -> viewModel.setThemeColor(color) },
                        onLanguageChange = { lang -> viewModel.setAppLanguage(lang) },
                        onFontScaleChange = { scale -> viewModel.setFontScale(scale) },
                        onOpenMemoSettings = {
                            showGlobalSettings = false
                            selectedTool = "food_bill"
                            showMemoSettings = true
                        },
                        onDataImported = {
                            viewModel.onDataImported()
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            } else if (selectedTool == "advance_salary") {
                val advanceSalaryViewModel: AdvanceSalaryViewModel = viewModel()
                AdvanceSalaryScreen(
                    viewModel = advanceSalaryViewModel,
                    onNavigateBack = { selectedTool = null },
                    onOpenGlobalSettings = { showGlobalSettings = true }
                )
            } else if (selectedTool == "medical_work") {
                val medicalViewModel: MedicalWorkViewModel = viewModel()
                MedicalWorkScreen(
                    viewModel = medicalViewModel,
                    onNavigateBack = { selectedTool = null },
                    onOpenGlobalSettings = { showGlobalSettings = true }
                )
            } else {
            Scaffold(
                topBar = {
                    ToolTopAppBar(
                        title = {
                            val billTypeLabel = if (currentBillState.billType == "transport") {
                                if (isEn) "Transport Fare" else "যাতায়াত ভাড়া"
                            } else {
                                if (isEn) "Food Bill" else "খাবার বিল"
                            }
                            val memoSettingsTypeLabel = if (memoSettingsBillType == "transport") {
                                if (isEn) "Transport Fare" else "যাতায়াত ভাড়া"
                            } else {
                                if (isEn) "Food Bill" else "খাবার বিল"
                            }
                            val titleText = when {
                                showGlobalSettings -> if (isEn) "Main App Settings" else "মেইন অ্যাপ সেটিংস"
                                showMemoSettings -> if (isEn) "Memo Settings – $memoSettingsTypeLabel" else "মেমো হেডার সেটিংস – $memoSettingsTypeLabel"
                                selectedTool == "food_bill" -> when (selectedTab) {
                                    1 -> if (isEn) "Saved Records" else "সংরক্ষিত হিসাব"
                                    else -> billTypeLabel
                                }
                                else -> if (isEn) "Digital Tool" else "ডিজিটাল টুল"
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = titleText,
                                    fontFamily = HeadingFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 19.sp,
                                    modifier = if (titleMenuEnabled) {
                                        Modifier
                                            .clickable { showTitleTypeMenu = true }
                                            .testTag("title_type_menu_trigger")
                                    } else Modifier
                                )
                                if (titleMenuEnabled) {
                                    Box {
                                        IconButton(
                                            onClick = { showTitleTypeMenu = true },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = if (isEn) "Switch bill type" else "বিলের ধরন পরিবর্তন করুন",
                                                tint = Color.White
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = showTitleTypeMenu,
                                            onDismissRequest = { showTitleTypeMenu = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text(if (isEn) "🛒 Market List" else "🛒 বাজার লিস্ট") },
                                                onClick = {
                                                    showTitleTypeMenu = false
                                                    viewModel.setBillType("market")
                                                },
                                                modifier = Modifier.testTag("title_menu_market")
                                            )
                                            DropdownMenuItem(
                                                text = { Text(if (isEn) "🚌 Transport Fare" else "🚌 যাতায়াত ভাড়া") },
                                                onClick = {
                                                    showTitleTypeMenu = false
                                                    viewModel.setBillType("transport")
                                                },
                                                modifier = Modifier.testTag("title_menu_transport")
                                            )
                                            Divider()
                                            DropdownMenuItem(
                                                text = { Text(if (isEn) "✏️ Edit memo title" else "✏️ মেমোর টাইটেল সম্পাদনা") },
                                                onClick = {
                                                    showTitleTypeMenu = false
                                                    showMemoSettings = true
                                                },
                                                modifier = Modifier.testTag("title_menu_edit")
                                            )
                                        }
                                    }
                                }
                                if (showMemoSettings) {
                                    Box {
                                        var showMemoSettingsTypeMenu by remember { mutableStateOf(false) }
                                        IconButton(
                                            onClick = { showMemoSettingsTypeMenu = true },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .testTag("memo_settings_type_menu_trigger")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = if (isEn) "Switch settings for bill type" else "কোন বিলের সেটিংস সম্পাদনা করবেন তা নির্বাচন করুন",
                                                tint = Color.White
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = showMemoSettingsTypeMenu,
                                            onDismissRequest = { showMemoSettingsTypeMenu = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text(if (isEn) "🛒 Market List" else "🛒 বাজার লিস্ট") },
                                                onClick = {
                                                    showMemoSettingsTypeMenu = false
                                                    memoSettingsBillType = "market"
                                                },
                                                modifier = Modifier.testTag("memo_settings_type_market")
                                            )
                                            DropdownMenuItem(
                                                text = { Text(if (isEn) "🚌 Transport Fare" else "🚌 যাতায়াত ভাড়া") },
                                                onClick = {
                                                    showMemoSettingsTypeMenu = false
                                                    memoSettingsBillType = "transport"
                                                },
                                                modifier = Modifier.testTag("memo_settings_type_transport")
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        onNavigateBack = when {
                            showMemoSettings -> { { showMemoSettings = false } }
                            showGlobalSettings -> { { showGlobalSettings = false } }
                            selectedTool != null -> { { selectedTool = null } }
                            else -> null
                        },
                        onOpenGlobalSettings = { showGlobalSettings = true }
                    )
                },
                bottomBar = {
                    if (!showGlobalSettings && !showMemoSettings && selectedTool == "food_bill") {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ) {
                            val darkUnselectedColor = MaterialTheme.colorScheme.onSurfaceVariant

                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = { Icon(Icons.Default.Edit, contentDescription = if (isEn) "Daily Bill" else "দৈনিক বিল") },
                                label = { Text(if (isEn) "Daily Bill" else "দৈনিক বিল", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = darkUnselectedColor,
                                    unselectedTextColor = darkUnselectedColor
                                ),
                                modifier = Modifier.testTag("tab_daily_bill")
                            )

                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = { Icon(Icons.Default.History, contentDescription = if (isEn) "Saved Records" else "সংরক্ষিত হিসাব") },
                                label = { Text(if (isEn) "Saved Records" else "সংরক্ষিত হিসাব", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = darkUnselectedColor,
                                    unselectedTextColor = darkUnselectedColor
                                ),
                                modifier = Modifier.testTag("tab_saved_history")
                            )
                        }
                    }
                },
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    if (showMemoSettings) {
                        SettingsScreen(
                            state = currentBillState,
                            editingBillType = memoSettingsBillType,
                            getSavedSettingsForType = { type -> viewModel.getSavedSettingsForType(type) },
                            quickPresets = quickPresets,
                            onAddCustomPreset = { name, qty, rate, amount ->
                                viewModel.addCustomQuickPreset(name, qty, rate, amount)
                            },
                            onRemovePreset = { preset ->
                                viewModel.removeQuickPreset(preset)
                            },
                            onResetPresetsDefault = {
                                viewModel.resetQuickPresetsToDefault()
                            },
                            onSaveSettings = { type, center, sub, purchaser ->
                                viewModel.saveSettings(type, center, sub, purchaser)
                                showMemoSettings = false
                            },
                            onResetTemplate = {
                                viewModel.resetToInitialTemplate()
                                showMemoSettings = false
                            },
                            onResetAllData = {
                                viewModel.resetAllUserData()
                                showMemoSettings = false
                            },
                            onOpenGlobalSettings = {
                                showGlobalSettings = true
                            }
                        )
                    } else if (selectedTool == "food_bill") {
                        when (selectedTab) {
                            0 -> {
                                // Daily Bill Voucher Editor
                                val mainScrollState = rememberScrollState()
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(mainScrollState)
                                        .padding(12.dp)
                                ) {
                                    // Memo Paper Cash Voucher
                                    MemoVoucherCard(
                                        state = currentBillState,
                                        quickPresets = quickPresets,
                                        scrollState = mainScrollState,
                                        onCenterNameChange = { viewModel.updateCenterName(it) },
                                        onSubtitleChange = { viewModel.updateSubtitle(it) },
                                        onPresetClick = { name, qty, rate, amount ->
                                            viewModel.addQuickPresetItem(name, qty, rate, amount)
                                        },
                                        onAddCustomPreset = { name, qty, rate, amount ->
                                            viewModel.addCustomQuickPreset(name, qty, rate, amount)
                                        },
                                        onAddCustomPresetWithParent = { name, qty, rate, amount, parentId ->
                                            viewModel.addCustomQuickPreset(name, qty, rate, amount, parentId)
                                        },
                                        onAddFolder = { folderName, parentId ->
                                            viewModel.addQuickPresetFolder(folderName, parentId)
                                        },
                                        onUpdatePresetNode = { nodeId, name, qty, rate, amount, isFolder, parentId, children ->
                                            viewModel.updateQuickPresetNode(nodeId, name, qty, rate, amount, isFolder, parentId, children)
                                        },
                                        onRemovePreset = { preset ->
                                            viewModel.removeQuickPreset(preset)
                                        },
                                        onResetDefaults = {
                                            viewModel.resetQuickPresetsToDefault()
                                        },
                                        onReorderPreset = { from, to ->
                                            viewModel.reorderQuickPresets(from, to)
                                        },
                                        onReorderPresetWithParent = { parentId, from, to ->
                                            viewModel.reorderQuickPresets(parentId, from, to)
                                        },
                                        onUpdateDateClick = { showDatePicker() },
                                        onUpdateItemName = { id, name -> viewModel.updateItemName(id, name) },
                                        onUpdateItemQty = { id, qty -> viewModel.updateItemQuantity(id, qty) },
                                        onUpdateItemRate = { id, rate -> viewModel.updateItemRate(id, rate) },
                                        onUpdateItemTraveler = { id, name -> viewModel.updateItemTraveler(id, name) },
                                        onUpdateItemAmount = { id, amount -> viewModel.updateItemAmount(id, amount) },
                                        onRemoveItem = { id -> viewModel.removeItemRow(id) },
                                        onMoveItem = { from, to -> viewModel.moveItem(from, to) },
                                        onAddItemRow = { viewModel.addItemRow() },
                                        onPurchaserLabelChange = { viewModel.updatePurchaserLabel(it) },
                                        onBillTypeChange = { viewModel.setBillType(it) },
                                        onShowSignatureChange = { viewModel.setShowSignature(it) }
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Action Buttons Bar
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Print / Preview Button
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(42.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    brush = Brush.horizontalGradient(
                                                        colors = listOf(
                                                            MaterialTheme.colorScheme.secondary,
                                                            MaterialTheme.colorScheme.primary
                                                        )
                                                    )
                                                )
                                                .clickable {
                                                    // Auto-save so sharing/printing never requires a separate
                                                    // "সংরক্ষণ" tap first — the bill is safely persisted (or
                                                    // updated, if already saved) the moment share/preview opens.
                                                    viewModel.saveCurrentBill(notifyUser = false) { savedId ->
                                                        previewTopMemo = previewTopMemo?.copy(memoId = savedId)
                                                    }
                                                    previewTopMemo = PrintMemoData(
                                                        memoId = currentBillState.editingBillId,
                                                        centerName = currentBillState.centerName,
                                                        subtitle = currentBillState.subtitle,
                                                        dateString = currentBillState.dateString,
                                                        purchaserName = currentBillState.purchaserName,
                                                        purchaserLabel = currentBillState.purchaserLabel,
                                                        items = currentBillState.items.filter { it.name.isNotBlank() || it.amount > 0 },
                                                        totalAmount = currentBillState.totalAmount,
                                                        billType = currentBillState.billType,
                                                        showSignature = currentBillState.showSignature
                                                    )
                                                    previewBottomMemo = null
                                                    showPreviewDialog = true
                                                }
                                                .testTag("print_bill_button")
                                                .padding(horizontal = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "শেয়ার / প্রিভিউ",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }

                                        // Save Button
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(42.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    brush = Brush.horizontalGradient(
                                                        colors = listOf(
                                                            MaterialTheme.colorScheme.primary,
                                                            MaterialTheme.colorScheme.tertiary
                                                        )
                                                    )
                                                )
                                                .clickable { viewModel.saveCurrentBill() }
                                                .testTag("save_bill_button")
                                                .padding(horizontal = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "সংরক্ষণ",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }

                                        // Clear / New Button
                                        OutlinedButton(
                                            onClick = { viewModel.resetToInitialTemplate() },
                                            modifier = Modifier
                                                .height(42.dp)
                                                .testTag("clear_bill_button"),
                                            contentPadding = PaddingValues(horizontal = 8.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.Clear, contentDescription = "নতুন মেমো", tint = Color.DarkGray, modifier = Modifier.size(17.dp))
                                        }
                                    }
                                }
                            }

                            1 -> {
                                // History View
                                BillHistoryList(
                                    bills = historyBills,
                                    searchQuery = searchQuery,
                                    onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                                    onEditBill = { bill ->
                                        viewModel.loadBillForEditing(bill)
                                        selectedTab = 0
                                    },
                                    onPrintBill = { bill ->
                                        previewTopMemo = PrintMemoData(
                                            memoId = bill.id,
                                            centerName = bill.centerName,
                                            subtitle = bill.subtitle,
                                            dateString = bill.dateString,
                                            purchaserName = bill.purchaserName,
                                            purchaserLabel = currentBillState.purchaserLabel,
                                            items = bill.items.filter { it.name.isNotBlank() || it.amount > 0 },
                                            totalAmount = bill.totalAmount,
                                            billType = bill.billType,
                                            showSignature = bill.showSignature
                                        )
                                        previewBottomMemo = null
                                        showPreviewDialog = true
                                    },
                                    onPrintDualBills = { bill1, bill2 ->
                                        previewTopMemo = PrintMemoData(
                                            memoId = bill1.id,
                                            centerName = bill1.centerName,
                                            subtitle = bill1.subtitle,
                                            dateString = bill1.dateString,
                                            purchaserName = bill1.purchaserName,
                                            purchaserLabel = currentBillState.purchaserLabel,
                                            items = bill1.items.filter { it.name.isNotBlank() || it.amount > 0 },
                                            totalAmount = bill1.totalAmount,
                                            billType = bill1.billType,
                                            showSignature = bill1.showSignature
                                        )
                                        previewBottomMemo = PrintMemoData(
                                            memoId = bill2.id,
                                            centerName = bill2.centerName,
                                            subtitle = bill2.subtitle,
                                            dateString = bill2.dateString,
                                            purchaserName = bill2.purchaserName,
                                            purchaserLabel = currentBillState.purchaserLabel,
                                            items = bill2.items.filter { it.name.isNotBlank() || it.amount > 0 },
                                            totalAmount = bill2.totalAmount,
                                            billType = bill2.billType,
                                            showSignature = bill2.showSignature
                                        )
                                        showPreviewDialog = true
                                    },
                                    onSharePdfBill = { bill ->
                                        previewTopMemo = PrintMemoData(
                                            memoId = bill.id,
                                            centerName = bill.centerName,
                                            subtitle = bill.subtitle,
                                            dateString = bill.dateString,
                                            purchaserName = bill.purchaserName,
                                            purchaserLabel = currentBillState.purchaserLabel,
                                            items = bill.items.filter { it.name.isNotBlank() || it.amount > 0 },
                                            totalAmount = bill.totalAmount,
                                            billType = bill.billType,
                                            showSignature = bill.showSignature
                                        )
                                        previewBottomMemo = null
                                        showPreviewDialog = true
                                    },
                                    onDeleteBill = { id -> viewModel.deleteBill(id) }
                                )
                            }

                            2 -> {
                                // Settings View
                                SettingsScreen(
                                    state = currentBillState,
                                    editingBillType = memoSettingsBillType,
                                    getSavedSettingsForType = { type -> viewModel.getSavedSettingsForType(type) },
                                    quickPresets = quickPresets,
                                    onAddCustomPreset = { name, qty, rate, amount ->
                                        viewModel.addCustomQuickPreset(name, qty, rate, amount)
                                    },
                                    onRemovePreset = { preset ->
                                        viewModel.removeQuickPreset(preset)
                                    },
                                    onResetPresetsDefault = {
                                        viewModel.resetQuickPresetsToDefault()
                                    },
                                    onSaveSettings = { billType, centerName, subtitle, purchaserLabel ->
                                        viewModel.saveSettings(billType, centerName, subtitle, purchaserLabel)
                                    },
                                    onResetTemplate = { viewModel.resetToInitialTemplate() },
                                    onResetAllData = { viewModel.resetAllUserData() },
                                    onOpenGlobalSettings = { showGlobalSettings = true }
                                )
                            }
                        }
                    } else {
                        // Digital Tools Hub
                        ToolsHubScreen(
                            appLanguage = appLanguage,
                            updateInfo = updateInfo,
                            isCheckingUpdate = isCheckingUpdate,
                            onCheckUpdate = {
                                coroutineScope.launch {
                                    isCheckingUpdate = true
                                    try {
                                        val info = updateManager.checkForUpdate(context, forceCheck = true)
                                        updateInfo = info
                                        if (info.hasUpdate) {
                                            updateDialogInfoToShow = info
                                        } else {
                                            snackbarHostState.showSnackbar(if (isEn) "You are using the latest version!" else "আপনার অ্যাপটি লেটেস্ট ভার্সনে আছে!")
                                        }
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar(if (isEn) "Update check failed" else "আপডেট চেক ব্যর্থ হয়েছে")
                                    } finally {
                                        isCheckingUpdate = false
                                    }
                                }
                            },
                            onOpenUpdateDialog = { info -> updateDialogInfoToShow = info },
                            onSelectFoodBillTool = { selectedTool = "food_bill" },
                            onSelectAdvanceSalaryTool = { selectedTool = "advance_salary" },
                            onSelectMedicalWorkTool = { selectedTool = "medical_work" },
                            onSelectAppSettings = { showGlobalSettings = true },
                            onSelectUpcomingTool = { title ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("'$title' শীঘ্রই পরবর্তী আপডেটে আসছে!")
                                }
                            }
                        )
                    }
                }
            }
            }
            }
        }
    }

    // Voucher Preview Modal Dialog
    if (showPreviewDialog) {
        val defaultTop = previewTopMemo ?: PrintMemoData(
            memoId = currentBillState.editingBillId,
            centerName = currentBillState.centerName,
            subtitle = currentBillState.subtitle,
            dateString = currentBillState.dateString,
            purchaserName = currentBillState.purchaserName,
            purchaserLabel = currentBillState.purchaserLabel,
            items = currentBillState.items.filter { it.name.isNotBlank() || it.amount > 0 },
            totalAmount = currentBillState.totalAmount,
            billType = currentBillState.billType,
            showSignature = currentBillState.showSignature
        )

        VoucherPreviewDialog(
            initialTopMemo = defaultTop,
            initialBottomMemo = previewBottomMemo,
            historyBills = historyBills,
            defaultCenterName = currentBillState.centerName,
            defaultSubtitle = currentBillState.subtitle,
            defaultPurchaserLabel = currentBillState.purchaserLabel,
            onDismiss = { showPreviewDialog = false },
            onPrint = { topMemo, bottomMemo, pos ->
                PrintUtils.printFoodBillDual(
                    context = context,
                    topMemo = topMemo,
                    bottomMemo = bottomMemo,
                    position = pos
                )
            },
            onSharePdf = { topMemo, bottomMemo, pos ->
                PrintUtils.shareFoodBillPdfDual(
                    context = context,
                    topMemo = topMemo,
                    bottomMemo = bottomMemo,
                    position = pos
                )
            },
            onSaveImage = { memo ->
                PrintUtils.saveFoodBillImage(
                    context = context,
                    memo = memo
                )
            },
            onShareImage = { memo ->
                PrintUtils.shareFoodBillImage(
                    context = context,
                    memo = memo
                )
            }
        )
    }

    // In-App Update Dialog
    updateDialogInfoToShow?.let { info ->
        UpdateDialog(
            updateInfo = info,
            updateManager = updateManager,
            onDismiss = { updateDialogInfoToShow = null }
        )
    }

    // First-launch walkthrough (shown once)
    if (showOnboarding && !isSplashLoading) {
        OnboardingDialog(onDismiss = { viewModel.dismissOnboarding() })
    }
}
