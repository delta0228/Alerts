package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CalculatedCandle
import com.example.model.ChartTimeframe
import com.example.model.Stock
import com.example.ui.theme.IndicatorBollinger
import com.example.ui.theme.IndicatorMA120
import com.example.ui.theme.IndicatorMA20
import com.example.ui.theme.IndicatorMA5
import com.example.ui.theme.IndicatorMA60
import com.example.ui.theme.IndicatorMACD
import com.example.ui.theme.IndicatorRSI
import com.example.ui.theme.IndicatorSignal
import com.example.ui.theme.StockBlue
import com.example.ui.theme.StockRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

enum class SubIndicatorType(val label: String) {
    VOLUME("거래량"),
    RSI("RSI"),
    MACD("MACD")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CandlestickChart(
    stock: Stock,
    candles: List<CalculatedCandle>,
    selectedTimeframe: ChartTimeframe,
    onTimeframeSelected: (ChartTimeframe) -> Unit,
    modifier: Modifier = Modifier
) {
    var showMA by remember { mutableStateOf(true) }
    var showBollinger by remember { mutableStateOf(true) }
    var selectedSubIndicator by remember { mutableStateOf(SubIndicatorType.VOLUME) }
    var scrubIndex by remember { mutableStateOf<Int?>(null) }

    val visibleCandles = remember(candles) { candles.takeLast(50) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(12.dp)
            .testTag("candlestick_chart_container")
    ) {
        // Timeframe selector row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ChartTimeframe.values().forEach { tf ->
                    val isSelected = tf == selectedTimeframe
                    Surface(
                        onClick = { onTimeframeSelected(tf) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.testTag("tf_button_${tf.name}")
                    ) {
                        Text(
                            text = tf.label,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Sub Indicator selector
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                SubIndicatorType.values().forEach { sub ->
                    val isSelected = sub == selectedSubIndicator
                    Surface(
                        onClick = { selectedSubIndicator = sub },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                        modifier = Modifier.testTag("sub_indicator_${sub.name}")
                    ) {
                        Text(
                            text = sub.label,
                            color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Indicator Toggle Chips & Legend
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            FilterChip(
                selected = showMA,
                onClick = { showMA = !showMA },
                label = { Text("이평선 (5/20/60)", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.height(28.dp)
            )
            FilterChip(
                selected = showBollinger,
                onClick = { showBollinger = !showBollinger },
                label = { Text("볼린저밴드 (20,2)", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.height(28.dp)
            )

            // Dynamic MA values display
            if (showMA && visibleCandles.isNotEmpty()) {
                val lastCandle = scrubIndex?.let { visibleCandles.getOrNull(it) } ?: visibleCandles.last()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    lastCandle.indicators.ma5?.let {
                        Text("MA5: %,.0f".format(it), color = IndicatorMA5, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    lastCandle.indicators.ma20?.let {
                        Text("MA20: %,.0f".format(it), color = IndicatorMA20, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    lastCandle.indicators.ma60?.let {
                        Text("MA60: %,.0f".format(it), color = IndicatorMA60, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Scrub Info Tooltip Badge
        if (scrubIndex != null && scrubIndex!! in visibleCandles.indices) {
            val scrubbed = visibleCandles[scrubIndex!!]
            val dateStr = SimpleDateFormat("MM/dd HH:mm", Locale.KOREA).format(Date(scrubbed.candle.timestamp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(dateStr, color = TextSecondary, fontSize = 11.sp)
                    Text("시: %,.0f".format(scrubbed.candle.open), color = TextPrimary, fontSize = 11.sp)
                    Text("고: %,.0f".format(scrubbed.candle.high), color = StockRed, fontSize = 11.sp)
                    Text("저: %,.0f".format(scrubbed.candle.low), color = StockBlue, fontSize = 11.sp)
                    Text("종: %,.0f".format(scrubbed.candle.close), color = if (scrubbed.candle.isBullish) StockRed else StockBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("량: %,d".format(scrubbed.candle.volume), color = TextSecondary, fontSize = 11.sp)
                }
            }
        }

        // Main Chart Canvas (Candlestick + Overlays) & Sub-chart Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(310.dp)
                .pointerInput(visibleCandles) {
                    detectTapGestures(
                        onTap = { offset ->
                            val candleWidth = size.width / visibleCandles.size
                            val idx = (offset.x / candleWidth).toInt().coerceIn(0, visibleCandles.lastIndex)
                            scrubIndex = idx
                        }
                    )
                }
                .pointerInput(visibleCandles) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val candleWidth = size.width / visibleCandles.size
                            val idx = (offset.x / candleWidth).toInt().coerceIn(0, visibleCandles.lastIndex)
                            scrubIndex = idx
                        },
                        onDragEnd = { scrubIndex = null },
                        onDragCancel = { scrubIndex = null },
                        onDrag = { change, _ ->
                            val candleWidth = size.width / visibleCandles.size
                            val idx = (change.position.x / candleWidth).toInt().coerceIn(0, visibleCandles.lastIndex)
                            scrubIndex = idx
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (visibleCandles.isEmpty()) return@Canvas

                val mainHeight = size.height * 0.70f
                val subHeight = size.height * 0.28f
                val subTop = size.height * 0.72f

                drawMainCandlestickChart(
                    candles = visibleCandles,
                    width = size.width,
                    height = mainHeight,
                    showMA = showMA,
                    showBollinger = showBollinger,
                    scrubIndex = scrubIndex
                )

                // Sub indicator chart
                when (selectedSubIndicator) {
                    SubIndicatorType.VOLUME -> {
                        drawVolumeSubChart(
                            candles = visibleCandles,
                            width = size.width,
                            top = subTop,
                            height = subHeight,
                            scrubIndex = scrubIndex
                        )
                    }
                    SubIndicatorType.RSI -> {
                        drawRsiSubChart(
                            candles = visibleCandles,
                            width = size.width,
                            top = subTop,
                            height = subHeight,
                            scrubIndex = scrubIndex
                        )
                    }
                    SubIndicatorType.MACD -> {
                        drawMacdSubChart(
                            candles = visibleCandles,
                            width = size.width,
                            top = subTop,
                            height = subHeight,
                            scrubIndex = scrubIndex
                        )
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawMainCandlestickChart(
    candles: List<CalculatedCandle>,
    width: Float,
    height: Float,
    showMA: Boolean,
    showBollinger: Boolean,
    scrubIndex: Int?
) {
    var minPrice = candles.minOf { it.candle.low }
    var maxPrice = candles.maxOf { it.candle.high }

    if (showBollinger) {
        candles.forEach { c ->
            c.indicators.bollingerLower?.let { minPrice = min(minPrice, it) }
            c.indicators.bollingerUpper?.let { maxPrice = max(maxPrice, it) }
        }
    }

    val priceRange = if (maxPrice - minPrice > 0) maxPrice - minPrice else 1.0
    val candleWidth = width / candles.size
    val bodyWidth = candleWidth * 0.68f

    fun priceToY(price: Double): Float {
        return (height - ((price - minPrice) / priceRange * (height - 20f)) - 10f).toFloat()
    }

    // Grid lines & price labels
    val gridLines = 4
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
    for (i in 0..gridLines) {
        val y = height * (i.toFloat() / gridLines)
        val p = maxPrice - (i.toDouble() / gridLines * priceRange)
        drawLine(
            color = Color(0x2294A3B8),
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 1f,
            pathEffect = dashEffect
        )
    }

    // Draw Bollinger Bands
    if (showBollinger) {
        val upperPath = Path()
        val lowerPath = Path()
        val fillPath = Path()

        var firstValid = true
        val upperPoints = mutableListOf<Offset>()
        val lowerPoints = mutableListOf<Offset>()

        candles.forEachIndexed { i, c ->
            val x = i * candleWidth + candleWidth / 2f
            val upper = c.indicators.bollingerUpper
            val lower = c.indicators.bollingerLower
            if (upper != null && lower != null) {
                val uy = priceToY(upper)
                val ly = priceToY(lower)
                upperPoints.add(Offset(x, uy))
                lowerPoints.add(Offset(x, ly))

                if (firstValid) {
                    upperPath.moveTo(x, uy)
                    lowerPath.moveTo(x, ly)
                    firstValid = false
                } else {
                    upperPath.lineTo(x, uy)
                    lowerPath.lineTo(x, ly)
                }
            }
        }

        if (upperPoints.isNotEmpty() && lowerPoints.isNotEmpty()) {
            fillPath.moveTo(upperPoints.first().x, upperPoints.first().y)
            upperPoints.forEach { fillPath.lineTo(it.x, it.y) }
            lowerPoints.reversed().forEach { fillPath.lineTo(it.x, it.y) }
            fillPath.close()

            drawPath(path = fillPath, color = IndicatorBollinger.copy(alpha = 0.08f))
            drawPath(path = upperPath, color = IndicatorBollinger.copy(alpha = 0.6f), style = Stroke(width = 1.5f))
            drawPath(path = lowerPath, color = IndicatorBollinger.copy(alpha = 0.6f), style = Stroke(width = 1.5f))
        }
    }

    // Draw Candlesticks
    candles.forEachIndexed { i, c ->
        val candle = c.candle
        val x = i * candleWidth + candleWidth / 2f
        val isBull = candle.isBullish
        val candleColor = if (isBull) StockRed else StockBlue

        val openY = priceToY(candle.open)
        val closeY = priceToY(candle.close)
        val highY = priceToY(candle.high)
        val lowY = priceToY(candle.low)

        // Wick
        drawLine(
            color = candleColor,
            start = Offset(x, highY),
            end = Offset(x, lowY),
            strokeWidth = 1.5f
        )

        // Body
        val topY = min(openY, closeY)
        val bodyHeight = max(kotlin.math.abs(openY - closeY), 2f)
        drawRect(
            color = candleColor,
            topLeft = Offset(x - bodyWidth / 2f, topY),
            size = Size(bodyWidth, bodyHeight)
        )
    }

    // Draw Moving Averages
    if (showMA) {
        drawIndicatorLine(candles, candleWidth, { it.indicators.ma5 }, IndicatorMA5, 1.8f) { priceToY(it) }
        drawIndicatorLine(candles, candleWidth, { it.indicators.ma20 }, IndicatorMA20, 2.0f) { priceToY(it) }
        drawIndicatorLine(candles, candleWidth, { it.indicators.ma60 }, IndicatorMA60, 2.0f) { priceToY(it) }
    }

    // Crosshair scrub
    if (scrubIndex != null && scrubIndex in candles.indices) {
        val scrubX = scrubIndex * candleWidth + candleWidth / 2f
        val candle = candles[scrubIndex].candle
        val closeY = priceToY(candle.close)

        drawLine(
            color = Color.White.copy(alpha = 0.7f),
            start = Offset(scrubX, 0f),
            end = Offset(scrubX, height),
            strokeWidth = 1.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
        )
        drawLine(
            color = Color.White.copy(alpha = 0.7f),
            start = Offset(0f, closeY),
            end = Offset(width, closeY),
            strokeWidth = 1.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
        )
        drawCircle(
            color = if (candle.isBullish) StockRed else StockBlue,
            radius = 4f,
            center = Offset(scrubX, closeY)
        )
    }
}

private fun DrawScope.drawIndicatorLine(
    candles: List<CalculatedCandle>,
    candleWidth: Float,
    extractor: (CalculatedCandle) -> Double?,
    color: Color,
    strokeWidth: Float,
    yCalculator: (Double) -> Float
) {
    val path = Path()
    var isStarted = false

    candles.forEachIndexed { i, c ->
        val value = extractor(c)
        if (value != null) {
            val x = i * candleWidth + candleWidth / 2f
            val y = yCalculator(value)
            if (!isStarted) {
                path.moveTo(x, y)
                isStarted = true
            } else {
                path.lineTo(x, y)
            }
        }
    }

    if (isStarted) {
        drawPath(path = path, color = color, style = Stroke(width = strokeWidth))
    }
}

private fun DrawScope.drawVolumeSubChart(
    candles: List<CalculatedCandle>,
    width: Float,
    top: Float,
    height: Float,
    scrubIndex: Int?
) {
    val maxVol = candles.maxOfOrNull { it.candle.volume }?.toDouble() ?: 1.0
    val candleWidth = width / candles.size
    val bodyWidth = candleWidth * 0.68f

    // Separator line
    drawLine(
        color = Color(0x3394A3B8),
        start = Offset(0f, top),
        end = Offset(width, top),
        strokeWidth = 1f
    )

    candles.forEachIndexed { i, c ->
        val x = i * candleWidth + candleWidth / 2f
        val v = c.candle.volume.toDouble()
        val vHeight = ((v / maxVol) * (height - 10f)).toFloat()
        val vTop = top + height - vHeight
        val col = if (c.candle.isBullish) StockRed.copy(alpha = 0.7f) else StockBlue.copy(alpha = 0.7f)

        drawRect(
            color = col,
            topLeft = Offset(x - bodyWidth / 2f, vTop),
            size = Size(bodyWidth, vHeight)
        )
    }

    // Volume MA20
    drawIndicatorLine(
        candles = candles,
        candleWidth = candleWidth,
        extractor = { it.indicators.volumeMa20 },
        color = Color(0xFFFBBF24),
        strokeWidth = 1.5f,
        yCalculator = { v -> (top + height - ((v / maxVol) * (height - 10f))).toFloat() }
    )
}

private fun DrawScope.drawRsiSubChart(
    candles: List<CalculatedCandle>,
    width: Float,
    top: Float,
    height: Float,
    scrubIndex: Int?
) {
    val candleWidth = width / candles.size

    // Separator
    drawLine(color = Color(0x3394A3B8), start = Offset(0f, top), end = Offset(width, top), strokeWidth = 1f)

    fun rsiToY(rsi: Double): Float {
        return (top + height - (rsi / 100.0 * height)).toFloat()
    }

    // 70 overbought and 30 oversold reference lines
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
    val y70 = rsiToY(70.0)
    val y30 = rsiToY(30.0)

    drawLine(color = StockRed.copy(alpha = 0.5f), start = Offset(0f, y70), end = Offset(width, y70), strokeWidth = 1f, pathEffect = dashEffect)
    drawLine(color = StockBlue.copy(alpha = 0.5f), start = Offset(0f, y30), end = Offset(width, y30), strokeWidth = 1f, pathEffect = dashEffect)

    // Overbought/oversold shaded zone
    drawRect(color = Color(0x0AFFFFFF), topLeft = Offset(0f, y70), size = Size(width, y30 - y70))

    // RSI line
    drawIndicatorLine(
        candles = candles,
        candleWidth = candleWidth,
        extractor = { it.indicators.rsi14 },
        color = IndicatorRSI,
        strokeWidth = 2.0f,
        yCalculator = { rsiToY(it) }
    )
}

private fun DrawScope.drawMacdSubChart(
    candles: List<CalculatedCandle>,
    width: Float,
    top: Float,
    height: Float,
    scrubIndex: Int?
) {
    val candleWidth = width / candles.size
    val bodyWidth = candleWidth * 0.5f

    // Separator
    drawLine(color = Color(0x3394A3B8), start = Offset(0f, top), end = Offset(width, top), strokeWidth = 1f)

    var maxAbs = 1.0
    candles.forEach { c ->
        c.indicators.macd?.let { maxAbs = max(maxAbs, kotlin.math.abs(it)) }
        c.indicators.macdSignal?.let { maxAbs = max(maxAbs, kotlin.math.abs(it)) }
        c.indicators.macdHist?.let { maxAbs = max(maxAbs, kotlin.math.abs(it)) }
    }

    val midY = top + height / 2f
    // Zero reference line
    drawLine(color = Color(0x4494A3B8), start = Offset(0f, midY), end = Offset(width, midY), strokeWidth = 1f)

    fun macdToY(v: Double): Float {
        return (midY - (v / maxAbs * (height / 2f - 4f))).toFloat()
    }

    // Histograms
    candles.forEachIndexed { i, c ->
        val hist = c.indicators.macdHist
        if (hist != null) {
            val x = i * candleWidth + candleWidth / 2f
            val hY = macdToY(hist)
            val isPositive = hist >= 0
            val hColor = if (isPositive) StockRed.copy(alpha = 0.8f) else StockBlue.copy(alpha = 0.8f)

            val bTop = min(midY, hY)
            val bHeight = max(kotlin.math.abs(midY - hY), 1f)
            drawRect(
                color = hColor,
                topLeft = Offset(x - bodyWidth / 2f, bTop),
                size = Size(bodyWidth, bHeight)
            )
        }
    }

    // MACD line & Signal line
    drawIndicatorLine(candles, candleWidth, { it.indicators.macd }, IndicatorMACD, 1.8f) { macdToY(it) }
    drawIndicatorLine(candles, candleWidth, { it.indicators.macdSignal }, IndicatorSignal, 1.8f) { macdToY(it) }
}
