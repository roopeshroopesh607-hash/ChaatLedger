package com.example.di

import android.content.Context
import com.example.data.auth.FirebaseAuthManager
import com.example.data.database.ChaatLedgerDatabase
import com.example.data.repository.ChaatLedgerRepository
import com.example.data.repository.FirebaseChaatLedgerRepository
import com.example.util.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

interface AppContainer {
    val repository: ChaatLedgerRepository
    val authManager: FirebaseAuthManager
    val networkMonitor: NetworkMonitor
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val authManager: FirebaseAuthManager by lazy {
        FirebaseAuthManager(context)
    }

    override val networkMonitor: NetworkMonitor by lazy {
        NetworkMonitor(context)
    }

    override val repository: ChaatLedgerRepository by lazy {
        val database = ChaatLedgerDatabase.getDatabase(context)
        FirebaseChaatLedgerRepository(
            saleDao = database.saleDao(),
            expenseDao = database.expenseDao(),
            pendingSyncDao = database.pendingSyncDao(),
            networkMonitor = networkMonitor,
            context = context,
            scope = applicationScope
        )
    }
}
