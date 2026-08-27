package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.ElliottWaveEngine
import com.example.model.CalculatedCandle
import com.example.model.ChartTimeframe
import com.example.model.ElliottWaveResult
import com.example.model.InviolableRuleCheck
import com.example.model.ReliabilityGuidelineCheck
import com.example.model.Stock
import com.example.model.WavePatternType
import com.example.model.WavePhase
import com.example.model.WavePoint
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkOledBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.IndicatorBollinger
import com.example.ui.theme.IndicatorMA120
import com.example.ui.theme.IndicatorMA20
import com.example.ui.theme.IndicatorMA5
import com.example.ui.theme.IndicatorMA60
import com.example.ui.theme.IndicatorMACD
import com.example.ui.theme.IndicatorRSI
import com.example.ui.theme.IndicatorSignal
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import com.example.ui.theme.StockBlue
import com.example.ui.theme.StockRed
import com.example.ui.theme.TabularRateBadge
import com.example.ui.theme.TextDim
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
    customDays: Int = 3,
    onCustomDaysChanged: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showMA by remember { mutableStateOf(false) }
    var showBollinger by remember { mutableStateOf(false) }
    var showElliottWave by remember { mutableStateOf(true) } // Elliott wave enabled by default
    var selectedSubIndicator by remember { mutableStateOf(SubIndicatorType.VOLUME) }
    var scrubIndex by remember { mutableStateOf<Int?>(null) }
    var isElliottDetailsExpanded by remember { mutableStateOf(false) }
    var showCustomDaysDialog by remember { mutableStateOf(false) }

    val visibleCandles = remember(candles) { candles.takeLast(50) }
    val elliottWaveResult = remember(visibleCandles) {
        ElliottWaveEngine.analyzeElliottWaves(visibleCandles)
    }

    if (showCustomDaysDialog) {
        CustomDaysIntervalDialog(
            currentDays = customDays,
            onDismiss = { showCustomDaysDialog = false },
            onApply = { newDays ->
                onCustomDaysChanged(newDays)
                onTimeframeSelected(ChartTimeframe.CUSTOM_DAYS)
                showCustomDaysDialog = false
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
            .padding(12.dp)
            .testTag("candlestick_chart_container")
    ) {
        // Timeframe selector row & Sub-Indicator Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Timeframe Pills
            Row(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChartTimeframe.values().forEach { tf ->
                    val isSelected = tf == selectedTimeframe
                    val isCustom = tf == ChartTimeframe.CUSTOM_DAYS
                    val label = if (isCustom) "${customDays}일" else tf.label

                    Surface(
                        onClick = {
                            if (isCustom) {
                                if (isSelected) {
                                    showCustomDaysDialog = true
                                } else {
                                    onTimeframeSelected(tf)
                                }
                            } else {
                                onTimeframeSelected(tf)
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) NeonCyan else DarkSurfaceVariant,
                        border = if (isCustom && !isSelected) androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)) else null,
                        modifier = Modifier.testTag("tf_button_${tf.name}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = if (isCustom) 6.dp else 7.dp, vertical = 4.dp)
                        ) {
                            if (isCustom) {
                                Icon(
                                    imageVector = Icons.Filled.Tune,
                                    contentDescription = "설정",
                                    tint = if (isSelected) Color.Black else NeonCyan,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                            }
                            Text(
                                text = label,
                                color = if (isSelected) Color.Black else if (isCustom) NeonCyan else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Sub Indicator Floating Pills (VOL, RSI, MACD)
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                SubIndicatorType.values().forEach { sub ->
                    val isSelected = sub == selectedSubIndicator
                    Surface(
                        onClick = { selectedSubIndicator = sub },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) NeonCyan.copy(alpha = 0.2f) else Color.Transparent,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, NeonCyan) else null,
                        modifier = Modifier.testTag("sub_indicator_${sub.name}")
                    ) {
                        Text(
                            text = sub.label,
                            color = if (isSelected) NeonCyan else TextMuted,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Floating Pill Buttons for Technical Overlay Indicators (Elliott Wave, MA, Bollinger)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Elliott Wave Toggle Pill
            Surface(
                onClick = { showElliottWave = !showElliottWave },
                shape = RoundedCornerShape(20.dp),
                color = if (showElliottWave) NeonCyan.copy(alpha = 0.22f) else DarkSurfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (showElliottWave) NeonCyan else DarkBorder),
                modifier = Modifier.testTag("toggle_elliott_pill")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(if (showElliottWave) NeonCyan else TextMuted))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "🌊 엘리엇 파동 (1-5/ABC)",
                        fontSize = 11.sp,
                        color = if (showElliottWave) NeonCyan else TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Surface(
                onClick = { showMA = !showMA },
                shape = RoundedCornerShape(20.dp),
                color = if (showMA) IndicatorMA5.copy(alpha = 0.2f) else DarkSurfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (showMA) IndicatorMA5 else DarkBorder),
                modifier = Modifier.testTag("toggle_ma_pill")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(if (showMA) IndicatorMA5 else TextMuted))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("MA (5/20/60)", fontSize = 11.sp, color = if (showMA) IndicatorMA5 else TextMuted, fontWeight = FontWeight.Bold)
                }
            }

            Surface(
                onClick = { showBollinger = !showBollinger },
                shape = RoundedCornerShape(20.dp),
                color = if (showBollinger) IndicatorBollinger.copy(alpha = 0.2f) else DarkSurfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (showBollinger) IndicatorBollinger else DarkBorder),
                modifier = Modifier.testTag("toggle_bb_pill")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(if (showBollinger) IndicatorBollinger else TextMuted))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("볼린저밴드", fontSize = 11.sp, color = if (showBollinger) IndicatorBollinger else TextMuted, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Scrub Info Tooltip Badge
        if (scrubIndex != null && scrubIndex!! in visibleCandles.indices) {
            val scrubbed = visibleCandles[scrubIndex!!]
            val datePattern = when (selectedTimeframe) {
                ChartTimeframe.YEARLY -> "yyyy년"
                ChartTimeframe.MONTHLY -> "yyyy.MM"
                ChartTimeframe.WEEKLY, ChartTimeframe.DAILY, ChartTimeframe.D2, ChartTimeframe.D3, ChartTimeframe.D5, ChartTimeframe.D10 -> "yy.MM.dd"
                ChartTimeframe.CUSTOM_DAYS -> "yy.MM.dd (${customDays}일)"
                else -> "MM.dd HH:mm"
            }
            val dateStr = SimpleDateFormat(datePattern, Locale.KOREA).format(Date(scrubbed.candle.timestamp))
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
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

        // Main Chart Canvas (Candlestick + Overlays + Elliott Waves) & Sub-chart Canvas
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
                    showElliottWave = showElliottWave,
                    elliottResult = elliottWaveResult,
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

        // Elliott Wave Live Analysis & Fibonacci Strategy Diagnostic Card
        if (showElliottWave && elliottWaveResult.points.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            ElliottWaveAnalysisCard(
                result = elliottWaveResult,
                currentPrice = stock.currentPrice,
                isExpanded = isElliottDetailsExpanded,
                onToggleExpand = { isElliottDetailsExpanded = !isElliottDetailsExpanded }
            )
        }
    }
}

@Composable
private fun ElliottWaveAnalysisCard(
    result: ElliottWaveResult,
    currentPrice: Double,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (result.isStrictlyValid) NeonCyan.copy(alpha = 0.5f) else NeonRed.copy(alpha = 0.6f)
        ),
        modifier = Modifier.fillMaxWidth().testTag("elliott_analysis_card")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row: Phase Badge, Validity & Confidence
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when (result.currentPhase) {
                            WavePhase.WAVE_3_IMPULSE -> NeonGreen.copy(alpha = 0.2f)
                            WavePhase.WAVE_1_START, WavePhase.WAVE_2_PULLBACK, WavePhase.WAVE_4_CONSOLIDATION -> NeonCyan.copy(alpha = 0.2f)
                            WavePhase.WAVE_5_CLIMAX -> NeonAmber.copy(alpha = 0.25f)
                            else -> NeonRed.copy(alpha = 0.2f)
                        },
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            when (result.currentPhase) {
                                WavePhase.WAVE_3_IMPULSE -> NeonGreen
                                WavePhase.WAVE_5_CLIMAX -> NeonAmber
                                WavePhase.WAVE_A_CORRECTION, WavePhase.WAVE_C_COMPLETION -> NeonRed
                                else -> NeonCyan
                            }
                        )
                    ) {
                        Text(
                            text = result.currentPhase.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = when (result.currentPhase) {
                                WavePhase.WAVE_3_IMPULSE -> NeonGreen
                                WavePhase.WAVE_5_CLIMAX -> NeonAmber
                                WavePhase.WAVE_A_CORRECTION, WavePhase.WAVE_C_COMPLETION -> NeonRed
                                else -> NeonCyan
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Invalidation / Pattern Type Tag
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(result.patternType.badgeColorHex).copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(result.patternType.badgeColorHex).copy(alpha = 0.7f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = if (result.isStrictlyValid) Icons.Filled.Verified else Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = Color(result.patternType.badgeColorHex),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (result.isStrictlyValid) "3대원칙 준수" else "원칙 위배(무효)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(result.patternType.badgeColorHex)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "신뢰도 ${result.confidenceScore}%",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (result.confidenceScore >= 80) NeonGreen else if (result.confidenceScore >= 60) NeonCyan else NeonRed
                    )
                }

                IconButton(onClick = onToggleExpand, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = "파동 상세",
                        tint = NeonCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Summary description
            Text(
                text = result.summary,
                fontSize = 12.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Strategy Tip
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Filled.TrendingUp,
                    contentDescription = null,
                    tint = NeonAmber,
                    modifier = Modifier.size(14.dp).padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "전략 가이드: ${result.strategyTip}",
                    fontSize = 11.sp,
                    color = NeonAmber,
                    lineHeight = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Target, Support & Resistance Key Levels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LevelBadge(label = "예상 목표가", price = result.targetPrice, color = NeonGreen)
                LevelBadge(label = "핵심 지지선", price = result.supportPrice, color = NeonCyan)
                LevelBadge(label = "상단 저항선", price = result.resistancePrice, color = NeonRed)
            }

            // Expandable Wave Breakdown & 3 Absolute Rules & Guidelines
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    HorizontalDivider(color = DarkBorder)
                    Spacer(modifier = Modifier.height(8.dp))

                    // 1. 절대 불가변 3대 원칙 (필요조건) 상태 섹션
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = null,
                            tint = if (result.isStrictlyValid) NeonGreen else NeonRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "엘리엇 충격파 3대 절대 불가변 법칙 (필요조건 검증)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (result.isStrictlyValid) NeonGreen else NeonRed
                        )
                    }
                    Spacer(modifier = Modifier.height(5.dp))

                    result.inviolableRules.forEach { rule ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (rule.isSatisfied) DarkSurface else NeonRed.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (rule.isSatisfied) DarkBorder else NeonRed.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (rule.isSatisfied) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                                    contentDescription = null,
                                    tint = if (rule.isSatisfied) NeonGreen else NeonRed,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = rule.ruleName,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (rule.isSatisfied) TextPrimary else NeonRed
                                        )
                                        if (rule.metricValueText.isNotBlank()) {
                                            Text(
                                                text = rule.metricValueText,
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = if (rule.isSatisfied) NeonCyan else NeonRed
                                            )
                                        }
                                    }
                                    Text(
                                        text = rule.reason,
                                        fontSize = 9.5.sp,
                                        color = TextSecondary,
                                        lineHeight = 13.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. 신뢰도 보조 지침 (충분조건 가이드) 섹션
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Rule,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "파동 신뢰도 보조 지침 (충분조건 분석)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    }
                    Spacer(modifier = Modifier.height(5.dp))

                    result.reliabilityGuidelines.forEach { guide ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = if (guide.isSatisfied) Icons.Filled.CheckCircleOutline else Icons.Filled.Info,
                                contentDescription = null,
                                tint = if (guide.isSatisfied) NeonGreen else TextMuted,
                                modifier = Modifier.size(12.dp).padding(top = 1.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = guide.guidelineName,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (guide.isSatisfied) TextPrimary else TextSecondary
                                    )
                                    Text(
                                        text = guide.scoreImpactText,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (guide.isSatisfied) NeonGreen else TextDim
                                    )
                                }
                                Text(
                                    text = guide.description,
                                    fontSize = 9.5.sp,
                                    color = TextSecondary,
                                    lineHeight = 13.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = DarkBorder)
                    Spacer(modifier = Modifier.height(8.dp))

                    // 3. 각 파동별 피보나치 변동 상세 테이블
                    Text(
                        text = "각 파동별 구간 분석 및 피보나치 비율",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    result.waveDetails.forEach { detail ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = detail.waveName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan,
                                modifier = Modifier.width(78.dp)
                            )
                            Text(
                                text = "%,.0f원 → %,.0f원".format(detail.startPrice, detail.endPrice),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextPrimary
                            )
                            Text(
                                text = detail.fibRatioText,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (detail.changePercent >= 0) NeonGreen else NeonRed
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LevelBadge(label: String, price: Double, color: Color) {
    Column {
        Text(text = label, fontSize = 10.sp, color = TextSecondary)
        Text(
            text = "%,.0f원".format(price),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

private fun DrawScope.drawMainCandlestickChart(
    candles: List<CalculatedCandle>,
    width: Float,
    height: Float,
    showMA: Boolean,
    showBollinger: Boolean,
    showElliottWave: Boolean,
    elliottResult: ElliottWaveResult,
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

    if (showElliottWave && elliottResult.points.isNotEmpty()) {
        elliottResult.points.forEach { p ->
            minPrice = min(minPrice, p.price * 0.98)
            maxPrice = max(maxPrice, p.price * 1.02)
        }
        elliottResult.projectedNextPoint?.let { p ->
            minPrice = min(minPrice, p.price * 0.98)
            maxPrice = max(maxPrice, p.price * 1.02)
        }
    }

    val priceRange = if (maxPrice - minPrice > 0) maxPrice - minPrice else 1.0
    val candleWidth = width / candles.size
    val bodyWidth = candleWidth * 0.68f

    fun priceToY(price: Double): Float {
        return (height - ((price - minPrice) / priceRange * (height - 30f)) - 15f).toFloat()
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

    // Draw Automatic Elliott Wave Overlay
    if (showElliottWave && elliottResult.points.isNotEmpty()) {
        drawElliottWaveOverlay(
            result = elliottResult,
            candleWidth = candleWidth,
            height = height,
            priceToY = { priceToY(it) }
        )
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

private fun DrawScope.drawElliottWaveOverlay(
    result: ElliottWaveResult,
    candleWidth: Float,
    height: Float,
    priceToY: (Double) -> Float
) {
    val points = result.points
    if (points.isEmpty()) return

    val pathImpulse = Path()
    val pathCorrection = Path()
    var impulseStarted = false
    var correctionStarted = false

    val nodeOffsets = mutableListOf<Triple<Offset, WavePoint, Color>>()

    points.forEachIndexed { i, p ->
        val x = p.index * candleWidth + candleWidth / 2f
        val y = priceToY(p.price)
        val offset = Offset(x, y)

        val isCorrection = p.label in listOf("A", "B", "C")
        val nodeColor = when (p.label) {
            "1", "3", "5" -> NeonGreen
            "2", "4" -> NeonCyan
            "A", "C" -> NeonRed
            "B" -> NeonAmber
            else -> NeonPurple
        }
        nodeOffsets.add(Triple(offset, p, nodeColor))

        if (!isCorrection && i <= 5) {
            // Impulse Waves (0 to 5)
            if (!impulseStarted) {
                pathImpulse.moveTo(x, y)
                impulseStarted = true
            } else {
                pathImpulse.lineTo(x, y)
            }
        } else {
            // Correction Waves (5 to C)
            if (!correctionStarted) {
                // Connect from Wave 5
                val lastImpulse = nodeOffsets.getOrNull(5)?.first ?: offset
                pathCorrection.moveTo(lastImpulse.x, lastImpulse.y)
                pathCorrection.lineTo(x, y)
                correctionStarted = true
            } else {
                pathCorrection.lineTo(x, y)
            }
        }
    }

    // 1. Draw Glowing Outer Polyline for Impulse Waves
    if (impulseStarted) {
        // Outer glow
        drawPath(
            path = pathImpulse,
            color = NeonCyan.copy(alpha = 0.25f),
            style = Stroke(width = 6f)
        )
        // Core line
        drawPath(
            path = pathImpulse,
            color = NeonCyan,
            style = Stroke(width = 2.5f)
        )
    }

    // 2. Draw Polyline for Correction Waves (A-B-C)
    if (correctionStarted) {
        drawPath(
            path = pathCorrection,
            color = NeonAmber.copy(alpha = 0.25f),
            style = Stroke(width = 6f)
        )
        drawPath(
            path = pathCorrection,
            color = NeonAmber,
            style = Stroke(width = 2.5f)
        )
    }

    // 3. Draw Future Projected Target Line (Dashed)
    result.projectedNextPoint?.let { nextP ->
        val lastNode = nodeOffsets.lastOrNull()?.first
        if (lastNode != null) {
            val nextX = nextP.index * candleWidth + candleWidth / 2f
            val nextY = priceToY(nextP.price)
            val nextOffset = Offset(nextX, nextY)

            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            drawLine(
                color = NeonPurple,
                start = lastNode,
                end = nextOffset,
                strokeWidth = 2.2f,
                pathEffect = dashEffect
            )

            // Target Node Ring
            drawCircle(color = NeonPurple.copy(alpha = 0.3f), radius = 16f, center = nextOffset)
            drawCircle(color = DarkOledBackground, radius = 10f, center = nextOffset)
            drawCircle(color = NeonPurple, radius = 10f, center = nextOffset, style = Stroke(width = 2f))

            // Label
            val targetPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 24f
                isFakeBoldText = true
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }
            drawContext.canvas.nativeCanvas.drawText(
                nextP.label,
                nextOffset.x,
                nextOffset.y - 14f,
                targetPaint
            )
        }
    }

    // 4. Draw Interactive Wave Node Badges (⓪, ①, ②, ③, ④, ⑤, Ⓐ, Ⓑ, Ⓒ)
    val textPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 28f
        isFakeBoldText = true
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
    }

    val ratioPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 22f
        isFakeBoldText = true
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
    }

    nodeOffsets.forEach { (offset, wavePoint, color) ->
        // Glow Halo
        drawCircle(
            color = color.copy(alpha = 0.35f),
            radius = 18f,
            center = offset
        )
        // Solid Badge Center
        drawCircle(
            color = color,
            radius = 12f,
            center = offset
        )
        drawCircle(
            color = Color.White,
            radius = 12f,
            center = offset,
            style = Stroke(width = 1.5f)
        )

        // Draw Wave Number/Letter inside badge
        drawContext.canvas.nativeCanvas.drawText(
            wavePoint.label,
            offset.x,
            offset.y + 9f,
            textPaint
        )

        // Draw Fibonacci / Wave ratio tooltip above or below pivot
        if (wavePoint.fibRatioText.isNotBlank()) {
            val labelY = if (wavePoint.isHigh) offset.y - 20f else offset.y + 32f
            drawContext.canvas.nativeCanvas.drawText(
                wavePoint.fibRatioText,
                offset.x,
                labelY,
                ratioPaint
            )
        }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomDaysIntervalDialog(
    currentDays: Int,
    onDismiss: () -> Unit,
    onApply: (Int) -> Unit
) {
    var tempDays by remember { mutableIntStateOf(currentDays.coerceIn(1, 365)) }
    val presetDays = listOf(2, 3, 4, 5, 7, 10, 14, 20, 30, 60, 120)

    val strategyDescription = when {
        tempDays == 1 -> "1일(단일 거래일) 표준 일봉"
        tempDays in 2..3 -> "${tempDays}일봉: 단기 변곡점 및 급등주 단타/스윙 타이밍 포착"
        tempDays in 4..7 -> "${tempDays}일봉: 1주일 단위 단기 스윙 및 눌림목 지지선 분석"
        tempDays in 8..15 -> "${tempDays}일봉: 2주 단위 중기 추세 및 기관/외인 수급 사이클 추종"
        tempDays in 16..30 -> "${tempDays}일봉: 1개월 단위 월간 모멘텀 및 골든크로스 검증"
        tempDays in 31..60 -> "${tempDays}일봉: 분기(Quarter) 단위 실적 발표 및 대세 상승장 파동"
        else -> "${tempDays}일봉: 반기/연간 장기 메가트렌드 및 밸류에이션 추세"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.CalendarMonth,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "원하는 Days(일) 간격 설정",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "원하는 일봉 간격을 직접 조절하거나 프리셋을 선택하여 멀티 데이 캔들 차트를 구성합니다.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                // Stepper Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkOledBackground)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (tempDays > 1) {
                                tempDays--
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant)
                            .testTag("custom_days_minus_btn")
                    ) {
                        Icon(imageVector = Icons.Filled.Remove, contentDescription = "감소", tint = TextPrimary)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$tempDays",
                            color = NeonCyan,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "일봉 간격",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = {
                            if (tempDays < 365) {
                                tempDays++
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant)
                            .testTag("custom_days_plus_btn")
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = "증가", tint = TextPrimary)
                    }
                }

                // Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("1일", color = TextMuted, fontSize = 11.sp)
                        Text("60일", color = TextMuted, fontSize = 11.sp)
                        Text("120일", color = TextMuted, fontSize = 11.sp)
                    }
                    Slider(
                        value = tempDays.toFloat().coerceIn(1f, 120f),
                        onValueChange = {
                            tempDays = it.toInt().coerceIn(1, 365)
                        },
                        valueRange = 1f..120f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = DarkBorder
                        )
                    )
                }

                // Quick Presets
                Column {
                    Text(
                        text = "빠른 일(Days)수 프리셋",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presetDays.forEach { days ->
                            val isCurrent = tempDays == days
                            Surface(
                                onClick = {
                                    tempDays = days
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isCurrent) NeonCyan else DarkSurfaceVariant,
                                border = if (isCurrent) null else androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                                modifier = Modifier.testTag("preset_day_${days}_btn")
                            ) {
                                Text(
                                    text = "${days}일",
                                    color = if (isCurrent) Color.Black else TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Strategy guide box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonCyan.copy(alpha = 0.1f))
                        .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "💡 $strategyDescription",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onApply(tempDays) },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("apply_custom_days_btn")
            ) {
                Text("${tempDays}일봉 적용", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Text("취소", color = TextSecondary)
            }
        }
    )
}
