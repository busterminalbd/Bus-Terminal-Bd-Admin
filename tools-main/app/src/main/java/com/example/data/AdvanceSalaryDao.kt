package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AdvanceSalaryDao {

    @Query("SELECT * FROM advance_salaries ORDER BY createdAt DESC")
    fun getAllApplications(): Flow<List<AdvanceSalaryEntity>>

    @Query("SELECT * FROM advance_salaries WHERE id = :id LIMIT 1")
    suspend fun getApplicationById(id: Long): AdvanceSalaryEntity?

    @Query("""
        SELECT * FROM advance_salaries 
        WHERE applicantName LIKE '%' || :query || '%' 
           OR employeeId LIKE '%' || :query || '%' 
           OR designation LIKE '%' || :query || '%' 
           OR department LIKE '%' || :query || '%' 
           OR applicationNo LIKE '%' || :query || '%'
        ORDER BY createdAt DESC
    """)
    fun searchApplications(query: String): Flow<List<AdvanceSalaryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApplication(entity: AdvanceSalaryEntity): Long

    @Update
    suspend fun updateApplication(entity: AdvanceSalaryEntity)

    @Query("DELETE FROM advance_salaries WHERE id = :id")
    suspend fun deleteApplicationById(id: Long)

    @Query("DELETE FROM advance_salaries")
    suspend fun deleteAllApplications()

    @Query("SELECT * FROM advance_salaries")
    suspend fun getAllApplicationsList(): List<AdvanceSalaryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApplications(entities: List<AdvanceSalaryEntity>): List<Long>
}
