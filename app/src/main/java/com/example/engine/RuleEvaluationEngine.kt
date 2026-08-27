package com.example.engine

import com.example.model.AlertHistory
import com.example.model.AlertRule
import com.example.model.Candle
import com.example.model.CalculatedCandle
import com.example.model.RuleScope
import com.example.model.RuleType
import com.example.model.Stock

object RuleEvaluationEngine {

    data class EvaluationResult(
        val isTriggered: Boolean,
        val message: String,
        val triggeredPrice: Double
    )

    /**
     * Evaluates a single rule for a specific stock and its calculated candles.
     */
    fun evaluateRule(
        rule: AlertRule,
        stock: Stock,
        calculatedCandles: List<CalculatedCandle>,
        currentTime: Long = System.currentTimeMillis()
    ): AlertHistory? {
        if (!rule.isEnabled) return null

        // Scope check
        val matchesScope = when (rule.scope) {
            RuleScope.SPECIFIC -> rule.targetSymbol.isEmpty() || rule.targetSymbol == stock.symbol
            RuleScope.ALL_KOSPI -> stock.market.name == "KOSPI"
            RuleScope.ALL_KOSDAQ -> stock.market.name == "KOSDAQ"
            RuleScope.ALL_US -> stock.market.name == "US"
            RuleScope.FAVORITES -> stock.isFavorite
        }
        if (!matchesScope) return null

        // Cooldown check
        if (rule.isCoolingDown(currentTime)) return null

        val result = evaluateCondition(rule, stock, calculatedCandles)
        if (result.isTriggered) {
            return AlertHistory(
                ruleId = rule.id,
                ruleName = rule.name,
                ruleType = rule.ruleType,
                symbol = stock.symbol,
                stockName = stock.name,
                triggeredPrice = result.triggeredPrice,
                changeRate = stock.changeRate,
                message = result.message,
                timestamp = currentTime,
                isRead = false
            )
        }

        return null
    }

