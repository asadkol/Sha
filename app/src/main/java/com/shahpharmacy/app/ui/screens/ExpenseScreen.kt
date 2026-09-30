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
import com.shahpharmacy.app.ui.components.formatMoney
import com.shahpharmacy.app.ui.theme.*
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(
    state: PharmacyUiState,
    onNavigateBack: () -> Unit,
    onAddExpense: (date: String, category: String, amount: Double, note: String) -> Unit,
    onDeleteExpense: (id: String) -> Unit
) {
    BackHandler { onNavigateBack() }

    val categories = listOf("Electricity", "Rent", "Salary", "Transport", "Other")
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var selectedCategory by remember { mutableStateOf(categories.first()) }
    var expandedCategory by remember { mutableStateOf(false) }
    var amountStr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("expense_screen")
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
                Text("Expenses", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        }

        item {
            HeroBanner(
                title = "Expense Tracking",
                subtitle = "Manage pharmacy operational costs, utilities, and salaries."
            )
        }

        // Add Expense Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_expense_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Add Expense",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = RedExpense
                    )

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
                                .testTag("expense_date_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        ExposedDropdownMenuBox(
                            expanded = expandedCategory,
                            onExpandedChange = { expandedCategory = !expandedCategory },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = selectedCategory,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Category") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .testTag("expense_category_dropdown"),
                                shape = RoundedCornerShape(10.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = expandedCategory,
                                onDismissRequest = { expandedCategory = false }
                            ) {
                                categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat) },
                                        onClick = {
                                            selectedCategory = cat
                                            expandedCategory = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("Amount (Rs.) *") },
                        placeholder = { Text("e.g. 4500") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("expense_amount_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Note / Description") },
                        placeholder = { Text("e.g. Generator fuel bill") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("expense_note_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Button(
                        onClick = {
                            val amount = amountStr.toDoubleOrNull() ?: 0.0
                            onAddExpense(date, selectedCategory, amount, note)
                            amountStr = ""
                            note = ""
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("save_expense_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = RedExpense),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save Expense", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Expense History
        item {
            Text(
                text = "Expense History (${state.appData.expenses.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = TextPrimary
            )
        }

        if (state.appData.expenses.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No expenses recorded yet.", color = TextMuted)
                    }
                }
            }
        } else {
            val sortedExpenses = state.appData.expenses.sortedByDescending { it.date }
            items(sortedExpenses, key = { it.id }) { expense ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_item_${expense.id}"),
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
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = expense.category,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "• ${expense.date}",
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }
                            Text(
                                text = formatMoney(expense.amount),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = RedExpense
                            )
                            if (expense.note.isNotBlank()) {
                                Text(
                                    text = expense.note,
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        IconButton(
                            onClick = { onDeleteExpense(expense.id) },
                            modifier = Modifier.testTag("delete_expense_${expense.id}")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RedExpense)
                        }
                    }
                }
            }
        }
    }
}
