package com.example.engine

import com.example.model.CalculatedCandle
import com.example.model.ChartTimeframe
import com.example.model.RuleType
import com.example.model.Stock
import kotlin.math.max
import kotlin.math.min

enum class ExitReason(val displayName: String) {
    TAKE_PROFIT("목표가 익절"),
    STOP_LOSS("손절매 이탈"),
    OPPOSITE_SIGNAL("반대 신호 청산"),
    MAX_HOLD_BARS("최대 보유 만료"),
    END_OF_DATA("데이터 종료 청산")
}

data class BacktestConfig(
    val symbol: String,
    val timeframe: ChartTimeframe = ChartTimeframe.M5,
    val ruleType: RuleType = RuleType.MA_GOLDEN_CROSS,
    val thresholdValue: Double = 30.0,
    val param1: Int = 5,
    val param2: Int = 20,
    val initialCapital: Double = 10_000_000.0,
    val takeProfitPct: Double = 4.0, // 익절 % (0.0이면 비활성)
    val stopLossPct: Double = 2.0,   // 손절 % (0.0이면 비활성)
    val maxHoldBars: Int = 20,       // 최대 보유 봉 수
    val exitOnOppositeSignal: Boolean = true,
    val feeRatePct: Double = 0.015   // 매매 수수료 + 슬리피지 % (편도)
)

data class BacktestTrade(
    val tradeId: Int,
    val entryIndex: Int,
    val entryTimestamp: Long,
    val entryPrice: Double,
    val exitIndex: Int,
    val exitTimestamp: Long,
    val exitPrice: Double,
    val shares: Int,
    val grossProfit: Double,
    val netProfit: Double,
    val profitRatePct: Double,
    val holdingBars: Int,
    val exitReason: ExitReason
) {
    val isWin: Boolean get() = netProfit > 0
}

data class EquityPoint(
    val index: Int,
    val timestamp: Long,
    val equity: Double,
    val returnPct: Double,
    val drawdownPct: Double,
    val price: Double
)

data class BacktestResult(
    val config: BacktestConfig,
    val stock: Stock,
    val initialCapital: Double,
    val finalCapital: Double,
    val totalProfit: Double,
    val totalReturnPct: Double,
    val benchmarkReturnPct: Double,
    val totalTrades: Int,
    val winningTrades: Int,
    val losingTrades: Int,
    val winRatePct: Double,
    val profitFactor: Double,
    val maxDrawdownPct: Double,
    val avgTradeProfitPct: Double,
    val avgWinPct: Double,
    val avgLossPct: Double,
    val avgHoldingBars: Double,
    val equityCurve: List<EquityPoint>,
    val trades: List<BacktestTrade>,
    val candles: List<CalculatedCandle>
)

data class StrategyPreset(
    val id: String,
    val title: String,
    val description: String,
    val iconEmoji: String,
    val ruleType: RuleType,
    val param1: Int,
    val param2: Int,
    val thresholdValue: Double,
    val takeProfitPct: Double,
    val stopLossPct: Double,
    val maxHoldBars: Int,
    val exitOnOpposite: Boolean
)

object BacktestEngine {

    val PRESET_STRATEGIES = listOf(
        StrategyPreset(
            id = "ma_golden_cross",
            title = "5/20일 이평 골든크로스",
            description = "단기 5일선이 20일선을 상향 돌파 시 매수, 익절 +5% / 손절 -2.5%",
            iconEmoji = "⚡",
            ruleType = RuleType.MA_GOLDEN_CROSS,
            param1 = 5,
            param2 = 20,
            thresholdValue = 0.0,
            takeProfitPct = 5.0,
            stopLossPct = 2.5,
            maxHoldBars = 25,
            exitOnOpposite = true
        ),
        StrategyPreset(
            id = "rsi_oversold_bounce",
            title = "RSI(14) 30 과매도 반등",
            description = "RSI가 30 이하 과매도권 진입 시 역추세 매수, 익절 +4% / 손절 -2%",
            iconEmoji = "💎",
            ruleType = RuleType.RSI_OVERSOLD,
            param1 = 14,
            param2 = 0,
            thresholdValue = 30.0,
            takeProfitPct = 4.0,
            stopLossPct = 2.0,
            maxHoldBars = 15,
            exitOnOpposite = true
        ),
        StrategyPreset(
            id = "bollinger_lower_reversal",
            title = "볼린저 밴드 하단 터치 반등",
            description = "하단 밴드 이탈 후 반등 시 매수, 익절 +3.5% / 손절 -2%",
            iconEmoji = "🛡️",
            ruleType = RuleType.BOLLINGER_LOWER_TOUCH,
            param1 = 20,
            param2 = 2,
            thresholdValue = 0.0,
            takeProfitPct = 3.5,
            stopLossPct = 2.0,
            maxHoldBars = 12,
            exitOnOpposite = false
        ),
        StrategyPreset(
            id = "volume_surge_breakout",
            title = "거래량 2배 폭증 돌파",
            description = "20일 평균 대비 2배 거래량 발생 시 모멘텀 진입, 익절 +6% / 손절 -3%",
            iconEmoji = "🚀",
            ruleType = RuleType.VOLUME_SURGE,
            param1 = 20,
            param2 = 0,
            thresholdValue = 2.0,
            takeProfitPct = 6.0,
            stopLossPct = 3.0,
            maxHoldBars = 20,
            exitOnOpposite = false
        ),
        StrategyPreset(
            id = "plunge_dip_buying",
            title = "단기 급락 저가 매수",
            description = "단기 -2.5% 이상 하락 시 기술적 반등 노림, 익절 +3% / 손절 -1.8%",
            iconEmoji = "📉",
            ruleType = RuleType.CHANGE_RATE_PLUNGE,
            param1 = 0,
            param2 = 0,
            thresholdValue = 2.5,
            takeProfitPct = 3.0,
            stopLossPct = 1.8,
            maxHoldBars = 10,
            exitOnOpposite = false
        )
    )

