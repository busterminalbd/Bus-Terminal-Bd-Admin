package com.example.util

import android.graphics.Paint

/**
 * Shared helper for wrapping long item names onto multiple lines instead of
 * shrinking the font or letting the text overflow/overlap the next column.
 *
 * Wraps on spaces first (normal word-wrap). If a single "word" is still wider
 * than the available column, it force-breaks that word character by character
 * so it never overflows the column boundary.
 */
object CanvasTextUtils {

    fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isBlank()) return listOf("")
        if (maxWidth <= 0f) return listOf(text)

        val words = text.trim().split(Regex("\\s+"))
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        fun breakLongWord(word: String) {
            var remaining = word
            while (paint.measureText(remaining) > maxWidth && remaining.length > 1) {
                var end = remaining.length
                while (end > 1 && paint.measureText(remaining.substring(0, end)) > maxWidth) {
                    end--
                }
                lines.add(remaining.substring(0, end))
                remaining = remaining.substring(end)
            }
            currentLine = StringBuilder(remaining)
        }

        for (word in words) {
            val candidate = if (currentLine.isEmpty()) word else "${currentLine} $word"
            when {
                paint.measureText(candidate) <= maxWidth -> currentLine = StringBuilder(candidate)
                currentLine.isNotEmpty() -> {
                    lines.add(currentLine.toString())
                    currentLine = StringBuilder()
                    if (paint.measureText(word) <= maxWidth) {
                        currentLine = StringBuilder(word)
                    } else {
                        breakLongWord(word)
                    }
                }
                else -> breakLongWord(word)
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine.toString())
        return if (lines.isEmpty()) listOf("") else lines
    }
}
