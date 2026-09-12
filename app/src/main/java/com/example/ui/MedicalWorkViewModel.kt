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
        val pId = patientId.trim().uppercase()
        val c = code.trim().uppercase()
        val n = patientName.trim().uppercase()

        if (pId.isBlank() && c.isBlank()) {
            viewModelScope.launch { _uiEvent.emit("আইডি বা কোড অন্তত একটি পূরণ করুন") }
            return
        }

        viewModelScope.launch {
            // Check if code contains multiple codes separated by comma, semicolon or newline
            val codeTokens = c.split(Regex("[,;\\n\\r]+")).map { it.trim() }.filter { it.isNotBlank() }
            if (codeTokens.size > 1) {
                val recordsToAdd = mutableListOf<MedicalRecordEntity>()
                val presetCodesToAdd = mutableListOf<PresetMedicalCodeEntity>()
                var currentId = pId.ifBlank { nextSuggestedPatientId.value }

                for (singleCode in codeTokens) {
                    recordsToAdd.add(
                        MedicalRecordEntity(
                            date = _selectedDate.value,
                            patientId = currentId,
                            code = singleCode,
                            patientName = n,
                            notes = notes.trim()
                        )
                    )
                    presetCodesToAdd.add(
                        PresetMedicalCodeEntity(
                            code = singleCode,
                            name = "",
                            category = "General"
                        )
                    )
                    currentId = calculateNextId(currentId)
                }
                repository.saveRecords(recordsToAdd)
                repository.savePresetCodes(presetCodesToAdd)
                _uiEvent.emit("${recordsToAdd.size} টি রেকর্ড ও কোড সফলভাবে যোগ হয়েছে!")
            } else {
                val finalCode = codeTokens.firstOrNull() ?: c
                val finalId = pId.ifBlank { nextSuggestedPatientId.value }
                val record = MedicalRecordEntity(
                    date = _selectedDate.value,
                    patientId = finalId,
                    code = finalCode,
                    patientName = n,
                    notes = notes.trim()
                )
                repository.saveRecord(record)
                if (finalCode.isNotBlank()) {
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
            // First attempt to detect and parse JSON format
            if (tryParseAndImportJson(trimmed)) {
                return@launch
            }

            // Determine line separator or item separator
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
                val tokens = if (parts.size <= 1) {
                    cleanedLine.split(Regex("\\s+")).map { it.trim() }.filter { it.isNotBlank() }
                } else {
                    parts
                }

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
                    presetCodesToAdd.add(
                        PresetMedicalCodeEntity(
                            code = codeToken.uppercase(),
                            name = "",
                            category = "General"
                        )
                    )
                } else {
                    // No explicit Patient ID found in this line/item!
                    // This means the user pasted JUST CODES (e.g. "AF07", "MD-01", "101", "CBC, USG")
                    for (token in tokens) {
                        val codeVal = token.uppercase()
                        records.add(
                            MedicalRecordEntity(
                                date = _selectedDate.value,
                                patientId = currentNextId,
                                code = codeVal,
                                patientName = ""
                            )
                        )
                        presetCodesToAdd.add(
                            PresetMedicalCodeEntity(
                                code = codeVal,
                                name = "",
                                category = "General"
                            )
                        )
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
                _uiEvent.emit("${records.size} টি এন্ট্রি সফলভাবে ইমপোর্ট হয়েছে এবং কোডগুলো কুইক তালিকায় যোগ হয়েছে!")
            } else {
                _uiEvent.emit("কোনো বৈধ এন্ট্রি চিহ্নিত করা যায়নি")
            }
        }
    }

    private suspend fun tryParseAndImportJson(rawText: String): Boolean {
        var jsonText = rawText.trim()
        if (jsonText.startsWith("```")) {
            jsonText = jsonText.replace(Regex("""^```(?:json)?\s*""", RegexOption.IGNORE_CASE), "")
                .replace(Regex("""\s*```$"""), "")
                .trim()
        }

        val firstBrace = jsonText.indexOf('{')
        val firstBracket = jsonText.indexOf('[')
        val startIdx = when {
            firstBrace != -1 && firstBracket != -1 -> minOf(firstBrace, firstBracket)
            firstBrace != -1 -> firstBrace
            firstBracket != -1 -> firstBracket
            else -> -1
        }

        val lastBrace = jsonText.lastIndexOf('}')
        val lastBracket = jsonText.lastIndexOf(']')
        val endIdx = maxOf(lastBrace, lastBracket)

        if (startIdx == -1 || endIdx == -1 || endIdx <= startIdx) {
            return false
        }

        val jsonSubstring = jsonText.substring(startIdx, endIdx + 1).trim()

        try {
            var targetDate = _selectedDate.value
            var dateChanged = false
            var dataArray: JSONArray? = null
            val presetCodesToAdd = mutableListOf<PresetMedicalCodeEntity>()

            if (jsonSubstring.startsWith("{")) {
                val rootObj = JSONObject(jsonSubstring)

                // 1. Extract Date if available
                val dateKeys = listOf("date", "তারিখ", "Date", "DATE", "report_date", "reportDate")
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

                // 2. Extract direct preset codes if root object contains a code array
                val directCodeKeys = listOf("presetMedicalCodes", "presetCodes", "preset_codes", "codes", "testCodes", "test_codes", "tests", "allCodes", "codeList")
                for (k in directCodeKeys) {
                    val arr = rootObj.optJSONArray(k) ?: continue
                    for (idx in 0 until arr.length()) {
                        val entry = arr.opt(idx) ?: continue
                        if (entry is JSONObject) {
                            val c = entry.optString("code")
                                .ifBlank { entry.optString("test") }
                                .ifBlank { entry.optString("testCode") }
                                .trim()
                            val n = entry.optString("name")
                                .ifBlank { entry.optString("testName") }
                                .ifBlank { entry.optString("description") }
                                .trim()
                            if (c.isNotBlank()) {
                                presetCodesToAdd.add(
                                    PresetMedicalCodeEntity(
                                        code = c.uppercase(Locale.ROOT),
                                        name = if (c.equals(n, ignoreCase = true)) "" else n,
                                        category = entry.optString("category", "General")
                                    )
                                )
                            }
                        } else if (entry is String && entry.isNotBlank()) {
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

                // 3. Find data array
                val arrayKeys = listOf(
                    "data", "records", "rows", "items", "patients", "list", "তথ্য",
                    "entries", "table", "result", "results", "patient_list",
                    "medical_records", "medicalRecords", "test_list"
                )
                for (key in arrayKeys) {
                    if (rootObj.has(key) && !rootObj.isNull(key)) {
                        dataArray = rootObj.optJSONArray(key)
                        if (dataArray != null) break
                    }
                }

                // Fallback: check if rootObj contains any array of objects/records
                if (dataArray == null) {
                    val keys = rootObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        if (k !in directCodeKeys) {
                            val arr = rootObj.optJSONArray(k)
                            if (arr != null && arr.length() > 0) {
                                dataArray = arr
                                break
                            }
                        }
                    }
                }
            } else if (jsonSubstring.startsWith("[")) {
                dataArray = JSONArray(jsonSubstring)
            }

            if ((dataArray == null || dataArray.length() == 0) && presetCodesToAdd.isEmpty()) {
                return false
            }

            val recordsToAdd = mutableListOf<MedicalRecordEntity>()
            var currentNextId = nextSuggestedPatientId.value

            // Fetch existing records for this date to update matching IDs or append
            val existingRecords = repository.getRecordsByDateList(targetDate)

            if (dataArray != null) {
                for (i in 0 until dataArray.length()) {
                    val item = dataArray.opt(i) ?: continue

                    // If array directly contains string codes e.g. ["AF07", "MD-01", "CBC"]
                    if (item is String) {
                        val trimmedCode = item.trim()
                        if (trimmedCode.isNotBlank()) {
                            val upperCode = trimmedCode.uppercase(Locale.ROOT)
                            presetCodesToAdd.add(
                                PresetMedicalCodeEntity(
                                    code = upperCode,
                                    name = "",
                                    category = "General"
                                )
                            )
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
                    var codeDescription = ""

                    if (item is JSONObject) {
                        // Match ID
                        val idKeys = listOf("ID", "id", "Id", "patientId", "patient_id", "patientID", "আইডি", "আইডি নং", "আইডি নম্বর", "Patient ID")
                        for (k in idKeys) {
                            if (item.has(k) && !item.isNull(k)) {
                                patientId = item.optString(k).trim()
                                if (patientId.isNotBlank()) break
                            }
                        }

                        // Match Code
                        val codeKeys = listOf(
                            "কোড", "code", "Code", "CODE",
                            "testCode", "test_code", "testCodes", "test_codes",
                            "medical_code", "medicalCode",
                            "test", "tests", "টেস্ট", "পরীক্ষা",
                            "investigation", "investigations",
                            "item", "items", "service", "services",
                            "short_code", "shortCode"
                        )
                        for (k in codeKeys) {
                            if (item.has(k) && !item.isNull(k)) {
                                val opt = item.opt(k)
                                if (opt is JSONArray) {
                                    val list = mutableListOf<String>()
                                    for (idx in 0 until opt.length()) {
                                        val s = opt.optString(idx).trim()
                                        if (s.isNotBlank()) list.add(s)
                                    }
                                    code = list.joinToString(", ")
                                } else {
                                    code = item.optString(k).trim()
                                }
                                if (code.isNotBlank()) break
                            }
                        }

                        // Match Name
                        val nameKeys = listOf("নাম", "name", "Name", "patientName", "patient_name", "Patient Name", "রোগীর নাম")
                        for (k in nameKeys) {
                            if (item.has(k) && !item.isNull(k)) {
                                patientName = item.optString(k).trim()
                                if (patientName.isNotBlank()) break
                            }
                        }

                        // Match Notes
                        val noteKeys = listOf("notes", "note", "মন্তব্য", "remarks")
                        for (k in noteKeys) {
                            if (item.has(k) && !item.isNull(k)) {
                                notes = item.optString(k).trim()
                                if (notes.isNotBlank()) break
                            }
                        }

                        // Match Code Description / Test Title
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
                                } else if (code.isBlank() && (kNorm == "code" || kNorm.contains("code") || k.contains("কোড") || kNorm.contains("test"))) {
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

                    val existingMatch = existingRecords.firstOrNull { it.patientId.equals(cleanPatientId, ignoreCase = true) }
                    if (existingMatch != null) {
                        recordsToAdd.add(
                            existingMatch.copy(
                                code = cleanCode,
                                patientName = if (cleanName.isNotBlank()) cleanName else existingMatch.patientName,
                                notes = if (notes.isNotBlank()) notes else existingMatch.notes
                            )
                        )
                    } else {
                        recordsToAdd.add(
                            MedicalRecordEntity(
                                id = 0,
                                date = targetDate,
                                patientId = cleanPatientId,
                                code = cleanCode,
                                patientName = cleanName,
                                notes = notes
                            )
                        )
                    }

                    // Automatically extract and register the code into shortcut/preset list
                    if (cleanCode.isNotBlank()) {
                        // 1. Add compound / full code as a preset
                        presetCodesToAdd.add(
                            PresetMedicalCodeEntity(
                                code = cleanCode,
                                name = codeDescription,
                                category = "General"
                            )
                        )

                        // 2. Also split compound codes (e.g. "CBC, USG", "AF07; MD-01", "AF07/MD-01") into individual shortcuts
                        val subTokens = cleanCode.split(Regex("[,;\\n\\r/]+"))
                            .map { it.replace(Regex("""[()[\]{}]"""), " ").trim() }
                            .flatMap { it.split(Regex("\\s+")) }
                            .map { it.trim().uppercase(Locale.ROOT) }
                            .filter { it.isNotBlank() && it.length in 2..25 && it != "DUE" }

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
                }
            }

            // Save preset codes to database so they appear in shortcut dropdown
            var newPresetCount = 0
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
                newPresetCount = mergedPresets.count { !existingPresets.containsKey(it.code.uppercase(Locale.ROOT)) }
                repository.savePresetCodes(mergedPresets)
            }

            if (recordsToAdd.isNotEmpty()) {
                repository.saveRecords(recordsToAdd)
                if (dateChanged) {
                    _selectedDate.value = targetDate
                }
                val formattedDate = MedicalPrintUtils.formatDateShort(targetDate)
                val countBn = BengaliUtils.toBengaliDigits(recordsToAdd.size.toString())
                val newCodesBn = BengaliUtils.toBengaliDigits(newPresetCount.toString())
                val presetInfo = if (newPresetCount > 0) " এবং $newCodesBn টি নতুন কোড শর্টকাটে যুক্ত হয়েছে" else " (সব কোড শর্টকাটে সক্রিয়)"
                _uiEvent.emit("JSON থেকে $countBn টি এন্ট্রি টেবিলে যুক্ত হয়েছে$presetInfo ($formattedDate)!")
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

    fun addMultiplePresetCodes(rawCodesText: String) {
        val trimmed = rawCodesText.trim()
        if (trimmed.isBlank()) {
            viewModelScope.launch { _uiEvent.emit("কোনো কোড পাওয়া যায়নি") }
            return
        }

        viewModelScope.launch {
            // Split by comma, semicolon, newline, tab, whitespace
            val tokens = trimmed.split(Regex("[,;\\n\\r\\t]+"))
                .map { it.trim().uppercase() }
                .flatMap { it.split(Regex("\\s+")) }
                .map { 
                    it.replace(Regex("^(?:\\d+|[০-৯]+)[.)\\-:]\\s*"), "")
                      .replace(Regex("^[•\\-*#]"), "")
                      .trim() 
                }
                .filter { it.isNotBlank() }
                .distinct()

            if (tokens.isEmpty()) {
                _uiEvent.emit("কোনো কোড চিহ্নিত করা যায়নি")
                return@launch
            }

            val presetEntities = tokens.map { codeStr ->
                PresetMedicalCodeEntity(
                    code = codeStr,
                    name = "",
                    category = "General"
                )
            }
            repository.savePresetCodes(presetEntities)
            _uiEvent.emit("${tokens.size} টি নতুন কোড কুইক তালিকায় যোগ হয়েছে!")
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
        val cleanCode = code.trim().uppercase()
        if (cleanCode.isBlank()) return

        viewModelScope.launch {
            repository.savePresetCode(
                PresetMedicalCodeEntity(
                    code = cleanCode,
                    name = name.trim(),
                    category = category.trim().ifBlank { "General" }
                )
            )
            _uiEvent.emit("নতুন প্রিসেট কোড যোগ হয়েছে: $cleanCode")
        }
    }

    fun deletePresetCode(code: String) {
        viewModelScope.launch {
            repository.deletePresetCode(code)
            _uiEvent.emit("কোডটি প্রিসেট তালিকা থেকে মুছে ফেলা হয়েছে")
        }
    }
}
