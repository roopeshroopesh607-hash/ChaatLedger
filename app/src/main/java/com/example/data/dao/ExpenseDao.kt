package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Expense
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY purchaseDate DESC, timestamp DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE purchaseDate >= :startTime AND purchaseDate <= :endTime ORDER BY purchaseDate DESC, timestamp DESC")
    fun getExpensesBetween(startTime: Long, endTime: Long): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE id = :id")
    fun getExpenseByIdFlow(id: Long): Flow<Expense?>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseById(id: Long): Expense?

    @Query("SELECT SUM(amount) FROM expenses WHERE purchaseDate >= :startTime AND purchaseDate <= :endTime")
    fun getTotalExpenseAmountBetween(startTime: Long, endTime: Long): Flow<Double?>

    @Query("SELECT COUNT(*) FROM expenses WHERE purchaseDate >= :startTime AND purchaseDate <= :endTime")
    fun getExpensesCountBetween(startTime: Long, endTime: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Long)

    @Query("UPDATE expenses SET paymentStatus = :status, paidDate = :paidDate WHERE id = :id")
    suspend fun updatePaymentStatus(id: Long, status: String, paidDate: Long?)

    @Query("SELECT * FROM expenses WHERE firestoreId = :firestoreId LIMIT 1")
    suspend fun getExpenseByFirestoreId(firestoreId: String): Expense?

    @Query("DELETE FROM expenses WHERE firestoreId = :firestoreId")
    suspend fun deleteExpenseByFirestoreId(firestoreId: String)
}
