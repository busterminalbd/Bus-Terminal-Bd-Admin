package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicalDao {

    // --- Medical Records ---
    @Query("SELECT * FROM medical_records ORDER BY id ASC")
    fun getAllRecords(): Flow<List<MedicalRecordEntity>>

    @Query("SELECT * FROM medical_records ORDER BY id ASC")
    suspend fun getAllRecordsList(): List<MedicalRecordEntity>

    @Query("SELECT * FROM medical_records WHERE date = :date ORDER BY id ASC")
    fun getRecordsByDate(date: String): Flow<List<MedicalRecordEntity>>

    @Query("SELECT * FROM medical_records WHERE date = :date ORDER BY id ASC")
    suspend fun getRecordsByDateList(date: String): List<MedicalRecordEntity>

    @Query("SELECT * FROM medical_records WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC, id ASC")
    fun getRecordsBetweenDates(startDate: String, endDate: String): Flow<List<MedicalRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: MedicalRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<MedicalRecordEntity>)

    @Update
    suspend fun updateRecord(record: MedicalRecordEntity)

    @Query("DELETE FROM medical_records WHERE id = :id")
    suspend fun deleteRecordById(id: Long)

    @Query("DELETE FROM medical_records WHERE date = :date")
    suspend fun deleteRecordsByDate(date: String)

    @Query("DELETE FROM medical_records WHERE id IN (:ids)")
    suspend fun deleteRecordsByIds(ids: List<Long>)

    @Query("DELETE FROM medical_records")
    suspend fun deleteAllRecords()

    // --- Code Groups (Owners) ---
    @Query("SELECT * FROM code_groups ORDER BY groupName ASC")
    fun getAllCodeGroups(): Flow<List<CodeGroupEntity>>

    @Query("SELECT * FROM code_groups ORDER BY groupName ASC")
    suspend fun getAllCodeGroupsList(): List<CodeGroupEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCodeGroup(group: CodeGroupEntity): Long

    @Query("DELETE FROM code_groups WHERE id = :groupId")
    suspend fun deleteCodeGroup(groupId: Long)

    // --- Code Group Items ---
    @Query("SELECT * FROM code_group_items ORDER BY id ASC")
    fun getAllGroupItems(): Flow<List<CodeGroupItemEntity>>

    @Query("SELECT * FROM code_group_items ORDER BY id ASC")
    suspend fun getAllGroupItemsList(): List<CodeGroupItemEntity>

    @Query("SELECT * FROM code_group_items WHERE groupId = :groupId")
    suspend fun getGroupItemsByGroupId(groupId: Long): List<CodeGroupItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupItems(items: List<CodeGroupItemEntity>)

    @Query("DELETE FROM code_group_items WHERE groupId = :groupId")
    suspend fun deleteGroupItemsByGroupId(groupId: Long)

    @Query("SELECT * FROM code_group_items WHERE code = :code LIMIT 1")
    suspend fun findGroupItemByCode(code: String): CodeGroupItemEntity?

    @Query("DELETE FROM code_group_items WHERE code = :code")
    suspend fun deleteGroupItemByCode(code: String)

    // --- Preset Medical Codes ---
    @Query("SELECT * FROM preset_medical_codes ORDER BY code ASC")
    fun getAllPresetCodes(): Flow<List<PresetMedicalCodeEntity>>

    @Query("SELECT * FROM preset_medical_codes ORDER BY code ASC")
    suspend fun getAllPresetCodesList(): List<PresetMedicalCodeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPresetCodes(codes: List<PresetMedicalCodeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPresetCode(code: PresetMedicalCodeEntity)

    @Query("DELETE FROM preset_medical_codes WHERE code = :code")
    suspend fun deletePresetCode(code: String)

    @Query("DELETE FROM preset_medical_codes")
    suspend fun deleteAllPresetCodes()
}
