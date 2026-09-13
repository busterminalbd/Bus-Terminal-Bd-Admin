package com.example.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class AppUpdateManager(
    private val repoOwner: String = "nahid6714",
    private val repoName: String = "tools"
) {

    companion object {
        const val UPDATE_JSON_RAW_URL = "https://raw.githubusercontent.com/nahid6714/tools/main/update.json"
        const val UPDATE_JSON_RAW_FALLBACK_URL = "https://raw.githubusercontent.com/nahid6714/tools/master/update.json"
        const val UPDATE_JSON_API_URL = "https://api.github.com/repos/nahid6714/tools/contents/update.json"
        const val RELEASES_API_URL = "https://api.github.com/repos/nahid6714/tools/releases/latest"

        private const val PREFS_NAME = "app_update_checker_prefs"
        private const val KEY_LAST_CHECK_TIME = "last_check_time"
        private const val KEY_CACHED_JSON = "cached_json"

        // Minimum time interval between automated background checks (5 minutes)
        private const val CACHE_EXPIRATION_MS = 5 * 60 * 1000L
        // Minimum time interval between manual checks to avoid spamming network (2 seconds)
        private const val MIN_CHECK_INTERVAL_MS = 2 * 1000L

        private val checkMutex = Mutex()
    }

    /**
     * Checks for updates by fetching update.json via multiple resilient fallbacks:
     * 1. GitHub API Contents with raw Accept header
     * 2. GitHub Raw User Content
     * 3. GitHub Latest Release REST API
     * Silent fallback on network/parsing failure - will never throw or crash the UI.
     */
    suspend fun checkForUpdate(context: Context, forceCheck: Boolean = true): UpdateInfo = withContext(Dispatchers.IO) {
        val currentVersionName = BuildConfig.VERSION_NAME
        val currentVersionCode = BuildConfig.VERSION_CODE

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastCheckTime = prefs.getLong(KEY_LAST_CHECK_TIME, 0L)
        val now = System.currentTimeMillis()

        // Throttling: If checked very recently or if cached & not forced
        if (!forceCheck && (now - lastCheckTime < CACHE_EXPIRATION_MS)) {
            val cachedJsonStr = prefs.getString(KEY_CACHED_JSON, null)
            if (!cachedJsonStr.isNullOrBlank()) {
                val cachedInfo = parseUpdateJson(cachedJsonStr, currentVersionName, currentVersionCode)
                if (cachedInfo != null) return@withContext cachedInfo
            }
        } else if (now - lastCheckTime < MIN_CHECK_INTERVAL_MS) {
            val cachedJsonStr = prefs.getString(KEY_CACHED_JSON, null)
            if (!cachedJsonStr.isNullOrBlank()) {
                val cachedInfo = parseUpdateJson(cachedJsonStr, currentVersionName, currentVersionCode)
                if (cachedInfo != null) return@withContext cachedInfo
            }
        }

        // Lock to avoid duplicate concurrent network requests
        checkMutex.withLock {
            try {
                // Strategy 1: GitHub API Contents (Accept: application/vnd.github.v3.raw)
                var jsonStr: String? = fetchUrl(
                    UPDATE_JSON_API_URL,
                    headers = mapOf("Accept" to "application/vnd.github.v3.raw")
                )

                // Strategy 2: GitHub raw main
                if (jsonStr == null) {
                    jsonStr = fetchUrl(UPDATE_JSON_RAW_URL)
                }

                // Strategy 3: GitHub raw master
                if (jsonStr == null) {
                    jsonStr = fetchUrl(UPDATE_JSON_RAW_FALLBACK_URL)
                }

                if (jsonStr != null) {
                    val updateInfo = parseUpdateJson(jsonStr, currentVersionName, currentVersionCode)
                    if (updateInfo != null) {
                        prefs.edit()
                            .putLong(KEY_LAST_CHECK_TIME, System.currentTimeMillis())
                            .putString(KEY_CACHED_JSON, jsonStr)
                            .apply()
                        return@withContext updateInfo
                    }
                }

                // Strategy 4: Fallback to GitHub Releases API
                val releasesJsonStr = fetchUrl(RELEASES_API_URL, headers = mapOf("Accept" to "application/vnd.github.v3+json"))
                if (releasesJsonStr != null) {
                    val releaseInfo = parseGitHubReleaseJson(releasesJsonStr, currentVersionName, currentVersionCode)
                    if (releaseInfo != null) {
                        prefs.edit()
                            .putLong(KEY_LAST_CHECK_TIME, System.currentTimeMillis())
                            .apply()
                        return@withContext releaseInfo
                    }
                }

                return@withContext UpdateInfo(
                    hasUpdate = false,
                    currentVersionName = currentVersionName,
                    errorMessage = "আপডেট সার্ভারে এই মুহূর্তে সংযোগ করা যাচ্ছে না। অনুগ্রহ করে ইন্টারনেট চেক করুন।"
                )
            } catch (e: java.net.UnknownHostException) {
                return@withContext UpdateInfo(
                    hasUpdate = false,
                    currentVersionName = currentVersionName,
                    errorMessage = "ইন্টারনেট সংযোগ নেই। আপনার ওয়াইফাই বা ডাটা চেক করুন।"
                )
            } catch (e: java.net.SocketTimeoutException) {
                return@withContext UpdateInfo(
                    hasUpdate = false,
                    currentVersionName = currentVersionName,
                    errorMessage = "সার্ভার রেসপন্স করতে সময় নিচ্ছে। কিছুক্ষণ পর আবার চেষ্টা করুন।"
                )
            } catch (e: Exception) {
                return@withContext UpdateInfo(
                    hasUpdate = false,
                    currentVersionName = currentVersionName,
                    errorMessage = "আপডেট চেক করার সময় সমস্যা হয়েছে: ${e.localizedMessage ?: "অজানা ত্রুটি"}"
                )
            }
        }
    }

    private fun fetchUrl(urlString: String, headers: Map<String, String> = emptyMap()): String? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                useCaches = false
                defaultUseCaches = false
                setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                setRequestProperty("Pragma", "no-cache")
                setRequestProperty("Expires", "0")
                setRequestProperty("User-Agent", "FoodBillManagerApp/1.0 (Android)")
                headers.forEach { (k, v) -> setRequestProperty(k, v) }
            }
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun parseGitHubReleaseJson(
        jsonStr: String,
        currentVersionName: String,
        currentVersionCode: Int
    ): UpdateInfo? {
        return try {
            val json = JSONObject(jsonStr)
            val tagName = json.optString("tag_name", "").trim()
            val rawBody = json.optString("body", "").trim()
            val cleanNotes = sanitizeReleaseNotes(rawBody)

            val remoteVersionCode = extractVersionCode(tagName, rawBody)
            val remoteVersionName = tagName.removePrefix("v").ifBlank { "v$remoteVersionCode" }

            var apkUrl = ""
            var apkSizeStr = ""
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i) ?: continue
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url", "")
                        val sizeBytes = asset.optLong("size", 0L)
                        if (sizeBytes > 0) {
                            val sizeMb = sizeBytes.toDouble() / (1024.0 * 1024.0)
                            apkSizeStr = String.format(java.util.Locale.US, "%.1f MB", sizeMb)
                        }
                        break
                    }
                }
            }

            val hasUpdate = if (remoteVersionCode > 0) {
                remoteVersionCode > currentVersionCode
            } else {
                isVersionNewer(remoteVersionName, currentVersionName)
            }

            UpdateInfo(
                hasUpdate = hasUpdate,
                latestVersionName = remoteVersionName,
                latestVersionCode = remoteVersionCode,
                releaseNotes = cleanNotes,
                downloadUrl = apkUrl,
                currentVersionName = currentVersionName,
                publishedAt = json.optString("published_at", ""),
                apkSizeFormatted = apkSizeStr
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun extractVersionCode(tagName: String, body: String): Int {
        val bodyMatch = Regex("""(?i)(?:build|version\s*code|code)\s*[:=]?\s*(\d+)""").find(body)
        if (bodyMatch != null) {
            return bodyMatch.groupValues[1].toIntOrNull() ?: 0
        }
        val tagNum = Regex("""\d+""").findAll(tagName).map { it.value.toIntOrNull() ?: 0 }.toList()
        if (tagNum.isNotEmpty()) {
            return tagNum.last()
        }
        return 0
    }

    private fun isVersionNewer(remote: String, current: String): Boolean {
        val rParts = remote.split(".").mapNotNull { it.filter { c -> c.isDigit() }.toIntOrNull() }
        val cParts = current.split(".").mapNotNull { it.filter { c -> c.isDigit() }.toIntOrNull() }
        val maxLen = maxOf(rParts.size, cParts.size)
        for (i in 0 until maxLen) {
            val r = rParts.getOrElse(i) { 0 }
            val c = cParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    private fun parseUpdateJson(
        jsonStr: String,
        currentVersionName: String,
        currentVersionCode: Int
    ): UpdateInfo? {
        return try {
            val json = JSONObject(jsonStr)
            val remoteVersionCode = json.optInt("versionCode", 0)
            val remoteVersionName = json.optString("versionName", "").trim()
            val downloadUrl = json.optString("downloadUrl", "").trim()
            val rawNotes = json.optString("releaseNotes", "").trim()
            val cleanNotes = sanitizeReleaseNotes(rawNotes)

            if (remoteVersionCode <= 0 && remoteVersionName.isBlank()) {
                return null
            }

            val hasUpdate = remoteVersionCode > currentVersionCode

            UpdateInfo(
                hasUpdate = hasUpdate,
                latestVersionName = if (remoteVersionName.isNotBlank()) remoteVersionName else "v$remoteVersionCode",
                latestVersionCode = remoteVersionCode,
                releaseNotes = cleanNotes,
                downloadUrl = downloadUrl,
                currentVersionName = currentVersionName,
                publishedAt = "",
                apkSizeFormatted = ""
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun sanitizeReleaseNotes(rawBody: String): String {
        if (rawBody.isBlank()) {
            return "• অ্যাপের পারফরম্যান্স ও অভিজ্ঞতা উন্নত করা হয়েছে।\n• সাধারণ সমস্যা ও বাগ ফিক্স করা হয়েছে।"
        }

        val lines = rawBody.lines()
        val cleanList = mutableListOf<String>()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank()) continue

            val lower = trimmed.lowercase()
            if (lower.contains("commit:") ||
                lower.contains("message:") ||
                lower.contains("update appupdatemanager") ||
                lower.contains("update build.gradle") ||
                lower.contains("new release v") ||
                trimmed.matches(Regex("(?i).*([0-9a-f]{7,40}).*")) ||
                trimmed.startsWith("#")
            ) {
                continue
            }

            val cleanLine = trimmed
                .replace("**", "")
                .replace("*", "")
                .replace("`", "")
                .trim()

            if (cleanLine.isNotBlank()) {
                if (!cleanLine.startsWith("•") && !cleanLine.startsWith("-")) {
                    cleanList.add("• $cleanLine")
                } else {
                    cleanList.add(cleanLine)
                }
            }
        }

        if (cleanList.isEmpty()) {
            return "• অ্যাপের নতুন ফিচার ও বিভিন্ন উন্নতি অন্তর্ভুক্ত করা হয়েছে।\n• পারফরম্যান্স ও স্থায়িত্ব বৃদ্ধি করা হয়েছে।"
        }

        return cleanList.joinToString("\n")
    }

    /**
     * Downloads the release APK into the app's cache directory and updates progress state.
     */
    suspend fun downloadAndInstallApk(
        context: Context,
        downloadUrl: String,
        onProgress: (Int) -> Unit,
        onSuccess: (File) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            if (downloadUrl.isBlank()) {
                withContext(Dispatchers.Main) {
                    onError("ডাউনলোড লিংক পাওয়া যায়নি।")
                }
                return@withContext
            }

            var urlStr = downloadUrl
            var connection: HttpURLConnection? = null
            var responseCode = 0
            var redirectCount = 0

            while (redirectCount < 10) {
                val url = URL(urlStr)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 30000
                    readTimeout = 60000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10) AndroidAppUpdater")
                    setRequestProperty("Accept-Encoding", "identity")
                }
                responseCode = connection.responseCode
                if (responseCode in 300..399) {
                    val newUrl = connection.getHeaderField("Location")
                    connection.disconnect()
                    if (!newUrl.isNullOrEmpty()) {
                        urlStr = newUrl
                        redirectCount++
                    } else {
                        break
                    }
                } else {
                    break
                }
            }

            val finalConn = connection ?: throw IllegalStateException("Could not establish connection")

            if (responseCode != HttpURLConnection.HTTP_OK) {
                finalConn.disconnect()
                withContext(Dispatchers.Main) {
                    onError("ডাউনলোড ব্যর্থ হয়েছে (HTTP $responseCode)")
                }
                return@withContext
            }

            val fileLength = finalConn.contentLength
            val updateDir = File(context.cacheDir, "updates")
            if (!updateDir.exists()) updateDir.mkdirs()

            val apkFile = File(updateDir, "update.apk")
            if (apkFile.exists()) apkFile.delete()

            try {
                finalConn.inputStream.use { input ->
                    FileOutputStream(apkFile).use { output ->
                        val data = ByteArray(16384)
                        var total: Long = 0
                        var count: Int
                        var lastProgress = -1
                        while (input.read(data).also { count = it } != -1) {
                            total += count.toLong()
                            output.write(data, 0, count)
                            if (fileLength > 0) {
                                val progress = ((total * 100) / fileLength).toInt().coerceIn(0, 99)
                                if (progress != lastProgress) {
                                    lastProgress = progress
                                    withContext(Dispatchers.Main) {
                                        onProgress(progress)
                                    }
                                }
                            }
                        }
                        output.flush()
                    }
                }
            } finally {
                finalConn.disconnect()
            }

            if (apkFile.exists() && apkFile.length() > 0) {
                withContext(Dispatchers.Main) {
                    onProgress(100)
                    onSuccess(apkFile)
                    installApk(context, apkFile)
                }
            } else {
                withContext(Dispatchers.Main) {
                    onError("ডাউনলোডকৃত ফাইলটি সঠিক পাওয়া যায়নি।")
                }
            }

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onError(e.localizedMessage ?: "ডাউনলোড করতে ব্যর্থ হয়েছে। ইন্টারনেট কানেকশন চেক করুন।")
            }
        }
    }

    /**
     * Launches the Android package installer for the downloaded APK using FileProvider.
     */
    fun installApk(context: Context, apkFile: File) {
        try {
            if (!apkFile.exists()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}")
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    return
                }
            }

            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
