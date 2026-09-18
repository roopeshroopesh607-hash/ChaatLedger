package com.example.data.sync

import com.example.data.model.Expense
import com.example.data.model.PaymentStatus
import com.example.data.model.Sale
import java.util.UUID

object SyncMappers {

    fun generateSaleFirestoreId(date: Long): String {
        return "sale_$date"
    }

    fun generateExpenseFirestoreId(): String {
        return "exp_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
    }

    fun saleToMap(sale: Sale): Map<String, Any?> {
        return mapOf(
            "date" to sale.date,
            "cashTotal" to sale.cashTotal,
            "paytmTotal" to sale.paytmTotal,
            "totalAmount" to sale.totalAmount,
            "timestamp" to sale.timestamp,
            "enteredBy" to sale.enteredBy,
            "firestoreId" to sale.firestoreId
        )
    }

    fun saleFromMap(docId: String, data: Map<String, Any?>): Sale {
        val date = (data["date"] as? Number)?.toLong() ?: 0L
        val cash = (data["cashTotal"] as? Number)?.toDouble() ?: 0.0
        val paytm = (data["paytmTotal"] as? Number)?.toDouble() ?: 0.0
        val total = (data["totalAmount"] as? Number)?.toDouble() ?: (cash + paytm)
        val timestamp = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
        val enteredBy = (data["enteredBy"] as? String).orEmpty()
        val firestoreId = (data["firestoreId"] as? String).takeUnless { it.isNullOrBlank() } ?: docId

        return Sale(
            id = 0L,
            date = date,
            cashTotal = cash,
            paytmTotal = paytm,
            totalAmount = total,
            timestamp = timestamp,
            enteredBy = enteredBy,
            firestoreId = firestoreId
        )
    }

    fun saleToJson(sale: Sale): String {
        val map = mapOf<String, Any?>(
            "id" to sale.id,
            "date" to sale.date,
            "cashTotal" to sale.cashTotal,
            "paytmTotal" to sale.paytmTotal,
            "totalAmount" to sale.totalAmount,
            "timestamp" to sale.timestamp,
            "enteredBy" to sale.enteredBy,
            "firestoreId" to sale.firestoreId
        )
        return serializeMap(map)
    }

    fun saleFromJson(str: String): Sale {
        val map = parseFlatJson(str)
        val cash = map["cashTotal"]?.toDoubleOrNull() ?: 0.0
        val paytm = map["paytmTotal"]?.toDoubleOrNull() ?: 0.0
        val total = map["totalAmount"]?.toDoubleOrNull() ?: (cash + paytm)
        return Sale(
            id = map["id"]?.toLongOrNull() ?: 0L,
            date = map["date"]?.toLongOrNull() ?: 0L,
            cashTotal = cash,
            paytmTotal = paytm,
            totalAmount = total,
            timestamp = map["timestamp"]?.toLongOrNull() ?: System.currentTimeMillis(),
            enteredBy = map["enteredBy"].orEmpty(),
            firestoreId = map["firestoreId"].orEmpty()
        )
    }

    fun expenseToMap(expense: Expense): Map<String, Any?> {
        return mapOf(
            "shopName" to expense.shopName,
            "category" to expense.category,
            "description" to expense.description,
            "amount" to expense.amount,
            "paymentType" to expense.paymentType,
            "paymentStatus" to expense.paymentStatus,
            "paidDate" to expense.paidDate,
            "billImageUri" to expense.billImageUri,
            "purchaseDate" to expense.purchaseDate,
            "timestamp" to expense.timestamp,
            "enteredBy" to expense.enteredBy,
            "firestoreId" to expense.firestoreId
        )
    }

