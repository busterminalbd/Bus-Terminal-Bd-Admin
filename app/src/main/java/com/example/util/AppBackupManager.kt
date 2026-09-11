package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.data.AdvanceSalaryEntity
import com.example.data.AppDatabase
import com.example.data.FoodBillEntity
import com.example.data.MedicalRecordEntity
import com.example.data.PresetMedicalCodeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ImportResult(
    val success: Boolean,
    val foodBillsCount: Int = 0,
    val advanceSalariesCount: Int = 0,
    val medicalRecordsCount: Int = 0,
    val presetsRestored: Boolean = false,
    val message: String = ""
)

object AppBackupManager {

    private const val APP_IDENTIFIER = "DigitalToolsHubBackup"
    private const val BACKUP_VERSION = 1

    suspend fun exportAllData(context: Context): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("appIdentifier", APP_IDENTIFIER)
        root.put("backupVersion", BACKUP_VERSION)
        root.put("exportedAt", System.currentTimeMillis())
        val dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        root.put("exportedDate", dateFormatted)

        val db = AppDatabase.getDatabase(context)

        // 1. Export Food & Transport Bills
        val foodBillDao = db.foodBillDao()
        val allBills = foodBillDao.getAllBillsList()
        val billsArray = JSONArray()
        for (bill in allBills) {
            val billObj = JSONObject()
            billObj.put("id", bill.id)
            billObj.put("dateString", bill.dateString)
            billObj.put("timestamp", bill.timestamp)
            billObj.put("purchaserName", bill.purchaserName)
            billObj.put("centerName", bill.centerName)
            billObj.put("subtitle", bill.subtitle)
            billObj.put("note", bill.note)
            billObj.put("totalAmount", bill.totalAmount)
            billObj.put("itemsJson", bill.itemsJson)
            billObj.put("billType", bill.billType)
            billObj.put("showSignature", bill.showSignature)
            billsArray.put(billObj)
        }
        root.put("foodBills", billsArray)

        // 2. Export Advance Salary Applications
        val advanceSalaryDao = db.advanceSalaryDao()
        val allSalaries = advanceSalaryDao.getAllApplicationsList()
        val salariesArray = JSONArray()
        for (sal in allSalaries) {
            val salObj = JSONObject()
            salObj.put("id", sal.id)
            salObj.put("companyName", sal.companyName)
            salObj.put("companySubtitle", sal.companySubtitle)
            salObj.put("applicationNo", sal.applicationNo)
            salObj.put("dateString", sal.dateString)
            salObj.put("applicantName", sal.applicantName)
            salObj.put("employeeId", sal.employeeId)
            salObj.put("designation", sal.designation)
            salObj.put("department", sal.department)
            salObj.put("contactNumber", sal.contactNumber)
            salObj.put("monthlySalary", sal.monthlySalary)
            salObj.put("advanceAmount", sal.advanceAmount)
            salObj.put("advanceAmountInWords", sal.advanceAmountInWords)
            salObj.put("reason", sal.reason)
            salObj.put("repaymentType", sal.repaymentType)
            salObj.put("installmentCount", sal.installmentCount)
            salObj.put("installmentAmountPerMonth", sal.installmentAmountPerMonth)
            salObj.put("deductionStartMonth", sal.deductionStartMonth)
            salObj.put("previousAdvancePending", sal.previousAdvancePending)
            salObj.put("guarantorOrRecommendedBy", sal.guarantorOrRecommendedBy)
            salObj.put("remarks", sal.remarks)
            salObj.put("status", sal.status)
            salObj.put("showSignatures", sal.showSignatures)
            salObj.put("createdAt", sal.createdAt)
            salariesArray.put(salObj)
        }
        root.put("advanceSalaries", salariesArray)