    /**
     * Executes backtest simulation on historical candlestick series.
     */
    fun runBacktest(
        stock: Stock,
        candles: List<CalculatedCandle>,
        config: BacktestConfig
    ): BacktestResult {
        if (candles.size < 25) {
            // Return empty result if not enough data
            return createEmptyResult(stock, candles, config)
        }

        var capital = config.initialCapital
        var maxCapitalSeen = capital
        var maxDrawdownPct = 0.0

        val executedTrades = mutableListOf<BacktestTrade>()
        val equityPoints = mutableListOf<EquityPoint>()

        var inPosition = false
        var entryIndex = -1
        var entryTime = 0L
        var entryPrice = 0.0
        var shares = 0
        var tradeCounter = 1

        val feeFactor = config.feeRatePct / 100.0

        for (i in 1 until candles.size) {
            val prevCalculated = candles[i - 1]
            val currentCalculated = candles[i]
            val currentCandle = currentCalculated.candle

            if (!inPosition) {
                // Check Entry Signal
                val isEntrySignal = evaluateEntrySignal(
                    prev = prevCalculated,
                    current = currentCalculated,
                    ruleType = config.ruleType,
                    param1 = config.param1,
                    param2 = config.param2,
                    threshold = config.thresholdValue,
                    stock = stock
                )

                if (isEntrySignal && i < candles.size - 2) {
                    // Open Long Position at current close
                    entryPrice = currentCandle.close
                    shares = (capital / entryPrice).toInt()
                    if (shares > 0) {
                        val buyCost = shares * entryPrice * (1.0 + feeFactor)
                        capital -= buyCost
                        inPosition = true
                        entryIndex = i
                        entryTime = currentCandle.timestamp
                    }
                }
            } else {
                // Currently in position -> Check Exit conditions
                val barsHeld = i - entryIndex
                var shouldExit = false
                var exitPrice = currentCandle.close
                var exitReason = ExitReason.END_OF_DATA

                // 1. Take Profit Check
                if (config.takeProfitPct > 0.0) {
                    val targetPrice = entryPrice * (1.0 + config.takeProfitPct / 100.0)
                    if (currentCandle.high >= targetPrice) {
                        shouldExit = true
                        exitPrice = targetPrice
                        exitReason = ExitReason.TAKE_PROFIT
                    }
                }

                // 2. Stop Loss Check (priority over take profit if hit)
                if (config.stopLossPct > 0.0) {
                    val stopPrice = entryPrice * (1.0 - config.stopLossPct / 100.0)
                    if (currentCandle.low <= stopPrice) {
                        shouldExit = true
                        exitPrice = stopPrice
                        exitReason = ExitReason.STOP_LOSS
                    }
                }

                // 3. Opposite Signal Check
                if (!shouldExit && config.exitOnOppositeSignal) {
                    val isOpposite = evaluateExitSignal(
                        prev = prevCalculated,
                        current = currentCalculated,
                        entryRule = config.ruleType,
                        param1 = config.param1,
                        param2 = config.param2,
                        threshold = config.thresholdValue
                    )
                    if (isOpposite) {
                        shouldExit = true
                        exitPrice = currentCandle.close
                        exitReason = ExitReason.OPPOSITE_SIGNAL
                    }
                }

                // 4. Max Holding Period
                if (!shouldExit && barsHeld >= config.maxHoldBars) {
                    shouldExit = true
                    exitPrice = currentCandle.close
                    exitReason = ExitReason.MAX_HOLD_BARS
                }

                // 5. Last Candle Forced Exit
                if (!shouldExit && i == candles.size - 1) {
                    shouldExit = true
                    exitPrice = currentCandle.close
                    exitReason = ExitReason.END_OF_DATA
                }

                if (shouldExit) {
                    val sellProceeds = shares * exitPrice * (1.0 - feeFactor)
                    capital += sellProceeds

                    val gross = shares * (exitPrice - entryPrice)
                    val net = gross - (shares * entryPrice * feeFactor) - (shares * exitPrice * feeFactor)
                    val returnPct = ((exitPrice - entryPrice) / entryPrice) * 100.0 - (feeFactor * 200.0)

                    executedTrades.add(
                        BacktestTrade(
                            tradeId = tradeCounter++,
                            entryIndex = entryIndex,
                            entryTimestamp = entryTime,
                            entryPrice = entryPrice,
                            exitIndex = i,
                            exitTimestamp = currentCandle.timestamp,
                            exitPrice = exitPrice,
                            shares = shares,
                            grossProfit = gross,
                            netProfit = net,
                            profitRatePct = returnPct,
                            holdingBars = barsHeld,
                            exitReason = exitReason
                        )
                    )

                    inPosition = false
                    entryIndex = -1
                    shares = 0
                }
            }

            // Calculate Portfolio Equity at this bar
            val currentPortfolioValue = if (inPosition) {
                capital + (shares * currentCandle.close * (1.0 - feeFactor))
            } else {
                capital
            }

            maxCapitalSeen = max(maxCapitalSeen, currentPortfolioValue)
            val currentDrawdownPct = if (maxCapitalSeen > 0) {
                ((maxCapitalSeen - currentPortfolioValue) / maxCapitalSeen) * 100.0
            } else 0.0

            maxDrawdownPct = max(maxDrawdownPct, currentDrawdownPct)

            val capitalReturnPct = ((currentPortfolioValue - config.initialCapital) / config.initialCapital) * 100.0

            equityPoints.add(
                EquityPoint(
                    index = i,
                    timestamp = currentCandle.timestamp,
                    equity = currentPortfolioValue,
                    returnPct = capitalReturnPct,
                    drawdownPct = currentDrawdownPct,
                    price = currentCandle.close
                )
            )
        }

        // Finalize Summary Metrics
        val finalCapital = equityPoints.lastOrNull()?.equity ?: capital
        val totalProfit = finalCapital - config.initialCapital
        val totalReturnPct = (totalProfit / config.initialCapital) * 100.0

        val firstPrice = candles.first().candle.close
        val lastPrice = candles.last().candle.close
        val benchmarkReturnPct = ((lastPrice - firstPrice) / firstPrice) * 100.0

        val winTrades = executedTrades.filter { it.isWin }
        val lossTrades = executedTrades.filter { !it.isWin }
        val totalTrades = executedTrades.size
        val winRatePct = if (totalTrades > 0) (winTrades.size.toDouble() / totalTrades) * 100.0 else 0.0

        val grossWins = winTrades.sumOf { it.netProfit }
        val grossLosses = lossTrades.sumOf { -it.netProfit }
        val profitFactor = if (grossLosses > 0.0) grossWins / grossLosses else if (grossWins > 0.0) 9.99 else 0.0

        val avgTradeProfitPct = if (totalTrades > 0) executedTrades.map { it.profitRatePct }.average() else 0.0
        val avgWinPct = if (winTrades.isNotEmpty()) winTrades.map { it.profitRatePct }.average() else 0.0
        val avgLossPct = if (lossTrades.isNotEmpty()) lossTrades.map { it.profitRatePct }.average() else 0.0
        val avgHoldingBars = if (totalTrades > 0) executedTrades.map { it.holdingBars.toDouble() }.average() else 0.0

        return BacktestResult(
            config = config,
            stock = stock,
            initialCapital = config.initialCapital,
            finalCapital = finalCapital,
            totalProfit = totalProfit,
            totalReturnPct = totalReturnPct,
            benchmarkReturnPct = benchmarkReturnPct,
            totalTrades = totalTrades,
            winningTrades = winTrades.size,
            losingTrades = lossTrades.size,
            winRatePct = winRatePct,
            profitFactor = min(profitFactor, 99.9),
            maxDrawdownPct = maxDrawdownPct,
            avgTradeProfitPct = avgTradeProfitPct,
            avgWinPct = avgWinPct,
            avgLossPct = avgLossPct,
            avgHoldingBars = avgHoldingBars,
            equityCurve = equityPoints,
            trades = executedTrades,
            candles = candles
        )
    }

