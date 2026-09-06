package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineCalendarScreen(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    accounts: List<AccountEntity>,
    currencyCode: String = "USD",
    onTransactionClick: (TransactionEntity) -> Unit,
    onTransactionDelete: (TransactionEntity) -> Unit,
    onAddTransactionClick: () -> Unit
) {
    val categoriesMap = remember(categories) { categories.associateBy { it.id } }
    val accountsMap = remember(accounts) { accounts.associateBy { it.id } }

    var selectedViewMode by remember { mutableStateOf("TIMELINE") } // "TIMELINE" or "CALENDAR"
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }

    // Calendar state
    var currentCalMonth by remember {
        mutableStateOf(Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
        })
    }
    var selectedCalendarTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }

    // Filter transactions
    val filteredTransactions = remember(transactions, searchQuery, selectedCategoryFilter) {
        transactions.filter { tx ->
            val matchesSearch = if (searchQuery.isBlank()) true else {
                val catName = categoriesMap[tx.categoryId]?.name ?: ""
                tx.note.contains(searchQuery, ignoreCase = true) || catName.contains(searchQuery, ignoreCase = true)
            }
            val matchesCategory = if (selectedCategoryFilter == "ALL") true else tx.categoryId == selectedCategoryFilter
            matchesSearch && matchesCategory
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Activity & History", fontWeight = FontWeight.Bold)
                },
                actions = {
                    IconButton(onClick = onAddTransactionClick) {
                        Icon(Icons.Default.Add, contentDescription = "Add Transaction")
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
            // Bento View Switcher (Timeline vs Calendar)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedViewMode == "TIMELINE") MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { selectedViewMode = "TIMELINE" }
                            .padding(vertical = 10.dp)
                            .testTag("tab_timeline"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.ViewList,
                                contentDescription = null,
                                tint = if (selectedViewMode == "TIMELINE") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Timeline",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (selectedViewMode == "TIMELINE") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedViewMode == "CALENDAR") MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { selectedViewMode = "CALENDAR" }
                            .padding(vertical = 10.dp)
                            .testTag("tab_calendar"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = if (selectedViewMode == "CALENDAR") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Calendar View",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (selectedViewMode == "CALENDAR") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (selectedViewMode == "TIMELINE") {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search description or category...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("timeline_search_input"),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category Chips Filter
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCategoryFilter == "ALL",
                        onClick = { selectedCategoryFilter = "ALL" },
                        label = { Text("All Categories", fontSize = 12.sp) },
                        shape = RoundedCornerShape(12.dp)
                    )
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategoryFilter == cat.id,
                            onClick = { selectedCategoryFilter = cat.id },
                            label = { Text(cat.name, fontSize = 12.sp) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Grouped Timeline List
                val groupedByDay = remember(filteredTransactions) {
                    filteredTransactions.groupBy {
                        val cal = Calendar.getInstance().apply { timeInMillis = it.date }
                        cal.set(Calendar.HOUR_OF_DAY, 0)
                        cal.set(Calendar.MINUTE, 0)
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        cal.timeInMillis
                    }
                }

                if (groupedByDay.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No matching transactions found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        groupedByDay.forEach { (dayTimestamp, dayList) ->
                            val dayTotalExpense = dayList.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                            val dayTotalIncome = dayList.filter { it.type == "INCOME" }.sumOf { it.amount }

                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = DateUtils.getRelativeDateHeader(dayTimestamp),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (dayTotalIncome > 0) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFF10B981).copy(alpha = 0.12f)
                                            ) {
                                                Text(
                                                    "+${CurrencyFormatter.formatCompact(dayTotalIncome, currencyCode)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF10B981),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        if (dayTotalExpense > 0) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFFEF4444).copy(alpha = 0.12f)
                                            ) {
                                                Text(
                                                    "-${CurrencyFormatter.formatCompact(dayTotalExpense, currencyCode)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFEF4444),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            items(dayList, key = { it.id }) { tx ->
                                BentoTransactionItemCard(
                                    transaction = tx,
                                    category = categoriesMap[tx.categoryId],
                                    account = accountsMap[tx.accountId],
                                    currencyCode = currencyCode,
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
            } else {
                // Calendar View Bento Box
                val year = currentCalMonth.get(Calendar.YEAR)
                val month = currentCalMonth.get(Calendar.MONTH)
                val daysMatrix = remember(year, month) { DateUtils.getDaysInMonthMatrix(year, month) }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Month Navigator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                val newCal = (currentCalMonth.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
                                currentCalMonth = newCal
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev Month")
                            }

                            Text(
                                text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(currentCalMonth.time),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            IconButton(onClick = {
                                val newCal = (currentCalMonth.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                                currentCalMonth = newCal
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
                            }
                        }

                        // Days of week header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            listOf("S", "M", "T", "W", "T", "F", "S").forEach { d ->
                                Text(
                                    d,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Days Grid
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(7),
                            modifier = Modifier.height(240.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(daysMatrix) { day ->
                                val isSelected = DateUtils.isSameDay(day.timestamp, selectedCalendarTimestamp)
                                val dayTx = transactions.filter { DateUtils.isSameDay(it.date, day.timestamp) }
                                val hasIncome = dayTx.any { it.type == "INCOME" }
                                val hasExpense = dayTx.any { it.type == "EXPENSE" }

                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            when {
                                                isSelected -> MaterialTheme.colorScheme.primary
                                                day.isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                                else -> Color.Transparent
                                            }
                                        )
                                        .border(
                                            width = if (isSelected) 1.5.dp else 0.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            selectedCalendarTimestamp = day.timestamp
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = day.dayOfMonth.toString(),
                                            fontSize = 13.sp,
                                            fontWeight = if (day.isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = when {
                                                isSelected -> Color.White
                                                day.isCurrentMonth -> MaterialTheme.colorScheme.onSurface
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                            }
                                        )
                                        if (hasIncome || hasExpense) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                modifier = Modifier.padding(top = 2.dp)
                                            ) {
                                                if (hasIncome) Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(if (isSelected) Color.White else Color(0xFF10B981)))
                                                if (hasExpense) Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(if (isSelected) Color(0xFFFECDD3) else Color(0xFFEF4444)))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Selected Day Drill Down
                val selectedDayTransactions = transactions.filter { DateUtils.isSameDay(it.date, selectedCalendarTimestamp) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = DateUtils.getRelativeDateHeader(selectedCalendarTimestamp),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${selectedDayTransactions.size} transactions",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (selectedDayTransactions.isEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No transactions on this date", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(selectedDayTransactions, key = { it.id }) { tx ->
                            BentoTransactionItemCard(
                                transaction = tx,
                                category = categoriesMap[tx.categoryId],
                                account = accountsMap[tx.accountId],
                                currencyCode = currencyCode,
                                onClick = { onTransactionClick(tx) },
                                onDelete = { onTransactionDelete(tx) }
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
}
