package com.shahpharmacy.app.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.shahpharmacy.app.ui.theme.*
import com.shahpharmacy.app.util.ExcelPurchaseParser
import com.shahpharmacy.app.util.ParsedPurchaseOrderRow
import com.shahpharmacy.app.util.ThermalPrinterHelper

@Composable
fun ExcelPurchaseImportDialog(
    onDismiss: () -> Unit,
    onConfirmImport: (List<ParsedPurchaseOrderRow>) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Upload File, 1: Paste Text, 2: Preview
    var parsedRows by remember { mutableStateOf<List<ParsedPurchaseOrderRow>>(emptyList()) }
    var pasteText by remember { mutableStateOf("") }
    var parsingError by remember { mutableStateOf<String?>(null) }
    var uploadedFileName by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            uploadedFileName = uri.lastPathSegment ?: "Selected File"
            val result = ExcelPurchaseParser.parseFileUri(context, uri)
            result.onSuccess { rows ->
                parsedRows = rows
                parsingError = null
                selectedTab = 2 // Go to preview
                Toast.makeText(context, "Parsed ${rows.size} items from file!", Toast.LENGTH_SHORT).show()
            }.onFailure { err ->
                parsingError = "Failed to parse file: ${err.message}"
                Toast.makeText(context, parsingError, Toast.LENGTH_LONG).show()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .testTag("excel_purchase_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("📊", fontSize = 22.sp)
                        Column {
                            Text(
                                text = "Upload Purchase Order",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = NavyPrimary
                            )
                            Text(
                                text = "Supports Excel (.xlsx, .xls) and CSV spreadsheets",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Tab Switcher
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFFF1F5F9),
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("📁 File Upload", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("📝 Paste Text", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        enabled = parsedRows.isNotEmpty(),
                        text = { Text("🔍 Preview (${parsedRows.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                if (!parsingError.isNullOrBlank()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = RedLight,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = parsingError!!,
                            color = RedExpense,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                when (selectedTab) {
                    0 -> {
                        // File Upload Tab
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(BlueLight)
                                    .border(2.dp, BlueSecondary.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                    .clickable {
                                        filePickerLauncher.launch("*/*")
                                    }
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("📥", fontSize = 48.sp)
                                    Text(
                                        text = "Select Excel / CSV File",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = NavyPrimary
                                    )
                                    Text(
                                        text = "Tap here to browse your phone storage for .xlsx, .xls, or .csv supplier invoices",
                                        fontSize = 12.sp,
                                        color = TextMuted,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Button(
                                        onClick = { filePickerLauncher.launch("*/*") },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                                    ) {
                                        Text("Choose File on Device")
                                    }
                                }
                            }

                            // Template information card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "Accepted Column Headers:",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "name, company, qty, purchase, sale, batch, expiry, date",
                                        fontSize = 11.sp,
                                        color = BlueSecondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    TextButton(
                                        onClick = {
                                            val tpl = ExcelPurchaseParser.getSampleTemplate()
                                            ThermalPrinterHelper.copyToClipboard(context, tpl, "Purchase Template")
                                        },
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Text("📋 Copy Sample Template CSV", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // Paste Text Tab
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Paste CSV or tab-separated data copied directly from Excel / Google Sheets:",
                                fontSize = 12.sp,
                                color = TextMuted
                            )

                            OutlinedTextField(
                                value = pasteText,
                                onValueChange = { pasteText = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .testTag("paste_excel_text"),
                                placeholder = {
                                    Text(
                                        "name,company,qty,purchase,sale,batch,expiry\n" +
                                                "Panadol 500mg,GSK,100,20.0,25.0,B-881,2027-12-31\n" +
                                                "Brufen 400mg,Abbott,50,45.0,55.0,LOT-12,2027-08-31"
                                    )
                                },
                                shape = RoundedCornerShape(10.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        pasteText = ExcelPurchaseParser.getSampleTemplate()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Load Sample Data", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        if (pasteText.isNotBlank()) {
                                            val lines = pasteText.lines()
                                            val res = ExcelPurchaseParser.parseCsvLines(lines)
                                            res.onSuccess { rows ->
                                                parsedRows = rows
                                                parsingError = null
                                                selectedTab = 2
                                                Toast.makeText(context, "Parsed ${rows.size} rows successfully!", Toast.LENGTH_SHORT).show()
                                            }.onFailure { err ->
                                                parsingError = err.message
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                                ) {
                                    Text("Parse Data", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    2 -> {
                        // Preview Tab
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val totalQty = remember(parsedRows) { parsedRows.sumOf { it.qty } }
                            val totalValue = remember(parsedRows) { parsedRows.sumOf { it.qty * it.purchasePrice } }

                            // Order Summary Banner
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = GreenLight,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Total Items to Import:", fontSize = 11.sp, color = GreenCash)
                                        Text("${parsedRows.size} medicines ($totalQty units)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GreenCash)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Total Order Cost:", fontSize = 11.sp, color = GreenCash)
                                        Text(formatMoney(totalValue), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = GreenCash)
                                    }
                                }
                            }

                            // Items List
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(parsedRows) { index, row ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        border = CardDefaults.outlinedCardBorder()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${index + 1}. ${row.name}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = TextPrimary
                                                )
                                                Text(
                                                    text = "Company: ${row.company.ifBlank { "N/A" }} • Qty: ${row.qty} pcs",
                                                    fontSize = 12.sp,
                                                    color = TextMuted
                                                )
                                                if (row.batch.isNotBlank() || row.expiry.isNotBlank()) {
                                                    Text(
                                                        text = "Batch: ${row.batch.ifBlank { "-" }} • Exp: ${row.expiry.ifBlank { "-" }}",
                                                        fontSize = 11.sp,
                                                        color = TextMuted
                                                    )
                                                }
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("Buy: ${formatMoney(row.purchasePrice)}", fontSize = 11.sp, color = OrangePurchase)
                                                Text("Sale: ${formatMoney(row.salePrice)}", fontSize = 11.sp, color = BlueSale)
                                                Text(
                                                    "Total: ${formatMoney(row.qty * row.purchasePrice)}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = NavyPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Footer Actions
                if (selectedTab == 2 && parsedRows.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { selectedTab = 0 },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Cancel / Re-pick")
                        }

                        Button(
                            onClick = {
                                onConfirmImport(parsedRows)
                                onDismiss()
                            },
                            modifier = Modifier
                                .weight(1.4f)
                                .height(46.dp)
                                .testTag("confirm_excel_import_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                        ) {
                            Text("Confirm & Import (${parsedRows.size})", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
