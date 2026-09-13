package com.example.util

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Stores the Gemini API key locally on the user's own device (SharedPreferences),
 * per-device. Nothing is ever sent to any server other than Google's Gemini API,
 * and only when the user has entered their own key.
 *
 * Also keeps a simple local counter of how many AI (camera) requests were made
 * "today", purely so Settings can show an approximate usage figure. This is a
 * local counter only — it is NOT the real remaining quota on Google's servers,
 * since Gemini does not expose a "remaining quota" endpoint to client apps.
 */
object GeminiSettingsStore {

    private const val PREFS_NAME = "gemini_settings"
    private const val KEY_API_KEY = "api_key"
    private const val KEY_USAGE_DATE = "usage_date"
    private const val KEY_USAGE_COUNT = "usage_count"

    /**
     * Approximate free-tier daily request cap shown to the user as a rough guide only.
     * Google can change real limits at any time; this is not fetched live.
     */
    const val APPROX_DAILY_FREE_LIMIT = 500

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun todayKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun getApiKey(context: Context): String {
        return prefs(context).getString(KEY_API_KEY, "") ?: ""
    }

    fun hasApiKey(context: Context): Boolean = getApiKey(context).isNotBlank()

    fun saveApiKey(context: Context, key: String) {
        prefs(context).edit().putString(KEY_API_KEY, key.trim()).apply()
    }

    fun clearApiKey(context: Context) {
        prefs(context).edit().remove(KEY_API_KEY).apply()
    }

    /** Call once per successful AI image request. */
    fun recordUsage(context: Context) {
        val p = prefs(context)
        val today = todayKey()
        val storedDate = p.getString(KEY_USAGE_DATE, "")
        val currentCount = if (storedDate == today) p.getInt(KEY_USAGE_COUNT, 0) else 0
        p.edit()
            .putString(KEY_USAGE_DATE, today)
            .putInt(KEY_USAGE_COUNT, currentCount + 1)
            .apply()
    }

    /** Requests made "today" using the locally stored key, by this local counter only. */
    fun getUsageToday(context: Context): Int {
        val p = prefs(context)
        val today = todayKey()
        val storedDate = p.getString(KEY_USAGE_DATE, "")
        return if (storedDate == today) p.getInt(KEY_USAGE_COUNT, 0) else 0
    }
}
