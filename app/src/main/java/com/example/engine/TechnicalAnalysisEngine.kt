package com.example.engine

import com.example.model.Candle
import com.example.model.CalculatedCandle
import com.example.model.IndicatorValues
import kotlin.math.pow
import kotlin.math.sqrt

object TechnicalAnalysisEngine {

    /**
     * Calculates all technical indicators for a given series of historical candles.
     */
    fun calculateIndicators(candles: List<Candle>): List<CalculatedCandle> {
        if (candles.isEmpty()) return emptyList()

        val closes = candles.map { it.close }
        val volumes = candles.map { it.volume.toDouble() }

        // Moving Averages
        val ma5 = calculateSMA(closes, 5)
        val ma20 = calculateSMA(closes, 20)
        val ma60 = calculateSMA(closes, 60)
        val ma120 = calculateSMA(closes, 120)

        // Bollinger Bands (20, 2)
        val (bbUpper, bbMiddle, bbLower) = calculateBollingerBands(closes, 20, 2.0)

        // RSI (14)
        val rsi14 = calculateRSI(closes, 14)

        // MACD (12, 26, 9)
        val (macdLine, signalLine, hist) = calculateMACD(closes, 12, 26, 9)

        // Volume MA (20)
        val volumeMa20 = calculateSMA(volumes, 20)

        return candles.mapIndexed { index, candle ->
            CalculatedCandle(
                candle = candle,
                indicators = IndicatorValues(
                    ma5 = ma5.getOrNull(index),
                    ma20 = ma20.getOrNull(index),
                    ma60 = ma60.getOrNull(index),
                    ma120 = ma120.getOrNull(index),
                    bollingerUpper = bbUpper.getOrNull(index),
                    bollingerMiddle = bbMiddle.getOrNull(index),
                    bollingerLower = bbLower.getOrNull(index),
                    rsi14 = rsi14.getOrNull(index),
                    macd = macdLine.getOrNull(index),
                    macdSignal = signalLine.getOrNull(index),
                    macdHist = hist.getOrNull(index),
                    volumeMa20 = volumeMa20.getOrNull(index)
                )
            )
        }
    }

    /**
     * Simple Moving Average (SMA)
     */
    fun calculateSMA(data: List<Double>, period: Int): List<Double?> {
        val result = ArrayList<Double?>(data.size)
        var sum = 0.0

        for (i in data.indices) {
            sum += data[i]
            if (i >= period) {
                sum -= data[i - period]
            }
            if (i >= period - 1) {
                result.add(sum / period)
            } else {
                result.add(null)
            }
        }
        return result
    }

    /**
     * Exponential Moving Average (EMA)
     */
    fun calculateEMA(data: List<Double>, period: Int): List<Double?> {
        val result = ArrayList<Double?>(data.size)
        if (data.isEmpty()) return result

        val multiplier = 2.0 / (period + 1.0)
        var previousEma: Double? = null

        for (i in data.indices) {
            if (i < period - 1) {
                result.add(null)
            } else if (i == period - 1) {
                // First EMA is the SMA of first period elements
                val initialSma = data.take(period).sum() / period
                previousEma = initialSma
                result.add(initialSma)
            } else {
                val currentEma = (data[i] - (previousEma ?: data[i])) * multiplier + (previousEma ?: data[i])
                previousEma = currentEma
                result.add(currentEma)
            }
        }
        return result
    }

    /**
     * Relative Strength Index (RSI 14) with Wilder's Smoothing
     */
    fun calculateRSI(closes: List<Double>, period: Int = 14): List<Double?> {
        val result = ArrayList<Double?>(closes.size)
        if (closes.size <= period) {
            return List(closes.size) { null }
        }

        var avgGain = 0.0
        var avgLoss = 0.0

        // Calculate initial gain / loss
        for (i in 1..period) {
            val change = closes[i] - closes[i - 1]
            if (change > 0) avgGain += change else avgLoss += -change
        }
        avgGain /= period
        avgLoss /= period

        for (i in closes.indices) {
            if (i < period) {
                result.add(null)
            } else if (i == period) {
                val rs = if (avgLoss == 0.0) 100.0 else avgGain / avgLoss
                val rsi = 100.0 - (100.0 / (1.0 + rs))
                result.add(rsi.coerceIn(0.0, 100.0))
            } else {
                val change = closes[i] - closes[i - 1]
                val gain = if (change > 0) change else 0.0
                val loss = if (change < 0) -change else 0.0

                avgGain = (avgGain * (period - 1) + gain) / period
                avgLoss = (avgLoss * (period - 1) + loss) / period

                val rs = if (avgLoss == 0.0) 100.0 else avgGain / avgLoss
                val rsi = 100.0 - (100.0 / (1.0 + rs))
                result.add(rsi.coerceIn(0.0, 100.0))
            }
        }
        return result
    }

    /**
     * Bollinger Bands (Period: 20, Multiplier: 2.0)
     */
    fun calculateBollingerBands(
        closes: List<Double>,
        period: Int = 20,
        multiplier: Double = 2.0
    ): Triple<List<Double?>, List<Double?>, List<Double?>> {
        val sma = calculateSMA(closes, period)
        val upper = ArrayList<Double?>(closes.size)
        val middle = ArrayList<Double?>(closes.size)
        val lower = ArrayList<Double?>(closes.size)

        for (i in closes.indices) {
            val mean = sma[i]
            if (mean == null || i < period - 1) {
                upper.add(null)
                middle.add(null)
                lower.add(null)
            } else {
                var varianceSum = 0.0
                for (j in (i - period + 1)..i) {
                    varianceSum += (closes[j] - mean).pow(2.0)
                }
                val stdDev = sqrt(varianceSum / period)
                middle.add(mean)
                upper.add(mean + multiplier * stdDev)
                lower.add(mean - multiplier * stdDev)
            }
        }
        return Triple(upper, middle, lower)
    }

    /**
     * Moving Average Convergence Divergence (MACD)
     */
    fun calculateMACD(
        closes: List<Double>,
        fastPeriod: Int = 12,
        slowPeriod: Int = 26,
        signalPeriod: Int = 9
    ): Triple<List<Double?>, List<Double?>, List<Double?>> {
        val fastEma = calculateEMA(closes, fastPeriod)
        val slowEma = calculateEMA(closes, slowPeriod)

        val macdLine = ArrayList<Double?>(closes.size)
        for (i in closes.indices) {
            val fast = fastEma[i]
            val slow = slowEma[i]
            if (fast != null && slow != null) {
                macdLine.add(fast - slow)
            } else {
                macdLine.add(null)
            }
        }

        // Calculate Signal line (EMA of MACD line)
        val validMacdStart = macdLine.indexOfFirst { it != null }
        val signalLine = ArrayList<Double?>(closes.size)
        val hist = ArrayList<Double?>(closes.size)

        if (validMacdStart == -1 || closes.size - validMacdStart < signalPeriod) {
            return Triple(macdLine, List(closes.size) { null }, List(closes.size) { null })
        }

        val nonNullMacd = macdLine.filterNotNull()
        val emaOfMacd = calculateEMA(nonNullMacd, signalPeriod)

        var emaIndex = 0
        for (i in closes.indices) {
            if (i < validMacdStart) {
                signalLine.add(null)
                hist.add(null)
            } else {
                val sig = emaOfMacd.getOrNull(emaIndex)
                signalLine.add(sig)
                val macdVal = macdLine[i]
                if (macdVal != null && sig != null) {
                    hist.add(macdVal - sig)
                } else {
                    hist.add(null)
                }
                emaIndex++
            }
        }

        return Triple(macdLine, signalLine, hist)
    }
}
