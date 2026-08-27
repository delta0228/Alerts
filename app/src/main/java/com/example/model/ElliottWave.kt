package com.example.model

enum class WavePhase(
    val title: String,
    val badgeName: String,
    val actionSuggestion: String
) {
    WAVE_1_START("제 1파 시작", "1파 상승", "바닥권 거래량 실린 추세 반등 포착. 분할 매수 고려"),
    WAVE_2_PULLBACK("제 2파 되돌림", "2파 눌림", "1파 저점 지지 확인 후 3파 진입 대비 매수 구간"),
    WAVE_3_IMPULSE("제 3파 강력 상승", "3파 급등", "가장 강력한 주 상승 파동. 적극적 홀딩 및 추세 추종"),
    WAVE_4_CONSOLIDATION("제 4파 기간 조정", "4파 지지", "1파 고점 상단 지지 확인 후 5파 대비 최종 매수 타점"),
    WAVE_5_CLIMAX("제 5파 최종 과열", "5파 고점", "상승 5파 완성 국면. 분할 매도 및 이익 실현 권장"),
    WAVE_A_CORRECTION("조정 A파 하락", "조정 A파", "5파 완성 후 1차 하락 추세. 리스크 관리 필요"),
    WAVE_B_REBOUND("조정 B파 반등", "반등 B파", "하락 중 일시적 기술적 반등(데드캣). 비중 축소 기회"),
    WAVE_C_COMPLETION("조정 C파 바닥 확인", "조정 C파", "조정 완성 임박. 새로운 1파 전환 시점 탐색"),
    CONSOLIDATION("파동 형성 중", "횡보 수렴", "방향성 돌파 및 주요 지지선 확인 대기")
}

data class WavePoint(
    val index: Int,
    val price: Double,
    val label: String, // "0", "1", "2", "3", "4", "5", "A", "B", "C"
    val badgeSymbol: String, // "⓪", "①", "②", "③", "④", "⑤", "Ⓐ", "Ⓑ", "Ⓒ"
    val timestamp: Long,
    val isHigh: Boolean,
    val fibRatioText: String = "",
    val description: String = ""
)

data class WaveDetailItem(
    val waveName: String,
    val startPrice: Double,
    val endPrice: Double,
    val changePercent: Double,
    val fibRatioText: String,
    val description: String
)

/**
 * 1. 절대 불가변 3대 원칙 (필요조건) 검증 결과
 */
data class InviolableRuleCheck(
    val ruleName: String,
    val isSatisfied: Boolean,
    val reason: String,
    val metricValueText: String = ""
)

/**
 * 2. 신뢰도 보조 지침 (충분조건 가이드라인) 검증 결과
 */
data class ReliabilityGuidelineCheck(
    val guidelineName: String,
    val isSatisfied: Boolean,
    val description: String,
    val scoreImpactText: String = ""
)

/**
 * 3. 패턴 유형 (정상 충격파, 리딩/엔딩 다이애거널 예외 패턴 등)
 */
enum class WavePatternType(val displayName: String, val badgeColorHex: Long) {
    STANDARD_IMPULSE("정석 충격파 (Standard Impulse)", 0xFF10B981),
    LEADING_DIAGONAL("리딩 다이애거널 (Leading Diagonal - 1파/A파)", 0xFF06B6D4),
    ENDING_DIAGONAL("엔딩 다이애거널 (Ending Diagonal - 5파/C파)", 0xFFF59E0B),
    CORRECTION_ABC("조정 사이클 (A-B-C Correction)", 0xFF8B5CF6),
    INVALID_STRUCTURE("불가변 원칙 위배 (Invalidated)", 0xFFEF4444),
    EMERGING("파동 진행/형성 중", 0xFF94A3B8)
}

data class ElliottWaveResult(
    val points: List<WavePoint>,
    val currentPhase: WavePhase,
    val isBullishCycle: Boolean,
    val confidenceScore: Int, // 0 ~ 100%
    val patternType: WavePatternType = WavePatternType.STANDARD_IMPULSE,
    val inviolableRules: List<InviolableRuleCheck> = emptyList(),
    val reliabilityGuidelines: List<ReliabilityGuidelineCheck> = emptyList(),
    val isStrictlyValid: Boolean = true,
    val projectedNextPoint: WavePoint? = null,
    val waveDetails: List<WaveDetailItem> = emptyList(),
    val summary: String,
    val strategyTip: String,
    val supportPrice: Double,
    val resistancePrice: Double,
    val targetPrice: Double
)

