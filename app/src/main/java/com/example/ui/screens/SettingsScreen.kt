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
import com.example.data.local.entity.BudgetSettingsEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.remote.SyncState
import com.example.domain.model.PayCycle
import com.example.ui.categories.AddEditCategoryDialog
import com.example.ui.components.CategoryIconHelper
import com.example.util.CurrencyFormatter
import com.example.util.SecurityManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    budgetSettings: BudgetSettingsEntity?,
    categories: List<CategoryEntity>,
    syncState: SyncState,
    securityManager: SecurityManager,
    onUpdateBudgetSettings: (PayCycle, Double) -> Unit,
    onSaveCategory: (CategoryEntity) -> Unit,
    onDeleteCategory: (CategoryEntity) -> Unit,
    onSyncNow: () -> Unit
) {
    val context = LocalContext.current

    var payCycle by remember(budgetSettings) {
        mutableStateOf(
            if (budgetSettings != null) PayCycle.fromString(budgetSettings.payCycle)
            else PayCycle.MONTHLY
        )
    }
    var totalBudgetInput by remember(budgetSettings) {
        mutableStateOf(budgetSettings?.totalBudget?.toInt()?.toString() ?: "3200")
    }

    var isBiometricEnabled by remember { mutableStateOf(securityManager.isBiometricEnabled) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Preferences", fontWeight = FontWeight.Bold) }
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
            // 1. Budget & Pay Cycle Settings
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = ButtonDefaults.outlinedButtonBorder
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Savings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pay Cycle & Budget Limits", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text("Select Pay Cycle", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(PayCycle.WEEKLY, PayCycle.FORTNIGHTLY, PayCycle.MONTHLY).forEach { cycle ->
                                val isSelected = payCycle == cycle
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { payCycle = cycle },
                                    label = { Text(cycle.displayName, fontSize = 12.sp) },
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = totalBudgetInput,
                            onValueChange = { totalBudgetInput = it },
                            label = { Text("Budget Limit ($)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                val budget = totalBudgetInput.toDoubleOrNull() ?: 3000.0
                                onUpdateBudgetSettings(payCycle, budget)
                                Toast.makeText(context, "Budget settings updated!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save Budget Settings", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 2. Category Manager
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
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
                                Icon(Icons.Default.Category, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Category Manager (${categories.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }

                            FilledTonalButton(
                                onClick = { showAddCategoryDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New", fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        categories.forEach { cat ->
                            val catColor = try { Color(android.graphics.Color.parseColor(cat.colorHex)) } catch (e: Exception) { Color.Gray }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(catColor.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            CategoryIconHelper.getIcon(cat.iconName),
                                            contentDescription = null,
                                            tint = catColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(cat.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text(if (cat.isIncome) "Income" else "Expense", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                if (!cat.isDefault) {
                                    IconButton(onClick = { onDeleteCategory(cat) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                } else {
                                    Text("Default", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            // 3. Security & Biometrics
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
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
                                Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Biometric Security", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("Require Fingerprint / Face Unlock", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Switch(
                                checked = isBiometricEnabled,
                                onCheckedChange = {
                                    isBiometricEnabled = it
                                    securityManager.isBiometricEnabled = it
                                }
                            )
                        }
                    }
                }
            }

            // 4. Cloud Synchronization
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
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
                                Icon(Icons.Default.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Cloud Sync (Firestore)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    val lastTime = securityManager.lastSyncTimestamp
                                    val text = if (lastTime > 0) "Last synced: ${SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(lastTime))}" else "Ready to sync"
                                    Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Button(
                                onClick = onSyncNow,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Sync Now", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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

    if (showAddCategoryDialog) {
        AddEditCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onSave = { cat ->
                onSaveCategory(cat)
                showAddCategoryDialog = false
                Toast.makeText(context, "Added category ${cat.name}", Toast.LENGTH_SHORT).show()
            }
        )
    }
}
