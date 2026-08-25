package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.AlertHistoryEntity
import com.example.data.local.entities.AlertRuleEntity
import com.example.data.local.entities.StockEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockDao {
    @Query("SELECT * FROM stocks ORDER BY tradingValue DESC")
    fun getAllStocks(): Flow<List<StockEntity>>

    @Query("SELECT * FROM stocks WHERE symbol = :symbol")
    suspend fun getStockBySymbol(symbol: String): StockEntity?

    @Query("SELECT * FROM stocks WHERE isFavorite = 1")
    fun getFavoriteStocks(): Flow<List<StockEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStocks(stocks: List<StockEntity>)

    @Update
    suspend fun updateStock(stock: StockEntity)

    @Query("UPDATE stocks SET isFavorite = :isFavorite WHERE symbol = :symbol")
    suspend fun toggleFavorite(symbol: String, isFavorite: Boolean)
}

@Dao
interface AlertRuleDao {
    @Query("SELECT * FROM alert_rules ORDER BY createdAt DESC")
    fun getAllRules(): Flow<List<AlertRuleEntity>>

    @Query("SELECT * FROM alert_rules WHERE isEnabled = 1")
    suspend fun getActiveRules(): List<AlertRuleEntity>

    @Query("SELECT * FROM alert_rules WHERE id = :id")
    suspend fun getRuleById(id: Long): AlertRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: AlertRuleEntity): Long

    @Update
    suspend fun updateRule(rule: AlertRuleEntity)

    @Query("UPDATE alert_rules SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun setRuleEnabled(id: Long, isEnabled: Boolean)

    @Query("UPDATE alert_rules SET lastTriggeredAt = :timestamp WHERE id = :id")
    suspend fun updateLastTriggered(id: Long, timestamp: Long)

    @Delete
    suspend fun deleteRule(rule: AlertRuleEntity)

    @Query("DELETE FROM alert_rules WHERE id = :id")
    suspend fun deleteRuleById(id: Long)
}

@Dao
interface AlertHistoryDao {
    @Query("SELECT * FROM alert_histories ORDER BY timestamp DESC")
    fun getAllHistories(): Flow<List<AlertHistoryEntity>>

    @Query("SELECT COUNT(*) FROM alert_histories WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: AlertHistoryEntity): Long

    @Query("UPDATE alert_histories SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    @Query("UPDATE alert_histories SET isRead = 1")
    suspend fun markAllAsRead()

    @Query("DELETE FROM alert_histories WHERE id = :id")
    suspend fun deleteHistory(id: Long)

    @Query("DELETE FROM alert_histories")
    suspend fun clearAll()
}
