package com.shahpharmacy.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shahpharmacy.app.ui.PharmacyUiState
import com.shahpharmacy.app.ui.components.HeroBanner
import com.shahpharmacy.app.ui.components.NoticeBanner
import com.shahpharmacy.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    state: PharmacyUiState,
    onNavigateBack: () -> Unit,
    onChangePassword: (newPass: String) -> Unit,
    onAddStaff: (username: String, pass: String) -> Unit,
    onDeleteStaff: (username: String) -> Unit,
    onClearAllData: () -> Unit,
    onRestoreBackup: (jsonContent: String) -> Unit,
    onGetExportJson: suspend () -> String
) {
    BackHandler { onNavigateBack() }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isAdmin = state.currentUser?.role == "admin"

    var showChangePassDialog by remember { mutableStateOf(false) }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var showAddStaffDialog by remember { mutableStateOf(false) }
    var staffUsername by remember { mutableStateOf("") }
    var staffPassword by remember { mutableStateOf("") }

    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreJsonText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen")
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
                Text("Settings & Management", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        }

        item {
            HeroBanner(
                title = "Settings & Backup",
                subtitle = "Account credentials, staff access, security, and database backup."
            )
        }

        // Account Profile Card
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
                        text = if (isAdmin) "Administrator Account" else "Staff Account",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = NavyPrimary
                    )

                    Text(
                        text = "Signed in as: ${state.currentUser?.fullName.orEmpty()} (${state.currentUser?.username.orEmpty()})",
                        fontSize = 13.sp,
                        color = TextPrimary
                    )

                    OutlinedButton(
                        onClick = { showChangePassDialog = true },
                        modifier = Modifier.testTag("change_password_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Change Password")
                    }
                }
            }
        }

        // Admin Staff Management
        if (isAdmin) {
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Staff User Accounts",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = NavyPrimary
                            )

                            Button(
                                onClick = { showAddStaffDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                                modifier = Modifier.testTag("add_staff_button")
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Staff", fontSize = 12.sp)
                            }
                        }

                        Text(
                            text = "Admin has full access. Staff accounts are limited to Dashboard, Daily Sales, Expenses, and Stock viewing.",
                            fontSize = 12.sp,
                            color = TextMuted
                        )

                        state.allUsers.forEach { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(user.username, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("Role: ${user.role}", fontSize = 12.sp, color = TextMuted)
                                }

                                if (user.username != "admin") {
                                    IconButton(
                                        onClick = { onDeleteStaff(user.username) },
                                        modifier = Modifier.testTag("delete_staff_${user.username}")
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RedExpense)
                                    }
                                } else {
                                    Text("Protected", fontSize = 11.sp, color = TextMuted)
                                }
                            }
                            HorizontalDivider(color = Color(0xFFF0F4FA))
                        }
                    }
                }
            }
        }

        // Database Backup & Restore Card
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
                        text = "Data Backup & Migration",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = NavyPrimary
                    )

                    Text(
                        text = "Pharmacy records are stored locally with AES/GCM encryption and can be exported as JSON for offsite safety.",
                        fontSize = 13.sp,
                        color = TextMuted
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val json = onGetExportJson()
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("ShahPharmacyBackup", json))
                                    Toast.makeText(context, "Backup JSON copied to clipboard (${json.length} chars)", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_backup_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = BlueSecondary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export Backup", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { showRestoreDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("restore_backup_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Restore", fontSize = 12.sp)
                        }
                    }

                    if (isAdmin) {
                        Button(
                            onClick = { showClearConfirmDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("clear_data_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = RedExpense),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clear All Data", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Accounting & Security Info
        item {
            NoticeBanner(
                text = "Security & Accounting Note: Authentication and storage are protected using Android KeyStore encryption. Because daily sales are recorded as a single day-end total, gross margin and net profits are estimated according to pharmacy accounting best practices."
            )
        }
    }

    if (showChangePassDialog) {
        AlertDialog(
            onDismissRequest = { showChangePassDialog = false },
            title = { Text("Change Password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New password (min 4 characters)") },
                        modifier = Modifier.fillMaxWidth().testTag("new_password_input")
                    )
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm new password") },
                        modifier = Modifier.fillMaxWidth().testTag("confirm_password_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPassword.isNotBlank() && newPassword == confirmPassword) {
                            onChangePassword(newPassword)
                            showChangePassDialog = false
                            newPassword = ""
                            confirmPassword = ""
                        } else {
                            Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("confirm_change_password")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePassDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddStaffDialog) {
        AlertDialog(
            onDismissRequest = { showAddStaffDialog = false },
            title = { Text("Add Staff User") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = staffUsername,
                        onValueChange = { staffUsername = it },
                        label = { Text("Staff username / email") },
                        modifier = Modifier.fillMaxWidth().testTag("staff_username_input")
                    )
                    OutlinedTextField(
                        value = staffPassword,
                        onValueChange = { staffPassword = it },
                        label = { Text("Temporary password") },
                        modifier = Modifier.fillMaxWidth().testTag("staff_password_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddStaff(staffUsername, staffPassword)
                        showAddStaffDialog = false
                        staffUsername = ""
                        staffPassword = ""
                    },
                    modifier = Modifier.testTag("confirm_add_staff")
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddStaffDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear All Pharmacy Data?") },
            text = {
                Text("This action will delete all recorded purchases, sales, expenses, and reset opening cash. This cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllData()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedExpense),
                    modifier = Modifier.testTag("confirm_clear_all_data")
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore Database Backup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Paste the JSON backup content below:", fontSize = 12.sp, color = TextMuted)
                    OutlinedTextField(
                        value = restoreJsonText,
                        onValueChange = { restoreJsonText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .testTag("restore_json_input"),
                        placeholder = { Text("{\"purchases\":[...], \"sales\":[...], ...}") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRestoreBackup(restoreJsonText)
                        showRestoreDialog = false
                        restoreJsonText = ""
                    },
                    modifier = Modifier.testTag("confirm_restore_json")
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
