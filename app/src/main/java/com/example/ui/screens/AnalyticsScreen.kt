package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TrendingUp
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
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.CurrencyFormatter
import com.example.util.DataExporters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    categorySpending: List<CategorySpendItem>,
    monthlyBarData: List<MonthlyBarData>,
    spendingTrend: List<TrendPoint>,
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    currencyCode: String = "USD"
) {
    val context = LocalContext.current
    val categoriesMap = remember(categories) { categories.associateBy { it.id } }

    val topCategory = remember(categorySpending) { categorySpending.maxByOrNull { it.amount } }
    val totalExpense = remember(categorySpending) { categorySpending.sumOf { it.amount } }
    val totalIncome = remember(monthlyBarData) { monthlyBarData.sumOf { it.income } }
    val totalExp6Mo = remember(monthlyBarData) { monthlyBarData.sumOf { it.expense } }
    val savingsRate = if (totalIncome > 0) (((totalIncome - totalExp6Mo) / totalIncome) * 100).toInt().coerceIn(0, 100) else 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Analytics & Reports", fontWeight = FontWeight.Bold)
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Bento KPI Stats Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Top Category Bento Tile
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        color = BentoPurpleContainer,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BentoPurpleBorder)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "TOP SPEND",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BentoPurpleOnContainer,
                                    letterSpacing = 0.8.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(BentoPurpleAccent.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.PieChart, contentDescription = null, tint = BentoPurpleAccent, modifier = Modifier.size(14.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = topCategory?.categoryName ?: "None",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = BentoPurpleOnContainer,
                                maxLines = 1
                            )
                            Text(
                                text = CurrencyFormatter.formatCompact(topCategory?.amount ?: 0.0, currencyCode),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BentoPurpleOnContainer.copy(alpha = 0.75f)
                            )
                        }
                    }

                    // Savings Rate Bento Tile
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        color = BentoBlueContainer,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BentoBlueBorder)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "SAVINGS RATE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BentoBlueOnContainer,
                                    letterSpacing = 0.8.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(BentoBluePrimary.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = BentoBluePrimary, modifier = Modifier.size(14.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "$savingsRate%",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = BentoBlueOnContainer
                            )
                            Text(
                                text = "Last 6 months",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BentoBlueOnContainer.copy(alpha = 0.75f)
                            )
                        }
                    }
                }
            }

            // 2. Spending Breakdown Bento Card
            item {
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
                                    text = "Spending Breakdown",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Category distribution for active filter",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            ) {
                                Text(
                                    text = CurrencyFormatter.formatCompact(totalExpense, currencyCode),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        SpendingDonutChart(
                            items = categorySpending,
                            currencyCode = currencyCode
                        )
                    }
                }
            }

            // 3. 6-Month Income vs Expense Bar Chart Bento Card
            item {
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
                        Text(
                            text = "Cash Flow (Last 6 Months)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Income vs Expense comparison",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        MonthlyComparisonBarChart(
                            dataList = monthlyBarData,
                            currencyCode = currencyCode
                        )
                    }
                }
            }

            // 4. 7-Day Spending Velocity Bento Card
            item {
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
                        Text(
                            text = "7-Day Spending Velocity",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Daily expense velocity curve",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        SpendingTrendLineChart(
                            points = spendingTrend,
                            currencyCode = currencyCode
                        )
                    }
                }
            }

            // 5. Data Export Bento Hub Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = BentoEmeraldContainer,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BentoEmeraldBorder)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(BentoEmeraldPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, tint = BentoEmeraldPrimary, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Export Statement & Reports",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BentoEmeraldOnContainer
                            )
                        }

                        Text(
                            text = "Generate and share PDF financial statements or CSV spreadsheet records.",
                            style = MaterialTheme.typography.bodySmall,
                            color = BentoEmeraldOnContainer.copy(alpha = 0.8f),
                            modifier = Modifier.padding(top = 6.dp, bottom = 14.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    val pdfFile = DataExporters.exportTransactionsToPdf(context, transactions, categoriesMap, currencyCode)
                                    if (pdfFile != null) {
                                        DataExporters.shareFile(context, pdfFile, "application/pdf")
                                    } else {
                                        Toast.makeText(context, "Failed to create PDF", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("export_pdf_btn"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BentoEmeraldPrimary,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("PDF Report", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            FilledTonalButton(
                                onClick = {
                                    val csvFile = DataExporters.exportTransactionsToCsv(context, transactions, categoriesMap)
                                    if (csvFile != null) {
                                        DataExporters.shareFile(context, csvFile, "text/csv")
                                    } else {
                                        Toast.makeText(context, "Failed to create CSV", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("export_csv_btn"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("CSV Table", fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
}
