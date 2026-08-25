package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.MarketType
import com.example.model.Stock

@Entity(tableName = "stocks")
data class StockEntity(
    @PrimaryKey val symbol: String,
    val name: String,
    val market: String,
    val currentPrice: Double,
    val changePrice: Double,
    val changeRate: Double,
    val openPrice: Double,
    val highPrice: Double,
    val lowPrice: Double,
    val prevClosePrice: Double,
    val volume: Long,
    val tradingValue: Long,
    val isFavorite: Boolean = false
) {
    fun toDomain(): Stock {
        return Stock(
            symbol = symbol,
            name = name,
            market = try { MarketType.valueOf(market) } catch (e: Exception) { MarketType.KOSPI },
            currentPrice = currentPrice,
            changePrice = changePrice,
            changeRate = changeRate,
            openPrice = openPrice,
            highPrice = highPrice,
            lowPrice = lowPrice,
            prevClosePrice = prevClosePrice,
            volume = volume,
            tradingValue = tradingValue,
            isFavorite = isFavorite
        )
    }

    companion object {
        fun fromDomain(stock: Stock): StockEntity {
            return StockEntity(
                symbol = stock.symbol,
                name = stock.name,
                market = stock.market.name,
                currentPrice = stock.currentPrice,
                changePrice = stock.changePrice,
                changeRate = stock.changeRate,
                openPrice = stock.openPrice,
                highPrice = stock.highPrice,
                lowPrice = stock.lowPrice,
                prevClosePrice = stock.prevClosePrice,
                volume = stock.volume,
                tradingValue = stock.tradingValue,
                isFavorite = stock.isFavorite
            )
        }
    }
}

@Entity(tableName = "alert_rules")
data class AlertRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val scope: String,
    val targetSymbol: String,
    val targetSymbolName: String,
    val timeframe: String,
    val ruleType: String,
    val thresholdValue: Double,
    val param1: Int,
    val param2: Int,
    val cooldownMinutes: Int,
    val isEnabled: Boolean,
    val lastTriggeredAt: Long,
    val createdAt: Long
)

@Entity(tableName = "alert_histories")
data class AlertHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ruleId: Long,
    val ruleName: String,
    val ruleType: String,
    val symbol: String,
    val stockName: String,
    val triggeredPrice: Double,
    val changeRate: Double,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean
)
