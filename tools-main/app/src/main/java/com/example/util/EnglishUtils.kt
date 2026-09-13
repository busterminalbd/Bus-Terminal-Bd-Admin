package com.example.util

import java.text.DecimalFormat

object EnglishUtils {

    private val units = arrayOf(
        "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
        "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
        "Seventeen", "Eighteen", "Nineteen"
    )

    private val tens = arrayOf(
        "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    )

    fun numberToWords(n: Long): String {
        if (n == 0L) return "Zero"
        if (n < 0) return "Minus " + numberToWords(-n)

        var num = n
        val sb = StringBuilder()

        val crore = num / 10000000L
        num %= 10000000L
        if (crore > 0) {
            sb.append(numberToWords(crore)).append(" Crore ")
        }

        val lakh = num / 100000L
        num %= 100000L
        if (lakh > 0) {
            sb.append(numberToWords(lakh)).append(" Lakh ")
        }

        val thousand = num / 1000L
        num %= 1000L
        if (thousand > 0) {
            sb.append(numberToWords(thousand)).append(" Thousand ")
        }

        val hundred = num / 100L
        num %= 100L
        if (hundred > 0) {
            sb.append(units[hundred.toInt()]).append(" Hundred ")
        }

        if (num > 0) {
            if (num < 20) {
                sb.append(units[num.toInt()])
            } else {
                val t = (num / 10).toInt()
                val u = (num % 10).toInt()
                sb.append(tens[t])
                if (u > 0) {
                    sb.append(" ").append(units[u])
                }
            }
        }

        return sb.toString().trim().replace(Regex("\\s+"), " ")
    }

    fun amountToEnglishWords(amount: Double): String {
        if (amount <= 0.0) return ""
        val wholePart = amount.toLong()
        val decimalPart = Math.round((amount - wholePart) * 100).toInt()

        val wholeWords = numberToWords(wholePart)
        return if (decimalPart > 0) {
            val decimalWords = numberToWords(decimalPart.toLong())
            "$wholeWords Taka and $decimalWords Paisa Only"
        } else {
            "$wholeWords Taka Only"
        }
    }

    fun formatEnglishCurrency(amount: Double): String {
        val df = DecimalFormat("#,##0.##")
        return df.format(amount)
    }
}
