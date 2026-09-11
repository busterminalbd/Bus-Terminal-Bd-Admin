package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class MedicalRepository(private val medicalDao: MedicalDao) {

    val allRecords: Flow<List<MedicalRecordEntity>> = medicalDao.getAllRecords()
    val allCodeGroups: Flow<List<CodeGroupEntity>> = medicalDao.getAllCodeGroups()
    val allGroupItems: Flow<List<CodeGroupItemEntity>> = medicalDao.getAllGroupItems()
    val allPresetCodes: Flow<List<PresetMedicalCodeEntity>> = medicalDao.getAllPresetCodes()

    fun getRecordsByDate(date: String): Flow<List<MedicalRecordEntity>> {
        return medicalDao.getRecordsByDate(date)
    }

    suspend fun getRecordsByDateList(date: String): List<MedicalRecordEntity> {
        return medicalDao.getRecordsByDateList(date)
    }

    fun getRecordsBetweenDates(startDate: String, endDate: String): Flow<List<MedicalRecordEntity>> {
        return medicalDao.getRecordsBetweenDates(startDate, endDate)
    }

    suspend fun getAllRecordsList(): List<MedicalRecordEntity> {
        return medicalDao.getAllRecordsList()
    }

    suspend fun saveRecords(records: List<MedicalRecordEntity>) {
        medicalDao.insertRecords(records)
    }

    suspend fun saveRecord(record: MedicalRecordEntity): Long {
        return medicalDao.insertRecord(record)
    }

    suspend fun updateRecord(record: MedicalRecordEntity) {
        medicalDao.updateRecord(record)
    }

    suspend fun deleteRecord(id: Long) {
        medicalDao.deleteRecordById(id)
    }

    suspend fun deleteRecordsByDate(date: String) {
        medicalDao.deleteRecordsByDate(date)
    }

    suspend fun deleteRecordsByIds(ids: List<Long>) {
        medicalDao.deleteRecordsByIds(ids)
    }

    suspend fun deleteAllRecords() {
        medicalDao.deleteAllRecords()
    }

    // --- Preset Codes ---
    suspend fun savePresetCode(code: PresetMedicalCodeEntity) {
        medicalDao.insertPresetCode(code)
    }

    suspend fun savePresetCodes(codes: List<PresetMedicalCodeEntity>) {
        medicalDao.insertPresetCodes(codes)
    }

    suspend fun deletePresetCode(code: String) {
        medicalDao.deletePresetCode(code)
    }

    suspend fun seedDefaultPresetCodesIfEmpty() {
        val existing = medicalDao.getAllPresetCodesList()
        if (existing.isEmpty()) {
            val defaults = listOf(
                PresetMedicalCodeEntity(code = "AF07", name = "AF07", category = "General"),
                PresetMedicalCodeEntity(code = "MD-01", name = "MD-01", category = "General"),
                PresetMedicalCodeEntity(code = "J007 (DUE)", name = "J007 (DUE)", category = "Due"),
                PresetMedicalCodeEntity(code = "USG", name = "Ultrasonography", category = "Imaging"),
                PresetMedicalCodeEntity(code = "CBC", name = "Complete Blood Count", category = "Pathology"),
                PresetMedicalCodeEntity(code = "ECG", name = "Electrocardiogram", category = "Cardiology"),
                PresetMedicalCodeEntity(code = "RBS", name = "Random Blood Sugar", category = "Pathology"),
                PresetMedicalCodeEntity(code = "X-RAY", name = "X-Ray Chest/Bone", category = "Imaging"),
                PresetMedicalCodeEntity(code = "SERUM", name = "Serum Creatinine", category = "Pathology"),
                PresetMedicalCodeEntity(code = "URINE", name = "Urine R/E", category = "Pathology")
            )
            medicalDao.insertPresetCodes(defaults)
        }
    }

    // --- Code Groups (Owners) ---
    suspend fun createCodeGroup(groupName: String, description: String, codes: List<String>): Pair<Long, List<String>> {
        val group = CodeGroupEntity(groupName = groupName, description = description)
        val groupId = medicalDao.insertCodeGroup(group)
        val skipped = mutableListOf<String>()
        val toInsert = mutableListOf<CodeGroupItemEntity>()
        for (raw in codes.map { it.trim() }.filter { it.isNotBlank() }.distinct()) {
            val existing = medicalDao.findGroupItemByCode(raw)
            if (existing != null) skipped.add(raw) else toInsert.add(CodeGroupItemEntity(groupId = groupId, code = raw))
        }
        if (toInsert.isNotEmpty()) medicalDao.insertGroupItems(toInsert)
        return Pair(groupId, skipped)
    }

    suspend fun updateCodeGroup(groupId: Long, groupName: String, description: String, codes: List<String>): List<String> {
        val group = CodeGroupEntity(id = groupId, groupName = groupName, description = description)
        medicalDao.insertCodeGroup(group)
        medicalDao.deleteGroupItemsByGroupId(groupId)
        val skipped = mutableListOf<String>()
        val toInsert = mutableListOf<CodeGroupItemEntity>()
        for (raw in codes.map { it.trim() }.filter { it.isNotBlank() }.distinct()) {
            val existing = medicalDao.findGroupItemByCode(raw)
            if (existing != null && existing.groupId != groupId) {
                skipped.add(raw)
            } else {
                toInsert.add(CodeGroupItemEntity(groupId = groupId, code = raw))
            }
        }
        if (toInsert.isNotEmpty()) medicalDao.insertGroupItems(toInsert)
        return skipped
    }

    suspend fun deleteCodeGroup(groupId: Long) {
        medicalDao.deleteGroupItemsByGroupId(groupId)
        medicalDao.deleteCodeGroup(groupId)
    }

    suspend fun assignCodeToGroup(groupId: Long, code: String): Boolean {
        val raw = code.trim()
        if (raw.isBlank()) return false
        val existing = medicalDao.findGroupItemByCode(raw)
        if (existing != null && existing.groupId != groupId) return false
        if (existing == null) {
            medicalDao.insertGroupItems(listOf(CodeGroupItemEntity(groupId = groupId, code = raw)))
        }
        return true
    }

    suspend fun removeCodeFromGroup(code: String) {
        medicalDao.deleteGroupItemByCode(code.trim())
    }

    suspend fun getOwnerNameForCode(code: String): String? {
        val raw = code.trim()
        if (raw.isBlank()) return null
        val item = medicalDao.findGroupItemByCode(raw) ?: return null
        val groups = medicalDao.getAllCodeGroupsList()
        return groups.find { it.id == item.groupId }?.groupName
    }
}
