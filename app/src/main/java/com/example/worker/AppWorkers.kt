package com.example.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.data.local.entity.TransactionEntity
import com.example.data.remote.ExchangeRateClient
import com.example.data.remote.FirebaseSyncManager
import com.example.domain.model.RecurrenceFrequency
import com.example.util.DateUtils
import java.util.Calendar
import java.util.UUID

class PeriodicSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getDatabase(applicationContext)
            val syncManager = FirebaseSyncManager(applicationContext, db)
            syncManager.syncAll()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

class RecurringTransactionWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getDatabase(applicationContext)
            val now = System.currentTimeMillis()
            val dueRules = db.recurringRuleDao().getDueRules(now)

            for (rule in dueRules) {
                // Generate transaction entry
                val tx = TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    accountId = rule.accountId,
                    categoryId = rule.categoryId,
                    amount = rule.amount,
                    type = rule.type,
                    note = "${rule.note} (Auto Recurring)",
                    date = now,
                    timeString = DateUtils.formatTime(now),
                    isRecurring = true,
                    recurringId = rule.id
                )
                db.transactionDao().insertTransaction(tx)

                // Update account balance
                val acc = db.accountDao().getAccountById(rule.accountId)
                if (acc != null) {
                    val newBal = if (rule.type == "INCOME") acc.currentBalance + rule.amount else acc.currentBalance - rule.amount
                    db.accountDao().updateAccount(acc.copy(currentBalance = newBal))
                }

                // Advance nextOccurrence
                val freq = RecurrenceFrequency.fromString(rule.frequency)
                val cal = Calendar.getInstance().apply { timeInMillis = rule.nextOccurrence }
                when (freq) {
                    RecurrenceFrequency.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
                    RecurrenceFrequency.WEEKLY -> cal.add(Calendar.DAY_OF_YEAR, 7)
                    RecurrenceFrequency.FORTNIGHTLY -> cal.add(Calendar.DAY_OF_YEAR, 14)
                    RecurrenceFrequency.MONTHLY -> cal.add(Calendar.MONTH, 1)
                    RecurrenceFrequency.QUARTERLY -> cal.add(Calendar.MONTH, 3)
                    RecurrenceFrequency.YEARLY -> cal.add(Calendar.YEAR, 1)
                }

                val nextTime = cal.timeInMillis
                val isStillActive = rule.endDate == null || nextTime <= rule.endDate
                val updatedRule = rule.copy(nextOccurrence = nextTime, isActive = isStillActive)
                db.recurringRuleDao().updateRule(updatedRule)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
