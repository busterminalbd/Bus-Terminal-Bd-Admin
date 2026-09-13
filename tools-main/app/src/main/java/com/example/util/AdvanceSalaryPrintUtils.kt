package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
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
import com.example.ui.AdvanceSalaryFormState
import java.io.File
import java.io.FileOutputStream

object AdvanceSalaryPrintUtils {

    fun printAdvanceSalary(
        context: Context,
        topState: AdvanceSalaryFormState,
        bottomState: AdvanceSalaryFormState? = null,
        position: PrintPosition = PrintPosition.TOP
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        val dateLabel = topState.dateString.replace("/", "-").ifBlank { "Advance" }
        val empLabel = topState.applicantName.replace(" ", "_").ifBlank { "Employee" }
        val jobName = "Advance_Salary_${empLabel}_$dateLabel"

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
                val info = PrintDocumentInfo.Builder("$jobName.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
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
                val pdfDocument = createAdvanceSalaryPdfDocument(
                    topState = topState,
                    bottomState = bottomState ?: topState,
                    position = position
                )
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

        printManager?.print(jobName, printAdapter, builder.build())
    }

    fun shareAdvanceSalaryPdf(
        context: Context,
        topState: AdvanceSalaryFormState,
        bottomState: AdvanceSalaryFormState? = null,
        position: PrintPosition = PrintPosition.TOP
    ) {
        try {
            val pdfDocument = createAdvanceSalaryPdfDocument(
                topState = topState,
                bottomState = bottomState ?: topState,
                position = position
            )

            val dateLabel = topState.dateString.replace("/", "-").ifBlank { "Advance" }
            val empLabel = topState.applicantName.replace(" ", "_").ifBlank { "Employee" }
            val cacheDir = File(context.cacheDir, "advance_salaries").apply { mkdirs() }
            val pdfFile = File(cacheDir, "Advance_Salary_${empLabel}_$dateLabel.pdf")
            if (pdfFile.exists()) pdfFile.delete()

            FileOutputStream(pdfFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, pdfFile)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Advance Salary Application - ${topState.applicantName}")
                putExtra(Intent.EXTRA_TEXT, "Advance Salary Application for ${topState.applicantName} (${topState.dateString})")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, "Share Advance Salary PDF"))
        } catch (e: Exception) {
            Toast.makeText(context, "PDF creation failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun createAdvanceSalaryPdfDocument(
        topState: AdvanceSalaryFormState?,
        bottomState: AdvanceSalaryFormState?,
        position: PrintPosition = PrintPosition.TOP
    ): PdfDocument {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 (72 DPI)
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val bgPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, 595f, 842f, bgPaint)

        val resolvedTop = topState ?: bottomState
        val resolvedBottom = bottomState ?: topState

        when (position) {
            PrintPosition.TOP -> {
                resolvedTop?.let {
                    drawSingleVoucherOnCanvas(canvas = canvas, startY = 0f, state = it)
                }
            }
            PrintPosition.BOTTOM -> {
                resolvedBottom?.let {
                    drawSingleVoucherOnCanvas(canvas = canvas, startY = 421f, state = it)
                }
            }
            PrintPosition.BOTH -> {
                resolvedTop?.let {
                    drawSingleVoucherOnCanvas(canvas = canvas, startY = 0f, state = it)
                }
                resolvedBottom?.let {
                    drawSingleVoucherOnCanvas(canvas = canvas, startY = 421f, state = it)
                }

                // Perforation line between top and bottom
                val dashPaint = Paint().apply {
                    color = Color.rgb(180, 180, 180)
                    strokeWidth = 0.8f
                    style = Paint.Style.STROKE
                    pathEffect = DashPathEffect(floatArrayOf(6f, 4f), 0f)
                }
                canvas.drawLine(15f, 421f, 580f, 421f, dashPaint)
            }
        }

        pdfDocument.finishPage(page)
        return pdfDocument
    }