    private fun evaluateEntrySignal(
        prev: CalculatedCandle,
        current: CalculatedCandle,
        ruleType: RuleType,
        param1: Int,
        param2: Int,
        threshold: Double,
        stock: Stock
    ): Boolean {
        return when (ruleType) {
            RuleType.MA_GOLDEN_CROSS -> {
                val prevFast = getMA(prev, param1)
                val prevSlow = getMA(prev, param2)
                val currFast = getMA(current, param1)
                val currSlow = getMA(current, param2)
                if (prevFast != null && prevSlow != null && currFast != null && currSlow != null) {
                    prevFast <= prevSlow && currFast > currSlow
                } else false
            }

            RuleType.MA_DEAD_CROSS -> {
                val prevFast = getMA(prev, param1)
                val prevSlow = getMA(prev, param2)
                val currFast = getMA(current, param1)
                val currSlow = getMA(current, param2)
                if (prevFast != null && prevSlow != null && currFast != null && currSlow != null) {
                    prevFast >= prevSlow && currFast < currSlow
                } else false
            }

            RuleType.RSI_OVERSOLD -> {
                val prevRsi = prev.indicators.rsi14
                val currRsi = current.indicators.rsi14
                if (prevRsi != null && currRsi != null) {
                    currRsi <= threshold || (prevRsi <= threshold && currRsi > prevRsi)
                } else false
            }

            RuleType.RSI_OVERBOUGHT -> {
                val currRsi = current.indicators.rsi14
                currRsi != null && currRsi >= threshold
            }

            RuleType.BOLLINGER_LOWER_TOUCH -> {
                val lower = current.indicators.bollingerLower
                lower != null && current.candle.low <= lower
            }

            RuleType.BOLLINGER_UPPER_BREAK -> {
                val upper = current.indicators.bollingerUpper
                upper != null && current.candle.high >= upper
            }

            RuleType.VOLUME_SURGE -> {
                val avgVol = current.indicators.volumeMa20
                if (avgVol != null && avgVol > 0) {
                    current.candle.volume >= avgVol * threshold && current.candle.close > current.candle.open
                } else false
            }

            RuleType.PRICE_ABOVE -> {
                current.candle.close >= threshold && prev.candle.close < threshold
            }

            RuleType.PRICE_BELOW -> {
                current.candle.close <= threshold && prev.candle.close > threshold
            }

            RuleType.CHANGE_RATE_SURGE -> {
                val changeRate = ((current.candle.close - prev.candle.close) / prev.candle.close) * 100.0
                changeRate >= threshold
            }

            RuleType.CHANGE_RATE_PLUNGE -> {
                val changeRate = ((current.candle.close - prev.candle.close) / prev.candle.close) * 100.0
                changeRate <= -threshold
            }
        }
    }

