package com.shahpharmacy.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shahpharmacy.app.data.model.AppData
import com.shahpharmacy.app.data.model.AppScreen
import com.shahpharmacy.app.data.model.DailySale
import com.shahpharmacy.app.data.model.Expense
import com.shahpharmacy.app.data.model.FinancialTotals
import com.shahpharmacy.app.data.model.InventoryAlert
import com.shahpharmacy.app.data.model.Purchase
import com.shahpharmacy.app.data.model.StockItem
import com.shahpharmacy.app.data.model.UserAccount
import com.shahpharmacy.app.data.storage.PharmacyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class PharmacyUiState(
    val currentUser: UserAccount? = null,
    val currentScreen: AppScreen = AppScreen.DASHBOARD,
    val appData: AppData = AppData(),
    val stock: List<StockItem> = emptyList(),
    val alerts: List<InventoryAlert> = emptyList(),
    val todayTotals: FinancialTotals = FinancialTotals(0.0, 0.0, 0.0, 0.0),
    val weeklyTotals: FinancialTotals = FinancialTotals(0.0, 0.0, 0.0, 0.0),
    val monthlyTotals: FinancialTotals = FinancialTotals(0.0, 0.0, 0.0, 0.0),
    val allTimeTotals: FinancialTotals = FinancialTotals(0.0, 0.0, 0.0, 0.0),
    val cashInHand: Double = 0.0,
    val totalStockPurchaseValue: Double = 0.0,
    val totalStockSaleValue: Double = 0.0,
    val potentialGrossMargin: Double = 0.0,
    val allUsers: List<UserAccount> = emptyList(),
    val lowStockThreshold: Int = 5,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

class PharmacyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PharmacyRepository(application.applicationContext)

    private val _currentScreen = MutableStateFlow(AppScreen.DASHBOARD)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _lowStockThreshold = MutableStateFlow(5)
    val lowStockThreshold: StateFlow<Int> = _lowStockThreshold.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val uiState: StateFlow<PharmacyUiState> = combine(
        repository.appData,
        repository.currentUser,
        repository.authData,
        _currentScreen,
        _lowStockThreshold
    ) { appData, currentUser, authData, screen, threshold ->
        val stock = calculateStock(appData.purchases, threshold)
        val alerts = calculateAlerts(appData.purchases, threshold)

        val today = LocalDate.now().toString()
        val weekAgo = LocalDate.now().minusDays(6).toString()
        val currentMonthPrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))

        val todayTotals = calculateTotals(appData, from = today, to = today, stock = stock)
        val weeklyTotals = calculateTotals(appData, from = weekAgo, to = today, stock = stock)
        val monthlyTotals = calculateTotals(
            appData,
            from = "$currentMonthPrefix-01",
            to = "$currentMonthPrefix-31",
            stock = stock
        )
        val allTimeTotals = calculateTotals(appData, from = null, to = null, stock = stock)

        val totalPurchasesAmount = appData.purchases.sumOf { it.total }
        val totalSalesAmount = appData.sales.sumOf { it.amount }
        val totalExpensesAmount = appData.expenses.sumOf { it.amount }
        val cashInHand = appData.openingCash + totalSalesAmount - totalPurchasesAmount - totalExpensesAmount

        val stockPurchVal = stock.sumOf { it.purchaseValue }
        val stockSaleVal = stock.sumOf { it.saleValue }
        val margin = stockSaleVal - stockPurchVal

        PharmacyUiState(
            currentUser = currentUser,
            currentScreen = screen,
            appData = appData,
            stock = stock,
            alerts = alerts,
            todayTotals = todayTotals,
            weeklyTotals = weeklyTotals,
            monthlyTotals = monthlyTotals,
            allTimeTotals = allTimeTotals,
            cashInHand = cashInHand,
            totalStockPurchaseValue = stockPurchVal,
            totalStockSaleValue = stockSaleVal,
            potentialGrossMargin = margin,
            allUsers = authData.users,
            lowStockThreshold = threshold,
            statusMessage = _statusMessage.value,
            errorMessage = _errorMessage.value
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PharmacyUiState()
    )

    fun clearMessages() {
        _statusMessage.value = null
        _errorMessage.value = null
    }

    fun navigateTo(screen: AppScreen) {
        val user = uiState.value.currentUser
        if (screen.adminOnly && user?.role != "admin") {
            _errorMessage.value = "Access restricted. Staff cannot access ${screen.title}."
            return
        }
        _currentScreen.value = screen
        clearMessages()
    }

    fun login(username: String, pass: String) {
        clearMessages()
        viewModelScope.launch {
            val result = repository.login(username, pass)
            result.onSuccess {
                _currentScreen.value = AppScreen.DASHBOARD
                _statusMessage.value = "Welcome back, ${it.fullName.ifEmpty { it.username }}!"
            }.onFailure {
                _errorMessage.value = it.message ?: "Login failed."
            }
        }
    }

    fun signUp(username: String, pass: String, fullName: String) {
        clearMessages()
        viewModelScope.launch {
            val result = repository.signUp(username, pass, fullName)
            result.onSuccess {
                _currentScreen.value = AppScreen.DASHBOARD
                _statusMessage.value = "Account created successfully."
            }.onFailure {
                _errorMessage.value = it.message ?: "Sign up failed."
            }
        }
    }

    fun logout() {
        clearMessages()
        viewModelScope.launch {
            repository.logout()
            _currentScreen.value = AppScreen.DASHBOARD
        }
    }

    fun changePassword(newPass: String) {
        clearMessages()
        viewModelScope.launch {
            val result = repository.changePassword(newPass)
            result.onSuccess {
                _statusMessage.value = "Password changed successfully."
            }.onFailure {
                _errorMessage.value = it.message ?: "Could not update password."
            }
        }
    }

    fun addStaff(username: String, pass: String) {
        clearMessages()
        viewModelScope.launch {
            val result = repository.addStaff(username, pass)
            result.onSuccess {
                _statusMessage.value = "Staff account '${it.username}' added."
            }.onFailure {
                _errorMessage.value = it.message ?: "Failed to add staff."
            }
        }
    }

    fun deleteStaff(username: String) {
        clearMessages()
        viewModelScope.launch {
            val result = repository.deleteStaff(username)
            result.onSuccess {
                _statusMessage.value = "Staff account removed."
            }.onFailure {
                _errorMessage.value = it.message ?: "Failed to remove staff."
            }
        }
    }

    fun addPurchase(
        name: String,
        company: String,
        qty: Int,
        purchasePrice: Double,
        salePrice: Double,
        date: String,
        batch: String,
        expiry: String
    ) {
        clearMessages()
        if (name.isBlank() || qty <= 0) {
            _errorMessage.value = "Please enter medicine name and a valid quantity."
            return
        }
        viewModelScope.launch {
            val item = Purchase(
                name = name.trim(),
                company = company.trim(),
                qty = qty,
                purchase = purchasePrice,
                sale = salePrice,
                date = date.ifBlank { LocalDate.now().toString() },
                batch = batch.trim(),
                expiry = expiry.trim(),
                total = qty * purchasePrice
            )
            val result = repository.addPurchase(item)
            result.onSuccess {
                _statusMessage.value = "Purchase of ${item.name} recorded."
            }.onFailure {
                _errorMessage.value = it.message ?: "Failed to save purchase."
            }
        }
    }

    fun deletePurchase(id: String) {
        viewModelScope.launch {
            repository.deletePurchase(id)
            _statusMessage.value = "Purchase deleted."
        }
    }

    fun importPurchasesCsv(csvText: String) {
        clearMessages()
        viewModelScope.launch {
            val result = repository.importPurchasesCsv(csvText)
            result.onSuccess { count ->
                _statusMessage.value = "Successfully imported $count purchase records."
            }.onFailure {
                _errorMessage.value = it.message ?: "Failed to import CSV."
            }
        }
    }

    fun saveSale(date: String, amount: Double) {
        clearMessages()
        if (date.isBlank() || amount < 0) {
            _errorMessage.value = "Please provide a valid date and sale amount."
            return
        }
        viewModelScope.launch {
            val result = repository.saveSale(date, amount)
            result.onSuccess {
                _statusMessage.value = "Daily sale for $date saved."
            }.onFailure {
                _errorMessage.value = it.message ?: "Failed to save sale."
            }
        }
    }

    fun deleteSale(id: String) {
        viewModelScope.launch {
            repository.deleteSale(id)
            _statusMessage.value = "Daily sale entry deleted."
        }
    }

    fun seedSampleMonthlySales() {
        clearMessages()
        viewModelScope.launch {
            val currentYearMonth = java.time.YearMonth.now()
            val monthPrefix = currentYearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))
            val daysInMonth = currentYearMonth.lengthOfMonth()
            val baseAmounts = listOf(24000.0, 31500.0, 28000.0, 34500.0, 41000.0, 37500.0, 45000.0, 32000.0, 29000.0, 36000.0)
            for (day in 1..daysInMonth) {
                val dateStr = String.format(java.util.Locale.US, "%s-%02d", monthPrefix, day)
                val variation = ((day * 950) % 7000).toDouble()
                val amount = baseAmounts[(day - 1) % baseAmounts.size] + variation
                repository.saveSale(dateStr, amount)
            }
            _statusMessage.value = "Generated daily sales records for $monthPrefix."
        }
    }

    fun addExpense(date: String, category: String, amount: Double, note: String) {
        clearMessages()
        if (amount <= 0) {
            _errorMessage.value = "Please enter a valid expense amount."
            return
        }
        viewModelScope.launch {
            val item = Expense(
                date = date.ifBlank { LocalDate.now().toString() },
                category = category,
                amount = amount,
                note = note.trim()
            )
            val result = repository.addExpense(item)
            result.onSuccess {
                _statusMessage.value = "Expense of Rs. $amount saved."
            }.onFailure {
                _errorMessage.value = it.message ?: "Failed to save expense."
            }
        }
    }

    fun deleteExpense(id: String) {
        viewModelScope.launch {
            repository.deleteExpense(id)
            _statusMessage.value = "Expense entry deleted."
        }
    }

    fun saveOpeningCash(amount: Double) {
        clearMessages()
        viewModelScope.launch {
            val result = repository.setOpeningCash(amount)
            result.onSuccess {
                _statusMessage.value = "Opening cash updated."
            }.onFailure {
                _errorMessage.value = it.message ?: "Failed to update opening cash."
            }
        }
    }

    fun clearAllData() {
        clearMessages()
        viewModelScope.launch {
            repository.clearAllData()
            _statusMessage.value = "All pharmacy records cleared."
        }
    }

    fun restoreBackup(jsonContent: String) {
        clearMessages()
        viewModelScope.launch {
            val result = repository.restoreJson(jsonContent)
            result.onSuccess {
                _statusMessage.value = "Backup restored successfully."
            }.onFailure {
                _errorMessage.value = "Invalid backup format: ${it.message}"
            }
        }
    }

    fun setLowStockThreshold(newThreshold: Int) {
        _lowStockThreshold.value = newThreshold.coerceIn(1, 100)
        _statusMessage.value = "Low stock alert threshold set to $newThreshold units."
    }

    fun importParsedPurchaseOrders(rows: List<com.shahpharmacy.app.util.ParsedPurchaseOrderRow>) {
        clearMessages()
        if (rows.isEmpty()) {
            _errorMessage.value = "No purchase items found to import."
            return
        }
        viewModelScope.launch {
            var successCount = 0
            rows.forEach { row ->
                val p = Purchase(
                    name = row.name.trim(),
                    company = row.company.trim(),
                    qty = row.qty.coerceAtLeast(1),
                    purchase = row.purchasePrice,
                    sale = row.salePrice,
                    date = row.date.ifBlank { LocalDate.now().toString() },
                    batch = row.batch.trim(),
                    expiry = row.expiry.trim()
                )
                val res = repository.addPurchase(p)
                if (res.isSuccess) successCount++
            }
            _statusMessage.value = "Successfully imported $successCount purchase order item(s)."
        }
    }

    suspend fun getExportJson(): String {
        return repository.exportJson()
    }

    private fun calculateStock(purchases: List<Purchase>, threshold: Int): List<StockItem> {
        val map = linkedMapOf<String, MutableStock>()
        val today = LocalDate.now()

        purchases.forEach { p ->
            val key = p.name.trim().lowercase()
            if (key.isNotEmpty()) {
                val entry = map.getOrPut(key) {
                    MutableStock(
                        name = p.name.trim(),
                        company = p.company.trim(),
                        qty = 0,
                        purchase = p.purchase,
                        sale = p.sale,
                        latestBatch = p.batch,
                        latestExpiry = p.expiry
                    )
                }
                entry.qty += p.qty
                entry.purchase = p.purchase
                entry.sale = p.sale
                if (entry.company.isEmpty() && p.company.isNotEmpty()) {
                    entry.company = p.company.trim()
                }
                if (p.batch.isNotEmpty()) entry.latestBatch = p.batch
                if (p.expiry.isNotEmpty()) entry.latestExpiry = p.expiry
            }
        }
        return map.values.map {
            var isExpiringSoon = false
            if (it.latestExpiry.isNotBlank()) {
                try {
                    val expDate = LocalDate.parse(it.latestExpiry)
                    val daysUntil = ChronoUnit.DAYS.between(today, expDate)
                    if (daysUntil in 0..60) {
                        isExpiringSoon = true
                    }
                } catch (_: Exception) {}
            }

            StockItem(
                name = it.name,
                company = it.company,
                qty = it.qty,
                purchase = it.purchase,
                sale = it.sale,
                purchaseValue = it.qty * it.purchase,
                saleValue = it.qty * it.sale,
                isLowStock = it.qty <= threshold,
                isOutOfStock = it.qty <= 0,
                isCritical = it.qty in 1..2,
                isExpiringSoon = isExpiringSoon,
                expiryDate = it.latestExpiry,
                batchNumber = it.latestBatch
            )
        }.sortedBy { it.name }
    }

    private data class MutableStock(
        var name: String,
        var company: String,
        var qty: Int,
        var purchase: Double,
        var sale: Double,
        var latestBatch: String = "",
        var latestExpiry: String = ""
    )

    private fun calculateAlerts(purchases: List<Purchase>, threshold: Int): List<InventoryAlert> {
        val list = mutableListOf<InventoryAlert>()
        val today = LocalDate.now()

        // Low stock alerts
        val stock = calculateStock(purchases, threshold)
        stock.filter { it.isLowStock }.forEach { item ->
            list.add(
                InventoryAlert(
                    name = item.name,
                    text = if (item.isOutOfStock) "OUT OF STOCK (0 units)" else "only ${item.qty} unit(s) in stock",
                    type = "stock",
                    icon = if (item.isOutOfStock) "⛔" else "⚠️",
                    isCritical = item.qty <= 2
                )
            )
        }

        // Expiry alerts
        purchases.filter { it.expiry.isNotBlank() }.forEach { p ->
            try {
                val expDate = LocalDate.parse(p.expiry)
                val daysUntil = ChronoUnit.DAYS.between(today, expDate)
                if (daysUntil < 0) {
                    list.add(
                        InventoryAlert(
                            name = p.name,
                            text = "expired on ${p.expiry}",
                            type = "expiry",
                            icon = "⛔",
                            isCritical = true
                        )
                    )
                } else if (daysUntil <= 60) {
                    list.add(
                        InventoryAlert(
                            name = p.name,
                            text = "expires in $daysUntil day(s) (${p.expiry})",
                            type = "expiry",
                            icon = "⏳",
                            isCritical = daysUntil <= 15
                        )
                    )
                }
            } catch (ignored: Exception) {}
        }
        return list
    }

    private fun calculateTotals(
        appData: AppData,
        from: String?,
        to: String?,
        stock: List<StockItem>
    ): FinancialTotals {
        fun matchDate(date: String): Boolean {
            if (from != null && date < from) return false
            if (to != null && date > to) return false
            return true
        }

        val saleSum = appData.sales.filter { matchDate(it.date) }.sumOf { it.amount }
        val purchaseSum = appData.purchases.filter { matchDate(it.date) }.sumOf { it.total }
        val expenseSum = appData.expenses.filter { matchDate(it.date) }.sumOf { it.amount }

        // Estimate profit: sale - (sale / max(marginRatio, 1)) - expense
        val totalCost = stock.sumOf { it.purchase * it.qty }
        val totalRevenueVal = stock.sumOf { (if (it.purchase > 0) it.sale / it.purchase else 1.0) * it.purchase * it.qty }
        val ratio = if (totalCost > 0) totalRevenueVal / totalCost else 1.0
        val effectiveRatio = maxOf(ratio, 1.0)
        val estimatedProfit = saleSum - (saleSum / effectiveRatio) - expenseSum

        return FinancialTotals(
            sale = saleSum,
            purchase = purchaseSum,
            expense = expenseSum,
            estimatedProfit = estimatedProfit
        )
    }
}