    private fun evaluateCondition(
        rule: AlertRule,
        stock: Stock,
        candles: List<CalculatedCandle>
    ): EvaluationResult {
        val currentPrice = stock.currentPrice
        val lastIdx = candles.lastIndex
        val lastCandle = candles.getOrNull(lastIdx)
        val prevCandle = candles.getOrNull(lastIdx - 1)

        when (rule.ruleType) {
            RuleType.PRICE_ABOVE -> {
                if (currentPrice >= rule.thresholdValue) {
                    return EvaluationResult(
                        isTriggered = true,
                        message = "목표가 돌파: 현재가 %,.0f원 (설정가: %,.0f원)".format(currentPrice, rule.thresholdValue),
                        triggeredPrice = currentPrice
                    )
                }
            }
            RuleType.PRICE_BELOW -> {
                if (currentPrice <= rule.thresholdValue) {
                    return EvaluationResult(
                        isTriggered = true,
                        message = "지지선 이탈: 현재가 %,.0f원 (설정가: %,.0f원)".format(currentPrice, rule.thresholdValue),
                        triggeredPrice = currentPrice
                    )
                }
            }
            RuleType.CHANGE_RATE_SURGE -> {
                if (stock.changeRate >= rule.thresholdValue) {
                    return EvaluationResult(
                        isTriggered = true,
                        message = "당일 급등 포착: %+,.2f%% 상승 (기준: +%.1f%%)".format(stock.changeRate, rule.thresholdValue),
                        triggeredPrice = currentPrice
                    )
                }
            }
            RuleType.CHANGE_RATE_PLUNGE -> {
                val targetPlunge = -kotlin.math.abs(rule.thresholdValue)
                if (stock.changeRate <= targetPlunge) {
                    return EvaluationResult(
                        isTriggered = true,
                        message = "당일 급락 경고: %+,.2f%% 하락 (기준: %.1f%%)".format(stock.changeRate, targetPlunge),
                        triggeredPrice = currentPrice
                    )
                }
            }
            RuleType.ELLIOTT_WAVE_3_IMPULSE -> {
                val elliott = ElliottWaveEngine.analyzeElliottWaves(candles)
                if (elliott.currentPhase == com.example.model.WavePhase.WAVE_3_IMPULSE) {
                    return EvaluationResult(
                        isTriggered = true,
                        message = "🌊 엘리엇 3파 급등 국면 진입! (목표가: %,.0f원)".format(elliott.targetPrice),
                        triggeredPrice = currentPrice
                    )
                }
            }
            RuleType.ELLIOTT_WAVE_4_PULLBACK -> {
                val elliott = ElliottWaveEngine.analyzeElliottWaves(candles)
                if (elliott.currentPhase == com.example.model.WavePhase.WAVE_4_CONSOLIDATION) {
                    return EvaluationResult(
                        isTriggered = true,
                        message = "🌊 엘리엇 4파 눌림목 지지선 도달 (지지선: %,.0f원)".format(elliott.supportPrice),
                        triggeredPrice = currentPrice
                    )
                }
            }
            RuleType.ELLIOTT_WAVE_5_TOP -> {
                val elliott = ElliottWaveEngine.analyzeElliottWaves(candles)
                if (elliott.currentPhase == com.example.model.WavePhase.WAVE_5_CLIMAX) {
                    return EvaluationResult(
                        isTriggered = true,
                        message = "⚠️ 엘리엇 5파 최고점 도달 경고! 분할 익절 고려",
                        triggeredPrice = currentPrice
                    )
                }
            }
            RuleType.MA_GOLDEN_CROSS -> {
                if (lastCandle != null && prevCandle != null) {
                    val fastLast = getMA(lastCandle, rule.param1)
                    val slowLast = getMA(lastCandle, rule.param2)
                    val fastPrev = getMA(prevCandle, rule.param1)
                    val slowPrev = getMA(prevCandle, rule.param2)

                    if (fastLast != null && slowLast != null && fastPrev != null && slowPrev != null) {
                        // Golden cross: previously fast <= slow, currently fast > slow
                        val isGoldenCross = (fastPrev <= slowPrev) && (fastLast > slowLast)
                        // Or strong crossover within recent candle
                        if (isGoldenCross || (fastLast > slowLast && (fastLast - slowLast) / slowLast < 0.005)) {
                            return EvaluationResult(
                                isTriggered = true,
                                message = "${rule.param1}일선(%,.0f)이 ${rule.param2}일선(%,.0f)을 골든크로스!".format(fastLast, slowLast),
                                triggeredPrice = currentPrice
                            )
                        }
                    }
                }
            }
            RuleType.MA_DEAD_CROSS -> {
                if (lastCandle != null && prevCandle != null) {
                    val fastLast = getMA(lastCandle, rule.param1)
                    val slowLast = getMA(lastCandle, rule.param2)
                    val fastPrev = getMA(prevCandle, rule.param1)
                    val slowPrev = getMA(prevCandle, rule.param2)

                    if (fastLast != null && slowLast != null && fastPrev != null && slowPrev != null) {
                        val isDeadCross = (fastPrev >= slowPrev) && (fastLast < slowLast)
                        if (isDeadCross) {
                            return EvaluationResult(
                                isTriggered = true,
                                message = "${rule.param1}일선(%,.0f)이 ${rule.param2}일선(%,.0f)을 데드크로스 하향!".format(fastLast, slowLast),
                                triggeredPrice = currentPrice
                            )
                        }
                    }
                }
            }
            RuleType.RSI_OVERSOLD -> {
                val currentRsi = lastCandle?.indicators?.rsi14
                if (currentRsi != null && currentRsi <= rule.thresholdValue) {
                    return EvaluationResult(
                        isTriggered = true,
                        message = "RSI 과매도 포착: RSI(14) %.1f (기준: %.0f 이하)".format(currentRsi, rule.thresholdValue),
                        triggeredPrice = currentPrice
                    )
                }
            }
            RuleType.RSI_OVERBOUGHT -> {
                val currentRsi = lastCandle?.indicators?.rsi14
                if (currentRsi != null && currentRsi >= rule.thresholdValue) {
                    return EvaluationResult(
                        isTriggered = true,
                        message = "RSI 과매수 경고: RSI(14) %.1f (기준: %.0f 이상)".format(currentRsi, rule.thresholdValue),
                        triggeredPrice = currentPrice
                    )
                }
            }
            RuleType.BOLLINGER_LOWER_TOUCH -> {
                val lowerBand = lastCandle?.indicators?.bollingerLower
                if (lowerBand != null && currentPrice <= lowerBand * 1.002) {
                    return EvaluationResult(
                        isTriggered = true,
                        message = "볼린저 밴드 하단선(%,.0f) 터치/반등 포착!".format(lowerBand),
                        triggeredPrice = currentPrice
                    )
                }
            }
            RuleType.BOLLINGER_UPPER_BREAK -> {
                val upperBand = lastCandle?.indicators?.bollingerUpper
                if (upperBand != null && currentPrice >= upperBand) {
                    return EvaluationResult(
                        isTriggered = true,
                        message = "볼린저 밴드 상단선(%,.0f) 강력 돌파!".format(upperBand),
                        triggeredPrice = currentPrice
                    )
                }
            }
            RuleType.VOLUME_SURGE -> {
                val volumeMa = lastCandle?.indicators?.volumeMa20
                if (volumeMa != null && volumeMa > 0) {
                    val currentVol = stock.volume.toDouble()
                    val ratio = currentVol / volumeMa
                    if (ratio >= rule.thresholdValue) {
                        return EvaluationResult(
                            isTriggered = true,
                            message = "거래량 급증: 20일 평균 대비 %.1f배 폭증 (현재: %,d주)".format(ratio, stock.volume),
                            triggeredPrice = currentPrice
                        )
                    }
                }
            }
        }

        return EvaluationResult(isTriggered = false, message = "", triggeredPrice = currentPrice)
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
}
