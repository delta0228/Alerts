package com.example.data

import android.content.Context
import com.example.engine.RuleEvaluationEngine
import com.example.engine.TechnicalAnalysisEngine
import com.example.model.AlertHistory
import com.example.model.AlertRule
import com.example.model.Candle
import com.example.model.CalculatedCandle
import com.example.model.ChartTimeframe
import com.example.model.MarketType
import com.example.model.RuleCategory
import com.example.model.RuleScope
import com.example.model.RuleType
import com.example.model.Stock
import com.example.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class StockDataManager(
    private val context: Context,
    private val repository: StockRepository
) {
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private val notificationHelper = NotificationHelper(context)

    private val _stocks = MutableStateFlow<List<Stock>>(emptyList())
    val stocks: StateFlow<List<Stock>> = _stocks.asStateFlow()

    private val candleCache = ConcurrentHashMap<String, MutableMap<String, List<CalculatedCandle>>>()

    private val _isMonitoring = MutableStateFlow(true)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    private val _latestAlertEvent = MutableStateFlow<AlertHistory?>(null)
    val latestAlertEvent: StateFlow<AlertHistory?> = _latestAlertEvent.asStateFlow()

    init {
        scope.launch {
            initSeedStocks()
            startRealtimeMarketSimulation()
            startConditionMonitoringEngine()
        }
    }

    private suspend fun initSeedStocks() {
        val initialStocks = listOf(
            Stock("005930", "삼성전자", MarketType.KOSPI, 74800.0, 1200.0, 1.63, 73900.0, 75200.0, 73800.0, 73600.0, 14230500, 1060000000000L, isFavorite = true),
            Stock("000660", "SK하이닉스", MarketType.KOSPI, 186500.0, 5500.0, 3.04, 182000.0, 187800.0, 181500.0, 181000.0, 4850200, 905000000000L, isFavorite = true),
            Stock("373220", "LG에너지솔루션", MarketType.KOSPI, 394500.0, -4500.0, -1.13, 399000.0, 401000.0, 393000.0, 399000.0, 320100, 127000000000L),
            Stock("005380", "현대차", MarketType.KOSPI, 248500.0, 3500.0, 1.43, 246000.0, 251000.0, 245000.0, 245000.0, 890400, 221000000000L),
            Stock("035420", "NAVER", MarketType.KOSPI, 178200.0, -2100.0, -1.16, 180500.0, 181200.0, 177500.0, 180300.0, 612400, 109000000000L, isFavorite = true),
            Stock("068270", "셀트리온", MarketType.KOSPI, 192400.0, 2800.0, 1.48, 190000.0, 194000.0, 189500.0, 189600.0, 720300, 138000000000L),
            Stock("247540", "에코프로비엠", MarketType.KOSDAQ, 168500.0, -5200.0, -2.99, 174000.0, 175200.0, 167000.0, 173700.0, 1205000, 205000000000L, isFavorite = true),
            Stock("035720", "카카오", MarketType.KOSPI, 41200.0, -400.0, -0.96, 416500.0, 41900.0, 40950.0, 41600.0, 954000, 39500000000L),
            Stock("005490", "POSCO홀딩스", MarketType.KOSPI, 362000.0, 4000.0, 1.12, 359000.0, 365000.0, 358000.0, 358000.0, 412000, 149000000000L),
            Stock("086520", "에코프로", MarketType.KOSDAQ, 84300.0, -3100.0, -3.55, 87600.0, 88200.0, 83900.0, 87400.0, 1850000, 158000000000L),
            Stock("NVDA", "엔비디아 (NVIDIA)", MarketType.US, 128.50, 4.20, 3.38, 125.00, 129.20, 124.80, 124.30, 42100000, 5400000000L, isFavorite = true),
            Stock("AAPL", "애플 (Apple)", MarketType.US, 224.80, 1.60, 0.72, 223.50, 225.40, 222.90, 223.20, 31200000, 7000000000L),
            Stock("TSLA", "테슬라 (Tesla)", MarketType.US, 215.30, -6.80, -3.06, 221.00, 222.50, 214.00, 222.10, 58400000, 12600000000L)
        )

        _stocks.value = initialStocks
        repository.insertStocks(initialStocks)

        // Generate initial candles for all stocks
        initialStocks.forEach { stock ->
            generateCandlesForStock(stock)
        }

        // Insert initial default sample alert rules if empty
        val existingRules = repository.allRules.first()
        if (existingRules.isEmpty()) {
            val sampleRules = listOf(
                AlertRule(
                    name = "삼성전자 5일/20일 골든크로스",
                    scope = RuleScope.SPECIFIC,
                    targetSymbol = "005930",
                    targetSymbolName = "삼성전자",
                    timeframe = ChartTimeframe.M5,
                    ruleType = RuleType.MA_GOLDEN_CROSS,
                    param1 = 5,
                    param2 = 20,
                    cooldownMinutes = 15
                ),
                AlertRule(
                    name = "SK하이닉스 188,000원 돌파 알림",
                    scope = RuleScope.SPECIFIC,
                    targetSymbol = "000660",
                    targetSymbolName = "SK하이닉스",
                    timeframe = ChartTimeframe.M5,
                    ruleType = RuleType.PRICE_ABOVE,
                    thresholdValue = 187000.0,
                    cooldownMinutes = 30
                ),
                AlertRule(
                    name = "코스피 전체 RSI 30 이하 과매도 포착",
                    scope = RuleScope.ALL_KOSPI,
                    timeframe = ChartTimeframe.M15,
                    ruleType = RuleType.RSI_OVERSOLD,
                    thresholdValue = 30.0,
                    cooldownMinutes = 60
                ),
                AlertRule(
                    name = "관심종목 거래량 2배 폭증 알림",
                    scope = RuleScope.FAVORITES,
                    timeframe = ChartTimeframe.M5,
                    ruleType = RuleType.VOLUME_SURGE,
                    thresholdValue = 2.0,
                    cooldownMinutes = 30
                )
            )
            sampleRules.forEach { repository.insertRule(it) }
        }
    }

    fun getCandles(symbol: String, timeframe: ChartTimeframe, customDays: Int = 1): List<CalculatedCandle> {
        val key = if (timeframe == ChartTimeframe.CUSTOM_DAYS) "CUSTOM_DAYS_$customDays" else timeframe.name
        val stockMap = candleCache.getOrPut(symbol) { ConcurrentHashMap() }
        return stockMap.getOrPut(key) {
            val stock = _stocks.value.find { it.symbol == symbol } ?: return emptyList()
            generateCandles(stock, timeframe, customDays)
        }
    }

    private fun generateCandlesForStock(stock: Stock) {
        ChartTimeframe.values().forEach { tf ->
            val candles = generateCandles(stock, tf, 1)
            candleCache.getOrPut(stock.symbol) { ConcurrentHashMap() }[tf.name] = candles
        }
    }

    private fun generateCandles(stock: Stock, timeframe: ChartTimeframe, customDays: Int = 1): List<CalculatedCandle> {
        val count = 80
        val basePrice = stock.prevClosePrice
        val now = System.currentTimeMillis()
        val effectiveMinutes = if (timeframe == ChartTimeframe.CUSTOM_DAYS) {
            customDays.coerceAtLeast(1) * 1440
        } else {
            timeframe.minutes
        }
        val intervalMs = effectiveMinutes * 60 * 1000L
        val rawCandles = mutableListOf<Candle>()

        val volatilityFactor = when (timeframe) {
            ChartTimeframe.M1 -> 0.005
            ChartTimeframe.M5 -> 0.008
            ChartTimeframe.M15 -> 0.012
            ChartTimeframe.H1 -> 0.018
            ChartTimeframe.DAILY -> 0.025
            ChartTimeframe.D2 -> 0.030
            ChartTimeframe.D3 -> 0.035
            ChartTimeframe.D5 -> 0.042
            ChartTimeframe.D10 -> 0.055
            ChartTimeframe.WEEKLY -> 0.045
            ChartTimeframe.MONTHLY -> 0.075
            ChartTimeframe.YEARLY -> 0.120
            ChartTimeframe.CUSTOM_DAYS -> (0.025 * kotlin.math.sqrt(customDays.toDouble().coerceAtLeast(1.0))).coerceIn(0.025, 0.12)
        }

        val volumeMultiplier = when (timeframe) {
            ChartTimeframe.M1, ChartTimeframe.M5 -> 1.0
            ChartTimeframe.M15 -> 2.5
            ChartTimeframe.H1 -> 8.0
            ChartTimeframe.DAILY -> 25.0
            ChartTimeframe.D2 -> 45.0
            ChartTimeframe.D3 -> 65.0
            ChartTimeframe.D5 -> 100.0
            ChartTimeframe.D10 -> 180.0
            ChartTimeframe.WEEKLY -> 120.0
            ChartTimeframe.MONTHLY -> 500.0
            ChartTimeframe.YEARLY -> 4000.0
            ChartTimeframe.CUSTOM_DAYS -> (25.0 * customDays.coerceAtLeast(1)).coerceAtLeast(25.0)
        }

        var currentClose = basePrice * (0.88 + Random.nextDouble() * 0.12)
        for (i in count downTo 0) {
            val time = now - (i * intervalMs)
            val volatility = currentClose * volatilityFactor
            val open = currentClose
            val delta = (Random.nextDouble() - 0.485) * volatility * 2
            val close = (open + delta).coerceAtLeast(1.0)
            val high = maxOf(open, close) + Random.nextDouble() * volatility
            val low = minOf(open, close) - Random.nextDouble() * volatility
            val baseVol = ((stock.volume / count) * volumeMultiplier).toLong().coerceAtLeast(100L)
            val volume = (Random.nextDouble(0.5, 2.5) * baseVol).toLong().coerceAtLeast(100L)

            rawCandles.add(
                Candle(
                    timestamp = time,
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    volume = volume
                )
            )
            currentClose = close
        }

        // Align latest candle with stock current price
        val last = rawCandles.last()
        rawCandles[rawCandles.lastIndex] = last.copy(
            close = stock.currentPrice,
            high = maxOf(last.high, stock.currentPrice),
            low = minOf(last.low, stock.currentPrice)
        )

        return TechnicalAnalysisEngine.calculateIndicators(rawCandles)
    }

    /**
     * Real-time tick oscillation simulation loop
     */
    private fun startRealtimeMarketSimulation() {
        scope.launch {
            while (isActive) {
                delay(2500) // Update tick every 2.5s
                val currentList = _stocks.value
                if (currentList.isEmpty()) continue

                val updatedList = currentList.map { stock ->
                    val tickDelta = (Random.nextDouble() - 0.49) * (stock.currentPrice * 0.0025)
                    val newPrice = (stock.currentPrice + tickDelta).let {
                        if (stock.market == MarketType.US) (it * 100).toLong() / 100.0
                        else ((it / 100).toLong() * 100).toDouble()
                    }
                    val newChangePrice = newPrice - stock.prevClosePrice
                    val newChangeRate = (newChangePrice / stock.prevClosePrice) * 100
                    val newHigh = maxOf(stock.highPrice, newPrice)
                    val newLow = minOf(stock.lowPrice, newPrice)
                    val volIncrement = Random.nextLong(100, 3000)

                    stock.copy(
                        currentPrice = newPrice,
                        changePrice = newChangePrice,
                        changeRate = newChangeRate,
                        highPrice = newHigh,
                        lowPrice = newLow,
                        volume = stock.volume + volIncrement
                    )
                }

                _stocks.value = updatedList

                // Update latest candle in cache
                updatedList.forEach { stock ->
                    val map = candleCache[stock.symbol] ?: return@forEach
                    map.forEach { (tf, candles) ->
                        if (candles.isNotEmpty()) {
                            val lastCalculated = candles.last()
                            val updatedLastCandle = lastCalculated.candle.copy(
                                close = stock.currentPrice,
                                high = maxOf(lastCalculated.candle.high, stock.currentPrice),
                                low = minOf(lastCalculated.candle.low, stock.currentPrice),
                                volume = lastCalculated.candle.volume + 50
                            )
                            val mutableList = candles.toMutableList()
                            mutableList[mutableList.lastIndex] = lastCalculated.copy(candle = updatedLastCandle)
                            map[tf] = TechnicalAnalysisEngine.calculateIndicators(mutableList.map { it.candle })
                        }
                    }
                }
            }
        }
    }

    /**
     * Continuous Condition Monitoring Engine loop
     */
    private fun startConditionMonitoringEngine() {
        scope.launch {
            while (isActive) {
                delay(4000) // Evaluate rules every 4s
                if (!_isMonitoring.value) continue

                val activeRules = repository.getActiveRules()
                if (activeRules.isEmpty()) continue

                val currentStocks = _stocks.value
                val now = System.currentTimeMillis()

                for (rule in activeRules) {
                    for (stock in currentStocks) {
                        val candles = getCandles(stock.symbol, rule.timeframe)
                        val alertHistory = RuleEvaluationEngine.evaluateRule(
                            rule = rule,
                            stock = stock,
                            calculatedCandles = candles,
                            currentTime = now
                        )

                        if (alertHistory != null) {
                            // Rule triggered!
                            val insertedId = repository.insertHistory(alertHistory)
                            repository.updateRuleLastTriggered(rule.id, now)

                            val fullAlert = alertHistory.copy(id = insertedId)
                            _latestAlertEvent.value = fullAlert
                            notificationHelper.sendStockAlertNotification(fullAlert)
                        }
                    }
                }
            }
        }
    }

    fun toggleMonitoring() {
        _isMonitoring.value = !_isMonitoring.value
    }

    fun dismissLatestAlert() {
        _latestAlertEvent.value = null
    }

    fun toggleFavorite(symbol: String) {
        scope.launch {
            val stock = _stocks.value.find { it.symbol == symbol } ?: return@launch
            val newFav = !stock.isFavorite
            _stocks.value = _stocks.value.map {
                if (it.symbol == symbol) it.copy(isFavorite = newFav) else it
            }
            repository.toggleFavorite(symbol, newFav)
        }
    }

    /**
     * Run manual full-market scan for preset or rule
     */
    fun runPresetScan(ruleType: RuleType, threshold: Double, param1: Int = 5, param2: Int = 20): List<Pair<Stock, String>> {
        val results = mutableListOf<Pair<Stock, String>>()
        val dummyRule = AlertRule(
            name = "즉시 스캔",
            ruleType = ruleType,
            thresholdValue = threshold,
            param1 = param1,
            param2 = param2,
            cooldownMinutes = 0
        )

        _stocks.value.forEach { stock ->
            val candles = getCandles(stock.symbol, ChartTimeframe.M5)
            val alert = RuleEvaluationEngine.evaluateRule(dummyRule, stock, candles)
            if (alert != null) {
                results.add(stock to alert.message)
            }
        }
        return results
    }
}
