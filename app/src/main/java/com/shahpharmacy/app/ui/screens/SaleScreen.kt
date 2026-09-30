package com.shahpharmacy.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shahpharmacy.app.ui.PharmacyUiState
import com.shahpharmacy.app.ui.components.HeroBanner
import com.shahpharmacy.app.ui.components.NoticeBanner
import com.shahpharmacy.app.ui.components.formatMoney
import com.shahpharmacy.app.ui.theme.*
import java.time.LocalDate

@Composable
fun SaleScreen(
    state: PharmacyUiState,
    onNavigateBack: () -> Unit,
    onSaveSale: (date: String, amount: Double) -> Unit,
    onDeleteSale: (id: String) -> Unit
) {
    BackHandler { onNavigateBack() }

    val todayStr = remember { LocalDate.now().toString() }
    val todaySale = state.appData.sales.find { it.date == todayStr }

    var date by remember { mutableStateOf(todayStr) }
    var amountStr by remember { mutableStateOf(todaySale?.amount?.let { if (it > 0) it.toString() else "" } ?: "") }

    LaunchedEffect(date) {
        val existing = state.appData.sales.find { it.date == date }
        if (existing != null) {
            amountStr = existing.amount.toString()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("sale_screen")
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
                Text("Daily Sale", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        }

        item {
            HeroBanner(
                title = "Daily Sales",
                subtitle = "Day-end total revenue recording for Shah Pharmacy."
            )
        }

        item {
            NoticeBanner(
                text = "Sales are intentionally recorded as ONE TOTAL AMOUNT PER DAY. No medicine-wise sale quantities are required."
            )
        }

        // Add / Update Sale Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_sale_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Day-End Sale Entry",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = NavyPrimary
                    )

                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sale_date_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("Total Sale Amount (Rs.) *") },
                        placeholder = { Text("e.g. 35000") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sale_amount_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Button(
                        onClick = {
                            val amount = amountStr.toDoubleOrNull() ?: 0.0
                            onSaveSale(date, amount)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("save_sale_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save / Update Daily Sale", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Daily Sales List
        item {
            Text(
                text = "Daily Sales History (${state.appData.sales.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = TextPrimary
            )
        }

        if (state.appData.sales.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No daily sales recorded yet.", color = TextMuted)
                    }
                }
            }
        } else {
            val sortedSales = state.appData.sales.sortedByDescending { it.date }
            items(sortedSales, key = { it.id }) { sale ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sale_item_${sale.id}"),
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
                        Column {
                            Text(
                                text = sale.date,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = formatMoney(sale.amount),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = BlueSale
                            )
                        }

                        IconButton(
                            onClick = { onDeleteSale(sale.id) },
                            modifier = Modifier.testTag("delete_sale_${sale.id}")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RedExpense)
                        }
                    }
                }
            }
        }
    }
}
