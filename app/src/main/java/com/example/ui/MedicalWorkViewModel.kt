package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.CodeGroupEntity
import com.example.data.CodeGroupItemEntity
import com.example.data.MedicalRecordEntity
import com.example.data.MedicalRepository
import com.example.data.PresetMedicalCodeEntity
import com.example.util.BengaliUtils
import com.example.util.MedicalPrintUtils
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MedicalWorkViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MedicalRepository
    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent: SharedFlow<String> = _uiEvent.asSharedFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private val _selectedDate = MutableStateFlow(dateFormat.format(Date()))
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val allRecords: StateFlow<List<MedicalRecordEntity>>
    val recordsForSelectedDate: StateFlow<List<MedicalRecordEntity>>
    val presetCodes: StateFlow<List<PresetMedicalCodeEntity>>
    val codeGroups: StateFlow<List<CodeGroupEntity>>
    val groupItems: StateFlow<List<CodeGroupItemEntity>>
    val nextSuggestedPatientId: StateFlow<String>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = MedicalRepository(database.medicalDao())

        viewModelScope.launch {
            repository.seedDefaultPresetCodesIfEmpty()
            cleanInvalidPresetCodes()
        }

        allRecords = repository.allRecords.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        presetCodes = repository.allPresetCodes.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        codeGroups = repository.allCodeGroups.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        groupItems = repository.allGroupItems.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        recordsForSelectedDate = combine(allRecords, _selectedDate) { records, date ->
            records.filter { it.date == date }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        nextSuggestedPatientId = combine(recordsForSelectedDate, allRecords) { todayList, fullList ->
            val refRecord = todayList.lastOrNull() ?: fullList.lastOrNull()
            if (refRecord != null) {
                calculateNextId(refRecord.patientId)
            } else {
                // Default initial suggestion e.g. AB260948
                val cal = Calendar.getInstance()
                val yy = String.format(Locale.US, "%02d", cal.get(Calendar.YEAR) % 100)
                val mm = String.format(Locale.US, "%02d", cal.get(Calendar.MONTH) + 1)
                "AB$yy${mm}01"
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "AB260901"
        )

        viewModelScope.launch {
            cleanInvalidPresetCodes()
        }
    }

    fun calculateNextId(lastId: String): String {
        val trimmed = lastId.trim()
        if (trimmed.isBlank()) return "AB260901"
        val digits = trimmed.takeLastWhile { it.isDigit() }
        return if (digits.isNotEmpty()) {
            val prefix = trimmed.dropLast(digits.length)
            val nextNum = (digits.toLongOrNull() ?: 0L) + 1
            "$prefix${String.format(Locale.US, "%0${digits.length}d", nextNum)}"
        } else {
            "${trimmed}01"
        }
    }

    fun getNextIdAfter(startId: String, steps: Int = 1): String {
        var id = startId
        val iterations = if (steps < 1) 1 else steps
        repeat(iterations) {
            id = calculateNextId(id)
        }
        return id
    }

    fun setSelectedDate(date: String) {
        _selectedDate.value = date
    }

    fun setToday() {
        _selectedDate.value = dateFormat.format(Date())
    }

    fun shiftDate(days: Int) {
        try {
            val current = dateFormat.parse(_selectedDate.value) ?: Date()
            val cal = Calendar.getInstance().apply {
                time = current
                add(Calendar.DAY_OF_YEAR, days)
            }
            _selectedDate.value = dateFormat.format(cal.time)
        } catch (e: Exception) {
            _selectedDate.value = dateFormat.format(Date())
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addRecord(patientId: String, code: String, patientName: String = "", notes: String = "") {
        val pId = patientId.trim()
        val c = code.trim()
        val n = patientName.trim()

        if (pId.isBlank() && c.isBlank()) {
            viewModelScope.launch { _uiEvent.emit("আইডি বা কোড অন্তত একটি পূরণ করুন") }
            return
        }

        // If JSON was pasted into ID or Code field
        if (c.startsWith("{") || c.startsWith("[") || pId.startsWith("{") || pId.startsWith("[")) {
            val jsonText = if (c.startsWith("{") || c.startsWith("[")) c else pId
            bulkAddFromText(jsonText)
            return
        }

        val pIdUpper = pId.uppercase()
        val cUpper = c.uppercase()
        val nUpper = n.uppercase()

        viewModelScope.launch {
            // Check if code contains multiple codes separated by comma, semicolon or newline
            val codeTokens = cUpper.split(Regex("[,;\\n\\r]+")).map { it.trim() }.filter { it.isNotBlank() }
            if (codeTokens.size > 1) {
                val recordsToAdd = mutableListOf<MedicalRecordEntity>()
                val presetCodesToAdd = mutableListOf<PresetMedicalCodeEntity>()
                var currentId = pIdUpper.ifBlank { nextSuggestedPatientId.value }

                for (singleCode in codeTokens) {
                    recordsToAdd.add(
                        MedicalRecordEntity(
                            date = _selectedDate.value,
                            patientId = currentId,
                            code = singleCode,
                            patientName = nUpper,
                            notes = notes.trim()
                        )
                    )
                    if (isValidPresetCode(singleCode)) {
                        presetCodesToAdd.add(
                            PresetMedicalCodeEntity(
                                code = singleCode,
                                name = "",
                                category = "General"
                            )
                        )
                    }
                    currentId = calculateNextId(currentId)
                }
                repository.saveRecords(recordsToAdd)
                if (presetCodesToAdd.isNotEmpty()) {
                    repository.savePresetCodes(presetCodesToAdd)
                }
                _uiEvent.emit("${recordsToAdd.size} টি রেকর্ড সফলভাবে যোগ হয়েছে!")
            } else {
                val finalCode = codeTokens.firstOrNull() ?: cUpper
                val finalId = pIdUpper.ifBlank { nextSuggestedPatientId.value }
                val record = MedicalRecordEntity(
                    date = _selectedDate.value,
                    patientId = finalId,
                    code = finalCode,
                    patientName = nUpper,
                    notes = notes.trim()
                )
                repository.saveRecord(record)
                if (finalCode.isNotBlank() && isValidPresetCode(finalCode)) {
                    repository.savePresetCode(
                        PresetMedicalCodeEntity(
                            code = finalCode,
                            name = "",
                            category = "General"
                        )
                    )
                }
                _uiEvent.emit("রেকর্ড সফলভাবে যোগ হয়েছে: $finalId")
            }
        }
    }

    fun updateRecord(id: Long, patientId: String, code: String, patientName: String, notes: String = "") {
        viewModelScope.launch {
            val record = MedicalRecordEntity(
                id = id,
                date = _selectedDate.value,
                patientId = patientId.trim().uppercase(),
                code = code.trim().uppercase(),
                patientName = patientName.trim().uppercase(),
                notes = notes.trim()
            )
            repository.updateRecord(record)
            _uiEvent.emit("রেকর্ড আপডেট হয়েছে")
        }
    }

    fun deleteRecord(id: Long) {
        viewModelScope.launch {
            repository.deleteRecord(id)
            _uiEvent.emit("রেকর্ড মুছে ফেলা হয়েছে")
        }
    }

    fun deleteRecordsForCurrentDate() {
        viewModelScope.launch {
            repository.deleteRecordsByDate(_selectedDate.value)
            _uiEvent.emit("${_selectedDate.value} এর সব রেকর্ড মুছে ফেলা হয়েছে")
        }
    }

    fun generateAutoSequence(startId: String, count: Int, defaultCode: String, defaultName: String = "") {
        if (startId.isBlank() || count <= 0) {
            viewModelScope.launch { _uiEvent.emit("সঠিক প্রারম্ভিক আইডি এবং সংখ্যা দিন") }
            return
        }

        viewModelScope.launch {
            val recordsToAdd = mutableListOf<MedicalRecordEntity>()
            var currentId = startId.trim().uppercase()
            val codeClean = defaultCode.trim().uppercase()
            val nameClean = defaultName.trim().uppercase()

            for (i in 0 until count) {
                recordsToAdd.add(
                    MedicalRecordEntity(
                        date = _selectedDate.value,
                        patientId = currentId,
                        code = codeClean,
                        patientName = nameClean
                    )
                )
                currentId = calculateNextId(currentId)
            }

            repository.saveRecords(recordsToAdd)
            _uiEvent.emit("$count টি সিরিয়াল এন্ট্রি সফলভাবে তৈরি হয়েছে")
        }
    }

    fun bulkAddFromText(rawText: String) {
        val trimmed = rawText.trim()
        if (trimmed.isBlank()) {
            viewModelScope.launch { _uiEvent.emit("পেস্ট করার জন্য কোনো টেক্সট পাওয়া যায়নি") }
            return
        }

        viewModelScope.launch {
            // Check if text looks like JSON format
            val hasJsonTokens = (trimmed.contains("{") && trimmed.contains("}")) ||
                    (trimmed.contains("[") && trimmed.contains("]"))

            if (hasJsonTokens) {
                if (tryParseAndImportJson(trimmed)) {
                    return@launch
                } else {
                    _uiEvent.emit("JSON ফরম্যাটটি সঠিকভাবে পড়া সম্ভব হয়নি বা কোনো বৈধ রেকর্ড পাওয়া যায়নি")
                    return@launch
                }
            }

            // Determine line separator or item separator for plain text
            val rawLines = if (trimmed.contains("\n")) {
                trimmed.split("\n").map { it.trim() }.filter { it.isNotBlank() }
            } else if (trimmed.contains(",") || trimmed.contains(";")) {
                trimmed.split(Regex("[,;]+")).map { it.trim() }.filter { it.isNotBlank() }
            } else {
                listOf(trimmed)
            }

            val records = mutableListOf<MedicalRecordEntity>()
            val presetCodesToAdd = mutableListOf<PresetMedicalCodeEntity>()
            var currentNextId = nextSuggestedPatientId.value

            for (line in rawLines) {
                // Strip leading serial numbers like "1.", "১.", "1)", "#1", "-", "•"
                val cleanedLine = line.replace(Regex("^(?:\\d+|[০-৯]+)[.)\\-:]\\s*"), "")
                    .replace(Regex("^[•\\-*#]\\s*"), "")
                    .trim()

                if (cleanedLine.isBlank()) continue

                // Check tokens separated by tab, comma, semicolon, or 2+ spaces, or pipe "|"
                val parts = cleanedLine.split(Regex("[\\t,;|]+|\\s{2,}")).map { it.trim() }.filter { it.isNotBlank() }

                // If line was just single spaced
                val rawTokens = if (parts.size <= 1) {
                    cleanedLine.split(Regex("\\s+")).map { it.trim() }.filter { it.isNotBlank() }
                } else {
                    parts
                }

                // Clean punctuation from tokens
                val tokens = rawTokens.map { it.replace(Regex("""^["'`]+|["'`,;:]+$"""), "").trim() }.filter { it.isNotBlank() }

                if (tokens.isEmpty()) continue

                // Check if any token looks like a Patient ID (starts with letters + digits, e.g. AB260901, or 5+ digits)
                val idTokenIndex = tokens.indexOfFirst { token ->
                    token.matches(Regex("(?i)^[A-Z]{1,4}\\d{4,8}$")) ||
                    token.matches(Regex("^\\d{5,10}$"))
                }

                if (idTokenIndex != -1) {
                    // ID is present
                    val pId = tokens[idTokenIndex].uppercase()
                    val otherTokens = tokens.filterIndexed { index, _ -> index != idTokenIndex }

                    val codeToken = otherTokens.firstOrNull() ?: "101"
                    val nameTokens = otherTokens.drop(1).joinToString(" ")

                    records.add(
                        MedicalRecordEntity(
                            date = _selectedDate.value,
                            patientId = pId,
                            code = codeToken.uppercase(),
                            patientName = nameTokens.uppercase()
                        )
                    )
                    if (isValidPresetCode(codeToken.uppercase())) {
                        presetCodesToAdd.add(
                            PresetMedicalCodeEntity(
                                code = codeToken.uppercase(),
                                name = "",
                                category = "General"
                            )
                        )
                    }
                } else {
                    // No explicit Patient ID found in this line/item!
                    // This means the user pasted JUST CODES (e.g. "AF07", "MD-01", "101", "CBC, USG")
                    for (token in tokens) {
                        val codeVal = token.uppercase()
                        if (codeVal.all { it in "{}[]():;\"',=<>*^$" }) continue
                        records.add(
                            MedicalRecordEntity(
                                date = _selectedDate.value,
                                patientId = currentNextId,
                                code = codeVal,
                                patientName = ""
                            )
                        )
                        if (isValidPresetCode(codeVal)) {
                            presetCodesToAdd.add(
                                PresetMedicalCodeEntity(
                                    code = codeVal,
                                    name = "",
                                    category = "General"
                                )
                            )
                        }
                        currentNextId = calculateNextId(currentNextId)
                    }
                }
            }

            if (records.isNotEmpty()) {
                repository.saveRecords(records)
                if (presetCodesToAdd.isNotEmpty()) {
                    val existingPresets = repository.getAllPresetCodesList().associateBy { it.code.uppercase(Locale.ROOT) }
                    val uniquePresets = presetCodesToAdd.distinctBy { it.code.uppercase(Locale.ROOT) }
                    val mergedPresets = uniquePresets.map { newPreset ->
                        val existing = existingPresets[newPreset.code.uppercase(Locale.ROOT)]
                        if (existing != null && newPreset.name.isBlank() && existing.name.isNotBlank()) {
                            newPreset.copy(name = existing.name, category = existing.category)
                        } else {
                            newPreset
                        }
                    }
                    repository.savePresetCodes(mergedPresets)
                }
                _uiEvent.emit("${records.size} টি এন্ট্রি সফলভাবে ইমপোর্ট হয়েছে এবং কোডগুলো শর্টকাট তালিকায় যোগ হয়েছে!")
            } else {
                _uiEvent.emit("কোনো বৈধ এন্ট্রি চিহ্নিত করা যায়নি")
            }
        }
    }

    private suspend fun tryParseAndImportJson(rawText: String): Boolean {
        var jsonText = rawText.trim()
        if (jsonText.contains("```")) {
            jsonText = jsonText.replace(Regex("""```(?:json)?[\r\n]*""", RegexOption.IGNORE_CASE), "")
                .replace("```", "")
                .trim()
        }

        val hasBraces = jsonText.contains("{") && jsonText.contains("}")
        val hasBrackets = jsonText.contains("[") && jsonText.contains("]")
        if (!hasBraces && !hasBrackets) {
            return false
        }

        try {
            // 1. Sanitize relaxed JSON syntax
            var sanitized = jsonText
            // Strip single-line comments // ...
            sanitized = sanitized.replace(Regex("""(?m)^\s*//.*$"""), "")
            sanitized = sanitized.replace(Regex("""(?<=\s)//.*$"""), "")
            // Strip multi-line comments /* ... */
            sanitized = sanitized.replace(Regex("""/\*[\s\S]*?\*/"""), "")
            // Strip trailing commas before } or ]
            sanitized = sanitized.replace(Regex(""",\s*([}\]])"""), "$1")
            // Convert single quotes to double quotes if no double quotes exist
            if (!sanitized.contains("\"") && sanitized.contains("'")) {
                sanitized = sanitized.replace('\'', '"')
            }

            val firstBrace = sanitized.indexOf('{')
            val firstBracket = sanitized.indexOf('[')
            val startIdx = when {
                firstBrace != -1 && firstBracket != -1 -> minOf(firstBrace, firstBracket)
                firstBrace != -1 -> firstBrace
                firstBracket != -1 -> firstBracket
                else -> -1
            }

            val lastBrace = sanitized.lastIndexOf('}')
            val lastBracket = sanitized.lastIndexOf(']')
            val endIdx = maxOf(lastBrace, lastBracket)

            if (startIdx == -1 || endIdx == -1 || endIdx <= startIdx) {
                return false
            }

            val jsonSubstring = sanitized.substring(startIdx, endIdx + 1).trim()

            var targetDate = _selectedDate.value
            var dateChanged = false
            val presetCodesToAdd = mutableListOf<PresetMedicalCodeEntity>()
            val itemsList = mutableListOf<Any>() // JSONObject, String, or JSONArray

            var parsedOk = false

            if (jsonSubstring.startsWith("[")) {
                try {
                    val rootArr = JSONArray(jsonSubstring)
                    for (i in 0 until rootArr.length()) {
                        val itm = rootArr.opt(i) ?: continue
                        itemsList.add(itm)
                    }
                    parsedOk = true
                } catch (e: Exception) {
                    // Fallback will handle
                }
            } else if (jsonSubstring.startsWith("{")) {
                try {
                    // Check if it is multiple concatenated objects e.g. {"a":1}{"b":2}
                    if (jsonSubstring.contains("}\n{") || jsonSubstring.contains("} {") || jsonSubstring.contains("},\n{") || jsonSubstring.contains("},{")) {
                        val objectPattern = Regex("""\{[^{}]*(?:\{[^{}]*\}[^{}]*)*\}""")
                        val matches = objectPattern.findAll(jsonSubstring).toList()
                        if (matches.size > 1) {
                            for (m in matches) {
                                try {
                                    val obj = JSONObject(m.value)
                                    itemsList.add(obj)
                                } catch (e: Exception) { /* ignore */ }
                            }
                            if (itemsList.isNotEmpty()) parsedOk = true
                        }
                    }

                    if (!parsedOk) {
                        val rootObj = JSONObject(jsonSubstring)
                        parsedOk = true

                        // Check root date
                        val dateKeys = listOf("date", "Date", "DATE", "report_date", "reportDate", "entry_date", "entryDate", "তারিখ", "রিপোর্ট তারিখ")
                        for (key in dateKeys) {
                            if (rootObj.has(key) && !rootObj.isNull(key)) {
                                val parsed = parseDateToIso(rootObj.optString(key))
                                if (parsed != null) {
                                    targetDate = parsed
                                    dateChanged = true
                                    break
                                }
                            }
                        }

                        // Check direct preset code lists
                        val directCodeKeys = listOf("presetMedicalCodes", "presetCodes", "preset_codes", "codes", "allCodes", "codeList", "কোডসমূহ")
                        for (k in directCodeKeys) {
                            val arr = rootObj.optJSONArray(k) ?: continue
                            for (idx in 0 until arr.length()) {
                                val entry = arr.opt(idx) ?: continue
                                if (entry is JSONObject) {
                                    val c = entry.optString("code").ifBlank { entry.optString("test") }.trim()
                                    val n = entry.optString("name").ifBlank { entry.optString("description") }.trim()
                                    if (c.isNotBlank() && isValidPresetCode(c)) {
                                        presetCodesToAdd.add(
                                            PresetMedicalCodeEntity(
                                                code = c.uppercase(Locale.ROOT),
                                                name = if (c.equals(n, ignoreCase = true)) "" else n,
                                                category = entry.optString("category", "General")
                                            )
                                        )
                                    }
                                } else if (entry is String && isValidPresetCode(entry)) {
                                    presetCodesToAdd.add(
                                        PresetMedicalCodeEntity(
                                            code = entry.trim().uppercase(Locale.ROOT),
                                            name = "",
                                            category = "General"
                                        )
                                    )
                                }
                            }
                        }

                        // Find records array inside rootObj
                        val arrayKeys = listOf(
                            "data", "records", "medicalRecords", "medical_records", "rows", "items", "patients",
                            "list", "entries", "table", "result", "results", "patient_list", "patientList",
                            "test_list", "tests", "testList", "investigations", "daily_records", "dailyRecords",
                            "তথ্য", "রিপোর্ট", "তালিকা"
                        )

                        var foundArray: JSONArray? = null
                        for (key in arrayKeys) {
                            if (rootObj.has(key) && !rootObj.isNull(key)) {
                                val arr = rootObj.optJSONArray(key)
                                if (arr != null && arr.length() > 0) {
                                    foundArray = arr
                                    break
                                }
                            }
                        }

                        // Fallback: check any other key in rootObj that holds a non-empty JSONArray
                        if (foundArray == null) {
                            val keys = rootObj.keys()
                            while (keys.hasNext()) {
                                val k = keys.next()
                                if (k !in directCodeKeys) {
                                    val arr = rootObj.optJSONArray(k)
                                    if (arr != null && arr.length() > 0) {
                                        foundArray = arr
                                        break
                                    }
                                }
                            }
                        }

                        if (foundArray != null) {
                            for (i in 0 until foundArray.length()) {
                                val itm = foundArray.opt(i) ?: continue
                                itemsList.add(itm)
                            }
                        } else {
                            // Check if rootObj ITSELF represents a single record
                            val isSingleRecord = rootObj.has("patientId") || rootObj.has("patient_id") ||
                                    rootObj.has("code") || rootObj.has("test") || rootObj.has("test_code") ||
                                    rootObj.has("patientName") || rootObj.has("patient_name") ||
                                    rootObj.has("id") || rootObj.has("আইডি") || rootObj.has("কোড")
                            if (isSingleRecord) {
                                itemsList.add(rootObj)
                            } else {
                                // Check if rootObj is a map: e.g. {"AB260901": "AF07"} or {"AB260901": {...}}
                                val keys = rootObj.keys()
                                while (keys.hasNext()) {
                                    val k = keys.next()
                                    val v = rootObj.opt(k) ?: continue
                                    if (v is JSONObject) {
                                        if (!v.has("patientId") && !v.has("id")) {
                                            v.put("patientId", k)
                                        }
                                        itemsList.add(v)
                                    } else if (v is String) {
                                        val obj = JSONObject()
                                        obj.put("patientId", k)
                                        obj.put("code", v)
                                        itemsList.add(obj)
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Fallback will handle
                }
            }

            // Fallback: regex-based extraction of JSON blocks if org.json parsing failed
            if (!parsedOk || itemsList.isEmpty()) {
                val objectRegex = Regex("""\{([^{}]+)\}""")
                val matches = objectRegex.findAll(sanitized).toList()
                for (m in matches) {
                    try {
                        val obj = JSONObject("{${m.groupValues[1]}}")
                        itemsList.add(obj)
                    } catch (e: Exception) {
                        val kvRegex = Regex(""""?([a-zA-Z0-9_\u0980-\u09FF]+)"?\s*:\s*(?:"([^"]*)"|'([^']*)'|([^,}\s]+))""")
                        val manualObj = JSONObject()
                        for (kv in kvRegex.findAll(m.value)) {
                            val key = kv.groupValues[1].trim()
                            val value = (kv.groupValues[2].ifBlank { kv.groupValues[3] }.ifBlank { kv.groupValues[4] }).trim()
                            manualObj.put(key, value)
                        }
                        if (manualObj.length() > 0) {
                            itemsList.add(manualObj)
                        }
                    }
                }
            }

            if (itemsList.isEmpty() && presetCodesToAdd.isEmpty()) {
                return false
            }

            val recordsToAdd = mutableListOf<MedicalRecordEntity>()
            var currentNextId = nextSuggestedPatientId.value
            var primaryImportDate: String? = if (dateChanged) targetDate else null

            val idKeys = listOf(
                "patientId", "patient_id", "patientID", "patient_Id", "pt_id", "ptid", "ptId", "p_id", "pid", "pId",
                "mrn", "mr_no", "mrNo", "reg_no", "regNo", "registration_no", "registrationNo", "uhid",
                "আইডি", "পেশেন্ট আইডি", "রোগী আইডি", "রোগীর আইডি", "রেজিস্ট্রেশন",
                "Patient ID", "patient id", "Patient Id", "PatientID",
                "id", "ID", "Id"
            )

            val codeKeys = listOf(
                "code", "Code", "CODE",
                "test_code", "testCode", "testCodes", "test_codes",
                "medical_code", "medicalCode", "short_code", "shortCode",
                "test", "tests", "Test", "Tests",
                "investigation", "investigations", "Investigation", "Investigations",
                "item", "items", "service", "services",
                "procedure", "procedures", "exam", "exams",
                "কোড", "টেস্ট", "পরীক্ষা", "বিবরণ"
            )

            val nameKeys = listOf(
                "patientName", "patient_name", "patient_Name",
                "name", "Name", "NAME", "pt_name", "patient", "client", "customer",
                "রোগীর নাম", "রোগী", "নাম", "full_name", "fullName"
            )

            val dateFieldKeys = listOf(
                "date", "Date", "DATE", "report_date", "reportDate", "entry_date", "entryDate",
                "তারিখ", "বিল তারিখ", "রিপোর্ট তারিখ"
            )

            for (item in itemsList) {
                if (item is String) {
                    val trimmedCode = item.trim()
                    if (trimmedCode.isNotBlank()) {
                        val upperCode = trimmedCode.uppercase(Locale.ROOT)
                        if (isValidPresetCode(upperCode)) {
                            presetCodesToAdd.add(PresetMedicalCodeEntity(code = upperCode, name = "", category = "General"))
                        }
                        recordsToAdd.add(
                            MedicalRecordEntity(
                                id = 0,
                                date = targetDate,
                                patientId = currentNextId,
                                code = upperCode,
                                patientName = "",
                                notes = ""
                            )
                        )
                        currentNextId = calculateNextId(currentNextId)
                    }
                    continue
                }

                var patientId = ""
                var code = ""
                var patientName = ""
                var notes = ""
                var itemDate = targetDate
                var codeDescription = ""

                if (item is JSONObject) {
                    // Extract Date
                    for (dk in dateFieldKeys) {
                        if (item.has(dk) && !item.isNull(dk)) {
                            val parsed = parseDateToIso(item.optString(dk))
                            if (parsed != null) {
                                itemDate = parsed
                                if (primaryImportDate == null) {
                                    primaryImportDate = parsed
                                }
                                break
                            }
                        }
                    }

                    // Extract ID
                    for (k in idKeys) {
                        if (item.has(k) && !item.isNull(k)) {
                            val v = item.optString(k).trim()
                            if (v.isNotBlank()) {
                                if ((k == "id" || k == "ID" || k == "Id") && v.length <= 3 && v.all { it.isDigit() }) {
                                    val otherId = idKeys.filter { it != "id" && it != "ID" && it != "Id" }
                                        .firstOrNull { item.has(it) && item.optString(it).isNotBlank() }
                                    if (otherId != null) {
                                        patientId = item.optString(otherId).trim()
                                        break
                                    }
                                }
                                patientId = v
                                break
                            }
                        }
                    }

                    // Extract Code
                    for (k in codeKeys) {
                        if (item.has(k) && !item.isNull(k)) {
                            val opt = item.opt(k)
                            if (opt is JSONArray) {
                                val list = mutableListOf<String>()
                                for (idx in 0 until opt.length()) {
                                    val sub = opt.opt(idx)
                                    if (sub is JSONObject) {
                                        val subCode = sub.optString("code").ifBlank { sub.optString("test") }.trim()
                                        if (subCode.isNotBlank()) list.add(subCode)
                                    } else if (sub != null) {
                                        val s = sub.toString().trim()
                                        if (s.isNotBlank()) list.add(s)
                                    }
                                }
                                code = list.joinToString(", ")
                            } else if (opt is JSONObject) {
                                code = opt.optString("code").ifBlank { opt.optString("test") }.trim()
                            } else {
                                code = item.optString(k).trim()
                            }
                            if (code.isNotBlank()) break
                        }
                    }

                    // Extract Name
                    for (k in nameKeys) {
                        if (item.has(k) && !item.isNull(k)) {
                            patientName = item.optString(k).trim()
                            if (patientName.isNotBlank()) break
                        }
                    }

                    // Extract Notes
                    val noteKeys = listOf("notes", "note", "remarks", "মন্তব্য")
                    for (k in noteKeys) {
                        if (item.has(k) && !item.isNull(k)) {
                            notes = item.optString(k).trim()
                            if (notes.isNotBlank()) break
                        }
                    }

                    // Extract Description
                    val descKeys = listOf("codeName", "code_name", "testName", "test_name", "description", "বিবরণ")
                    for (dk in descKeys) {
                        if (item.has(dk) && !item.isNull(dk)) {
                            codeDescription = item.optString(dk).trim()
                            if (codeDescription.isNotBlank()) break
                        }
                    }

                    // Fallback key search if exact keys not matched
                    if (patientId.isBlank() || code.isBlank()) {
                        val keys = item.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            val kNorm = k.trim().lowercase(Locale.ROOT)
                            val v = item.optString(k).trim()
                            if (v.isBlank()) continue
                            if (patientId.isBlank() && (kNorm == "id" || kNorm.contains("id") || k.contains("আইডি"))) {
                                patientId = v
                            } else if (code.isBlank() && (kNorm == "code" || kNorm.contains("code") || k.contains("কোড") || kNorm.contains("test") || kNorm.contains("investigation"))) {
                                code = v
                            } else if (patientName.isBlank() && (kNorm == "name" || kNorm.contains("name") || k.contains("নাম"))) {
                                patientName = v
                            }
                        }
                    }
                } else if (item is JSONArray) {
                    val list = mutableListOf<String>()
                    for (j in 0 until item.length()) {
                        list.add(item.optString(j).trim())
                    }
                    val nonSerial = list.filterIndexed { index, s ->
                        !(index == 0 && s.matches(Regex("""^(?:\d+|[০-৯]+)$""")))
                    }
                    if (nonSerial.isNotEmpty()) {
                        patientId = nonSerial.firstOrNull() ?: ""
                        code = nonSerial.getOrNull(1) ?: ""
                        patientName = nonSerial.drop(2).joinToString(" ")
                    }
                }

                if (patientId.isBlank()) {
                    patientId = currentNextId
                    currentNextId = calculateNextId(currentNextId)
                } else {
                    currentNextId = calculateNextId(patientId)
                }

                if (code.isBlank()) {
                    code = "101"
                }

                val cleanPatientId = patientId.uppercase(Locale.ROOT)
                val cleanCode = code.uppercase(Locale.ROOT)
                val cleanName = patientName.uppercase(Locale.ROOT)

                recordsToAdd.add(
                    MedicalRecordEntity(
                        id = 0,
                        date = itemDate,
                        patientId = cleanPatientId,
                        code = cleanCode,
                        patientName = cleanName,
                        notes = notes
                    )
                )

                // ONLY valid preset codes go to shortcuts:
                // IDs, patient names, dates are strictly rejected
                if (isValidPresetCode(cleanCode)) {
                    presetCodesToAdd.add(
                        PresetMedicalCodeEntity(
                            code = cleanCode,
                            name = codeDescription,
                            category = "General"
                        )
                    )
                }

                // Also split compound codes (e.g. "AF07, MD-01", "CBC/USG")
                val subTokens = cleanCode.split(Regex("[,;\\n\\r/]+"))
                    .map { it.replace(Regex("""[()[\]{}]"""), " ").trim() }
                    .flatMap { it.split(Regex("\\s+")) }
                    .map { it.trim().uppercase(Locale.ROOT) }
                    .filter { isValidPresetCode(it) && it != "DUE" }

                for (sub in subTokens) {
                    presetCodesToAdd.add(
                        PresetMedicalCodeEntity(
                            code = sub,
                            name = "",
                            category = "General"
                        )
                    )
                }
            }

            // Save preset codes to database (strictly valid codes only)
            var newPresetCount = 0
            if (presetCodesToAdd.isNotEmpty()) {
                val validPresets = presetCodesToAdd.filter { isValidPresetCode(it.code) }
                val existingPresets = repository.getAllPresetCodesList().associateBy { it.code.uppercase(Locale.ROOT) }
                val uniquePresets = validPresets.distinctBy { it.code.uppercase(Locale.ROOT) }
                val mergedPresets = uniquePresets.map { newPreset ->
                    val existing = existingPresets[newPreset.code.uppercase(Locale.ROOT)]
                    if (existing != null && newPreset.name.isBlank() && existing.name.isNotBlank()) {
                        newPreset.copy(name = existing.name, category = existing.category)
                    } else {
                        newPreset
                    }
                }
                newPresetCount = mergedPresets.count { !existingPresets.containsKey(it.code.uppercase(Locale.ROOT)) }
                repository.savePresetCodes(mergedPresets)
            }

            if (recordsToAdd.isNotEmpty()) {
                repository.saveRecords(recordsToAdd)
                val targetDisplayDate = primaryImportDate ?: targetDate
                _selectedDate.value = targetDisplayDate

                val formattedDate = MedicalPrintUtils.formatDateShort(targetDisplayDate)
                val countBn = BengaliUtils.toBengaliDigits(recordsToAdd.size.toString())
                val newCodesBn = BengaliUtils.toBengaliDigits(newPresetCount.toString())
                val presetInfo = if (newPresetCount > 0) " এবং $newCodesBn টি নতুন কোড শর্টকাটে যুক্ত হয়েছে" else ""
                _uiEvent.emit("JSON থেকে $countBn টি এন্ট্রি সফলভাবে টেবিলে যুক্ত হয়েছে$presetInfo ($formattedDate)!")
                return true
            } else if (presetCodesToAdd.isNotEmpty()) {
                val newCodesBn = BengaliUtils.toBengaliDigits(newPresetCount.toString())
                val totalBn = BengaliUtils.toBengaliDigits(presetCodesToAdd.distinctBy { it.code }.size.toString())
                _uiEvent.emit("JSON থেকে $totalBn টি কোড পাওয়া গেছে ($newCodesBn টি নতুন কোড শর্টকাটে যোগ হয়েছে)!")
                return true
            }
        } catch (e: Exception) {
            return false
        }
        return false
    }

    private fun parseDateToIso(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val clean = BengaliUtils.toEnglishDigits(raw).trim()

        // 1. Check YYYY-MM-DD or YYYY/MM/DD
        val ymdMatch = Regex("""^(\d{4})[-/](\d{1,2})[-/](\d{1,2})$""").find(clean)
        if (ymdMatch != null) {
            val (y, m, d) = ymdMatch.destructured
            return String.format(Locale.US, "%04d-%02d-%02d", y.toInt(), m.toInt(), d.toInt())
        }

        // 2. Check DD/MM/YYYY or DD-MM-YYYY or DD.MM.YYYY
        val dmyLongMatch = Regex("""^(\d{1,2})[-/. ](\d{1,2})[-/. ](\d{4})$""").find(clean)
        if (dmyLongMatch != null) {
            val (d, m, y) = dmyLongMatch.destructured
            return String.format(Locale.US, "%04d-%02d-%02d", y.toInt(), m.toInt(), d.toInt())
        }

        // 3. Check DD/MM/YY or DD-MM-YY or DD.MM.YY (e.g. 10/09/26)
        val dmyShortMatch = Regex("""^(\d{1,2})[-/. ](\d{1,2})[-/. ](\d{2})$""").find(clean)
        if (dmyShortMatch != null) {
            val (d, m, y) = dmyShortMatch.destructured
            val yearFull = 2000 + y.toInt()
            return String.format(Locale.US, "%04d-%02d-%02d", yearFull, m.toInt(), d.toInt())
        }

        return null
    }

    companion object {
        fun isValidPresetCode(code: String): Boolean {
            val trimmed = code.trim()
            if (trimmed.length < 2 || trimmed.length > 30) return false

            // Reject JSON syntax, brackets, braces, colons, quotes
            if (trimmed.any { it in "{}[];:\"\\=<>*^$" }) return false

            val cleanUpper = BengaliUtils.toEnglishDigits(trimmed.uppercase(Locale.ROOT)).trim()

            // Reject JSON field names, boolean values, common record keys
            val forbiddenKeywords = setOf(
                "ID", "PATIENTID", "PATIENT_ID", "PATIENT ID", "NAME", "PATIENTNAME",
                "PATIENT_NAME", "PATIENT NAME", "DATE", "DATA", "RECORDS", "RECORD",
                "NOTES", "NOTE", "TIMESTAMP", "CATEGORY", "TESTS", "TEST", "CODE",
                "CODES", "ITEM", "ITEMS", "NULL", "TRUE", "FALSE", "UNDEFINED",
                "OBJECT", "ARRAY", "LIST", "ROW", "ROWS", "ENTRY", "ENTRIES",
                "আইডি", "নাম", "তারিখ", "কোড", "মন্তব্য", "টেস্ট", "রোগীর নাম", "বিবরণ"
            )
            if (cleanUpper in forbiddenKeywords) return false

            // Reject patient IDs like AB260901, AB260948, PID1020, or pure numbers with 4+ digits
            if (cleanUpper.matches(Regex("^[A-Z]{1,4}\\d{4,9}$")) || cleanUpper.matches(Regex("^\\d{4,12}$"))) {
                return false
            }

            // Reject dates e.g. 2026-09-12 or 12/09/2026
            if (cleanUpper.matches(Regex("^\\d{4}[-/.]\\d{1,2}[-/.]\\d{1,2}$")) || cleanUpper.matches(Regex("^\\d{1,2}[-/.]\\d{1,2}[-/.]\\d{2,4}$"))) {
                return false
            }

            // Reject person names starting with typical honorifics
            val namePrefixes = listOf("MD ", "MD. ", "MST ", "MST. ", "MR ", "MR. ", "MRS ", "MRS. ", "DR ", "DR. ", "MOHAMMAD ")
            if (namePrefixes.any { cleanUpper.startsWith(it) }) {
                return false
            }

            return true
        }

        fun extractCodesOnlyFromJson(jsonStr: String): List<String> {
            val extractedCodes = mutableListOf<String>()
            try {
                val trimmed = jsonStr.trim()
                val startIdx = trimmed.indexOfAny(charArrayOf('{', '['))
                val endIdx = trimmed.lastIndexOfAny(charArrayOf('}', ']'))
                if (startIdx == -1 || endIdx <= startIdx) return emptyList()
                val jsonCleaned = trimmed.substring(startIdx, endIdx + 1)

                fun extractFromObject(obj: JSONObject) {
                    val codeKeys = listOf(
                        "code", "কোড", "Code", "CODE", "testCode", "test_code",
                        "testCodes", "test_codes", "medical_code", "short_code",
                        "shortCode", "test", "tests", "টেস্ট", "পরীক্ষা"
                    )
                    for (k in codeKeys) {
                        if (obj.has(k) && !obj.isNull(k)) {
                            val opt = obj.opt(k)
                            if (opt is JSONArray) {
                                for (idx in 0 until opt.length()) {
                                    val c = opt.optString(idx).trim()
                                    if (isValidPresetCode(c)) extractedCodes.add(c)
                                }
                            } else if (opt != null) {
                                val c = opt.toString().trim()
                                if (c.isNotBlank()) {
                                    val parts = c.split(Regex("[,;/]+"))
                                    for (p in parts) {
                                        val cleanP = p.trim()
                                        if (isValidPresetCode(cleanP)) extractedCodes.add(cleanP)
                                    }
                                }
                            }
                            return
                        }
                    }
                }

                if (jsonCleaned.startsWith("{")) {
                    val rootObj = JSONObject(jsonCleaned)
                    val directCodeKeys = listOf("codes", "presetCodes", "preset_codes", "medical_codes", "shortcuts", "কোডসমূহ")
                    for (dk in directCodeKeys) {
                        val arr = rootObj.optJSONArray(dk) ?: continue
                        for (i in 0 until arr.length()) {
                            val item = arr.opt(i) ?: continue
                            if (item is JSONObject) extractFromObject(item)
                            else if (item is String && isValidPresetCode(item)) extractedCodes.add(item.trim())
                        }
                    }

                    val arrayKeys = listOf("data", "records", "rows", "items", "patients", "list", "entries", "তথ্য")
                    for (ak in arrayKeys) {
                        val arr = rootObj.optJSONArray(ak) ?: continue
                        for (i in 0 until arr.length()) {
                            val item = arr.opt(i) ?: continue
                            if (item is JSONObject) extractFromObject(item)
                        }
                    }

                    val keys = rootObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        if (k !in directCodeKeys && k !in arrayKeys) {
                            val arr = rootObj.optJSONArray(k) ?: continue
                            for (i in 0 until arr.length()) {
                                val item = arr.opt(i) ?: continue
                                if (item is JSONObject) extractFromObject(item)
                            }
                        }
                    }
                } else if (jsonCleaned.startsWith("[")) {
                    val rootArr = JSONArray(jsonCleaned)
                    for (i in 0 until rootArr.length()) {
                        val item = rootArr.opt(i) ?: continue
                        if (item is JSONObject) {
                            extractFromObject(item)
                        } else if (item is String && isValidPresetCode(item)) {
                            extractedCodes.add(item.trim())
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore parse exceptions
            }
            return extractedCodes.filter { isValidPresetCode(it) }.distinctBy { it.uppercase(Locale.ROOT) }
        }
    }

    suspend fun cleanInvalidPresetCodes() {
        try {
            val currentPresets = repository.getAllPresetCodesList()
            val invalidCodes = currentPresets.filter { !isValidPresetCode(it.code) }
            for (invalid in invalidCodes) {
                repository.deletePresetCode(invalid.code)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun addMultiplePresetCodes(rawCodesText: String) {
        val trimmed = rawCodesText.trim()
        if (trimmed.isBlank()) {
            viewModelScope.launch { _uiEvent.emit("কোনো কোড পাওয়া যায়নি") }
            return
        }

        viewModelScope.launch {
            // Check if input is JSON (e.g. user pasted JSON file text)
            val jsonCodes = if (trimmed.contains("{") || trimmed.contains("[")) {
                extractCodesOnlyFromJson(trimmed)
            } else {
                emptyList()
            }

            val finalCodes = if (jsonCodes.isNotEmpty()) {
                jsonCodes
            } else {
                // Split by comma, semicolon, newline, tab, whitespace
                trimmed.split(Regex("[,;\\n\\r\\t]+"))
                    .map { it.trim().uppercase(Locale.ROOT) }
                    .flatMap { it.split(Regex("\\s+")) }
                    .map { 
                        it.replace(Regex("^(?:\\d+|[০-৯]+)[.)\\-:]\\s*"), "")
                          .replace(Regex("^[•\\-*#]"), "")
                          .trim() 
                    }
                    .filter { isValidPresetCode(it) }
                    .distinct()
            }

            if (finalCodes.isEmpty()) {
                _uiEvent.emit("কোনো সঠিক কোড চিহ্নিত করা যায়নি (আইডি বা নাম প্রিসেট তালিকায় যোগ হবে না)")
                return@launch
            }

            val existingPresets = repository.getAllPresetCodesList().associateBy { it.code.uppercase(Locale.ROOT) }
            val presetEntities = finalCodes.map { codeStr ->
                val upper = codeStr.uppercase(Locale.ROOT)
                val existing = existingPresets[upper]
                PresetMedicalCodeEntity(
                    code = upper,
                    name = existing?.name ?: "",
                    category = existing?.category ?: "General"
                )
            }
            repository.savePresetCodes(presetEntities)
            _uiEvent.emit("${finalCodes.size} টি কোড সফলভাবে শর্টকাট তালিকায় যোগ হয়েছে!")
        }
    }

    fun batchUpdateCode(ids: List<Long>, newCode: String) {
        val cleanCode = newCode.trim().uppercase()
        viewModelScope.launch {
            val currentRecords = repository.getRecordsByDateList(_selectedDate.value)
            val updated = currentRecords.map { r ->
                if (r.id in ids) r.copy(code = cleanCode) else r
            }
            repository.saveRecords(updated)
            _uiEvent.emit("নির্বাচিত রেকর্ডগুলোর কোড আপডেট হয়েছে: $cleanCode")
        }
    }

    fun addPresetCode(code: String, name: String = "", category: String = "General") {
        val cleanCode = code.trim()
        if (cleanCode.isBlank()) return

        // If JSON passed to addPresetCode, delegate to addMultiplePresetCodes
        if (cleanCode.contains("{") || cleanCode.contains("[")) {
            addMultiplePresetCodes(cleanCode)
            return
        }

        if (!isValidPresetCode(cleanCode)) {
            viewModelScope.launch {
                _uiEvent.emit("সতর্কতা: '$cleanCode' একটি সঠিক কোড নয় (আইডি বা নাম শর্টকাট কোড হিসেবে যোগ হবে না)")
            }
            return
        }

        val upperCode = cleanCode.uppercase(Locale.ROOT)
        viewModelScope.launch {
            repository.savePresetCode(
                PresetMedicalCodeEntity(
                    code = upperCode,
                    name = name.trim(),
                    category = category.trim().ifBlank { "General" }
                )
            )
            _uiEvent.emit("নতুন প্রিসেট কোড যোগ হয়েছে: $upperCode")
        }
    }

    fun deletePresetCode(code: String) {
        viewModelScope.launch {
            repository.deletePresetCode(code)
            _uiEvent.emit("কোডটি প্রিসেট তালিকা থেকে মুছে ফেলা হয়েছে")
        }
    }
}
