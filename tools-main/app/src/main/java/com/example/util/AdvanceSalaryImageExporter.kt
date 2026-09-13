package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.ui.AdvanceSalaryFormState
import java.io.File
import java.io.FileOutputStream

object AdvanceSalaryImageExporter {

    fun generateAdvanceSalaryBitmap(
        state: AdvanceSalaryFormState
    ): Bitmap {
        val imageWidth = 850
        val imageHeight = 1160

        val bitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Pure white background
        canvas.drawColor(Color.WHITE)

        val margin = 26f
        val innerMargin = 22f
        val tableLeft = margin + innerMargin
        val tableRight = imageWidth - margin - innerMargin
        val centerX = imageWidth / 2f

        // Paints
        val outerBorderPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        val solidBorderPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 1.8f
        }
        val dashedLinePaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 1.6f
            pathEffect = android.graphics.DashPathEffect(floatArrayOf(7f, 5f), 0f)
        }
        val badgeBgPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            style = Paint.Style.FILL
        }
        val badgeTextPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = 17f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val titleTextPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 25f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val dateTextPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val cellLabelPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 17.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val cellColonPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 17.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val cellValuePaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 17.5f
            typeface = Typeface.DEFAULT
        }
        val undertakingPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 16f
            typeface = Typeface.DEFAULT
        }
        val sigLinePaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            strokeWidth = 1.8f
        }
        val sigTextPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 17f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        // 1. Outer Border Frame
        val outerRect = RectF(margin, margin, imageWidth - margin, imageHeight - margin)
        canvas.drawRect(outerRect, outerBorderPaint)

        // 2. Centered Main Title
        var curY = margin + 55f
        canvas.drawText("ADVANCE SALARY REQUISITION VOUCHER", centerX, curY, titleTextPaint)

        // 3. Date Row (Right Aligned)
        curY += 46f
        val dateDisplay = state.dateString.ifBlank { "01/09/2026" }
        canvas.drawText("Date: $dateDisplay", tableRight, curY, dateTextPaint)

        // 4. Section 1 Badge: APPLICANT / EMPLOYEE DETAILS
        curY += 24f
        val badgeH = 36f
        val badge1Width = 330f
        val badge1Rect = RectF(tableLeft, curY, tableLeft + badge1Width, curY + badgeH)
        canvas.drawRect(badge1Rect, badgeBgPaint)
        canvas.drawText("APPLICANT / EMPLOYEE DETAILS", tableLeft + 14f, curY + 25f, badgeTextPaint)

        // 5. Section 1 Table (3 Rows default or 5 Rows if Previous Advance exists)
        val hasPrevAdvance = state.previousAdvancePending > 0.0
        curY += badgeH
        val sec1TableTop = curY
        val sec1RowH = if (hasPrevAdvance) 45f else 52f
        val sec1RowCount = if (hasPrevAdvance) 5 else 3
        val sec1TableH = sec1RowH * sec1RowCount.toFloat()
        val sec1TableRect = RectF(tableLeft, sec1TableTop, tableRight, sec1TableTop + sec1TableH)
        canvas.drawRect(sec1TableRect, solidBorderPaint)

        val col1DividerX = tableLeft + 220f
        val colColonX = tableLeft + 242f
        val colValueX = tableLeft + 265f

        // Vertical divider
        canvas.drawLine(col1DividerX, sec1TableTop, col1DividerX, sec1TableTop + sec1TableH, solidBorderPaint)

        // Row 1: Name
        val r1Y = sec1TableTop + (sec1RowH * 0.68f)
        canvas.drawText("Name", tableLeft + 14f, r1Y, cellLabelPaint)
        canvas.drawText(":", colColonX, r1Y, cellColonPaint)
        canvas.drawText(state.applicantName.ifBlank { "Nahid" }, colValueX, r1Y, cellValuePaint)
        canvas.drawLine(tableLeft, sec1TableTop + sec1RowH, tableRight, sec1TableTop + sec1RowH, solidBorderPaint)

        // Row 2: Designation
        val r2Y = sec1TableTop + sec1RowH + (sec1RowH * 0.68f)
        canvas.drawText("Designation", tableLeft + 14f, r2Y, cellLabelPaint)
        canvas.drawText(":", colColonX, r2Y, cellColonPaint)
        canvas.drawText(state.designation.ifBlank { "Office Assistant" }, colValueX, r2Y, cellValuePaint)
        canvas.drawLine(tableLeft, sec1TableTop + (sec1RowH * 2f), tableRight, sec1TableTop + (sec1RowH * 2f), solidBorderPaint)

        // Row 3: Basic Salary
        val r3Y = sec1TableTop + (sec1RowH * 2f) + (sec1RowH * 0.68f)
        canvas.drawText("Basic Salary", tableLeft + 14f, r3Y, cellLabelPaint)
        canvas.drawText(":", colColonX, r3Y, cellColonPaint)
        val salaryStr = if (state.monthlySalary > 0) "Tk. ${EnglishUtils.formatEnglishCurrency(state.monthlySalary)}/-" else "Tk. 11,000/-"
        canvas.drawText(salaryStr, colValueX, r3Y, cellValuePaint)

        if (hasPrevAdvance) {
            canvas.drawLine(tableLeft, sec1TableTop + (sec1RowH * 3f), tableRight, sec1TableTop + (sec1RowH * 3f), solidBorderPaint)

            // Row 4: Previous Advance Taken
            val r4Y = sec1TableTop + (sec1RowH * 3f) + (sec1RowH * 0.68f)
            canvas.drawText("Prev. Advance Taken", tableLeft + 14f, r4Y, cellLabelPaint)
            canvas.drawText(":", colColonX, r4Y, cellColonPaint)
            val prevStr = "Tk. ${EnglishUtils.formatEnglishCurrency(state.previousAdvancePending)}/-"
            canvas.drawText(prevStr, colValueX, r4Y, cellValuePaint)
            canvas.drawLine(tableLeft, sec1TableTop + (sec1RowH * 4f), tableRight, sec1TableTop + (sec1RowH * 4f), solidBorderPaint)

            // Row 5: Net Available Salary
            val r5Y = sec1TableTop + (sec1RowH * 4f) + (sec1RowH * 0.68f)
            canvas.drawText("Net Eligible Limit", tableLeft + 14f, r5Y, cellLabelPaint)
            canvas.drawText(":", colColonX, r5Y, cellColonPaint)
            val netEligible = (state.monthlySalary - state.previousAdvancePending).coerceAtLeast(0.0)
            val netStr = "Tk. ${EnglishUtils.formatEnglishCurrency(netEligible)}/-"
            canvas.drawText(netStr, colValueX, r5Y, cellValuePaint)
        }

        // 6. Section 2 Badge: ADVANCE & REPAYMENT TERMS
        curY = sec1TableTop + sec1TableH + (if (hasPrevAdvance) 26f else 34f)
        val badge2Width = 330f
        val badge2Rect = RectF(tableLeft, curY, tableLeft + badge2Width, curY + badgeH)
        canvas.drawRect(badge2Rect, badgeBgPaint)
        canvas.drawText("ADVANCE & REPAYMENT TERMS", tableLeft + 14f, curY + 25f, badgeTextPaint)

        // 7. Section 2 Table (6 Rows with Dashed Inner Dividers)
        curY += badgeH
        val sec2TableTop = curY
        val sec2RowH = if (hasPrevAdvance) 45f else 50f
        val sec2RowCount = 6
        val sec2TableH = sec2RowH * sec2RowCount.toFloat()
        val sec2TableRect = RectF(tableLeft, sec2TableTop, tableRight, sec2TableTop + sec2TableH)
        canvas.drawRect(sec2TableRect, solidBorderPaint)

        // Vertical divider
        canvas.drawLine(col1DividerX, sec2TableTop, col1DividerX, sec2TableTop + sec2TableH, solidBorderPaint)

        // Row 1: Advance Amount
        val s2r1Y = sec2TableTop + (sec2RowH * 0.68f)
        canvas.drawText("Advance Amount", tableLeft + 14f, s2r1Y, cellLabelPaint)
        canvas.drawText(":", colColonX, s2r1Y, cellColonPaint)
        val advAmtStr = if (state.advanceAmount > 0) "Tk. ${EnglishUtils.formatEnglishCurrency(state.advanceAmount)}/-" else "Tk. 5,000/-"
        canvas.drawText(advAmtStr, colValueX, s2r1Y, cellValuePaint)
        canvas.drawLine(tableLeft, sec2TableTop + sec2RowH, tableRight, sec2TableTop + sec2RowH, dashedLinePaint)

        // Row 2: In Words
        val s2r2Y = sec2TableTop + sec2RowH + (sec2RowH * 0.68f)
        canvas.drawText("In Words", tableLeft + 14f, s2r2Y, cellLabelPaint)
        canvas.drawText(":", colColonX, s2r2Y, cellColonPaint)
        val inWordsText = state.advanceAmountInWords.ifBlank {
            if (state.advanceAmount > 0) EnglishUtils.amountToEnglishWords(state.advanceAmount) else "Five Thousand Taka Only"
        }
        canvas.drawText(inWordsText, colValueX, s2r2Y, cellValuePaint)
        canvas.drawLine(tableLeft, sec2TableTop + (sec2RowH * 2f), tableRight, sec2TableTop + (sec2RowH * 2f), dashedLinePaint)

        // Row 3: Remaining Salary
        val s2r3Y = sec2TableTop + (sec2RowH * 2f) + (sec2RowH * 0.68f)
        canvas.drawText("Remaining Salary", tableLeft + 14f, s2r3Y, cellLabelPaint)
        canvas.drawText(":", colColonX, s2r3Y, cellColonPaint)
        val remainingSalary = if (state.monthlySalary > 0) {
            (state.monthlySalary - state.previousAdvancePending - state.advanceAmount).coerceAtLeast(0.0)
        } else 0.0
        val remainingStr = if (state.monthlySalary > 0) "Tk. ${EnglishUtils.formatEnglishCurrency(remainingSalary)}/-" else "Tk. 0/-"
        canvas.drawText(remainingStr, colValueX, s2r3Y, cellValuePaint)
        canvas.drawLine(tableLeft, sec2TableTop + (sec2RowH * 3f), tableRight, sec2TableTop + (sec2RowH * 3f), dashedLinePaint)

        // Row 4: Reason
        val s2r4Y = sec2TableTop + (sec2RowH * 3f) + (sec2RowH * 0.68f)
        canvas.drawText("Reason", tableLeft + 14f, s2r4Y, cellLabelPaint)
        canvas.drawText(":", colColonX, s2r4Y, cellColonPaint)
        val reasonText = state.reason.ifBlank { "Personal Emergency" }
        canvas.drawText(reasonText, colValueX, s2r4Y, cellValuePaint)
        canvas.drawLine(tableLeft, sec2TableTop + (sec2RowH * 4f), tableRight, sec2TableTop + (sec2RowH * 4f), dashedLinePaint)

        // Row 5: Repayment
        val s2r5Y = sec2TableTop + (sec2RowH * 4f) + (sec2RowH * 0.68f)
        canvas.drawText("Repayment", tableLeft + 14f, s2r5Y, cellLabelPaint)
        canvas.drawText(":", colColonX, s2r5Y, cellColonPaint)
        val repayStr = if (state.repaymentType == "installments") {
            "Monthly Installments: ${state.installmentCount} Months (Tk. ${EnglishUtils.formatEnglishCurrency(state.installmentAmountPerMonth)}/mo)"
        } else {
            "One-time full deduction from salary"
        }
        canvas.drawText(repayStr, colValueX, s2r5Y, cellValuePaint)
        canvas.drawLine(tableLeft, sec2TableTop + (sec2RowH * 5f), tableRight, sec2TableTop + (sec2RowH * 5f), dashedLinePaint)

        // Row 6: Deduction Starts
        val s2r6Y = sec2TableTop + (sec2RowH * 5f) + (sec2RowH * 0.68f)
        canvas.drawText("Deduction Starts", tableLeft + 14f, s2r6Y, cellLabelPaint)
        canvas.drawText(":", colColonX, s2r6Y, cellColonPaint)
        val dedStart = state.deductionStartMonth.ifBlank { "Next Month" }
        canvas.drawText(dedStart, colValueX, s2r6Y, cellValuePaint)

        // 8. Declaration Card Box with pill badge & connector lines
        curY = sec2TableTop + sec2TableH + (if (hasPrevAdvance) 28f else 36f)
        val declBoxTop = curY
        val declBoxH = if (hasPrevAdvance) 92f else 102f
        val declBoxRect = RectF(tableLeft, declBoxTop, tableRight, declBoxTop + declBoxH)
        canvas.drawRoundRect(declBoxRect, 16f, 16f, solidBorderPaint)

        // Badge centered on top line of declaration box
        val badgeDeclW = 180f
        val badgeDeclH = 34f
        val badgeDeclRect = RectF(centerX - (badgeDeclW / 2f), declBoxTop - (badgeDeclH / 2f), centerX + (badgeDeclW / 2f), declBoxTop + (badgeDeclH / 2f))

        // White mask behind badge and side lines
        val maskRect = RectF(centerX - 165f, declBoxTop - (badgeDeclH / 2f) - 2f, centerX + 165f, declBoxTop + (badgeDeclH / 2f) + 2f)
        val whiteBgPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(maskRect, whiteBgPaint)

        // Side connector lines and bullets: ●—— DECLARATION ——●
        val connectorPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            strokeWidth = 1.8f
            style = Paint.Style.STROKE
        }
        val dotPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            style = Paint.Style.FILL
        }
        // Left line & dot
        canvas.drawLine(centerX - 155f, declBoxTop, centerX - (badgeDeclW / 2f) - 8f, declBoxTop, connectorPaint)
        canvas.drawCircle(centerX - 155f, declBoxTop, 4.5f, dotPaint)

        // Right line & dot
        canvas.drawLine(centerX + (badgeDeclW / 2f) + 8f, declBoxTop, centerX + 155f, declBoxTop, connectorPaint)
        canvas.drawCircle(centerX + 155f, declBoxTop, 4.5f, dotPaint)

        // Draw black pill badge
        canvas.drawRoundRect(badgeDeclRect, 8f, 8f, badgeBgPaint)
        val badgeCenterTextPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = 17f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("DECLARATION", centerX, declBoxTop + 6.5f, badgeCenterTextPaint)

        // Inside declaration text (italic/regular, centered)
        val declTextPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        val line1Y = declBoxTop + 43f
        val line2Y = declBoxTop + 71f
        canvas.drawText("I hereby authorize the management to deduct the stated advance amount", centerX, line1Y, declTextPaint)
        canvas.drawText("from my monthly salary in accordance with the terms mentioned above.", centerX, line2Y, declTextPaint)

        // 9. Signatures Row
        if (state.showSignatures) {
            val sigLineY = imageHeight - margin - 75f
            val sigLineWidth = 220f

            // Left: Applicant's Signature
            val sigLeftStart = tableLeft + 8f
            val sigLeftEnd = sigLeftStart + sigLineWidth
            val sigLeftCenter = (sigLeftStart + sigLeftEnd) / 2f
            canvas.drawLine(sigLeftStart, sigLineY, sigLeftEnd, sigLineY, sigLinePaint)
            canvas.drawText("Applicant's Signature", sigLeftCenter, sigLineY + 28f, sigTextPaint)

            // Right: Authorized Signature
            val sigRightEnd = tableRight - 8f
            val sigRightStart = sigRightEnd - sigLineWidth
            val sigRightCenter = (sigRightStart + sigRightEnd) / 2f
            canvas.drawLine(sigRightStart, sigLineY, sigRightEnd, sigLineY, sigLinePaint)
            canvas.drawText("Authorized Signature", sigRightCenter, sigLineY + 28f, sigTextPaint)
        }

        return bitmap
    }

    fun saveAdvanceSalaryImageToGallery(context: Context, state: AdvanceSalaryFormState): Boolean {
        return try {
            val bitmap = generateAdvanceSalaryBitmap(state)
            val empLabel = state.applicantName.replace(" ", "_").ifBlank { "Employee" }
            val dateLabel = state.dateString.replace("/", "-").ifBlank { "Advance" }
            val filename = "Advance_Salary_${empLabel}_$dateLabel.png"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AdvanceSalary")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return false

                resolver.openOutputStream(uri)?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }

                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "AdvanceSalary")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, filename)
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("image/png"), null)
            }

            Toast.makeText(context, "Image saved to gallery", Toast.LENGTH_SHORT).show()
            true
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to save image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun shareAdvanceSalaryImage(context: Context, state: AdvanceSalaryFormState) {
        try {
            val bitmap = generateAdvanceSalaryBitmap(state)
            val empLabel = state.applicantName.replace(" ", "_").ifBlank { "Employee" }
            val dateLabel = state.dateString.replace("/", "-").ifBlank { "Advance" }
            val cacheDir = File(context.cacheDir, "advance_salaries").apply { mkdirs() }
            val file = File(cacheDir, "Advance_Salary_${empLabel}_$dateLabel.png")
            if (file.exists()) file.delete()

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, file)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Advance Salary Application - ${state.applicantName}")
                putExtra(Intent.EXTRA_TEXT, "Advance Salary Application for ${state.applicantName} (${state.dateString})")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, "Share Advance Salary Image"))
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to share image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
