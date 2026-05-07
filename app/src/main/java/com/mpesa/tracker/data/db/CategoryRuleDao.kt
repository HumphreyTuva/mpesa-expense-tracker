package com.mpesa.tracker.data.db

import androidx.room.*
import com.mpesa.tracker.data.model.CategoryRule

@Dao
interface CategoryRuleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: CategoryRule)

    @Query("SELECT category FROM category_rules WHERE identifier = :identifier LIMIT 1")
    suspend fun getCategoryByIdentifier(identifier: String): String?

    @Query("DELETE FROM category_rules")
    suspend fun clearAllRules()
}
