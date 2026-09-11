package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medical_records")
data class MedicalRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val patientId: String, // e.g. AB260948
    val code: String, // e.g. AF07, MD-01, J007 (DUE)
    val patientName: String = "", // e.g. TUHIN ALI, MD ALIM HAQUE
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(tableName = "code_groups")
data class CodeGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupName: String,
    val description: String = ""
)

@Entity(tableName = "code_group_items")
data class CodeGroupItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val code: String
)

@Entity(tableName = "preset_medical_codes")
data class PresetMedicalCodeEntity(
    @PrimaryKey val code: String,
    val name: String = "",
    val category: String = "General"
)
