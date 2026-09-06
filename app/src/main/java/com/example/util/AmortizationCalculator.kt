package com.example.util

import com.example.domain.model.AmortizationScheduleItem
import kotlin.math.pow

object AmortizationCalculator {

    fun calculateMonthlyPayment(principal: Double, annualInterestRatePercent: Double, termMonths: Int): Double {
        if (principal <= 0 || termMonths <= 0) return 0.0
        if (annualInterestRatePercent <= 0) return principal / termMonths

        val monthlyRate = (annualInterestRatePercent / 100.0) / 12.0
        val numerator = principal * monthlyRate * (1 + monthlyRate).pow(termMonths.toDouble())
        val denominator = (1 + monthlyRate).pow(termMonths.toDouble()) - 1

        if (denominator == 0.0) return principal / termMonths
        return numerator / denominator
    }

    fun calculateTotalInterest(monthlyPayment: Double, termMonths: Int, principal: Double): Double {
        val totalPaid = monthlyPayment * termMonths
        return (totalPaid - principal).coerceAtLeast(0.0)
    }

    fun generateSchedule(
        principal: Double,
        annualInterestRatePercent: Double,
        termMonths: Int
    ): List<AmortizationScheduleItem> {
        val schedule = mutableListOf<AmortizationScheduleItem>()
        if (principal <= 0 || termMonths <= 0) return schedule

        val monthlyPayment = calculateMonthlyPayment(principal, annualInterestRatePercent, termMonths)
        val monthlyRate = (annualInterestRatePercent / 100.0) / 12.0

        var balance = principal
        for (month in 1..termMonths) {
            val interestPaid = if (monthlyRate > 0) balance * monthlyRate else 0.0
            var principalPaid = monthlyPayment - interestPaid
            if (principalPaid > balance || month == termMonths) {
                principalPaid = balance
            }
            balance -= principalPaid
            if (balance < 0.01) balance = 0.0

            schedule.add(
                AmortizationScheduleItem(
                    monthNumber = month,
                    paymentAmount = principalPaid + interestPaid,
                    principalPaid = principalPaid,
                    interestPaid = interestPaid,
                    remainingBalance = balance
                )
            )
            if (balance <= 0.0) break
        }
        return schedule
    }
}
