package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodBillDao {
    @Query("SELECT * FROM food_bills ORDER BY timestamp DESC")
    fun getAllBills(): Flow<List<FoodBillEntity>>

    @Query("SELECT * FROM food_bills WHERE id = :id LIMIT 1")
    suspend fun getBillById(id: Long): FoodBillEntity?

    @Query("SELECT * FROM food_bills WHERE dateString = :dateString ORDER BY timestamp DESC")
    fun getBillsByDate(dateString: String): Flow<List<FoodBillEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: FoodBillEntity): Long

    @Update
    suspend fun updateBill(bill: FoodBillEntity)

    @Delete
    suspend fun deleteBill(bill: FoodBillEntity)

    @Query("DELETE FROM food_bills WHERE id = :id")
    suspend fun deleteBillById(id: Long)

    @Query("SELECT SUM(totalAmount) FROM food_bills")
    fun getTotalSpentAllTime(): Flow<Double?>

    @Query("SELECT * FROM food_bills")
    suspend fun getAllBillsList(): List<FoodBillEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBills(bills: List<FoodBillEntity>): List<Long>
}
