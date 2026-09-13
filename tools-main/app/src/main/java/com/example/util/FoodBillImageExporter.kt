package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat

object FoodBillImageExporter {

    /**
     * Renders a clean, vertical cash voucher / memo image matching the exact design and ratio:
     * - Top box with centered bold title & full-width underline
     * - Right-aligned Date ("তারিখ : ...")
     * - 14-row structured table with column lines, dashed row dividers, and formatted Sl (০১..১৪)
     * - Total row ("মোট –" and amount with "/-")
     * - Left-aligned Signature ("স্বাক্ষর") with horizontal line
     */
    fun generateMemoListBitmap(memo: PrintMemoData): Bitmap {
        val validItems = memo.items.filter { it.name.isNotBlank() || it.amount > 0 }
        val numRows = maxOf(14, validItems.size) // Exact 14 rows standard voucher ratio

        val imageWidth = 900
        val paddingX = 36f
        val paddingTop = 32f
        val paddingBottom = 45f
        val contentWidth = imageWidth - (paddingX * 2)

        // Heights & Spacing
        val headerBoxHeight = if (memo.subtitle.isNotBlank()) 92f else 76f
        val headerGap = 12f
        val lineGap = 14f
        val metaHeight = 36f
        val tableHeaderHeight = 46f
        val baseRowHeight = 44f
        val totalRowHeight = 48f
        val footerSpace = 40f
        val signatureHeight = if (memo.showSignature) 55f else 10f

        // Column widths are needed early now, so we can pre-measure how many
        // lines each item name will wrap onto and size each row accordingly
        // (instead of shrinking the font / letting text overflow the row).
        val colWidthsForMeasure = floatArrayOf(
            contentWidth * 0.105f,
            contentWidth * 0.425f,
            contentWidth * 0.150f,
            contentWidth * 0.130f,
            contentWidth * 0.190f
        )
        val itemFontSize = 21f
        val nameLineHeight = itemFontSize + 5f
        val nameVerticalPad = 8f
        val namePadLeft = 12f
        val namePadRight = 8f
        val maxNameWidth = colWidthsForMeasure[1] - namePadLeft - namePadRight
        val measurePaint = Paint().apply {
            isAntiAlias = true
            textSize = itemFontSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val rowNameLines = ArrayList<List<String>>(numRows)
        val rowHeights = FloatArray(numRows)
        for (r in 0 until numRows) {
            if (r < validItems.size) {
                val lines = CanvasTextUtils.wrapText(validItems[r].name, measurePaint, maxNameWidth)
                rowNameLines.add(lines)
                val neededHeight = (lines.size * nameLineHeight) + nameVerticalPad
                rowHeights[r] = maxOf(baseRowHeight, neededHeight)
            } else {
                rowNameLines.add(listOf(""))
                rowHeights[r] = baseRowHeight
            }
        }
        val tableHeight = tableHeaderHeight + rowHeights.sum() + totalRowHeight

        val totalHeight = (paddingTop + headerBoxHeight + headerGap + 2f + lineGap + metaHeight + 10f + tableHeight + footerSpace + signatureHeight + paddingBottom).toInt()

        val bitmap = Bitmap.createBitmap(imageWidth, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background (Pure Crisp White)
        val bgPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, imageWidth.toFloat(), totalHeight.toFloat(), bgPaint)

        var currentY = paddingTop
        val leftX = paddingX
        val rightX = imageWidth - paddingX
        val centerX = imageWidth / 2f

        // 1. Header Box (Top Rectangular Frame)
        val headerRect = RectF(leftX, currentY, rightX, currentY + headerBoxHeight)
        val headerBoxBorderPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        canvas.drawRect(headerRect, headerBoxBorderPaint)

        // Header Title
        val titleText = memo.centerName.ifBlank { "খাবার বিল ক্যাশ মেমো" }
        val titlePaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        if (memo.subtitle.isNotBlank()) {
            canvas.drawText(titleText, centerX, currentY + 44f, titlePaint)
            val subPaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(40, 40, 40)
                textSize = 20f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(memo.subtitle, centerX, currentY + 76f, subPaint)
        } else {
            canvas.drawText(titleText, centerX, currentY + 50f, titlePaint)
        }

        currentY += headerBoxHeight + headerGap

        // Full-width underline under header box
        val fullLinePaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            strokeWidth = 2.2f
        }
        canvas.drawLine(leftX, currentY, rightX, currentY, fullLinePaint)
        currentY += lineGap

        // 2. Date & Meta Info
        val metaBoldPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        // Left meta (Memo # or Purchaser if provided)
        if (memo.purchaserName.isNotBlank()) {
            metaBoldPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("ক্রেতা: ${memo.purchaserName}", leftX + 2f, currentY + 22f, metaBoldPaint)
        } else if (memo.memoId > 0) {
            metaBoldPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("মেমো নং: #${BengaliUtils.toBengaliDigits(memo.memoId.toString())}", leftX + 2f, currentY + 22f, metaBoldPaint)
        }

        // Right Date ("তারিখ : ১১/০৮/২০২৬")
        val bnDate = BengaliUtils.toBengaliDigits(memo.dateString)
        metaBoldPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("তারিখ : $bnDate", rightX - 2f, currentY + 22f, metaBoldPaint)

        currentY += metaHeight + 10f

        // 3. Table Structure & Columns
        // Sl: 10.5%, Name: 42.5%, Qty: 15%, Rate: 13%, Amount: 19%
        val colWidths = floatArrayOf(
            contentWidth * 0.105f, // Sl (10.5%)
            contentWidth * 0.425f, // Description (42.5%)
            contentWidth * 0.150f, // Quantity (15%)
            contentWidth * 0.130f, // Rate (13%)
            contentWidth * 0.190f  // Amount (19%)
        )

        val tableTop = currentY
        val tableBottom = tableTop + tableHeight
        val tableBorderPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }

