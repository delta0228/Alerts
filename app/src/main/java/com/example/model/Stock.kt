package com.example.model

enum class MarketType(val displayName: String) {
    ALL("전체"),
    KOSPI("코스피"),
    KOSDAQ("코스닥"),
    US("해외(US)")
}

data class Stock(
    val symbol: String,
    val name: String,
    val market: MarketType,
    val currentPrice: Double,
    val changePrice: Double,
    val changeRate: Double,
    val openPrice: Double,
    val highPrice: Double,
    val lowPrice: Double,
    val prevClosePrice: Double,
    val volume: Long,
    val tradingValue: Long, // 거래대금
    val high52w: Double = currentPrice * 1.35,
    val low52w: Double = currentPrice * 0.72,
    val per: Double = 12.4,
    val pbr: Double = 1.15,
    val marketCap: Long = (currentPrice * 5969782550).toLong(), // 시가총액
    val isFavorite: Boolean = false
) {
    val isRising: Boolean get() = changeRate > 0
    val isFalling: Boolean get() = changeRate < 0
    val isFlat: Boolean get() = changeRate == 0.0
}

data class Candle(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long
) {
    val isBullish: Boolean get() = close >= open
}

enum class ChartTimeframe(val label: String, val minutes: Int) {
    M1("1분", 1),
    M5("5분", 5),
    M15("15분", 15),
    H1("1시간", 60),
    DAILY("일봉", 1440),
    WEEKLY("주봉", 10080)
}
