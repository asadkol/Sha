package com.shahpharmacy.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shahpharmacy.app.data.model.AppScreen
import com.shahpharmacy.app.ui.PharmacyViewModel
import com.shahpharmacy.app.ui.components.AppBottomNav
import com.shahpharmacy.app.ui.components.AppHeader
import com.shahpharmacy.app.ui.screens.*
import com.shahpharmacy.app.ui.theme.ShahPharmacyTheme

class MainActivity : ComponentActivity() {

    private val viewModel: PharmacyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ShahPharmacyTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(state.statusMessage) {
                    state.statusMessage?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                        viewModel.clearMessages()
                    }
                }

                LaunchedEffect(state.errorMessage) {
                    state.errorMessage?.let { err ->
                        snackbarHostState.showSnackbar(err)
                        viewModel.clearMessages()
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (state.currentUser == null) {
                        LoginScreen(
                            errorMessage = state.errorMessage,
                            statusMessage = state.statusMessage,
                            onLogin = { u, p -> viewModel.login(u, p) },
                            onSignUp = { u, p, f -> viewModel.signUp(u, p, f) }
                        )
                    } else {
                        Scaffold(
                            topBar = {
                                AppHeader(
                                    currentUser = state.currentUser,
                                    onLogout = { viewModel.logout() }
                                )
                            },
                            bottomBar = {
                                AppBottomNav(
                                    currentScreen = state.currentScreen,
                                    userRole = state.currentUser?.role ?: "staff",
                                    onNavigate = { viewModel.navigateTo(it) }
                                )
                            },
                            snackbarHost = { SnackbarHost(snackbarHostState) },
                            contentWindowInsets = WindowInsets.safeDrawing
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                when (state.currentScreen) {
                                    AppScreen.DASHBOARD -> DashboardScreen(
                                        state = state,
                                        onNavigate = { viewModel.navigateTo(it) }
                                    )
                                    AppScreen.PURCHASE -> PurchaseScreen(
                                        state = state,
                                        onNavigateBack = { viewModel.navigateTo(AppScreen.DASHBOARD) },
                                        onAddPurchase = { name, company, qty, purchase, sale, date, batch, expiry ->
                                            viewModel.addPurchase(name, company, qty, purchase, sale, date, batch, expiry)
                                        },
                                        onDeletePurchase = { viewModel.deletePurchase(it) },
                                        onImportCsv = { viewModel.importPurchasesCsv(it) }
                                    )
                                    AppScreen.SALE -> SaleScreen(
                                        state = state,
                                        onNavigateBack = { viewModel.navigateTo(AppScreen.DASHBOARD) },
                                        onSaveSale = { date, amount ->
                                            viewModel.saveSale(date, amount)
                                        },
                                        onDeleteSale = { viewModel.deleteSale(it) }
                                    )
                                    AppScreen.EXPENSE -> ExpenseScreen(
                                        state = state,
                                        onNavigateBack = { viewModel.navigateTo(AppScreen.DASHBOARD) },
                                        onAddExpense = { date, category, amount, note ->
                                            viewModel.addExpense(date, category, amount, note)
                                        },
                                        onDeleteExpense = { viewModel.deleteExpense(it) }
                                    )
                                    AppScreen.STOCK -> StockScreen(
                                        state = state,
                                        onNavigateBack = { viewModel.navigateTo(AppScreen.DASHBOARD) }
                                    )
                                    AppScreen.REPORTS -> ReportsScreen(
                                        state = state,
                                        onNavigateBack = { viewModel.navigateTo(AppScreen.DASHBOARD) }
                                    )
                                    AppScreen.CASH -> CashScreen(
                                        state = state,
                                        onNavigateBack = { viewModel.navigateTo(AppScreen.DASHBOARD) },
                                        onSaveOpeningCash = { viewModel.saveOpeningCash(it) }
                                    )
                                    AppScreen.SETTINGS -> SettingsScreen(
                                        state = state,
                                        onNavigateBack = { viewModel.navigateTo(AppScreen.DASHBOARD) },
                                        onChangePassword = { viewModel.changePassword(it) },
                                        onAddStaff = { u, p -> viewModel.addStaff(u, p) },
                                        onDeleteStaff = { viewModel.deleteStaff(it) },
                                        onClearAllData = { viewModel.clearAllData() },
                                        onRestoreBackup = { viewModel.restoreBackup(it) },
                                        onGetExportJson = { viewModel.getExportJson() }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
