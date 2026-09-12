package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.BillItem
import com.example.data.BillRepository
import com.example.data.FoodBillUiModel
import com.example.util.BengaliUtils
import com.example.util.QuickPreset
import com.example.util.parseQuickPresetFromJsonObject
import com.example.util.quickPresetToJsonObject
import com.example.util.removeNodeById
import com.example.util.addNodeToParent
import com.example.util.updateOrMoveNode
import com.example.util.reorderNodesInParent
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FoodBillViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BillRepository

    init {
        val dao = AppDatabase.getDatabase(application).foodBillDao()
        repository = BillRepository(dao)
    }

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)

    private val _currentBillState = MutableStateFlow(CurrentBillState())
    val currentBillState: StateFlow<CurrentBillState> = _currentBillState.asStateFlow()

    private val _historyBills = MutableStateFlow<List<FoodBillUiModel>>(emptyList())
    val historyBills: StateFlow<List<FoodBillUiModel>> = _historyBills.asStateFlow()

    private val _quickPresets = MutableStateFlow<List<QuickPreset>>(emptyList())
    val quickPresets: StateFlow<List<QuickPreset>> = _quickPresets.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent: SharedFlow<String> = _uiEvent.asSharedFlow()

    private val prefs = application.getSharedPreferences("food_bill_prefs", android.content.Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(prefs.getString("app_theme_mode", "system") ?: "system")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _themeColor = MutableStateFlow(prefs.getString("app_theme_color", "emerald") ?: "emerald")
    val themeColor: StateFlow<String> = _themeColor.asStateFlow()

    private val _appLanguage = MutableStateFlow(prefs.getString("app_language", "bn") ?: "bn")
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    // Accessibility: lets users with difficulty reading small text scale up the whole app's
    // font size. Stored as a plain multiplier (1.0 = normal) applied via CompositionLocalProvider.
    private val _fontScale = MutableStateFlow(prefs.getFloat("app_font_scale", 1.0f))
    val fontScale: StateFlow<Float> = _fontScale.asStateFlow()

    fun setFontScale(scale: Float) {
        _fontScale.value = scale
        prefs.edit().putFloat("app_font_scale", scale).apply()
    }

    // First-launch onboarding: shown once, then permanently dismissed.
    private val _showOnboarding = MutableStateFlow(!prefs.getBoolean("onboarding_shown", false))
    val showOnboarding: StateFlow<Boolean> = _showOnboarding.asStateFlow()

    fun dismissOnboarding() {
        _showOnboarding.value = false
        prefs.edit().putBoolean("onboarding_shown", true).apply()
    }

    fun setThemeColor(colorKey: String) {
        _themeColor.value = colorKey
        prefs.edit().putString("app_theme_color", colorKey).apply()
        viewModelScope.launch {
            val msg = if (_appLanguage.value == "en") "Theme color updated" else "থিমের কালার পরিবর্তন করা হয়েছে"
            _uiEvent.emit(msg)
        }
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        prefs.edit().putString("app_theme_mode", mode).apply()
        viewModelScope.launch {
            val msg = when (mode) {
                "dark" -> if (_appLanguage.value == "en") "Dark mode activated" else "ডার্ক মোড চালু করা হয়েছে"
                "light" -> if (_appLanguage.value == "en") "Light mode activated" else "লাইট মোড চালু করা হয়েছে"
                else -> if (_appLanguage.value == "en") "System theme activated" else "সিস্টেম থিম নির্বাচন করা হয়েছে"
            }
            _uiEvent.emit(msg)
        }
    }

    fun setAppLanguage(lang: String) {
        _appLanguage.value = lang
        prefs.edit().putString("app_language", lang).apply()
        viewModelScope.launch {
            val msg = if (lang == "en") "Language set to English" else "ভাষা বাংলায় পরিবর্তন করা হয়েছে"
            _uiEvent.emit(msg)
        }
    }

    init {
        // Load Quick Presets from SharedPreferences
        loadQuickPresets()

        // Default initial items corresponding to the user's template
        resetToInitialTemplate()

        viewModelScope.launch {
            try {
                repository.allBills
                    .catch { e ->
                        e.printStackTrace()
                    }
                    .collectLatest { bills ->
                        _historyBills.value = bills
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadQuickPresets() {
        val jsonStr = prefs.getString("quick_presets_json", null)
        if (jsonStr.isNullOrEmpty()) {
            _quickPresets.value = emptyList()
        } else {
            try {
                val array = org.json.JSONArray(jsonStr)
                val list = mutableListOf<QuickPreset>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val preset = parseQuickPresetFromJsonObject(obj)
                    if (preset.name.isNotBlank()) {
                        list.add(preset)
                    }
                }
                _quickPresets.value = list
            } catch (e: Exception) {
                _quickPresets.value = emptyList()
            }
        }
    }

    private fun saveQuickPresetsToPrefs(presets: List<QuickPreset>) {
        try {
            val array = org.json.JSONArray()
            presets.forEach { p ->
                array.put(quickPresetToJsonObject(p))
            }
            prefs.edit().putString("quick_presets_json", array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addCustomQuickPreset(
        name: String,
        qty: String,
        rate: String = "",
        amount: String = "",
        parentFolderId: String? = null
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return

        val newPreset = QuickPreset(
            name = trimmedName,
            defaultQty = qty.trim(),
            defaultRate = rate.trim(),
            defaultAmount = amount.trim(),
            isFolder = false
        )
        val updatedTree = _quickPresets.value.addNodeToParent(newPreset, parentFolderId)
        _quickPresets.value = updatedTree
        saveQuickPresetsToPrefs(updatedTree)
        viewModelScope.launch { _uiEvent.emit("'$trimmedName' প্রিসেট তালিকায় যোগ করা হয়েছে") }
    }

    fun addQuickPresetFolder(
        folderName: String,
        parentFolderId: String? = null
    ) {
        val trimmedName = folderName.trim()
        if (trimmedName.isBlank()) return

        val newFolder = QuickPreset(
            name = trimmedName,
            isFolder = true,
            children = emptyList()
        )
        val updatedTree = _quickPresets.value.addNodeToParent(newFolder, parentFolderId)
        _quickPresets.value = updatedTree
        saveQuickPresetsToPrefs(updatedTree)
        viewModelScope.launch { _uiEvent.emit("'$trimmedName' ফোল্ডার তৈরি করা হয়েছে") }
    }

    fun updateQuickPresetNode(
        nodeId: String,
        newName: String,
        newQty: String = "",
        newRate: String = "",
        newAmount: String = "",
        isFolder: Boolean = false,
        targetFolderId: String? = null,
        existingChildren: List<QuickPreset> = emptyList()
    ) {
        val trimmedName = newName.trim()
        if (trimmedName.isBlank()) return

        val updatedNode = QuickPreset(
            id = nodeId,
            name = trimmedName,
            defaultQty = newQty.trim(),
            defaultRate = newRate.trim(),
            defaultAmount = newAmount.trim(),
            isFolder = isFolder,
            children = existingChildren
        )
        val updatedTree = _quickPresets.value.updateOrMoveNode(nodeId, updatedNode, targetFolderId)
        _quickPresets.value = updatedTree
        saveQuickPresetsToPrefs(updatedTree)
        viewModelScope.launch { _uiEvent.emit("'$trimmedName' আপডেট করা হয়েছে") }
    }

    fun removeQuickPreset(preset: QuickPreset) {
        val updatedTree = _quickPresets.value.removeNodeById(preset.id)
        _quickPresets.value = updatedTree
        saveQuickPresetsToPrefs(updatedTree)
        viewModelScope.launch { _uiEvent.emit("'${preset.name}' সরানো হয়েছে") }
    }

    fun reorderQuickPresets(parentFolderId: String?, fromIndex: Int, toIndex: Int) {
        val updatedTree = _quickPresets.value.reorderNodesInParent(parentFolderId, fromIndex, toIndex)
        _quickPresets.value = updatedTree
        saveQuickPresetsToPrefs(updatedTree)
    }

    fun reorderQuickPresets(fromIndex: Int, toIndex: Int) {
        reorderQuickPresets(null, fromIndex, toIndex)
    }

    fun resetQuickPresetsToDefault() {
        _quickPresets.value = emptyList()
        saveQuickPresetsToPrefs(emptyList())
        viewModelScope.launch { _uiEvent.emit("প্রিসেট তালিকা রিসেট করা হয়েছে") }
    }

    // The header (title/subtitle/signature) is kept separately per bill type — "market"
    // (খাবার বিল / বাজার লিস্ট) and "transport" (যাতায়াত ভাড়া) — so switching type doesn't
    // overwrite one memo's saved header with the other's. Falls back to the old shared
    // "market"-only keys used before this per-type split existed, so upgrading users don't
    // lose their previously saved header.
    private fun centerNameKey(type: String) = "saved_center_name_$type"
    private fun subtitleKey(type: String) = "saved_subtitle_$type"
    private fun purchaserLabelKey(type: String) = "saved_purchaser_label_$type"

    fun getSavedSettingsForType(type: String): Triple<String, String, String> {
        val centerName = prefs.getString(centerNameKey(type), null)
            ?: (if (type == "market") prefs.getString("saved_center_name", "") else "") ?: ""
        val subtitle = prefs.getString(subtitleKey(type), null)
            ?: (if (type == "market") prefs.getString("saved_subtitle", "") else "") ?: ""
        val purchaserLabel = prefs.getString(purchaserLabelKey(type), null)
            ?: (if (type == "market") prefs.getString("saved_purchaser_label", "") else "") ?: ""
        return Triple(centerName, subtitle, purchaserLabel)
    }

    fun saveSettings(billType: String, centerName: String, subtitle: String, purchaserLabel: String) {
        prefs.edit()
            .putString(centerNameKey(billType), centerName.trim())
            .putString(subtitleKey(billType), subtitle.trim())
            .putString(purchaserLabelKey(billType), purchaserLabel.trim())
            .apply()
        // Only push these into the live in-progress bill if we're editing settings for the
        // type that's actually active right now — otherwise editing "খাবার বিল" settings
        // while a "যাতায়াত ভাড়া" memo is open would overwrite what's on screen.
        if (_currentBillState.value.billType == billType) {
            _currentBillState.update {
                it.copy(
                    centerName = centerName.trim(),
                    subtitle = subtitle.trim(),
                    purchaserLabel = purchaserLabel.trim()
                )
            }
        }
        viewModelScope.launch { _uiEvent.emit("সেটিংস সফলভাবে সংরক্ষণ করা হয়েছে!") }
    }

    fun onDataImported() {
        loadQuickPresets()
        val currentType = _currentBillState.value.billType
        val (savedCenterName, savedSubtitle, savedPurchaserLabel) = getSavedSettingsForType(currentType)
        _currentBillState.update {
            it.copy(
                centerName = savedCenterName,
                subtitle = savedSubtitle,
                purchaserLabel = savedPurchaserLabel
            )
        }
        _themeMode.value = prefs.getString("app_theme_mode", "system") ?: "system"
        _themeColor.value = prefs.getString("app_theme_color", "emerald") ?: "emerald"
        _appLanguage.value = prefs.getString("app_language", "bn") ?: "bn"
        _fontScale.value = prefs.getFloat("app_font_scale", 1.0f)
        viewModelScope.launch {
            val msg = if (_appLanguage.value == "en") "Data imported successfully!" else "ডাটা সফলভাবে অ্যাপে ইমপোর্ট হয়েছে!"
            _uiEvent.emit(msg)
        }
    }

    fun resetAllUserData() {
        prefs.edit().clear().apply()
        _quickPresets.value = emptyList()
        resetToInitialTemplate()
        viewModelScope.launch { _uiEvent.emit("সমস্ত সংরক্ষিত তথ্য রিসেট করা হয়েছে") }
    }

    fun resetToInitialTemplate() {
        val todayStr = dateFormat.format(Date())
        val (savedCenterName, savedSubtitle, savedPurchaserLabel) = getSavedSettingsForType("market")

        _currentBillState.value = CurrentBillState(
            editingBillId = 0L,
            dateString = todayStr,
            centerName = savedCenterName,
            subtitle = savedSubtitle,
            purchaserName = "",
            purchaserLabel = savedPurchaserLabel,
            billType = "market",
            items = listOf(
                BillItem(name = "", quantity = "", rate = "0", amount = 0.0)
            )
        )
    }

    fun setBillType(type: String) {
        _currentBillState.update { state ->
            if (state.billType == type) return@update state
            // The "rate" field means different things in each mode (৳ price vs. "যাত্রী" /
            // traveler name), so clear it when switching types to avoid a leftover number
            // being misread as a traveler name, or vice versa.
            val clearedItems = state.items.map { it.copy(rate = "0") }
            // Each bill type keeps its own saved title/subtitle/signature, so load them
            // whenever the user switches type from the app bar dropdown.
            val (newCenterName, newSubtitle, newPurchaserLabel) = getSavedSettingsForType(type)
            state.copy(
                billType = type,
                items = clearedItems,
                centerName = newCenterName,
                subtitle = newSubtitle,
                purchaserLabel = newPurchaserLabel
            )
        }
    }

    fun updateDate(newDate: String) {
        _currentBillState.update { it.copy(dateString = newDate) }
    }

    /** Toggles whether the signature box/line should be included when printing/
     * sharing this bill. Some bills (e.g. informal quick memos) don't need one. */
    fun setShowSignature(show: Boolean) {
        _currentBillState.update { it.copy(showSignature = show) }
    }

    fun updateCenterName(newName: String) {
        _currentBillState.update { it.copy(centerName = newName) }
    }

    fun updateSubtitle(newSubtitle: String) {
        _currentBillState.update { it.copy(subtitle = newSubtitle) }
    }

    fun updatePurchaserName(name: String) {
        _currentBillState.update { it.copy(purchaserName = name) }
    }

    fun updatePurchaserLabel(label: String) {
        _currentBillState.update { it.copy(purchaserLabel = label) }
    }

    fun addItemRow() {
        _currentBillState.update { state ->
            if (state.items.size >= 18) {
                viewModelScope.launch { _uiEvent.emit("সর্বোচ্চ ১৮ টি আইটেম যোগ করা সম্ভব") }
                return@update state
            }
            val updatedItems = state.items.toMutableList()
            updatedItems.add(BillItem(name = "", quantity = "", rate = "0", amount = 0.0))
            state.copy(items = updatedItems)
        }
    }

    /** Reorders a bill item from [fromIndex] to [toIndex], used for drag-to-reorder in the UI. */
    fun moveItem(fromIndex: Int, toIndex: Int) {
        _currentBillState.update { state ->
            val list = state.items
            if (fromIndex !in list.indices || toIndex !in list.indices || fromIndex == toIndex) {
                return@update state
            }
            val mutableList = list.toMutableList()
            val moved = mutableList.removeAt(fromIndex)
            mutableList.add(toIndex, moved)
            state.copy(items = mutableList)
        }
    }

    fun addQuickPresetItem(name: String, qty: String, rate: String = "", amount: String = "") {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return

        val rateVal = BengaliUtils.parseBengaliNumber(rate)
        val amountVal = BengaliUtils.parseBengaliNumber(amount)
        val qtyVal = extractNumber(qty)

        val calcAmount = when {
            amountVal > 0 -> amountVal
            rateVal > 0 -> if (qtyVal > 0) rateVal * qtyVal else rateVal
            else -> 0.0
        }

        val calcRate = when {
            rate.isNotBlank() && rate != "0" -> rate
            else -> "0"
        }

        _currentBillState.update { state ->
            val updatedItems = state.items.toMutableList()
            val existingIndex = updatedItems.indexOfFirst { it.name.trim() == trimmedName && it.name.isNotBlank() }
            if (existingIndex != -1) {
                // Item already in memo -> Unselect / Remove
                if (updatedItems.size > 1) {
                    updatedItems.removeAt(existingIndex)
                } else {
                    updatedItems[0] = BillItem(id = updatedItems[0].id, name = "", quantity = "", rate = "0", amount = 0.0)
                }
                return@update state.copy(items = updatedItems)
            }

            // Item not in memo -> Add to memo
            val emptyIndex = updatedItems.indexOfFirst { it.name.isBlank() && it.quantity.isBlank() }
            val newItem = BillItem(name = trimmedName, quantity = qty, rate = calcRate, amount = calcAmount)
            if (emptyIndex != -1) {
                updatedItems[emptyIndex] = newItem
            } else {
                if (updatedItems.size >= 18) {
                    viewModelScope.launch { _uiEvent.emit("সর্বোচ্চ ১৮ টি আইটেম যোগ করা সম্ভব") }
                    return@update state
                }
                updatedItems.add(newItem)
            }
            state.copy(items = updatedItems)
        }
    }

    fun updateItemName(id: String, name: String) {
        _currentBillState.update { state ->
            val updated = state.items.map { item ->
                if (item.id == id) item.copy(name = name) else item
            }
            state.copy(items = updated)
        }
    }

    fun updateItemQuantity(id: String, qty: String) {
        _currentBillState.update { state ->
            val updated = state.items.map { item ->
                if (item.id == id) item.copy(quantity = qty) else item
            }
            state.copy(items = updated)
        }
    }

    fun updateItemRate(id: String, rate: String) {
        _currentBillState.update { state ->
            val updated = state.items.map { item ->
                if (item.id == id) {
                    val rateVal = BengaliUtils.parseBengaliNumber(rate)
                    val qtyVal = extractNumber(item.quantity)
                    val calcAmount = if (qtyVal > 0) rateVal * qtyVal else rateVal
                    item.copy(rate = rate, amount = calcAmount)
                } else item
            }
            state.copy(items = updated)
        }
    }

    /**
     * Used only for transport ("যাতায়াত ভাড়া") bills, where the column that used to be
     * "দর" (rate) is repurposed to record "যাত্রী" (who is traveling). Unlike [updateItemRate],
     * this never recalculates [BillItem.amount] since the value here is a name/text, not a price.
     */
    fun updateItemTraveler(id: String, travelerName: String) {
        _currentBillState.update { state ->
            val updated = state.items.map { item ->
                if (item.id == id) item.copy(rate = travelerName) else item
            }
            state.copy(items = updated)
        }
    }

    fun updateItemAmount(id: String, amountStr: String) {
        _currentBillState.update { state ->
            val amountVal = BengaliUtils.parseBengaliNumber(amountStr)
            val updated = state.items.map { item ->
                if (item.id == id) item.copy(amount = amountVal) else item
            }
            state.copy(items = updated)
        }
    }

    fun removeItemRow(id: String) {
        _currentBillState.update { state ->
            if (state.items.size <= 1) {
                // If only 1 row left, reset it instead of deleting
                state.copy(items = listOf(BillItem(name = "", quantity = "", rate = "0", amount = 0.0)))
            } else {
                state.copy(items = state.items.filterNot { it.id == id })
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun saveCurrentBill(notifyUser: Boolean = true, onSaved: ((Long) -> Unit)? = null) {
        val currentState = _currentBillState.value
        val validItems = currentState.items.filter { it.name.isNotBlank() || it.amount > 0 }
        
        if (validItems.isEmpty()) {
            if (notifyUser) {
                viewModelScope.launch { _uiEvent.emit("কমপক্ষে একটি আইটেমের বিবরণ বা টাকা লিখুন") }
            }
            return
        }

        viewModelScope.launch {
            val total = validItems.sumOf { it.amount }
            val timestamp = System.currentTimeMillis()

            val id = repository.saveBill(
                id = currentState.editingBillId,
                dateString = currentState.dateString,
                timestamp = timestamp,
                purchaserName = currentState.purchaserName,
                centerName = currentState.centerName,
                subtitle = currentState.subtitle,
                note = "",
                items = validItems,
                totalAmount = total,
                billType = currentState.billType,
                showSignature = currentState.showSignature
            )

            _currentBillState.update { it.copy(editingBillId = id) }
            if (notifyUser) {
                _uiEvent.emit("বিল সফলভাবে সংরক্ষণ করা হয়েছে!")
            }
            onSaved?.invoke(id)
        }
    }

    fun loadBillForEditing(bill: FoodBillUiModel) {
        val savedPurchaserLabel = getSavedSettingsForType(bill.billType).third

        _currentBillState.value = CurrentBillState(
            editingBillId = bill.id,
            dateString = bill.dateString,
            centerName = bill.centerName,
            subtitle = bill.subtitle,
            purchaserName = bill.purchaserName,
            purchaserLabel = savedPurchaserLabel,
            billType = bill.billType,
            showSignature = bill.showSignature,
            items = bill.items.ifEmpty { listOf(BillItem(name = "", quantity = "", rate = "0", amount = 0.0)) }
        )
        viewModelScope.launch {
            _uiEvent.emit("${bill.dateString} তারিখের বিল লোড করা হয়েছে")
        }
    }

    fun deleteBill(id: Long) {
        viewModelScope.launch {
            repository.deleteBill(id)
            if (_currentBillState.value.editingBillId == id) {
                resetToInitialTemplate()
            }
            _uiEvent.emit("বিলটি মুছে ফেলা হয়েছে")
        }
    }

    private fun extractNumber(str: String): Double {
        val englishStr = BengaliUtils.toEnglishDigits(str)
        val regex = Regex("""\d+(\.\d+)?""")
        val match = regex.find(englishStr)
        return match?.value?.toDoubleOrNull() ?: 0.0
    }
}

data class CurrentBillState(
    val editingBillId: Long = 0L,
    val dateString: String = "",
    val centerName: String = "",
    val subtitle: String = "",
    val purchaserName: String = "",
    val purchaserLabel: String = "",
    val approverLabel: String = "",
    val billType: String = "market", // "market" (বাজার লিস্ট) or "transport" (যাতায়াত ভাড়া)
    val showSignature: Boolean = true, // false হলে স্বাক্ষরের ঘর প্রিন্ট/PDF/ছবিতে দেখানো হবে না
    val items: List<BillItem> = emptyList()
) {
    val totalAmount: Double
        get() = items.sumOf { it.amount }
}
