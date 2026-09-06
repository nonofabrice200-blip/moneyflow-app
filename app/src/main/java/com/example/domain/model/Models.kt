package com.example.domain.model

enum class TransactionType {
    EXPENSE,
    INCOME,
    TRANSFER
}

enum class PayCycle(val displayName: String, val days: Int) {
    WEEKLY("Weekly", 7),
    FORTNIGHTLY("Fortnightly", 14),
    MONTHLY("Monthly", 30);

    companion object {
        fun fromString(value: String): PayCycle {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: MONTHLY
        }
    }
}

enum class RecurrenceFrequency(val displayName: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    FORTNIGHTLY("Fortnightly"),
    MONTHLY("Monthly"),
    QUARTERLY("Quarterly"),
    YEARLY("Yearly");

    companion object {
        fun fromString(value: String): RecurrenceFrequency {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: MONTHLY
        }
    }
}

enum class AccountType(val displayName: String) {
    CHECKING("Checking"),
    SAVINGS("Savings"),
    CASH("Cash"),
    CREDIT_CARD("Credit Card"),
    LOAN("Loan"),
    INVESTMENT("Investment");

    companion object {
        fun fromString(value: String): AccountType {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: CHECKING
        }
    }
}

enum class AnalyticsPeriod(val displayName: String) {
    CURRENT_PERIOD("Current Period"),
    LAST_30_DAYS("Last 30 Days"),
    LAST_3_MONTHS("Last 3 Months"),
    LAST_6_MONTHS("Last 6 Months"),
    LAST_YEAR("Last Year"),
    CUSTOM("Custom Range")
}

data class AmortizationScheduleItem(
    val monthNumber: Int,
    val paymentAmount: Double,
    val principalPaid: Double,
    val interestPaid: Double,
    val remainingBalance: Double
)

data class CurrencyInfo(
    val code: String,
    val name: String,
    val symbol: String,
    val flag: String
)
