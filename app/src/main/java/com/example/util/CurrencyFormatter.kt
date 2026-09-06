package com.example.util

import com.example.domain.model.CurrencyInfo
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

object CurrencyFormatter {

    val SUPPORTED_CURRENCIES = listOf(
        CurrencyInfo("USD", "US Dollar", "$", "🇺🇸"),
        CurrencyInfo("EUR", "Euro", "€", "🇪🇺"),
        CurrencyInfo("GBP", "British Pound", "£", "🇬🇧"),
        CurrencyInfo("JPY", "Japanese Yen", "¥", "🇯🇵"),
        CurrencyInfo("CAD", "Canadian Dollar", "CA$", "🇨🇦"),
        CurrencyInfo("AUD", "Australian Dollar", "A$", "🇦🇺"),
        CurrencyInfo("CHF", "Swiss Franc", "CHF", "🇨🇭"),
        CurrencyInfo("CNY", "Chinese Yuan", "¥", "🇨🇳"),
        CurrencyInfo("INR", "Indian Rupee", "₹", "🇮🇳"),
        CurrencyInfo("BRL", "Brazilian Real", "R$", "🇧🇷"),
        CurrencyInfo("MXN", "Mexican Peso", "Mex$", "🇲🇽"),
        CurrencyInfo("SGD", "Singapore Dollar", "S$", "🇸🇬"),
        CurrencyInfo("HKD", "Hong Kong Dollar", "HK$", "🇭🇰"),
        CurrencyInfo("NZD", "New Zealand Dollar", "NZ$", "🇳🇿"),
        CurrencyInfo("KRW", "South Korean Won", "₩", "🇰🇷"),
        CurrencyInfo("SEK", "Swedish Krona", "kr", "🇸🇪"),
        CurrencyInfo("NOK", "Norwegian Krone", "kr", "🇳🇴"),
        CurrencyInfo("DKK", "Danish Krone", "kr", "🇩🇰"),
        CurrencyInfo("ZAR", "South African Rand", "R", "🇿🇦"),
        CurrencyInfo("AED", "UAE Dirham", "AED", "🇦🇪"),
        CurrencyInfo("SAR", "Saudi Riyal", "SAR", "🇸🇦"),
        CurrencyInfo("PLN", "Polish Zloty", "zł", "🇵🇱"),
        CurrencyInfo("TRY", "Turkish Lira", "₺", "🇹🇷"),
        CurrencyInfo("THB", "Thai Baht", "฿", "🇹🇭"),
        CurrencyInfo("IDR", "Indonesian Rupiah", "Rp", "🇮🇩"),
        CurrencyInfo("MYR", "Malaysian Ringgit", "RM", "🇲🇾"),
        CurrencyInfo("PHP", "Philippine Peso", "₱", "🇵🇭"),
        CurrencyInfo("CZK", "Czech Koruna", "Kč", "🇨🇿"),
        CurrencyInfo("HUF", "Hungarian Forint", "Ft", "🇭🇺"),
        CurrencyInfo("ILS", "Israeli Shekel", "₪", "🇮🇱"),
        CurrencyInfo("CLP", "Chilean Peso", "CLP$", "🇨🇱"),
        CurrencyInfo("COP", "Colombian Peso", "COL$", "🇨🇴")
    )

    private val numberFormat = DecimalFormat("#,##0.00")
    private val compactFormat = DecimalFormat("#,##0")

    fun getSymbol(currencyCode: String): String {
        return SUPPORTED_CURRENCIES.find { it.code.equals(currencyCode, ignoreCase = true) }?.symbol
            ?: try {
                Currency.getInstance(currencyCode).symbol
            } catch (e: Exception) {
                "$"
            }
    }

    fun getCurrencyInfo(currencyCode: String): CurrencyInfo {
        return SUPPORTED_CURRENCIES.find { it.code.equals(currencyCode, ignoreCase = true) }
            ?: CurrencyInfo(currencyCode, currencyCode, "$", "🌐")
    }

    fun format(amount: Double, currencyCode: String = "USD"): String {
        val symbol = getSymbol(currencyCode)
        val isNegative = amount < 0
        val absAmount = Math.abs(amount)
        val formattedNumber = numberFormat.format(absAmount)
        return if (isNegative) "-$symbol$formattedNumber" else "$symbol$formattedNumber"
    }

    fun formatCompact(amount: Double, currencyCode: String = "USD"): String {
        val symbol = getSymbol(currencyCode)
        val isNegative = amount < 0
        val absAmount = Math.abs(amount)
        val formattedNumber = compactFormat.format(absAmount)
        return if (isNegative) "-$symbol$formattedNumber" else "$symbol$formattedNumber"
    }
}
