package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.AccountDao
import com.example.data.local.dao.BudgetSettingsDao
import com.example.data.local.dao.CategoryDao
import com.example.data.local.dao.ExchangeRateDao
import com.example.data.local.dao.LinkedBankAccountDao
import com.example.data.local.dao.LoanDao
import com.example.data.local.dao.RecurringRuleDao
import com.example.data.local.dao.SavingsGoalDao
import com.example.data.local.dao.SyncQueueDao
import com.example.data.local.dao.TransactionDao
import com.example.data.local.dao.UserDao
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.BudgetSettingsEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ExchangeRateEntity
import com.example.data.local.entity.LinkedBankAccountEntity
import com.example.data.local.entity.LoanEntity
import com.example.data.local.entity.RecurringRuleEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.SyncQueueEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

@Database(
    entities = [
        UserEntity::class,
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        RecurringRuleEntity::class,
        BudgetSettingsEntity::class,
        SavingsGoalEntity::class,
        LoanEntity::class,
        ExchangeRateEntity::class,
        LinkedBankAccountEntity::class,
        SyncQueueEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun recurringRuleDao(): RecurringRuleDao
    abstract fun budgetSettingsDao(): BudgetSettingsDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun loanDao(): LoanDao
    abstract fun exchangeRateDao(): ExchangeRateDao
    abstract fun linkedBankAccountDao(): LinkedBankAccountDao
    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "financeflow_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }
        }

        suspend fun populateInitialData(db: AppDatabase) {
            val now = System.currentTimeMillis()

            // 1. Initial Default Accounts
            val checkingAccount = AccountEntity(
                id = "acc_personal_checking",
                name = "Personal Checking",
                type = "CHECKING",
                currencyCode = "USD",
                startingBalance = 3500.0,
                currentBalance = 4250.0,
                colorHex = "#3B82F6",
                iconName = "account_balance"
            )
            val savingsAccount = AccountEntity(
                id = "acc_high_yield_savings",
                name = "Emergency Savings",
                type = "SAVINGS",
                currencyCode = "USD",
                startingBalance = 10000.0,
                currentBalance = 12450.0,
                colorHex = "#10B981",
                iconName = "savings"
            )
            val creditCardAccount = AccountEntity(
                id = "acc_credit_card",
                name = "Sapphire Credit Card",
                type = "CREDIT_CARD",
                currencyCode = "USD",
                startingBalance = 0.0,
                currentBalance = -420.50,
                colorHex = "#8B5CF6",
                iconName = "credit_card"
            )
            db.accountDao().insertAll(listOf(checkingAccount, savingsAccount, creditCardAccount))

            // 2. 12 Default Categories
            val defaultCategories = listOf(
                CategoryEntity("cat_food", "ALL", "Food & Dining", "restaurant", "#EF4444", isIncome = false, isDefault = true),
                CategoryEntity("cat_transport", "ALL", "Transport", "directions_car", "#3B82F6", isIncome = false, isDefault = true),
                CategoryEntity("cat_shopping", "ALL", "Shopping", "shopping_bag", "#EC4899", isIncome = false, isDefault = true),
                CategoryEntity("cat_entertainment", "ALL", "Entertainment", "movie", "#F59E0B", isIncome = false, isDefault = true),
                CategoryEntity("cat_health", "ALL", "Health", "favorite", "#10B981", isIncome = false, isDefault = true),
                CategoryEntity("cat_utilities", "ALL", "Utilities", "flash_on", "#F97316", isIncome = false, isDefault = true),
                CategoryEntity("cat_rent", "ALL", "Rent/Mortgage", "home", "#6366F1", isIncome = false, isDefault = true),
                CategoryEntity("cat_salary", "ALL", "Salary", "payments", "#10B981", isIncome = true, isDefault = true),
                CategoryEntity("cat_investments", "ALL", "Investments", "trending_up", "#06B6D4", isIncome = true, isDefault = true),
                CategoryEntity("cat_savings", "ALL", "Savings", "savings", "#14B8A6", isIncome = false, isDefault = true),
                CategoryEntity("cat_travel", "ALL", "Travel", "flight", "#8B5CF6", isIncome = false, isDefault = true),
                CategoryEntity("cat_subscriptions", "ALL", "Subscriptions", "subscriptions", "#A855F7", isIncome = false, isDefault = true)
            )
            db.categoryDao().insertAll(defaultCategories)

            // 3. Initial Budget Settings
            val budget = BudgetSettingsEntity(
                id = "budget_default",
                accountId = "ALL",
                payCycle = "MONTHLY",
                startDate = now,
                totalBudget = 3200.0
            )
            db.budgetSettingsDao().insertOrUpdate(budget)

            // 4. Sample Recurring Rules
            val recurringHealth = RecurringRuleEntity(
                id = "rec_health_ins",
                accountId = "acc_personal_checking",
                categoryId = "cat_health",
                amount = 120.0,
                type = "EXPENSE",
                note = "Health Insurance Monthly Premium",
                frequency = "MONTHLY",
                startDate = now - (86400000L * 15),
                nextOccurrence = now + (86400000L * 15),
                isActive = true
            )
            val recurringNetflix = RecurringRuleEntity(
                id = "rec_netflix",
                accountId = "acc_credit_card",
                categoryId = "cat_subscriptions",
                amount = 19.99,
                type = "EXPENSE",
                note = "Netflix Premium Subscription",
                frequency = "MONTHLY",
                startDate = now - (86400000L * 10),
                nextOccurrence = now + (86400000L * 20),
                isActive = true
            )
            val recurringGym = RecurringRuleEntity(
                id = "rec_gym",
                accountId = "acc_personal_checking",
                categoryId = "cat_health",
                amount = 55.0,
                type = "EXPENSE",
                note = "Gym Membership",
                frequency = "MONTHLY",
                startDate = now - (86400000L * 5),
                nextOccurrence = now + (86400000L * 25),
                isActive = true
            )
            db.recurringRuleDao().insertRule(recurringHealth)
            db.recurringRuleDao().insertRule(recurringNetflix)
            db.recurringRuleDao().insertRule(recurringGym)

            // 5. Initial Sample Transactions
            val cal = Calendar.getInstance()
            val sampleTransactions = mutableListOf<TransactionEntity>()

            // Today's transaction
            sampleTransactions.add(
                TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    accountId = "acc_personal_checking",
                    categoryId = "cat_food",
                    amount = 28.50,
                    type = "EXPENSE",
                    note = "Lunch with Team",
                    date = cal.timeInMillis,
                    timeString = "13:15",
                    isRecurring = false
                )
            )
            sampleTransactions.add(
                TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    accountId = "acc_credit_card",
                    categoryId = "cat_shopping",
                    amount = 64.99,
                    type = "EXPENSE",
                    note = "Groceries & Supplies",
                    date = cal.timeInMillis,
                    timeString = "10:30",
                    isRecurring = false
                )
            )

            // Yesterday
            cal.add(Calendar.DAY_OF_YEAR, -1)
            sampleTransactions.add(
                TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    accountId = "acc_personal_checking",
                    categoryId = "cat_transport",
                    amount = 35.00,
                    type = "EXPENSE",
                    note = "Gas Station fill up",
                    date = cal.timeInMillis,
                    timeString = "18:40",
                    isRecurring = false
                )
            )
            sampleTransactions.add(
                TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    accountId = "acc_credit_card",
                    categoryId = "cat_subscriptions",
                    amount = 19.99,
                    type = "EXPENSE",
                    note = "Netflix (Monthly)",
                    date = cal.timeInMillis,
                    timeString = "08:00",
                    isRecurring = true,
                    recurringId = "rec_netflix"
                )
            )

            // 3 days ago
            cal.add(Calendar.DAY_OF_YEAR, -2)
            sampleTransactions.add(
                TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    accountId = "acc_personal_checking",
                    categoryId = "cat_salary",
                    amount = 2800.00,
                    type = "INCOME",
                    note = "Bi-weekly Payroll",
                    date = cal.timeInMillis,
                    timeString = "09:00",
                    isRecurring = false
                )
            )
            sampleTransactions.add(
                TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    accountId = "acc_personal_checking",
                    categoryId = "cat_utilities",
                    amount = 115.40,
                    type = "EXPENSE",
                    note = "Electric & Internet Bill",
                    date = cal.timeInMillis,
                    timeString = "14:20",
                    isRecurring = false
                )
            )

            // 6 days ago
            cal.add(Calendar.DAY_OF_YEAR, -3)
            sampleTransactions.add(
                TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    accountId = "acc_personal_checking",
                    categoryId = "cat_rent",
                    amount = 1200.00,
                    type = "EXPENSE",
                    note = "Apartment Rent",
                    date = cal.timeInMillis,
                    timeString = "07:30",
                    isRecurring = false
                )
            )
            db.transactionDao().insertAll(sampleTransactions)

            // 6. Savings Goals
            val japanGoal = SavingsGoalEntity(
                id = "goal_japan_trip",
                name = "Tokyo Vacation Trip",
                targetAmount = 4500.0,
                currentAmount = 2850.0,
                targetDate = now + (86400000L * 90),
                iconName = "flight",
                colorHex = "#8B5CF6"
            )
            val emergencyFundGoal = SavingsGoalEntity(
                id = "goal_emergency_fund",
                name = "6-Month Emergency Cushion",
                targetAmount = 15000.0,
                currentAmount = 12450.0,
                targetDate = now + (86400000L * 180),
                iconName = "security",
                colorHex = "#10B981"
            )
            db.savingsGoalDao().insertGoal(japanGoal)
            db.savingsGoalDao().insertGoal(emergencyFundGoal)

            // 7. Sample Loan
            val carLoan = LoanEntity(
                id = "loan_car",
                accountId = "acc_personal_checking",
                lenderName = "Chase Auto Finance",
                principalAmount = 22000.0,
                interestRate = 4.85,
                termMonths = 48,
                startDate = now - (86400000L * 120),
                paymentFrequency = "MONTHLY",
                remainingBalance = 16420.0,
                monthlyPayment = 505.24,
                totalInterest = 2251.52
            )
            db.loanDao().insertLoan(carLoan)

            // 8. Sample Exchange Rates
            val rates = listOf(
                ExchangeRateEntity("rate_eur", "USD", "EUR", 0.92),
                ExchangeRateEntity("rate_gbp", "USD", "GBP", 0.78),
                ExchangeRateEntity("rate_jpy", "USD", "JPY", 154.30),
                ExchangeRateEntity("rate_cad", "USD", "CAD", 1.36),
                ExchangeRateEntity("rate_aud", "USD", "AUD", 1.51),
                ExchangeRateEntity("rate_chf", "USD", "CHF", 0.89),
                ExchangeRateEntity("rate_cny", "USD", "CNY", 7.23),
                ExchangeRateEntity("rate_inr", "USD", "INR", 83.45),
                ExchangeRateEntity("rate_brl", "USD", "BRL", 5.20),
                ExchangeRateEntity("rate_mxn", "USD", "MXN", 17.10)
            )
            db.exchangeRateDao().insertRates(rates)

            // 9. Linked Bank Account Placeholder
            val chaseLinked = LinkedBankAccountEntity(
                id = "bank_chase",
                institutionName = "Chase Bank",
                institutionLogo = "account_balance",
                accountMask = "•••• 9021",
                plaidAccessToken = "token_chase_mock_encrypted",
                accountType = "Premier Plus Checking",
                balance = 4250.0,
                lastSyncedAt = now - 3600000L
            )
            db.linkedBankAccountDao().insertLinkedAccount(chaseLinked)
        }
    }
}
