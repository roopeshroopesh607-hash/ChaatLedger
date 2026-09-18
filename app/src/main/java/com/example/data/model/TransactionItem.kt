package com.example.data.model

enum class TransactionType {
    SALE,
    EXPENSE
}

data class TransactionItem(
    val id: Long,
    val type: TransactionType,
    val title: String,
    val subtitle: String,
    val category: String,
    val amount: Double,
    val timestamp: Long,
    val badgeInfo: String,
    val enteredBy: String,
    val paymentStatus: String? = null,
    val billImageUri: String? = null
)
