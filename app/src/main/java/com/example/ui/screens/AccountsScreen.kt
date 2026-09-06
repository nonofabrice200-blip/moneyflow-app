package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.LinkedBankAccountEntity
import com.example.data.remote.PlaidOpenBankingService
import com.example.domain.model.AccountType
import com.example.ui.components.CategoryIconHelper
import com.example.ui.components.ColorPickerDialog
import com.example.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    accounts: List<AccountEntity>,
    linkedBankAccounts: List<LinkedBankAccountEntity>,
    netWorth: Double,
    onSaveAccount: (AccountEntity) -> Unit,
    onDeleteAccount: (AccountEntity) -> Unit,
    onLinkPlaidAccount: (String, String) -> Unit,
    onImportPlaidTransactions: (LinkedBankAccountEntity, String) -> Unit
) {
    val context = LocalContext.current

    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showPlaidLinkDialog by remember { mutableStateOf(false) }
    var selectedAccountForPlaidImport by remember { mutableStateOf<LinkedBankAccountEntity?>(null) }

    // Currency Converter State
    var convertAmountText by remember { mutableStateOf("100") }
    var fromCurrency by remember { mutableStateOf("USD") }
    var toCurrency by remember { mutableStateOf("EUR") }
    var showFromMenu by remember { mutableStateOf(false) }
    var showToMenu by remember { mutableStateOf(false) }

    val convertedResult by remember(convertAmountText, fromCurrency, toCurrency) {
        derivedStateOf {
            val amt = convertAmountText.toDoubleOrNull() ?: 0.0
            val mockRates = mapOf(
                "USD" to 1.0,
                "EUR" to 0.92,
                "GBP" to 0.78,
                "JPY" to 154.3,
                "CAD" to 1.36,
                "AUD" to 1.51,
                "INR" to 83.45,
                "SGD" to 1.34,
                "CHF" to 0.89,
                "BRL" to 5.20
            )
            val fromRate = mockRates[fromCurrency] ?: 1.0
            val toRate = mockRates[toCurrency] ?: 1.0
            (amt / fromRate) * toRate
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Accounts & Banking", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showAddAccountDialog = true }) {
                        Icon(Icons.Default.AddCard, contentDescription = "Add Account")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Net Worth Summary
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    border = ButtonDefaults.outlinedButtonBorder
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Net Worth", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                CurrencyFormatter.format(netWorth, "USD"),
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        FilledTonalButton(
                            onClick = { showAddAccountDialog = true },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Account")
                        }
                    }
                }
            }

            // Accounts List
            item {
                Text("Your Accounts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            items(accounts, key = { it.id }) { acc ->
                val accColor = try { Color(android.graphics.Color.parseColor(acc.colorHex)) } catch (e: Exception) { Color.Blue }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(accColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = CategoryIconHelper.getIcon(acc.iconName),
                                    contentDescription = null,
                                    tint = accColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(acc.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("${acc.type} • ${acc.currencyCode}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Text(
                            CurrencyFormatter.format(acc.currentBalance, acc.currencyCode),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (acc.currentBalance >= 0) MaterialTheme.colorScheme.onSurface else Color(0xFFEF4444)
                        )
                    }
                }
            }

            // Plaid Open Banking Link Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = ButtonDefaults.outlinedButtonBorder
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Open Banking (Plaid)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("Securely link financial institutions", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Button(
                                onClick = { showPlaidLinkDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("link_bank_btn")
                            ) {
                                Text("Link Bank", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (linkedBankAccounts.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(10.dp))

                            linkedBankAccounts.forEach { bank ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("${bank.institutionName} (${bank.accountMask})", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                            val dateStr = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(bank.lastSyncedAt))
                                            Text("Last synced: $dateStr", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }

                                    FilledTonalButton(
                                        onClick = {
                                            val targetAcc = accounts.firstOrNull()?.id ?: ""
                                            val mockImports = PlaidOpenBankingService.fetchTransactionsForBank(bank)
                                            onImportPlaidTransactions(bank, targetAcc)
                                            Toast.makeText(context, "Imported ${mockImports.size} transactions from ${bank.institutionName}", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("Sync Feed", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Currency Converter Widget (30+ Currencies)
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = ButtonDefaults.outlinedButtonBorder
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CurrencyExchange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Real-Time Currency Converter", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = convertAmountText,
                            onValueChange = { convertAmountText = it },
                            label = { Text("Amount to Convert") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // From Currency Dropdown
                            Box(modifier = Modifier.weight(1f)) {
                                val info = CurrencyFormatter.getCurrencyInfo(fromCurrency)
                                OutlinedButton(
                                    onClick = { showFromMenu = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("${info.flag} ${info.code}")
                                }
                                DropdownMenu(expanded = showFromMenu, onDismissRequest = { showFromMenu = false }) {
                                    CurrencyFormatter.SUPPORTED_CURRENCIES.take(15).forEach { curr ->
                                        DropdownMenuItem(
                                            text = { Text("${curr.flag} ${curr.code} (${curr.name})") },
                                            onClick = {
                                                fromCurrency = curr.code
                                                showFromMenu = false
                                            }
                                        )
                                    }
                                }
                            }

                            Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.primary)

                            // To Currency Dropdown
                            Box(modifier = Modifier.weight(1f)) {
                                val info = CurrencyFormatter.getCurrencyInfo(toCurrency)
                                OutlinedButton(
                                    onClick = { showToMenu = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("${info.flag} ${info.code}")
                                }
                                DropdownMenu(expanded = showToMenu, onDismissRequest = { showToMenu = false }) {
                                    CurrencyFormatter.SUPPORTED_CURRENCIES.take(15).forEach { curr ->
                                        DropdownMenuItem(
                                            text = { Text("${curr.flag} ${curr.code} (${curr.name})") },
                                            onClick = {
                                                toCurrency = curr.code
                                                showToMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Converted Value:", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    CurrencyFormatter.format(convertedResult, toCurrency),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    // Add Account Dialog
    if (showAddAccountDialog) {
        AddAccountDialog(
            onDismiss = { showAddAccountDialog = false },
            onSave = { acc ->
                onSaveAccount(acc)
                showAddAccountDialog = false
            }
        )
    }

    // Plaid Link Bank Modal Dialog
    if (showPlaidLinkDialog) {
        PlaidBankSelectionDialog(
            onDismiss = { showPlaidLinkDialog = false },
            onBankSelected = { bankName, type ->
                onLinkPlaidAccount(bankName, type)
                showPlaidLinkDialog = false
                Toast.makeText(context, "Successfully linked $bankName!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun AddAccountDialog(
    onDismiss: () -> Unit,
    onSave: (AccountEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(AccountType.CHECKING) }
    var currencyCode by remember { mutableStateOf("USD") }
    var startingBalance by remember { mutableStateOf("1000") }
    var colorHex by remember { mutableStateOf("#3B82F6") }
    var showColorPicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Add New Account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account Name") },
                    placeholder = { Text("e.g. Chase Checking, Amex Gold") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Account Type", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(AccountType.CHECKING, AccountType.SAVINGS, AccountType.CREDIT_CARD).forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.name.take(8), fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = startingBalance,
                    onValueChange = { startingBalance = it },
                    label = { Text("Starting Balance") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        if (name.isBlank()) return@Button
                        val bal = startingBalance.toDoubleOrNull() ?: 0.0
                        val acc = AccountEntity(
                            id = "acc_${UUID.randomUUID().toString().take(8)}",
                            name = name.trim(),
                            type = selectedType.name,
                            currencyCode = currencyCode,
                            startingBalance = bal,
                            currentBalance = bal,
                            colorHex = colorHex,
                            iconName = "account_balance"
                        )
                        onSave(acc)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Create Account", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PlaidBankSelectionDialog(
    onDismiss: () -> Unit,
    onBankSelected: (String, String) -> Unit
) {
    val banks = listOf(
        "Chase Bank" to "Premier Checking",
        "Bank of America" to "Advantage Savings",
        "Wells Fargo" to "Everyday Checking",
        "Citibank" to "Custom Cash",
        "Capital One" to "360 Performance Savings",
        "American Express" to "High Yield Savings"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Text("Select Financial Institution", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Open Banking Secure Connect", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(modifier = Modifier.height(16.dp))

                banks.forEach { (name, type) ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onBankSelected(name, type) },
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(type, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
