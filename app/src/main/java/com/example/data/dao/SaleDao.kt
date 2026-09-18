package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Sale
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales ORDER BY date DESC, timestamp DESC")
    fun getAllSales(): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE date >= :startTime AND date <= :endTime ORDER BY date DESC")
    fun getSalesBetween(startTime: Long, endTime: Long): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE id = :id")
    fun getSaleByIdFlow(id: Long): Flow<Sale?>

    @Query("SELECT * FROM sales WHERE id = :id")
    suspend fun getSaleById(id: Long): Sale?

    @Query("SELECT * FROM sales WHERE date = :date LIMIT 1")
    fun getSaleByDateFlow(date: Long): Flow<Sale?>

    @Query("SELECT * FROM sales WHERE date = :date LIMIT 1")
    suspend fun getSaleByDate(date: Long): Sale?

    @Query("SELECT * FROM sales WHERE date >= :startOfDay AND date <= :endOfDay LIMIT 1")
    suspend fun findSaleForDay(startOfDay: Long, endOfDay: Long): Sale?

    @Query("SELECT SUM(totalAmount) FROM sales WHERE date >= :startTime AND date <= :endTime")
    fun getTotalSalesAmountBetween(startTime: Long, endTime: Long): Flow<Double?>

    @Query("SELECT COUNT(*) FROM sales WHERE date >= :startTime AND date <= :endTime")
    fun getSalesCountBetween(startTime: Long, endTime: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: Sale): Long

    @Update
    suspend fun updateSale(sale: Sale)

    @Delete
    suspend fun deleteSale(sale: Sale)

    @Query("DELETE FROM sales WHERE id = :id")
    suspend fun deleteSaleById(id: Long)

    @Query("SELECT * FROM sales WHERE firestoreId = :firestoreId LIMIT 1")
    suspend fun getSaleByFirestoreId(firestoreId: String): Sale?

    @Query("DELETE FROM sales WHERE firestoreId = :firestoreId")
    suspend fun deleteSaleByFirestoreId(firestoreId: String)
}

