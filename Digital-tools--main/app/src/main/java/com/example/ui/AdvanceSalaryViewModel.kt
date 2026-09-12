package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AdvanceSalaryEntity
import com.example.data.AdvanceSalaryRepository
import com.example.data.AppDatabase
import com.example.util.BengaliUtils
import com.example.util.EnglishUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AdvanceSalaryFormState(
    val editingId: Long? = null,
    val companyName: String = "Al-Baraka General Store",
    val companySubtitle: String = "Head Office, Dhaka",
    val applicationNo: String = "",
    val dateString: String = "",
    val applicantName: String = "",
    val designation: String = "",
    val contactNumber: String = "",
    val monthlySalaryInput: String = "",
    val monthlySalary: Double = 0.0,
    val advanceAmountInput: String = "",
    val advanceAmount: Double = 0.0,
    val advanceAmountInWords: String = "",
    val reason: String = "",
    val repaymentType: String = "one_time", // "one_time" or "installments"
    val installmentCount: Int = 1,
    val installmentAmountPerMonth: Double = 0.0,
    val deductionStartMonth: String = "Next Month",
    val previousAdvancePendingInput: String = "",
    val previousAdvancePending: Double = 0.0,
    val remarks: String = "",
    val status: String = "APPROVED",
    val showSignatures: Boolean = true
)

class AdvanceSalaryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AdvanceSalaryRepository
    private val prefs = application.getSharedPreferences("advance_salary_prefs", android.content.Context.MODE_PRIVATE)
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)

    private val _formState = MutableStateFlow(AdvanceSalaryFormState())
    val formState: StateFlow<AdvanceSalaryFormState> = _formState.asStateFlow()

    private val _historyList = MutableStateFlow<List<AdvanceSalaryEntity>>(emptyList())
    val historyList: StateFlow<List<AdvanceSalaryEntity>> = _historyList.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent: SharedFlow<String> = _uiEvent.asSharedFlow()

    private val _presetReasons = MutableStateFlow<List<String>>(emptyList())
    val presetReasons: StateFlow<List<String>> = _presetReasons.asStateFlow()

    private val _presetDesignations = MutableStateFlow<List<String>>(emptyList())
    val presetDesignations: StateFlow<List<String>> = _presetDesignations.asStateFlow()

    private val defaultReasons = listOf(
        "Medical Expenses & Treatment",
        "Family Emergency",
        "Eid / Festival Expenses",
        "House Rent & Advance",
        "Children School / College Admission",
        "Urgent Debt Repayment",
        "Personal Emergency"
    )

    private val defaultDesignations = listOf(
        "Manager",
        "Accountant",
        "Senior Executive",
        "Sales Officer",
        "Computer Operator",
        "Store Keeper",
        "Driver",
        "Delivery Man",
        "Assistant",
        "Security Guard",
        "Staff"
    )

    init {
        val dao = AppDatabase.getDatabase(application).advanceSalaryDao()
        repository = AdvanceSalaryRepository(dao)

        loadPresets()

        val savedCompanyName = prefs.getString("default_company_name", "Al-Baraka General Store") ?: "Al-Baraka General Store"
        val savedSubtitle = prefs.getString("default_company_subtitle", "Head Office, Dhaka") ?: "Head Office, Dhaka"
        val today = dateFormat.format(Date())
        val defaultAppNo = "ADV-${System.currentTimeMillis() % 100000}"

        _formState.value = AdvanceSalaryFormState(
            companyName = savedCompanyName,
            companySubtitle = savedSubtitle,
            applicationNo = defaultAppNo,
            dateString = today,
            deductionStartMonth = "Next Month"
        )

        loadHistory()
    }

    private fun loadPresets() {
        val reasonsJson = prefs.getString("preset_reasons_json", null)
        if (reasonsJson != null) {
            try {
                val array = JSONArray(reasonsJson)
                val list = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    list.add(array.getString(i))
                }
                _presetReasons.value = list
            } catch (e: Exception) {
                _presetReasons.value = defaultReasons
            }
        } else {
            _presetReasons.value = defaultReasons
        }

        val desigJson = prefs.getString("preset_designations_json", null)
        if (desigJson != null) {
            try {
                val array = JSONArray(desigJson)
                val list = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    list.add(array.getString(i))
                }
                _presetDesignations.value = list
            } catch (e: Exception) {
                _presetDesignations.value = defaultDesignations
            }
        } else {
            _presetDesignations.value = defaultDesignations
        }
    }

    private fun saveReasonsToPrefs(list: List<String>) {
        _presetReasons.value = list
        val array = JSONArray()
        list.forEach { array.put(it) }
        prefs.edit().putString("preset_reasons_json", array.toString()).apply()
    }

    private fun saveDesignationsToPrefs(list: List<String>) {
        _presetDesignations.value = list
        val array = JSONArray()
        list.forEach { array.put(it) }
        prefs.edit().putString("preset_designations_json", array.toString()).apply()
    }

    fun addReasonPreset(reason: String) {
        val trimmed = reason.trim()
        if (trimmed.isNotBlank() && !_presetReasons.value.contains(trimmed)) {
            val updated = _presetReasons.value + trimmed
            saveReasonsToPrefs(updated)
            viewModelScope.launch { _uiEvent.emit("Reason added to presets") }
        }
    }

    fun updateReasonPreset(oldReason: String, newReason: String) {
        val trimmed = newReason.trim()
        if (trimmed.isNotBlank()) {
            val updated = _presetReasons.value.map { if (it == oldReason) trimmed else it }
            saveReasonsToPrefs(updated)
        }
    }

    fun deleteReasonPreset(reason: String) {
        val updated = _presetReasons.value.filter { it != reason }
        saveReasonsToPrefs(updated)
    }

    fun onDataImported() {
        loadPresets()
        val savedCompanyName = prefs.getString("default_company_name", "Al-Baraka General Store") ?: "Al-Baraka General Store"
        val savedSubtitle = prefs.getString("default_company_subtitle", "Head Office, Dhaka") ?: "Head Office, Dhaka"
        _formState.update {
            it.copy(
                companyName = savedCompanyName,
                companySubtitle = savedSubtitle
            )
        }
    }

    fun addDesignationPreset(desig: String) {
        val trimmed = desig.trim()
        if (trimmed.isNotBlank() && !_presetDesignations.value.contains(trimmed)) {
            val updated = _presetDesignations.value + trimmed
            saveDesignationsToPrefs(updated)
            viewModelScope.launch { _uiEvent.emit("Designation added to presets") }
        }
    }

    fun updateDesignationPreset(oldDesig: String, newDesig: String) {
        val trimmed = newDesig.trim()
        if (trimmed.isNotBlank()) {
            val updated = _presetDesignations.value.map { if (it == oldDesig) trimmed else it }
            saveDesignationsToPrefs(updated)
        }
    }

    fun deleteDesignationPreset(desig: String) {
        val updated = _presetDesignations.value.filter { it != desig }
        saveDesignationsToPrefs(updated)
    }

    private fun loadHistory() {
        viewModelScope.launch {
            repository.getAllApplications()
                .catch { e -> _uiEvent.emit("Error loading records: ${e.message}") }
                .collectLatest { list ->
                    _historyList.value = list
                }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            if (query.isBlank()) {
                repository.getAllApplications().collectLatest { _historyList.value = it }
            } else {
                repository.searchApplications(query).collectLatest { _historyList.value = it }
            }
        }
    }

    fun updateCompanyName(name: String) {
        _formState.update { it.copy(companyName = name) }
    }

    fun updateCompanySubtitle(subtitle: String) {
        _formState.update { it.copy(companySubtitle = subtitle) }
    }

    fun updateApplicationNo(no: String) {
        _formState.update { it.copy(applicationNo = no) }
    }

    fun updateDate(date: String) {
        _formState.update { it.copy(dateString = date) }
    }

    fun updateApplicantName(name: String) {
        _formState.update { it.copy(applicantName = name) }
    }

    fun updateDesignation(designation: String) {
        _formState.update { it.copy(designation = designation) }
    }

    fun updateContactNumber(contact: String) {
        _formState.update { it.copy(contactNumber = contact) }
    }

    fun updateMonthlySalary(input: String) {
        val parsed = BengaliUtils.parseBengaliNumber(input)
        _formState.update { it.copy(monthlySalaryInput = input, monthlySalary = parsed) }
    }

    fun updatePreviousAdvancePending(input: String) {
        val parsed = BengaliUtils.parseBengaliNumber(input)
        _formState.update { it.copy(previousAdvancePendingInput = input, previousAdvancePending = parsed) }
    }

    fun fillMaxEligibleAdvance() {
        val current = _formState.value
        if (current.monthlySalary > 0.0) {
            val maxEligible = (current.monthlySalary - current.previousAdvancePending).coerceAtLeast(0.0)
            val inputStr = if (maxEligible > 0) maxEligible.toLong().toString() else "0"
            updateAdvanceAmount(inputStr)
        }
    }

    fun updateAdvanceAmount(input: String) {
        val parsed = BengaliUtils.parseBengaliNumber(input)
        val inWords = if (parsed > 0) EnglishUtils.amountToEnglishWords(parsed) else ""
        val count = _formState.value.installmentCount.coerceAtLeast(1)
        val perMonth = if (parsed > 0) parsed / count else 0.0

        _formState.update {
            it.copy(
                advanceAmountInput = input,
                advanceAmount = parsed,
                advanceAmountInWords = inWords,
                installmentAmountPerMonth = perMonth
            )
        }
    }

    fun updateAdvanceAmountInWords(words: String) {
        _formState.update { it.copy(advanceAmountInWords = words) }
    }

    fun updateReason(reason: String) {
        _formState.update { it.copy(reason = reason) }
    }

    fun updateRepaymentType(type: String) {
        val count = if (type == "one_time") 1 else _formState.value.installmentCount.coerceAtLeast(2)
        val amount = _formState.value.advanceAmount
        val perMonth = if (amount > 0) amount / count else 0.0

        _formState.update {
            it.copy(
                repaymentType = type,
                installmentCount = count,
                installmentAmountPerMonth = perMonth
            )
        }
    }

    fun updateInstallmentCount(count: Int) {
        val safeCount = count.coerceAtLeast(1)
        val amount = _formState.value.advanceAmount
        val perMonth = if (amount > 0) amount / safeCount else 0.0

        _formState.update {
            it.copy(
                installmentCount = safeCount,
                installmentAmountPerMonth = perMonth
            )
        }
    }

    fun updateDeductionStartMonth(month: String) {
        _formState.update { it.copy(deductionStartMonth = month) }
    }

    fun updateRemarks(remarks: String) {
        _formState.update { it.copy(remarks = remarks) }
    }

    fun updateStatus(status: String) {
        _formState.update { it.copy(status = status) }
    }

    fun updateShowSignatures(show: Boolean) {
        _formState.update { it.copy(showSignatures = show) }
    }

    fun saveApplication(notifyUser: Boolean = true, onSaved: ((Long) -> Unit)? = null) {
        val current = _formState.value
        if (current.applicantName.isBlank()) {
            if (notifyUser) {
                viewModelScope.launch { _uiEvent.emit("Please enter employee name") }
            }
            return
        }
        if (current.advanceAmount <= 0.0) {
            if (notifyUser) {
                viewModelScope.launch { _uiEvent.emit("Please enter advance amount") }
            }
            return
        }
        if (current.monthlySalary > 0.0) {
            val maxEligible = (current.monthlySalary - current.previousAdvancePending).coerceAtLeast(0.0)
            if (current.advanceAmount > maxEligible) {
                if (notifyUser) {
                    viewModelScope.launch {
                        val maxFormatted = EnglishUtils.formatEnglishCurrency(maxEligible)
                        _uiEvent.emit("Advance cannot exceed eligible limit of Tk. $maxFormatted (Salary Tk. ${EnglishUtils.formatEnglishCurrency(current.monthlySalary)} - Prev Advance Tk. ${EnglishUtils.formatEnglishCurrency(current.previousAdvancePending)})")
                    }
                }
                return
            }
        }

        viewModelScope.launch {
            try {
                val entity = AdvanceSalaryEntity(
                    id = current.editingId ?: 0L,
                    companyName = current.companyName.trim(),
                    companySubtitle = current.companySubtitle.trim(),
                    applicationNo = current.applicationNo.trim().ifBlank { "ADV-${System.currentTimeMillis() % 100000}" },
                    dateString = current.dateString.trim().ifBlank { dateFormat.format(Date()) },
                    applicantName = current.applicantName.trim(),
                    employeeId = "",
                    designation = current.designation.trim(),
                    department = "",
                    contactNumber = current.contactNumber.trim(),
                    monthlySalary = current.monthlySalary,
                    advanceAmount = current.advanceAmount,
                    advanceAmountInWords = current.advanceAmountInWords.trim().ifBlank {
                        EnglishUtils.amountToEnglishWords(current.advanceAmount)
                    },
                    reason = current.reason.trim(),
                    repaymentType = current.repaymentType,
                    installmentCount = current.installmentCount,
                    installmentAmountPerMonth = current.installmentAmountPerMonth,
                    deductionStartMonth = current.deductionStartMonth.trim(),
                    previousAdvancePending = current.previousAdvancePending,
                    guarantorOrRecommendedBy = "",
                    remarks = current.remarks.trim(),
                    status = current.status,
                    showSignatures = current.showSignatures,
                    createdAt = System.currentTimeMillis()
                )

                if (current.editingId != null && current.editingId > 0L) {
                    repository.updateApplication(entity)
                    onSaved?.invoke(current.editingId)
                    if (notifyUser) {
                        _uiEvent.emit("Advance salary application updated successfully")
                    }
                } else {
                    val id = repository.insertApplication(entity)
                    _formState.update { it.copy(editingId = id) }
                    onSaved?.invoke(id)
                    if (notifyUser) {
                        _uiEvent.emit("Advance salary application saved successfully")
                    }
                }
            } catch (e: Exception) {
                if (notifyUser) {
                    _uiEvent.emit("Save failed: ${e.message}")
                }
            }
        }
    }

    fun loadApplicationForEditing(entity: AdvanceSalaryEntity) {
        _formState.value = AdvanceSalaryFormState(
            editingId = entity.id,
            companyName = entity.companyName,
            companySubtitle = entity.companySubtitle,
            applicationNo = entity.applicationNo,
            dateString = entity.dateString,
            applicantName = entity.applicantName,
            designation = entity.designation,
            contactNumber = entity.contactNumber,
            monthlySalary = entity.monthlySalary,
            monthlySalaryInput = if (entity.monthlySalary > 0) entity.monthlySalary.toLong().toString() else "",
            advanceAmount = entity.advanceAmount,
            advanceAmountInput = if (entity.advanceAmount > 0) entity.advanceAmount.toLong().toString() else "",
            advanceAmountInWords = entity.advanceAmountInWords,
            reason = entity.reason,
            repaymentType = entity.repaymentType,
            installmentCount = entity.installmentCount,
            installmentAmountPerMonth = entity.installmentAmountPerMonth,
            deductionStartMonth = entity.deductionStartMonth,
            previousAdvancePending = entity.previousAdvancePending,
            previousAdvancePendingInput = if (entity.previousAdvancePending > 0) entity.previousAdvancePending.toLong().toString() else "",
            remarks = entity.remarks,
            status = entity.status,
            showSignatures = entity.showSignatures
        )
    }

    fun deleteApplication(id: Long) {
        viewModelScope.launch {
            try {
                repository.deleteApplicationById(id)
                if (_formState.value.editingId == id) {
                    resetForm()
                }
                _uiEvent.emit("Application deleted successfully")
            } catch (e: Exception) {
                _uiEvent.emit("Delete failed: ${e.message}")
            }
        }
    }

    fun resetForm() {
        val savedCompanyName = prefs.getString("default_company_name", "Al-Baraka General Store") ?: "Al-Baraka General Store"
        val savedSubtitle = prefs.getString("default_company_subtitle", "Head Office, Dhaka") ?: "Head Office, Dhaka"
        val today = dateFormat.format(Date())
        val defaultAppNo = "ADV-${System.currentTimeMillis() % 100000}"

        _formState.value = AdvanceSalaryFormState(
            companyName = savedCompanyName,
            companySubtitle = savedSubtitle,
            applicationNo = defaultAppNo,
            dateString = today,
            deductionStartMonth = "Next Month"
        )
    }

    fun saveDefaultCompanySettings(name: String, subtitle: String) {
        prefs.edit()
            .putString("default_company_name", name.trim())
            .putString("default_company_subtitle", subtitle.trim())
            .apply()

        _formState.update {
            it.copy(companyName = name.trim(), companySubtitle = subtitle.trim())
        }
        viewModelScope.launch {
            _uiEvent.emit("Company settings saved")
        }
    }
}
