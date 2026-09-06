package com.example.util

import com.example.domain.model.PayCycle
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {

    private val fullDateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    private val shortDateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val dayOfWeekFormat = SimpleDateFormat("EEE", Locale.getDefault())

    fun formatDate(timestamp: Long): String {
        return fullDateFormat.format(Date(timestamp))
    }

    fun formatShortDate(timestamp: Long): String {
        return shortDateFormat.format(Date(timestamp))
    }

    fun formatMonthYear(timestamp: Long): String {
        return monthYearFormat.format(Date(timestamp))
    }

    fun formatTime(timestamp: Long): String {
        return timeFormat.format(Date(timestamp))
    }

    fun getRelativeDateHeader(timestamp: Long): String {
        val nowCal = Calendar.getInstance()
        val targetCal = Calendar.getInstance().apply { timeInMillis = timestamp }

        val isSameYear = nowCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR)
        val dayDiff = nowCal.get(Calendar.DAY_OF_YEAR) - targetCal.get(Calendar.DAY_OF_YEAR)

        return when {
            isSameYear && dayDiff == 0 -> "Today"
            isSameYear && dayDiff == 1 -> "Yesterday"
            isSameYear -> SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date(timestamp))
            else -> fullDateFormat.format(Date(timestamp))
        }
    }

    data class PayPeriod(
        val startDate: Long,
        val endDate: Long,
        val daysRemaining: Int,
        val totalDays: Int,
        val displayRange: String
    )

    fun calculateCurrentPayPeriod(payCycle: PayCycle, anchorStartDate: Long): PayPeriod {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()

        return when (payCycle) {
            PayCycle.WEEKLY -> {
                cal.timeInMillis = now
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis

                cal.add(Calendar.DAY_OF_YEAR, 6)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                val end = cal.timeInMillis

                val remainingDays = ((end - now) / 86400000L).toInt().coerceAtLeast(0) + 1
                PayPeriod(
                    startDate = start,
                    endDate = end,
                    daysRemaining = remainingDays,
                    totalDays = 7,
                    displayRange = "${formatShortDate(start)} – ${formatShortDate(end)}"
                )
            }
            PayCycle.FORTNIGHTLY -> {
                val fortnightMs = 14L * 86400000L
                val diff = now - anchorStartDate
                val cycleIndex = if (diff >= 0) diff / fortnightMs else (diff / fortnightMs) - 1
                val start = anchorStartDate + (cycleIndex * fortnightMs)
                val end = start + fortnightMs - 1000L

                val remainingDays = ((end - now) / 86400000L).toInt().coerceAtLeast(0) + 1
                PayPeriod(
                    startDate = start,
                    endDate = end,
                    daysRemaining = remainingDays,
                    totalDays = 14,
                    displayRange = "${formatShortDate(start)} – ${formatShortDate(end)}"
                )
            }
            PayCycle.MONTHLY -> {
                cal.timeInMillis = now
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis

                val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                cal.set(Calendar.DAY_OF_MONTH, maxDay)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                val end = cal.timeInMillis

                val remainingDays = ((end - now) / 86400000L).toInt().coerceAtLeast(0) + 1
                PayPeriod(
                    startDate = start,
                    endDate = end,
                    daysRemaining = remainingDays,
                    totalDays = maxDay,
                    displayRange = "${formatShortDate(start)} – ${formatShortDate(end)}"
                )
            }
        }
    }

    fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    data class CalendarDay(
        val dayOfMonth: Int,
        val timestamp: Long,
        val isCurrentMonth: Boolean,
        val isToday: Boolean
    )

    fun getDaysInMonthMatrix(year: Int, month: Int): List<CalendarDay> {
        val days = mutableListOf<CalendarDay>()
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0 (Sunday) to 6 (Saturday)
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Previous month padding
        val prevCal = (cal.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
        val prevMax = prevCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (i in (firstDayOfWeek - 1) downTo 0) {
            val dayNum = prevMax - i
            prevCal.set(Calendar.DAY_OF_MONTH, dayNum)
            days.add(
                CalendarDay(
                    dayOfMonth = dayNum,
                    timestamp = prevCal.timeInMillis,
                    isCurrentMonth = false,
                    isToday = isSameDay(prevCal.timeInMillis, System.currentTimeMillis())
                )
            )
        }

        // Current month days
        for (dayNum in 1..maxDays) {
            cal.set(Calendar.DAY_OF_MONTH, dayNum)
            days.add(
                CalendarDay(
                    dayOfMonth = dayNum,
                    timestamp = cal.timeInMillis,
                    isCurrentMonth = true,
                    isToday = isSameDay(cal.timeInMillis, System.currentTimeMillis())
                )
            )
        }

        // Next month padding to make full 35 or 42 grid cells
        val remainder = (7 - (days.size % 7)) % 7
        val nextCal = (cal.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
        }
        for (dayNum in 1..remainder) {
            nextCal.set(Calendar.DAY_OF_MONTH, dayNum)
            days.add(
                CalendarDay(
                    dayOfMonth = dayNum,
                    timestamp = nextCal.timeInMillis,
                    isCurrentMonth = false,
                    isToday = isSameDay(nextCal.timeInMillis, System.currentTimeMillis())
                )
            )
        }

        return days
    }
}
