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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FoodBillUiModel
import com.example.ui.theme.LedgerRed
import com.example.util.BengaliUtils
import com.example.util.FoodBillImageExporter
import com.example.util.PrintMemoData

@Composable
fun BillHistoryList(
    bills: List<FoodBillUiModel>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onEditBill: (FoodBillUiModel) -> Unit,
    onPrintBill: (FoodBillUiModel) -> Unit,
    onPrintDualBills: (FoodBillUiModel, FoodBillUiModel) -> Unit,
    onSharePdfBill: (FoodBillUiModel) -> Unit,
    onShareImageBill: ((FoodBillUiModel) -> Unit)? = null,
    onSaveImageBill: ((FoodBillUiModel) -> Unit)? = null,
    onDeleteBill: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var billToDelete by remember { mutableStateOf<FoodBillUiModel?>(null) }
    val selectedBillIds = remember { mutableStateListOf<Long>() }
    var selectedBillTypeFilter by remember { mutableStateOf("all") } // "all", "market", "transport"

    val marketCount = remember(bills) { bills.count { it.billType == "market" } }
    val transportCount = remember(bills) { bills.count { it.billType == "transport" } }

    val filteredBills = remember(bills, searchQuery, selectedBillTypeFilter) {
        var list = bills
        if (selectedBillTypeFilter != "all") {
            list = list.filter { it.billType == selectedBillTypeFilter }
        }
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.lowercase().trim()
            val englishQ = BengaliUtils.toEnglishDigits(q)
            val bengaliQ = BengaliUtils.toBengaliDigits(q)
            list = list.filter { bill ->
                bill.id.toString().contains(q) ||
                bill.id.toString().contains(englishQ) ||
                bill.dateString.lowercase().contains(q) ||
                bill.dateString.lowercase().contains(englishQ) ||
                bill.dateString.lowercase().contains(bengaliQ) ||
                bill.purchaserName.lowercase().contains(q) ||
                bill.items.any { item -> 
                    item.name.lowercase().contains(q) || 
                    item.quantity.lowercase().contains(q) ||
                    item.quantity.lowercase().contains(englishQ) ||
                    item.quantity.lowercase().contains(bengaliQ)
                }
            }
        }
        list
    }

    val totalSpent = remember(filteredBills) {
        filteredBills.sumOf { it.totalAmount }
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("মেমো নম্বর, তারিখ বা আইটেম দিয়ে খুঁজুন...") },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp),
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                keyboardController?.hide()
            }),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_history_input")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Bill Type Filter Chips (খাবার বিল / যাতায়াত ভাড়া)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedBillTypeFilter == "all",
                onClick = { selectedBillTypeFilter = "all" },
                label = {
                    Text(
                        text = "সকল (${BengaliUtils.toBengaliDigits(bills.size.toString())})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = Color.White
                ),
                modifier = Modifier.weight(1f).testTag("filter_chip_all")
            )

            FilterChip(
                selected = selectedBillTypeFilter == "market",
                onClick = { selectedBillTypeFilter = "market" },
                label = {
                    Text(
                        text = "খাবার বিল (${BengaliUtils.toBengaliDigits(marketCount.toString())})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF1565C0),
                    selectedLabelColor = Color.White
                ),
                modifier = Modifier.weight(1.1f).testTag("filter_chip_food_bill")
            )

            FilterChip(
                selected = selectedBillTypeFilter == "transport",
                onClick = { selectedBillTypeFilter = "transport" },
                label = {
                    Text(
                        text = "যাতায়াত ভাড়া (${BengaliUtils.toBengaliDigits(transportCount.toString())})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF00695C),
                    selectedLabelColor = Color.White
                ),
                modifier = Modifier.weight(1.2f).testTag("filter_chip_transport_bill")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Dual Selection Action Banner (When bills are selected for A4 Dual Print)
        if (selectedBillIds.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dual_selection_banner"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        val countText = BengaliUtils.toBengaliDigits(selectedBillIds.size.toString())
                        Text(
                            text = "$countText টি মেমো সিলেক্টেড (A4 ডাবল পেজ)",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (selectedBillIds.size == 1) "অন্য যেকোনো ১টি মেমো সিলেক্ট করুন (উপরে/নিচে সেট করতে)" else "উপরে ও নিচে ২টি মেমো একসাথে প্রিন্ট করা যাবে",
                            fontSize = 11.sp,
                            color = Color.DarkGray
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (selectedBillIds.size == 2) {
                            Button(
                                onClick = {
                                    val b1 = bills.firstOrNull { it.id == selectedBillIds[0] }
                                    val b2 = bills.firstOrNull { it.id == selectedBillIds[1] }
                                    if (b1 != null && b2 != null) {
                                        onPrintDualBills(b1, b2)
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White)
                            ) {
                                Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.height(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("A4 প্রিন্ট", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedButton(
                            onClick = { selectedBillIds.clear() },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("বাতিল", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // Spending summary card with classic premium gradient
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("summary_card"),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.secondary,
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "মোট খরচের হিসাব (${BengaliUtils.toBengaliDigits(filteredBills.size.toString())}টি মেমো)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.88f)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = BengaliUtils.formatBengaliCurrency(totalSpent),
                            fontSize = 23.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0x25FFFFFF), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (filteredBills.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 40.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = if (searchQuery.isBlank()) "এখনো কোনো মেমো সংরক্ষণ করা হয়নি।" else "কোনো সম্পর্কিত মেমো পাওয়া যায়নি।",
                    fontSize = 15.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize().testTag("history_list_column")
            ) {
                items(filteredBills, key = { it.id }) { bill ->
                    val isSelectedForDual = selectedBillIds.contains(bill.id)

                    HistoryBillItemCard(
                        bill = bill,
                        isSelected = isSelectedForDual,
                        onToggleSelect = {
                            if (isSelectedForDual) {
                                selectedBillIds.remove(bill.id)
                            } else {
                                if (selectedBillIds.size >= 2) {
                                    selectedBillIds.removeAt(0)
                                }
                                selectedBillIds.add(bill.id)
                            }
                        },
                        onEdit = { onEditBill(bill) },
                        onPrint = { onPrintBill(bill) },
                        onSharePdf = { onSharePdfBill(bill) },
                        onShareImage = {
                            if (onShareImageBill != null) {
                                onShareImageBill(bill)
                            } else {
                                val printMemo = PrintMemoData(
                                    memoId = bill.id,
                                    centerName = bill.centerName,
                                    subtitle = bill.subtitle,
                                    dateString = bill.dateString,
                                    purchaserName = bill.purchaserName,
                                    items = bill.items.filter { it.name.isNotBlank() || it.amount > 0 },
                                    totalAmount = bill.totalAmount,
                                    billType = bill.billType,
                                    showSignature = bill.showSignature
                                )
                                FoodBillImageExporter.shareMemoImage(context, printMemo)
                            }
                        },
                        onSaveImage = {
                            if (onSaveImageBill != null) {
                                onSaveImageBill(bill)
                            } else {
                                val printMemo = PrintMemoData(
                                    memoId = bill.id,
                                    centerName = bill.centerName,
                                    subtitle = bill.subtitle,
                                    dateString = bill.dateString,
                                    purchaserName = bill.purchaserName,
                                    items = bill.items.filter { it.name.isNotBlank() || it.amount > 0 },
                                    totalAmount = bill.totalAmount,
                                    billType = bill.billType,
                                    showSignature = bill.showSignature
                                )
                                FoodBillImageExporter.saveMemoImageToGallery(context, printMemo)
                            }
                        },
                        onDelete = { billToDelete = bill }
                    )
                }
            }
        }
    }

    // Delete Confirmation Dialog
    billToDelete?.let { bill ->
        AlertDialog(
            onDismissRequest = { billToDelete = null },
            title = { Text("মেমো মুছে ফেলুন") },
            text = { Text("মেমো #${BengaliUtils.toBengaliDigits(bill.id.toString())} (${bill.dateString}) মুছে ফেলতে চান?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteBill(bill.id)
                        selectedBillIds.remove(bill.id)
                        billToDelete = null
                    }
                ) {
                    Text("মুছুন", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { billToDelete = null }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

@Composable
fun HistoryBillItemCard(
    bill: FoodBillUiModel,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onEdit: () -> Unit,
    onPrint: () -> Unit,
    onSharePdf: () -> Unit,
    onShareImage: () -> Unit = {},
    onSaveImage: () -> Unit = {},
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_item_card_${bill.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.5.dp else 0.8.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelect() },
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                    )

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "মেমো #${BengaliUtils.toBengaliDigits(bill.id.toString())}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            // Bill Type Badge
                            val isTransport = bill.billType == "transport"
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isTransport) Color(0xFFE0F2F1) else Color(0xFFE3F2FD))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isTransport) "যাতায়াত" else "খাবার বিল",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isTransport) Color(0xFF00695C) else Color(0xFF1565C0)
                                )
                            }
                        }
                        val formattedTime = remember(bill.timestamp) {
                            BengaliUtils.formatBengaliTime(bill.timestamp)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "তারিখ: ${BengaliUtils.toBengaliDigits(bill.dateString)}",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (formattedTime.isNotBlank()) {
                                Text(
                                    text = "• সময়: $formattedTime",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (bill.purchaserName.isNotBlank()) {
                            Text(
                                text = "ক্রয়কারী: ${bill.purchaserName}",
                                fontSize = 11.5.sp,
                                color = Color.DarkGray
                            )
                        }
                    }
                }

                Text(
                    text = BengaliUtils.formatBengaliCurrency(bill.totalAmount),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(6.dp))

            // Items Preview (up to 4 items)
            val previewItems = bill.items.take(4)
            Text(
                text = previewItems.joinToString(", ") { "${it.name} (${BengaliUtils.toBengaliDigits(it.quantity)})" } +
                        if (bill.items.size > 4) " এবং আরও ${BengaliUtils.toBengaliDigits((bill.items.size - 4).toString())}টি" else "",
                fontSize = 12.5.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Select for A4 Dual Print button
                TextButton(
                    onClick = onToggleSelect,
                    modifier = Modifier.testTag("select_dual_bill_${bill.id}")
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.height(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isSelected) "A4 ডাবলে যুক্ত" else "A4 ডাবলে নির্বাচন",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // Edit
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("edit_history_bill_${bill.id}")
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "সম্পাদনা", tint = MaterialTheme.colorScheme.primary)
                    }

                    // Share Image (Normal list receipt image)
                    IconButton(
                        onClick = onShareImage,
                        modifier = Modifier.testTag("share_image_history_bill_${bill.id}")
                    ) {
                        Icon(imageVector = Icons.Default.Image, contentDescription = "ছবি শেয়ার করুন", tint = Color(0xFF0D47A1))
                    }

                    // Save Image (Save receipt image to gallery)
                    IconButton(
                        onClick = onSaveImage,
                        modifier = Modifier.testTag("save_image_history_bill_${bill.id}")
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = "ছবি সেভ করুন", tint = Color(0xFF00796B))
                    }

                    // Print / PDF Preview
                    IconButton(
                        onClick = onPrint,
                        modifier = Modifier.testTag("print_history_bill_${bill.id}")
                    ) {
                        Icon(imageVector = Icons.Default.Print, contentDescription = "প্রিন্ট ও পিডিএফ", tint = MaterialTheme.colorScheme.primary)
                    }

                    // Delete
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_history_bill_${bill.id}")
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "মুছুন", tint = LedgerRed)
                    }
                }
            }
        }
    }
}
