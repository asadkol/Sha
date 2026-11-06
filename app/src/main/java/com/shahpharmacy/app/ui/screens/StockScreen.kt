package com.shahpharmacy.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shahpharmacy.app.data.model.StockItem
import com.shahpharmacy.app.ui.PharmacyUiState
import com.shahpharmacy.app.ui.components.ExcelPurchaseImportDialog
import com.shahpharmacy.app.ui.components.HeroBanner
import com.shahpharmacy.app.ui.components.ThermalReceiptDialog
import com.shahpharmacy.app.ui.components.formatMoney
import com.shahpharmacy.app.ui.theme.*
import com.shahpharmacy.app.util.ParsedPurchaseOrderRow
import com.shahpharmacy.app.util.ThermalPrinterHelper

enum class StockFilter(val label: String) {
    ALL("All"),
    LOW_STOCK("⚠️ Low Stock"),
    OUT_OF_STOCK("⛔ Out of Stock"),
    EXPIRING("⏳ Expiring"),
    IN_STOCK("✅ Healthy")
}

@Composable
fun StockScreen(
    state: PharmacyUiState,
    onNavigateBack: () -> Unit,
    onSetLowStockThreshold: (Int) -> Unit = {},
    onImportExcelPurchaseOrder: (List<ParsedPurchaseOrderRow>) -> Unit = {},
    onNavigateToPurchase: (() -> Unit)? = null
) {
    BackHandler { onNavigateBack() }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(StockFilter.ALL) }
    var showThresholdDialog by remember { mutableStateOf(false) }
    var tempThresholdStr by remember(state.lowStockThreshold) { mutableStateOf(state.lowStockThreshold.toString()) }

    // Thermal Receipt Dialog State
    var showThermalDialog by remember { mutableStateOf(false) }
    var thermalReceiptText by remember { mutableStateOf("") }
    var thermalReceiptTitle by remember { mutableStateOf("") }

    // Excel Purchase Order Dialog State
    var showExcelImportDialog by remember { mutableStateOf(false) }

    val lowStockList = remember(state.stock, state.lowStockThreshold) {
        state.stock.filter { it.isLowStock }
    }
    val outOfStockList = remember(state.stock) {
        state.stock.filter { it.isOutOfStock }
    }
    val expiringList = remember(state.stock) {
        state.stock.filter { it.isExpiringSoon }
    }
    val inStockList = remember(state.stock) {
        state.stock.filter { !it.isLowStock }
    }

    val filteredStock = remember(state.stock, searchQuery, selectedFilter) {
        val baseList = when (selectedFilter) {
            StockFilter.ALL -> state.stock
            StockFilter.LOW_STOCK -> lowStockList
            StockFilter.OUT_OF_STOCK -> outOfStockList
            StockFilter.EXPIRING -> expiringList
            StockFilter.IN_STOCK -> inStockList
        }

        if (searchQuery.isBlank()) {
            baseList
        } else {
            val q = searchQuery.trim().lowercase()
            baseList.filter {
                it.name.lowercase().contains(q) || it.company.lowercase().contains(q)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("stock_screen")
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("Inventory & Stock Alert", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        }

        item {
            HeroBanner(
                title = "Inventory Alert System",
                subtitle = "Automated stock tracking, thermal receipt printing, and Excel reorders."
            )
        }

        // Action Toolbar: Thermal Print & Excel Upload
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Inventory Actions & Tools",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = NavyPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                thermalReceiptTitle = "Reorder Purchase Slip"
                                thermalReceiptText = ThermalPrinterHelper.generateLowStockOrderReceipt(
                                    lowStockItems = if (lowStockList.isNotEmpty()) lowStockList else state.stock.take(5),
                                    threshold = state.lowStockThreshold
                                )
                                showThermalDialog = true
                            },
                            modifier = Modifier
                                .weight(1.2f)
                                .height(42.dp)
                                .testTag("thermal_print_reorder_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BlueSecondary)
                        ) {
                            Text("🖨️ Thermal Reorder", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                showExcelImportDialog = true
                            },
                            modifier = Modifier
                                .weight(1.2f)
                                .height(42.dp)
                                .testTag("upload_excel_po_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                        ) {
                            Text("📊 Upload Excel PO", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                thermalReceiptTitle = "Inventory Stock Audit"
                                thermalReceiptText = ThermalPrinterHelper.generateInventoryAuditReceipt(state.stock)
                                showThermalDialog = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("thermal_print_audit_btn"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Audit Slip", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Automated Low Stock Banner
        if (lowStockList.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("automated_low_stock_banner"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = if (outOfStockList.isNotEmpty()) Color(0xFFFFF1F2) else Color(0xFFFFFBEB)),
                    border = BorderStroke(1.5.dp, if (outOfStockList.isNotEmpty()) RedExpense else OrangePurchase)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(if (outOfStockList.isNotEmpty()) "⛔" else "⚠️", fontSize = 18.sp)
                                Text(
                                    text = "Automated Stock Alert (${lowStockList.size} items below threshold)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (outOfStockList.isNotEmpty()) RedExpense else OrangePurchase
                                )
                            }

                            TextButton(
                                onClick = { showThresholdDialog = true },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Threshold: ≤ ${state.lowStockThreshold}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Text(
                            text = if (outOfStockList.isNotEmpty()) {
                                "${outOfStockList.size} items are completely OUT OF STOCK and ${lowStockList.size - outOfStockList.size} are running critically low."
                            } else {
                                "Highlighted items have quantities at or below ${state.lowStockThreshold} units. Reorder recommended."
                            },
                            fontSize = 12.sp,
                            color = TextPrimary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    thermalReceiptTitle = "Urgent Purchase Reorder Slip"
                                    thermalReceiptText = ThermalPrinterHelper.generateLowStockOrderReceipt(
                                        lowStockItems = lowStockList,
                                        threshold = state.lowStockThreshold
                                    )
                                    showThermalDialog = true
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RedExpense)
                            ) {
                                Text("Print Reorder Slip", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { selectedFilter = StockFilter.LOW_STOCK },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Filter Low Stock (${lowStockList.size})", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Search & Threshold Controller Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search medicine or company...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("stock_search_input"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Alert Threshold: ≤ ${state.lowStockThreshold} units",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (state.lowStockThreshold > 1) {
                                        onSetLowStockThreshold(state.lowStockThreshold - 1)
                                    }
                                },
                                modifier = Modifier.size(32.dp),
                                contentPadding = PaddingValues(0.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("-", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFF1F5F9))
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${state.lowStockThreshold}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NavyPrimary
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    onSetLowStockThreshold(state.lowStockThreshold + 1)
                                },
                                modifier = Modifier.size(32.dp),
                                contentPadding = PaddingValues(0.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Filter Chips (All, Low Stock, Out of Stock, Expiring, Healthy)
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(StockFilter.values()) { filter ->
                    val isSelected = selectedFilter == filter
                    val count = when (filter) {
                        StockFilter.ALL -> state.stock.size
                        StockFilter.LOW_STOCK -> lowStockList.size
                        StockFilter.OUT_OF_STOCK -> outOfStockList.size
                        StockFilter.EXPIRING -> expiringList.size
                        StockFilter.IN_STOCK -> inStockList.size
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = filter },
                        label = {
                            Text(
                                text = "${filter.label} ($count)",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = when (filter) {
                                StockFilter.OUT_OF_STOCK -> RedExpense
                                StockFilter.LOW_STOCK -> OrangePurchase
                                StockFilter.EXPIRING -> Color(0xFFD97706)
                                else -> NavyPrimary
                            },
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // KPI Summary Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Cost Value", fontSize = 10.sp, color = TextMuted)
                        Text(
                            formatMoney(state.totalStockPurchaseValue),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = OrangePurchase
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Sale Value", fontSize = 10.sp, color = TextMuted)
                        Text(
                            formatMoney(state.totalStockSaleValue),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = BlueSale
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Est. Margin", fontSize = 10.sp, color = TextMuted)
                        Text(
                            formatMoney(state.potentialGrossMargin),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = GreenCash
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Inventory Items (${filteredStock.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextPrimary
            )
        }

        if (filteredStock.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (selectedFilter != StockFilter.ALL) "No items matching filter: ${selectedFilter.label}" else "No inventory items recorded yet.",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            items(filteredStock, key = { it.name }) { item ->
                // Visually Distinct Card Styling Based On Stock Alert Status
                val isOutOfStock = item.isOutOfStock
                val isCritical = item.isCritical
                val isLowStock = item.isLowStock

                val cardBg = when {
                    isOutOfStock -> Color(0xFFFFF1F2)
                    isCritical -> Color(0xFFFFF7ED)
                    isLowStock -> Color(0xFFFFFBEB)
                    else -> Color.White
                }

                val borderStroke = when {
                    isOutOfStock -> BorderStroke(2.dp, RedExpense)
                    isCritical -> BorderStroke(1.5.dp, OrangePurchase)
                    isLowStock -> BorderStroke(1.2.dp, Color(0xFFF59E0B))
                    else -> BorderStroke(1.dp, CardBorder)
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stock_item_${item.name.replace(" ", "_")}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = borderStroke
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Header with Name & Alert Badges
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (isOutOfStock) {
                                        Text("⛔", fontSize = 14.sp)
                                    } else if (isLowStock) {
                                        Text("⚠️", fontSize = 14.sp)
                                    }
                                    Text(
                                        text = item.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (isOutOfStock) RedExpense else TextPrimary
                                    )
                                }
                                if (item.company.isNotBlank()) {
                                    Text(
                                        text = item.company,
                                        fontSize = 12.sp,
                                        color = TextMuted
                                    )
                                }
                            }

                            // Alert Status Badges
                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            when {
                                                isOutOfStock -> RedExpense
                                                isCritical -> OrangePurchase
                                                isLowStock -> Color(0xFFF59E0B)
                                                else -> GreenLight
                                            }
                                        )
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = when {
                                            isOutOfStock -> "OUT OF STOCK"
                                            isCritical -> "CRITICAL (${item.qty})"
                                            isLowStock -> "LOW STOCK (${item.qty})"
                                            else -> "${item.qty} in stock"
                                        },
                                        color = if (isLowStock) Color.White else GreenCash,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (item.isExpiringSoon) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(OrangeLight)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("⏳ Exp: ${item.expiryDate}", fontSize = 10.sp, color = OrangePurchase, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFFE2E8F0))

                        // Quantity, Cost & Valuation Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Cost / Unit", fontSize = 10.sp, color = TextMuted)
                                Text(formatMoney(item.purchase), fontSize = 12.sp, color = OrangePurchase)
                            }
                            Column {
                                Text("Sale / Unit", fontSize = 10.sp, color = TextMuted)
                                Text(formatMoney(item.sale), fontSize = 12.sp, color = BlueSale)
                            }
                            Column {
                                Text("Total Valuation", fontSize = 10.sp, color = TextMuted)
                                Text(formatMoney(item.purchaseValue), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }

                            // Action button per card
                            if (isLowStock) {
                                Button(
                                    onClick = {
                                        thermalReceiptTitle = "Reorder - ${item.name}"
                                        thermalReceiptText = ThermalPrinterHelper.generateLowStockOrderReceipt(
                                            lowStockItems = listOf(item),
                                            threshold = state.lowStockThreshold
                                        )
                                        showThermalDialog = true
                                    },
                                    modifier = Modifier.height(32.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = if (isOutOfStock) RedExpense else OrangePurchase)
                                ) {
                                    Text("Reorder Slip", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Thermal Receipt Dialog
    if (showThermalDialog) {
        ThermalReceiptDialog(
            receiptText = thermalReceiptText,
            title = thermalReceiptTitle,
            onDismiss = { showThermalDialog = false }
        )
    }

    // Excel Purchase Order Upload Dialog
    if (showExcelImportDialog) {
        ExcelPurchaseImportDialog(
            onDismiss = { showExcelImportDialog = false },
            onConfirmImport = { rows ->
                onImportExcelPurchaseOrder(rows)
            }
        )
    }

    // Threshold Customizer Dialog
    if (showThresholdDialog) {
        AlertDialog(
            onDismissRequest = { showThresholdDialog = false },
            title = { Text("Configure Low Stock Threshold") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Medicines with quantities at or below this number will be flagged with automated inventory alerts.",
                        fontSize = 13.sp,
                        color = TextMuted
                    )
                    OutlinedTextField(
                        value = tempThresholdStr,
                        onValueChange = { tempThresholdStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Threshold Units") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val num = tempThresholdStr.toIntOrNull() ?: 5
                        onSetLowStockThreshold(num)
                        showThresholdDialog = false
                    }
                ) {
                    Text("Apply Threshold")
                }
            },
            dismissButton = {
                TextButton(onClick = { showThresholdDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
