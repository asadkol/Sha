package com.shahpharmacy.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.shahpharmacy.app.ui.components.StatCard
import com.shahpharmacy.app.ui.components.formatMoney
import com.shahpharmacy.app.ui.theme.*

@Composable
fun CashScreen(
    state: PharmacyUiState,
    onNavigateBack: () -> Unit,
    onSaveOpeningCash: (amount: Double) -> Unit
) {
    BackHandler { onNavigateBack() }

    var openingCashStr by remember(state.appData.openingCash) {
        mutableStateOf(if (state.appData.openingCash > 0) state.appData.openingCash.toString() else "")
    }

    val totalSales = state.appData.sales.sumOf { it.amount }
    val totalPurchases = state.appData.purchases.sumOf { it.total }
    val totalExpenses = state.appData.expenses.sumOf { it.amount }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("cash_screen")
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
                Text("Cash In Hand", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        }

        item {
            HeroBanner(
                title = "Cash In Hand Management",
                subtitle = "Reconcile register opening float, sales, purchases, and expenses."
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        label = "Opening Cash",
                        value = formatMoney(state.appData.openingCash),
                        valueColor = TextPrimary,
                        iconEmoji = "🪙",
                        bgColor = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "All Sales",
                        value = formatMoney(totalSales),
                        valueColor = BlueSale,
                        iconEmoji = "💰",
                        bgColor = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        label = "Purchases",
                        value = formatMoney(totalPurchases),
                        valueColor = OrangePurchase,
                        iconEmoji = "📦",
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

        // Cash Formula Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Cash Flow Equation",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = NavyPrimary
                    )

                    Text(
                        text = "Opening Cash + All Sales − Purchases − Expenses = Cash In Hand",
                        fontSize = 13.sp,
                        color = TextMuted
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = GreenLight,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Current Register Balance:", fontSize = 12.sp, color = GreenCash)
                            Text(
                                formatMoney(state.cashInHand),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp,
                                color = GreenCash
                            )
                        }
                    }

                    OutlinedTextField(
                        value = openingCashStr,
                        onValueChange = { openingCashStr = it },
                        label = { Text("Opening Cash Float (Rs.)") },
                        placeholder = { Text("Enter register starting cash") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("opening_cash_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Button(
                        onClick = {
                            val amount = openingCashStr.toDoubleOrNull() ?: 0.0
                            onSaveOpeningCash(amount)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("save_opening_cash_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save Opening Cash", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
