package com.example.model

data class IndicatorValues(
    val ma5: Double? = null,
    val ma20: Double? = null,
    val ma60: Double? = null,
    val ma120: Double? = null,
    val bollingerUpper: Double? = null,
    val bollingerMiddle: Double? = null,
    val bollingerLower: Double? = null,
    val rsi14: Double? = null,
    val macd: Double? = null,
    val macdSignal: Double? = null,
    val macdHist: Double? = null,
    val volumeMa20: Double? = null
)

data class CalculatedCandle(
    val candle: Candle,
    val indicators: IndicatorValues
)
