package com.example.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Expense
import com.example.data.model.Sale
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.data.repository.ChaatLedgerRepository
import com.example.data.sync.SyncStatus
import com.example.util.FormatUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val todaySalesTotal: Double = 0.0,
    val todayCashTotal: Double = 0.0,
    val todayPaytmTotal: Double = 0.0,
    val hasTodaySalesEntry: Boolean = false,
    val todaySalesEntryId: Long? = null,
    val todayExpenseTotal: Double = 0.0,
    val todayExpenseCount: Int = 0,
    val recentTransactions: List<TransactionItem> = emptyList(),
    val isLoading: Boolean = false
)

class DashboardViewModel(
    private val repository: ChaatLedgerRepository
) : ViewModel() {

    private val todayStart = FormatUtils.getTodayStart()
    private val todayEnd = FormatUtils.getTodayEnd()

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.getAllSales(),
        repository.getAllExpenses()
    ) { allSales, allExpenses ->
        // Pull today's sales from today's daily sales entry
        val todaySale = allSales.firstOrNull { sale ->
            FormatUtils.normalizeToStartOfDay(sale.date) == todayStart
        }
        val salesTotal = todaySale?.totalAmount ?: 0.0
        val cashTotal = todaySale?.cashTotal ?: 0.0
        val paytmTotal = todaySale?.paytmTotal ?: 0.0
        val hasEntry = todaySale != null

        val todayExpenses = allExpenses.filter {
            it.purchaseDate in todayStart..todayEnd || it.timestamp in todayStart..todayEnd
        }
        val expensesTotal = todayExpenses.sumOf { it.amount }

        // Combine into unified transaction items and take last 5
        val saleTransactions = allSales.map { sale ->
            TransactionItem(
                id = sale.id,
                type = TransactionType.SALE,
                title = "Daily Sales • ${FormatUtils.formatBusinessDay(sale.date)}",
                subtitle = "Cash: ${FormatUtils.formatCurrency(sale.cashTotal)} • Paytm: ${FormatUtils.formatCurrency(sale.paytmTotal)}",
                category = "Daily Sales",
                amount = sale.totalAmount,
                timestamp = sale.timestamp,
                badgeInfo = "Cash + Paytm",
                enteredBy = sale.enteredBy
            )
        }

        val expenseTransactions = allExpenses.map { expense ->
            TransactionItem(
                id = expense.id,
                type = TransactionType.EXPENSE,
                title = expense.shopName.ifBlank { expense.category },
                subtitle = "${expense.category}${if (expense.description.isNotBlank()) " • ${expense.description}" else ""}",
                category = expense.category,
                amount = expense.amount,
                timestamp = expense.timestamp,
                badgeInfo = expense.paymentType,
                enteredBy = expense.enteredBy,
                paymentStatus = expense.paymentStatus,
                billImageUri = expense.billImageUri
            )
        }

        val mixedRecent = (saleTransactions + expenseTransactions)
            .sortedByDescending { it.timestamp }
            .take(5)

        DashboardUiState(
            todaySalesTotal = salesTotal,
            todayCashTotal = cashTotal,
            todayPaytmTotal = paytmTotal,
            hasTodaySalesEntry = hasEntry,
            todaySalesEntryId = todaySale?.id,
            todayExpenseTotal = expensesTotal,
            todayExpenseCount = todayExpenses.size,
            recentTransactions = mixedRecent,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(isLoading = true)
    )

    fun deleteTransaction(item: TransactionItem) {
        viewModelScope.launch {
            if (item.type == TransactionType.SALE) {
                repository.deleteSaleById(item.id)
            } else {
                repository.deleteExpenseById(item.id)
            }
        }
    }

    val syncStatus: StateFlow<SyncStatus> = repository.syncStatus

    fun triggerSync() {
        viewModelScope.launch {
            repository.syncPendingNow()
        }
    }
}
