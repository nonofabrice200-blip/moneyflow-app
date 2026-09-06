package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DataExporters {

    fun exportTransactionsToCsv(
        context: Context,
        transactions: List<TransactionEntity>,
        categoriesMap: Map<String, CategoryEntity>
    ): File? {
        return try {
            val fileName = "FinanceFlow_Transactions_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.csv"
            val file = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(file)

            val writer = outputStream.bufferedWriter()
            writer.write("ID,Date,Time,Type,Category,Amount,Note,IsRecurring\n")

            for (tx in transactions) {
                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(tx.date))
                val catName = categoriesMap[tx.categoryId]?.name ?: "Uncategorized"
                val escapedNote = "\"${tx.note.replace("\"", "\"\"")}\""
                writer.write("${tx.id},$dateStr,${tx.timeString},${tx.type},$catName,${tx.amount},$escapedNote,${tx.isRecurring}\n")
            }

            writer.flush()
            writer.close()
            outputStream.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportTransactionsToPdf(
        context: Context,
        transactions: List<TransactionEntity>,
        categoriesMap: Map<String, CategoryEntity>,
        currencyCode: String = "USD"
    ): File? {
        return try {
            val pdfDoc = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
            val page = pdfDoc.startPage(pageInfo)
            val canvas = page.canvas

            val titlePaint = Paint().apply {
                color = Color.parseColor("#0F172A")
                textSize = 18f
                isFakeBoldText = true
            }

            val subtitlePaint = Paint().apply {
                color = Color.parseColor("#64748B")
                textSize = 10f
            }

            val headerPaint = Paint().apply {
                color = Color.parseColor("#1E293B")
                textSize = 10f
                isFakeBoldText = true
            }

            val bodyPaint = Paint().apply {
                color = Color.parseColor("#334155")
                textSize = 9f
            }

            val incomePaint = Paint().apply {
                color = Color.parseColor("#10B981")
                textSize = 9f
                isFakeBoldText = true
            }

            val expensePaint = Paint().apply {
                color = Color.parseColor("#EF4444")
                textSize = 9f
                isFakeBoldText = true
            }

            // Draw Header
            canvas.drawText("FinanceFlow Statement Report", 40f, 50f, titlePaint)
            val dateGenerated = "Generated on ${SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date())}"
            canvas.drawText(dateGenerated, 40f, 68f, subtitlePaint)

            // Draw Table Header
            var y = 100f
            val linePaint = Paint().apply {
                color = Color.parseColor("#E2E8F0")
                strokeWidth = 1f
            }
            canvas.drawLine(40f, y, 555f, y, linePaint)
            y += 16f

            canvas.drawText("Date", 40f, y, headerPaint)
            canvas.drawText("Category", 130f, y, headerPaint)
            canvas.drawText("Note / Description", 240f, y, headerPaint)
            canvas.drawText("Type", 420f, y, headerPaint)
            canvas.drawText("Amount", 480f, y, headerPaint)

            y += 8f
            canvas.drawLine(40f, y, 555f, y, linePaint)
            y += 16f

            for (tx in transactions.take(35)) {
                val dateStr = SimpleDateFormat("MMM d, yy", Locale.getDefault()).format(Date(tx.date))
                val catName = (categoriesMap[tx.categoryId]?.name ?: "Other").take(16)
                val noteStr = (if (tx.note.isNotBlank()) tx.note else "—").take(26)
                val formattedAmt = CurrencyFormatter.format(tx.amount, currencyCode)

                canvas.drawText(dateStr, 40f, y, bodyPaint)
                canvas.drawText(catName, 130f, y, bodyPaint)
                canvas.drawText(noteStr, 240f, y, bodyPaint)
                canvas.drawText(tx.type, 420f, y, bodyPaint)

                val paint = if (tx.type == "INCOME") incomePaint else expensePaint
                canvas.drawText(formattedAmt, 480f, y, paint)

                y += 18f
                if (y > 800f) break
            }

            pdfDoc.finishPage(page)

            val fileName = "FinanceFlow_Report_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.pdf"
            val file = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(file)
            pdfDoc.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDoc.close()

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun shareFile(context: Context, file: File, mimeType: String) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Statement"))
        } catch (e: Exception) {
            // Fallback
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_TEXT, "Export file created: ${file.name}")
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Statement"))
        }
    }
}
