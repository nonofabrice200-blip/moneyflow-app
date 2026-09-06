package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
    val firebaseUid: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String = "local_user",
    val name: String,
    val type: String = "CHECKING",
    val currencyCode: String = "USD",
    val startingBalance: Double = 0.0,
    val currentBalance: Double = 0.0,
    val colorHex: String = "#3B82F6",
    val iconName: String = "account_balance",
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val accountId: String = "ALL",
    val name: String,
    val iconName: String,
    val colorHex: String,
    val isIncome: Boolean = false,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val accountId: String,
    val categoryId: String,
    val amount: Double,
    val type: String, // EXPENSE, INCOME, TRANSFER
    val note: String = "",
    val date: Long, // timestamp ms for date
    val timeString: String = "", // HH:mm
    val isRecurring: Boolean = false,
    val recurringId: String? = null,
    val targetAccountId: String? = null, // for TRANSFER
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncedAt: Long = 0L
)

@Entity(tableName = "recurring_rules")
data class RecurringRuleEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val accountId: String,
    val categoryId: String,
    val amount: Double,
    val type: String, // EXPENSE, INCOME
    val note: String = "",
    val frequency: String, // DAILY, WEEKLY, FORTNIGHTLY, MONTHLY, QUARTERLY, YEARLY
    val startDate: Long,
    val endDate: Long? = null,
    val nextOccurrence: Long,
    val isActive: Boolean = true
)

@Entity(tableName = "budget_settings")
data class BudgetSettingsEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val accountId: String = "ALL",
    val payCycle: String = "MONTHLY", // WEEKLY, FORTNIGHTLY, MONTHLY
    val startDate: Long = System.currentTimeMillis(),
    val totalBudget: Double = 3000.0,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val accountId: String = "ALL",
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val targetDate: Long,
    val iconName: String = "savings",
    val colorHex: String = "#10B981",
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val accountId: String = "ALL",
    val lenderName: String,
    val principalAmount: Double,
    val interestRate: Double, // annual rate percentage e.g. 5.5
    val termMonths: Int,
    val startDate: Long,
    val paymentFrequency: String = "MONTHLY",
    val remainingBalance: Double,
    val monthlyPayment: Double,
    val totalInterest: Double
)

@Entity(tableName = "exchange_rates")
data class ExchangeRateEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val baseCurrency: String = "USD",
    val targetCurrency: String,
    val rate: Double,
    val fetchedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "linked_bank_accounts")
data class LinkedBankAccountEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String = "local_user",
    val institutionName: String,
    val institutionLogo: String = "",
    val accountMask: String = "•••• 4242",
    val plaidAccessToken: String = "",
    val accountType: String = "Checking",
    val balance: Double = 0.0,
    val lastSyncedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val entityType: String,
    val entityId: String,
    val action: String, // INSERT, UPDATE, DELETE
    val payloadJson: String,
    val timestamp: Long = System.currentTimeMillis()
)
