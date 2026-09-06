package com.example.data.remote

import com.example.data.local.entity.ExchangeRateEntity
import com.example.data.local.entity.LinkedBankAccountEntity
import com.example.data.local.entity.TransactionEntity
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.UUID

interface ExchangeRateApi {
    @GET("v4/latest/{base}")
    suspend fun getLatestRates(@Path("base") base: String): ExchangeRateResponse
}

data class ExchangeRateResponse(
    val base: String,
    val date: String,
    val rates: Map<String, Double>
)

object ExchangeRateClient {
    private const val BASE_URL = "https://api.exchangerate-api.com/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    val api: ExchangeRateApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(ExchangeRateApi::class.java)
    }
}

data class PlaidTransactionImport(
    val transactionId: String,
    val name: String,
    val amount: Double,
    val date: Long,
    val categoryName: String,
    val categoryId: String,
    val isExpense: Boolean
)

object PlaidOpenBankingService {

    fun createMockLinkedAccount(institutionName: String, accountType: String = "Checking"): LinkedBankAccountEntity {
        val randomDigits = (1000..9999).random()
        return LinkedBankAccountEntity(
            id = "plaid_acc_${UUID.randomUUID().toString().take(8)}",
            institutionName = institutionName,
            institutionLogo = "account_balance",
            accountMask = "•••• $randomDigits",
            plaidAccessToken = "access_token_plaid_${UUID.randomUUID()}",
            accountType = accountType,
            balance = (1200..8500).random().toDouble() + 0.50,
            lastSyncedAt = System.currentTimeMillis()
        )
    }

    fun fetchTransactionsForBank(bankAccount: LinkedBankAccountEntity): List<PlaidTransactionImport> {
        val now = System.currentTimeMillis()
        val oneDay = 86400000L

        return listOf(
            PlaidTransactionImport(
                transactionId = "tx_plaid_1_${UUID.randomUUID()}",
                name = "${bankAccount.institutionName} Direct Deposit",
                amount = 2650.00,
                date = now - (oneDay * 2),
                categoryName = "Salary",
                categoryId = "cat_salary",
                isExpense = false
            ),
            PlaidTransactionImport(
                transactionId = "tx_plaid_2_${UUID.randomUUID()}",
                name = "Whole Foods Market",
                amount = 86.42,
                date = now - (oneDay * 1),
                categoryName = "Food & Dining",
                categoryId = "cat_food",
                isExpense = true
            ),
            PlaidTransactionImport(
                transactionId = "tx_plaid_3_${UUID.randomUUID()}",
                name = "Shell Gas Station",
                amount = 45.10,
                date = now - (oneDay * 3),
                categoryName = "Transport",
                categoryId = "cat_transport",
                isExpense = true
            ),
            PlaidTransactionImport(
                transactionId = "tx_plaid_4_${UUID.randomUUID()}",
                name = "Amazon.com Purchase",
                amount = 32.90,
                date = now - (oneDay * 4),
                categoryName = "Shopping",
                categoryId = "cat_shopping",
                isExpense = true
            )
        )
    }
}
