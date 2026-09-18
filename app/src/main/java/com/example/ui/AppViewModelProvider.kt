package com.example.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.ChaatLedgerApplication
import com.example.ui.dashboard.DashboardViewModel
import com.example.ui.expenses.ExpensesViewModel
import com.example.ui.reports.ReportsViewModel
import com.example.ui.sales.SalesViewModel

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            DashboardViewModel(chaatLedgerApplication().container.repository)
        }
        initializer {
            SalesViewModel(chaatLedgerApplication().container.repository)
        }
        initializer {
            ExpensesViewModel(chaatLedgerApplication().container.repository)
        }
        initializer {
            ReportsViewModel(chaatLedgerApplication().container.repository)
        }
    }
}

fun CreationExtras.chaatLedgerApplication(): ChaatLedgerApplication =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ChaatLedgerApplication)
