package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_bills")
data class FoodBillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateString: String, // e.g. "03/08/2026"
    val timestamp: Long, // epoch millis
    val purchaserName: String = "",
    val centerName: String = "",
    val subtitle: String = "",
    val note: String = "",
    val totalAmount: Double,
    val itemsJson: String, // Serialized list of items
    val billType: String = "market", // "market" (বাজার লিস্ট) or "transport" (যাতায়াত ভাড়া)
    val showSignature: Boolean = true // false হলে স্বাক্ষরের ঘর প্রিন্ট/PDF/ছবিতে দেখানো হবে না
)

data class BillItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val quantity: String,
    val rate: String = "0",
    val amount: Double = 0.0
)
