package com.example.data.repository

import com.example.data.dao.ExpenseDao
import com.example.data.dao.SaleDao
import com.example.data.model.Expense
import com.example.data.model.Sale
import com.example.data.sync.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Clean repository interface abstracting Room data access and cloud sync.
 */
interface ChaatLedgerRepository {
    val syncStatus: StateFlow<SyncStatus>
    suspend fun syncPendingNow()

    fun getAllSales(): Flow<List<Sale>>
    fun getSalesBetween(startTime: Long, endTime: Long): Flow<List<Sale>>
    fun getSaleByIdFlow(id: Long): Flow<Sale?>
    suspend fun getSaleById(id: Long): Sale?
    fun getSaleByDateFlow(date: Long): Flow<Sale?>
    suspend fun getSaleByDate(date: Long): Sale?
    suspend fun findSaleForDay(startOfDay: Long, endOfDay: Long): Sale?
    suspend fun insertSale(sale: Sale): Long
    suspend fun updateSale(sale: Sale)
    suspend fun deleteSale(sale: Sale)
    suspend fun deleteSaleById(id: Long)

    fun getAllExpenses(): Flow<List<Expense>>
    fun getExpensesBetween(startTime: Long, endTime: Long): Flow<List<Expense>>
    fun getExpenseByIdFlow(id: Long): Flow<Expense?>
    suspend fun getExpenseById(id: Long): Expense?
    suspend fun insertExpense(expense: Expense): Long
    suspend fun updateExpense(expense: Expense)
    suspend fun updateExpensePaymentStatus(id: Long, status: String, paidDate: Long?)
    suspend fun deleteExpense(expense: Expense)
    suspend fun deleteExpenseById(id: Long)
}

class OfflineChaatLedgerRepository(
    private val saleDao: SaleDao,
    private val expenseDao: ExpenseDao
) : ChaatLedgerRepository {

    override val syncStatus: StateFlow<SyncStatus> =
        MutableStateFlow(SyncStatus.OnlineSynced)

    override suspend fun syncPendingNow() {
        // No-op for offline repository
    }

    override fun getAllSales(): Flow<List<Sale>> = saleDao.getAllSales()

    override fun getSalesBetween(startTime: Long, endTime: Long): Flow<List<Sale>> =
        saleDao.getSalesBetween(startTime, endTime)

    override fun getSaleByIdFlow(id: Long): Flow<Sale?> = saleDao.getSaleByIdFlow(id)

    override suspend fun getSaleById(id: Long): Sale? = saleDao.getSaleById(id)

    override fun getSaleByDateFlow(date: Long): Flow<Sale?> = saleDao.getSaleByDateFlow(date)

    override suspend fun getSaleByDate(date: Long): Sale? = saleDao.getSaleByDate(date)

    override suspend fun findSaleForDay(startOfDay: Long, endOfDay: Long): Sale? =
        saleDao.findSaleForDay(startOfDay, endOfDay)

    override suspend fun insertSale(sale: Sale): Long = saleDao.insertSale(sale)

    override suspend fun updateSale(sale: Sale) = saleDao.updateSale(sale)

    override suspend fun deleteSale(sale: Sale) = saleDao.deleteSale(sale)

    override suspend fun deleteSaleById(id: Long) = saleDao.deleteSaleById(id)

    override fun getAllExpenses(): Flow<List<Expense>> = expenseDao.getAllExpenses()

    override fun getExpensesBetween(startTime: Long, endTime: Long): Flow<List<Expense>> =
        expenseDao.getExpensesBetween(startTime, endTime)

    override fun getExpenseByIdFlow(id: Long): Flow<Expense?> = expenseDao.getExpenseByIdFlow(id)

    override suspend fun getExpenseById(id: Long): Expense? = expenseDao.getExpenseById(id)

    override suspend fun insertExpense(expense: Expense): Long = expenseDao.insertExpense(expense)

    override suspend fun updateExpense(expense: Expense) = expenseDao.updateExpense(expense)

    override suspend fun updateExpensePaymentStatus(id: Long, status: String, paidDate: Long?) =
        expenseDao.updatePaymentStatus(id, status, paidDate)

    override suspend fun deleteExpense(expense: Expense) = expenseDao.deleteExpense(expense)

    override suspend fun deleteExpenseById(id: Long) = expenseDao.deleteExpenseById(id)
}
