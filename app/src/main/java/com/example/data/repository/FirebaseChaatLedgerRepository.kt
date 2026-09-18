package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.dao.ExpenseDao
import com.example.data.dao.PendingSyncDao
import com.example.data.dao.SaleDao
import com.example.data.model.Expense
import com.example.data.model.PendingSyncAction
import com.example.data.model.Sale
import com.example.data.model.SyncActionType
import com.example.data.model.SyncCollection
import com.example.data.sync.SyncMappers
import com.example.data.sync.SyncStatus
import com.example.util.NetworkMonitor
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirebaseChaatLedgerRepository(
    private val saleDao: SaleDao,
    private val expenseDao: ExpenseDao,
    private val pendingSyncDao: PendingSyncDao,
    private val networkMonitor: NetworkMonitor,
    private val context: Context,
    private val scope: CoroutineScope
) : ChaatLedgerRepository {

    private val tag = "FirebaseRepository"
    private val _isSyncing = MutableStateFlow(false)

    private var salesListenerRegistration: ListenerRegistration? = null
    private var expensesListenerRegistration: ListenerRegistration? = null

    private val isFirebaseAvailable: Boolean
        get() = try {
            FirebaseApp.getApps(context).isNotEmpty()
        } catch (e: Exception) {
            false
        }

    private val firestore: FirebaseFirestore?
        get() = if (isFirebaseAvailable) {
            try {
                FirebaseFirestore.getInstance()
            } catch (e: Exception) {
                null
            }
        } else null

    override val syncStatus: StateFlow<SyncStatus> = combine(
        networkMonitor.isOnline,
        pendingSyncDao.getPendingCountFlow(),
        _isSyncing
    ) { isOnline, pendingCount, isSyncing ->
        when {
            isSyncing -> SyncStatus.Syncing(pendingCount)
            !isOnline && pendingCount > 0 -> SyncStatus.OfflinePending(pendingCount)
            !isOnline -> SyncStatus.OfflineSynced
            else -> SyncStatus.OnlineSynced
        }
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = SyncStatus.OnlineSynced
    )

    init {
        // Start Firestore real-time listeners and offline auto-sync monitor
        startRealtimeSync()

        // Automatically sync pending queue when internet connectivity is restored
        scope.launch {
            networkMonitor.isOnline.collect { online ->
                if (online) {
                    Log.d(tag, "Internet connected - attempting to sync pending queue")
                    syncPendingNow()
                }
            }
        }
    }

    private fun startRealtimeSync() {
        val db = firestore ?: return

        try {
            // Listen to top-level "sales" collection
            salesListenerRegistration = db.collection(SyncCollection.SALES)
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w(tag, "Sales snapshot listener encountered an issue: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshots != null) {
                        scope.launch(Dispatchers.IO) {
                            for (docChange in snapshots.documentChanges) {
                                val doc = docChange.document
                                val docId = doc.id
                                val data = doc.data

                                when (docChange.type) {
                                    DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                        val remoteSale = SyncMappers.saleFromMap(docId, data)
                                        // Find local record matching docId or date
                                        val existing = saleDao.getSaleByFirestoreId(docId)
                                            ?: saleDao.getSaleByDate(remoteSale.date)

                                        val saleToSave = remoteSale.copy(
                                            id = existing?.id ?: 0L,
                                            firestoreId = docId
                                        )
                                        saleDao.insertSale(saleToSave)
                                    }
                                    DocumentChange.Type.REMOVED -> {
                                        saleDao.deleteSaleByFirestoreId(docId)
                                    }
                                }
                            }
                        }
                    }
                }

            // Listen to top-level "expenses" collection
            expensesListenerRegistration = db.collection(SyncCollection.EXPENSES)
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w(tag, "Expenses snapshot listener encountered an issue: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshots != null) {
                        scope.launch(Dispatchers.IO) {
                            for (docChange in snapshots.documentChanges) {
                                val doc = docChange.document
                                val docId = doc.id
                                val data = doc.data

                                when (docChange.type) {
                                    DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                        val remoteExpense = SyncMappers.expenseFromMap(docId, data)
                                        val existing = expenseDao.getExpenseByFirestoreId(docId)

                                        val expenseToSave = remoteExpense.copy(
                                            id = existing?.id ?: 0L,
                                            // Retain local image URI if remote doesn't have it
                                            billImageUri = remoteExpense.billImageUri ?: existing?.billImageUri,
                                            firestoreId = docId
                                        )
                                        expenseDao.insertExpense(expenseToSave)
                                    }
                                    DocumentChange.Type.REMOVED -> {
                                        expenseDao.deleteExpenseByFirestoreId(docId)
                                    }
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(tag, "Failed to start Firestore snapshot listeners: ${e.message}", e)
        }
    }

    override suspend fun syncPendingNow() {
        withContext(Dispatchers.IO) {
            val db = firestore
            if (db == null || !networkMonitor.isCurrentlyOnline()) {
                Log.d(tag, "Cannot sync: Firestore unavailable or currently offline")
                return@withContext
            }

            val actions = pendingSyncDao.getAllPendingActions()
            if (actions.isEmpty()) return@withContext

            _isSyncing.value = true
            try {
                for (action in actions) {
                    try {
                        when (action.collection) {
                            SyncCollection.SALES -> {
                                val salesCol = db.collection(SyncCollection.SALES)
                                if (action.actionType == SyncActionType.UPSERT) {
                                    val sale = SyncMappers.saleFromJson(action.payloadJson)
                                    salesCol.document(action.documentId).set(SyncMappers.saleToMap(sale)).await()
                                } else if (action.actionType == SyncActionType.DELETE) {
                                    salesCol.document(action.documentId).delete().await()
                                }
                            }
                            SyncCollection.EXPENSES -> {
                                val expensesCol = db.collection(SyncCollection.EXPENSES)
                                if (action.actionType == SyncActionType.UPSERT) {
                                    val expense = SyncMappers.expenseFromJson(action.payloadJson)
                                    expensesCol.document(action.documentId).set(SyncMappers.expenseToMap(expense)).await()
                                } else if (action.actionType == SyncActionType.DELETE) {
                                    expensesCol.document(action.documentId).delete().await()
                                }
                            }
                        }
                        // Delete successfully pushed action from local queue
                        pendingSyncDao.deleteActionById(action.id)
                    } catch (itemError: Exception) {
                        Log.w(tag, "Failed to sync pending action ${action.id}: ${itemError.message}")
                        // Stop draining queue on network disruption to preserve ordering
                        break
                    }
                }
            } finally {
                _isSyncing.value = false
            }
        }
    }

    // --- SALES OPERATIONS (Room Cache + Real-time Firestore Sync) ---

    override fun getAllSales(): Flow<List<Sale>> = saleDao.getAllSales()

    override fun getSalesBetween(startTime: Long, endTime: Long): Flow<List<Sale>> =
        saleDao.getSalesBetween(startTime, endTime)

    override fun getSaleByIdFlow(id: Long): Flow<Sale?> = saleDao.getSaleByIdFlow(id)

    override suspend fun getSaleById(id: Long): Sale? = saleDao.getSaleById(id)

    override fun getSaleByDateFlow(date: Long): Flow<Sale?> = saleDao.getSaleByDateFlow(date)

    override suspend fun getSaleByDate(date: Long): Sale? = saleDao.getSaleByDate(date)

    override suspend fun findSaleForDay(startOfDay: Long, endOfDay: Long): Sale? =
        saleDao.findSaleForDay(startOfDay, endOfDay)

    override suspend fun insertSale(sale: Sale): Long {
        val docId = sale.firestoreId.ifEmpty { SyncMappers.generateSaleFirestoreId(sale.date) }
        val saleWithDocId = sale.copy(firestoreId = docId)
        val localId = saleDao.insertSale(saleWithDocId)
        val finalSale = saleWithDocId.copy(id = localId)

        scope.launch(Dispatchers.IO) {
            pushOrQueueSale(finalSale, SyncActionType.UPSERT)
        }
        return localId
    }

    override suspend fun updateSale(sale: Sale) {
        val docId = sale.firestoreId.ifEmpty { SyncMappers.generateSaleFirestoreId(sale.date) }
        val saleWithDocId = sale.copy(firestoreId = docId)
        saleDao.updateSale(saleWithDocId)

        scope.launch(Dispatchers.IO) {
            pushOrQueueSale(saleWithDocId, SyncActionType.UPSERT)
        }
    }

    override suspend fun deleteSale(sale: Sale) {
        saleDao.deleteSale(sale)
        val docId = sale.firestoreId.ifEmpty { SyncMappers.generateSaleFirestoreId(sale.date) }

        scope.launch(Dispatchers.IO) {
            pushOrQueueSaleDelete(docId)
        }
    }

    override suspend fun deleteSaleById(id: Long) {
        val sale = saleDao.getSaleById(id)
        saleDao.deleteSaleById(id)
        if (sale != null) {
            val docId = sale.firestoreId.ifEmpty { SyncMappers.generateSaleFirestoreId(sale.date) }
            scope.launch(Dispatchers.IO) {
                pushOrQueueSaleDelete(docId)
            }
        }
    }

    private suspend fun pushOrQueueSale(sale: Sale, actionType: String) {
        val db = firestore
        val online = networkMonitor.isCurrentlyOnline()

        if (online && db != null) {
            try {
                _isSyncing.value = true
                db.collection(SyncCollection.SALES)
                    .document(sale.firestoreId)
                    .set(SyncMappers.saleToMap(sale))
                    .await()
                // Clear any pending queue entries for this docId
                pendingSyncDao.deleteActionsForDoc(SyncCollection.SALES, sale.firestoreId)
            } catch (e: Exception) {
                Log.w(tag, "Direct push sale failed; queuing locally: ${e.message}")
                queueSaleAction(sale.firestoreId, actionType, sale)
            } finally {
                _isSyncing.value = false
            }
        } else {
            queueSaleAction(sale.firestoreId, actionType, sale)
        }
    }

    private suspend fun pushOrQueueSaleDelete(docId: String) {
        val db = firestore
        val online = networkMonitor.isCurrentlyOnline()

        if (online && db != null) {
            try {
                _isSyncing.value = true
                db.collection(SyncCollection.SALES).document(docId).delete().await()
                pendingSyncDao.deleteActionsForDoc(SyncCollection.SALES, docId)
            } catch (e: Exception) {
                Log.w(tag, "Direct delete sale failed; queuing locally: ${e.message}")
                pendingSyncDao.insertAction(
                    PendingSyncAction(
                        collection = SyncCollection.SALES,
                        actionType = SyncActionType.DELETE,
                        documentId = docId
                    )
                )
            } finally {
                _isSyncing.value = false
            }
        } else {
            pendingSyncDao.insertAction(
                PendingSyncAction(
                    collection = SyncCollection.SALES,
                    actionType = SyncActionType.DELETE,
                    documentId = docId
                )
            )
        }
    }

    private suspend fun queueSaleAction(docId: String, actionType: String, sale: Sale) {
        pendingSyncDao.insertAction(
            PendingSyncAction(
                collection = SyncCollection.SALES,
                actionType = actionType,
                documentId = docId,
                payloadJson = SyncMappers.saleToJson(sale)
            )
        )
    }

    // --- EXPENSES OPERATIONS (Room Cache + Real-time Firestore Sync) ---

    override fun getAllExpenses(): Flow<List<Expense>> = expenseDao.getAllExpenses()

    override fun getExpensesBetween(startTime: Long, endTime: Long): Flow<List<Expense>> =
        expenseDao.getExpensesBetween(startTime, endTime)

    override fun getExpenseByIdFlow(id: Long): Flow<Expense?> = expenseDao.getExpenseByIdFlow(id)

    override suspend fun getExpenseById(id: Long): Expense? = expenseDao.getExpenseById(id)

    override suspend fun insertExpense(expense: Expense): Long {
        val docId = expense.firestoreId.ifEmpty { SyncMappers.generateExpenseFirestoreId() }
        val expenseWithDocId = expense.copy(firestoreId = docId)
        val localId = expenseDao.insertExpense(expenseWithDocId)
        val finalExpense = expenseWithDocId.copy(id = localId)

        scope.launch(Dispatchers.IO) {
            pushOrQueueExpense(finalExpense, SyncActionType.UPSERT)
        }
        return localId
    }

    override suspend fun updateExpense(expense: Expense) {
        val docId = expense.firestoreId.ifEmpty { SyncMappers.generateExpenseFirestoreId() }
        val expenseWithDocId = expense.copy(firestoreId = docId)
        expenseDao.updateExpense(expenseWithDocId)

        scope.launch(Dispatchers.IO) {
            pushOrQueueExpense(expenseWithDocId, SyncActionType.UPSERT)
        }
    }

    override suspend fun updateExpensePaymentStatus(id: Long, status: String, paidDate: Long?) {
        expenseDao.updatePaymentStatus(id, status, paidDate)
        val expense = expenseDao.getExpenseById(id)
        if (expense != null) {
            scope.launch(Dispatchers.IO) {
                pushOrQueueExpense(expense, SyncActionType.UPSERT)
            }
        }
    }

    override suspend fun deleteExpense(expense: Expense) {
        expenseDao.deleteExpense(expense)
        val docId = expense.firestoreId.ifEmpty { SyncMappers.generateExpenseFirestoreId() }

        scope.launch(Dispatchers.IO) {
            pushOrQueueExpenseDelete(docId)
        }
    }

    override suspend fun deleteExpenseById(id: Long) {
        val expense = expenseDao.getExpenseById(id)
        expenseDao.deleteExpenseById(id)
        if (expense != null) {
            val docId = expense.firestoreId.ifEmpty { SyncMappers.generateExpenseFirestoreId() }
            scope.launch(Dispatchers.IO) {
                pushOrQueueExpenseDelete(docId)
            }
        }
    }

    private suspend fun pushOrQueueExpense(expense: Expense, actionType: String) {
        val db = firestore
        val online = networkMonitor.isCurrentlyOnline()

        if (online && db != null) {
            try {
                _isSyncing.value = true
                db.collection(SyncCollection.EXPENSES)
                    .document(expense.firestoreId)
                    .set(SyncMappers.expenseToMap(expense))
                    .await()
                pendingSyncDao.deleteActionsForDoc(SyncCollection.EXPENSES, expense.firestoreId)
            } catch (e: Exception) {
                Log.w(tag, "Direct push expense failed; queuing locally: ${e.message}")
                queueExpenseAction(expense.firestoreId, actionType, expense)
            } finally {
                _isSyncing.value = false
            }
        } else {
            queueExpenseAction(expense.firestoreId, actionType, expense)
        }
    }

    private suspend fun pushOrQueueExpenseDelete(docId: String) {
        val db = firestore
        val online = networkMonitor.isCurrentlyOnline()

        if (online && db != null) {
            try {
                _isSyncing.value = true
                db.collection(SyncCollection.EXPENSES).document(docId).delete().await()
                pendingSyncDao.deleteActionsForDoc(SyncCollection.EXPENSES, docId)
            } catch (e: Exception) {
                Log.w(tag, "Direct delete expense failed; queuing locally: ${e.message}")
                pendingSyncDao.insertAction(
                    PendingSyncAction(
                        collection = SyncCollection.EXPENSES,
                        actionType = SyncActionType.DELETE,
                        documentId = docId
                    )
                )
            } finally {
                _isSyncing.value = false
            }
        } else {
            pendingSyncDao.insertAction(
                PendingSyncAction(
                    collection = SyncCollection.EXPENSES,
                    actionType = SyncActionType.DELETE,
                    documentId = docId
                )
            )
        }
    }

    private suspend fun queueExpenseAction(docId: String, actionType: String, expense: Expense) {
        pendingSyncDao.insertAction(
            PendingSyncAction(
                collection = SyncCollection.EXPENSES,
                actionType = actionType,
                documentId = docId,
                payloadJson = SyncMappers.expenseToJson(expense)
            )
        )
    }
}
