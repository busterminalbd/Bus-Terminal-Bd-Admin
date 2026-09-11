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

    private fun calculateNextId(lastId: String): String {
        val trimmed = lastId.trim()
        val digits = trimmed.takeLastWhile { it.isDigit() }
        return if (digits.isNotEmpty()) {
            val prefix = trimmed.dropLast(digits.length)
            val nextNum = (digits.toLongOrNull() ?: 0L) + 1
            "$prefix${String.format(Locale.US, "%0${digits.length}d", nextNum)}"
        } else {
            trimmed
        }
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
                val record = MedicalRecordEntity(
                    date = _selectedDate.value,
                    patientId = pId,
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
                _uiEvent.emit("রেকর্ড সফলভাবে যোগ হয়েছে: ${pId.ifBlank { finalCode }}")
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
                    repository.savePresetCodes(presetCodesToAdd.distinctBy { it.code })
                }
                _uiEvent.emit("${records.size} টি এন্ট্রি সফলভাবে ইমপোর্ট হয়েছে এবং কোডগুলো কুইক তালিকায় যোগ হয়েছে!")
            } else {
                _uiEvent.emit("কোনো বৈধ এন্ট্রি চিহ্নিত করা যায়নি")
            }
        }
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
