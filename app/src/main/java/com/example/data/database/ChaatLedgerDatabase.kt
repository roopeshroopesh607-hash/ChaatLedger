package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.ExpenseDao
import com.example.data.dao.PendingSyncDao
import com.example.data.dao.SaleDao
import com.example.data.model.Expense
import com.example.data.model.PendingSyncAction
import com.example.data.model.Sale

@Database(entities = [Sale::class, Expense::class, PendingSyncAction::class], version = 4, exportSchema = false)
abstract class ChaatLedgerDatabase : RoomDatabase() {
    abstract fun saleDao(): SaleDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun pendingSyncDao(): PendingSyncDao

    companion object {
        @Volatile
        private var Instance: ChaatLedgerDatabase? = null

        fun getDatabase(context: Context): ChaatLedgerDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    ChaatLedgerDatabase::class.java,
                    "chaat_ledger.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                .also { Instance = it }
            }
        }
    }
}
