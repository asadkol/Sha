package com.shahpharmacy.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.shahpharmacy.app.data.model.FinancialTotals
import com.shahpharmacy.app.ui.PharmacyUiState
import com.shahpharmacy.app.ui.components.HeroBanner
import com.shahpharmacy.app.ui.components.NoticeBanner
import com.shahpharmacy.app.ui.components.formatMoney
import com.shahpharmacy.app.ui.theme.*

@Composable
fun ReportsScreen(
    state: PharmacyUiState,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Weekly (7 Days)", "Monthly (This Month)", "All-Time")

    val currentTotals: FinancialTotals = when (selectedTab) {
        0 -> state.weeklyTotals
        1 -> state.monthlyTotals
        else -> state.allTimeTotals
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("reports_screen")
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
                Text("Financial Reports", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        }

        item {
            HeroBanner(
                title = "Performance Reports",
                subtitle = "Revenue, expenditures, purchasing, and net profit analytics."
            )
        }

        item {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .testTag("reports_tab_row")
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) NavyPrimary else TextMuted
                            )
                        }
                    )
                }
            }
        }

        // Selected Period Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = tabs[selectedTab],
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = NavyPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = BlueLight),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Total Sales", fontSize = 11.sp, color = BlueSale)
                                Text(
                                    formatMoney(currentTotals.sale),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = BlueSale
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = OrangeLight),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Total Purchases", fontSize = 11.sp, color = OrangePurchase)
                                Text(
                                    formatMoney(currentTotals.purchase),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = OrangePurchase
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
                            colors = CardDefaults.cardColors(containerColor = RedLight),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Total Expenses", fontSize = 11.sp, color = RedExpense)
                                Text(
                                    formatMoney(currentTotals.expense),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = RedExpense
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = GreenLight),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Est. Net Profit", fontSize = 11.sp, color = GreenCash)
                                Text(
                                    formatMoney(currentTotals.estimatedProfit),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = GreenCash
                                )
                            }
                        }
                    }
                }
            }
        }

        // Multi-period comparison table
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Period Overview Comparison",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )

                    val periods = listOf(
                        Triple("Weekly", state.weeklyTotals.sale, state.weeklyTotals.estimatedProfit),
                        Triple("Monthly", state.monthlyTotals.sale, state.monthlyTotals.estimatedProfit),
                        Triple("All-Time", state.allTimeTotals.sale, state.allTimeTotals.estimatedProfit)
                    )

                    periods.forEach { (label, sale, profit) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Sales: ${formatMoney(sale)}", fontSize = 12.sp, color = BlueSale)
                                Text(
                                    "Est. Profit: ${formatMoney(profit)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (profit >= 0) GreenCash else RedExpense
                                )
                            }
                        }
                        HorizontalDivider(color = Color(0xFFF0F4FA))
                    }
                }
            }
        }

        item {
            NoticeBanner(
                text = "Estimated Net Profit is computed based on historical gross margin ratio across current purchases and total recorded operating expenses."
            )
        }
    }
}
