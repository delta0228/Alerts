package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.StockDataManager
import com.example.data.StockRepository
import com.example.data.local.StockDatabase
import com.example.model.AlertHistory
import com.example.model.AlertRule
import com.example.model.ChartTimeframe
import com.example.model.RuleType
import com.example.model.Stock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class Screen {
    data object MarketWatch : Screen()
    data class StockDetail(val stock: Stock) : Screen()
    data class RuleBuilder(val stock: Stock? = null, val ruleType: RuleType? = null) : Screen()
    data class Backtest(val stock: Stock? = null, val ruleType: RuleType? = null) : Screen()
    data object RulesList : Screen()
    data object AlertHistoryList : Screen()
    data object ConditionScanner : Screen()
}

class StockAlertViewModel(application: Application) : AndroidViewModel(application) {

    private val db = StockDatabase.getDatabase(application)
    val repository = StockRepository(db.stockDao(), db.alertRuleDao(), db.alertHistoryDao())
    val dataManager = StockDataManager(application, repository)

    val stocks: StateFlow<List<Stock>> = dataManager.stocks
    val isMonitoring: StateFlow<Boolean> = dataManager.isMonitoring
    val latestAlert: StateFlow<AlertHistory?> = dataManager.latestAlertEvent

    val allRules: StateFlow<List<AlertRule>> = repository.allRules.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allHistories: StateFlow<List<AlertHistory>> = repository.allHistories.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val unreadCount: StateFlow<Int> = repository.unreadCount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    private val _currentScreen = MutableStateFlow<Screen>(Screen.MarketWatch)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _showArchitectureDocs = MutableStateFlow(false)
    val showArchitectureDocs: StateFlow<Boolean> = _showArchitectureDocs.asStateFlow()

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun openArchitectureDocs() {
        _showArchitectureDocs.value = true
    }

    fun closeArchitectureDocs() {
        _showArchitectureDocs.value = false
    }

    fun toggleMonitoring() {
        dataManager.toggleMonitoring()
    }

    fun toggleFavorite(symbol: String) {
        dataManager.toggleFavorite(symbol)
    }

    fun saveRule(rule: AlertRule) {
        viewModelScope.launch {
            repository.insertRule(rule)
            _currentScreen.value = Screen.RulesList
        }
    }

    fun toggleRule(id: Long, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.setRuleEnabled(id, isEnabled)
        }
    }

    fun deleteRule(id: Long) {
        viewModelScope.launch {
            repository.deleteRule(id)
        }
    }

    fun markHistoryAsRead(id: Long) {
        viewModelScope.launch {
            repository.markHistoryAsRead(id)
        }
    }

    fun markAllHistoriesAsRead() {
        viewModelScope.launch {
            repository.markAllHistoriesAsRead()
        }
    }

    fun clearAllHistories() {
        viewModelScope.launch {
            repository.clearAllHistories()
        }
    }

    fun dismissBanner() {
        dataManager.dismissLatestAlert()
    }
}
