package com.example.ui.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Sale
import com.example.data.repository.ChaatLedgerRepository
import com.example.util.DateFilterOption
import com.example.util.FormatUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SalesUiState(
    val sales: List<Sale> = emptyList(),
    val filter: DateFilterOption = DateFilterOption.TODAY,
    val totalCash: Double = 0.0,
    val totalPaytm: Double = 0.0,
    val totalAmount: Double = 0.0,
    val daysCount: Int = 0,
    val isLoading: Boolean = false
)

data class SaleFormState(
    val id: Long = 0L,
    val selectedDate: Long = FormatUtils.getTodayStart(),
    val cashInput: String = "",
    val paytmInput: String = "",
    val enteredBy: String = "Owner",
    val isExistingEntry: Boolean = false,
    val isSaving: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
) {
    val cashTotal: Double
        get() = cashInput.toDoubleOrNull() ?: 0.0

    val paytmTotal: Double
        get() = paytmInput.toDoubleOrNull() ?: 0.0

    val totalAmount: Double
        get() = cashTotal + paytmTotal

    val isValid: Boolean
        get() {
            val c = cashInput.toDoubleOrNull()
            val p = paytmInput.toDoubleOrNull()
            val nonNegative = (c == null || c >= 0.0) && (p == null || p >= 0.0)
            val hasAtLeastOne = (c != null && c > 0.0) || (p != null && p > 0.0)
            return nonNegative && hasAtLeastOne
        }
}

