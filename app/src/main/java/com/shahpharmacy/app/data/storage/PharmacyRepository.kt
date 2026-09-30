package com.shahpharmacy.app.data.storage

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.shahpharmacy.app.data.model.AppData
import com.shahpharmacy.app.data.model.AuthData
import com.shahpharmacy.app.data.model.DailySale
import com.shahpharmacy.app.data.model.Expense
import com.shahpharmacy.app.data.model.Purchase
import com.shahpharmacy.app.data.model.UserAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class PharmacyRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
        prettyPrint = true
    }

    private val dbHelper = DatabaseHelper(context)

    private val _appData = MutableStateFlow(AppData())
    val appData: StateFlow<AppData> = _appData.asStateFlow()

    private val _authData = MutableStateFlow(AuthData())
    val authData: StateFlow<AuthData> = _authData.asStateFlow()

    private val _currentUser = MutableStateFlow<UserAccount?>(null)
    val currentUser: StateFlow<UserAccount?> = _currentUser.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        try {
            val authJson = dbHelper.load(KEY_AUTH_ID, DEFAULT_AUTH_JSON)
            val parsedAuth = json.decodeFromString<AuthData>(authJson)
            _authData.value = parsedAuth

            // If there's a stored current user that exists, restore session
            val activeUser = parsedAuth.users.find { it.username == parsedAuth.currentUsername }
            _currentUser.value = activeUser

            val appJson = dbHelper.load(KEY_APP_DATA_ID, DEFAULT_APP_JSON)
            val parsedApp = json.decodeFromString<AppData>(appJson)
            _appData.value = parsedApp
        } catch (e: Exception) {
            e.printStackTrace()
            _appData.value = AppData()
            _authData.value = AuthData()
        }
    }

    suspend fun login(usernameOrEmail: String, pass: String): Result<UserAccount> = withContext(Dispatchers.IO) {
        val trimmed = usernameOrEmail.trim()
        val user = _authData.value.users.find {
            (it.username.equals(trimmed, ignoreCase = true) || 
             (it.username.split("@").firstOrNull()?.equals(trimmed, ignoreCase = true) == true)) &&
            it.password == pass
        }

        if (user != null) {
            val updatedAuth = _authData.value.copy(
                currentUsername = user.username,
                lastLogin = java.time.LocalDate.now().toString()
            )
            saveAuthDataInternal(updatedAuth)
            _currentUser.value = user
            Result.success(user)
        } else {
            Result.failure(IllegalArgumentException("Invalid username/email or password."))
        }
    }

    suspend fun signUp(usernameOrEmail: String, pass: String, fullName: String): Result<UserAccount> = withContext(Dispatchers.IO) {
        val trimmed = usernameOrEmail.trim()
        if (trimmed.isEmpty() || pass.length < 4) {
            return@withContext Result.failure(IllegalArgumentException("Username must not be empty and password at least 4 characters."))
        }

        val existing = _authData.value.users.find { it.username.equals(trimmed, ignoreCase = true) }
        if (existing != null) {
            return@withContext Result.failure(IllegalArgumentException("Account with this username or email already exists."))
        }

        val newUser = UserAccount(
            username = trimmed,
            password = pass,
            role = "staff", // New signups default to staff for security
            fullName = fullName.ifEmpty { trimmed.split("@").first() }
        )

        val updatedUsers = _authData.value.users + newUser
        val updatedAuth = _authData.value.copy(
            users = updatedUsers,
            currentUsername = newUser.username,
            lastLogin = java.time.LocalDate.now().toString()
        )
        saveAuthDataInternal(updatedAuth)
        _currentUser.value = newUser
        Result.success(newUser)
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        val updatedAuth = _authData.value.copy(currentUsername = null)
        saveAuthDataInternal(updatedAuth)
        _currentUser.value = null
    }

    suspend fun changePassword(newPass: String): Result<Unit> = withContext(Dispatchers.IO) {
        val current = _currentUser.value ?: return@withContext Result.failure(IllegalStateException("Not logged in."))
        if (newPass.length < 4) {
            return@withContext Result.failure(IllegalArgumentException("Password must be at least 4 characters."))
        }

        val updatedUsers = _authData.value.users.map {
            if (it.username == current.username) it.copy(password = newPass) else it
        }
        val updatedAuth = _authData.value.copy(users = updatedUsers)
        saveAuthDataInternal(updatedAuth)
        _currentUser.value = current.copy(password = newPass)
        Result.success(Unit)
    }

    suspend fun addStaff(staffUsername: String, tempPass: String): Result<UserAccount> = withContext(Dispatchers.IO) {
        val current = _currentUser.value
        if (current?.role != "admin") {
            return@withContext Result.failure(IllegalStateException("Only admin can create staff accounts."))
        }
        val trimmed = staffUsername.trim()
        if (trimmed.isEmpty() || tempPass.length < 4) {
            return@withContext Result.failure(IllegalArgumentException("Username required and password at least 4 chars."))
        }

        if (_authData.value.users.any { it.username.equals(trimmed, ignoreCase = true) }) {
            return@withContext Result.failure(IllegalArgumentException("Username already exists."))
        }

        val newStaff = UserAccount(username = trimmed, password = tempPass, role = "staff", fullName = trimmed)
        val updated = _authData.value.copy(users = _authData.value.users + newStaff)
        saveAuthDataInternal(updated)
        Result.success(newStaff)
    }

    suspend fun deleteStaff(username: String): Result<Unit> = withContext(Dispatchers.IO) {
        val current = _currentUser.value
        if (current?.role != "admin") {
            return@withContext Result.failure(IllegalStateException("Only admin can delete staff."))
        }
        if (username.equals("admin", ignoreCase = true)) {
            return@withContext Result.failure(IllegalArgumentException("Cannot delete default admin account."))
        }

        val updated = _authData.value.copy(users = _authData.value.users.filterNot { it.username == username })
        saveAuthDataInternal(updated)
        Result.success(Unit)
    }

    suspend fun addPurchase(purchase: Purchase): Result<Unit> = withContext(Dispatchers.IO) {
        val current = _appData.value
        val updatedPurchases = current.purchases + purchase
        saveAppDataInternal(current.copy(purchases = updatedPurchases))
        Result.success(Unit)
    }

    suspend fun deletePurchase(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        val current = _appData.value
        val updatedPurchases = current.purchases.filterNot { it.id == id }
        saveAppDataInternal(current.copy(purchases = updatedPurchases))
        Result.success(Unit)
    }

    suspend fun saveSale(date: String, amount: Double): Result<Unit> = withContext(Dispatchers.IO) {
        val current = _appData.value
        val existingIndex = current.sales.indexOfFirst { it.date == date }
        val updatedSales = if (existingIndex >= 0) {
            current.sales.mapIndexed { index, dailySale ->
                if (index == existingIndex) dailySale.copy(amount = amount) else dailySale
            }
        } else {
            current.sales + DailySale(date = date, amount = amount)
        }
        saveAppDataInternal(current.copy(sales = updatedSales))
        Result.success(Unit)
    }

    suspend fun deleteSale(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        val current = _appData.value
        val updatedSales = current.sales.filterNot { it.id == id }
        saveAppDataInternal(current.copy(sales = updatedSales))
        Result.success(Unit)
    }

    suspend fun addExpense(expense: Expense): Result<Unit> = withContext(Dispatchers.IO) {
        val current = _appData.value
        val updatedExpenses = current.expenses + expense
        saveAppDataInternal(current.copy(expenses = updatedExpenses))
        Result.success(Unit)
    }

    suspend fun deleteExpense(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        val current = _appData.value
        val updatedExpenses = current.expenses.filterNot { it.id == id }
        saveAppDataInternal(current.copy(expenses = updatedExpenses))
        Result.success(Unit)
    }

    suspend fun setOpeningCash(amount: Double): Result<Unit> = withContext(Dispatchers.IO) {
        val current = _appData.value
        saveAppDataInternal(current.copy(openingCash = amount))
        Result.success(Unit)
    }

    suspend fun importPurchasesCsv(csvContent: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val lines = csvContent.lines().filter { it.isNotBlank() }
            if (lines.isEmpty()) return@withContext Result.failure(IllegalArgumentException("CSV is empty."))

            val header = lines.first().split(",").map { it.trim().lowercase() }
            val nameIdx = header.indexOfFirst { it == "name" || it == "medicine" || it == "item" }
            val companyIdx = header.indexOfFirst { it == "company" || it == "manufacturer" }
            val qtyIdx = header.indexOfFirst { it == "qty" || it == "quantity" }
            val purchaseIdx = header.indexOfFirst { it == "purchase" || it.contains("purchase price") }
            val saleIdx = header.indexOfFirst { it == "sale" || it.contains("sale price") }
            val dateIdx = header.indexOfFirst { it == "date" }
            val batchIdx = header.indexOfFirst { it == "batch" }
            val expiryIdx = header.indexOfFirst { it == "expiry" }

            val today = java.time.LocalDate.now().toString()
            val newPurchases = mutableListOf<Purchase>()

            for (i in 1 until lines.size) {
                val row = lines[i].split(",")
                val name = if (nameIdx in row.indices) row[nameIdx].trim() else ""
                val company = if (companyIdx in row.indices) row[companyIdx].trim() else ""
                val qty = (if (qtyIdx in row.indices) row[qtyIdx].trim().toIntOrNull() else null) ?: 0
                val purchase = (if (purchaseIdx in row.indices) row[purchaseIdx].trim().toDoubleOrNull() else null) ?: 0.0
                val sale = (if (saleIdx in row.indices) row[saleIdx].trim().toDoubleOrNull() else null) ?: 0.0
                val date = (if (dateIdx in row.indices && row[dateIdx].isNotBlank()) row[dateIdx].trim() else today)
                val batch = if (batchIdx in row.indices) row[batchIdx].trim() else ""
                val expiry = if (expiryIdx in row.indices) row[expiryIdx].trim() else ""

                if (name.isNotEmpty() && qty > 0) {
                    newPurchases.add(
                        Purchase(
                            name = name,
                            company = company,
                            qty = qty,
                            purchase = purchase,
                            sale = sale,
                            date = date,
                            batch = batch,
                            expiry = expiry,
                            total = qty * purchase
                        )
                    )
                }
            }

            if (newPurchases.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("No valid purchase records found in CSV."))
            }

            val current = _appData.value
            saveAppDataInternal(current.copy(purchases = current.purchases + newPurchases))
            Result.success(newPurchases.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportJson(): String = withContext(Dispatchers.IO) {
        json.encodeToString(_appData.value)
    }

    suspend fun restoreJson(jsonString: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val parsed = json.decodeFromString<AppData>(jsonString)
            saveAppDataInternal(parsed)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearAllData(): Result<Unit> = withContext(Dispatchers.IO) {
        val empty = AppData()
        saveAppDataInternal(empty)
        Result.success(Unit)
    }

    private fun saveAppDataInternal(data: AppData) {
        val encoded = json.encodeToString(data)
        dbHelper.save(KEY_APP_DATA_ID, encoded)
        _appData.value = data
    }

    private fun saveAuthDataInternal(auth: AuthData) {
        val encoded = json.encodeToString(auth)
        dbHelper.save(KEY_AUTH_ID, encoded)
        _authData.value = auth
    }

    companion object {
        const val KEY_APP_DATA_ID = 1
        const val KEY_AUTH_ID = 2
        const val DEFAULT_APP_JSON = "{\"purchases\":[],\"sales\":[],\"expenses\":[],\"openingCash\":0.0}"
        const val DEFAULT_AUTH_JSON = "{\"users\":[{\"username\":\"admin\",\"password\":\"1234\",\"role\":\"admin\",\"fullName\":\"Pharmacy Administrator\"},{\"username\":\"staff\",\"password\":\"1234\",\"role\":\"staff\",\"fullName\":\"Pharmacy Staff\"}],\"currentUsername\":null,\"lastLogin\":null}"
    }

    private class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "shah_pharmacy.db", null, 2) {
        private val keyAlias = "ShahPharmacyDbKey"

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS app_data (id INTEGER PRIMARY KEY, json TEXT NOT NULL)")
            val v1 = ContentValues().apply {
                put("id", KEY_APP_DATA_ID)
                put("json", DEFAULT_APP_JSON)
            }
            db.insertWithOnConflict("app_data", null, v1, SQLiteDatabase.CONFLICT_REPLACE)

            val v2 = ContentValues().apply {
                put("id", KEY_AUTH_ID)
                put("json", DEFAULT_AUTH_JSON)
            }
            db.insertWithOnConflict("app_data", null, v2, SQLiteDatabase.CONFLICT_REPLACE)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("CREATE TABLE IF NOT EXISTS app_data (id INTEGER PRIMARY KEY, json TEXT NOT NULL)")
        }

        private fun getSecretKey(): SecretKey {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (keyStore.containsAlias(keyAlias)) {
                return (keyStore.getEntry(keyAlias, null) as KeyStore.SecretKeyEntry).secretKey
            }
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            keyGenerator.init(
                KeyGenParameterSpec.Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build()
            )
            return keyGenerator.generateKey()
        }

        private fun encrypt(plain: String): String {
            return try {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
                val iv = cipher.iv
                val encrypted = cipher.doFinal(plain.toByteArray(StandardCharsets.UTF_8))
                "ENC1:" + Base64.encodeToString(iv, Base64.NO_WRAP) + ":" + Base64.encodeToString(encrypted, Base64.NO_WRAP)
            } catch (e: Exception) {
                // Fallback to plain if KeyStore is not available on test JVM
                plain
            }
        }

        private fun decrypt(stored: String): String {
            if (!stored.startsWith("ENC1:")) return stored
            return try {
                val parts = stored.split(":", limit = 3)
                val iv = Base64.decode(parts[1], Base64.NO_WRAP)
                val cipherText = Base64.decode(parts[2], Base64.NO_WRAP)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), GCMParameterSpec(128, iv))
                String(cipher.doFinal(cipherText), StandardCharsets.UTF_8)
            } catch (e: Exception) {
                stored
            }
        }

        @Synchronized
        fun load(id: Int, fallback: String): String {
            val db = readableDatabase
            val cursor = db.query(
                "app_data",
                arrayOf("json"),
                "id = ?",
                arrayOf(id.toString()),
                null,
                null,
                null
            )
            return cursor.use {
                if (!it.moveToFirst()) {
                    save(id, fallback)
                    fallback
                } else {
                    val raw = it.getString(0)
                    try {
                        val plain = decrypt(raw)
                        if (!raw.startsWith("ENC1:")) {
                            save(id, plain) // Migrate legacy plaintext to encrypted
                        }
                        plain
                    } catch (e: Exception) {
                        fallback
                    }
                }
            }
        }

        @Synchronized
        fun save(id: Int, json: String) {
            try {
                val encrypted = encrypt(json)
                val db = writableDatabase
                val values = ContentValues().apply {
                    put("id", id)
                    put("json", encrypted)
                }
                db.insertWithOnConflict("app_data", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            } catch (ignored: Exception) {}
        }
    }
}
