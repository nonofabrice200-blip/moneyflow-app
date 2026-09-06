package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.TransactionEntity
import com.example.ui.MainViewModel
import com.example.ui.categories.AddEditCategoryDialog
import com.example.ui.components.CalculatorBottomSheet
import com.example.ui.screens.*
import com.example.ui.theme.FinanceFlowTheme
import com.example.ui.transactions.AddEditTransactionSheet

class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FinanceFlowTheme {
                MainAppContent(viewModel = viewModel, onAuthenticateBiometrics = ::showBiometricPrompt)
            }
        }
    }

    private fun showBiometricPrompt(onSuccess: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Toast.makeText(this@MainActivity, "Authentication: $errString", Toast.LENGTH_SHORT).show()
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock FinanceFlow")
            .setSubtitle("Authenticate using your biometrics to access your financial portfolio")
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()

        prompt.authenticate(promptInfo)
    }
}

enum class NavigationTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    TIMELINE("Timeline", Icons.Default.ReceiptLong),
    ANALYTICS("Analytics", Icons.Default.Analytics),
    ACCOUNTS("Accounts", Icons.Default.AccountBalance),
    GOALS("Goals & Debt", Icons.Default.Savings),
    SETTINGS("Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(
    viewModel: MainViewModel,
    onAuthenticateBiometrics: (() -> Unit) -> Unit
) {
    var isAuthenticated by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!isAuthenticated) {
            onAuthenticateBiometrics {
                isAuthenticated = true
            }
        }
    }

    if (!isAuthenticated) {
        BiometricLockScreen(
            onUnlockClick = {
                onAuthenticateBiometrics {
                    isAuthenticated = true
                }
            }
        )
        return
    }

    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val budgetState by viewModel.budgetDashboard.collectAsStateWithLifecycle()
    val selectedAccountId by viewModel.selectedAccountId.collectAsStateWithLifecycle()
    val netWorth by viewModel.netWorth.collectAsStateWithLifecycle()
    val savingsGoals by viewModel.savingsGoals.collectAsStateWithLifecycle()
    val loans by viewModel.loans.collectAsStateWithLifecycle()
    val categorySpending by viewModel.categorySpending.collectAsStateWithLifecycle()
    val monthlyBarData by viewModel.monthlyBarData.collectAsStateWithLifecycle()
    val spendingTrend by viewModel.spendingTrend.collectAsStateWithLifecycle()
    val linkedBanks by viewModel.linkedBankAccounts.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val budgetSettings by viewModel.budgetSettings.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(NavigationTab.DASHBOARD) }

    // Modal dialog / sheet states
    var showAddEditTransactionSheet by remember { mutableStateOf(false) }
    var selectedTransactionToEdit by remember { mutableStateOf<TransactionEntity?>(null) }
    var showCalculatorSheet by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                NavigationBar(
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp
                ) {
                    listOf(
                        NavigationTab.DASHBOARD,
                        NavigationTab.TIMELINE,
                        NavigationTab.ANALYTICS,
                        NavigationTab.ACCOUNTS,
                        NavigationTab.GOALS
                    ).forEach { tab ->
                        NavigationBarItem(
                            selected = currentTab == tab,
                            onClick = { currentTab = tab },
                            icon = { Icon(tab.icon, contentDescription = tab.title) },
                            label = {
                                Text(
                                    tab.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (currentTab == tab) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (currentTab != NavigationTab.SETTINGS) {
                FloatingActionButton(
                    onClick = {
                        selectedTransactionToEdit = null
                        showAddEditTransactionSheet = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(18.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                    modifier = Modifier.testTag("fab_add_transaction")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Transaction")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                NavigationTab.DASHBOARD -> {
                    DashboardScreen(
                        accounts = accounts,
                        categories = categories,
                        transactions = transactions,
                        savingsGoals = savingsGoals,
                        budgetState = budgetState,
                        selectedAccountId = selectedAccountId,
                        netWorth = netWorth,
                        syncState = syncState,
                        onSelectAccount = { viewModel.setSelectedAccount(it) },
                        onAddTransactionClick = {
                            selectedTransactionToEdit = null
                            showAddEditTransactionSheet = true
                        },
                        onOpenCalculatorClick = { showCalculatorSheet = true },
                        onTransactionClick = { tx ->
                            selectedTransactionToEdit = tx
                            showAddEditTransactionSheet = true
                        },
                        onTransactionDelete = { tx -> viewModel.deleteTransaction(tx) },
                        onViewAllTransactions = { currentTab = NavigationTab.TIMELINE },
                        onSyncClick = { viewModel.triggerSync() },
                        onOpenSettingsClick = { currentTab = NavigationTab.SETTINGS }
                    )
                }
                NavigationTab.TIMELINE -> {
                    TimelineCalendarScreen(
                        transactions = transactions,
                        categories = categories,
                        accounts = accounts,
                        currencyCode = "USD",
                        onTransactionClick = { tx ->
                            selectedTransactionToEdit = tx
                            showAddEditTransactionSheet = true
                        },
                        onTransactionDelete = { tx -> viewModel.deleteTransaction(tx) },
                        onAddTransactionClick = {
                            selectedTransactionToEdit = null
                            showAddEditTransactionSheet = true
                        }
                    )
                }
                NavigationTab.ANALYTICS -> {
                    AnalyticsScreen(
                        categorySpending = categorySpending,
                        monthlyBarData = monthlyBarData,
                        spendingTrend = spendingTrend,
                        transactions = transactions,
                        categories = categories,
                        currencyCode = "USD"
                    )
                }
                NavigationTab.ACCOUNTS -> {
                    AccountsScreen(
                        accounts = accounts,
                        linkedBankAccounts = linkedBanks,
                        netWorth = netWorth,
                        onSaveAccount = { viewModel.saveAccount(it) },
                        onDeleteAccount = { viewModel.deleteAccount(it) },
                        onLinkPlaidAccount = { name, type -> viewModel.linkPlaidAccount(name, type) },
                        onImportPlaidTransactions = { bank, targetId ->
                            val imports = com.example.data.remote.PlaidOpenBankingService.fetchTransactionsForBank(bank)
                            viewModel.importPlaidTransactions(bank, imports, targetId)
                        }
                    )
                }
                NavigationTab.GOALS -> {
                    GoalsLoansScreen(
                        savingsGoals = savingsGoals,
                        loans = loans,
                        accounts = accounts,
                        currencyCode = "USD",
                        onSaveGoal = { viewModel.saveSavingsGoal(it) },
                        onDeleteGoal = { viewModel.deleteSavingsGoal(it) },
                        onAddFundsToGoal = { goal, amt, accId -> viewModel.addSavingsContribution(goal, amt, accId) },
                        onSaveLoan = { viewModel.saveLoan(it) },
                        onDeleteLoan = { viewModel.deleteLoan(it) },
                        onLogLoanPayment = { loan, amt, accId -> viewModel.logLoanPayment(loan, amt, accId) }
                    )
                }
                NavigationTab.SETTINGS -> {
                    SettingsScreen(
                        budgetSettings = budgetSettings,
                        categories = categories,
                        syncState = syncState,
                        securityManager = viewModel.securityManager,
                        onUpdateBudgetSettings = { cycle, budget -> viewModel.updateBudgetSettings(cycle, budget) },
                        onSaveCategory = { viewModel.saveCategory(it) },
                        onDeleteCategory = { viewModel.deleteCategory(it) },
                        onSyncNow = { viewModel.triggerSync() }
                    )
                }
            }
        }
    }

    // Add / Edit Transaction Sheet
    if (showAddEditTransactionSheet) {
        AddEditTransactionSheet(
            accounts = accounts,
            categories = categories,
            initialTransaction = selectedTransactionToEdit,
            onDismiss = {
                showAddEditTransactionSheet = false
                selectedTransactionToEdit = null
            },
            onSave = { tx, rule ->
                viewModel.saveTransaction(tx, rule)
                showAddEditTransactionSheet = false
                selectedTransactionToEdit = null
            },
            onAddNewCategory = {
                showAddCategoryDialog = true
            }
        )
    }

    // Calculator Bottom Sheet (Quick standalone mode)
    if (showCalculatorSheet) {
        CalculatorBottomSheet(
            initialAmount = 0.0,
            currencyCode = "USD",
            onDismiss = { showCalculatorSheet = false },
            onResultSelected = { amount ->
                showCalculatorSheet = false
                selectedTransactionToEdit = null
                showAddEditTransactionSheet = true
            }
        )
    }

    // Add Custom Category Dialog
    if (showAddCategoryDialog) {
        AddEditCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onSave = { cat ->
                viewModel.saveCategory(cat)
                showAddCategoryDialog = false
            }
        )
    }
}

@Composable
fun BiometricLockScreen(onUnlockClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Locked",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "App Locked",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Unlock to access your financial data",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onUnlockClick,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .height(56.dp)
            ) {
                Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Unlock with Biometrics", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
