package com.shahpharmacy.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Purchase(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val company: String = "",
    val qty: Int,
    val purchase: Double,
    val sale: Double,
    val date: String,
    val batch: String = "",
    val expiry: String = "",
    val total: Double = qty * purchase
)

@Serializable
data class DailySale(
    val id: String = java.util.UUID.randomUUID().toString(),
    val date: String,
    val amount: Double
)

@Serializable
data class Expense(
    val id: String = java.util.UUID.randomUUID().toString(),
    val date: String,
    val category: String,
    val amount: Double,
    val note: String = ""
)

@Serializable
data class AppData(
    val purchases: List<Purchase> = emptyList(),
    val sales: List<DailySale> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val openingCash: Double = 0.0
)

@Serializable
data class UserAccount(
    val username: String,
    val password: String,
    val role: String = "staff", // "admin" or "staff"
    val fullName: String = ""
)

@Serializable
data class AuthData(
    val users: List<UserAccount> = listOf(
        UserAccount(username = "admin", password = "1234", role = "admin", fullName = "Pharmacy Administrator"),
        UserAccount(username = "staff", password = "1234", role = "staff", fullName = "Pharmacy Staff")
    ),
    val currentUsername: String? = null,
    val lastLogin: String? = null
)

data class StockItem(
    val name: String,
    val company: String,
    val qty: Int,
    val purchase: Double,
    val sale: Double,
    val purchaseValue: Double,
    val saleValue: Double,
    val isLowStock: Boolean
)

data class InventoryAlert(
    val name: String,
    val text: String,
    val type: String, // "stock" or "expiry"
    val icon: String,
    val isCritical: Boolean
)

data class FinancialTotals(
    val sale: Double,
    val purchase: Double,
    val expense: Double,
    val estimatedProfit: Double
)

enum class AppScreen(val title: String, val icon: String, val adminOnly: Boolean = false) {
    DASHBOARD("Dashboard", "🏠"),
    PURCHASE("Purchase", "📦", adminOnly = true),
    SALE("Daily Sale", "💰"),
    EXPENSE("Expenses", "🧾"),
    STOCK("Stock", "💊"),
    REPORTS("Reports", "📊", adminOnly = true),
    CASH("Cash In Hand", "💵", adminOnly = true),
    SETTINGS("Settings", "⚙️")
}
