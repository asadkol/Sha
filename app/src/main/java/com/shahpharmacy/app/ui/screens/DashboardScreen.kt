package com.shahpharmacy.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shahpharmacy.app.data.model.AppScreen
import com.shahpharmacy.app.ui.PharmacyUiState
import com.shahpharmacy.app.ui.components.HeroBanner
import com.shahpharmacy.app.ui.components.StatCard
import com.shahpharmacy.app.ui.components.formatMoney
import com.shahpharmacy.app.ui.theme.*

@Composable
fun DashboardScreen(
    state: PharmacyUiState,
    onNavigate: (AppScreen) -> Unit
) {
    val isAdmin = state.currentUser?.role == "admin"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen")
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HeroBanner(
                title = "Pharmacy Dashboard",
                subtitle = "Welcome back, ${state.currentUser?.fullName?.ifEmpty { state.currentUser.username } ?: "Pharmacist"}."
            )
        }

        // Quick Actions
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Quick Actions",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isAdmin) {
                        Button(
                            onClick = { onNavigate(AppScreen.PURCHASE) },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("quick_purchase_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BlueSecondary)
                        ) {
                            Text("+ Purchase", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = { onNavigate(AppScreen.SALE) },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("quick_sale_btn"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                    ) {
                        Text("+ Daily Sale", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onNavigate(AppScreen.EXPENSE) },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("quick_expense_btn"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD63B52))
                    ) {
                        Text("+ Expense", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { onNavigate(AppScreen.STOCK) },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("quick_stock_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("💊 Stock", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 4 KPI Summary Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        label = "Today's Sale",
                        value = formatMoney(state.todayTotals.sale),
                        valueColor = BlueSale,
                        iconEmoji = "💰",
                        bgColor = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Today's Purchase",
                        value = formatMoney(state.todayTotals.purchase),
                        valueColor = OrangePurchase,
                        iconEmoji = "📦",
                        bgColor = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        label = "Today's Expenses",
                        value = formatMoney(state.todayTotals.expense),
                        valueColor = RedExpense,
                        iconEmoji = "🧾",
                        bgColor = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Cash In Hand",
                        value = formatMoney(state.cashInHand),
                        valueColor = GreenCash,
                        iconEmoji = "💵",
                        bgColor = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Inventory Alerts
        if (state.alerts.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("inventory_alerts_card"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Inventory Alerts (${state.alerts.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextPrimary
                            )
                            TextButton(onClick = { onNavigate(AppScreen.STOCK) }) {
                                Text("View All", fontSize = 12.sp, color = BlueSecondary)
                            }
                        }

                        state.alerts.take(5).forEach { alert ->
                            val bg = if (alert.isCritical) RedLight else OrangeLight
                            val txtColor = if (alert.isCritical) RedExpense else OrangePurchase

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(bg)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(alert.icon, fontSize = 16.sp)
                                Text(
                                    text = alert.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = txtColor
                                )
                                Text(
                                    text = "— ${alert.text}",
                                    fontSize = 12.sp,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Stock Value Summary & Profit Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Stock Valuation",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Purchase Value", fontSize = 11.sp, color = TextMuted)
                                Text(
                                    formatMoney(state.totalStockPurchaseValue),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = OrangePurchase
                                )
                            }
                            Column {
                                Text("Sale Value", fontSize = 11.sp, color = TextMuted)
                                Text(
                                    formatMoney(state.totalStockSaleValue),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = BlueSale
                                )
                            }
                            Column {
                                Text("Potential Margin", fontSize = 11.sp, color = TextMuted)
                                Text(
                                    formatMoney(state.potentialGrossMargin),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = GreenCash
                                )
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Today's Estimated Net Profit",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = formatMoney(state.todayTotals.estimatedProfit),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp,
                            color = if (state.todayTotals.estimatedProfit >= 0) GreenCash else RedExpense
                        )
                        Text(
                            text = "Estimate only: Sales are entered as one daily total rather than itemized receipt lines.",
                            fontSize = 11.sp,
                            color = TextMuted,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}