    private fun evaluateExitSignal(
        prev: CalculatedCandle,
        current: CalculatedCandle,
        entryRule: RuleType,
        param1: Int,
        param2: Int,
        threshold: Double
    ): Boolean {
        return when (entryRule) {
            RuleType.MA_GOLDEN_CROSS -> {
                // Exit on Dead Cross
                val prevFast = getMA(prev, param1)
                val prevSlow = getMA(prev, param2)
                val currFast = getMA(current, param1)
                val currSlow = getMA(current, param2)
                if (prevFast != null && prevSlow != null && currFast != null && currSlow != null) {
                    currFast < currSlow
                } else false
            }

            RuleType.RSI_OVERSOLD -> {
                // Exit when RSI reaches overbought level (65+)
                val rsi = current.indicators.rsi14
                rsi != null && rsi >= 65.0
            }

            RuleType.BOLLINGER_LOWER_TOUCH -> {
                // Exit when touching Upper band or middle band
                val upper = current.indicators.bollingerUpper
                upper != null && current.candle.high >= upper
            }

            RuleType.VOLUME_SURGE, RuleType.CHANGE_RATE_SURGE -> {
                // Exit on bearish reversal candle
                current.candle.close < current.candle.open && current.candle.close < prev.candle.low
            }

            else -> false
        }
    }

    private fun getMA(candle: CalculatedCandle, period: Int): Double? {
        return when (period) {
            5 -> candle.indicators.ma5
            20 -> candle.indicators.ma20
            60 -> candle.indicators.ma60
            120 -> candle.indicators.ma120
            else -> candle.indicators.ma20
        }
    }

    private fun createEmptyResult(
        stock: Stock,
        candles: List<CalculatedCandle>,
        config: BacktestConfig
    ): BacktestResult {
        return BacktestResult(
            config = config,
            stock = stock,
            initialCapital = config.initialCapital,
            finalCapital = config.initialCapital,
            totalProfit = 0.0,
            totalReturnPct = 0.0,
            benchmarkReturnPct = 0.0,
            totalTrades = 0,
            winningTrades = 0,
            losingTrades = 0,
            winRatePct = 0.0,
            profitFactor = 0.0,
            maxDrawdownPct = 0.0,
            avgTradeProfitPct = 0.0,
            avgWinPct = 0.0,
            avgLossPct = 0.0,
            avgHoldingBars = 0.0,
            equityCurve = emptyList(),
            trades = emptyList(),
            candles = candles
        )
    }
}