        // 3. Export Medical Records & Preset Codes
        val medicalDao = db.medicalDao()
        val allMedicalRecords = medicalDao.getAllRecordsList()
        val medicalArray = JSONArray()
        for (med in allMedicalRecords) {
            val medObj = JSONObject()
            medObj.put("id", med.id)
            medObj.put("date", med.date)
            medObj.put("patientId", med.patientId)
            medObj.put("code", med.code)
            medObj.put("patientName", med.patientName)
            medObj.put("notes", med.notes)
            medObj.put("timestamp", med.timestamp)
            medicalArray.put(medObj)
        }
        root.put("medicalRecords", medicalArray)

        val presetCodes = medicalDao.getAllPresetCodesList()
        val presetCodesArray = JSONArray()
        for (pc in presetCodes) {
            val pcObj = JSONObject()
            pcObj.put("code", pc.code)
            pcObj.put("name", pc.name)
            pcObj.put("category", pc.category)
            presetCodesArray.put(pcObj)
        }
        root.put("presetMedicalCodes", presetCodesArray)

        // 4. Export Presets & Headers
        val foodPrefs = context.getSharedPreferences("food_bill_prefs", Context.MODE_PRIVATE)
        val salaryPrefs = context.getSharedPreferences("advance_salary_prefs", Context.MODE_PRIVATE)

        val presetsObj = JSONObject()
        presetsObj.put("quickPresetsJson", foodPrefs.getString("quick_presets_json", "[]"))
        presetsObj.put("presetReasonsJson", salaryPrefs.getString("preset_reasons_json", "[]"))
        presetsObj.put("presetDesignationsJson", salaryPrefs.getString("preset_designations_json", "[]"))
        root.put("presets", presetsObj)

        val settingsObj = JSONObject()
        settingsObj.put("saved_center_name_market", foodPrefs.getString("saved_center_name_market", ""))
        settingsObj.put("saved_subtitle_market", foodPrefs.getString("saved_subtitle_market", ""))
        settingsObj.put("saved_purchaser_label_market", foodPrefs.getString("saved_purchaser_label_market", ""))
        settingsObj.put("saved_center_name_transport", foodPrefs.getString("saved_center_name_transport", ""))
        settingsObj.put("saved_subtitle_transport", foodPrefs.getString("saved_subtitle_transport", ""))
        settingsObj.put("saved_purchaser_label_transport", foodPrefs.getString("saved_purchaser_label_transport", ""))
        settingsObj.put("default_company_name", salaryPrefs.getString("default_company_name", "Al-Baraka General Store"))
        settingsObj.put("default_company_subtitle", salaryPrefs.getString("default_company_subtitle", "Head Office, Dhaka"))
        settingsObj.put("app_theme_mode", foodPrefs.getString("app_theme_mode", "system"))
        settingsObj.put("app_theme_color", foodPrefs.getString("app_theme_color", "emerald"))
        settingsObj.put("app_language", foodPrefs.getString("app_language", "bn"))
        settingsObj.put("app_font_scale", foodPrefs.getFloat("app_font_scale", 1.0f).toDouble())
        root.put("settings", settingsObj)

