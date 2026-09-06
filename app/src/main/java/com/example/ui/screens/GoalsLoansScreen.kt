package com.example.ui.screens

import android.app.DatePickerDialog
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
import com.example.data.local.entity.LoanEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.domain.model.AmortizationScheduleItem
import com.example.ui.components.CategoryIconHelper
import com.example.util.AmortizationCalculator
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils
import java.util.Calendar
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsLoansScreen(
    savingsGoals: List<SavingsGoalEntity>,
    loans: List<LoanEntity>,
    accounts: List<AccountEntity>,
    currencyCode: String = "USD",
    onSaveGoal: (SavingsGoalEntity) -> Unit,
    onDeleteGoal: (SavingsGoalEntity) -> Unit,
    onAddFundsToGoal: (SavingsGoalEntity, Double, String) -> Unit,
    onSaveLoan: (LoanEntity) -> Unit,
    onDeleteLoan: (LoanEntity) -> Unit,
    onLogLoanPayment: (LoanEntity, Double, String) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf("GOALS") } // "GOALS" or "LOANS"

    var showAddGoalDialog by remember { mutableStateOf(false) }
    var selectedGoalForFunds by remember { mutableStateOf<SavingsGoalEntity?>(null) }

    var showAddLoanDialog by remember { mutableStateOf(false) }
    var selectedLoanForSchedule by remember { mutableStateOf<LoanEntity?>(null) }
    var selectedLoanForPayment by remember { mutableStateOf<LoanEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Goals & Loans", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = {
                        if (selectedTab == "GOALS") showAddGoalDialog = true else showAddLoanDialog = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Tab Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedTab == "GOALS") MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { selectedTab = "GOALS" }
                        .padding(vertical = 8.dp)
                        .testTag("tab_goals"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Savings,
                            contentDescription = null,
                            tint = if (selectedTab == "GOALS") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Savings Goals (${savingsGoals.size})",
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == "GOALS") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedTab == "LOANS") MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { selectedTab = "LOANS" }
                        .padding(vertical = 8.dp)
                        .testTag("tab_loans"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = if (selectedTab == "LOANS") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Loans & Debt (${loans.size})",
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == "LOANS") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == "GOALS") {
                if (savingsGoals.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No savings goals created yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(savingsGoals, key = { it.id }) { goal ->
                            SavingsGoalItemCard(
                                goal = goal,
                                currencyCode = currencyCode,
                                onAddFundsClick = { selectedGoalForFunds = goal },
                                onDeleteClick = { onDeleteGoal(goal) }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(60.dp))
                        }
                    }
                }
            } else {
                // Loans Tab
                if (loans.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No active loans tracked", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(loans, key = { it.id }) { loan ->
                            LoanItemCard(
                                loan = loan,
                                currencyCode = currencyCode,
                                onViewScheduleClick = { selectedLoanForSchedule = loan },
                                onLogPaymentClick = { selectedLoanForPayment = loan },
                                onDeleteClick = { onDeleteLoan(loan) }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(60.dp))
                        }
                    }
                }
            }
        }
    }

    // Add Goal Dialog
    if (showAddGoalDialog) {
        AddGoalDialog(
            onDismiss = { showAddGoalDialog = false },
            onSave = { goal ->
                onSaveGoal(goal)
                showAddGoalDialog = false
            }
        )
    }

    // Add Funds to Goal Dialog
    selectedGoalForFunds?.let { goal ->
        AddFundsGoalDialog(
            goal = goal,
            accounts = accounts,
            currencyCode = currencyCode,
            onDismiss = { selectedGoalForFunds = null },
            onAddFunds = { amt, accId ->
                onAddFundsToGoal(goal, amt, accId)
                selectedGoalForFunds = null
                Toast.makeText(context, "Added ${CurrencyFormatter.format(amt, currencyCode)} to ${goal.name}!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Add Loan Dialog
    if (showAddLoanDialog) {
        AddLoanDialog(
            accounts = accounts,
            onDismiss = { showAddLoanDialog = false },
            onSave = { loan ->
                onSaveLoan(loan)
                showAddLoanDialog = false
            }
        )
    }

    // Amortization Schedule Dialog
    selectedLoanForSchedule?.let { loan ->
        AmortizationScheduleDialog(
            loan = loan,
            currencyCode = currencyCode,
            onDismiss = { selectedLoanForSchedule = null }
        )
    }

    // Log Loan Payment Dialog
    selectedLoanForPayment?.let { loan ->
        LogPaymentDialog(
            loan = loan,
            accounts = accounts,
            currencyCode = currencyCode,
            onDismiss = { selectedLoanForPayment = null },
            onLogPayment = { amt, accId ->
                onLogLoanPayment(loan, amt, accId)
                selectedLoanForPayment = null
                Toast.makeText(context, "Logged payment of ${CurrencyFormatter.format(amt, currencyCode)}", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun SavingsGoalItemCard(
    goal: SavingsGoalEntity,
    currencyCode: String,
    onAddFundsClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val progress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
    val pct = (progress * 100).toInt()
    val color = try { Color(android.graphics.Color.parseColor(goal.colorHex)) } catch (e: Exception) { Color(0xFF10B981) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = ButtonDefaults.outlinedButtonBorder
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(CategoryIconHelper.getIcon(goal.iconName), contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(goal.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        val targetDateStr = DateUtils.formatDate(goal.targetDate)
                        Text("Target: $targetDateStr", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (goal.isCompleted) Color(0xFF10B981).copy(alpha = 0.2f) else color.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (goal.isCompleted) "Completed 🎉" else "$pct%",
                        color = if (goal.isCompleted) Color(0xFF10B981) else color,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape),
                color = color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Saved Amount", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${CurrencyFormatter.format(goal.currentAmount, currencyCode)} / ${CurrencyFormatter.format(goal.targetAmount, currencyCode)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Button(
                    onClick = onAddFundsClick,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Contribute", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun LoanItemCard(
    loan: LoanEntity,
    currencyCode: String,
    onViewScheduleClick: () -> Unit,
    onLogPaymentClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = ButtonDefaults.outlinedButtonBorder
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(loan.lenderName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("${loan.interestRate}% APR • ${loan.termMonths} Months", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFEF4444).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "Monthly: ${CurrencyFormatter.format(loan.monthlyPayment, currencyCode)}",
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Remaining Principal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(CurrencyFormatter.format(loan.remainingBalance, currencyCode), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFEF4444))
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Original Loan", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(CurrencyFormatter.format(loan.principalAmount, currencyCode), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onViewScheduleClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CalendarViewMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Amortization", fontSize = 12.sp)
                }

                Button(
                    onClick = onLogPaymentClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Log Payment", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AddGoalDialog(
    onDismiss: () -> Unit,
    onSave: (SavingsGoalEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var targetAmount by remember { mutableStateOf("3000") }
    var currentAmount by remember { mutableStateOf("500") }
    var targetDateTimestamp by remember { mutableStateOf(System.currentTimeMillis() + (86400000L * 90)) }
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Create Savings Goal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Goal Name") },
                    placeholder = { Text("e.g. Dream Vacation, New Car") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = targetAmount,
                    onValueChange = { targetAmount = it },
                    label = { Text("Target Amount ($)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = currentAmount,
                    onValueChange = { currentAmount = it },
                    label = { Text("Starting / Saved Amount ($)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = targetDateTimestamp }
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                val newCal = Calendar.getInstance().apply { set(y, m, d) }
                                targetDateTimestamp = newCal.timeInMillis
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Target Date: ${DateUtils.formatDate(targetDateTimestamp)}")
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        if (name.isBlank()) return@Button
                        val tAmt = targetAmount.toDoubleOrNull() ?: 1000.0
                        val cAmt = currentAmount.toDoubleOrNull() ?: 0.0
                        val goal = SavingsGoalEntity(
                            id = "goal_${UUID.randomUUID().toString().take(8)}",
                            name = name.trim(),
                            targetAmount = tAmt,
                            currentAmount = cAmt,
                            targetDate = targetDateTimestamp,
                            iconName = "savings",
                            colorHex = "#10B981"
                        )
                        onSave(goal)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Save Goal", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AddFundsGoalDialog(
    goal: SavingsGoalEntity,
    accounts: List<AccountEntity>,
    currencyCode: String,
    onDismiss: () -> Unit,
    onAddFunds: (Double, String) -> Unit
) {
    var amountText by remember { mutableStateOf("100") }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Contribute to ${goal.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Contribution Amount ($)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Source Account", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(6.dp))

                accounts.forEach { acc ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedAccountId = acc.id }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedAccountId == acc.id, onClick = { selectedAccountId = acc.id })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${acc.name} (${CurrencyFormatter.formatCompact(acc.currentBalance, acc.currencyCode)})", fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        if (amt > 0 && selectedAccountId.isNotBlank()) {
                            onAddFunds(amt, selectedAccountId)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Confirm Contribution", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AddLoanDialog(
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onSave: (LoanEntity) -> Unit
) {
    var lenderName by remember { mutableStateOf("") }
    var principal by remember { mutableStateOf("15000") }
    var interestRate by remember { mutableStateOf("5.5") }
    var termMonths by remember { mutableStateOf("36") }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: "") }

    val monthlyPayment by remember(principal, interestRate, termMonths) {
        derivedStateOf {
            val p = principal.toDoubleOrNull() ?: 0.0
            val r = interestRate.toDoubleOrNull() ?: 0.0
            val t = termMonths.toIntOrNull() ?: 12
            AmortizationCalculator.calculateMonthlyPayment(p, r, t)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Add Loan / Debt Tracker", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = lenderName,
                    onValueChange = { lenderName = it },
                    label = { Text("Lender / Loan Name") },
                    placeholder = { Text("e.g. Student Loan, Auto Loan") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = principal,
                    onValueChange = { principal = it },
                    label = { Text("Principal Amount ($)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = interestRate,
                        onValueChange = { interestRate = it },
                        label = { Text("Interest Rate (%)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = termMonths,
                        onValueChange = { termMonths = it },
                        label = { Text("Term (Months)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Calculated Monthly:")
                        Text(CurrencyFormatter.format(monthlyPayment, "USD"), fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (lenderName.isBlank()) return@Button
                        val p = principal.toDoubleOrNull() ?: 1000.0
                        val r = interestRate.toDoubleOrNull() ?: 5.0
                        val t = termMonths.toIntOrNull() ?: 12
                        val mPay = AmortizationCalculator.calculateMonthlyPayment(p, r, t)
                        val totalInt = AmortizationCalculator.calculateTotalInterest(mPay, t, p)

                        val loan = LoanEntity(
                            id = "loan_${UUID.randomUUID().toString().take(8)}",
                            accountId = selectedAccountId,
                            lenderName = lenderName.trim(),
                            principalAmount = p,
                            interestRate = r,
                            termMonths = t,
                            startDate = System.currentTimeMillis(),
                            paymentFrequency = "MONTHLY",
                            remainingBalance = p,
                            monthlyPayment = mPay,
                            totalInterest = totalInt
                        )
                        onSave(loan)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Save Loan", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AmortizationScheduleDialog(
    loan: LoanEntity,
    currencyCode: String,
    onDismiss: () -> Unit
) {
    val schedule = remember(loan) {
        AmortizationCalculator.generateSchedule(loan.principalAmount, loan.interestRate, loan.termMonths)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .height(520.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Amortization Schedule", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(loan.lenderName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Mo", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(28.dp))
                    Text("Principal", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    Text("Interest", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    Text("Balance", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(schedule) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${item.monthNumber}", fontSize = 11.sp, modifier = Modifier.width(28.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(CurrencyFormatter.formatCompact(item.principalPaid, currencyCode), fontSize = 11.sp, modifier = Modifier.weight(1f))
                            Text(CurrencyFormatter.formatCompact(item.interestPaid, currencyCode), fontSize = 11.sp, modifier = Modifier.weight(1f), color = Color(0xFFEF4444))
                            Text(CurrencyFormatter.formatCompact(item.remainingBalance, currencyCode), fontSize = 11.sp, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LogPaymentDialog(
    loan: LoanEntity,
    accounts: List<AccountEntity>,
    currencyCode: String,
    onDismiss: () -> Unit,
    onLogPayment: (Double, String) -> Unit
) {
    var paymentAmountText by remember { mutableStateOf(loan.monthlyPayment.toInt().toString()) }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Log Loan Repayment", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(loan.lenderName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = paymentAmountText,
                    onValueChange = { paymentAmountText = it },
                    label = { Text("Payment Amount ($)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Debit From Account", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(6.dp))

                accounts.forEach { acc ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedAccountId = acc.id }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedAccountId == acc.id, onClick = { selectedAccountId = acc.id })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${acc.name} (${CurrencyFormatter.formatCompact(acc.currentBalance, acc.currencyCode)})", fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val amt = paymentAmountText.toDoubleOrNull() ?: 0.0
                        if (amt > 0 && selectedAccountId.isNotBlank()) {
                            onLogPayment(amt, selectedAccountId)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Confirm Payment", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
