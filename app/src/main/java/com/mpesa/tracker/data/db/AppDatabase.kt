package com.mpesa.tracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mpesa.tracker.data.model.Budget
import com.mpesa.tracker.data.model.Category
import com.mpesa.tracker.data.model.CategoryMapping
import com.mpesa.tracker.data.model.CategoryRule
import com.mpesa.tracker.data.model.Transaction
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Transaction::class, Budget::class, CategoryMapping::class, CategoryRule::class, Category::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun categoryMappingDao(): CategoryMappingDao
    abstract fun categoryRuleDao(): CategoryRuleDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mpesa_tracker.db"
                )
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Prepopulate default categories
                            CoroutineScope(Dispatchers.IO).launch {
                                val dao = getInstance(context).categoryDao()
                                val defaults = listOf(
                                    "Groceries", "Utilities", "Transport", "Food & Dining",
                                    "Airtime", "Entertainment", "Health", "Education",
                                    "Transfer", "Withdrawal", "Income", "Other"
                                )
                                defaults.forEach { dao.insert(Category(it, isSystem = true)) }
                            }
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
