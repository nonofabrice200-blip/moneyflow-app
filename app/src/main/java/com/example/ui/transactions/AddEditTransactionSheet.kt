package com.example.ui.transactions

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.RecurringRuleEntity
import com.example.data.local.entity.TransactionEntity
import com.example.domain.model.RecurrenceFrequency
import com.example.domain.model.TransactionType
import com.example.ui.components.CalculatorBottomSheet
import com.example.ui.components.CategoryIconHelper
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils
import java.util.Calendar
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionSheet(
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    initialTransaction: TransactionEntity? = null,
    onDismiss: () -> Unit,
    onSave: (TransactionEntity, RecurringRuleEntity?) -> Unit,
    onAddNewCategory: () -> Unit
) {
    val context = LocalContext.current
    val modalBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedType by remember {
        mutableStateOf(
            if (initialTransaction != null) {
                try { TransactionType.valueOf(initialTransaction.type) } catch (e: Exception) { TransactionType.EXPENSE }
            } else TransactionType.EXPENSE
        )
    }

    var amount by remember { mutableStateOf(initialTransaction?.amount ?: 0.0) }
    var showCalculator by remember { mutableStateOf(false) }

    val filteredCategories = remember(categories, selectedType) {
        if (selectedType == TransactionType.INCOME) categories.filter { it.isIncome }
        else categories.filter { !it.isIncome }
    }

    var selectedCategoryId by remember {
        mutableStateOf(
            initialTransaction?.categoryId
                ?: filteredCategories.firstOrNull()?.id
                ?: categories.firstOrNull()?.id
                ?: ""
        )
    }

    var selectedAccountId by remember {
        mutableStateOf(initialTransaction?.accountId ?: accounts.firstOrNull()?.id ?: "")
    }

    var targetAccountId by remember {
        mutableStateOf(initialTransaction?.targetAccountId ?: accounts.getOrNull(1)?.id ?: "")
    }

    var dateTimestamp by remember { mutableStateOf(initialTransaction?.date ?: System.currentTimeMillis()) }
    var timeString by remember {
        mutableStateOf(
            if (!initialTransaction?.timeString.isNullOrBlank()) initialTransaction?.timeString ?: ""
            else DateUtils.formatTime(System.currentTimeMillis())
        )
    }
    var note by remember { mutableStateOf(initialTransaction?.note ?: "") }

    // Recurring Options
    var isRecurring by remember { mutableStateOf(initialTransaction?.isRecurring ?: false) }
    var recurrenceFrequency by remember { mutableStateOf(RecurrenceFrequency.MONTHLY) }
    var hasEndDate by remember { mutableStateOf(false) }
    var endDateTimestamp by remember { mutableStateOf(System.currentTimeMillis() + (86400000L * 180)) }

    fun triggerHaptic() {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator?.vibrate(40)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = modalBottomSheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialTransaction == null) "Add Transaction" else "Edit Transaction",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Transaction Type Segmented Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    TransactionType.EXPENSE to "Expense",
                    TransactionType.INCOME to "Income",
                    TransactionType.TRANSFER to "Transfer"
                ).forEach { (type, label) ->
                    val isSelected = selectedType == type
                    val activeBg = when (type) {
                        TransactionType.EXPENSE -> Color(0xFFF43F5E)
                        TransactionType.INCOME -> Color(0xFF10B981)
                        TransactionType.TRANSFER -> Color(0xFF8B5CF6)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) activeBg else Color.Transparent)
                            .clickable {
                                selectedType = type
                                triggerHaptic()
                            }
                            .padding(vertical = 10.dp)
                            .testTag("type_${type.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Amount Box (Tap for In-Built Calculator)
            val selectedAccount = accounts.find { it.id == selectedAccountId }
            val currencyCode = selectedAccount?.currencyCode ?: "USD"

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { showCalculator = true }
                    .testTag("amount_field_card"),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = ButtonDefaults.outlinedButtonBorder
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Amount (${currencyCode})",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = CurrencyFormatter.format(amount, currencyCode),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (selectedType) {
                                TransactionType.EXPENSE -> Color(0xFFF43F5E)
                                TransactionType.INCOME -> Color(0xFF10B981)
                                TransactionType.TRANSFER -> Color(0xFF8B5CF6)
                            }
                        )
                    }
                    FilledTonalIconButton(
                        onClick = { showCalculator = true },
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Calculate, contentDescription = "Open Calculator")
                    }
                }
            }

            if (showCalculator) {
                CalculatorBottomSheet(
                    initialAmount = amount,
                    currencyCode = currencyCode,
                    onDismiss = { showCalculator = false },
                    onResultSelected = { res ->
                        amount = res
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Categories (Chips) - only for Expense / Income
            if (selectedType != TransactionType.TRANSFER) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onAddNewCategory) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Custom")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filteredCategories.forEach { cat ->
                        val isSelected = cat.id == selectedCategoryId
                        val catColor = try { Color(android.graphics.Color.parseColor(cat.colorHex)) } catch (e: Exception) { Color.Gray }

                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedCategoryId = cat.id
                                triggerHaptic()
                            },
                            label = { Text(cat.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            leadingIcon = {
                                Icon(
                                    imageVector = CategoryIconHelper.getIcon(cat.iconName),
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else catColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = catColor,
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("cat_chip_${cat.id}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
            }

            // Account Selection
            Text(
                text = if (selectedType == TransactionType.TRANSFER) "From Account" else "Account",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                accounts.forEach { acc ->
                    val isSelected = acc.id == selectedAccountId
                    val accColor = try { Color(android.graphics.Color.parseColor(acc.colorHex)) } catch (e: Exception) { Color.Blue }

                    ElevatedFilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedAccountId = acc.id
                            triggerHaptic()
                        },
                        label = { Text("${acc.name} (${CurrencyFormatter.formatCompact(acc.currentBalance, acc.currencyCode)})") },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(accColor)
                            )
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            if (selectedType == TransactionType.TRANSFER) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "To Account",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    accounts.filter { it.id != selectedAccountId }.forEach { acc ->
                        val isSelected = acc.id == targetAccountId
                        val accColor = try { Color(android.graphics.Color.parseColor(acc.colorHex)) } catch (e: Exception) { Color.Blue }

                        ElevatedFilterChip(
                            selected = isSelected,
                            onClick = {
                                targetAccountId = acc.id
                                triggerHaptic()
                            },
                            label = { Text("${acc.name} (${CurrencyFormatter.formatCompact(acc.currentBalance, acc.currencyCode)})") },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(accColor)
                                )
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Date and Time Picker Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = dateTimestamp }
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                val newCal = Calendar.getInstance().apply {
                                    set(Calendar.YEAR, y)
                                    set(Calendar.MONTH, m)
                                    set(Calendar.DAY_OF_MONTH, d)
                                }
                                dateTimestamp = newCal.timeInMillis
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(DateUtils.formatDate(dateTimestamp), fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = {
                        val cal = Calendar.getInstance()
                        TimePickerDialog(
                            context,
                            { _, h, min ->
                                val calTime = Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, h)
                                    set(Calendar.MINUTE, min)
                                }
                                timeString = DateUtils.formatTime(calTime.timeInMillis)
                            },
                            cal.get(Calendar.HOUR_OF_DAY),
                            cal.get(Calendar.MINUTE),
                            false
                        ).show()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(timeString.ifBlank { "Select Time" }, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Note TextField
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note / Description (Optional)") },
                placeholder = { Text("e.g. Dinner, Grocery run, Paycheck...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_note_input"),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Recurring Rule Section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = ButtonDefaults.outlinedButtonBorder
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Repeat,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Mark as Recurring", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Auto-generate transactions on schedule", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = isRecurring,
                            onCheckedChange = {
                                isRecurring = it
                                triggerHaptic()
                            }
                        )
                    }

                    if (isRecurring) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Frequency", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            RecurrenceFrequency.entries.forEach { freq ->
                                val isSelected = recurrenceFrequency == freq
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { recurrenceFrequency = freq },
                                    label = { Text(freq.displayName, fontSize = 12.sp) },
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save Button
            Button(
                onClick = {
                    if (amount <= 0.0) return@Button

                    val tx = (initialTransaction ?: TransactionEntity(
                        id = UUID.randomUUID().toString(),
                        accountId = selectedAccountId,
                        categoryId = if (selectedType == TransactionType.TRANSFER) "cat_transfer" else selectedCategoryId,
                        amount = amount,
                        type = selectedType.name,
                        date = dateTimestamp,
                        timeString = timeString
                    )).copy(
                        accountId = selectedAccountId,
                        categoryId = if (selectedType == TransactionType.TRANSFER) "cat_transfer" else selectedCategoryId,
                        amount = amount,
                        type = selectedType.name,
                        note = note,
                        date = dateTimestamp,
                        timeString = timeString,
                        isRecurring = isRecurring,
                        targetAccountId = if (selectedType == TransactionType.TRANSFER) targetAccountId else null
                    )

                    val recurringRule = if (isRecurring) {
                        RecurringRuleEntity(
                            id = "rec_${UUID.randomUUID().toString().take(8)}",
                            accountId = selectedAccountId,
                            categoryId = if (selectedType == TransactionType.TRANSFER) "cat_transfer" else selectedCategoryId,
                            amount = amount,
                            type = selectedType.name,
                            note = note,
                            frequency = recurrenceFrequency.name,
                            startDate = dateTimestamp,
                            endDate = if (hasEndDate) endDateTimestamp else null,
                            nextOccurrence = dateTimestamp + (86400000L * 30),
                            isActive = true
                        )
                    } else null

                    triggerHaptic()
                    onSave(tx, recurringRule)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_transaction_button"),
                shape = RoundedCornerShape(16.dp),
                enabled = amount > 0.0 && selectedAccountId.isNotBlank()
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (initialTransaction == null) "Save Transaction" else "Update Transaction",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
