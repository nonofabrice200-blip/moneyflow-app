package com.example

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.local.AppDatabase
import com.example.data.remote.FirebaseSyncManager
import com.example.data.repository.FinanceRepository
import com.example.util.SecurityManager
import com.example.worker.PeriodicSyncWorker
import com.example.worker.RecurringTransactionWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class FinanceFlowApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: FinanceRepository
        private set

    lateinit var securityManager: SecurityManager
        private set

    override fun onCreate() {
        super.onCreate()
        val applicationScope = CoroutineScope(Dispatchers.Default)

        database = AppDatabase.getDatabase(this, applicationScope)
        val syncManager = FirebaseSyncManager(this, database)
        repository = FinanceRepository(this, database, syncManager)
        securityManager = SecurityManager(this)

        applicationScope.launch {
            if (database.categoryDao().getCount() == 0) {
                AppDatabase.populateInitialData(database)
            }
        }

        setupPeriodicWorkers()
    }

    private fun setupPeriodicWorkers() {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            // 15-minute Periodic Sync
            val syncRequest = PeriodicWorkRequestBuilder<PeriodicSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "financeflow_periodic_sync",
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )

            // Daily Recurring Transactions generation
            val recurringRequest = PeriodicWorkRequestBuilder<RecurringTransactionWorker>(1, TimeUnit.DAYS)
                .build()

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "financeflow_recurring_tx",
                ExistingPeriodicWorkPolicy.KEEP,
                recurringRequest
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
