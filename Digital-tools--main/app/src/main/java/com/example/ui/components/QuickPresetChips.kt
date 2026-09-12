package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import com.example.ui.theme.HeadingFontFamily
import com.example.ui.theme.LedgerRed
import com.example.util.BengaliUtils
import com.example.util.FolderOption
import com.example.util.QuickPreset
import com.example.util.findParentFolderId
import com.example.util.getAllFlatItems
import com.example.util.getFolderOptions

@Composable
fun QuickPresetChips(
    presets: List<QuickPreset>,
    addedItemNames: Set<String> = emptySet(),
    onPresetClick: (name: String, qty: String, rate: String, amount: String) -> Unit,
    onAddCustomPreset: (name: String, qty: String, rate: String, amount: String) -> Unit = { _, _, _, _ -> },
    onAddCustomPresetWithParent: (name: String, qty: String, rate: String, amount: String, parentFolderId: String?) -> Unit = { n, q, r, a, _ -> onAddCustomPreset(n, q, r, a) },
    onAddFolder: (folderName: String, parentFolderId: String?) -> Unit = { _, _ -> },
    onUpdateNode: (nodeId: String, name: String, qty: String, rate: String, amount: String, isFolder: Boolean, parentFolderId: String?, children: List<QuickPreset>) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onRemovePreset: (preset: QuickPreset) -> Unit = {},
    onResetDefaults: () -> Unit = {},
    onReorderPreset: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
    onReorderPresetWithParent: (parentFolderId: String?, fromIndex: Int, toIndex: Int) -> Unit = { _, f, t -> onReorderPreset(f, t) },
    modifier: Modifier = Modifier
) {
    var showManageDialog by remember { mutableStateOf(false) }
    var expandedDropdown by remember { mutableStateOf(false) }
    var selectedFolderForDialog by remember { mutableStateOf<QuickPreset?>(null) }
    var expandedFolderIds by remember { mutableStateOf(setOf<String>()) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable { expandedDropdown = true }
                        .testTag("quick_preset_dropdown_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "দ্রুত প্রিসেট আইটেম ▾",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                DropdownMenu(
                    expanded = expandedDropdown,
                    onDismissRequest = { expandedDropdown = false },
                    modifier = Modifier
                        .widthIn(min = 280.dp, max = 340.dp)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Text(
                        text = "— প্রিসেট লিস্ট (ট্যাপ করে সিলেক্ট / আনসেলেক্ট করুন) —",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                    Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

                    PresetDropdownList(
                        nodes = presets,
                        addedItemNames = addedItemNames,
                        expandedFolderIds = expandedFolderIds,
                        onToggleFolder = { id ->
                            expandedFolderIds = if (expandedFolderIds.contains(id)) {
                                expandedFolderIds - id
                            } else {
                                expandedFolderIds + id
                            }
                        },
                        onItemClick = { preset ->
                            onPresetClick(preset.name, preset.defaultQty, preset.defaultRate, preset.defaultAmount)
                        },
                        onAddAllFolderItems = { folder ->
                            val flatItems = folder.children.getAllFlatItems()
                            flatItems.forEach { item ->
                                if (!addedItemNames.contains(item.name.trim())) {
                                    onPresetClick(item.name, item.defaultQty, item.defaultRate, item.defaultAmount)
                                }
                            }
                        }
                    )

                    Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "⚙️ প্রিসেট ম্যানেজ / এডিট করুন",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        onClick = {
                            expandedDropdown = false
                            showManageDialog = true
                        }
                    )
                }
            }

            // Customize / Manage Button
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .clickable { showManageDialog = true }
                    .testTag("manage_quick_presets_btn")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "কাস্টমাইজ",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "কাস্টমাইজ",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Horizontal Preset Chips (Root items and Root Folders)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(presets) { preset ->
                if (preset.isFolder) {
                    FolderChip(
                        folder = preset,
                        addedItemNames = addedItemNames,
                        onClick = { selectedFolderForDialog = preset }
                    )
                } else {
                    val isAdded = addedItemNames.contains(preset.name.trim())
                    PresetChip(
                        preset = preset,
                        isAdded = isAdded,
                        onClick = {
                            onPresetClick(preset.name, preset.defaultQty, preset.defaultRate, preset.defaultAmount)
                        }
                    )
                }
            }
        }
    }

    if (selectedFolderForDialog != null) {
        FolderItemsDialog(
            folder = selectedFolderForDialog!!,
            addedItemNames = addedItemNames,
            onDismiss = { selectedFolderForDialog = null },
            onPresetClick = { name, qty, rate, amount ->
                onPresetClick(name, qty, rate, amount)
            }
        )
    }

    if (showManageDialog) {
        ManageQuickPresetsDialog(
            presets = presets,
            onDismiss = { showManageDialog = false },
            onAddPresetWithParent = onAddCustomPresetWithParent,
            onAddFolder = onAddFolder,
            onUpdateNode = onUpdateNode,
            onRemovePreset = onRemovePreset,
            onResetDefaults = onResetDefaults,
            onReorderPresetWithParent = onReorderPresetWithParent
        )
    }
}