        // Table Header Row
        val thRect = RectF(leftX, tableTop, rightX, tableTop + tableHeaderHeight)
        canvas.drawRect(thRect, tableBorderPaint)

        val thTextPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val colTitles = if (memo.billType == "transport") {
            arrayOf("ক্র. নং", "বিবরণ / রুট", "মাধ্যম", "যাত্রী", "টাকা")
        } else {
            arrayOf("ক্র. নং", "খাবারের নাম / বিবরণ", "পরিমাণ", "দর", "টাকা")
        }
        var curColX = leftX
        for (i in 0 until 5) {
            val colCenterX = curColX + colWidths[i] / 2f
            canvas.drawText(colTitles[i], colCenterX, tableTop + 31f, thTextPaint)
            curColX += colWidths[i]
        }

        // Table Rows
        val rowDashPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            strokeWidth = 1.5f
            pathEffect = DashPathEffect(floatArrayOf(6f, 4f), 0f)
        }
        val itemTextPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val itemRegularPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        var rowY = tableTop + tableHeaderHeight
        for (r in 0 until numRows) {
            val thisRowHeight = rowHeights[r]
            val slNo = BengaliUtils.toBengaliDigits(String.format("%02d", r + 1))
            // Vertically centered baseline for the single-line columns (Sl/Qty/Rate/Amount)
            val centerTextY = rowY + (thisRowHeight / 2f) + (itemFontSize * 0.35f)

            // 1. Serial Number (০১, ০২...) - Bold & Centered
            itemTextPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(slNo, leftX + colWidths[0] / 2f, centerTextY, itemTextPaint)

            if (r < validItems.size) {
                val item = validItems[r]

                // 2. Name / Description - Left aligned, wraps onto multiple lines
                // (pre-measured above) instead of shrinking font or overflowing the row.
                itemTextPaint.textAlign = Paint.Align.LEFT
                val lines = rowNameLines[r]
                val firstLineY = rowY + nameVerticalPad / 2f + itemFontSize
                for ((i, line) in lines.withIndex()) {
                    canvas.drawText(line, leftX + colWidths[0] + namePadLeft, firstLineY + (i * nameLineHeight), itemTextPaint)
                }

                // 3. Quantity - Centered (e.g. "২ কেজি", "১ পোয়া", "১৪ পিস")
                itemRegularPaint.textAlign = Paint.Align.CENTER
                val bnQty = BengaliUtils.toBengaliDigits(item.quantity)
                if (bnQty.isNotBlank()) {
                    canvas.drawText(bnQty, leftX + colWidths[0] + colWidths[1] + colWidths[2] / 2f, centerTextY, itemRegularPaint)
                }

                // 4. Rate - Centered / Blank
                val bnRate = if (item.rate == "0" || item.rate.isBlank()) "" else BengaliUtils.toBengaliDigits(item.rate)
                if (bnRate.isNotBlank()) {
                    itemRegularPaint.textAlign = Paint.Align.CENTER
                    canvas.drawText(bnRate, leftX + colWidths[0] + colWidths[1] + colWidths[2] + colWidths[3] / 2f, centerTextY, itemRegularPaint)
                }

                // 5. Amount - Right aligned with "/-" or "—"
                itemTextPaint.textAlign = Paint.Align.RIGHT
                val bnAmount = if (item.amount <= 0) "—" else "${BengaliUtils.toBengaliDigits(DecimalFormat("#,##0").format(item.amount))}/-"
                canvas.drawText(bnAmount, rightX - 14f, centerTextY, itemTextPaint)
            } else {
                // Empty row amount dash
                itemTextPaint.textAlign = Paint.Align.RIGHT
                // canvas.drawText("—", rightX - 14f, centerTextY, itemTextPaint)
            }

            // Draw dashed row bottom separator
            canvas.drawLine(leftX, rowY + thisRowHeight, rightX, rowY + thisRowHeight, rowDashPaint)
            rowY += thisRowHeight
        }

        // Total Row (At the bottom of the table)
        val totalRowY = rowY
        canvas.drawLine(leftX, totalRowY, rightX, totalRowY, tableBorderPaint)

        // "মোট –" label in the Rate column
        val totalLabelPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val totalColCenterX = leftX + colWidths[0] + colWidths[1] + colWidths[2] + colWidths[3] / 2f
        canvas.drawText("মোট –", totalColCenterX, totalRowY + 33f, totalLabelPaint)

        // Total Amount Value in the Amount column
        val totalValPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 23f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val bnTotal = if (memo.totalAmount <= 0) "০/-" else "${BengaliUtils.formatBengaliCurrency(memo.totalAmount)}/-"
        canvas.drawText(bnTotal, rightX - 14f, totalRowY + 33f, totalValPaint)

        // Draw Outer table boundary and vertical column dividing lines
        canvas.drawRect(leftX, tableTop, rightX, tableBottom, tableBorderPaint)

        var divX = leftX
        for (i in 0 until 4) {
            divX += colWidths[i]
            canvas.drawLine(divX, tableTop, divX, tableBottom, tableBorderPaint)
        }

        currentY = tableBottom + footerSpace

        // 4. Footer: Left Signature ("স্বাক্ষর") — skipped entirely if the user disabled it
        if (memo.showSignature) {
            val sigLineWidth = 280f
            val sigLinePaint = Paint().apply {
                isAntiAlias = true
                color = Color.BLACK
                strokeWidth = 2f
            }
            val sigTextPaint = Paint().apply {
                isAntiAlias = true
                color = Color.BLACK
                textSize = 21f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }

            // Left Signature line & label
            val sigStartX = leftX
            val sigEndX = leftX + sigLineWidth
            canvas.drawLine(sigStartX, currentY + 10f, sigEndX, currentY + 10f, sigLinePaint)
            val sigLabel = memo.purchaserLabel.ifBlank { "স্বাক্ষর" }
            canvas.drawText(sigLabel, sigStartX + (sigLineWidth / 2f), currentY + 36f, sigTextPaint)
        }

        return bitmap
    }

    /**
     * Saves the generated food bill memo image directly to the device Gallery / Pictures folder.
     */
    fun saveMemoImageToGallery(context: Context, memo: PrintMemoData): Boolean {
        return try {
            val bitmap = generateMemoListBitmap(memo)
            val dateLabel = memo.dateString.replace("/", "-").ifBlank { "Bill" }
            val fileName = "Food_Bill_Memo_${dateLabel}_${System.currentTimeMillis()}.png"

            val success = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/FoodBills")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    context.contentResolver.update(uri, contentValues, null, null)
                    true
                } else {
                    false
                }
            } else {
                @Suppress("DEPRECATION")
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val foodBillsDir = File(picturesDir, "FoodBills").apply { mkdirs() }
                val imageFile = File(foodBillsDir, fileName)
                FileOutputStream(imageFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                MediaScannerConnection.scanFile(context, arrayOf(imageFile.absolutePath), arrayOf("image/png"), null)
                true
            }

            if (success) {
                Toast.makeText(context, "মেমোর ছবি গ্যালারিতে (Pictures/FoodBills) সেভ হয়েছে!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "ছবি সেভ করতে ব্যর্থ হয়েছে।", Toast.LENGTH_SHORT).show()
            }
            success
        } catch (e: Exception) {
            Toast.makeText(context, "ত্রুটি: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    /**
     * Shares the food bill memo as a PNG image via Android share sheet.
     */
    fun shareMemoImage(context: Context, memo: PrintMemoData) {
        try {
            val bitmap = generateMemoListBitmap(memo)
            val dateLabel = memo.dateString.replace("/", "-").ifBlank { "Bill" }
            val cacheDir = File(context.cacheDir, "food_bill_images").apply { mkdirs() }
            val imageFile = File(cacheDir, "Food_Bill_Memo_$dateLabel.png")
            if (imageFile.exists()) imageFile.delete()

            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val authority = "${context.packageName}.fileprovider"
            val contentUri: Uri = FileProvider.getUriForFile(context, authority, imageFile)
            val displayName = memo.centerName.ifBlank { "খাবার বিল মেমো" }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "খাবার বিল মেমো - ${memo.dateString}")
                putExtra(Intent.EXTRA_TEXT, "$displayName - ${memo.dateString} তারিখের খাবার বিল মেমোর ছবি।")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "মেমোর ছবি শেয়ার করুন (WhatsApp / মেসেঞ্জার)")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "ছবি শেয়ার করতে ব্যর্থ: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
