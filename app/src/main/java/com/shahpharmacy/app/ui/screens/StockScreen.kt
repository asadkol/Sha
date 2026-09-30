package com.shahpharmacy.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shahpharmacy.app.ui.PharmacyUiState
import com.shahpharmacy.app.ui.components.HeroBanner
import com.shahpharmacy.app.ui.components.formatMoney
import com.shahpharmacy.app.ui.theme.*

@Composable
fun StockScreen(
    state: PharmacyUiState,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }

    var searchQuery by remember { mutableStateOf("") }

    val filteredStock = remember(state.stock, searchQuery) {
        if (searchQuery.isBlank()) {
            state.stock
        } else {
            val q = searchQuery.trim().lowercase()
            state.stock.filter {
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
                Text("Current Stock", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        }

        item {
            HeroBanner(
                title = "Current Stock",
                subtitle = "Real-time inventory levels, valuation, and margin calculations."
            )
        }

        // Search card & threshold info
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    Text(
                        text = "Low stock indicator: ≤ 5 units • Expiry alerts: within 60 days",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }
        }

        // 4 KPI Summary Cards for Stock
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Purchase Value", fontSize = 11.sp, color = TextMuted)
                            Text(
                                formatMoney(state.totalStockPurchaseValue),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = OrangePurchase
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Potential Sale Value", fontSize = 11.sp, color = TextMuted)
                            Text(
                                formatMoney(state.totalStockSaleValue),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = BlueSale
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Potential Margin", fontSize = 11.sp, color = TextMuted)
                            Text(
                                formatMoney(state.potentialGrossMargin),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = GreenCash
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Medicine Lines", fontSize = 11.sp, color = TextMuted)
                            Text(
                                "${state.stock.size}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = NavyPrimary
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Medicine Stock Items (${filteredStock.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
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
                        Text("No stock records found.", color = TextMuted)
                    }
                }
            }
        } else {
            items(filteredStock, key = { it.name }) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stock_item_${item.name.replace(" ", "_")}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextPrimary
                                )
                                if (item.company.isNotBlank()) {
                                    Text(
                                        text = item.company,
                                        fontSize = 12.sp,
                                        color = TextMuted
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (item.isLowStock) RedLight else GreenLight)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = if (item.isLowStock) "Low Stock (${item.qty})" else "${item.qty} in stock",
                                        color = if (item.isLowStock) RedExpense else GreenCash,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFFF0F4FA))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
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
                                Text("Total Cost", fontSize = 10.sp, color = TextMuted)
                                Text(formatMoney(item.purchaseValue), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Column {
                                Text("Total Sale", fontSize = 10.sp, color = TextMuted)
                                Text(formatMoney(item.saleValue), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}
