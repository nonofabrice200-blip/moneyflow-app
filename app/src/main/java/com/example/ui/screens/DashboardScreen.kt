package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.remote.SyncState
import com.example.ui.BudgetDashboardState
import com.example.ui.components.CategoryIconHelper
import com.example.ui.theme.*
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    transactions: List<TransactionEntity>,
    savingsGoals: List<SavingsGoalEntity>,
    budgetState: BudgetDashboardState,
    selectedAccountId: String,
    netWorth: Double,
    syncState: SyncState,
    onSelectAccount: (String) -> Unit,
    onAddTransactionClick: () -> Unit,
    onOpenCalculatorClick: () -> Unit,
    onTransactionClick: (TransactionEntity) -> Unit,
    onTransactionDelete: (TransactionEntity) -> Unit,
    onViewAllTransactions: () -> Unit,
    onSyncClick: () -> Unit,
    onOpenSettingsClick: () -> Unit
) {
    val categoriesMap = remember(categories) { categories.associateBy { it.id } }
    val accountsMap = remember(accounts) { accounts.associateBy { it.id } }

    var accountMenuExpanded by remember { mutableStateOf(false) }

    val currentAccountName = if (selectedAccountId == "ALL") {
        "All Accounts (${accounts.size})"
    } else {
        accountsMap[selectedAccountId]?.name ?: "Account"
    }

    val baseCurrency = if (selectedAccountId == "ALL") "USD" else (accountsMap[selectedAccountId]?.currencyCode ?: "USD")

    val totalIncome = remember(transactions) {
        transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    }
    val totalExpense = remember(transactions) {
        transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier.clickable { accountMenuExpanded = true }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = currentAccountName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = accountMenuExpanded,
                            onDismissRequest = { accountMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Accounts", fontWeight = FontWeight.Bold) },
                                onClick = {
                                    onSelectAccount("ALL")
                                    accountMenuExpanded = false
                                },
                                leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) }
                            )
                            HorizontalDivider()
                            accounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text("${acc.name} (${CurrencyFormatter.formatCompact(acc.currentBalance, acc.currencyCode)})") },
                                    onClick = {
                                        onSelectAccount(acc.id)
                                        accountMenuExpanded = false
                                    },
                                    leadingIcon = {
                                        val color = try { Color(android.graphics.Color.parseColor(acc.colorHex)) } catch (e: Exception) { Color.Gray }
                                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color))
                                    }
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Sync Status Indicator & Button
                    IconButton(onClick = onSyncClick, modifier = Modifier.testTag("sync_icon_button")) {
                        when (syncState) {
                            is SyncState.Syncing -> CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            is SyncState.Success -> Icon(Icons.Default.CloudDone, contentDescription = "Synced", tint = Color(0xFF10B981))
                            else -> Icon(Icons.Default.CloudSync, contentDescription = "Sync")
                        }
                    }
                    IconButton(onClick = onOpenSettingsClick) {
                        Icon(Icons.Default.Tune, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Bento Hero Net Worth Card
            item {
                BentoHeroBalanceCard(
                    netWorth = netWorth,
                    selectedAccountId = selectedAccountId,
                    account = accountsMap[selectedAccountId],
                    currencyCode = baseCurrency
                )
            }

            // 2. Bento 2-Column Split: Income vs Expense
            item {
                BentoIncomeExpenseGrid(
                    incomeAmount = totalIncome,
                    expenseAmount = totalExpense,
                    currencyCode = baseCurrency
                )
            }

            // 3. Bento Budget Pay Cycle Card
            item {
                BentoBudgetProgressCard(
                    budgetState = budgetState,
                    currencyCode = baseCurrency
                )
            }

            // 4. Bento Quick Action Row
            item {
                BentoQuickActionsRow(
                    onAddClick = onAddTransactionClick,
                    onCalculatorClick = onOpenCalculatorClick
                )
            }

            // 5. Bento Savings Goals Snapshot Carousel
            if (savingsGoals.isNotEmpty()) {
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Savings Goals",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${savingsGoals.size} active",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            savingsGoals.forEach { goal ->
                                BentoGoalSnapshotCard(goal = goal, currencyCode = baseCurrency)
                            }
                        }
                    }
                }
            }

            // 6. Recent Transactions Header & Container
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Activity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onViewAllTransactions) {
                        Text("See All", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                }
            }

            if (transactions.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Receipt,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("No transactions yet", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                "Tap 'Add Transaction' to record your first income or spend",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(transactions.take(6), key = { it.id }) { tx ->
                    val category = categoriesMap[tx.categoryId]
                    BentoTransactionItemCard(
                        transaction = tx,
                        category = category,
                        account = accountsMap[tx.accountId],
                        currencyCode = baseCurrency,
                        onClick = { onTransactionClick(tx) },
                        onDelete = { onTransactionDelete(tx) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}

@Composable
fun BentoHeroBalanceCard(
    netWorth: Double,
    selectedAccountId: String,
    account: AccountEntity?,
    currencyCode: String
) {
    val isAll = selectedAccountId == "ALL"
    val displayAmount = if (isAll) netWorth else (account?.currentBalance ?: 0.0)
    val titleLabel = if (isAll) "Total Net Worth" else "${account?.name ?: "Account"} Balance"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)),
        color = BentoEmeraldPrimary,
        shadowElevation = 4.dp,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.25f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0F766E),
                            Color(0xFF0D9488),
                            Color(0xFF115E59)
                        )
                    )
                )
                .padding(22.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF86EFAC))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = titleLabel.uppercase(),
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = currencyCode,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = CurrencyFormatter.format(displayAmount, currencyCode),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = (-0.5).sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.15f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = Color(0xFF86EFAC),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Active Portfolio", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.15f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Encrypted Safe", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BentoIncomeExpenseGrid(
    incomeAmount: Double,
    expenseAmount: Double,
    currencyCode: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Income Bento Box
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(20.dp),
            color = BentoEmeraldContainer,
            border = androidx.compose.foundation.BorderStroke(1.dp, BentoEmeraldBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "INCOME",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BentoEmeraldOnContainer,
                        letterSpacing = 0.8.sp
                    )
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "+${CurrencyFormatter.formatCompact(incomeAmount, currencyCode)}",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BentoEmeraldOnContainer
                )
            }
        }

        // Expense Bento Box
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(20.dp),
            color = BentoRoseContainer,
            border = androidx.compose.foundation.BorderStroke(1.dp, BentoRoseBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "EXPENSE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BentoRoseOnContainer,
                        letterSpacing = 0.8.sp
                    )
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF43F5E).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = null,
                            tint = Color(0xFFF43F5E),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "-${CurrencyFormatter.formatCompact(expenseAmount, currencyCode)}",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BentoRoseOnContainer
                )
            }
        }
    }
}

