package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.FinanceFlowApplication
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.BudgetSettingsEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ExchangeRateEntity
import com.example.data.local.entity.LinkedBankAccountEntity
import com.example.data.local.entity.LoanEntity
import com.example.data.local.entity.RecurringRuleEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.remote.PlaidTransactionImport
import com.example.data.remote.SyncState
import com.example.domain.model.PayCycle
import com.example.ui.components.CategorySpendItem
import com.example.ui.components.MonthlyBarData
import com.example.ui.components.TrendPoint
import com.example.util.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class BudgetDashboardState(
    val totalBudget: Double = 0.0,
    val totalSpent: Double = 0.0,
    val remainingBudget: Double = 0.0,
    val dailyAllowance: Double = 0.0,
    val daysRemaining: Int = 0,
    val payPeriodRange: String = "",
    val percentSpent: Int = 0,
    val isOverBudget: Boolean = false,
    val payCycle: PayCycle = PayCycle.MONTHLY
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FinanceFlowApplication
    val repository = app.repository
    val securityManager = app.securityManager

    // Selected Account filter ("ALL" or specific account ID)
    private val _selectedAccountId = MutableStateFlow(securityManager.selectedAccountId)
    val selectedAccountId: StateFlow<String> = _selectedAccountId.asStateFlow()

    // Base Currency Code
    private val _baseCurrency = MutableStateFlow("USD")
    val baseCurrency: StateFlow<String> = _baseCurrency.asStateFlow()

    // Navigation and UI state
    val accounts = repository.allAccounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val categories = repository.allCategories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allTransactions = repository.allTransactions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val budgetSettings = repository.budgetSettings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val recurringRules = repository.activeRecurringRules.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val savingsGoals = repository.allSavingsGoals.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val loans = repository.allLoans.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val exchangeRates = repository.exchangeRates.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val linkedBankAccounts = repository.linkedBankAccounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val syncState = repository.syncManager.syncState

    // Filtered Transactions based on selected account
    val filteredTransactions = combine(allTransactions, _selectedAccountId) { txList, accId ->
        if (accId == "ALL") txList else txList.filter { it.accountId == accId || it.targetAccountId == accId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Net Worth Calculation (Accounts Balances - Loans)
    val netWorth = combine(accounts, loans) { accList, loanList ->
        val totalAssets = accList.sumOf { it.currentBalance }
        val totalDebt = loanList.sumOf { it.remainingBalance }
        totalAssets - totalDebt
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Current Pay Period & Budget Calculation
    val budgetDashboard = combine(filteredTransactions, budgetSettings) { txList, settings ->
        val budget = settings ?: BudgetSettingsEntity("budget_default", "ALL", "MONTHLY", System.currentTimeMillis(), 3200.0)
        val payCycle = PayCycle.fromString(budget.payCycle)
        val period = DateUtils.calculateCurrentPayPeriod(payCycle, budget.startDate)

        val periodExpenses = txList.filter {
            it.type == "EXPENSE" && it.date >= period.startDate && it.date <= period.endDate
        }.sumOf { it.amount }

        val remaining = (budget.totalBudget - periodExpenses).coerceAtLeast(0.0)
        val allowance = if (period.daysRemaining > 0) remaining / period.daysRemaining else 0.0
        val percent = if (budget.totalBudget > 0) ((periodExpenses / budget.totalBudget) * 100).toInt() else 0

        BudgetDashboardState(
            totalBudget = budget.totalBudget,
            totalSpent = periodExpenses,
            remainingBudget = remaining,
            dailyAllowance = allowance,
            daysRemaining = period.daysRemaining,
            payPeriodRange = period.displayRange,
            percentSpent = percent,
            isOverBudget = periodExpenses > budget.totalBudget,
            payCycle = payCycle
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BudgetDashboardState())

    // Category Spending Breakdown
    val categorySpending = combine(filteredTransactions, categories) { txList, catList ->
        val catMap = catList.associateBy { it.id }
        val expenseTx = txList.filter { it.type == "EXPENSE" }
        val grouped = expenseTx.groupBy { it.categoryId }

        grouped.map { (catId, list) ->
            val cat = catMap[catId]
            val catColor = try {
                androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(cat?.colorHex ?: "#64748B"))
            } catch (e: Exception) {
                androidx.compose.ui.graphics.Color(0xFF64748B)
            }
            CategorySpendItem(
                categoryName = cat?.name ?: "Other",
                amount = list.sumOf { it.amount },
                color = catColor,
                iconName = cat?.iconName ?: "shopping_bag"
            )
        }.sortedByDescending { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Monthly Bar Comparison Data (Last 6 months)
    val monthlyBarData = filteredTransactions.map { txList ->
        val result = mutableListOf<MonthlyBarData>()
        val cal = Calendar.getInstance()

        for (i in 5 downTo 0) {
            val mCal = (cal.clone() as Calendar).apply {
                add(Calendar.MONTH, -i)
            }
            val year = mCal.get(Calendar.YEAR)
            val month = mCal.get(Calendar.MONTH)
            val monthLabel = SimpleDateFormat("MMM", Locale.getDefault()).format(mCal.time)

            val monthTx = txList.filter {
                val tCal = Calendar.getInstance().apply { timeInMillis = it.date }
                tCal.get(Calendar.YEAR) == year && tCal.get(Calendar.MONTH) == month
            }

            val inc = monthTx.filter { it.type == "INCOME" }.sumOf { it.amount }
            val exp = monthTx.filter { it.type == "EXPENSE" }.sumOf { it.amount }

            result.add(MonthlyBarData(monthLabel, inc, exp))
        }
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Spending Trend Line (Last 7 Days)
    val spendingTrend = filteredTransactions.map { txList ->
        val list = mutableListOf<TrendPoint>()
        val cal = Calendar.getInstance()

        for (i in 6 downTo 0) {
            val dCal = (cal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, -i)
            }
            val label = SimpleDateFormat("EEE", Locale.getDefault()).format(dCal.time)
            val dayTx = txList.filter {
                DateUtils.isSameDay(it.date, dCal.timeInMillis) && it.type == "EXPENSE"
            }
            list.add(TrendPoint(label, dayTx.sumOf { it.amount }))
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSelectedAccount(accountId: String) {
        _selectedAccountId.value = accountId
        securityManager.selectedAccountId = accountId
    }

    fun setBaseCurrency(code: String) {
        _baseCurrency.value = code
    }

    fun saveTransaction(transaction: TransactionEntity, recurringRule: RecurringRuleEntity? = null) {
        viewModelScope.launch {
            repository.insertTransaction(transaction)
            if (recurringRule != null) {
                repository.insertRecurringRule(recurringRule)
            }
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun saveAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.insertAccount(account)
        }
    }

    fun deleteAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.deleteAccount(account)
        }
    }

    fun saveCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.insertCategory(category)
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    fun updateBudgetSettings(payCycle: PayCycle, totalBudget: Double) {
        viewModelScope.launch {
            val current = budgetSettings.value ?: BudgetSettingsEntity("budget_default", "ALL", payCycle.name, System.currentTimeMillis(), totalBudget)
            repository.updateBudgetSettings(
                current.copy(
                    payCycle = payCycle.name,
                    totalBudget = totalBudget
                )
            )
        }
    }

    fun saveSavingsGoal(goal: SavingsGoalEntity) {
        viewModelScope.launch {
            repository.insertSavingsGoal(goal)
        }
    }

    fun deleteSavingsGoal(goal: SavingsGoalEntity) {
        viewModelScope.launch {
            repository.deleteSavingsGoal(goal)
        }
    }

    fun addSavingsContribution(goal: SavingsGoalEntity, amount: Double, sourceAccountId: String) {
        viewModelScope.launch {
            repository.addFundsToGoal(goal, amount, sourceAccountId)
        }
    }

    fun saveLoan(loan: LoanEntity) {
        viewModelScope.launch {
            repository.insertLoan(loan)
        }
    }

    fun deleteLoan(loan: LoanEntity) {
        viewModelScope.launch {
            repository.deleteLoan(loan)
        }
    }

    fun logLoanPayment(loan: LoanEntity, paymentAmount: Double, accountId: String) {
        viewModelScope.launch {
            repository.logLoanPayment(loan, paymentAmount, accountId)
        }
    }

    fun linkPlaidAccount(institutionName: String, accountType: String) {
        viewModelScope.launch {
            repository.linkBankAccount(institutionName, accountType)
        }
    }

    fun importPlaidTransactions(bankAccount: LinkedBankAccountEntity, imports: List<PlaidTransactionImport>, targetAccountId: String) {
        viewModelScope.launch {
            repository.importPlaidTransactions(bankAccount, imports, targetAccountId)
        }
    }

    fun triggerSync() {
        viewModelScope.launch {
            repository.syncManager.syncAll()
        }
    }
}
