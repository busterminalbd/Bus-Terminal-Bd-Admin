package com.example

import com.example.util.EnglishUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class AdvanceSalaryTest {

    @Test
    fun `test English amount in words`() {
        assertEquals("Fifteen Thousand Taka Only", EnglishUtils.amountToEnglishWords(15000.0))
        assertEquals("Five Hundred Taka Only", EnglishUtils.amountToEnglishWords(500.0))
        assertEquals("One Lakh Twenty-Five Thousand Taka Only".replace("-", " "), EnglishUtils.amountToEnglishWords(125000.0))
        assertEquals("Twenty-Five Thousand Four Hundred Fifty Taka Only".replace("-", " "), EnglishUtils.amountToEnglishWords(25450.0))
    }

    @Test
    fun `test currency formatting`() {
        assertEquals("15,000", EnglishUtils.formatEnglishCurrency(15000.0))
        assertEquals("25,450", EnglishUtils.formatEnglishCurrency(25450.0))
    }
}