@Composable
fun BentoBudgetProgressCard(
    budgetState: BudgetDashboardState,
    currencyCode: String
) {
    val progress = (budgetState.percentSpent / 100f).coerceIn(0f, 1f)
    val isOver = budgetState.isOverBudget
    val isWarning = budgetState.percentSpent in 80..99

    val progressColor = when {
        isOver -> Color(0xFFEF4444)
        isWarning -> Color(0xFFF59E0B)
        else -> Color(0xFF10B981)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${budgetState.payCycle.displayName} Budget",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = budgetState.payPeriodRange,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = progressColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${budgetState.percentSpent}% Used",
                        color = progressColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3-Metric Bento Tiles: Spent / Remaining / Daily Allowance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Spent", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            CurrencyFormatter.formatCompact(budgetState.totalSpent, currencyCode),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444)
                        )
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Remaining", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            CurrencyFormatter.formatCompact(budgetState.remainingBudget, currencyCode),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (budgetState.remainingBudget > 0) Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Daily (${budgetState.daysRemaining}d)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            CurrencyFormatter.formatCompact(budgetState.dailyAllowance, currencyCode),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BentoQuickActionsRow(
    onAddClick: () -> Unit,
    onCalculatorClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = onAddClick,
            modifier = Modifier
                .weight(1.3f)
                .height(48.dp)
                .testTag("quick_add_transaction_btn"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Add Transaction", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        FilledTonalButton(
            onClick = onCalculatorClick,
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("quick_calc_btn"),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Calculator", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
    }
}

@Composable
fun BentoGoalSnapshotCard(
    goal: SavingsGoalEntity,
    currencyCode: String
) {
    val progress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
    val color = try { Color(android.graphics.Color.parseColor(goal.colorHex)) } catch (e: Exception) { Color(0xFF10B981) }

    Surface(
        modifier = Modifier
            .width(170.dp)
            .clip(RoundedCornerShape(20.dp)),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        CategoryIconHelper.getIcon(goal.iconName),
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = color.copy(alpha = 0.12f)
                ) {
                    Text(
                        "${(progress * 100).toInt()}%",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = color,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(goal.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1)

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                "${CurrencyFormatter.formatCompact(goal.currentAmount, currencyCode)} / ${CurrencyFormatter.formatCompact(goal.targetAmount, currencyCode)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
fun BentoTransactionItemCard(
    transaction: TransactionEntity,
    category: CategoryEntity?,
    account: AccountEntity?,
    currencyCode: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val isExpense = transaction.type == "EXPENSE"
    val isIncome = transaction.type == "INCOME"
    val isTransfer = transaction.type == "TRANSFER"

    val catColor = try {
        Color(android.graphics.Color.parseColor(category?.colorHex ?: "#64748B"))
    } catch (e: Exception) {
        Color(0xFF64748B)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("tx_item_${transaction.id}"),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(catColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = CategoryIconHelper.getIcon(category?.iconName ?: "shopping_bag"),
                        contentDescription = null,
                        tint = catColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = category?.name ?: (if (isTransfer) "Transfer" else "Transaction"),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (transaction.isRecurring) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.Repeat, contentDescription = "Recurring", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    val subtitle = if (transaction.note.isNotBlank()) transaction.note else (account?.name ?: "Account")
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                val prefix = if (isIncome) "+" else if (isExpense) "-" else ""
                val amountColor = when {
                    isIncome -> Color(0xFF10B981)
                    isExpense -> Color(0xFFEF4444)
                    else -> Color(0xFF8B5CF6)
                }

                Text(
                    text = "$prefix${CurrencyFormatter.format(transaction.amount, currencyCode)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )

                Text(
                    text = DateUtils.formatShortDate(transaction.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