        root.toString(2)
    }

    suspend fun importAllData(context: Context, jsonString: String): ImportResult = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)

            // Basic sanity validation
            val hasBills = root.has("foodBills")
            val hasSalaries = root.has("advanceSalaries")
            val hasPresets = root.has("presets")
            val hasSettings = root.has("settings")

            if (!hasBills && !hasSalaries && !hasPresets && !hasSettings) {
                return@withContext ImportResult(
                    success = false,
                    message = "অবৈধ ব্যাকআপ ফাইল। সঠিক JSON ব্যাকআপ ফাইল নির্বাচন করুন।"
                )
            }

            val db = AppDatabase.getDatabase(context)
            var restoredBillsCount = 0
            var restoredSalariesCount = 0

            // 1. Restore Food & Transport Bills
            if (hasBills) {
                val billsArray = root.getJSONArray("foodBills")
                val foodBillDao = db.foodBillDao()
                val existingBills = foodBillDao.getAllBillsList()

                val billsToInsert = mutableListOf<FoodBillEntity>()
                for (i in 0 until billsArray.length()) {
                    val obj = billsArray.getJSONObject(i)
                    val timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    val dateString = obj.optString("dateString", "")
                    val purchaserName = obj.optString("purchaserName", "")
                    val totalAmount = obj.optDouble("totalAmount", 0.0)

                    // Duplicate check: skip if identical timestamp and purchaser and totalAmount already exists
                    val exists = existingBills.any {
                        it.timestamp == timestamp &&
                        it.purchaserName == purchaserName &&
                        kotlin.math.abs(it.totalAmount - totalAmount) < 0.01
                    }

                    if (!exists) {
                        billsToInsert.add(
                            FoodBillEntity(
                                id = 0L, // auto-generate new unique ID
                                dateString = dateString,
                                timestamp = timestamp,
                                purchaserName = purchaserName,
                                centerName = obj.optString("centerName", ""),
                                subtitle = obj.optString("subtitle", ""),
                                note = obj.optString("note", ""),
                                totalAmount = totalAmount,
                                itemsJson = obj.optString("itemsJson", "[]"),
                                billType = obj.optString("billType", "market"),
                                showSignature = obj.optBoolean("showSignature", true)
                            )
                        )
                    }
                }

                if (billsToInsert.isNotEmpty()) {
                    foodBillDao.insertBills(billsToInsert)
                    restoredBillsCount = billsToInsert.size
                }
            }

            // 2. Restore Advance Salary Applications
            if (hasSalaries) {
                val salariesArray = root.getJSONArray("advanceSalaries")
                val advanceSalaryDao = db.advanceSalaryDao()
                val existingSalaries = advanceSalaryDao.getAllApplicationsList()

                val salariesToInsert = mutableListOf<AdvanceSalaryEntity>()
                for (i in 0 until salariesArray.length()) {
                    val obj = salariesArray.getJSONObject(i)
                    val appNo = obj.optString("applicationNo", "")
                    val applicantName = obj.optString("applicantName", "")
                    val createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    val advanceAmount = obj.optDouble("advanceAmount", 0.0)

                    val exists = existingSalaries.any {
                        (it.applicationNo.isNotBlank() && it.applicationNo == appNo) ||
                        (it.applicantName == applicantName && it.createdAt == createdAt && kotlin.math.abs(it.advanceAmount - advanceAmount) < 0.01)
                    }

                    if (!exists) {
                        salariesToInsert.add(
                            AdvanceSalaryEntity(
                                id = 0L,
                                companyName = obj.optString("companyName", ""),
                                companySubtitle = obj.optString("companySubtitle", ""),
                                applicationNo = appNo,
                                dateString = obj.optString("dateString", ""),
                                applicantName = applicantName,
                                employeeId = obj.optString("employeeId", ""),
                                designation = obj.optString("designation", ""),
                                department = obj.optString("department", ""),
                                contactNumber = obj.optString("contactNumber", ""),
                                monthlySalary = obj.optDouble("monthlySalary", 0.0),
                                advanceAmount = advanceAmount,
                                advanceAmountInWords = obj.optString("advanceAmountInWords", ""),
                                reason = obj.optString("reason", ""),
                                repaymentType = obj.optString("repaymentType", "one_time"),
                                installmentCount = obj.optInt("installmentCount", 1),
                                installmentAmountPerMonth = obj.optDouble("installmentAmountPerMonth", 0.0),
                                deductionStartMonth = obj.optString("deductionStartMonth", ""),
                                previousAdvancePending = obj.optDouble("previousAdvancePending", 0.0),
                                guarantorOrRecommendedBy = obj.optString("guarantorOrRecommendedBy", ""),
                                remarks = obj.optString("remarks", ""),
                                status = obj.optString("status", "APPROVED"),
                                showSignatures = obj.optBoolean("showSignatures", true),
                                createdAt = createdAt
                            )
                        )
                    }
                }

                if (salariesToInsert.isNotEmpty()) {
                    advanceSalaryDao.insertApplications(salariesToInsert)
                    restoredSalariesCount = salariesToInsert.size
                }
            }

            // 3. Restore Medical Records & Preset Codes
            var restoredMedicalCount = 0
            if (root.has("medicalRecords")) {
                val medicalArray = root.getJSONArray("medicalRecords")
                val medicalDao = db.medicalDao()
                val existing = medicalDao.getAllRecordsList()
                val toInsert = mutableListOf<MedicalRecordEntity>()
                for (i in 0 until medicalArray.length()) {
                    val obj = medicalArray.getJSONObject(i)
                    val date = obj.optString("date", "")
                    val pId = obj.optString("patientId", "")
                    val code = obj.optString("code", "")
                    val name = obj.optString("patientName", "")
                    val notes = obj.optString("notes", "")
                    val timestamp = obj.optLong("timestamp", System.currentTimeMillis())

                    val exists = existing.any { it.date == date && it.patientId == pId && it.code == code }
                    if (!exists) {
                        toInsert.add(
                            MedicalRecordEntity(
                                id = 0L,
                                date = date,
                                patientId = pId,
                                code = code,
                                patientName = name,
                                notes = notes,
                                timestamp = timestamp
                            )
                        )
                    }
                }
                if (toInsert.isNotEmpty()) {
                    medicalDao.insertRecords(toInsert)
                    restoredMedicalCount = toInsert.size
                }
            }

            if (root.has("presetMedicalCodes")) {
                val array = root.getJSONArray("presetMedicalCodes")
                val medicalDao = db.medicalDao()
                val list = mutableListOf<PresetMedicalCodeEntity>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val c = obj.optString("code", "")
                    if (c.isNotBlank()) {
                        list.add(
                            PresetMedicalCodeEntity(
                                code = c,
                                name = obj.optString("name", ""),
                                category = obj.optString("category", "General")
                            )
                        )
                    }
                }
                if (list.isNotEmpty()) {
                    medicalDao.insertPresetCodes(list)
                }
            }

            // 4. Restore Presets
            var presetsRestored = false
            val foodPrefs = context.getSharedPreferences("food_bill_prefs", Context.MODE_PRIVATE)
            val salaryPrefs = context.getSharedPreferences("advance_salary_prefs", Context.MODE_PRIVATE)

            if (hasPresets) {
                val presetsObj = root.getJSONObject("presets")
                val quickPresetsJson = presetsObj.optString("quickPresetsJson", "")
                if (quickPresetsJson.isNotBlank() && quickPresetsJson != "[]") {
                    foodPrefs.edit().putString("quick_presets_json", quickPresetsJson).apply()
                    presetsRestored = true
                }
                val reasonsJson = presetsObj.optString("presetReasonsJson", "")
                if (reasonsJson.isNotBlank() && reasonsJson != "[]") {
                    salaryPrefs.edit().putString("preset_reasons_json", reasonsJson).apply()
                    presetsRestored = true
                }
                val desigJson = presetsObj.optString("presetDesignationsJson", "")
                if (desigJson.isNotBlank() && desigJson != "[]") {
                    salaryPrefs.edit().putString("preset_designations_json", desigJson).apply()
                    presetsRestored = true
                }
            }

            // 4. Restore Preferences / Settings
            if (hasSettings) {
                val settingsObj = root.getJSONObject("settings")
                val fEditor = foodPrefs.edit()
                val sEditor = salaryPrefs.edit()

                if (settingsObj.has("saved_center_name_market")) {
                    fEditor.putString("saved_center_name_market", settingsObj.getString("saved_center_name_market"))
                }
                if (settingsObj.has("saved_subtitle_market")) {
                    fEditor.putString("saved_subtitle_market", settingsObj.getString("saved_subtitle_market"))
                }
                if (settingsObj.has("saved_purchaser_label_market")) {
                    fEditor.putString("saved_purchaser_label_market", settingsObj.getString("saved_purchaser_label_market"))
                }
                if (settingsObj.has("saved_center_name_transport")) {
                    fEditor.putString("saved_center_name_transport", settingsObj.getString("saved_center_name_transport"))
                }
                if (settingsObj.has("saved_subtitle_transport")) {
                    fEditor.putString("saved_subtitle_transport", settingsObj.getString("saved_subtitle_transport"))
                }
                if (settingsObj.has("saved_purchaser_label_transport")) {
                    fEditor.putString("saved_purchaser_label_transport", settingsObj.getString("saved_purchaser_label_transport"))
                }
                if (settingsObj.has("default_company_name")) {
                    sEditor.putString("default_company_name", settingsObj.getString("default_company_name"))
                }
                if (settingsObj.has("default_company_subtitle")) {
                    sEditor.putString("default_company_subtitle", settingsObj.getString("default_company_subtitle"))
                }
                if (settingsObj.has("app_theme_color")) {
                    fEditor.putString("app_theme_color", settingsObj.getString("app_theme_color"))
                }
                if (settingsObj.has("app_theme_mode")) {
                    fEditor.putString("app_theme_mode", settingsObj.getString("app_theme_mode"))
                }
                if (settingsObj.has("app_language")) {
                    fEditor.putString("app_language", settingsObj.getString("app_language"))
                }
                if (settingsObj.has("app_font_scale")) {
                    fEditor.putFloat("app_font_scale", settingsObj.getDouble("app_font_scale").toFloat())
                }
                fEditor.apply()
                sEditor.apply()
            }

            val msg = buildString {
                append("ডাটা সফলভাবে ইমপোর্ট হয়েছে! ")
                if (restoredBillsCount > 0) append("$restoredBillsCount টি মেমো, ")
                if (restoredSalariesCount > 0) append("$restoredSalariesCount টি স্যালারি রেকর্ড, ")
                if (restoredMedicalCount > 0) append("$restoredMedicalCount টি মেডিকেল রেকর্ড, ")
                if (presetsRestored) append("প্রিসেট ও সেটিংস ")
                append("স্ব-স্ব স্থানে যুক্ত হয়েছে।")
            }

            ImportResult(
                success = true,
                foodBillsCount = restoredBillsCount,
                advanceSalariesCount = restoredSalariesCount,
                medicalRecordsCount = restoredMedicalCount,
                presetsRestored = presetsRestored,
                message = msg
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ImportResult(
                success = false,
                message = "ইমপোর্ট ব্যর্থ হয়েছে: ${e.localizedMessage ?: "অজ্ঞাত সমস্যা"}"
            )
        }
    }

    /**
     * Saves backup directly to Downloads directory
     */
    suspend fun saveBackupToDownloads(context: Context, jsonString: String): String = withContext(Dispatchers.IO) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "digital_tools_backup_$timeStamp.json"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/DigitalTools")
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
                }
                return@withContext "ডাউনলোডস/DigitalTools/$fileName ফোল্ডারে সেভ হয়েছে"
            }
        }

        // Fallback for older Android or direct file write
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (downloadsDir != null && (downloadsDir.exists() || downloadsDir.mkdirs())) {
            val file = File(downloadsDir, fileName)
            FileOutputStream(file).use { it.write(jsonString.toByteArray(Charsets.UTF_8)) }
            return@withContext "ডাউনলোডস/$fileName ফোল্ডারে সেভ হয়েছে"
        }

        // Internal files fallback
        val internalFile = File(context.filesDir, fileName)
        FileOutputStream(internalFile).use { it.write(jsonString.toByteArray(Charsets.UTF_8)) }
        "ফাইলটি $fileName নামে সেভ হয়েছে"
    }

    /**
     * Shares backup file via standard Android share sheet
     */
    fun shareBackupFile(context: Context, jsonString: String) {
        try {
            val cacheDir = File(context.cacheDir, "backups").apply { mkdirs() }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(cacheDir, "digital_tools_backup_$timeStamp.json")
            FileOutputStream(file).use { it.write(jsonString.toByteArray(Charsets.UTF_8)) }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Digital Tools Data Backup")
                putExtra(Intent.EXTRA_TEXT, "Digital Tools App Data Backup ($timeStamp)")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "ব্যাকআপ ফাইলটি ডাউনলোড বা শেয়ার করুন"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
