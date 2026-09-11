package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.MedicalRecordEntity
import java.io.File
import java.io.FileOutputStream

object MedicalPrintUtils {

    // Base coordinate system matching high-definition image export
    private const val CANVAS_WIDTH = 1080f
    private const val TABLE_MARGIN_HORIZONTAL = 54f
    private const val TABLE_TOP = 180f
    private const val HEADER_HEIGHT = 110f
    private const val ROW_HEIGHT = 115f
    private const val BORDER_STROKE_WIDTH = 4.5f

    // Column widths: ক্রমিক (155), ID (220), কোড (180), নাম (417) -> Total = 972
    private const val COL_WIDTH_INDEX = 155f
    private const val COL_WIDTH_ID = 220f
    private const val COL_WIDTH_CODE = 180f
    // COL_WIDTH_NAME = 972 - (155 + 220 + 180) = 417f

    /**
     * Formats date string (YYYY-MM-DD or DD/MM/YYYY) to DD/MM/YY as shown in the user's sample (e.g. "10/09/26").
     */
    fun formatDateShort(dateStr: String): String {
        return try {
            val clean = dateStr.trim()
            if (clean.matches(Regex("""\d{4}-\d{2}-\d{2}"""))) {
                val parts = clean.split("-")
                val yy = if (parts[0].length == 4) parts[0].substring(2) else parts[0]
                "${parts[2]}/${parts[1]}/$yy"
            } else if (clean.matches(Regex("""\d{2}/\d{2}/\d{4}"""))) {
                val parts = clean.split("/")
                val yy = if (parts[2].length == 4) parts[2].substring(2) else parts[2]
                "${parts[0]}/${parts[1]}/$yy"
            } else {
                clean
            }
        } catch (e: Exception) {
            dateStr
        }
    }

    /**
     * Renders a crisp, high-resolution Bitmap matching the user's sample report image.
     */
    fun renderDailyReportBitmap(
        dateStr: String,
        records: List<MedicalRecordEntity>
    ): Bitmap {
        val width = CANVAS_WIDTH.toInt()
        val minHeight = 1550f // Portrait document aspect ratio like paper sheet
        val contentHeight = TABLE_TOP + HEADER_HEIGHT + (records.size * ROW_HEIGHT) + 200f
        val totalHeight = maxOf(minHeight, contentHeight).toInt()

        val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        drawSinglePageDailyReport(
            canvas = canvas,
            dateStr = dateStr,
            records = records,
            startIndexOffset = 0,
            canvasWidth = CANVAS_WIDTH,
            canvasHeight = totalHeight.toFloat()
        )

        return bitmap
    }

    /**
     * Draws the table and header onto the given Canvas.
     * Table columns: [ ক্রমিক | ID | কোড | নাম ]
     * Perfectly matches the user's uploaded sample picture.
     */
    private fun drawSinglePageDailyReport(
        canvas: Canvas,
        dateStr: String,
        records: List<MedicalRecordEntity>,
        startIndexOffset: Int,
        canvasWidth: Float,
        canvasHeight: Float
    ) {
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = BORDER_STROKE_WIDTH
        }

        val formattedDate = formatDateShort(dateStr)

        val tableLeft = TABLE_MARGIN_HORIZONTAL
        val tableRight = canvasWidth - TABLE_MARGIN_HORIZONTAL
        val tableWidth = tableRight - tableLeft

        val colWidthName = tableWidth - (COL_WIDTH_INDEX + COL_WIDTH_ID + COL_WIDTH_CODE)

        val colX0 = tableLeft
        val colX1 = colX0 + COL_WIDTH_INDEX
        val colX2 = colX1 + COL_WIDTH_ID
        val colX3 = colX2 + COL_WIDTH_CODE
        val colX4 = tableRight

        val rowCount = records.size
        val tableBottom = TABLE_TOP + HEADER_HEIGHT + (rowCount * ROW_HEIGHT)

        // 1. Date Header (Top Right aligned with table right border, e.g. "তারিখ: 10/09/26")
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 46f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val dateText = "তারিখ: $formattedDate"
        val dateTextWidth = datePaint.measureText(dateText)
        val dateX = tableRight - dateTextWidth
        val dateY = 130f
        canvas.drawText(dateText, dateX, dateY, datePaint)

