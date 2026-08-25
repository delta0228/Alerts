package com.example.model

enum class RuleCategory(val title: String, val description: String) {
    PRICE("가격 조건", "특정 가격 돌파 및 목표가 도달"),
    CHANGE_RATE("등락률 조건", "전일 또는 시가 대비 급등락"),
    MOVING_AVERAGE("이동평균선 (이평선)", "골든크로스 / 데드크로스 및 배열"),
    RSI("RSI (상대강도지수)", "과매수(Overbought) 및 과매도(Oversold)"),
    BOLLINGER("볼린저 밴드", "상/하단 밴드 터치 및 돌파"),
    VOLUME("거래량", "직전 평균 대비 거래량 폭증")
}

enum class RuleType(
    val category: RuleCategory,
    val title: String,
    val templateText: String
) {
    PRICE_ABOVE(RuleCategory.PRICE, "목표가 상향 돌파", "현재가가 {value}원 이상 상승 돌파 시"),
    PRICE_BELOW(RuleCategory.PRICE, "목표가 하향 이탈", "현재가가 {value}원 이하로 하향 이탈 시"),
    CHANGE_RATE_SURGE(RuleCategory.CHANGE_RATE, "전일 대비 급등", "전일 대비 +{value}% 이상 상승 시"),
    CHANGE_RATE_PLUNGE(RuleCategory.CHANGE_RATE, "전일 대비 급락", "전일 대비 -{value}% 이하 하락 시"),
    MA_GOLDEN_CROSS(RuleCategory.MOVING_AVERAGE, "골든크로스 (단기>장기)", "{param1}일선이 {param2}일선을 상향 돌파 시"),
    MA_DEAD_CROSS(RuleCategory.MOVING_AVERAGE, "데드크로스 (단기<장기)", "{param1}일선이 {param2}일선을 하향 이탈 시"),
    RSI_OVERSOLD(RuleCategory.RSI, "RSI 과매도 구간 진입", "RSI(14)가 {value} 이하로 과매도 진입 시"),
    RSI_OVERBOUGHT(RuleCategory.RSI, "RSI 과매수 구간 진입", "RSI(14)가 {value} 이상으로 과매수 진입 시"),
    BOLLINGER_LOWER_TOUCH(RuleCategory.BOLLINGER, "볼린저 밴드 하단 터치/반등", "주가가 볼린저 밴드 하단선 이하로 진입 시"),
    BOLLINGER_UPPER_BREAK(RuleCategory.BOLLINGER, "볼린저 밴드 상단 돌파", "주가가 볼린저 밴드 상단선을 상향 돌파 시"),
    VOLUME_SURGE(RuleCategory.VOLUME, "거래량 급증", "현재 거래량이 20일 평균 거래량의 {value}배 이상 급증 시")
}

enum class RuleScope(val displayName: String) {
    SPECIFIC("지정 종목"),
    ALL_KOSPI("코스피 전체"),
    ALL_KOSDAQ("코스닥 전체"),
    ALL_US("미국 주식 전체"),
    FAVORITES("내 관심종목 전체")
}

data class AlertRule(
    val id: Long = 0,
    val name: String,
    val scope: RuleScope = RuleScope.SPECIFIC,
    val targetSymbol: String = "", // e.g. "005930" or empty if whole market
    val targetSymbolName: String = "",
    val timeframe: ChartTimeframe = ChartTimeframe.M5,
    val ruleType: RuleType,
    val thresholdValue: Double = 0.0,
    val param1: Int = 5, // e.g. MA fast period
    val param2: Int = 20, // e.g. MA slow period
    val cooldownMinutes: Int = 30, // 중복 방지 쿨다운
    val isEnabled: Boolean = true,
    val lastTriggeredAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun isCoolingDown(currentTime: Long): Boolean {
        if (lastTriggeredAt <= 0) return false
        val elapsedMs = currentTime - lastTriggeredAt
        return elapsedMs < (cooldownMinutes * 60 * 1000L)
    }

    fun formattedSummary(): String {
        return when (ruleType) {
            RuleType.PRICE_ABOVE -> "현재가 ≥ %,.0f원".format(thresholdValue)
            RuleType.PRICE_BELOW -> "현재가 ≤ %,.0f원".format(thresholdValue)
            RuleType.CHANGE_RATE_SURGE -> "전일 대비 ≥ +%.1f%%".format(thresholdValue)
            RuleType.CHANGE_RATE_PLUNGE -> "전일 대비 ≤ -%.1f%%".format(thresholdValue)
            RuleType.MA_GOLDEN_CROSS -> "이평선 ${param1}일선 > ${param2}일선 골든크로스"
            RuleType.MA_DEAD_CROSS -> "이평선 ${param1}일선 < ${param2}일선 데드크로스"
            RuleType.RSI_OVERSOLD -> "RSI(14) ≤ %.0f (과매도)".format(thresholdValue)
            RuleType.RSI_OVERBOUGHT -> "RSI(14) ≥ %.0f (과매수)".format(thresholdValue)
            RuleType.BOLLINGER_LOWER_TOUCH -> "볼린저 밴드 하단 터치"
            RuleType.BOLLINGER_UPPER_BREAK -> "볼린저 밴드 상단 돌파"
            RuleType.VOLUME_SURGE -> "20일 평균 거래량 대비 %.1f배 폭증".format(thresholdValue)
        }
    }
}

data class AlertHistory(
    val id: Long = 0,
    val ruleId: Long,
    val ruleName: String,
    val ruleType: RuleType,
    val symbol: String,
    val stockName: String,
    val triggeredPrice: Double,
    val changeRate: Double,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
