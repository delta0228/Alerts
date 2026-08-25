package com.example.data

import com.example.data.local.AlertHistoryDao
import com.example.data.local.AlertRuleDao
import com.example.data.local.StockDao
import com.example.data.local.entities.AlertHistoryEntity
import com.example.data.local.entities.AlertRuleEntity
import com.example.data.local.entities.StockEntity
import com.example.model.AlertHistory
import com.example.model.AlertRule
import com.example.model.ChartTimeframe
import com.example.model.RuleScope
import com.example.model.RuleType
import com.example.model.Stock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StockRepository(
    private val stockDao: StockDao,
    private val alertRuleDao: AlertRuleDao,
    private val alertHistoryDao: AlertHistoryDao
) {
    val allStocks: Flow<List<Stock>> = stockDao.getAllStocks().map { list ->
        list.map { it.toDomain() }
    }

    val favoriteStocks: Flow<List<Stock>> = stockDao.getFavoriteStocks().map { list ->
        list.map { it.toDomain() }
    }

    val allRules: Flow<List<AlertRule>> = alertRuleDao.getAllRules().map { list ->
        list.map { it.toDomain() }
    }

    val allHistories: Flow<List<AlertHistory>> = alertHistoryDao.getAllHistories().map { list ->
        list.map { it.toDomain() }
    }

    val unreadCount: Flow<Int> = alertHistoryDao.getUnreadCount()

    suspend fun insertStocks(stocks: List<Stock>) {
        stockDao.insertStocks(stocks.map { StockEntity.fromDomain(it) })
    }

    suspend fun toggleFavorite(symbol: String, isFavorite: Boolean) {
        stockDao.toggleFavorite(symbol, isFavorite)
    }

    suspend fun insertRule(rule: AlertRule): Long {
        return alertRuleDao.insertRule(rule.toEntity())
    }

    suspend fun setRuleEnabled(id: Long, isEnabled: Boolean) {
        alertRuleDao.setRuleEnabled(id, isEnabled)
    }

    suspend fun deleteRule(id: Long) {
        alertRuleDao.deleteRuleById(id)
    }

    suspend fun updateRuleLastTriggered(id: Long, timestamp: Long) {
        alertRuleDao.updateLastTriggered(id, timestamp)
    }

    suspend fun getActiveRules(): List<AlertRule> {
        return alertRuleDao.getActiveRules().map { it.toDomain() }
    }

    suspend fun insertHistory(history: AlertHistory): Long {
        return alertHistoryDao.insertHistory(history.toEntity())
    }

    suspend fun markHistoryAsRead(id: Long) {
        alertHistoryDao.markAsRead(id)
    }

    suspend fun markAllHistoriesAsRead() {
        alertHistoryDao.markAllAsRead()
    }

    suspend fun clearAllHistories() {
        alertHistoryDao.clearAll()
    }
}

// Mapper extension functions
private fun AlertRuleEntity.toDomain(): AlertRule {
    return AlertRule(
        id = id,
        name = name,
        scope = try { RuleScope.valueOf(scope) } catch (e: Exception) { RuleScope.SPECIFIC },
        targetSymbol = targetSymbol,
        targetSymbolName = targetSymbolName,
        timeframe = try { ChartTimeframe.valueOf(timeframe) } catch (e: Exception) { ChartTimeframe.M5 },
        ruleType = try { RuleType.valueOf(ruleType) } catch (e: Exception) { RuleType.PRICE_ABOVE },
        thresholdValue = thresholdValue,
        param1 = param1,
        param2 = param2,
        cooldownMinutes = cooldownMinutes,
        isEnabled = isEnabled,
        lastTriggeredAt = lastTriggeredAt,
        createdAt = createdAt
    )
}

private fun AlertRule.toEntity(): AlertRuleEntity {
    return AlertRuleEntity(
        id = id,
        name = name,
        scope = scope.name,
        targetSymbol = targetSymbol,
        targetSymbolName = targetSymbolName,
        timeframe = timeframe.name,
        ruleType = ruleType.name,
        thresholdValue = thresholdValue,
        param1 = param1,
        param2 = param2,
        cooldownMinutes = cooldownMinutes,
        isEnabled = isEnabled,
        lastTriggeredAt = lastTriggeredAt,
        createdAt = createdAt
    )
}

private fun AlertHistoryEntity.toDomain(): AlertHistory {
    return AlertHistory(
        id = id,
        ruleId = ruleId,
        ruleName = ruleName,
        ruleType = try { RuleType.valueOf(ruleType) } catch (e: Exception) { RuleType.PRICE_ABOVE },
        symbol = symbol,
        stockName = stockName,
        triggeredPrice = triggeredPrice,
        changeRate = changeRate,
        message = message,
        timestamp = timestamp,
        isRead = isRead
    )
}

private fun AlertHistory.toEntity(): AlertHistoryEntity {
    return AlertHistoryEntity(
        id = id,
        ruleId = ruleId,
        ruleName = ruleName,
        ruleType = ruleType.name,
        symbol = symbol,
        stockName = stockName,
        triggeredPrice = triggeredPrice,
        changeRate = changeRate,
        message = message,
        timestamp = timestamp,
        isRead = isRead
    )
}
