package com.example

import com.example.engine.RuleEvaluationEngine
import com.example.engine.TechnicalAnalysisEngine
import com.example.model.AlertRule
import com.example.model.Candle
import com.example.model.ChartTimeframe
import com.example.model.MarketType
import com.example.model.RuleCategory
import com.example.model.RuleScope
import com.example.model.RuleType
import com.example.model.Stock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StockAlertEngineTest {

    @Test
    fun testSmaCalculation() {
        val prices = listOf(10.0, 20.0, 30.0, 40.0, 50.0)
        val sma = TechnicalAnalysisEngine.calculateSMA(prices, 5)
        assertEquals(30.0, sma!!, 0.001)
    }

    @Test
    fun testRsiCalculation() {
        val prices = listOf(
            44.34, 44.09, 44.15, 43.61, 44.33, 44.83, 45.10,
            45.42, 45.84, 46.08, 45.89, 46.03, 45.61, 46.28, 46.28
        )
        val rsi = TechnicalAnalysisEngine.calculateRSI(prices, 14)
        assertNotNull(rsi)
        assertTrue(rsi!! > 0 && rsi < 100)
    }

    @Test
    fun testBollingerBandsCalculation() {
        val prices = (1..20).map { 100.0 + it }
        val bands = TechnicalAnalysisEngine.calculateBollingerBands(prices, 20, 2.0)
        assertNotNull(bands)
        assertTrue(bands!!.upper > bands.middle)
        assertTrue(bands.middle > bands.lower)
    }

    @Test
    fun testMacdCalculation() {
        val prices = (1..35).map { 100.0 + it * 1.5 }
        val macd = TechnicalAnalysisEngine.calculateMACD(prices)
        assertNotNull(macd)
        assertNotNull(macd!!.macd)
        assertNotNull(macd.signal)
    }

    @Test
    fun testPriceAboveRuleEvaluation() {
        val stock = Stock(
            symbol = "005930",
            name = "삼성전자",
            market = MarketType.KOSPI,
            currentPrice = 75000.0,
            previousClose = 70000.0,
            openPrice = 71000.0,
            highPrice = 75500.0,
            lowPrice = 70500.0,
            volume = 15000000
        )

        val rule = AlertRule(
            id = 1,
            name = "목표가 돌파",
            scope = RuleScope.SPECIFIC,
            targetSymbol = "005930",
            timeframe = ChartTimeframe.M5,
            ruleType = RuleType.PRICE_ABOVE,
            thresholdValue = 74000.0
        )

        val candles = TechnicalAnalysisEngine.generateCalculatedCandles(
            listOf(
                Candle(1000, 71000.0, 73000.0, 70500.0, 72000.0, 500000),
                Candle(2000, 72000.0, 75500.0, 71500.0, 75000.0, 600000)
            )
        )

        val result = RuleEvaluationEngine.evaluateRule(rule, stock, candles)
        assertTrue(result.isTriggered)
    }

    @Test
    fun testCooldownSuppression() {
        val now = System.currentTimeMillis()
        val rule = AlertRule(
            id = 1,
            name = "쿨다운 테스트",
            scope = RuleScope.SPECIFIC,
            targetSymbol = "005930",
            timeframe = ChartTimeframe.M5,
            ruleType = RuleType.PRICE_ABOVE,
            thresholdValue = 70000.0,
            cooldownMinutes = 30,
            lastTriggeredAt = now - (10 * 60 * 1000) // Triggered 10 minutes ago, cooldown is 30 mins
        )

        val stock = Stock("005930", "삼성전자", MarketType.KOSPI, 75000.0, 70000.0, 70000.0, 76000.0, 70000.0, 10000)
        val candles = TechnicalAnalysisEngine.generateCalculatedCandles(listOf(Candle(1000, 70000.0, 76000.0, 70000.0, 75000.0, 10000)))

        val result = RuleEvaluationEngine.evaluateRule(rule, stock, candles, now)
        assertFalse(result.isTriggered) // Should be suppressed by cooldown
    }
}
