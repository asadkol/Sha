package com.shahpharmacy.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shahpharmacy.app.data.model.AppScreen
import com.shahpharmacy.app.ui.PharmacyUiState
import com.shahpharmacy.app.ui.components.HeroBanner
import com.shahpharmacy.app.ui.components.formatMoney
import com.shahpharmacy.app.ui.theme.*
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseScreen(
    state: PharmacyUiState,
    onNavigateBack: () -> Unit,
    onAddPurchase: (name: String, company: String, qty: Int, purchase: Double, sale: Double, date: String, batch: String, expiry: String) -> Unit,
    onDeletePurchase: (id: String) -> Unit,
    onImportCsv: (csvText: String) -> Unit
) {
    BackHandler { onNavigateBack() }

    var name by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var qtyStr by remember { mutableStateOf("") }
    var purchasePriceStr by remember { mutableStateOf("") }
    var salePriceStr by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var batch by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    var importCsvText by remember { mutableStateOf("") }

    val filteredPurchases = remember(state.appData.purchases, searchQuery) {
        if (searchQuery.isBlank()) {
            state.appData.purchases.reversed()
        } else {
            val q = searchQuery.trim().lowercase()
            state.appData.purchases.reversed().filter {
                it.name.lowercase().contains(q) || it.company.lowercase().contains(q) || it.batch.lowercase().contains(q)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("purchase_screen")
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
                Text("Purchase Entry", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        }

        item {
            HeroBanner(
                title = "Purchase Entry",
                subtitle = "Record medicine purchases, costs, batches, and supplier records."
            )
        }

        // Add Purchase Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_purchase_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Add Purchased Medicine",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = NavyPrimary
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Medicine name *") },
                        placeholder = { Text("e.g. Panadol 500mg") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("purchase_name_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = company,
                            onValueChange = { company = it },
                            label = { Text("Company / Brand") },
                            placeholder = { Text("e.g. GSK") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("purchase_company_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = qtyStr,
                            onValueChange = { qtyStr = it.filter { char -> char.isDigit() } },
                            label = { Text("Quantity *") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("purchase_qty_input"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = purchasePriceStr,
                            onValueChange = { purchasePriceStr = it },
                            label = { Text("Purchase / unit (Rs.)") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("purchase_price_input"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = salePriceStr,
                            onValueChange = { salePriceStr = it },
                            label = { Text("Sale / unit (Rs.)") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("purchase_sale_price_input"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = date,
                            onValueChange = { date = it },
                            label = { Text("Date (YYYY-MM-DD)") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("purchase_date_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = batch,
                            onValueChange = { batch = it },
                            label = { Text("Batch #") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("purchase_batch_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    OutlinedTextField(
                        value = expiry,
                        onValueChange = { expiry = it },
                        label = { Text("Expiry (YYYY-MM-DD)") },
                        placeholder = { Text("e.g. 2026-12-31") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("purchase_expiry_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    val calcQty = qtyStr.toIntOrNull() ?: 0
                    val calcPrice = purchasePriceStr.toDoubleOrNull() ?: 0.0
                    val totalCalc = calcQty * calcPrice

                    if (totalCalc > 0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(BlueLight)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total Purchase Value:", fontSize = 13.sp, color = TextPrimary)
                            Text(formatMoney(totalCalc), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = NavyPrimary)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val q = qtyStr.toIntOrNull() ?: 0
                                val p = purchasePriceStr.toDoubleOrNull() ?: 0.0
                                val s = salePriceStr.toDoubleOrNull() ?: 0.0
                                onAddPurchase(name, company, q, p, s, date, batch, expiry)
                                // clear inputs
                                name = ""
                                company = ""
                                qtyStr = ""
                                purchasePriceStr = ""
                                salePriceStr = ""
                                batch = ""
                                expiry = ""
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("save_purchase_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Save Purchase", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { showImportDialog = true },
                            modifier = Modifier
                                .height(46.dp)
                                .testTag("import_csv_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Import CSV")
                        }
                    }
                }
            }
        }

        // Recent Purchases Header & Search
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Recent Purchases (${state.appData.purchases.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = TextPrimary
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search purchases by name, company, or batch...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("purchase_search_input"),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
            }
        }

        if (filteredPurchases.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No purchases recorded yet.", color = TextMuted)
                    }
                }
            }
        } else {
            items(filteredPurchases, key = { it.id }) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("purchase_item_${item.id}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = item.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextPrimary
                                )
                                if (item.company.isNotBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(BlueLight)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(item.company, fontSize = 10.sp, color = BlueSale, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            Text(
                                text = "Date: ${item.date} • Qty: ${item.qty} units",
                                fontSize = 12.sp,
                                color = TextMuted
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Cost: ${formatMoney(item.purchase)}", fontSize = 11.sp, color = OrangePurchase)
                                Text("Sale: ${formatMoney(item.sale)}", fontSize = 11.sp, color = BlueSale)
                                Text("Total: ${formatMoney(item.total)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                            }

                            if (item.batch.isNotBlank() || item.expiry.isNotBlank()) {
                                Text(
                                    text = listOfNotNull(
                                        if (item.batch.isNotBlank()) "Batch: ${item.batch}" else null,
                                        if (item.expiry.isNotBlank()) "Exp: ${item.expiry}" else null
                                    ).joinToString(" • "),
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        IconButton(
                            onClick = { onDeletePurchase(item.id) },
                            modifier = Modifier.testTag("delete_purchase_${item.id}")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RedExpense)
                        }
                    }
                }
            }
        }
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Purchases from CSV") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Paste CSV rows with headers: name, company, qty, purchase, sale, date, batch, expiry",
                        fontSize = 12.sp,
                        color = TextMuted
                    )

                    OutlinedTextField(
                        value = importCsvText,
                        onValueChange = { importCsvText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .testTag("csv_input_text"),
                        placeholder = {
                            Text("name,company,qty,purchase,sale,date\nPanadol 500mg,GSK,100,20,25,2026-09-01\nBrufen 400mg,Abbott,50,45,60,2026-09-02")
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onImportCsv(importCsvText)
                        showImportDialog = false
                        importCsvText = ""
                    },
                    modifier = Modifier.testTag("confirm_import_csv")
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
