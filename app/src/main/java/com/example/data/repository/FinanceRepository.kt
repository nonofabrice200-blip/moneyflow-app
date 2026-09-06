package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.BudgetSettingsEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ExchangeRateEntity
import com.example.data.local.entity.LinkedBankAccountEntity
import com.example.data.local.entity.LoanEntity
import com.example.data.local.entity.RecurringRuleEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.remote.FirebaseSyncManager
import com.example.data.remote.PlaidOpenBankingService
import com.example.data.remote.PlaidTransactionImport
import com.example.domain.model.AmortizationScheduleItem
import com.example.domain.model.PayCycle
import com.example.util.AmortizationCalculator
import com.example.util.DateUtils
import com.example.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.UUID

class FinanceRepository(
    private val context: Context,
    private val db: AppDatabase,
    val syncManager: FirebaseSyncManager
) {
    private val notificationHelper = NotificationHelper(context)

    // Accounts
    val allAccounts: Flow<List<AccountEntity>> = db.accountDao().getAllAccounts()
    suspend fun getAccountById(id: String): AccountEntity? = db.accountDao().getAccountById(id)

    suspend fun insertAccount(account: AccountEntity) {
        withContext(Dispatchers.IO) {
            db.accountDao().insertAccount(account)
            syncManager.queueSyncItem("accounts", account.id, "INSERT")
        }
    }

    suspend fun updateAccount(account: AccountEntity) {
        withContext(Dispatchers.IO) {
            db.accountDao().updateAccount(account)
            syncManager.queueSyncItem("accounts", account.id, "UPDATE")
        }
    }

    suspend fun deleteAccount(account: AccountEntity) {
        withContext(Dispatchers.IO) {
            db.accountDao().deleteAccount(account)
            syncManager.queueSyncItem("accounts", account.id, "DELETE")
        }
    }

    // Categories
    val allCategories: Flow<List<CategoryEntity>> = db.categoryDao().getAllCategories()
    suspend fun insertCategory(category: CategoryEntity) {
        withContext(Dispatchers.IO) {
            db.categoryDao().insertCategory(category)
            syncManager.queueSyncItem("categories", category.id, "INSERT")
        }
    }

    suspend fun updateCategory(category: CategoryEntity) {
        withContext(Dispatchers.IO) {
            db.categoryDao().updateCategory(category)
            syncManager.queueSyncItem("categories", category.id, "UPDATE")
        }
    }

    suspend fun deleteCategory(category: CategoryEntity) {
        withContext(Dispatchers.IO) {
            db.categoryDao().deleteCategory(category)
            syncManager.queueSyncItem("categories", category.id, "DELETE")
        }
    }

    // Transactions
    val allTransactions: Flow<List<TransactionEntity>> = db.transactionDao().getAllTransactions()

    suspend fun insertTransaction(transaction: TransactionEntity) {
        withContext(Dispatchers.IO) {
            db.transactionDao().insertTransaction(transaction)

            // Adjust Account Balance
            val account = db.accountDao().getAccountById(transaction.accountId)
            if (account != null) {
                val newBalance = when (transaction.type) {
                    "INCOME" -> account.currentBalance + transaction.amount
                    "EXPENSE" -> account.currentBalance - transaction.amount
                    "TRANSFER" -> {
                        // Subtract from source
                        val updatedSource = account.copy(currentBalance = account.currentBalance - transaction.amount)
                        db.accountDao().updateAccount(updatedSource)
                        // Add to destination if targetAccountId exists
                        transaction.targetAccountId?.let { targetId ->
                            val targetAcc = db.accountDao().getAccountById(targetId)
                            if (targetAcc != null) {
                                db.accountDao().updateAccount(targetAcc.copy(currentBalance = targetAcc.currentBalance + transaction.amount))
                            }
                        }
                        account.currentBalance - transaction.amount
                    }
                    else -> account.currentBalance
                }
                if (transaction.type != "TRANSFER") {
                    db.accountDao().updateAccount(account.copy(currentBalance = newBalance))
                }
            }

            syncManager.queueSyncItem("transactions", transaction.id, "INSERT")

            // Check Budget Alert
            checkBudgetExceeded()
        }
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        withContext(Dispatchers.IO) {
            db.transactionDao().deleteTransaction(transaction)

            // Revert Account Balance
            val account = db.accountDao().getAccountById(transaction.accountId)
            if (account != null) {
                val revertedBalance = when (transaction.type) {
                    "INCOME" -> account.currentBalance - transaction.amount
                    "EXPENSE" -> account.currentBalance + transaction.amount
                    "TRANSFER" -> {
                        val updatedSource = account.copy(currentBalance = account.currentBalance + transaction.amount)
                        db.accountDao().updateAccount(updatedSource)
                        transaction.targetAccountId?.let { targetId ->
                            val targetAcc = db.accountDao().getAccountById(targetId)
                            if (targetAcc != null) {
                                db.accountDao().updateAccount(targetAcc.copy(currentBalance = targetAcc.currentBalance - transaction.amount))
                            }
                        }
                        account.currentBalance + transaction.amount
                    }
                    else -> account.currentBalance
                }
                if (transaction.type != "TRANSFER") {
                    db.accountDao().updateAccount(account.copy(currentBalance = revertedBalance))
                }
            }

            syncManager.queueSyncItem("transactions", transaction.id, "DELETE")
        }
    }

    // Budget Settings
    val budgetSettings: Flow<BudgetSettingsEntity?> = db.budgetSettingsDao().getSettingsForAccount("ALL")

    suspend fun updateBudgetSettings(settings: BudgetSettingsEntity) {
        withContext(Dispatchers.IO) {
            db.budgetSettingsDao().insertOrUpdate(settings)
            syncManager.queueSyncItem("budget_settings", settings.id, "UPDATE")
        }
    }

    private suspend fun checkBudgetExceeded() {
        val budget = db.budgetSettingsDao().getSettingsForAccountDirect("ALL") ?: return
        val payCycle = PayCycle.fromString(budget.payCycle)
        val period = DateUtils.calculateCurrentPayPeriod(payCycle, budget.startDate)

        val txList = db.transactionDao().getTransactionsInRangeList(period.startDate, period.endDate)
        val spent = txList.filter { it.type == "EXPENSE" }.sumOf { it.amount }

        if (budget.totalBudget > 0) {
            val percentage = ((spent / budget.totalBudget) * 100).toInt()
            if (percentage in 80..99) {
                notificationHelper.sendBudgetAlert(percentage, "$%.2f".format(spent), "$%.2f".format(budget.totalBudget))
            } else if (percentage >= 100) {
                notificationHelper.sendBudgetAlert(percentage, "$%.2f".format(spent), "$%.2f".format(budget.totalBudget))
            }
        }
    }

    // Recurring Rules
    val activeRecurringRules: Flow<List<RecurringRuleEntity>> = db.recurringRuleDao().getAllActiveRules()

    suspend fun insertRecurringRule(rule: RecurringRuleEntity) {
        withContext(Dispatchers.IO) {
            db.recurringRuleDao().insertRule(rule)
        }
    }

    suspend fun deleteRecurringRule(rule: RecurringRuleEntity) {
        withContext(Dispatchers.IO) {
            db.recurringRuleDao().deleteRule(rule)
        }
    }

    // Savings Goals
    val allSavingsGoals: Flow<List<SavingsGoalEntity>> = db.savingsGoalDao().getAllGoals()

    suspend fun insertSavingsGoal(goal: SavingsGoalEntity) {
        withContext(Dispatchers.IO) {
            db.savingsGoalDao().insertGoal(goal)
            syncManager.queueSyncItem("savings_goals", goal.id, "INSERT")
        }
    }

    suspend fun updateSavingsGoal(goal: SavingsGoalEntity) {
        withContext(Dispatchers.IO) {
            db.savingsGoalDao().updateGoal(goal)
            syncManager.queueSyncItem("savings_goals", goal.id, "UPDATE")
        }
    }

    suspend fun deleteSavingsGoal(goal: SavingsGoalEntity) {
        withContext(Dispatchers.IO) {
            db.savingsGoalDao().deleteGoal(goal)
            syncManager.queueSyncItem("savings_goals", goal.id, "DELETE")
        }
    }

    suspend fun addFundsToGoal(goal: SavingsGoalEntity, amount: Double, sourceAccountId: String) {
        withContext(Dispatchers.IO) {
            val newAmount = goal.currentAmount + amount
            val isCompleted = newAmount >= goal.targetAmount
            val updatedGoal = goal.copy(currentAmount = newAmount, isCompleted = isCompleted)
            db.savingsGoalDao().updateGoal(updatedGoal)

            // Create linked transaction
            val tx = TransactionEntity(
                id = UUID.randomUUID().toString(),
                accountId = sourceAccountId,
                categoryId = "cat_savings",
                amount = amount,
                type = "EXPENSE",
                note = "Contributed to ${goal.name}",
                date = System.currentTimeMillis(),
                timeString = DateUtils.formatTime(System.currentTimeMillis())
            )
            insertTransaction(tx)

            // Check milestone notifications (25, 50, 75, 100)
            if (goal.targetAmount > 0) {
                val oldPct = ((goal.currentAmount / goal.targetAmount) * 100).toInt()
                val newPct = ((newAmount / goal.targetAmount) * 100).toInt()

                val milestones = listOf(25, 50, 75, 100)
                for (m in milestones) {
                    if (oldPct < m && newPct >= m) {
                        notificationHelper.sendSavingsMilestone(goal.name, m, "$%.2f".format(newAmount))
                    }
                }
            }
        }
    }

    // Loans
    val allLoans: Flow<List<LoanEntity>> = db.loanDao().getAllLoans()

    suspend fun insertLoan(loan: LoanEntity) {
        withContext(Dispatchers.IO) {
            db.loanDao().insertLoan(loan)
        }
    }

    suspend fun deleteLoan(loan: LoanEntity) {
        withContext(Dispatchers.IO) {
            db.loanDao().deleteLoan(loan)
        }
    }

    suspend fun logLoanPayment(loan: LoanEntity, paymentAmount: Double, accountId: String) {
        withContext(Dispatchers.IO) {
            val newBalance = (loan.remainingBalance - paymentAmount).coerceAtLeast(0.0)
            val updated = loan.copy(remainingBalance = newBalance)
            db.loanDao().updateLoan(updated)

            val tx = TransactionEntity(
                id = UUID.randomUUID().toString(),
                accountId = accountId,
                categoryId = "cat_utilities",
                amount = paymentAmount,
                type = "EXPENSE",
                note = "Loan Repayment: ${loan.lenderName}",
                date = System.currentTimeMillis(),
                timeString = DateUtils.formatTime(System.currentTimeMillis())
            )
            insertTransaction(tx)
        }
    }

    // Exchange Rates
    val exchangeRates: Flow<List<ExchangeRateEntity>> = db.exchangeRateDao().getRatesForBase("USD")

    suspend fun convertCurrency(amount: Double, fromCurrency: String, toCurrency: String): Double {
        if (fromCurrency.equals(toCurrency, ignoreCase = true)) return amount
        return withContext(Dispatchers.IO) {
            val fromRate = if (fromCurrency == "USD") 1.0 else db.exchangeRateDao().getRate("USD", fromCurrency)?.rate ?: 1.0
            val toRate = if (toCurrency == "USD") 1.0 else db.exchangeRateDao().getRate("USD", toCurrency)?.rate ?: 1.0
            (amount / fromRate) * toRate
        }
    }

    // Linked Bank Accounts (Plaid)
    val linkedBankAccounts: Flow<List<LinkedBankAccountEntity>> = db.linkedBankAccountDao().getAllLinkedAccounts()

    suspend fun linkBankAccount(institutionName: String, accountType: String) {
        withContext(Dispatchers.IO) {
            val linked = PlaidOpenBankingService.createMockLinkedAccount(institutionName, accountType)
            db.linkedBankAccountDao().insertLinkedAccount(linked)
        }
    }

    suspend fun importPlaidTransactions(account: LinkedBankAccountEntity, imports: List<PlaidTransactionImport>, targetAccountId: String) {
        withContext(Dispatchers.IO) {
            for (item in imports) {
                val tx = TransactionEntity(
                    id = item.transactionId,
                    accountId = targetAccountId,
                    categoryId = item.categoryId,
                    amount = item.amount,
                    type = if (item.isExpense) "EXPENSE" else "INCOME",
                    note = item.name,
                    date = item.date,
                    timeString = DateUtils.formatTime(item.date)
                )
                insertTransaction(tx)
            }
        }
    }
}