    fun expenseFromMap(docId: String, data: Map<String, Any?>): Expense {
        val shopName = (data["shopName"] as? String).orEmpty()
        val category = (data["category"] as? String).orEmpty()
        val description = (data["description"] as? String).orEmpty()
        val amount = (data["amount"] as? Number)?.toDouble() ?: 0.0
        val paymentType = (data["paymentType"] as? String).orEmpty()
        val paymentStatus = (data["paymentStatus"] as? String) ?: PaymentStatus.PAID
        val paidDate = (data["paidDate"] as? Number)?.toLong()
        val billImageUri = data["billImageUri"] as? String
        val purchaseDate = (data["purchaseDate"] as? Number)?.toLong() ?: System.currentTimeMillis()
        val timestamp = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
        val enteredBy = (data["enteredBy"] as? String).orEmpty()
        val firestoreId = (data["firestoreId"] as? String).takeUnless { it.isNullOrBlank() } ?: docId

        return Expense(
            id = 0L,
            shopName = shopName,
            category = category,
            description = description,
            amount = amount,
            paymentType = paymentType,
            paymentStatus = paymentStatus,
            paidDate = paidDate,
            billImageUri = billImageUri,
            purchaseDate = purchaseDate,
            timestamp = timestamp,
            enteredBy = enteredBy,
            firestoreId = firestoreId
        )
    }

    fun expenseToJson(expense: Expense): String {
        val map = mapOf<String, Any?>(
            "id" to expense.id,
            "shopName" to expense.shopName,
            "category" to expense.category,
            "description" to expense.description,
            "amount" to expense.amount,
            "paymentType" to expense.paymentType,
            "paymentStatus" to expense.paymentStatus,
            "paidDate" to expense.paidDate,
            "billImageUri" to expense.billImageUri,
            "purchaseDate" to expense.purchaseDate,
            "timestamp" to expense.timestamp,
            "enteredBy" to expense.enteredBy,
            "firestoreId" to expense.firestoreId
        )
        return serializeMap(map)
    }

    fun expenseFromJson(str: String): Expense {
        val map = parseFlatJson(str)
        return Expense(
            id = map["id"]?.toLongOrNull() ?: 0L,
            shopName = map["shopName"].orEmpty(),
            category = map["category"].orEmpty(),
            description = map["description"].orEmpty(),
            amount = map["amount"]?.toDoubleOrNull() ?: 0.0,
            paymentType = map["paymentType"].orEmpty(),
            paymentStatus = map["paymentStatus"] ?: PaymentStatus.PAID,
            paidDate = map["paidDate"]?.takeIf { it != "null" }?.toLongOrNull(),
            billImageUri = map["billImageUri"]?.takeIf { it != "null" && it.isNotBlank() },
            purchaseDate = map["purchaseDate"]?.toLongOrNull() ?: System.currentTimeMillis(),
            timestamp = map["timestamp"]?.toLongOrNull() ?: System.currentTimeMillis(),
            enteredBy = map["enteredBy"].orEmpty(),
            firestoreId = map["firestoreId"].orEmpty()
        )
    }

    private fun serializeMap(map: Map<String, Any?>): String {
        return map.entries.joinToString(prefix = "{", postfix = "}") { (k, v) ->
            val valueStr = when (v) {
                null -> "null"
                is Number, is Boolean -> v.toString()
                else -> "\"${v.toString().replace("\\", "\\\\").replace("\"", "\\\"")}\""
            }
            "\"$k\":$valueStr"
        }
    }

    private fun parseFlatJson(jsonStr: String): Map<String, String> {
        val trimmed = jsonStr.trim().removeSurrounding("{", "}").trim()
        if (trimmed.isEmpty()) return emptyMap()
        val result = mutableMapOf<String, String>()
        val regex = Regex("\"([^\"]+)\"\\s*:\\s*(\"[^\"]*\"|null|[0-9.-]+|true|false)")
        for (match in regex.findAll(trimmed)) {
            val key = match.groupValues[1]
            val rawValue = match.groupValues[2]
            val cleanValue = if (rawValue.startsWith("\"") && rawValue.endsWith("\"")) {
                rawValue.substring(1, rawValue.length - 1).replace("\\\"", "\"").replace("\\\\", "\\")
            } else {
                rawValue
            }
            result[key] = cleanValue
        }
        return result
    }
}
