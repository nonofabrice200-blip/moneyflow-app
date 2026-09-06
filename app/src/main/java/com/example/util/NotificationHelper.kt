package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_BUDGET_ID = "channel_budget_alerts"
        const val CHANNEL_SAVINGS_ID = "channel_savings_milestones"
        const val CHANNEL_RECURRING_ID = "channel_recurring_bills"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val budgetChannel = NotificationChannel(
                CHANNEL_BUDGET_ID,
                "Budget Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications when budget limits reach 80% or 100%"
                enableVibration(true)
            }

            val savingsChannel = NotificationChannel(
                CHANNEL_SAVINGS_ID,
                "Savings Milestones",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Celebrations when you hit savings goal targets"
            }

            val recurringChannel = NotificationChannel(
                CHANNEL_RECURRING_ID,
                "Recurring Bill Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders for scheduled recurring transactions"
            }

            manager.createNotificationChannel(budgetChannel)
            manager.createNotificationChannel(savingsChannel)
            manager.createNotificationChannel(recurringChannel)
        }
    }

    fun sendBudgetAlert(spentPercent: Int, spentAmount: String, limitAmount: String) {
        try {
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                101,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val title = if (spentPercent >= 100) {
                "🚨 Budget Limit Exceeded ($spentPercent%)"
            } else {
                "⚠️ Budget Warning ($spentPercent% Reached)"
            }

            val message = "You've spent $spentAmount of your $limitAmount pay period budget."

            val builder = NotificationCompat.Builder(context, CHANNEL_BUDGET_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            NotificationManagerCompat.from(context).notify(201, builder.build())
        } catch (e: SecurityException) {
            // Notification permission might not be granted yet
        }
    }

    fun sendSavingsMilestone(goalName: String, milestonePercent: Int, currentAmount: String) {
        try {
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                102,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val message = "🎉 Amazing! You've reached $milestonePercent% of your '$goalName' goal ($currentAmount saved)!"

            val builder = NotificationCompat.Builder(context, CHANNEL_SAVINGS_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Goal Milestone Reached!")
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            NotificationManagerCompat.from(context).notify(300 + milestonePercent, builder.build())
        } catch (e: SecurityException) {
            // Ignored
        }
    }
}
