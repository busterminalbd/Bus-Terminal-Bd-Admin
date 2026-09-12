package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "advance_salaries")
data class AdvanceSalaryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val companyName: String = "",
    val companySubtitle: String = "",
    val applicationNo: String = "",
    val dateString: String = "",
    val applicantName: String = "",
    val employeeId: String = "",
    val designation: String = "",
    val department: String = "",
    val contactNumber: String = "",
    val monthlySalary: Double = 0.0,
    val advanceAmount: Double = 0.0,
    val advanceAmountInWords: String = "",
    val reason: String = "",
    val repaymentType: String = "one_time", // "one_time" | "installments"
    val installmentCount: Int = 1,
    val installmentAmountPerMonth: Double = 0.0,
    val deductionStartMonth: String = "",
    val previousAdvancePending: Double = 0.0,
    val guarantorOrRecommendedBy: String = "",
    val remarks: String = "",
    val status: String = "APPROVED", // "PENDING", "APPROVED", "PAID"
    val showSignatures: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
