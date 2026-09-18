package com.example.ui.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategories
import com.example.data.model.PaymentStatus
import com.example.data.model.PaymentTypes
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

data class ExpensesUiState(
    val expenses: List<Expense> = emptyList(),
    val filter: DateFilterOption = DateFilterOption.TODAY,
    val totalAmount: Double = 0.0,
    val cashAmount: Double = 0.0,
    val creditAmount: Double = 0.0,
    val pendingCreditAmount: Double = 0.0,
    val pendingCount: Int = 0,
    val totalCount: Int = 0,
    val isLoading: Boolean = false
)

data class ExpenseFormState(
    val id: Long = 0L,
    val shopName: String = "",
    val category: String = ExpenseCategories.ALL.first(),
    val description: String = "",
    val amount: String = "",
    val paymentType: String = PaymentTypes.CASH,
    val paymentStatus: String = PaymentStatus.PAID,
    val paidDate: Long? = System.currentTimeMillis(),
    val billImageUri: String? = null,
    val purchaseDate: Long = System.currentTimeMillis(),
    val enteredBy: String = "Owner",
    val isSaving: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
) {
    val isValid: Boolean
        get() = shopName.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0.0
}

class ExpensesViewModel(
    private val repository: ChaatLedgerRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(DateFilterOption.TODAY)
    val filter = _filter.asStateFlow()

    private val _formState = MutableStateFlow(ExpenseFormState())
    val formState = _formState.asStateFlow()

    val uiState: StateFlow<ExpensesUiState> = combine(
        repository.getAllExpenses(),
        _filter
    ) { allExpenses, currentFilter ->
        val filteredExpenses = when (currentFilter) {
            DateFilterOption.TODAY -> {
                val start = FormatUtils.getTodayStart()
                val end = FormatUtils.getTodayEnd()
                allExpenses.filter { it.purchaseDate in start..end || it.timestamp in start..end }
            }
            DateFilterOption.THIS_WEEK -> {
                val start = FormatUtils.getWeekStart()
                allExpenses.filter { it.purchaseDate >= start || it.timestamp >= start }
            }
            DateFilterOption.THIS_MONTH -> {
                val start = FormatUtils.getMonthStart()
                allExpenses.filter { it.purchaseDate >= start || it.timestamp >= start }
            }
            DateFilterOption.ALL -> allExpenses
        }.sortedWith(compareByDescending<Expense> { it.purchaseDate }.thenByDescending { it.timestamp })

        val totalSum = filteredExpenses.sumOf { it.amount }
        val cashSum = filteredExpenses.filter { it.paymentType == PaymentTypes.CASH }.sumOf { it.amount }
        val creditSum = filteredExpenses.filter { it.paymentType == PaymentTypes.CREDIT }.sumOf { it.amount }
        val pendingExpenses = filteredExpenses.filter { it.paymentStatus == PaymentStatus.PENDING }
        val pendingSum = pendingExpenses.sumOf { it.amount }

        ExpensesUiState(
            expenses = filteredExpenses,
            filter = currentFilter,
            totalAmount = totalSum,
            cashAmount = cashSum,
            creditAmount = creditSum,
            pendingCreditAmount = pendingSum,
            pendingCount = pendingExpenses.size,
            totalCount = filteredExpenses.size,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ExpensesUiState(isLoading = true)
    )

    fun setFilter(filter: DateFilterOption) {
        _filter.value = filter
    }

    fun initNewExpense() {
        val lastEnteredBy = _formState.value.enteredBy.ifBlank { "Owner" }
        val now = System.currentTimeMillis()
        _formState.value = ExpenseFormState(
            id = 0L,
            shopName = "",
            category = ExpenseCategories.ALL.first(),
            description = "",
            amount = "",
            paymentType = PaymentTypes.CASH,
            paymentStatus = PaymentStatus.PAID,
            paidDate = now,
            billImageUri = null,
            purchaseDate = now,
            enteredBy = lastEnteredBy
        )
    }

    fun loadExpenseForEdit(expenseId: Long) {
        viewModelScope.launch {
            val expense = repository.getExpenseById(expenseId)
            if (expense != null) {
                _formState.value = ExpenseFormState(
                    id = expense.id,
                    shopName = expense.shopName,
                    category = expense.category,
                    description = expense.description,
                    amount = if (expense.amount % 1.0 == 0.0) expense.amount.toLong().toString() else expense.amount.toString(),
                    paymentType = expense.paymentType,
                    paymentStatus = expense.paymentStatus,
                    paidDate = expense.paidDate,
                    billImageUri = expense.billImageUri,
                    purchaseDate = expense.purchaseDate,
                    enteredBy = expense.enteredBy
                )
            }
        }
    }

    fun updateShopName(name: String) {
        _formState.update { it.copy(shopName = name, errorMessage = null) }
    }

    fun updateCategory(category: String) {
        _formState.update { it.copy(category = category, errorMessage = null) }
    }

    fun updateDescription(desc: String) {
        _formState.update { it.copy(description = desc) }
    }

    fun updateAmount(amount: String) {
        val filtered = amount.filter { it.isDigit() || it == '.' }
        _formState.update { it.copy(amount = filtered, errorMessage = null) }
    }

    /**
     * Logic: when paymentType is "Cash", paymentStatus should default to "Paid" and paidDate can be set
     * equal to purchaseDate automatically. When paymentType is "Credit", paymentStatus should default to
     * "Pending" with paidDate left empty.
     */
    fun updatePaymentType(type: String) {
        _formState.update { current ->
            if (type == PaymentTypes.CASH) {
                current.copy(
                    paymentType = type,
                    paymentStatus = PaymentStatus.PAID,
                    paidDate = current.purchaseDate
                )
            } else {
                current.copy(
                    paymentType = type,
                    paymentStatus = PaymentStatus.PENDING,
                    paidDate = null
                )
            }
        }
    }

    fun updatePaymentStatus(status: String) {
        _formState.update { current ->
            val newPaidDate = if (status == PaymentStatus.PAID) {
                current.paidDate ?: System.currentTimeMillis()
            } else {
                null
            }
            current.copy(paymentStatus = status, paidDate = newPaidDate)
        }
    }

    fun updatePaidDate(dateMillis: Long?) {
        _formState.update { it.copy(paidDate = dateMillis) }
    }

    fun updatePurchaseDate(dateMillis: Long) {
        _formState.update { current ->
            val updatedPaidDate = if (current.paymentType == PaymentTypes.CASH) dateMillis else current.paidDate
            current.copy(purchaseDate = dateMillis, paidDate = updatedPaidDate)
        }
    }

    fun updateBillImageUri(uri: String?) {
        _formState.update { it.copy(billImageUri = uri) }
    }

    fun updateEnteredBy(enteredBy: String) {
        _formState.update { it.copy(enteredBy = enteredBy) }
    }

    fun markExpenseAsPaid(expenseId: Long, paidDateMillis: Long) {
        viewModelScope.launch {
            repository.updateExpensePaymentStatus(expenseId, PaymentStatus.PAID, paidDateMillis)
        }
    }

    fun saveExpense(onSaved: () -> Unit) {
        val state = _formState.value
        if (!state.isValid) {
            _formState.update { it.copy(errorMessage = "Please enter shop name and a valid amount") }
            return
        }

        val amt = state.amount.toDoubleOrNull() ?: 0.0

        viewModelScope.launch {
            _formState.update { it.copy(isSaving = true) }
            try {
                if (state.id == 0L) {
                    val newExpense = Expense(
                        shopName = state.shopName.trim(),
                        category = state.category,
                        description = state.description.trim(),
                        amount = amt,
                        paymentType = state.paymentType,
                        paymentStatus = state.paymentStatus,
                        paidDate = state.paidDate,
                        billImageUri = state.billImageUri,
                        purchaseDate = state.purchaseDate,
                        timestamp = System.currentTimeMillis(),
                        enteredBy = state.enteredBy.trim()
                    )
                    repository.insertExpense(newExpense)
                } else {
                    val existing = repository.getExpenseById(state.id)
                    val updatedExpense = Expense(
                        id = state.id,
                        shopName = state.shopName.trim(),
                        category = state.category,
                        description = state.description.trim(),
                        amount = amt,
                        paymentType = state.paymentType,
                        paymentStatus = state.paymentStatus,
                        paidDate = state.paidDate,
                        billImageUri = state.billImageUri,
                        purchaseDate = state.purchaseDate,
                        timestamp = existing?.timestamp ?: System.currentTimeMillis(),
                        enteredBy = state.enteredBy.trim()
                    )
                    repository.updateExpense(updatedExpense)
                }
                _formState.update { it.copy(isSaving = false, isSuccess = true) }
                onSaved()
            } catch (e: Exception) {
                _formState.update { it.copy(isSaving = false, errorMessage = e.localizedMessage ?: "Failed to save expense") }
            }
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }

    fun deleteExpenseById(id: Long) {
        viewModelScope.launch {
            repository.deleteExpenseById(id)
        }
    }
}
