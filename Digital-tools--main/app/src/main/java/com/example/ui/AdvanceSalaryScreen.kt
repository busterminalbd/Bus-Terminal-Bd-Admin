package com.example.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AdvanceSalaryFormCard
import com.example.ui.components.AdvanceSalaryHistoryList
import com.example.ui.components.AdvanceSalaryPreviewDialog
import com.example.ui.theme.HeadingFontFamily
import com.example.util.AdvanceSalaryImageExporter
import com.example.util.AdvanceSalaryPrintUtils
import com.example.util.PrintPosition
import kotlinx.coroutines.flow.collectLatest
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvanceSalaryScreen(
    viewModel: AdvanceSalaryViewModel,
    onNavigateBack: () -> Unit,
    onOpenGlobalSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val historyList by viewModel.historyList.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val presetReasons by viewModel.presetReasons.collectAsStateWithLifecycle()
    val presetDesignations by viewModel.presetDesignations.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showPreviewDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (selectedTab == 0) "Advance Salary Application" else "Saved Applications",
                            fontFamily = HeadingFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        if (selectedTab == 0) {
                            Text(
                                text = "Staff Requisition Voucher",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Tools Hub",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (selectedTab == 0) {
                        IconButton(
                            onClick = { viewModel.resetForm() },
                            modifier = Modifier.testTag("reset_form_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = "New Form",
                                tint = Color.White
                            )
                        }
                    }
                    IconButton(
                        onClick = onOpenGlobalSettings,
                        modifier = Modifier.testTag("advance_salary_global_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "মেইন অ্যাপ সেটিংস",
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
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Edit, contentDescription = "Application Form") },
                    label = { Text("Application", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("tab_advance_form")
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.History, contentDescription = "Saved List") },
                    label = { Text("History", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("tab_advance_history")
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (selectedTab) {
                0 -> {
                    // Application Form & Actions
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(12.dp)
                    ) {
                        AdvanceSalaryFormCard(
                            state = formState,
                            presetReasons = presetReasons,
                            presetDesignations = presetDesignations,
                            onAddReasonPreset = { viewModel.addReasonPreset(it) },
                            onEditReasonPreset = { old, new -> viewModel.updateReasonPreset(old, new) },
                            onDeleteReasonPreset = { viewModel.deleteReasonPreset(it) },
                            onAddDesignationPreset = { viewModel.addDesignationPreset(it) },
                            onEditDesignationPreset = { old, new -> viewModel.updateDesignationPreset(old, new) },
                            onDeleteDesignationPreset = { viewModel.deleteDesignationPreset(it) },
                            onCompanyNameChange = { viewModel.updateCompanyName(it) },
                            onCompanySubtitleChange = { viewModel.updateCompanySubtitle(it) },
                            onApplicationNoChange = { viewModel.updateApplicationNo(it) },
                            onDateClick = { showDatePicker() },
                            onApplicantNameChange = { viewModel.updateApplicantName(it) },
                            onDesignationChange = { viewModel.updateDesignation(it) },
                            onContactNumberChange = { viewModel.updateContactNumber(it) },
                            onMonthlySalaryChange = { viewModel.updateMonthlySalary(it) },
                            onAdvanceAmountChange = { viewModel.updateAdvanceAmount(it) },
                            onAdvanceAmountInWordsChange = { viewModel.updateAdvanceAmountInWords(it) },
                            onReasonChange = { viewModel.updateReason(it) },
                            onRepaymentTypeChange = { viewModel.updateRepaymentType(it) },
                            onInstallmentCountChange = { viewModel.updateInstallmentCount(it) },
                            onDeductionStartMonthChange = { viewModel.updateDeductionStartMonth(it) },
                            onPreviousAdvancePendingChange = { viewModel.updatePreviousAdvancePending(it) },
                            onRemarksChange = { viewModel.updateRemarks(it) },
                            onStatusChange = { viewModel.updateStatus(it) },
                            onShowSignaturesChange = { viewModel.updateShowSignatures(it) }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Action Buttons Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Save / Update Button
                            Button(
                                onClick = { viewModel.saveApplication() },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(48.dp)
                                    .testTag("save_advance_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (formState.editingId != null) "Update" else "Save",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            // Print / Preview Button
                            Button(
                                onClick = {
                                    viewModel.saveApplication(notifyUser = false)
                                    showPreviewDialog = true
                                },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(48.dp)
                                    .testTag("preview_advance_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Preview / Print", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Share Image
                            OutlinedButton(
                                onClick = {
                                    viewModel.saveApplication(notifyUser = false)
                                    AdvanceSalaryImageExporter.shareAdvanceSalaryImage(context, formState)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share Image", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }

                            // Share PDF
                            OutlinedButton(
                                onClick = {
                                    viewModel.saveApplication(notifyUser = false)
                                    AdvanceSalaryPrintUtils.shareAdvanceSalaryPdf(context, formState, null, PrintPosition.TOP)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFFD32F2F))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share PDF", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                1 -> {
                    // Saved Applications History View
                    AdvanceSalaryHistoryList(
                        historyList = historyList,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        onEditApplication = { entity ->
                            viewModel.loadApplicationForEditing(entity)
                            selectedTab = 0
                        },
                        onDeleteApplication = { id -> viewModel.deleteApplication(id) },
                        onPrintApplication = { entity ->
                            viewModel.loadApplicationForEditing(entity)
                            showPreviewDialog = true
                        },
                        onShareApplication = { entity ->
                            viewModel.loadApplicationForEditing(entity)
                            AdvanceSalaryImageExporter.shareAdvanceSalaryImage(context, viewModel.formState.value)
                        }
                    )
                }
            }
        }
    }

    if (showPreviewDialog) {
        AdvanceSalaryPreviewDialog(
            formState = formState,
            onDismiss = { showPreviewDialog = false },
            onPrint = { topState, bottomState, pos ->
                viewModel.saveApplication(notifyUser = false)
                AdvanceSalaryPrintUtils.printAdvanceSalary(context, topState, bottomState, pos)
            },
            onSharePdf = { topState, bottomState, pos ->
                viewModel.saveApplication(notifyUser = false)
                AdvanceSalaryPrintUtils.shareAdvanceSalaryPdf(context, topState, bottomState, pos)
            },
            onSaveImage = { state ->
                viewModel.saveApplication(notifyUser = false)
                AdvanceSalaryImageExporter.saveAdvanceSalaryImageToGallery(context, state)
            },
            onShareImage = { state ->
                viewModel.saveApplication(notifyUser = false)
                AdvanceSalaryImageExporter.shareAdvanceSalaryImage(context, state)
            }
        )
    }
}
