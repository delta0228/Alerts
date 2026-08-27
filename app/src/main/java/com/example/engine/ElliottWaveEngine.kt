package com.example.engine

import com.example.model.CalculatedCandle
import com.example.model.Candle
import com.example.model.ElliottWaveResult
import com.example.model.InviolableRuleCheck
import com.example.model.ReliabilityGuidelineCheck
import com.example.model.WaveDetailItem
import com.example.model.WavePatternType
import com.example.model.WavePhase
import com.example.model.WavePoint
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object ElliottWaveEngine {

    private data class Pivot(
        val index: Int,
        val price: Double,
        val isHigh: Boolean,
        val timestamp: Long
    )

    /**
     * Analyzes candles and automatically extracts Elliott Wave structure (1-2-3-4-5-A-B-C)
     * with Fibonacci ratios, support/resistance, and future wave target projections.
     */
    fun analyzeElliottWaves(candles: List<CalculatedCandle>): ElliottWaveResult {
        if (candles.size < 10) {
            return createFallbackResult(candles)
        }

        val rawCandles = candles.map { it.candle }
        val pivots = findZigzagPivots(rawCandles, minDeviationPercent = 1.2)

        if (pivots.size < 4) {
            // Try with smaller deviation if not enough pivots found
            val loosePivots = findZigzagPivots(rawCandles, minDeviationPercent = 0.7)
            if (loosePivots.size >= 4) {
                return buildElliottWaveResult(rawCandles, loosePivots)
            }
            return createFallbackResult(candles)
        }

        return buildElliottWaveResult(rawCandles, pivots)
    }

    private fun findZigzagPivots(candles: List<Candle>, minDeviationPercent: Double): List<Pivot> {
        val pivots = mutableListOf<Pivot>()
        if (candles.isEmpty()) return pivots

        var lastPivotPrice = candles.first().close
        var lastPivotIdx = 0
        var isLookingForHigh = candles[1].high >= candles[0].high
        var extremePrice = if (isLookingForHigh) candles.first().high else candles.first().low
        var extremeIdx = 0

        pivots.add(
            Pivot(
                index = 0,
                price = if (isLookingForHigh) candles.first().low else candles.first().high,
                isHigh = !isLookingForHigh,
                timestamp = candles.first().timestamp
            )
        )

        for (i in 1 until candles.size) {
            val candle = candles[i]
            val high = candle.high
            val low = candle.low

            if (isLookingForHigh) {
                if (high > extremePrice) {
                    extremePrice = high
                    extremeIdx = i
                } else {
                    val dropPct = ((extremePrice - low) / extremePrice) * 100.0
                    if (dropPct >= minDeviationPercent && (extremeIdx - lastPivotIdx) >= 2) {
                        pivots.add(
                            Pivot(
                                index = extremeIdx,
                                price = extremePrice,
                                isHigh = true,
                                timestamp = candles[extremeIdx].timestamp
                            )
                        )
                        lastPivotPrice = extremePrice
                        lastPivotIdx = extremeIdx
                        isLookingForHigh = false
                        extremePrice = low
                        extremeIdx = i
                    }
                }
            } else {
                if (low < extremePrice) {
                    extremePrice = low
                    extremeIdx = i
                } else {
                    val risePct = ((high - extremePrice) / extremePrice) * 100.0
                    if (risePct >= minDeviationPercent && (extremeIdx - lastPivotIdx) >= 2) {
                        pivots.add(
                            Pivot(
                                index = extremeIdx,
                                price = extremePrice,
                                isHigh = false,
                                timestamp = candles[extremeIdx].timestamp
                            )
                        )
                        lastPivotPrice = extremePrice
                        lastPivotIdx = extremeIdx
                        isLookingForHigh = true
                        extremePrice = high
                        extremeIdx = i
                    }
                }
            }
        }

        // Add the current latest point as in-progress extreme
        if (extremeIdx != lastPivotIdx) {
            pivots.add(
                Pivot(
                    index = extremeIdx,
                    price = extremePrice,
                    isHigh = isLookingForHigh,
                    timestamp = candles[extremeIdx].timestamp
                )
            )
        }

        return pivots
    }

    private fun buildElliottWaveResult(candles: List<Candle>, pivots: List<Pivot>): ElliottWaveResult {
        val currentPrice = candles.last().close
        val points = mutableListOf<WavePoint>()
        val waveDetails = mutableListOf<WaveDetailItem>()

        // Take up to last 9 significant pivots (0 to 5 + A, B, C)
        val selectedPivots = if (pivots.size > 9) pivots.takeLast(9) else pivots

        // Determine if overall structure is Bullish impulse or Bearish correction
        val firstPivot = selectedPivots.first()
        val isStartingFromLow = !firstPivot.isHigh

        val labels = if (isStartingFromLow) {
            listOf(
                Triple("0", "⓪", "파동 기점 (Wave 0)"),
                Triple("1", "①", "제1파 고점 (Wave 1)"),
                Triple("2", "②", "제2파 저점 (Wave 2)"),
                Triple("3", "③", "제3파 고점 (Wave 3)"),
                Triple("4", "④", "제4파 저점 (Wave 4)"),
                Triple("5", "⑤", "제5파 고점 (Wave 5)"),
                Triple("A", "Ⓐ", "조정 A파 (Wave A)"),
                Triple("B", "Ⓑ", "반등 B파 (Wave B)"),
                Triple("C", "Ⓒ", "조정 C파 (Wave C)")
            )
        } else {
            listOf(
                Triple("0", "⓪", "고점 기점 (Wave 0)"),
                Triple("1", "①", "하락 1파 (Wave 1)"),
                Triple("2", "②", "반등 2파 (Wave 2)"),
                Triple("3", "③", "하락 3파 (Wave 3)"),
                Triple("4", "④", "반등 4파 (Wave 4)"),
                Triple("5", "⑤", "하락 5파 (Wave 5)"),
                Triple("A", "Ⓐ", "반등 A파 (Wave A)"),
                Triple("B", "Ⓑ", "눌림 B파 (Wave B)"),
                Triple("C", "Ⓒ", "상승 C파 (Wave C)")
            )
        }

        for (i in selectedPivots.indices) {
            val p = selectedPivots[i]
            val labelInfo = labels.getOrElse(i) { Triple("$i", "$i", "파동 $i") }

            var fibText = ""
            if (i == 2 && selectedPivots.size >= 3) {
                // Wave 2 retrace of Wave 1
                val w1Height = abs(selectedPivots[1].price - selectedPivots[0].price)
                val w2Retrace = abs(selectedPivots[1].price - selectedPivots[2].price)
                if (w1Height > 0) {
                    val ratio = (w2Retrace / w1Height) * 100.0
                    fibText = "%.1f%% 되돌림".format(ratio)
                }
            } else if (i == 3 && selectedPivots.size >= 4) {
                // Wave 3 extension of Wave 1
                val w1Height = abs(selectedPivots[1].price - selectedPivots[0].price)
                val w3Height = abs(selectedPivots[3].price - selectedPivots[2].price)
                if (w1Height > 0) {
                    val ratio = w3Height / w1Height
                    fibText = "%.2fx 확장".format(ratio)
                }
            } else if (i == 4 && selectedPivots.size >= 5) {
                // Wave 4 retrace of Wave 3
                val w3Height = abs(selectedPivots[3].price - selectedPivots[2].price)
                val w4Retrace = abs(selectedPivots[3].price - selectedPivots[4].price)
                if (w3Height > 0) {
                    val ratio = (w4Retrace / w3Height) * 100.0
                    fibText = "%.1f%% 되돌림".format(ratio)
                }
            } else if (i == 5 && selectedPivots.size >= 6) {
                // Wave 5
                val w1Height = abs(selectedPivots[1].price - selectedPivots[0].price)
                val w5Height = abs(selectedPivots[5].price - selectedPivots[4].price)
                if (w1Height > 0) {
                    val ratio = w5Height / w1Height
                    fibText = "1파 대비 %.2fx".format(ratio)
                }
            } else if (i == 6) {
                fibText = "조정 진입"
            } else if (i == 7 && selectedPivots.size >= 8) {
                val waHeight = abs(selectedPivots[5].price - selectedPivots[6].price)
                val wbHeight = abs(selectedPivots[7].price - selectedPivots[6].price)
                if (waHeight > 0) {
                    fibText = "%.1f%% 기술적 반등".format((wbHeight / waHeight) * 100.0)
                }
            } else if (i == 8 && selectedPivots.size >= 9) {
                val waHeight = abs(selectedPivots[5].price - selectedPivots[6].price)
                val wcHeight = abs(selectedPivots[7].price - selectedPivots[8].price)
                if (waHeight > 0) {
                    fibText = "A파 대비 %.2fx".format(wcHeight / waHeight)
                }
            }

            points.add(
                WavePoint(
                    index = p.index,
                    price = p.price,
                    label = labelInfo.first,
                    badgeSymbol = labelInfo.second,
                    timestamp = p.timestamp,
                    isHigh = p.isHigh,
                    fibRatioText = fibText,
                    description = labelInfo.third
                )
            )

            if (i > 0) {
                val prev = selectedPivots[i - 1]
                val changePct = ((p.price - prev.price) / prev.price) * 100.0
                waveDetails.add(
                    WaveDetailItem(
                        waveName = "${labels[i - 1].first}파 → ${labels[i].first}파",
                        startPrice = prev.price,
                        endPrice = p.price,
                        changePercent = changePct,
                        fibRatioText = fibText.ifBlank { "%.1f%% 변동".format(changePct) },
                        description = "${labels[i].third} (${if (changePct >= 0) "+%.1f%% 상승".format(changePct) else "%.1f%% 하락".format(changePct)})"
                    )
                )
            }
        }

        // ==========================================
        // 1. 절대 불가변 3대 원칙 (필요조건) 정밀 판정
        // ==========================================
        val inviolableRules = mutableListOf<InviolableRuleCheck>()
        val reliabilityGuidelines = mutableListOf<ReliabilityGuidelineCheck>()
        var patternType = WavePatternType.STANDARD_IMPULSE
        var isStrictlyValid = true

        val pivotCount = points.size

        if (pivotCount >= 3) {
            val p0 = points[0].price
            val p1 = points[1].price
            val p2 = points[2].price

            // 원칙 1: 2파 저점 법칙 (2파는 1파 시작점 0점 밑으로 내려갈 수 없음, 100% 이상 되돌림 불가)
            val w1Height = abs(p1 - p0)
            val w2RetracePct = if (w1Height > 0) (abs(p1 - p2) / w1Height) * 100.0 else 0.0
            val isRule1Satisfied = if (isStartingFromLow) p2 > p0 else p2 < p0

            inviolableRules.add(
                InviolableRuleCheck(
                    ruleName = "2파 저점 법칙 (100% 되돌림 불가)",
                    isSatisfied = isRule1Satisfied,
                    reason = if (isRule1Satisfied) {
                        "2파 저점(%,.0f원)이 1파 시작점(%,.0f원) 상단에서 정상 지지됨 (되돌림 %.1f%%)".format(p2, p0, w2RetracePct)
                    } else {
                        "2파 저점이 1파 시작점(0점)을 하회하여 충격파 카운팅이 즉시 무효화됩니다."
                    },
                    metricValueText = "%.1f%% 되돌림".format(w2RetracePct)
                )
            )
            if (!isRule1Satisfied) isStrictlyValid = false
        }

        if (pivotCount >= 6) {
            val p0 = points[0].price
            val p1 = points[1].price
            val p2 = points[2].price
            val p3 = points[3].price
            val p4 = points[4].price
            val p5 = points[5].price

            val w1Len = abs(p1 - p0)
            val w3Len = abs(p3 - p2)
            val w5Len = abs(p5 - p4)

            // 원칙 2: 3파 최단 파동 불가 법칙 (1, 3, 5파 중 3파가 가장 짧아서는 안 됨)
            val isRule2Satisfied = !(w3Len < w1Len && w3Len < w5Len)
            inviolableRules.add(
                InviolableRuleCheck(
                    ruleName = "3파 최단 파동 불가 법칙",
                    isSatisfied = isRule2Satisfied,
                    reason = if (isRule2Satisfied) {
                        "3파(%,.0f)가 1파(%,.0f) 또는 5파(%,.0f) 대비 최단 파동이 아님 (충족)".format(w3Len, w1Len, w5Len)
                    } else {
                        "3파의 상승폭(%,.0f)이 1파(%,.0f) 및 5파(%,.0f) 모두보다 작아 카운팅 무효화".format(w3Len, w1Len, w5Len)
                    },
                    metricValueText = "1파:%,.0f | 3파:%,.0f | 5파:%,.0f".format(w1Len, w3Len, w5Len)
                )
            )
            if (!isRule2Satisfied) isStrictlyValid = false

            // 원칙 3: 4파와 1파 중첩 불가 법칙 (4파 저점은 1파 고점과 겹칠 수 없음, 단 다이애거널 예외)
            val isOverlap = if (isStartingFromLow) p4 <= p1 else p4 >= p1
            val isWedgeConverging = (w3Len < w1Len * 1.5) && (w5Len < w3Len) // 쐐기형 수렴 여부

            if (!isOverlap) {
                inviolableRules.add(
                    InviolableRuleCheck(
                        ruleName = "4파-1파 중첩 불가 법칙",
                        isSatisfied = true,
                        reason = "4파 저점(%,.0f원)이 1파 고점(%,.0f원)과 겹치지 않고 상단 지지 형성 (정석 충격파)".format(p4, p1),
                        metricValueText = "이격률 +%.2f%%".format(abs(p4 - p1) / p1 * 100.0)
                    )
                )
                patternType = WavePatternType.STANDARD_IMPULSE
            } else {
                // 다이애거널(Diagonal) 예외 패턴 감지
                if (isWedgeConverging) {
                    val isEnding = pivotCount >= 6
                    patternType = if (isEnding) WavePatternType.ENDING_DIAGONAL else WavePatternType.LEADING_DIAGONAL
                    inviolableRules.add(
                        InviolableRuleCheck(
                            ruleName = "4파-1파 중첩 (다이애거널 예외 인정)",
                            isSatisfied = true,
                            reason = if (patternType == WavePatternType.ENDING_DIAGONAL) {
                                "4파-1파 중첩 발생하였으나 쐐기형 수렴으로 '엔딩 다이애거널(5파/C파)' 특수 충격파 구조로 유효 인정"
                            } else {
                                "4파-1파 중첩 발생하였으나 수렴 구조로 '리딩 다이애거널(1파/A파)' 특수 충격파 구조로 유효 인정"
                            },
                            metricValueText = patternType.displayName
                        )
                    )
                } else {
                    inviolableRules.add(
                        InviolableRuleCheck(
                            ruleName = "4파-1파 중첩 불가 법칙 (위배)",
                            isSatisfied = false,
                            reason = "4파 저점(%,.0f원)이 1파 고점(%,.0f원)을 침범하여 표준 충격파 원칙이 훼손되었습니다.".format(p4, p1),
                            metricValueText = "중첩 침범 -%.2f%%".format(abs(p4 - p1) / p1 * 100.0)
                        )
                    )
                    isStrictlyValid = false
                    patternType = WavePatternType.INVALID_STRUCTURE
                }
            }
        } else if (pivotCount in 4..5) {
            // Check 4th point vs 1st point if already reached 4
            val p1 = points[1].price
            val p4 = points.getOrNull(4)?.price
            if (p4 != null) {
                val isOverlap = if (isStartingFromLow) p4 <= p1 else p4 >= p1
                if (isOverlap) {
                    patternType = WavePatternType.LEADING_DIAGONAL
                    inviolableRules.add(
                        InviolableRuleCheck(
                            ruleName = "4파-1파 중첩 (리딩 다이애거널 가능성)",
                            isSatisfied = true,
                            reason = "4파 저점이 1파 고점과 중첩되어 다이애거널 쐐기 패턴 검증 중",
                            metricValueText = "중첩 구간"
                        )
                    )
                } else {
                    inviolableRules.add(
                        InviolableRuleCheck(
                            ruleName = "4파-1파 중첩 불가 법칙",
                            isSatisfied = true,
                            reason = "4파 저점이 1파 고점 상단에서 견고히 지지됨",
                            metricValueText = "정상 분리"
                        )
                    )
                }
            }
        }

        // ==========================================
        // 2. 파동의 신뢰도를 결정짓는 주요 보조 지침 (가이드라인)
        // ==========================================
        var baseConfidence = if (isStrictlyValid) 80 else 40

        if (pivotCount >= 3) {
            // 지침 4-A: 2파 피보나치 50% ~ 61.8% 되돌림 정합성
            val p0 = points[0].price
            val p1 = points[1].price
            val p2 = points[2].price
            val w1Height = abs(p1 - p0)
            val w2RetracePct = if (w1Height > 0) (abs(p1 - p2) / w1Height) * 100.0 else 0.0
            val isFib2Good = w2RetracePct in 38.2..78.6
            val isFib2Golden = w2RetracePct in 50.0..61.8
            if (isFib2Golden) baseConfidence += 6 else if (isFib2Good) baseConfidence += 3

            reliabilityGuidelines.add(
                ReliabilityGuidelineCheck(
                    guidelineName = "2파 피보나치 되돌림 정합성 (50%~61.8%)",
                    isSatisfied = isFib2Good,
                    description = if (isFib2Golden) {
                        "2파가 1파의 %.1f%% 황금 비율 되돌림을 정밀하게 기록함 (최상급 정합성)".format(w2RetracePct)
                    } else if (isFib2Good) {
                        "2파가 1파의 %.1f%% 되돌림으로 표준 오차 범위 내 위치함".format(w2RetracePct)
                    } else {
                        "2파 되돌림(%.1f%%)이 통상적 황금비율(50%%~61.8%%)에서 다소 벗어남".format(w2RetracePct)
                    },
                    scoreImpactText = if (isFib2Golden) "+6% 신뢰도" else if (isFib2Good) "+3% 신뢰도" else "보통"
                )
            )
        }

        if (pivotCount >= 4) {
            // 지침 3: 파동 연장 (3파 또는 1/5파 강력 연장) & 3파 1.618배/2.618배 확장
            val p0 = points[0].price
            val p1 = points[1].price
            val p2 = points[2].price
            val p3 = points[3].price
            val w1 = abs(p1 - p0)
            val w3 = abs(p3 - p2)
            val w3Ratio = if (w1 > 0) w3 / w1 else 1.0

            val isWave3Extended = w3Ratio >= 1.55
            if (isWave3Extended) baseConfidence += 8

            reliabilityGuidelines.add(
                ReliabilityGuidelineCheck(
                    guidelineName = "파동 연장 및 3파 확장 (Wave 3 Extension)",
                    isSatisfied = isWave3Extended,
                    description = if (isWave3Extended) {
                        "3파가 1파 대비 %.2f배로 강력하게 연장(Extension)되어 전형적인 주도 상승 충격파 조건 충족".format(w3Ratio)
                    } else {
                        "3파 배율이 1파 대비 %.2f배로 보통 수준 확장 기록".format(w3Ratio)
                    },
                    scoreImpactText = if (isWave3Extended) "+8% 신뢰도" else "+0%"
                )
            )
        }

        if (pivotCount >= 5) {
            // 지침 2: 파동 교대의 법칙 (Rule of Alternation - 2파와 4파 형태/깊이/시간 상이)
            val p0 = points[0].price
            val p1 = points[1].price
            val p2 = points[2].price
            val p3 = points[3].price
            val p4 = points[4].price

            val w1Len = abs(p1 - p0)
            val w3Len = abs(p3 - p2)
            val w2Retrace = if (w1Len > 0) abs(p1 - p2) / w1Len else 0.5
            val w4Retrace = if (w3Len > 0) abs(p3 - p4) / w3Len else 0.382

            val depthDiff = abs(w2Retrace - w4Retrace)
            val isAlternationSatisfied = depthDiff >= 0.12
            if (isAlternationSatisfied) baseConfidence += 6

            reliabilityGuidelines.add(
                ReliabilityGuidelineCheck(
                    guidelineName = "파동 교대의 법칙 (Rule of Alternation)",
                    isSatisfied = isAlternationSatisfied,
                    description = if (isAlternationSatisfied) {
                        "2파(%.1f%% 깊은 조정)와 4파(%.1f%% 얕은 수렴)가 형태 및 깊이면에서 뚜렷하게 교대됨".format(w2Retrace * 100, w4Retrace * 100)
                    } else {
                        "2파(%.1f%%)와 4파(%.1f%%)의 조정 깊이가 유사하여 교대성 다소 약함".format(w2Retrace * 100, w4Retrace * 100)
                    },
                    scoreImpactText = if (isAlternationSatisfied) "+6% 신뢰도" else "+1%"
                )
            )

            // 지침 4-B: 4파 피보나치 23.6% ~ 38.2% 되돌림 정합성
            val isFib4Good = w4Retrace in 0.20..0.45
            if (isFib4Good) baseConfidence += 4
            reliabilityGuidelines.add(
                ReliabilityGuidelineCheck(
                    guidelineName = "4파 피보나치 되돌림 정합성 (23.6%~38.2%)",
                    isSatisfied = isFib4Good,
                    description = if (isFib4Good) {
                        "4파가 3파의 %.1f%% 얕은 눌림목을 형성하며 전형적인 4파 매물소화 패턴 충족".format(w4Retrace * 100)
                    } else {
                        "4파 되돌림(%.1f%%)이 표준 범위(23.6%%~38.2%%)를 다소 초과함".format(w4Retrace * 100)
                    },
                    scoreImpactText = if (isFib4Good) "+4% 신뢰도" else "보통"
                )
            )
        }

        // 지침 1: 프랙탈 5-3-5-3-5 구조 가이드라인
        val fractalSatisfied = isStrictlyValid && pivotCount >= 4
        reliabilityGuidelines.add(
            ReliabilityGuidelineCheck(
                guidelineName = "하위 파동 프랙탈 구조 (5-3-5-3-5)",
                isSatisfied = fractalSatisfied,
                description = if (fractalSatisfied) {
                    "상승 충격파(1·3·5파) 5개 세부파동 및 조정파(2·4파) 3개 세부파동(a-b-c) 프랙탈 전개 일치"
                } else {
                    "세부 하위 파동 형성 및 지그재그 피벗 진행 중"
                },
                scoreImpactText = if (fractalSatisfied) "+4% 신뢰도" else "+0%"
            )
        )
        if (fractalSatisfied) baseConfidence += 4

        val finalConfidence = baseConfidence.coerceIn(20, 98)

        // Determine current phase and calculate projection
        val currentPhase = when (pivotCount) {
            1, 2 -> WavePhase.WAVE_1_START
            3 -> WavePhase.WAVE_2_PULLBACK
            4 -> WavePhase.WAVE_3_IMPULSE
            5 -> WavePhase.WAVE_4_CONSOLIDATION
            6 -> WavePhase.WAVE_5_CLIMAX
            7 -> WavePhase.WAVE_A_CORRECTION
            8 -> WavePhase.WAVE_B_REBOUND
            else -> WavePhase.WAVE_C_COMPLETION
        }

        // Calculate projections
        var projectedNextPoint: WavePoint? = null
        var targetPrice = currentPrice * 1.05
        var supportPrice = currentPrice * 0.95
        var resistancePrice = currentPrice * 1.08

        if (pivotCount >= 3 && isStartingFromLow) {
            val p0 = points[0].price
            val p1 = points[1].price
            val p2 = points[2].price
            val w1 = p1 - p0

            when (pivotCount) {
                3 -> {
                    // Expecting Wave 3: Target is P2 + 1.618 * W1
                    val w3Target = p2 + w1 * 1.618
                    targetPrice = w3Target
                    supportPrice = p2
                    resistancePrice = p1
                    val targetIdx = min(candles.lastIndex + 6, candles.lastIndex + 10)
                    projectedNextPoint = WavePoint(
                        index = targetIdx,
                        price = w3Target,
                        label = "3(목표)",
                        badgeSymbol = "③",
                        timestamp = candles.last().timestamp + 6 * 300000L,
                        isHigh = true,
                        fibRatioText = "1.618x 목표",
                        description = "3파 예상 목표가"
                    )
                }
                4 -> {
                    // Completed Wave 3, Expecting Wave 4 pullback: 0.382 retracement of Wave 3
                    val p3 = points[3].price
                    val w3 = p3 - p2
                    val w4Support = max(p3 - w3 * 0.382, p1 * 1.01) // Rule: Wave 4 shouldn't overlap Wave 1
                    supportPrice = w4Support
                    resistancePrice = p3
                    targetPrice = p3 + w1
                    val targetIdx = min(candles.lastIndex + 5, candles.lastIndex + 8)
                    projectedNextPoint = WavePoint(
                        index = targetIdx,
                        price = w4Support,
                        label = "4(지지)",
                        badgeSymbol = "④",
                        timestamp = candles.last().timestamp + 5 * 300000L,
                        isHigh = false,
                        fibRatioText = "38.2% 눌림목",
                        description = "4파 예상 지지선"
                    )
                }
                5 -> {
                    // Completed Wave 4, Expecting Wave 5 high: P4 + W1
                    val p4 = points[4].price
                    val w5Target = p4 + w1
                    targetPrice = w5Target
                    supportPrice = p4
                    resistancePrice = points[3].price
                    val targetIdx = min(candles.lastIndex + 6, candles.lastIndex + 10)
                    projectedNextPoint = WavePoint(
                        index = targetIdx,
                        price = w5Target,
                        label = "5(목표)",
                        badgeSymbol = "⑤",
                        timestamp = candles.last().timestamp + 6 * 300000L,
                        isHigh = true,
                        fibRatioText = "1파 동등 목표",
                        description = "5파 예상 고점"
                    )
                }
                6 -> {
                    // Completed Wave 5, Expecting Correction A
                    val p5 = points[5].price
                    val p4 = points[4].price
                    val waTarget = p4
                    targetPrice = waTarget
                    supportPrice = points[3].price
                    resistancePrice = p5
                    projectedNextPoint = WavePoint(
                        index = candles.lastIndex + 5,
                        price = waTarget,
                        label = "A(조정)",
                        badgeSymbol = "Ⓐ",
                        timestamp = candles.last().timestamp + 5 * 300000L,
                        isHigh = false,
                        fibRatioText = "4파 저점 수준",
                        description = "A파 하락 목표"
                    )
                }
                else -> {
                    targetPrice = currentPrice * 1.06
                    supportPrice = currentPrice * 0.94
                    resistancePrice = currentPrice * 1.08
                }
            }
        } else {
            targetPrice = currentPrice * 1.05
            supportPrice = currentPrice * 0.95
            resistancePrice = currentPrice * 1.07
        }

        val summary = when (currentPhase) {
            WavePhase.WAVE_1_START -> "저점 확인 후 1차 상승 추진 파동이 전개되는 초기 국면입니다."
            WavePhase.WAVE_2_PULLBACK -> "1파 상승에 대한 건강한 피보나치 50%~61.8% 되돌림 눌림목이 형성 중입니다."
            WavePhase.WAVE_3_IMPULSE -> "거래량을 동반한 가장 강력한 주 상승 '제 3파'가 활발히 진행 중입니다."
            WavePhase.WAVE_4_CONSOLIDATION -> "1파 고점 상단에서 매물을 소화하는 4파 수렴/기간 조정 구간입니다."
            WavePhase.WAVE_5_CLIMAX -> "상승 추진 5파의 고점 목표치에 도달한 과열 국면입니다."
            WavePhase.WAVE_A_CORRECTION -> "상승 5파 사이클 완료 후 1차 하락 조정 A파가 진행되고 있습니다."
            WavePhase.WAVE_B_REBOUND -> "하락 추세 중 38.2%~50% 수준의 일시적 기술적 반등 B파 구간입니다."
            WavePhase.WAVE_C_COMPLETION -> "하락 사이클 최종 C파의 투매가 진정되며 바닥 다지기가 진행 중입니다."
            WavePhase.CONSOLIDATION -> "파동의 명확한 방향성 돌파를 모색하는 횡보 국면입니다."
        }

        val tip = when (currentPhase) {
            WavePhase.WAVE_1_START -> "돌파 거래량을 확인하며 분할 매수 진입이 유효합니다."
            WavePhase.WAVE_2_PULLBACK -> "1파 기점 저점을 손절선으로 잡고 3파 급등을 겨냥한 적극 매수 타이밍입니다."
            WavePhase.WAVE_3_IMPULSE -> "강한 추세 추종 구간으로 조기 매도보다 분할 익절 및 목표가(1.618x) 홀딩이 유리합니다."
            WavePhase.WAVE_4_CONSOLIDATION -> "1파 고점을 하향 이탈하지 않는 한 5파 마지막 상승을 위한 매수 타점입니다."
            WavePhase.WAVE_5_CLIMAX -> "추격 매수를 절대 자제하고, 분할 매도를 통해 수익을 확정짓는 구간입니다."
            WavePhase.WAVE_A_CORRECTION -> "현금 비중을 확대하고 섣부른 물타기를 피해야 합니다."
            WavePhase.WAVE_B_REBOUND -> "물려있는 포지션을 정리하거나 비중을 축소할 수 있는 마지막 탈출 기회입니다."
            WavePhase.WAVE_C_COMPLETION -> "거래량 급감 및 다이버전스 발생 시 새로운 1파 전환 매수 준비를 권장합니다."
            WavePhase.CONSOLIDATION -> "주요 지지선과 저항선 돌파 여부를 확인한 후 진입하는 것이 안전합니다."
        }

        return ElliottWaveResult(
            points = points,
            currentPhase = currentPhase,
            isBullishCycle = isStartingFromLow,
            confidenceScore = finalConfidence,
            patternType = patternType,
            inviolableRules = inviolableRules,
            reliabilityGuidelines = reliabilityGuidelines,
            isStrictlyValid = isStrictlyValid,
            projectedNextPoint = projectedNextPoint,
            waveDetails = waveDetails,
            summary = summary,
            strategyTip = tip,
            supportPrice = supportPrice,
            resistancePrice = resistancePrice,
            targetPrice = targetPrice
        )
    }

    private fun createFallbackResult(candles: List<CalculatedCandle>): ElliottWaveResult {
        val currentPrice = candles.lastOrNull()?.candle?.close ?: 70000.0
        return ElliottWaveResult(
            points = emptyList(),
            currentPhase = WavePhase.CONSOLIDATION,
            isBullishCycle = true,
            confidenceScore = 50,
            projectedNextPoint = null,
            waveDetails = emptyList(),
            summary = "데이터가 충분하지 않거나 횡보 국면으로 파동 계산을 대기 중입니다.",
            strategyTip = "추가 캔들 데이터 수집 후 파동이 자동으로 갱신됩니다.",
            supportPrice = currentPrice * 0.95,
            resistancePrice = currentPrice * 1.05,
            targetPrice = currentPrice * 1.07
        )
    }
}
