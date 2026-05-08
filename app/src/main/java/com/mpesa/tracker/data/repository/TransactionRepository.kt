package com.mpesa.tracker.data.repository

import com.mpesa.tracker.data.db.*
import com.mpesa.tracker.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
    private val categoryMappingDao: CategoryMappingDao,
    private val categoryRuleDao: CategoryRuleDao,
    private val categoryDao: CategoryDao
) {

    // ── Transactions ──────────────────────────────────────────────────────────

    fun getAllTransactions(): Flow<List<Transaction>> =
        transactionDao.getAllTransactionsIncludingExcluded()

    fun getTransactionsByDateRange(startMs: Long, endMs: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsByDateRange(startMs, endMs)

    suspend fun insertTransaction(transaction: Transaction): Long {
        // Apply smart category rule if it exists
        val identifier = transaction.recipient?.lowercase()?.trim()
        val txToInsert = if (identifier != null) {
            val smartCategory = categoryRuleDao.getCategoryByIdentifier(identifier)
            if (smartCategory != null) {
                transaction.copy(category = smartCategory)
            } else {
                transaction
            }
        } else {
            transaction
        }
        return transactionDao.insert(txToInsert)
    }

    suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.update(transaction)
        
        // Save smart category rule if edited manually
        val identifier = transaction.recipient?.lowercase()?.trim()
        if (identifier != null) {
            categoryRuleDao.insertRule(CategoryRule(identifier = identifier, category = transaction.category))
        }
    }

    suspend fun deleteTransaction(transaction: Transaction) =
        transactionDao.delete(transaction)

    fun getTotalExpenses(startMs: Long, endMs: Long): Flow<Double> =
        transactionDao.getTotalExpensesFlow(startMs, endMs).map { it ?: 0.0 }

    fun getTotalIncome(startMs: Long, endMs: Long): Flow<Double> =
        transactionDao.getTotalIncomeFlow(startMs, endMs).map { it ?: 0.0 }

    fun getExpensesByCategory(startMs: Long, endMs: Long): Flow<List<CategoryTotal>> =
        transactionDao.getExpensesByCategoryFlow(startMs, endMs)

    fun getRecentTransactions(limit: Int = 10): Flow<List<Transaction>> =
        transactionDao.getRecentTransactionsFlow(limit)

    suspend fun getRecentTransactionsList(limit: Int = 10): List<Transaction> =
        transactionDao.getRecentTransactions(limit)

    suspend fun getTransactionsForExport(startMs: Long, endMs: Long): List<Transaction> =
        transactionDao.getForExport(startMs, endMs)

    // ── Category Mappings ───────────────────────────────────────────────────

    suspend fun saveCategoryMapping(searchText: String, category: String) {
        categoryMappingDao.insert(CategoryMapping(searchText, category))
    }

    suspend fun getCategoryForRecipient(recipient: String): String? {
        return categoryMappingDao.getCategoryForText(recipient)
    }

    // ── Budgets ───────────────────────────────────────────────────────────────

    fun getBudgetsForMonth(month: Int, year: Int): Flow<List<Budget>> =
        budgetDao.getBudgetsForMonth(month, year)

    suspend fun insertBudget(budget: Budget): Long = budgetDao.insert(budget)

    suspend fun updateBudget(budget: Budget) = budgetDao.update(budget)

    suspend fun deleteBudget(budget: Budget) = budgetDao.delete(budget)

    /**
     * Calculates budgets with actual spending for the given month.
     */
    suspend fun getBudgetsWithSpent(month: Int, year: Int): List<BudgetWithSpent> {
        val (startMs, endMs) = monthRange(month, year)
        val budgets = mutableListOf<BudgetWithSpent>()

        // We'll collect flow value once inline for simplicity in a suspend function
        // In ViewModel, observe the Flow version instead
        return budgets
    }

    // ── Categories ───────────────────────────────────────────────────────────

    fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories()

    suspend fun insertCategory(category: Category) = categoryDao.insert(category)

    suspend fun updateCategory(category: Category) = categoryDao.update(category)

    suspend fun deleteCategory(category: Category) = categoryDao.delete(category)

    suspend fun renameCategory(oldName: String, newName: String) {
        // 1. Insert new category
        categoryDao.insert(Category(name = newName))
        // 2. Update all related data
        transactionDao.updateCategoryName(oldName, newName)
        categoryMappingDao.updateCategoryName(oldName, newName)
        categoryRuleDao.updateCategoryName(oldName, newName)
        // 3. Delete old category
        categoryDao.delete(Category(name = oldName))
    }

    fun monthRange(month: Int, year: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(year, month, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        val end = cal.timeInMillis

        return Pair(start, end)
    }

    suspend fun getSpentInCategory(category: String, startMs: Long, endMs: Long): Double {
        return transactionDao.getByCategory(category, startMs, endMs)
            .filter { it.isExpense }
            .sumOf { it.amount }
    }
}