        // 2. Table Outer Border
        canvas.drawRect(tableLeft, TABLE_TOP, tableRight, tableBottom, strokePaint)

        // 3. Header Row Separator Line (Background is pure white)
        canvas.drawLine(tableLeft, TABLE_TOP + HEADER_HEIGHT, tableRight, TABLE_TOP + HEADER_HEIGHT, strokePaint)

        // 4. Vertical Grid Lines
        canvas.drawLine(colX1, TABLE_TOP, colX1, tableBottom, strokePaint)
        canvas.drawLine(colX2, TABLE_TOP, colX2, tableBottom, strokePaint)
        canvas.drawLine(colX3, TABLE_TOP, colX3, tableBottom, strokePaint)

        // 5. Header Texts (Bold, Black, Centered)
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 40f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        drawCenteredText(canvas, headerPaint, "ক্রমিক", colX0, colX1, TABLE_TOP, TABLE_TOP + HEADER_HEIGHT)
        drawCenteredText(canvas, headerPaint, "ID", colX1, colX2, TABLE_TOP, TABLE_TOP + HEADER_HEIGHT)
        drawCenteredText(canvas, headerPaint, "কোড", colX2, colX3, TABLE_TOP, TABLE_TOP + HEADER_HEIGHT)
        drawCenteredText(canvas, headerPaint, "নাম", colX3, colX4, TABLE_TOP, TABLE_TOP + HEADER_HEIGHT)

