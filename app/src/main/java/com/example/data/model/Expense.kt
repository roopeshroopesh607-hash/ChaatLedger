package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val shopName: String, // Vendor/Supplier purchase was made from
    val category: String, // Ingredients, Rent, Utilities, Staff, Equipment, Other
    val description: String,
    val amount: Double,
    val paymentType: String, // Credit, Cash
    val paymentStatus: String = PaymentStatus.PAID, // Paid, Pending
    val paidDate: Long? = null, // Settled date for credit expense, nullable
    val billImageUri: String? = null, // Local storage image URI, nullable
    val purchaseDate: Long = System.currentTimeMillis(), // Date of purchase, backdatable
    val timestamp: Long = System.currentTimeMillis(), // When entry was logged
    val enteredBy: String = "",
    val firestoreId: String = ""
)

object PaymentStatus {
    const val PAID = "Paid"
    const val PENDING = "Pending"
    val ALL = listOf(PAID, PENDING)
}

object ExpenseCategories {
    val ALL = listOf(
        "Ingredients",
        "Rent",
        "Utilities",
        "Staff",
        "Equipment",
        "Other"
    )
}

object PaymentTypes {
    const val CASH = "Cash"
    const val CREDIT = "Credit"
    val ALL = listOf(CASH, CREDIT)
}