@Composable
private fun PresetDropdownList(
    nodes: List<QuickPreset>,
    addedItemNames: Set<String>,
    expandedFolderIds: Set<String>,
    onToggleFolder: (String) -> Unit,
    onItemClick: (QuickPreset) -> Unit,
    onAddAllFolderItems: (QuickPreset) -> Unit,
    level: Int = 0
) {
    nodes.forEach { node ->
        if (node.isFolder) {
            val isExpanded = expandedFolderIds.contains(node.id)
            val childItems = node.children.getAllFlatItems()
            val addedCount = childItems.count { addedItemNames.contains(it.name.trim()) }

            Column(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleFolder(node.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(
                            start = (12 + level * 12).dp,
                            end = 8.dp,
                            top = 6.dp,
                            bottom = 6.dp
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${node.name} (${childItems.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (addedCount > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "[$addedCount যোগ করা]",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (childItems.isNotEmpty() && addedCount < childItems.size) {
                                TextButton(
                                    onClick = { onAddAllFolderItems(node) },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                    modifier = Modifier.height(24.dp)
                                ) {
                                    Text("সব যোগ", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                if (isExpanded) {
                    PresetDropdownList(
                        nodes = node.children,
                        addedItemNames = addedItemNames,
                        expandedFolderIds = expandedFolderIds,
                        onToggleFolder = onToggleFolder,
                        onItemClick = onItemClick,
                        onAddAllFolderItems = onAddAllFolderItems,
                        level = level + 1
                    )
                }
            }
        } else {
            val isAdded = addedItemNames.contains(node.name.trim())
            val formattedText = BengaliUtils.formatPresetDisplayText(node)
            DropdownMenuItem(
                text = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = (level * 12).dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (level > 0) "├── $formattedText" else formattedText,
                            fontSize = 12.sp,
                            fontWeight = if (isAdded) FontWeight.Bold else FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isAdded) {
                            Text(
                                text = "(বাদ দিন)",
                                fontSize = 10.sp,
                                color = LedgerRed,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                },
                trailingIcon = {
                    if (isAdded) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "যোগ করা আছে",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "বাদ দিন",
                                tint = LedgerRed,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                },
                onClick = { onItemClick(node) }
            )
        }
    }
}

@Composable
fun FolderChip(
    folder: QuickPreset,
    addedItemNames: Set<String>,
    onClick: () -> Unit
) {
    val flatItems = folder.children.getAllFlatItems()
    val addedCount = flatItems.count { addedItemNames.contains(it.name.trim()) }
    val hasAdded = addedCount > 0

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (hasAdded) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        modifier = Modifier
            .clickable { onClick() }
            .testTag("folder_chip_${folder.name}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = folder.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = " (${flatItems.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = Color.DarkGray
            )
            if (hasAdded) {
                Text(
                    text = " [$addedCount]",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun FolderItemsDialog(
    folder: QuickPreset,
    addedItemNames: Set<String>,
    onDismiss: () -> Unit,
    onPresetClick: (name: String, qty: String, rate: String, amount: String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(0.95f),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = folder.name,
                    fontFamily = HeadingFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "ট্যাপ করে আইটেম ক্যাশ মেমোতে যোগ বা বাদ দিন:",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                FolderDialogTreeContent(
                    nodes = folder.children,
                    addedItemNames = addedItemNames,
                    onPresetClick = onPresetClick
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("সম্পন্ন", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun FolderDialogTreeContent(
    nodes: List<QuickPreset>,
    addedItemNames: Set<String>,
    onPresetClick: (name: String, qty: String, rate: String, amount: String) -> Unit,
    level: Int = 0
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        nodes.forEach { node ->
            if (node.isFolder) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = (level * 12).dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = node.name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    FolderDialogTreeContent(
                        nodes = node.children,
                        addedItemNames = addedItemNames,
                        onPresetClick = onPresetClick,
                        level = level + 1
                    )
                }
            } else {
                val isAdded = addedItemNames.contains(node.name.trim())
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isAdded) Color(0xFFD4EDDA) else MaterialTheme.colorScheme.surfaceVariant,
                    border = if (isAdded) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = (level * 12).dp)
                        .clickable {
                            onPresetClick(node.name, node.defaultQty, node.defaultRate, node.defaultAmount)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isAdded) Icons.Default.Check else Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val formattedText = BengaliUtils.formatPresetDisplayText(node)
                            Text(
                                text = formattedText,
                                fontSize = 13.sp,
                                fontWeight = if (isAdded) FontWeight.Bold else FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (isAdded) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "বাদ দিন",
                                tint = LedgerRed,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun extractQtyNum(str: String): Double {
    val eng = BengaliUtils.toEnglishDigits(str)
    val match = Regex("""\d+(\.\d+)?""").find(eng)
    return match?.value?.toDoubleOrNull() ?: 0.0
}

private fun formatNum(value: Double): String {
    val str = if (value % 1.0 == 0.0) value.toLong().toString() else String.format(java.util.Locale.US, "%.1f", value)
    return BengaliUtils.toBengaliDigits(str)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ManageQuickPresetsDialog(
    presets: List<QuickPreset>,
    onDismiss: () -> Unit,
    onAddPresetWithParent: (name: String, qty: String, rate: String, amount: String, parentFolderId: String?) -> Unit = { _, _, _, _, _ -> },
    onAddFolder: (folderName: String, parentFolderId: String?) -> Unit = { _, _ -> },
    onUpdateNode: (nodeId: String, name: String, qty: String, rate: String, amount: String, isFolder: Boolean, parentFolderId: String?, children: List<QuickPreset>) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onRemovePreset: (preset: QuickPreset) -> Unit = {},
    onResetDefaults: () -> Unit = {},
    onReorderPresetWithParent: (parentFolderId: String?, fromIndex: Int, toIndex: Int) -> Unit = { _, _, _ -> }
) {
    val focusManager = LocalFocusManager.current
    val focusName = remember { FocusRequester() }
    val focusQty = remember { FocusRequester() }
    val focusRate = remember { FocusRequester() }
    val focusAmount = remember { FocusRequester() }

    // Tab mode: 0 -> Add Item, 1 -> Add Folder/Menu
    var isFolderMode by remember { mutableStateOf(false) }

    var editingNode by remember { mutableStateOf<QuickPreset?>(null) }
    var newItemName by remember { mutableStateOf("") }
    var newItemQtyVal by remember { mutableStateOf("") }
    var newItemRateVal by remember { mutableStateOf("") }
    var newItemAmountVal by remember { mutableStateOf("") }
    var selectedUnit by remember { mutableStateOf("") }

    // Folder selection state ("রাখার স্থান")
    var selectedTargetFolderId by remember { mutableStateOf<String?>(null) }
    var expandedFolderPicker by remember { mutableStateOf(false) }

    // Accordion expand/collapse state in management tree
    var expandedTreeFolderIds by remember { mutableStateOf(setOf<String>()) }

    val commonUnits = listOf("কেজি", "লিটার", "পোয়া", "পিস", "প্যাকেট", "গ্রাম", "ডজন", "আঁটি")
    val folderOptions = presets.getFolderOptions(excludeFolderId = editingNode?.id)

    val currentFolderDisplayName = folderOptions.find { it.id == (selectedTargetFolderId ?: "") }?.displayName
        ?: "🏠 প্রধান তালিকা (Main List)"

    fun clearForm() {
        editingNode = null
        newItemName = ""
        newItemQtyVal = ""
        newItemRateVal = ""
        newItemAmountVal = ""
        selectedUnit = ""
        selectedTargetFolderId = null
    }

    fun startEditing(node: QuickPreset) {
        editingNode = node
        isFolderMode = node.isFolder
        newItemName = node.name
        newItemQtyVal = node.defaultQty
        newItemRateVal = node.defaultRate
        newItemAmountVal = node.defaultAmount
        selectedTargetFolderId = presets.findParentFolderId(node.id)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.96f),
        title = {
            Text(
                text = "প্রিসেট ও ফোল্ডার ব্যবস্থাপনা",
                fontFamily = HeadingFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            val dialogScrollState = rememberScrollState()
            var dialogBoundsInWindow by remember { mutableStateOf(Rect.Zero) }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coords ->
                        val pos = coords.positionInWindow()
                        val size = coords.size
                        dialogBoundsInWindow = Rect(
                            pos.x, pos.y, pos.x + size.width, pos.y + size.height
                        )
                    }
                    .verticalScroll(dialogScrollState)
            ) {
                // Segmented buttons: Add Item vs Add Folder
                if (editingNode == null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (!isFolderMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    isFolderMode = false
                                    clearForm()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    tint = if (!isFolderMode) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "➕ নতুন আইটেম",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isFolderMode) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isFolderMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    isFolderMode = true
                                    clearForm()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CreateNewFolder,
                                    contentDescription = null,
                                    tint = if (isFolderMode) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "📁 নতুন ফোল্ডার/মেনু",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isFolderMode) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                } else {
                    // Editing Mode Banner
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "✏️ ‘${editingNode!!.name}’ ${if (editingNode!!.isFolder) "ফোল্ডার" else "আইটেম"} সম্পাদনা করছেন",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(
                                onClick = { clearForm() },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("বাতিল", fontSize = 11.sp, color = LedgerRed)
                            }
                        }
                    }
                }

                // Input Name Field
                OutlinedTextField(
                    value = newItemName,
                    onValueChange = { newItemName = it },
                    label = {
                        Text(
                            if (isFolderMode) "ফোল্ডার / মেনুর নাম (যেমন: 🛒 বাজারের লিস্ট, 👥 অফিসের মানুষ)"
                            else "আইটেমের নাম (যেমন: কাঁচা মরিচ, পেঁয়াজ)",
                            fontSize = 12.sp
                        )
                    },
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { if (!isFolderMode) focusQty.requestFocus() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusName)
                        .testTag("custom_preset_name_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Location / Parent Folder Picker Dropdown
                Text(
                    text = if (isFolderMode) "প্যারেন্ট ফোল্ডার (Optional - সাবমেনুর জন্য):" else "রাখার স্থান (Optional):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedFolderPicker = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentFolderDisplayName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = expandedFolderPicker,
                        onDismissRequest = { expandedFolderPicker = false },
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        folderOptions.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option.displayName,
                                        fontSize = 13.sp,
                                        fontWeight = if (selectedTargetFolderId == option.id) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedTargetFolderId == option.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    selectedTargetFolderId = option.id.ifBlank { null }
                                    expandedFolderPicker = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (!isFolderMode) {
                    // Unit Selector
                    Text(
                        text = "ডিফল্ট একক সিলেক্ট করুন:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        commonUnits.forEach { unit ->
                            val isSelected = selectedUnit == unit
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable {
                                    selectedUnit = if (isSelected) "" else unit
                                }
                            ) {
                                Text(
                                    text = unit,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else Color.DarkGray,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = newItemQtyVal,
                            onValueChange = { newVal ->
                                val bnVal = BengaliUtils.toBengaliDigits(newVal)
                                newItemQtyVal = bnVal
                                val rateNum = BengaliUtils.parseBengaliNumber(newItemRateVal)
                                if (rateNum > 0) {
                                    val qtyNum = extractQtyNum(bnVal)
                                    val effectiveQty = if (qtyNum > 0) qtyNum else 1.0
                                    newItemAmountVal = formatNum(rateNum * effectiveQty)
                                }
                            },
                            label = { Text("পরিমাণ", fontSize = 10.sp) },
                            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusRate.requestFocus() }),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusQty)
                                .testTag("custom_preset_qty_input"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = newItemRateVal,
                            onValueChange = { newVal ->
                                val bnVal = BengaliUtils.toBengaliDigits(newVal)
                                newItemRateVal = bnVal
                                val rateNum = BengaliUtils.parseBengaliNumber(bnVal)
                                if (rateNum > 0) {
                                    val qtyNum = extractQtyNum(newItemQtyVal)
                                    val effectiveQty = if (qtyNum > 0) qtyNum else 1.0
                                    newItemAmountVal = formatNum(rateNum * effectiveQty)
                                }
                            },
                            label = { Text("দর (টাকা)", fontSize = 10.sp) },
                            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusAmount.requestFocus() }),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRate)
                                .testTag("custom_preset_rate_input"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = newItemAmountVal,
                            onValueChange = { newVal ->
                                val bnVal = BengaliUtils.toBengaliDigits(newVal)
                                newItemAmountVal = bnVal
                            },
                            label = { Text("মোট টাকা", fontSize = 10.sp) },
                            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusName.requestFocus() }),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusAmount)
                                .testTag("custom_preset_amount_input"),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Submit Button
                val bnQty = BengaliUtils.toBengaliDigits(newItemQtyVal.trim())
                val formattedQty = when {
                    bnQty.isNotBlank() && selectedUnit.isNotBlank() -> {
                        if (bnQty.endsWith(selectedUnit)) bnQty else "$bnQty $selectedUnit"
                    }
                    bnQty.isNotBlank() -> bnQty
                    else -> ""
                }

                Button(
                    onClick = {
                        if (newItemName.isNotBlank()) {
                            if (editingNode != null) {
                                onUpdateNode(
                                    editingNode!!.id,
                                    newItemName.trim(),
                                    formattedQty,
                                    newItemRateVal.trim(),
                                    newItemAmountVal.trim(),
                                    isFolderMode,
                                    selectedTargetFolderId,
                                    editingNode!!.children
                                )
                            } else {
                                if (isFolderMode) {
                                    onAddFolder(newItemName.trim(), selectedTargetFolderId)
                                } else {
                                    onAddPresetWithParent(
                                        newItemName.trim(),
                                        formattedQty,
                                        newItemRateVal.trim(),
                                        newItemAmountVal.trim(),
                                        selectedTargetFolderId
                                    )
                                }
                            }
                            clearForm()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("add_custom_preset_submit"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        if (editingNode != null) Icons.Default.Edit else if (isFolderMode) Icons.Default.CreateNewFolder else Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (editingNode != null) "আপডেট করুন"
                        else if (isFolderMode) "ফোল্ডার তৈরি করুন"
                        else "প্রিসেট আইটেম যোগ করুন",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "সংরক্ষিত প্রিসেট ও ফোল্ডারসমূহ [ট্যাপ করে বিস্তার করুন / এডিট করুন]:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Management Tree List View
                PresetManagementTree(
                    nodes = presets,
                    parentFolderId = null,
                    editingNode = editingNode,
                    expandedTreeFolderIds = expandedTreeFolderIds,
                    scrollState = dialogScrollState,
                    dialogBoundsInWindow = dialogBoundsInWindow,
                    onToggleExpand = { id ->
                        expandedTreeFolderIds = if (expandedTreeFolderIds.contains(id)) {
                            expandedTreeFolderIds - id
                        } else {
                            expandedTreeFolderIds + id
                        }
                    },
                    onEditNode = { node -> startEditing(node) },
                    onDeleteNode = { node ->
                        if (editingNode?.id == node.id) clearForm()
                        onRemovePreset(node)
                    },
                    onReorderNode = { pId, fromIdx, toIdx ->
                        onReorderPresetWithParent(pId, fromIdx, toIdx)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Reset Defaults Button
                TextButton(
                    onClick = { onResetDefaults() },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .testTag("reset_presets_default")
                ) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ডিফল্ট তালিকায় ফেরত যান",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White)
            ) {
                Text("বন্ধ করুন", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun PresetManagementTree(
    nodes: List<QuickPreset>,
    parentFolderId: String?,
    editingNode: QuickPreset?,
    expandedTreeFolderIds: Set<String>,
    scrollState: ScrollState? = null,
    dialogBoundsInWindow: Rect = Rect.Zero,
    onToggleExpand: (String) -> Unit,
    onEditNode: (QuickPreset) -> Unit,
    onDeleteNode: (QuickPreset) -> Unit,
    onReorderNode: (parentFolderId: String?, fromIndex: Int, toIndex: Int) -> Unit,
    level: Int = 0
) {
    var draggedNodeId by remember { mutableStateOf<String?>(null) }
    var nodeDragOffsetY by remember { mutableFloatStateOf(0f) }
    var activeAutoScrollSpeed by remember { mutableFloatStateOf(0f) }
    val nodeRowHeights = remember { mutableStateMapOf<String, Int>() }
    val nodeWindowTops = remember { mutableStateMapOf<String, Float>() }

    var initialRowWindowTop by remember { mutableFloatStateOf(0f) }
    var initialTouchInRowY by remember { mutableFloatStateOf(0f) }
    var totalTouchDragY by remember { mutableFloatStateOf(0f) }

    val currentNodes by rememberUpdatedState(nodes)
    val currentOnReorder by rememberUpdatedState(onReorderNode)

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    val viewportTop = if (dialogBoundsInWindow.top > 0f) dialogBoundsInWindow.top else with(density) { 60.dp.toPx() }
    val viewportBottom = if (dialogBoundsInWindow.bottom > 0f) dialogBoundsInWindow.bottom else screenHeightPx - with(density) { 60.dp.toPx() }
    val scrollThreshold = with(density) { 80.dp.toPx() }
    val maxScrollSpeed = with(density) { 18.dp.toPx() }

    LaunchedEffect(draggedNodeId) {
        if (draggedNodeId == null) return@LaunchedEffect
        while (isActive && draggedNodeId != null) {
            val speed = activeAutoScrollSpeed
            if (speed != 0f && scrollState != null) {
                val consumed = scrollState.scrollBy(speed)
                if (consumed != 0f) {
                    initialRowWindowTop -= consumed
                    nodeDragOffsetY += consumed

                    val list = currentNodes
                    val curIndex = list.indexOfFirst { it.id == draggedNodeId }
                    if (curIndex != -1) {
                        if (nodeDragOffsetY < 0f && curIndex > 0) {
                            val prevNode = list[curIndex - 1]
                            val prevHeight = maxOf((nodeRowHeights[prevNode.id] ?: 48).toFloat(), 30f)
                            if (nodeDragOffsetY < -(prevHeight * 0.45f)) {
                                currentOnReorder(parentFolderId, curIndex, curIndex - 1)
                                nodeDragOffsetY += prevHeight
                            }
                        } else if (nodeDragOffsetY > 0f && curIndex < list.lastIndex) {
                            val nextNode = list[curIndex + 1]
                            val nextHeight = maxOf((nodeRowHeights[nextNode.id] ?: 48).toFloat(), 30f)
                            if (nodeDragOffsetY > (nextHeight * 0.45f)) {
                                currentOnReorder(parentFolderId, curIndex, curIndex + 1)
                                nodeDragOffsetY -= nextHeight
                            }
                        }
                    }
                } else {
                    activeAutoScrollSpeed = 0f
                }
            }
            delay(16)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        nodes.forEachIndexed { index, node ->
            key(node.id) {
                val isEditing = editingNode?.id == node.id
                val isBeingDragged = draggedNodeId == node.id

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coords ->
                            nodeWindowTops[node.id] = coords.positionInWindow().y
                            nodeRowHeights[node.id] = coords.size.height
                        }
                        .graphicsLayer {
                            translationY = if (isBeingDragged) nodeDragOffsetY else 0f
                        }
                        .zIndex(if (isBeingDragged) 10f else 0f)
                ) {
                    if (node.isFolder) {
                        val isExpanded = expandedTreeFolderIds.contains(node.id)
                        val childItemsCount = node.children.getAllFlatItems().size

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = (level * 12).dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isEditing) MaterialTheme.colorScheme.primary else if (isBeingDragged) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 6.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Drag Handle (=) on left, matching Home screen
                                    Icon(
                                        imageVector = Icons.Default.DragHandle,
                                        contentDescription = "টেনে অবস্থান পরিবর্তন করুন",
                                        tint = if (isEditing) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                        modifier = Modifier
                                            .size(24.dp)
                                            .padding(horizontal = 2.dp)
                                            .pointerInput(Unit) {
                                                detectDragGesturesAfterLongPress(
                                                    onDragStart = { offset ->
                                                        draggedNodeId = node.id
                                                        nodeDragOffsetY = 0f
                                                        activeAutoScrollSpeed = 0f
                                                        initialTouchInRowY = offset.y
                                                        initialRowWindowTop = nodeWindowTops[node.id] ?: 0f
                                                        totalTouchDragY = 0f
                                                    },
                                                    onDragEnd = {
                                                        draggedNodeId = null
                                                        nodeDragOffsetY = 0f
                                                        activeAutoScrollSpeed = 0f
                                                    },
                                                    onDragCancel = {
                                                        draggedNodeId = null
                                                        nodeDragOffsetY = 0f
                                                        activeAutoScrollSpeed = 0f
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        nodeDragOffsetY += dragAmount.y
                                                        totalTouchDragY += dragAmount.y

                                                        val list = currentNodes
                                                        val curIndex = list.indexOfFirst { it.id == draggedNodeId }
                                                        if (curIndex != -1) {
                                                            if (nodeDragOffsetY < 0f && curIndex > 0) {
                                                                val prevNode = list[curIndex - 1]
                                                                val prevHeight = maxOf((nodeRowHeights[prevNode.id] ?: 48).toFloat(), 30f)
                                                                if (nodeDragOffsetY < -(prevHeight * 0.45f)) {
                                                                    currentOnReorder(parentFolderId, curIndex, curIndex - 1)
                                                                    nodeDragOffsetY += prevHeight
                                                                }
                                                            } else if (nodeDragOffsetY > 0f && curIndex < list.lastIndex) {
                                                                val nextNode = list[curIndex + 1]
                                                                val nextHeight = maxOf((nodeRowHeights[nextNode.id] ?: 48).toFloat(), 30f)
                                                                if (nodeDragOffsetY > (nextHeight * 0.45f)) {
                                                                    currentOnReorder(parentFolderId, curIndex, curIndex + 1)
                                                                    nodeDragOffsetY -= nextHeight
                                                                }
                                                            }
                                                        }

                                                        if (viewportBottom > viewportTop) {
                                                            val currentPointerY = initialRowWindowTop + initialTouchInRowY + totalTouchDragY
                                                            val distFromBottom = viewportBottom - currentPointerY
                                                            val distFromTop = currentPointerY - viewportTop

                                                            activeAutoScrollSpeed = when {
                                                                distFromBottom < scrollThreshold -> {
                                                                    val ratio = ((scrollThreshold - distFromBottom) / scrollThreshold).coerceIn(0f, 1f)
                                                                    maxScrollSpeed * ratio
                                                                }
                                                                distFromTop < scrollThreshold -> {
                                                                    val ratio = ((scrollThreshold - distFromTop) / scrollThreshold).coerceIn(0f, 1f)
                                                                    -maxScrollSpeed * ratio
                                                                }
                                                                else -> 0f
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                    )

                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onToggleExpand(node.id) },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = if (isEditing) Color.White else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Icon(
                                            imageVector = Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = if (isEditing) Color.White else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${node.name} ($childItemsCount)",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isEditing) Color.White else MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Edit button
                                        IconButton(
                                            onClick = { onEditNode(node) },
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "এডিট",
                                                tint = if (isEditing) Color.White else MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }

                                        // Delete button
                                        IconButton(
                                            onClick = { onDeleteNode(node) },
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "ডিলিট",
                                                tint = if (isEditing) Color(0xFFFF8A80) else LedgerRed,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (isExpanded) {
                                Spacer(modifier = Modifier.height(4.dp))
                                PresetManagementTree(
                                    nodes = node.children,
                                    parentFolderId = node.id,
                                    editingNode = editingNode,
                                    expandedTreeFolderIds = expandedTreeFolderIds,
                                    scrollState = scrollState,
                                    dialogBoundsInWindow = dialogBoundsInWindow,
                                    onToggleExpand = onToggleExpand,
                                    onEditNode = onEditNode,
                                    onDeleteNode = onDeleteNode,
                                    onReorderNode = onReorderNode,
                                    level = level + 1
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isEditing) MaterialTheme.colorScheme.primary else if (isBeingDragged) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f) else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = (level * 12).dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Drag handle on the left matching the Home screen (= icon)
                                Icon(
                                    imageVector = Icons.Default.DragHandle,
                                    contentDescription = "টেনে অবস্থান পরিবর্তন করুন",
                                    tint = if (isEditing) Color.White.copy(alpha = 0.8f) else Color.Gray,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .padding(horizontal = 2.dp)
                                        .pointerInput(Unit) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = { offset ->
                                                    draggedNodeId = node.id
                                                    nodeDragOffsetY = 0f
                                                    activeAutoScrollSpeed = 0f
                                                    initialTouchInRowY = offset.y
                                                    initialRowWindowTop = nodeWindowTops[node.id] ?: 0f
                                                    totalTouchDragY = 0f
                                                },
                                                onDragEnd = {
                                                    draggedNodeId = null
                                                    nodeDragOffsetY = 0f
                                                    activeAutoScrollSpeed = 0f
                                                },
                                                onDragCancel = {
                                                    draggedNodeId = null
                                                    nodeDragOffsetY = 0f
                                                    activeAutoScrollSpeed = 0f
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    nodeDragOffsetY += dragAmount.y
                                                    totalTouchDragY += dragAmount.y

                                                    val list = currentNodes
                                                    val curIndex = list.indexOfFirst { it.id == draggedNodeId }
                                                    if (curIndex != -1) {
                                                        if (nodeDragOffsetY < 0f && curIndex > 0) {
                                                            val prevNode = list[curIndex - 1]
                                                            val prevHeight = maxOf((nodeRowHeights[prevNode.id] ?: 48).toFloat(), 30f)
                                                            if (nodeDragOffsetY < -(prevHeight * 0.45f)) {
                                                                currentOnReorder(parentFolderId, curIndex, curIndex - 1)
                                                                nodeDragOffsetY += prevHeight
                                                            }
                                                        } else if (nodeDragOffsetY > 0f && curIndex < list.lastIndex) {
                                                            val nextNode = list[curIndex + 1]
                                                            val nextHeight = maxOf((nodeRowHeights[nextNode.id] ?: 48).toFloat(), 30f)
                                                            if (nodeDragOffsetY > (nextHeight * 0.45f)) {
                                                                currentOnReorder(parentFolderId, curIndex, curIndex + 1)
                                                                nodeDragOffsetY -= nextHeight
                                                            }
                                                        }
                                                    }

                                                    if (viewportBottom > viewportTop) {
                                                        val currentPointerY = initialRowWindowTop + initialTouchInRowY + totalTouchDragY
                                                        val distFromBottom = viewportBottom - currentPointerY
                                                        val distFromTop = currentPointerY - viewportTop

                                                        activeAutoScrollSpeed = when {
                                                            distFromBottom < scrollThreshold -> {
                                                                val ratio = ((scrollThreshold - distFromBottom) / scrollThreshold).coerceIn(0f, 1f)
                                                                maxScrollSpeed * ratio
                                                            }
                                                            distFromTop < scrollThreshold -> {
                                                                val ratio = ((scrollThreshold - distFromTop) / scrollThreshold).coerceIn(0f, 1f)
                                                                -maxScrollSpeed * ratio
                                                            }
                                                            else -> 0f
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                )

                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onEditNode(node) },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = BengaliUtils.formatPresetDisplayText(node),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isEditing) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Edit button
                                    IconButton(
                                        onClick = { onEditNode(node) },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "এডিট",
                                            tint = if (isEditing) Color.White else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }

                                    // Delete button
                                    IconButton(
                                        onClick = { onDeleteNode(node) },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "ডিলিট",
                                            tint = if (isEditing) Color(0xFFFF8A80) else LedgerRed,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PromptQuantityDialog(
    itemName: String,
    initialRate: String = "",
    initialAmount: String = "",
    onDismiss: () -> Unit,
    onConfirm: (qty: String, rate: String, amount: String) -> Unit
) {
    val focusQty = remember { FocusRequester() }
    val focusRate = remember { FocusRequester() }
    val focusAmount = remember { FocusRequester() }

    var qtyVal by remember { mutableStateOf("") }
    var rateVal by remember { mutableStateOf(initialRate) }
    var amountVal by remember { mutableStateOf(initialAmount) }
    var selectedUnit by remember { mutableStateOf("") }

    val commonUnits = listOf("কেজি", "লিটার", "পোয়া", "পিস", "প্যাকেট", "গ্রাম", "ডজন", "আঁটি")
    val quickQtyOptions = listOf("১", "২", "৩", "৫", "১০", "২৫০ গ্রাম", "৫০০ গ্রাম")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.96f),
        title = {
            Column {
                Text(
                    text = "‘$itemName’ এর পরিমাণ, দর বা টাকা লিখুন",
                    fontFamily = HeadingFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "পরিমাণ (কেজি/লিটার/পিস), দর অথবা মোট টাকা লিখুন:",
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "একক নির্বাচন করুন (Unit):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    commonUnits.forEach { unit ->
                        val isSelected = selectedUnit == unit
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable {
                                selectedUnit = if (isSelected) "" else unit
                            }
                        ) {
                            Text(
                                text = unit,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = qtyVal,
                        onValueChange = { newVal ->
                            val bnVal = BengaliUtils.toBengaliDigits(newVal)
                            qtyVal = bnVal
                            val rateNum = BengaliUtils.parseBengaliNumber(rateVal)
                            if (rateNum > 0) {
                                val qtyNum = extractQtyNum(bnVal)
                                val effectiveQty = if (qtyNum > 0) qtyNum else 1.0
                                amountVal = formatNum(rateNum * effectiveQty)
                            }
                        },
                        label = { Text("পরিমাণ (১,২)", fontSize = 10.sp) },
                        singleLine = true,
                        textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusRate.requestFocus() }),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusQty)
                            .testTag("prompt_qty_input")
                    )

                    OutlinedTextField(
                        value = rateVal,
                        onValueChange = { newVal ->
                            val bnVal = BengaliUtils.toBengaliDigits(newVal)
                            rateVal = bnVal
                            val rateNum = BengaliUtils.parseBengaliNumber(bnVal)
                            if (rateNum > 0) {
                                val qtyNum = extractQtyNum(qtyVal)
                                val effectiveQty = if (qtyNum > 0) qtyNum else 1.0
                                amountVal = formatNum(rateNum * effectiveQty)
                            }
                        },
                        label = { Text("দর (টাকা)", fontSize = 10.sp) },
                        singleLine = true,
                        textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusAmount.requestFocus() }),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRate)
                            .testTag("prompt_rate_input")
                    )

                    OutlinedTextField(
                        value = amountVal,
                        onValueChange = { newVal ->
                            val bnVal = BengaliUtils.toBengaliDigits(newVal)
                            amountVal = bnVal
                        },
                        label = { Text("মোট টাকা", fontSize = 10.sp) },
                        singleLine = true,
                        textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusQty.requestFocus() }),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusAmount)
                            .testTag("prompt_amount_input")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "দ্রুত পরিমাণ নির্বাচন:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    quickQtyOptions.forEach { option ->
                        val optionText = if (option.contains("গ্রাম")) option else if (selectedUnit.isNotBlank()) "$option $selectedUnit" else option
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable {
                                qtyVal = optionText
                                val rateNum = BengaliUtils.parseBengaliNumber(rateVal)
                                if (rateNum > 0) {
                                    val qtyNum = extractQtyNum(optionText)
                                    val effectiveQty = if (qtyNum > 0) qtyNum else 1.0
                                    amountVal = formatNum(rateNum * effectiveQty)
                                }
                            }
                        ) {
                            Text(
                                text = optionText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val bnQty = BengaliUtils.toBengaliDigits(qtyVal.trim())
                    val finalQty = when {
                        bnQty.isNotBlank() && selectedUnit.isNotBlank() && !bnQty.contains(selectedUnit) -> "$bnQty $selectedUnit"
                        bnQty.isNotBlank() -> bnQty
                        else -> ""
                    }
                    onConfirm(finalQty, rateVal.trim(), amountVal.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White)
            ) {
                Text("তালিকায় যোগ করুন", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onConfirm("", rateVal.trim(), amountVal.trim()) }
            ) {
                Text("পরিমাণ ছাড়া যোগ", color = Color.Gray)
            }
        }
    )
}

@Composable
fun PresetChip(
    preset: QuickPreset,
    isAdded: Boolean = false,
    onClick: () -> Unit
) {
    val bgColor = if (isAdded) Color(0xFFD4EDDA) else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isAdded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    val subTextColor = if (isAdded) Color(0xFF1B5E20) else Color.Gray
    val iconColor = if (isAdded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        shadowElevation = if (isAdded) 0.dp else 1.dp,
        border = if (isAdded) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier
            .clickable { onClick() }
            .testTag("preset_chip_${preset.name}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isAdded) Icons.Default.Check else Icons.Default.Add,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier
                    .height(14.dp)
                    .width(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = preset.name,
                fontSize = 13.sp,
                fontWeight = if (isAdded) FontWeight.Bold else FontWeight.SemiBold,
                color = textColor
            )
            val hasDetails = preset.defaultQty.isNotBlank() || preset.defaultRate.isNotBlank() || preset.defaultAmount.isNotBlank()
            if (hasDetails) {
                Spacer(modifier = Modifier.width(4.dp))
                val subText = buildString {
                    var first = true
                    if (preset.defaultQty.isNotBlank()) {
                        append(preset.defaultQty)
                        first = false
                    }
                    if (preset.defaultRate.isNotBlank()) {
                        if (!first) append(" | ")
                        append("দর: ${preset.defaultRate}৳")
                        first = false
                    }
                    if (preset.defaultAmount.isNotBlank()) {
                        if (!first) append(" | ")
                        append("টাকা: ${preset.defaultAmount}৳")
                    }
                }
                Text(
                    text = "($subText)",
                    fontSize = 11.sp,
                    color = subTextColor
                )
            }
            if (isAdded) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "বাদ দিন",
                    tint = LedgerRed,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