        // 6. Draw Rows
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        for (i in records.indices) {
            val record = records[i]
            val rowTop = TABLE_TOP + HEADER_HEIGHT + (i * ROW_HEIGHT)
            val rowBottom = rowTop + ROW_HEIGHT

            // Row Bottom Divider
            canvas.drawLine(tableLeft, rowBottom, tableRight, rowBottom, strokePaint)

            val serialNumber = startIndexOffset + i + 1
            val serialBn = BengaliUtils.toBengaliDigits(serialNumber.toString())

            // 1) ক্রমিক (Bengali numerals, bold, centered)
            textPaint.textSize = 38f
            drawCenteredText(canvas, textPaint, serialBn, colX0, colX1, rowTop, rowBottom)

            // 2) ID (e.g. AB260948, bold, centered)
            textPaint.textSize = 37f
            drawCenteredText(canvas, textPaint, record.patientId.trim().uppercase(), colX1, colX2, rowTop, rowBottom)

            // 3) কোড (e.g. AF07, MD-01, J007 / (DUE), bold, centered, multi-line support)
            textPaint.textSize = 35f
            drawMultilineCenteredText(
                canvas = canvas,
                paint = textPaint,
                rawText = record.code.trim().uppercase(),
                left = colX2,
                right = colX3,
                top = rowTop,
                bottom = rowBottom,
                isCode = true
            )

            // 4) নাম (Patient Name, e.g. TUHIN ALI, MD ALIM / HAQUE, centered, smart 2-line wrap)
            textPaint.textSize = 34f
            val pName = record.patientName.trim().uppercase()
            if (pName.isNotBlank()) {
                drawMultilineCenteredText(
                    canvas = canvas,
                    paint = textPaint,
                    rawText = pName,
                    left = colX3,
                    right = colX4,
                    top = rowTop,
                    bottom = rowBottom,
                    isCode = false
                )
            }
        }
    }

    /**
     * Draws single-line text perfectly centered both horizontally and vertically.
     */
    private fun drawCenteredText(
        canvas: Canvas,
        paint: Paint,
        text: String,
        left: Float,
        right: Float,
        top: Float,
        bottom: Float
    ) {
        val textWidth = paint.measureText(text)
        val centerX = left + (right - left - textWidth) / 2f

        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        val centerY = top + (bottom - top) / 2f + bounds.height() / 2f - bounds.bottom

        canvas.drawText(text, centerX, centerY, paint)
    }

    /**
     * Smartly wraps and centers text in 1 or 2 lines.
     * Matches the sample photo where:
     * - "MD ALIM HAQUE" wraps to "MD ALIM" / "HAQUE"
     * - "MD SHAHID HOSSAIN" wraps to "MD SHAHID" / "HOSSAIN"
     * - "J007 (DUE)" wraps to "J007" / "(DUE)"
     */
    private fun drawMultilineCenteredText(
        canvas: Canvas,
        paint: Paint,
        rawText: String,
        left: Float,
        right: Float,
        top: Float,
        bottom: Float,
        isCode: Boolean
    ) {
        val availableWidth = (right - left) - 20f

        // Determine lines
        val lines: List<String> = when {
            // Already contains newline
            rawText.contains("\n") -> {
                rawText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
            }
            // Code with parentheses, e.g. "J007 (DUE)"
            isCode && rawText.contains("(") -> {
                val openParenIdx = rawText.indexOf("(")
                val part1 = rawText.substring(0, openParenIdx).trim()
                val part2 = rawText.substring(openParenIdx).trim()
                listOf(part1, part2)
            }
            // Single line fits comfortably
            paint.measureText(rawText) <= availableWidth -> {
                listOf(rawText)
            }
            // Multi-word name or text that needs smart wrapping into 2 lines
            else -> {
                val words = rawText.split(Regex("\\s+")).filter { it.isNotEmpty() }
                if (words.size >= 2) {
                    val half = if (words.size == 3) 2 else (words.size + 1) / 2
                    val line1 = words.take(half).joinToString(" ")
                    val line2 = words.drop(half).joinToString(" ")
                    listOf(line1, line2)
                } else {
                    listOf(rawText)
                }
            }
        }

        val lineCount = lines.size
        if (lineCount == 1) {
            // Auto scale font size down if a single word is too long
            val measured = paint.measureText(lines[0])
            if (measured > availableWidth) {
                val originalSize = paint.textSize
                paint.textSize = originalSize * (availableWidth / measured)
                drawCenteredText(canvas, paint, lines[0], left, right, top, bottom)
                paint.textSize = originalSize
            } else {
                drawCenteredText(canvas, paint, lines[0], left, right, top, bottom)
            }
            return
        }

        // Draw 2 centered lines
        val lineSpacing = 40f
        val bounds = Rect()
        paint.getTextBounds("A", 0, 1, bounds)
        val charHeight = bounds.height()

        val totalBlockHeight = (lineCount - 1) * lineSpacing + charHeight
        var currentY = top + (bottom - top - totalBlockHeight) / 2f + charHeight

        for (line in lines) {
            val measured = paint.measureText(line)
            val scale = if (measured > availableWidth) availableWidth / measured else 1f
            val originalSize = paint.textSize
            if (scale < 1f) {
                paint.textSize = originalSize * scale
            }

            val textWidth = paint.measureText(line)
            val centerX = left + (right - left - textWidth) / 2f
            canvas.drawText(line, centerX, currentY, paint)

            if (scale < 1f) {
                paint.textSize = originalSize
            }
            currentY += lineSpacing
        }
    }

    /**
     * Generates a multi-page PDF document for printing or sharing.
     * Uses standard A4 (595 x 842 points) with exact scaling from the 1080 design.
     */
    fun createDailyReportPdfDocument(
        dateStr: String,
        records: List<MedicalRecordEntity>
    ): PdfDocument {
        val pdfDocument = PdfDocument()
        val a4Width = 595
        val a4Height = 842

        // Scale factor from 1080 canvas to 595 A4 width
        val scale = a4Width.toFloat() / CANVAS_WIDTH
        val virtualHeight = a4Height.toFloat() / scale // ~1528.5f

        // Usable rows per page
        val usableHeight = virtualHeight - TABLE_TOP - HEADER_HEIGHT - 100f
        val rowsPerPage = maxOf(1, (usableHeight / ROW_HEIGHT).toInt()) // ~9 rows per A4 page

        val pageCount = if (records.isEmpty()) 1 else (records.size + rowsPerPage - 1) / rowsPerPage

        for (pageIdx in 0 until pageCount) {
            val pageInfo = PdfDocument.PageInfo.Builder(a4Width, a4Height, pageIdx + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            canvas.drawColor(Color.WHITE)

            val startIdx = pageIdx * rowsPerPage
            val endIdx = minOf(records.size, (pageIdx + 1) * rowsPerPage)
            val pageRecords = if (startIdx < records.size) records.subList(startIdx, endIdx) else emptyList()

            canvas.save()
            canvas.scale(scale, scale)

            drawSinglePageDailyReport(
                canvas = canvas,
                dateStr = dateStr,
                records = pageRecords,
                startIndexOffset = startIdx,
                canvasWidth = CANVAS_WIDTH,
                canvasHeight = virtualHeight
            )

            canvas.restore()
            pdfDocument.finishPage(page)
        }

        return pdfDocument
    }

    /**
     * Prints the daily medical report using Android's PrintManager.
     * Sends the pre-rendered standard A4 document directly to the printer.
     */
    fun printDailyReport(
        context: Context,
        dateStr: String,
        records: List<MedicalRecordEntity>
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val cleanDate = dateStr.replace("/", "-").replace("-", "_")
        val jobName = "Medical_Report_$cleanDate"

        val printAdapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }

                val rowsPerPage = 9
                val pageCount = if (records.isEmpty()) 1 else (records.size + rowsPerPage - 1) / rowsPerPage

                val info = PrintDocumentInfo.Builder("$jobName.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(pageCount)
                    .build()

                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                if (destination == null) return
                val pdfDocument = createDailyReportPdfDocument(dateStr, records)
                try {
                    FileOutputStream(destination.fileDescriptor).use { out ->
                        pdfDocument.writeTo(out)
                    }
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                } finally {
                    pdfDocument.close()
                }
            }
        }

        val builder = PrintAttributes.Builder()
        builder.setMediaSize(PrintAttributes.MediaSize.ISO_A4.asPortrait())
        builder.setMinMargins(PrintAttributes.Margins(0, 0, 0, 0))

        printManager.print(jobName, printAdapter, builder.build())
    }

    /**
     * Shares the daily report as a PNG image matching the sample photo.
     */
    fun shareDailyReportAsImage(
        context: Context,
        dateStr: String,
        records: List<MedicalRecordEntity>
    ) {
        try {
            val bitmap = renderDailyReportBitmap(dateStr, records)
            val cleanDate = dateStr.replace("/", "_").replace("-", "_")
            shareBitmapImage(context, bitmap, "Medical_Report_$cleanDate")
        } catch (e: Exception) {
            Toast.makeText(context, "ছবি তৈরি করতে সমস্যা হয়েছে: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Shares the daily report as a PDF document.
     */
    fun shareDailyReportAsPdf(
        context: Context,
        dateStr: String,
        records: List<MedicalRecordEntity>
    ) {
        try {
            val pdfDocument = createDailyReportPdfDocument(dateStr, records)
            val cleanDate = dateStr.replace("/", "_").replace("-", "_")
            val cacheDir = File(context.cacheDir, "documents").apply { mkdirs() }
            val file = File(cacheDir, "Medical_Report_$cleanDate.pdf")
            if (file.exists()) file.delete()

            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "Medical Report - $cleanDate")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Medical PDF Report"))
        } catch (e: Exception) {
            Toast.makeText(context, "PDF তৈরি করতে সমস্যা হয়েছে: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Copies the table data formatted as clean text to clipboard.
     */
    fun copyTableAsText(
        context: Context,
        dateStr: String,
        records: List<MedicalRecordEntity>
    ) {
        val sb = java.lang.StringBuilder()
        sb.append("তারিখ: ").append(formatDateShort(dateStr)).append("\n\n")
        sb.append("ক্রমিক\tID\t\tকোড\t\tনাম\n")
        sb.append("------------------------------------------\n")
        for (i in records.indices) {
            val r = records[i]
            val sBn = BengaliUtils.toBengaliDigits((i + 1).toString())
            sb.append(sBn).append("\t")
                .append(r.patientId.uppercase()).append("\t\t")
                .append(r.code.uppercase()).append("\t\t")
                .append(r.patientName.uppercase()).append("\n")
        }
        sb.append("------------------------------------------\n")
        sb.append("মোট এন্ট্রি: ").append(BengaliUtils.toBengaliDigits(records.size.toString())).append(" টি\n")

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("Medical Work Report", sb.toString())
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(context, "রিপোর্ট ক্লিপবোর্ডে কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
    }

    private fun shareBitmapImage(context: Context, bitmap: Bitmap, filename: String) {
        try {
            val cacheDir = File(context.cacheDir, "images").apply { mkdirs() }
            val file = File(cacheDir, "$filename.png")
            if (file.exists()) file.delete()

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "Medical Report Image")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Medical Report Image"))
        } catch (e: Exception) {
            Toast.makeText(context, "ছবি শেয়ার করা যায়নি: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}

