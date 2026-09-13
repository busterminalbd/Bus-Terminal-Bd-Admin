package com.example.util

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Sends a photo (e.g. of a handwritten/printed medical test list) to the Gemini API
 * and asks it to return the data in the exact JSON shape that
 * MedicalWorkViewModel.bulkAddFromText() already knows how to parse:
 *
 *   { "date": "DD/MM/YYYY", "data": [ { "patientId": "...", "code": "...", "name": "..." }, ... ] }
 *
 * The user never sees or writes this prompt — it lives here in code so the
 * camera button "just works" for every user, using only the API key they've
 * entered themselves in Settings.
 */
object GeminiVisionRepository {

    private const val MODEL = "gemini-2.5-flash"
    private const val ENDPOINT =
        "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"

    private val EXTRACTION_PROMPT = """
        You are reading a photo of a medical center's daily patient/test list (a handwritten
        or printed register, ledger, or list). Extract every row you can find and return
        ONLY a single JSON object, with no explanation and no markdown code fences, in
        exactly this shape:

        {
          "date": "DD/MM/YYYY",
          "data": [
            { "patientId": "string, the patient ID / registration number / serial as written", "code": "string, the test/investigation code(s) for that row, comma separated if multiple", "name": "string, the patient's name if visible, otherwise empty string" }
          ]
        }

        Rules:
        - "date" is the date written on the page/register if visible; if no date is visible, omit the "date" field entirely.
        - Each entry in "data" is one patient/one row from the list.
        - "patientId" should be the ID/serial/registration number exactly as written (keep leading zeros/letters).
        - "code" should be the test or investigation code(s) exactly as abbreviated in the source (e.g. AF07, CBC, USG, MD-01). If a row lists multiple codes, join them with ", ".
        - "name" is optional; use an empty string "" if not clearly written or not present.
        - Do not invent rows, IDs, or codes that are not visible in the image.
        - Return valid JSON only — no commentary, no markdown, no trailing commas.
    """.trimIndent()

    sealed class Result {
        data class Success(val jsonText: String) : Result()
        data class Failure(val message: String) : Result()
    }

    suspend fun extractMedicalDataFromImage(apiKey: String, imageBytes: ByteArray): Result =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) {
                return@withContext Result.Failure("Gemini API key দেওয়া হয়নি")
            }

            try {
                val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

                val requestBody = JSONObject().apply {
                    put("contents", JSONArray().put(
                        JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("inline_data", JSONObject().apply {
                                        put("mime_type", "image/jpeg")
                                        put("data", base64Image)
                                    })
                                })
                                put(JSONObject().apply {
                                    put("text", EXTRACTION_PROMPT)
                                })
                            })
                        }
                    ))
                    put("generationConfig", JSONObject().apply {
                        put("responseMimeType", "application/json")
                        put("temperature", 0.1)
                    })
                }

                val url = URL("$ENDPOINT?key=$apiKey")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = 30000
                    readTimeout = 60000
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }

                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(requestBody.toString())
                    writer.flush()
                }

                val responseCode = connection.responseCode
                val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
                val responseText = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""

                if (responseCode !in 200..299) {
                    val errMsg = try {
                        JSONObject(responseText).optJSONObject("error")?.optString("message")
                    } catch (e: Exception) {
                        null
                    } ?: "HTTP $responseCode"
                    return@withContext Result.Failure("Gemini API এরর: $errMsg")
                }

                val root = JSONObject(responseText)
                val candidates = root.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    return@withContext Result.Failure("Gemini থেকে কোনো ফলাফল পাওয়া যায়নি (সম্ভবত ছবিতে কিছু বোঝা যায়নি)")
                }

                val parts = candidates.getJSONObject(0).optJSONObject("content")?.optJSONArray("parts")
                val text = parts?.let {
                    val sb = StringBuilder()
                    for (i in 0 until it.length()) {
                        sb.append(it.getJSONObject(i).optString("text"))
                    }
                    sb.toString()
                }?.trim()

                if (text.isNullOrBlank()) {
                    return@withContext Result.Failure("Gemini থেকে খালি রেসপন্স এসেছে")
                }

                Result.Success(text)
            } catch (e: Exception) {
                Result.Failure("নেটওয়ার্ক/প্রসেসিং সমস্যা: ${e.message ?: e.javaClass.simpleName}")
            }
        }
}
