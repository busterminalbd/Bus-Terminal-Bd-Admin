package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import com.example.util.getAllFlatItems
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.data.BillItem
import com.example.ui.CurrentBillState
import com.example.ui.theme.BrassAccent
import com.example.ui.theme.CreamPaperBg
import com.example.ui.theme.DarkForestGreen
import com.example.ui.theme.ForestGreenText
import com.example.ui.theme.HeadingFontFamily
import com.example.ui.theme.LedgerRed
import com.example.ui.theme.LightForestGreen
import com.example.ui.theme.WarmBorderColor
import com.example.util.BengaliUtils
import com.example.util.QuickPreset

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.isActive

val MaroonHeaderColor = DarkForestGreen
val MaroonTextColor = ForestGreenText

@Composable
fun MemoVoucherCard(
    state: CurrentBillState,
    quickPresets: List<QuickPreset> = emptyList(),
    scrollState: ScrollState? = null,
    onPresetClick: (name: String, qty: String, rate: String, amount: String) -> Unit = { _, _, _, _ -> },
    onAddCustomPreset: (name: String, qty: String, rate: String, amount: String) -> Unit = { _, _, _, _ -> },
    onAddCustomPresetWithParent: (name: String, qty: String, rate: String, amount: String, parentFolderId: String?) -> Unit = { n, q, r, a, p -> onAddCustomPreset(n, q, r, a) },
    onAddFolder: (folderName: String, parentFolderId: String?) -> Unit = { _, _ -> },
    onUpdatePresetNode: (nodeId: String, name: String, qty: String, rate: String, amount: String, isFolder: Boolean, parentFolderId: String?, children: List<QuickPreset>) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onRemovePreset: (preset: QuickPreset) -> Unit = {},
    onResetDefaults: () -> Unit = {},
    onReorderPreset: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
    onReorderPresetWithParent: (parentFolderId: String?, fromIndex: Int, toIndex: Int) -> Unit = { p, f, t -> onReorderPreset(f, t) },
    onUpdateDateClick: () -> Unit,
    onUpdateItemName: (id: String, name: String) -> Unit,
    onUpdateItemQty: (id: String, qty: String) -> Unit,
    onUpdateItemRate: (id: String, rate: String) -> Unit,
    onUpdateItemTraveler: (id: String, name: String) -> Unit = { _, _ -> },
    onUpdateItemAmount: (id: String, amount: String) -> Unit,
    onRemoveItem: (id: String) -> Unit,
    onMoveItem: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
    onAddItemRow: () -> Unit,
    onPurchaserLabelChange: ((String) -> Unit)? = null,
    onCenterNameChange: ((String) -> Unit)? = null,
    onSubtitleChange: ((String) -> Unit)? = null,
    onBillTypeChange: ((String) -> Unit)? = null,
    onShowSignatureChange: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isTransport = state.billType == "transport"
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var showManagePresetsDialog by remember { mutableStateOf(false) }

    val itemFocusRequesters = remember(state.items.size) {
        List(state.items.size) {
            List(4) { FocusRequester() }
        }
    }

    var containerBoundsInWindow by remember { mutableStateOf(Rect.Zero) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                val pos = coords.positionInWindow()
                val size = coords.size
                containerBoundsInWindow = Rect(
                    pos.x, pos.y, pos.x + size.width, pos.y + size.height
                )
            }
            .testTag("memo_voucher_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header Banner with Theme Palette Gradient - Editable Center Name & Subtitle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.secondary,
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    )
                    .padding(vertical = 18.dp, horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Center/Institution Name Field
                    BasicTextField(
                        value = state.centerName,
                        onValueChange = { onCenterNameChange?.invoke(it) },
                        textStyle = TextStyle(
                            fontFamily = HeadingFontFamily,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        ),
                        cursorBrush = SolidColor(Color.White),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.Center) {
                                if (state.centerName.isBlank()) {
                                    Text(
                                        text = "প্রতিষ্ঠানের নাম / টাইটেল লিখুন...",
                                        style = TextStyle(
                                            fontFamily = HeadingFontFamily,
                                            fontSize = 19.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White.copy(alpha = 0.65f),
                                            textAlign = TextAlign.Center
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("memo_center_name_input")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Subtitle / Address Field
                    BasicTextField(
                        value = state.subtitle,
                        onValueChange = { onSubtitleChange?.invoke(it) },
                        textStyle = TextStyle(
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.90f),
                            textAlign = TextAlign.Center
                        ),
                        cursorBrush = SolidColor(Color.White),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.Center) {
                                if (state.subtitle.isBlank()) {
                                    Text(
                                        text = "ঠিকানা / বিবরণ লিখুন...",
                                        style = TextStyle(
                                            fontSize = 12.5.sp,
                                            color = Color.White.copy(alpha = 0.55f),
                                            textAlign = TextAlign.Center
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("memo_subtitle_input")
                    )
                }
            }

            // Sawtooth / Decorative teeth line with double rule
            SawtoothDivider()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // Bill Type Toggle: বাজার লিস্ট (market) vs যাতায়াত ভাড়া (transport)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BillTypeChip(
                        label = "🛒 বাজার লিস্ট",
                        selected = !isTransport,
                        modifier = Modifier.weight(1f),
                        onClick = { onBillTypeChange?.invoke("market") }
                    )
                    BillTypeChip(
                        label = "🚌 যাতায়াত ভাড়া",
                        selected = isTransport,
                        modifier = Modifier.weight(1f),
                        onClick = { onBillTypeChange?.invoke("transport") }
                    )
                }

                // Signature On/Off Toggle
                if (onShowSignatureChange != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "✍️ স্বাক্ষরের ঘর দেখাও",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = state.showSignature,
                            onCheckedChange = { onShowSignatureChange(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .scale(0.8f)
                                .testTag("show_signature_switch")
                        )
                    }
                }

                // Date & Action Buttons Compact Header Row
                val canAddItem = state.items.size < 18
                val addedItemNames = remember(state.items) {
                    state.items.map { it.name.trim() }.filter { it.isNotBlank() }.toSet()
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    // Date Selector with Calendar icon (own line so it never fights the
                    // action buttons for space, even at larger font-scale settings)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { onUpdateDateClick() }
                            .padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = "তারিখ:",
                            style = TextStyle(
                                fontFamily = HeadingFontFamily,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = BengaliUtils.toBengaliDigits(state.dateString),
                            style = TextStyle(
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            modifier = Modifier.size(21.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "তারিখ পরিবর্তন",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Compact Action Buttons (Add Item & Quick Preset), full width so
                    // each button gets equal, predictable space and nothing clips off-screen
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Compact Add Item Button
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .weight(1f)
                                .height(30.dp)
                                .clickable(enabled = canAddItem) { onAddItemRow() }
                                .testTag("add_item_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "নতুন আইটেম",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "আইটেম",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        // Compact Quick Item Selector Button
                        QuickItemSelectorButton(
                            presets = quickPresets,
                            addedItemNames = addedItemNames,
                            onPresetClick = onPresetClick,
                            onManagePresetsClick = { showManagePresetsDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (showManagePresetsDialog) {
                    ManageQuickPresetsDialog(
                        presets = quickPresets,
                        onDismiss = { showManagePresetsDialog = false },
                        onAddPresetWithParent = onAddCustomPresetWithParent,
                        onAddFolder = onAddFolder,
                        onUpdateNode = onUpdatePresetNode,
                        onRemovePreset = onRemovePreset,
                        onResetDefaults = onResetDefaults,
                        onReorderPresetWithParent = onReorderPresetWithParent
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Table Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(4.dp))
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(20.dp))
                    Text(
                        text = "বিবরণ",
                        modifier = Modifier.weight(1.8f),
                        style = TextStyle(
                            fontFamily = HeadingFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Text(
                        text = if (isTransport) "মাধ্যম" else "পরিমাণ",
                        modifier = Modifier.weight(1.4f),
                        style = TextStyle(
                            fontFamily = HeadingFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                    )
                    Text(
                        text = if (isTransport) "যাত্রী" else "দর",
                        modifier = Modifier.weight(0.9f),
                        style = TextStyle(
                            fontFamily = HeadingFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                    )
                    Text(
                        text = "টাকা",
                        modifier = Modifier.weight(1.1f),
                        style = TextStyle(
                            fontFamily = HeadingFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.End
                        )
                    )
                    Spacer(modifier = Modifier.width(32.dp))
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Item Rows
                var draggedItemId by remember { mutableStateOf<String?>(null) }
                var itemDragOffsetY by remember { mutableFloatStateOf(0f) }
                var activeAutoScrollSpeed by remember { mutableFloatStateOf(0f) }
                val itemRowHeights = remember { mutableStateMapOf<String, Int>() }
                val rowWindowTops = remember { mutableStateMapOf<String, Float>() }

                var initialRowWindowTop by remember { mutableFloatStateOf(0f) }
                var initialTouchInRowY by remember { mutableFloatStateOf(0f) }
                var totalTouchDragY by remember { mutableFloatStateOf(0f) }

                val currentBillItems by rememberUpdatedState(state.items)
                val currentOnMoveItem by rememberUpdatedState(onMoveItem)

                val density = LocalDensity.current
                val configuration = LocalConfiguration.current
                val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

                val viewportTop = maxOf(containerBoundsInWindow.top, with(density) { 60.dp.toPx() })
                val viewportBottom = if (containerBoundsInWindow.bottom > 0f) minOf(containerBoundsInWindow.bottom, screenHeightPx - with(density) { 60.dp.toPx() }) else screenHeightPx - with(density) { 60.dp.toPx() }

                val scrollThreshold = with(density) { 100.dp.toPx() }
                val maxScrollSpeed = with(density) { 20.dp.toPx() }

                LaunchedEffect(draggedItemId) {
                    if (draggedItemId == null) return@LaunchedEffect
                    while (isActive && draggedItemId != null) {
                        val speed = activeAutoScrollSpeed
                        if (speed != 0f && scrollState != null) {
                            val consumed = scrollState.scrollBy(speed)
                            if (consumed != 0f) {
                                initialRowWindowTop -= consumed
                                itemDragOffsetY += consumed

                                val list = currentBillItems
                                val curIndex = list.indexOfFirst { it.id == draggedItemId }
                                if (curIndex != -1) {
                                    if (itemDragOffsetY < 0f && curIndex > 0) {
                                        val prevItem = list[curIndex - 1]
                                        val prevHeight = maxOf((itemRowHeights[prevItem.id] ?: 48).toFloat(), 30f)
                                        if (itemDragOffsetY < -(prevHeight * 0.45f)) {
                                            currentOnMoveItem(curIndex, curIndex - 1)
                                            itemDragOffsetY += prevHeight
                                        }
                                    } else if (itemDragOffsetY > 0f && curIndex < list.lastIndex) {
                                        val nextItem = list[curIndex + 1]
                                        val nextHeight = maxOf((itemRowHeights[nextItem.id] ?: 48).toFloat(), 30f)
                                        if (itemDragOffsetY > (nextHeight * 0.45f)) {
                                            currentOnMoveItem(curIndex, curIndex + 1)
                                            itemDragOffsetY -= nextHeight
                                        }
                                    }
                                }
                            } else {
                                activeAutoScrollSpeed = 0f
                            }
                        }
                        kotlinx.coroutines.delay(16)
                    }
                }

                state.items.forEachIndexed { index, item ->
                    key(item.id) {
                        val rowRequesters = itemFocusRequesters.getOrNull(index)
                        val nextItemRowNameRequester = itemFocusRequesters.getOrNull(index + 1)?.getOrNull(0)
                        val nextTargetRequester = nextItemRowNameRequester
                        val isBeingDragged = draggedItemId == item.id

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { coords ->
                                    rowWindowTops[item.id] = coords.positionInWindow().y
                                    itemRowHeights[item.id] = coords.size.height
                                }
                                .graphicsLayer {
                                    translationY = if (isBeingDragged) itemDragOffsetY else 0f
                                }
                                .zIndex(if (isBeingDragged) 10f else 0f)
                                .background(if (isBeingDragged) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                        ) {
                            MemoItemRow(
                                index = index,
                                item = item,
                                isTransport = isTransport,
                                nameFocusRequester = rowRequesters?.getOrNull(0) ?: remember { FocusRequester() },
                                qtyFocusRequester = rowRequesters?.getOrNull(1) ?: remember { FocusRequester() },
                                rateFocusRequester = rowRequesters?.getOrNull(2) ?: remember { FocusRequester() },
                                amountFocusRequester = rowRequesters?.getOrNull(3) ?: remember { FocusRequester() },
                                nextTargetRequester = nextTargetRequester,
                                onNameChange = { onUpdateItemName(item.id, it) },
                                onQtyChange = { onUpdateItemQty(item.id, it) },
                                onRateChange = { onUpdateItemRate(item.id, it) },
                                onTravelerChange = { onUpdateItemTraveler(item.id, it) },
                                onAmountChange = { onUpdateItemAmount(item.id, it) },
                                onRemove = { onRemoveItem(item.id) },
                                dragHandleModifier = Modifier.pointerInput(Unit) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { offset ->
                                            draggedItemId = item.id
                                            itemDragOffsetY = 0f
                                            activeAutoScrollSpeed = 0f
                                            initialTouchInRowY = offset.y
                                            initialRowWindowTop = rowWindowTops[item.id] ?: 0f
                                            totalTouchDragY = 0f
                                        },
                                        onDragEnd = {
                                            draggedItemId = null
                                            itemDragOffsetY = 0f
                                            activeAutoScrollSpeed = 0f
                                        },
                                        onDragCancel = {
                                            draggedItemId = null
                                            itemDragOffsetY = 0f
                                            activeAutoScrollSpeed = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            itemDragOffsetY += dragAmount.y
                                            totalTouchDragY += dragAmount.y

                                            val list = currentBillItems
                                            val curIndex = list.indexOfFirst { it.id == draggedItemId }
                                            if (curIndex != -1) {
                                                if (itemDragOffsetY < 0f && curIndex > 0) {
                                                    val prevItem = list[curIndex - 1]
                                                    val prevHeight = maxOf((itemRowHeights[prevItem.id] ?: 48).toFloat(), 30f)
                                                    if (itemDragOffsetY < -(prevHeight * 0.45f)) {
                                                        currentOnMoveItem(curIndex, curIndex - 1)
                                                        itemDragOffsetY += prevHeight
                                                    }
                                                } else if (itemDragOffsetY > 0f && curIndex < list.lastIndex) {
                                                    val nextItem = list[curIndex + 1]
                                                    val nextHeight = maxOf((itemRowHeights[nextItem.id] ?: 48).toFloat(), 30f)
                                                    if (itemDragOffsetY > (nextHeight * 0.45f)) {
                                                        currentOnMoveItem(curIndex, curIndex + 1)
                                                        itemDragOffsetY -= nextHeight
                                                    }
                                                }
                                            }

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
                                    )
                                }
                            )
                        }
                        Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Total Summary Row with Brass Dashed/Stitched Border
                val primaryColor = MaterialTheme.colorScheme.primary
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            val stroke = Stroke(
                                width = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                            )
                            drawRoundRect(
                                color = primaryColor,
                                style = stroke,
                                cornerRadius = CornerRadius(8.dp.toPx())
                            )
                        }
                        .background(MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "মোট — ",
                            style = TextStyle(
                                fontFamily = HeadingFontFamily,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                        Text(
                            text = BengaliUtils.formatBengaliCurrency(state.totalAmount),
                            style = TextStyle(
                                fontFamily = HeadingFontFamily,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("total_amount_text")
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BillTypeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            width = if (selected) 0.dp else 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier
            .height(34.dp)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
fun MemoItemRow(
    index: Int,
    item: BillItem,
    isTransport: Boolean = false,
    nameFocusRequester: FocusRequester,
    qtyFocusRequester: FocusRequester,
    rateFocusRequester: FocusRequester,
    amountFocusRequester: FocusRequester,
    nextTargetRequester: FocusRequester?,
    onNameChange: (String) -> Unit,
    onQtyChange: (String) -> Unit,
    onRateChange: (String) -> Unit,
    onTravelerChange: (String) -> Unit = {},
    onAmountChange: (String) -> Unit,
    onRemove: () -> Unit,
    dragHandleModifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Drag handle: long-press and drag to reorder this item
        Icon(
            imageVector = Icons.Default.DragHandle,
            contentDescription = "টেনে অবস্থান পরিবর্তন করুন",
            tint = Color.Gray,
            modifier = dragHandleModifier
                .size(18.dp)
                .testTag("drag_handle_item_$index")
        )
        Spacer(modifier = Modifier.width(2.dp))

        // Description (Name)
        Box(
            modifier = Modifier
                .weight(1.8f)
                .height(40.dp)
                .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(4.dp))
                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (item.name.isEmpty()) {
                Text(
                    text = if (isTransport) "রুট (যেমন: সাহাবাগ থেকে কুড়িল)" else "আইটেমের নাম",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 14.sp
                )
            }
            val nameFontSize = when {
                item.name.length > 35 -> 11.sp
                item.name.length > 20 -> 12.5.sp
                else -> 14.sp
            }
            BasicTextField(
                value = item.name,
                onValueChange = onNameChange,
                textStyle = TextStyle(
                    fontSize = nameFontSize,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { qtyFocusRequester.requestFocus() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(nameFocusRequester)
                    .testTag("item_name_input_$index")
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Quantity
        var showUnitMenu by remember { mutableStateOf(false) }
        val commonUnits = if (isTransport) {
            listOf("বাস", "রিকশা", "সিএনজি", "বাইক", "লেগুনা", "অন্যান্য")
        } else {
            listOf("কেজি", "লিটার", "পোয়া", "পিস", "প্যাকেট", "গ্রাম", "ডজন", "আঁটি")
        }

        Box(
            modifier = Modifier
                .weight(1.4f)
                .height(40.dp)
                .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(4.dp))
                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (item.quantity.isEmpty()) {
                        Text(
                            text = if (isTransport) "মাধ্যম" else "পরিমাণ",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    BasicTextField(
                        value = item.quantity,
                        onValueChange = { newValue ->
                            // Transport mode's "মাধ্যম" field holds free text (a custom medium
                            // name like "লেগুনা" or "উবার"), not a quantity, so typed letters
                            // must pass through as-is instead of being digit-converted.
                            onQtyChange(if (isTransport) newValue else BengaliUtils.toBengaliDigits(newValue))
                        },
                        textStyle = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            // Market mode counts units (numeric), transport mode names a
                            // medium (text) — forcing the numeric keyboard here was blocking
                            // users from typing a custom medium name at all.
                            keyboardType = if (isTransport) KeyboardType.Text else KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(onNext = { rateFocusRequester.requestFocus() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(qtyFocusRequester)
                            .testTag("item_qty_input_$index")
                    )
                }

                // Small dropdown unit selector button
                Box {
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clickable { showUnitMenu = true }
                            .padding(horizontal = 2.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "▼",
                            fontSize = 8.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(2.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showUnitMenu,
                        onDismissRequest = { showUnitMenu = false }
                    ) {
                        commonUnits.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(unit, fontSize = 12.sp) },
                                onClick = {
                                    showUnitMenu = false
                                    // Transport mode ("মাধ্যম"): just record the medium itself
                                    // (বাস / রিকশা / ...) — no forced quantity number, since a
                                    // fare row isn't counted in units the way a market item is.
                                    val newQty = if (isTransport) {
                                        unit
                                    } else {
                                        val digits = item.quantity.filter { it.isDigit() || it == '.' || it in '০'..'৯' }
                                        val bnDigits = BengaliUtils.toBengaliDigits(digits)
                                        if (bnDigits.isNotBlank()) "$bnDigits $unit" else "১ $unit"
                                    }
                                    onQtyChange(newQty)
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Rate (market bills) / Traveler name (transport bills — "কে যাতায়াত করছে")
        Box(
            modifier = Modifier
                .weight(0.9f)
                .height(40.dp)
                .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(4.dp))
                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isTransport) {
                val displayTraveler = if (item.rate == "0") "" else item.rate
                BasicTextField(
                    value = displayTraveler,
                    onValueChange = { onTravelerChange(it) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { amountFocusRequester.requestFocus() }),
                    textStyle = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(rateFocusRequester)
                        .testTag("item_traveler_input_$index")
                )
                if (displayTraveler.isBlank()) {
                    Text(
                        text = "কে যাচ্ছেন",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        fontSize = 10.5.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                val displayRate = if (item.rate == "0") "" else BengaliUtils.toBengaliDigits(item.rate)
                BasicTextField(
                    value = displayRate,
                    onValueChange = { onRateChange(BengaliUtils.toBengaliDigits(it)) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onNext = { amountFocusRequester.requestFocus() }),
                    textStyle = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(rateFocusRequester)
                        .testTag("item_rate_input_$index")
                )
                if (item.rate == "0" || item.rate.isEmpty()) {
                    Text(
                        text = "০",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Amount
        val displayAmount = if (item.amount <= 0) "" else BengaliUtils.toBengaliDigits(if (item.amount % 1.0 == 0.0) item.amount.toLong().toString() else String.format(java.util.Locale.US, "%.1f", item.amount))
        Box(
            modifier = Modifier
                .weight(1.1f)
                .height(40.dp)
                .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(4.dp))
                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            BasicTextField(
                value = displayAmount,
                onValueChange = { onAmountChange(BengaliUtils.toBengaliDigits(it)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = if (nextTargetRequester != null) ImeAction.Next else ImeAction.Done
                ),
                keyboardActions = if (nextTargetRequester != null) {
                    KeyboardActions(onNext = { nextTargetRequester.requestFocus() })
                } else {
                    KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    })
                },
                textStyle = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(amountFocusRequester)
                    .testTag("item_amount_input_$index")
            )
            if (displayAmount.isEmpty()) {
                Text(
                    text = "০",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Delete Row Icon in LedgerRed
        IconButton(
            onClick = onRemove,
            modifier = Modifier.testTag("remove_item_button_$index")
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "আইটেম মুছুন",
                tint = LedgerRed
            )
        }
    }
}

@Composable
fun SawtoothDivider() {
    val toothColor = MaterialTheme.colorScheme.primary
    val ruleGold = MaterialTheme.colorScheme.secondary
    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clipToBounds()
        ) {
            val width = size.width
            val numTeeth = (width / 16f).toInt().coerceAtLeast(1)
            val toothWidth = width / numTeeth
            val toothHeight = size.height
            val path = Path()

            path.moveTo(0f, 0f)
            var x = 0f
            var isUp = false
            for (i in 0 until numTeeth) {
                x += toothWidth
                val y = if (isUp) 0f else toothHeight
                path.lineTo(x, y)
                isUp = !isUp
            }
            path.lineTo(width, 0f)
            path.close()

            drawPath(path = path, color = toothColor)
        }

        // Double rule line (Classic Gold / Accent + Ledger Red)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(ruleGold)
        )
        Spacer(modifier = Modifier.height(1.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(LedgerRed)
        )
    }
}

@Composable
fun QuickItemSelectorButton(
    presets: List<QuickPreset>,
    addedItemNames: Set<String>,
    onPresetClick: (name: String, qty: String, rate: String, amount: String) -> Unit,
    onManagePresetsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedDropdown by remember { mutableStateOf(false) }
    var expandedFolderIds by remember { mutableStateOf(setOf<String>()) }
    val transitionState = remember {
        MutableTransitionState(false).apply { targetState = false }
    }

    LaunchedEffect(expandedDropdown) {
        transitionState.targetState = expandedDropdown
    }

    val isPopupVisible = transitionState.currentState || transitionState.targetState

    Box(modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .clickable { expandedDropdown = !expandedDropdown }
                .testTag("quick_item_select_button")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = if (expandedDropdown) "দ্রুত ▴" else "দ্রুত ▾",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        if (isPopupVisible) {
            Popup(
                onDismissRequest = { expandedDropdown = false },
                properties = PopupProperties(
                    focusable = true,
                    dismissOnClickOutside = true,
                    dismissOnBackPress = true
                )
            ) {
                // Dimmed Backdrop
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { expandedDropdown = false },
                    contentAlignment = Alignment.TopCenter
                ) {
                    // Shutter animation container
                    AnimatedVisibility(
                        visibleState = transitionState,
                        enter = expandVertically(
                            animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
                            expandFrom = Alignment.Top
                        ) + fadeIn(animationSpec = tween(durationMillis = 220)),
                        exit = shrinkVertically(
                            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                            shrinkTowards = Alignment.Top
                        ) + fadeOut(animationSpec = tween(durationMillis = 180)),
                        modifier = Modifier
                            .padding(top = 70.dp, start = 16.dp, end = 16.dp)
                            .widthIn(max = 460.dp)
                            .fillMaxWidth()
                            .clipToBounds()
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CreamPaperBg),
                            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                            border = BorderStroke(1.5.dp, DarkForestGreen.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {} // block clicks from closing backdrop
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 14.dp)
                            ) {
                                // Header Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = DarkForestGreen.copy(alpha = 0.12f),
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = null,
                                                    tint = DarkForestGreen,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "দ্রুত প্রিসেট নির্বাচন করুন",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DarkForestGreen,
                                            fontFamily = HeadingFontFamily
                                        )
                                    }

                                    IconButton(
                                        onClick = { expandedDropdown = false },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "বন্ধ করুন",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Divider(color = WarmBorderColor, thickness = 1.dp)
                                Spacer(modifier = Modifier.height(10.dp))

                                // Item List
                                if (presets.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "কোনো প্রিসেট আইটেম সংরক্ষিত নেই",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color.Gray
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "নিচের বাটনে ট্যাপ করে নতুন প্রিসেট যোগ করুন",
                                                fontSize = 11.sp,
                                                color = Color.LightGray
                                            )
                                        }
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 320.dp)
                                            .verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        QuickItemSelectorTree(
                                            nodes = presets,
                                            addedItemNames = addedItemNames,
                                            expandedFolderIds = expandedFolderIds,
                                            onToggleFolder = { id ->
                                                expandedFolderIds = if (expandedFolderIds.contains(id)) expandedFolderIds - id else expandedFolderIds + id
                                            },
                                            onPresetClick = onPresetClick
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Divider(color = WarmBorderColor, thickness = 1.dp)
                                Spacer(modifier = Modifier.height(8.dp))

                                // Footer action button
                                Button(
                                    onClick = {
                                        expandedDropdown = false
                                        onManagePresetsClick()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFEAE3D9),
                                        contentColor = DarkForestGreen
                                    )
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = null,
                                            tint = DarkForestGreen,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "প্রিসেট ম্যানেজ / এডিট করুন",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DarkForestGreen
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

@Composable
private fun QuickItemSelectorTree(
    nodes: List<QuickPreset>,
    addedItemNames: Set<String>,
    expandedFolderIds: Set<String>,
    onToggleFolder: (String) -> Unit,
    onPresetClick: (name: String, qty: String, rate: String, amount: String) -> Unit,
    level: Int = 0
) {
    nodes.forEach { preset ->
        if (preset.isFolder) {
            val isExpanded = expandedFolderIds.contains(preset.id)
            val childItems = preset.children.getAllFlatItems()
            val addedCount = childItems.count { addedItemNames.contains(it.name.trim()) }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = (level * 10).dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DarkForestGreen.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, DarkForestGreen.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleFolder(preset.id) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                                contentDescription = null,
                                tint = DarkForestGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${preset.name} (${childItems.size})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkForestGreen
                            )
                            if (addedCount > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "[$addedCount যোগ করা]",
                                    fontSize = 10.sp,
                                    color = DarkForestGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            tint = DarkForestGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (isExpanded) {
                    Spacer(modifier = Modifier.height(4.dp))
                    QuickItemSelectorTree(
                        nodes = preset.children,
                        addedItemNames = addedItemNames,
                        expandedFolderIds = expandedFolderIds,
                        onToggleFolder = onToggleFolder,
                        onPresetClick = onPresetClick,
                        level = level + 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        } else {
            val isAdded = addedItemNames.contains(preset.name.trim())
            val formattedText = BengaliUtils.formatPresetDisplayText(preset)

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isAdded) DarkForestGreen.copy(alpha = 0.10f) else Color.White,
                border = BorderStroke(
                    width = if (isAdded) 1.2.dp else 1.dp,
                    color = if (isAdded) DarkForestGreen.copy(alpha = 0.45f) else WarmBorderColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = (level * 10).dp)
                    .clickable {
                        onPresetClick(
                            preset.name,
                            preset.defaultQty,
                            preset.defaultRate,
                            preset.defaultAmount
                        )
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedText,
                        fontSize = 13.5.sp,
                        fontWeight = if (isAdded) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isAdded) DarkForestGreen else Color(0xFF222222),
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    if (isAdded) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = DarkForestGreen
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "যোগ করা আছে",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = DarkForestGreen.copy(alpha = 0.08f),
                            border = BorderStroke(0.8.dp, DarkForestGreen.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = DarkForestGreen,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "যোগ করুন",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = DarkForestGreen
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

