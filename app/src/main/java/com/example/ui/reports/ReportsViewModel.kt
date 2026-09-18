package com.example.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.ChaatLedgerRepository
import com.example.util.FormatUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

enum class ReportPeriod(val label: String) {
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    LAST_30_DAYS("Last 30 Days"),
    CUSTOM_RANGE("Custom Range")
}

data class DailyReportItem(
    val dateMillis: Long,
    val dayOfWeek: String,
    val dayMonthLabel: String,
    val salesAmount: Double,
    val expenseAmount: Double,
    val netProfit: Double
)

data class ReportsUiState(
    val selectedPeriod: ReportPeriod = ReportPeriod.THIS_WEEK,
    val startDateMillis: Long = 0L,
    val endDateMillis: Long = 0L,
    val dateRangeLabel: String = "",
    val totalSales: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val netProfit: Double = 0.0,
    val dailyItems: List<DailyReportItem> = emptyList(),
    val maxDailyAmount: Double = 1.0,
    val selectedDayItem: DailyReportItem? = null,
    val isCustomRangePickerOpen: Boolean = false,
    val isLoading: Boolean = false
)

private data class PeriodFilterState(
    val period: ReportPeriod = ReportPeriod.THIS_WEEK,
    val customStart: Long? = null,
    val customEnd: Long? = null,
    val selectedDay: DailyReportItem? = null,
    val isPickerOpen: Boolean = false
)

class ReportsViewModel(
    private val repository: ChaatLedgerRepository
) : ViewModel() {

    private val filterState = MutableStateFlow(PeriodFilterState())

    val uiState: StateFlow<ReportsUiState> = combine(
        repository.getAllSales(),
        repository.getAllExpenses(),
        filterState
    ) { allSales, allExpenses, filter ->
        val (startTime, endTime, rangeLabel) = computeRange(filter)

        // Aggregate day by day
        val items = mutableListOf<DailyReportItem>()
        val cal = Calendar.getInstance().apply {
            timeInMillis = startTime
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        while (cal.timeInMillis <= endTime) {
            val dayStart = cal.timeInMillis

            val daySale = allSales.firstOrNull { sale ->
                FormatUtils.normalizeToStartOfDay(sale.date) == dayStart
            }
            val daySalesAmount = daySale?.totalAmount ?: 0.0

            val dayExpensesAmount = allExpenses.filter { expense ->
                FormatUtils.normalizeToStartOfDay(expense.purchaseDate) == dayStart
            }.sumOf { it.amount }

            items.add(
                DailyReportItem(
                    dateMillis = dayStart,
                    dayOfWeek = FormatUtils.formatDayOfWeek(dayStart),
                    dayMonthLabel = FormatUtils.formatDayAndMonth(dayStart),
                    salesAmount = daySalesAmount,
                    expenseAmount = dayExpensesAmount,
                    netProfit = daySalesAmount - dayExpensesAmount
                )
            )

            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        val totalSales = items.sumOf { it.salesAmount }
        val totalExpenses = items.sumOf { it.expenseAmount }
        val netProfit = totalSales - totalExpenses
        val maxAmount = items.maxOfOrNull { maxOf(it.salesAmount, it.expenseAmount) }?.coerceAtLeast(100.0) ?: 100.0

        // Keep or update selected day item
        val updatedSelectedDay = if (filter.selectedDay != null) {
            items.firstOrNull { it.dateMillis == filter.selectedDay.dateMillis }
        } else null

        ReportsUiState(
            selectedPeriod = filter.period,
            startDateMillis = startTime,
            endDateMillis = endTime,
            dateRangeLabel = rangeLabel,
            totalSales = totalSales,
            totalExpenses = totalExpenses,
            netProfit = netProfit,
            dailyItems = items,
            maxDailyAmount = maxAmount,
            selectedDayItem = updatedSelectedDay,
            isCustomRangePickerOpen = filter.isPickerOpen,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReportsUiState(isLoading = true)
    )

    private fun computeRange(filter: PeriodFilterState): Triple<Long, Long, String> {
        return when (filter.period) {
            ReportPeriod.THIS_WEEK -> {
                val start = FormatUtils.getWeekStart()
                val end = FormatUtils.getWeekEnd()
                Triple(start, end, "${FormatUtils.formatDayAndMonth(start)} – ${FormatUtils.formatDayAndMonth(end)}")
            }
            ReportPeriod.THIS_MONTH -> {
                val start = FormatUtils.getMonthStart()
                val end = FormatUtils.getMonthEnd()
                Triple(start, end, "${FormatUtils.formatDayAndMonth(start)} – ${FormatUtils.formatDayAndMonth(end)}")
            }
            ReportPeriod.LAST_30_DAYS -> {
                val start = FormatUtils.getLast30DaysStart()
                val end = FormatUtils.getTodayEnd()
                Triple(start, end, "${FormatUtils.formatDayAndMonth(start)} – ${FormatUtils.formatDayAndMonth(end)}")
            }
            ReportPeriod.CUSTOM_RANGE -> {
                val start = filter.customStart ?: FormatUtils.getLast30DaysStart()
                val end = filter.customEnd ?: FormatUtils.getTodayEnd()
                Triple(start, end, "${FormatUtils.formatDate(start)} – ${FormatUtils.formatDate(end)}")
            }
        }
    }

    fun setPeriod(period: ReportPeriod) {
        if (period == ReportPeriod.CUSTOM_RANGE) {
            // Open custom range picker
            filterState.value = filterState.value.copy(
                period = period,
                isPickerOpen = true,
                selectedDay = null
            )
        } else {
            filterState.value = filterState.value.copy(
                period = period,
                isPickerOpen = false,
                selectedDay = null
            )
        }
    }

    fun setCustomRange(startUtcMillis: Long, endUtcMillis: Long) {
        val startLocal = FormatUtils.utcMillisToLocalStartOfDay(startUtcMillis)
        val endLocal = FormatUtils.utcMillisToLocalEndOfDay(endUtcMillis)
        val actualStart = minOf(startLocal, endLocal)
        val actualEnd = maxOf(startLocal, endLocal)

        filterState.value = filterState.value.copy(
            period = ReportPeriod.CUSTOM_RANGE,
            customStart = actualStart,
            customEnd = actualEnd,
            isPickerOpen = false,
            selectedDay = null
        )
    }

    fun openCustomRangePicker() {
        filterState.value = filterState.value.copy(isPickerOpen = true)
    }

    fun closeCustomRangePicker() {
        filterState.value = filterState.value.copy(isPickerOpen = false)
    }

    fun selectDay(item: DailyReportItem?) {
        filterState.value = filterState.value.copy(selectedDay = item)
    }
}