    /**
     * Draws the exact voucher matching the user's reference design:
     * - Outer solid frame
     * - "ADVANCE SALARY REQUISITION VOUCHER" bold centered title
     * - "Date: dd/MM/yyyy" right aligned
     * - Black badge: "APPLICANT / EMPLOYEE DETAILS"
     * - 3-Row solid table for Name, Designation, Basic Salary
     * - Black badge: "ADVANCE & REPAYMENT TERMS"
     * - 5-Row dashed-divider table for Advance Amount, In Words, Reason, Repayment, Deduction Starts
     * - Undertaking statement
     * - Two signatures at bottom: Applicant's Signature and Authorized Signature
     */
    private fun drawSingleVoucherOnCanvas(
        canvas: android.graphics.Canvas,
        startY: Float,
        state: AdvanceSalaryFormState
    ) {
        canvas.save()
        canvas.translate(15f, startY + 421f)
        canvas.rotate(-90f)

        val localStartY = 12f
        val tableLeft = 12f
        val tableRight = 408f
        val tableWidth = tableRight - tableLeft
        val centerX = tableLeft + (tableWidth / 2f)
        val voucherHeight = 556f

        // Paints
        val outerBorderPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        val solidBorderPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val dashedLinePaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 0.9f
            pathEffect = DashPathEffect(floatArrayOf(4f, 3f), 0f)
        }
        val badgeBgPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            style = Paint.Style.FILL
        }
        val badgeTextPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val titleTextPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val dateTextPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val cellLabelPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val cellColonPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val cellValuePaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 9.5f
            typeface = Typeface.DEFAULT
        }
        val undertakingPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 8.5f
            typeface = Typeface.DEFAULT
        }
        val sigLinePaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            strokeWidth = 1f
        }
        val sigTextPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        // 1. Outer Border Frame
        val outerFrame = RectF(tableLeft, localStartY, tableRight, localStartY + voucherHeight)
        canvas.drawRect(outerFrame, outerBorderPaint)

        val innerLeft = tableLeft + 12f
        val innerRight = tableRight - 12f
        val innerWidth = innerRight - innerLeft

        // 2. Main Title (Centered)
        var curY = localStartY + 28f
        canvas.drawText("ADVANCE SALARY REQUISITION VOUCHER", centerX, curY, titleTextPaint)

        // 3. Date Row (Right Aligned)
        curY += 24f
        val dateDisplay = state.dateString.ifBlank { "01/09/2026" }
        canvas.drawText("Date: $dateDisplay", innerRight, curY, dateTextPaint)

        // 4. Section 1 Badge: APPLICANT / EMPLOYEE DETAILS
        curY += 12f
        val badgeH = 18f
        val badge1Width = 170f
        val badge1Rect = RectF(innerLeft, curY, innerLeft + badge1Width, curY + badgeH)
        canvas.drawRect(badge1Rect, badgeBgPaint)
        canvas.drawText("APPLICANT / EMPLOYEE DETAILS", innerLeft + 7f, curY + 12.5f, badgeTextPaint)

        // 5. Section 1 Table (3 Rows default or 5 Rows if Previous Advance exists)
        val hasPrevAdvance = state.previousAdvancePending > 0.0
        curY += badgeH
        val sec1TableTop = curY
        val sec1RowH = if (hasPrevAdvance) 22f else 26f
        val sec1RowCount = if (hasPrevAdvance) 5 else 3
        val sec1TableH = sec1RowH * sec1RowCount.toFloat()
        val sec1TableRect = RectF(innerLeft, sec1TableTop, innerRight, sec1TableTop + sec1TableH)
        canvas.drawRect(sec1TableRect, solidBorderPaint)

        val col1DividerX = innerLeft + 115f
        val colColonX = innerLeft + 126f
        val colValueX = innerLeft + 136f

        // Vertical divider after label column
        canvas.drawLine(col1DividerX, sec1TableTop, col1DividerX, sec1TableTop + sec1TableH, solidBorderPaint)

        // Row 1: Name
        val r1Y = sec1TableTop + (sec1RowH * 0.68f)
        canvas.drawText("Name", innerLeft + 8f, r1Y, cellLabelPaint)
        canvas.drawText(":", colColonX, r1Y, cellColonPaint)
        canvas.drawText(state.applicantName.ifBlank { "Nahid" }, colValueX, r1Y, cellValuePaint)
        canvas.drawLine(innerLeft, sec1TableTop + sec1RowH, innerRight, sec1TableTop + sec1RowH, solidBorderPaint)

        // Row 2: Designation
        val r2Y = sec1TableTop + sec1RowH + (sec1RowH * 0.68f)
        canvas.drawText("Designation", innerLeft + 8f, r2Y, cellLabelPaint)
        canvas.drawText(":", colColonX, r2Y, cellColonPaint)
        canvas.drawText(state.designation.ifBlank { "Office Assistant" }, colValueX, r2Y, cellValuePaint)
        canvas.drawLine(innerLeft, sec1TableTop + (sec1RowH * 2f), innerRight, sec1TableTop + (sec1RowH * 2f), solidBorderPaint)

        // Row 3: Basic Salary
        val r3Y = sec1TableTop + (sec1RowH * 2f) + (sec1RowH * 0.68f)
        canvas.drawText("Basic Salary", innerLeft + 8f, r3Y, cellLabelPaint)
        canvas.drawText(":", colColonX, r3Y, cellColonPaint)
        val salaryStr = if (state.monthlySalary > 0) "Tk. ${EnglishUtils.formatEnglishCurrency(state.monthlySalary)}/-" else "Tk. 11,000/-"
        canvas.drawText(salaryStr, colValueX, r3Y, cellValuePaint)

        if (hasPrevAdvance) {
            canvas.drawLine(innerLeft, sec1TableTop + (sec1RowH * 3f), innerRight, sec1TableTop + (sec1RowH * 3f), solidBorderPaint)

            // Row 4: Previous Advance Taken
            val r4Y = sec1TableTop + (sec1RowH * 3f) + (sec1RowH * 0.68f)
            canvas.drawText("Prev. Advance Taken", innerLeft + 8f, r4Y, cellLabelPaint)
            canvas.drawText(":", colColonX, r4Y, cellColonPaint)
            val prevStr = "Tk. ${EnglishUtils.formatEnglishCurrency(state.previousAdvancePending)}/-"
            canvas.drawText(prevStr, colValueX, r4Y, cellValuePaint)
            canvas.drawLine(innerLeft, sec1TableTop + (sec1RowH * 4f), innerRight, sec1TableTop + (sec1RowH * 4f), solidBorderPaint)

            // Row 5: Net Available Salary
            val r5Y = sec1TableTop + (sec1RowH * 4f) + (sec1RowH * 0.68f)
            canvas.drawText("Net Eligible Limit", innerLeft + 8f, r5Y, cellLabelPaint)
            canvas.drawText(":", colColonX, r5Y, cellColonPaint)
            val netEligible = (state.monthlySalary - state.previousAdvancePending).coerceAtLeast(0.0)
            val netStr = "Tk. ${EnglishUtils.formatEnglishCurrency(netEligible)}/-"
            canvas.drawText(netStr, colValueX, r5Y, cellValuePaint)
        }

        // 6. Section 2 Badge: ADVANCE & REPAYMENT TERMS
        curY = sec1TableTop + sec1TableH + (if (hasPrevAdvance) 14f else 18f)
        val badge2Width = 170f
        val badge2Rect = RectF(innerLeft, curY, innerLeft + badge2Width, curY + badgeH)
        canvas.drawRect(badge2Rect, badgeBgPaint)
        canvas.drawText("ADVANCE & REPAYMENT TERMS", innerLeft + 7f, curY + 12.5f, badgeTextPaint)

        // 7. Section 2 Table (6 Rows with Dashed Inner Dividers)
        curY += badgeH
        val sec2TableTop = curY
        val sec2RowH = if (hasPrevAdvance) 22f else 25f
        val sec2RowCount = 6
        val sec2TableH = sec2RowH * sec2RowCount.toFloat()
        val sec2TableRect = RectF(innerLeft, sec2TableTop, innerRight, sec2TableTop + sec2TableH)
        canvas.drawRect(sec2TableRect, solidBorderPaint)

        // Vertical divider
        canvas.drawLine(col1DividerX, sec2TableTop, col1DividerX, sec2TableTop + sec2TableH, solidBorderPaint)

        // Row 1: Advance Amount
        val s2r1Y = sec2TableTop + (sec2RowH * 0.68f)
        canvas.drawText("Advance Amount", innerLeft + 8f, s2r1Y, cellLabelPaint)
        canvas.drawText(":", colColonX, s2r1Y, cellColonPaint)
        val advAmtStr = if (state.advanceAmount > 0) "Tk. ${EnglishUtils.formatEnglishCurrency(state.advanceAmount)}/-" else "Tk. 5,000/-"
        canvas.drawText(advAmtStr, colValueX, s2r1Y, cellValuePaint)
        canvas.drawLine(innerLeft, sec2TableTop + sec2RowH, innerRight, sec2TableTop + sec2RowH, dashedLinePaint)

        // Row 2: In Words
        val s2r2Y = sec2TableTop + sec2RowH + (sec2RowH * 0.68f)
        canvas.drawText("In Words", innerLeft + 8f, s2r2Y, cellLabelPaint)
        canvas.drawText(":", colColonX, s2r2Y, cellColonPaint)
        val inWordsText = state.advanceAmountInWords.ifBlank {
            if (state.advanceAmount > 0) EnglishUtils.amountToEnglishWords(state.advanceAmount) else "Five Thousand Taka Only"
        }
        canvas.drawText(inWordsText, colValueX, s2r2Y, cellValuePaint)
        canvas.drawLine(innerLeft, sec2TableTop + (sec2RowH * 2f), innerRight, sec2TableTop + (sec2RowH * 2f), dashedLinePaint)

        // Row 3: Remaining Salary
        val s2r3Y = sec2TableTop + (sec2RowH * 2f) + (sec2RowH * 0.68f)
        canvas.drawText("Remaining Salary", innerLeft + 8f, s2r3Y, cellLabelPaint)
        canvas.drawText(":", colColonX, s2r3Y, cellColonPaint)
        val remainingSalary = if (state.monthlySalary > 0) {
            (state.monthlySalary - state.previousAdvancePending - state.advanceAmount).coerceAtLeast(0.0)
        } else 0.0
        val remainingStr = if (state.monthlySalary > 0) "Tk. ${EnglishUtils.formatEnglishCurrency(remainingSalary)}/-" else "Tk. 0/-"
        canvas.drawText(remainingStr, colValueX, s2r3Y, cellValuePaint)
        canvas.drawLine(innerLeft, sec2TableTop + (sec2RowH * 3f), innerRight, sec2TableTop + (sec2RowH * 3f), dashedLinePaint)

        // Row 4: Reason
        val s2r4Y = sec2TableTop + (sec2RowH * 3f) + (sec2RowH * 0.68f)
        canvas.drawText("Reason", innerLeft + 8f, s2r4Y, cellLabelPaint)
        canvas.drawText(":", colColonX, s2r4Y, cellColonPaint)
        val reasonText = state.reason.ifBlank { "Personal Emergency" }
        canvas.drawText(reasonText, colValueX, s2r4Y, cellValuePaint)
        canvas.drawLine(innerLeft, sec2TableTop + (sec2RowH * 4f), innerRight, sec2TableTop + (sec2RowH * 4f), dashedLinePaint)

        // Row 5: Repayment
        val s2r5Y = sec2TableTop + (sec2RowH * 4f) + (sec2RowH * 0.68f)
        canvas.drawText("Repayment", innerLeft + 8f, s2r5Y, cellLabelPaint)
        canvas.drawText(":", colColonX, s2r5Y, cellColonPaint)
        val repayStr = if (state.repaymentType == "installments") {
            "Monthly Installments: ${state.installmentCount} Months (Tk. ${EnglishUtils.formatEnglishCurrency(state.installmentAmountPerMonth)}/mo)"
        } else {
            "One-time full deduction from salary"
        }
        canvas.drawText(repayStr, colValueX, s2r5Y, cellValuePaint)
        canvas.drawLine(innerLeft, sec2TableTop + (sec2RowH * 5f), innerRight, sec2TableTop + (sec2RowH * 5f), dashedLinePaint)

        // Row 6: Deduction Starts
        val s2r6Y = sec2TableTop + (sec2RowH * 5f) + (sec2RowH * 0.68f)
        canvas.drawText("Deduction Starts", innerLeft + 8f, s2r6Y, cellLabelPaint)
        canvas.drawText(":", colColonX, s2r6Y, cellColonPaint)
        val dedStart = state.deductionStartMonth.ifBlank { "Next Month" }
        canvas.drawText(dedStart, colValueX, s2r6Y, cellValuePaint)

        // 8. Declaration Card Box with pill badge & connector lines
        curY = sec2TableTop + sec2TableH + (if (hasPrevAdvance) 14f else 18f)
        val declBoxTop = curY
        val declBoxH = if (hasPrevAdvance) 44f else 48f
        val declBoxRect = RectF(innerLeft, declBoxTop, innerRight, declBoxTop + declBoxH)
        canvas.drawRoundRect(declBoxRect, 8f, 8f, solidBorderPaint)

        // Badge centered on top line of declaration box
        val badgeDeclW = 86f
        val badgeDeclH = 15f
        val badgeDeclRect = RectF(centerX - (badgeDeclW / 2f), declBoxTop - (badgeDeclH / 2f), centerX + (badgeDeclW / 2f), declBoxTop + (badgeDeclH / 2f))

        // White mask behind badge and side lines
        val maskRect = RectF(centerX - 80f, declBoxTop - (badgeDeclH / 2f) - 1f, centerX + 80f, declBoxTop + (badgeDeclH / 2f) + 1f)
        val whiteBgPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(maskRect, whiteBgPaint)

        // Side connector lines and bullets: ●—— DECLARATION ——●
        val connectorPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        val dotPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            style = Paint.Style.FILL
        }
        // Left line & dot
        canvas.drawLine(centerX - 76f, declBoxTop, centerX - (badgeDeclW / 2f) - 4f, declBoxTop, connectorPaint)
        canvas.drawCircle(centerX - 76f, declBoxTop, 2.2f, dotPaint)

        // Right line & dot
        canvas.drawLine(centerX + (badgeDeclW / 2f) + 4f, declBoxTop, centerX + 76f, declBoxTop, connectorPaint)
        canvas.drawCircle(centerX + 76f, declBoxTop, 2.2f, dotPaint)

        // Draw black pill badge
        canvas.drawRoundRect(badgeDeclRect, 4f, 4f, badgeBgPaint)
        val badgeCenterTextPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("DECLARATION", centerX, declBoxTop + 3.2f, badgeCenterTextPaint)

        // Inside declaration text (italic, centered)
        val declTextPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 8.2f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        val line1Y = declBoxTop + 20f
        val line2Y = declBoxTop + 33f
        canvas.drawText("I hereby authorize the management to deduct the stated advance amount", centerX, line1Y, declTextPaint)
        canvas.drawText("from my monthly salary in accordance with the terms mentioned above.", centerX, line2Y, declTextPaint)

        // 9. Signatures (Applicant's Signature on left, Authorized Signature on right)
        if (state.showSignatures) {
            val sigLineY = localStartY + voucherHeight - 40f
            val sigLineWidth = 115f

            // Left: Applicant's Signature
            val sigLeftStart = innerLeft + 8f
            val sigLeftEnd = sigLeftStart + sigLineWidth
            val sigLeftCenter = (sigLeftStart + sigLeftEnd) / 2f
            canvas.drawLine(sigLeftStart, sigLineY, sigLeftEnd, sigLineY, sigLinePaint)
            canvas.drawText("Applicant's Signature", sigLeftCenter, sigLineY + 15f, sigTextPaint)

            // Right: Authorized Signature
            val sigRightEnd = innerRight - 8f
            val sigRightStart = sigRightEnd - sigLineWidth
            val sigRightCenter = (sigRightStart + sigRightEnd) / 2f
            canvas.drawLine(sigRightStart, sigLineY, sigRightEnd, sigLineY, sigLinePaint)
            canvas.drawText("Authorized Signature", sigRightCenter, sigLineY + 15f, sigTextPaint)
        }

        canvas.restore()
    }
}