class SalesViewModel(
    private val repository: ChaatLedgerRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(DateFilterOption.TODAY)
    val filter = _filter.asStateFlow()

    private val _formState = MutableStateFlow(SaleFormState())
    val formState = _formState.asStateFlow()

    val uiState: StateFlow<SalesUiState> = combine(
        repository.getAllSales(),
        _filter
    ) { allSales, currentFilter ->
        val filteredSales = when (currentFilter) {
            DateFilterOption.TODAY -> {
                val start = FormatUtils.getTodayStart()
                val end = FormatUtils.getTodayEnd()
                allSales.filter { it.date in start..end }
            }
            DateFilterOption.THIS_WEEK -> {
                val start = FormatUtils.getWeekStart()
                allSales.filter { it.date >= start }
            }
            DateFilterOption.THIS_MONTH -> {
                val start = FormatUtils.getMonthStart()
                allSales.filter { it.date >= start }
            }
            DateFilterOption.ALL -> allSales
        }.sortedWith(compareByDescending<Sale> { it.date }.thenByDescending { it.timestamp })

        val totalCash = filteredSales.sumOf { it.cashTotal }
        val totalPaytm = filteredSales.sumOf { it.paytmTotal }
        val totalAmount = filteredSales.sumOf { it.totalAmount }

        SalesUiState(
            sales = filteredSales,
            filter = currentFilter,
            totalCash = totalCash,
            totalPaytm = totalPaytm,
            totalAmount = totalAmount,
            daysCount = filteredSales.size,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SalesUiState(isLoading = true)
    )

    fun setFilter(filter: DateFilterOption) {
        _filter.value = filter
    }

    fun initDailyEntry(defaultDateMillis: Long? = null) {
        val targetDate = FormatUtils.normalizeToStartOfDay(defaultDateMillis ?: FormatUtils.getTodayStart())
        val currentEnteredBy = _formState.value.enteredBy.ifBlank { "Owner" }

        viewModelScope.launch {
            val existing = repository.getSaleByDate(targetDate)
            if (existing != null) {
                _formState.value = SaleFormState(
                    id = existing.id,
                    selectedDate = existing.date,
                    cashInput = formatNumberForInput(existing.cashTotal),
                    paytmInput = formatNumberForInput(existing.paytmTotal),
                    enteredBy = existing.enteredBy.ifBlank { currentEnteredBy },
                    isExistingEntry = true
                )
            } else {
                _formState.value = SaleFormState(
                    id = 0L,
                    selectedDate = targetDate,
                    cashInput = "",
                    paytmInput = "",
                    enteredBy = currentEnteredBy,
                    isExistingEntry = false
                )
            }
        }
    }

    fun loadSaleForEdit(saleId: Long) {
        viewModelScope.launch {
            val sale = repository.getSaleById(saleId)
            if (sale != null) {
                _formState.value = SaleFormState(
                    id = sale.id,
                    selectedDate = sale.date,
                    cashInput = formatNumberForInput(sale.cashTotal),
                    paytmInput = formatNumberForInput(sale.paytmTotal),
                    enteredBy = sale.enteredBy,
                    isExistingEntry = true
                )
            }
        }
    }

    fun selectDate(newDateMillis: Long) {
        val normalized = FormatUtils.normalizeToStartOfDay(newDateMillis)
        val currentEnteredBy = _formState.value.enteredBy.ifBlank { "Owner" }

        viewModelScope.launch {
            val existing = repository.getSaleByDate(normalized)
            if (existing != null) {
                _formState.value = SaleFormState(
                    id = existing.id,
                    selectedDate = existing.date,
                    cashInput = formatNumberForInput(existing.cashTotal),
                    paytmInput = formatNumberForInput(existing.paytmTotal),
                    enteredBy = existing.enteredBy.ifBlank { currentEnteredBy },
                    isExistingEntry = true
                )
            } else {
                _formState.update {
                    it.copy(
                        id = 0L,
                        selectedDate = normalized,
                        cashInput = "",
                        paytmInput = "",
                        isExistingEntry = false,
                        errorMessage = null
                    )
                }
            }
        }
    }

    fun updateCashInput(input: String) {
        val filtered = input.filter { it.isDigit() || it == '.' }
        _formState.update { it.copy(cashInput = filtered, errorMessage = null) }
    }

    fun updatePaytmInput(input: String) {
        val filtered = input.filter { it.isDigit() || it == '.' }
        _formState.update { it.copy(paytmInput = filtered, errorMessage = null) }
    }

    fun updateEnteredBy(enteredBy: String) {
        _formState.update { it.copy(enteredBy = enteredBy) }
    }

    fun saveDailySale(onSaved: () -> Unit) {
        val state = _formState.value
        if (!state.isValid) {
            _formState.update {
                it.copy(errorMessage = "Please enter a valid Cash or Paytm sales amount (greater than 0)")
            }
            return
        }

        val cash = state.cashTotal
        val paytm = state.paytmTotal
        val total = cash + paytm
        val normalizedDate = FormatUtils.normalizeToStartOfDay(state.selectedDate)

        viewModelScope.launch {
            _formState.update { it.copy(isSaving = true) }
            try {
                // Double check if record exists for this date to prevent duplicate
                val existing = repository.getSaleByDate(normalizedDate)
                val targetId = if (state.id != 0L) state.id else (existing?.id ?: 0L)

                val sale = Sale(
                    id = targetId,
                    date = normalizedDate,
                    cashTotal = cash,
                    paytmTotal = paytm,
                    totalAmount = total,
                    timestamp = System.currentTimeMillis(),
                    enteredBy = state.enteredBy.trim()
                )

                if (targetId == 0L) {
                    repository.insertSale(sale)
                } else {
                    repository.updateSale(sale)
                }

                _formState.update { it.copy(isSaving = false, isSuccess = true) }
                onSaved()
            } catch (e: Exception) {
                _formState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = e.localizedMessage ?: "Failed to save daily sales entry"
                    )
                }
            }
        }
    }

    fun deleteSale(sale: Sale) {
        viewModelScope.launch {
            repository.deleteSale(sale)
        }
    }

    fun deleteSaleById(id: Long) {
        viewModelScope.launch {
            repository.deleteSaleById(id)
        }
    }

    private fun formatNumberForInput(number: Double): String {
        return if (number == 0.0) {
            ""
        } else if (number % 1.0 == 0.0) {
            number.toLong().toString()
        } else {
            number.toString()
        }
    }
}
