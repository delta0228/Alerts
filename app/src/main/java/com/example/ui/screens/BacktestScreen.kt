package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StockDataManager
import com.example.engine.BacktestConfig
import com.example.engine.BacktestEngine
import com.example.engine.BacktestResult
import com.example.engine.BacktestTrade
import com.example.engine.EquityPoint
import com.example.engine.ExitReason
import com.example.engine.StrategyPreset
import com.example.model.AlertRule
import com.example.model.ChartTimeframe
import com.example.model.MarketType
import com.example.model.RuleScope
import com.example.model.RuleType
import com.example.model.Stock
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandPrimaryLight
import com.example.ui.theme.BrandSecondary
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceHighlight
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.IndicatorBollinger
import com.example.ui.theme.IndicatorMA20
import com.example.ui.theme.IndicatorMA5
import com.example.ui.theme.IndicatorRSI
import com.example.ui.theme.StockBlue
import com.example.ui.theme.StockRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BacktestScreen(
    stocks: List<Stock>,
    dataManager: StockDataManager,
    initialStock: Stock? = null,
    initialRuleType: RuleType? = null,
    onSaveRule: (AlertRule) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Selected Target Stock
    var selectedStock by remember {
        mutableStateOf(initialStock ?: stocks.firstOrNull() ?: Stock("005930", "삼성전자", MarketType.KOSPI, 74800.0, 1200.0, 1.63, 73900.0, 75200.0, 73800.0, 73600.0, 14230500, 1060000000000L))
    }

    // Timeframe
    var selectedTimeframe by remember { mutableStateOf(ChartTimeframe.M5) }

    // Strategy Parameters
    var selectedRuleType by remember { mutableStateOf(initialRuleType ?: RuleType.MA_GOLDEN_CROSS) }
    var param1 by remember { mutableIntStateOf(5) }
    var param2 by remember { mutableIntStateOf(20) }
    var thresholdValue by remember { mutableDoubleStateOf(30.0) }

    // Risk Management Parameters
    var takeProfitPct by remember { mutableDoubleStateOf(4.0) }
    var stopLossPct by remember { mutableDoubleStateOf(2.0) }
    var maxHoldBars by remember { mutableIntStateOf(20) }
    var exitOnOpposite by remember { mutableStateOf(true) }
    var initialCapital by remember { mutableDoubleStateOf(10_000_000.0) }

    var isConfigExpanded by remember { mutableStateOf(true) }
    var selectedPresetId by remember { mutableStateOf<String?>("ma_golden_cross") }

    // Backtest Result State
    var backtestResult by remember { mutableStateOf<BacktestResult?>(null) }
    var isRunning by remember { mutableStateOf(false) }

    // Execution helper
    fun executeBacktest() {
        isRunning = true
        val config = BacktestConfig(
            symbol = selectedStock.symbol,
            timeframe = selectedTimeframe,
            ruleType = selectedRuleType,
            thresholdValue = thresholdValue,
            param1 = param1,
            param2 = param2,
            initialCapital = initialCapital,
            takeProfitPct = takeProfitPct,
            stopLossPct = stopLossPct,
            maxHoldBars = maxHoldBars,
            exitOnOppositeSignal = exitOnOpposite,
            feeRatePct = 0.015
        )
        val candles = dataManager.getCandles(selectedStock.symbol, selectedTimeframe)
        val result = BacktestEngine.runBacktest(selectedStock, candles, config)
        backtestResult = result
        isRunning = false
    }

    // Run initial backtest when screen loads
    LaunchedEffect(selectedStock, selectedTimeframe) {
        executeBacktest()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "전략 백테스트 시뮬레이터",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${selectedStock.name} (${selectedStock.symbol}) • ${selectedTimeframe.label}",
                            fontSize = 12.sp,
                            color = BrandSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("backtest_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { executeBacktest() },
                        modifier = Modifier.testTag("backtest_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "다시 실행",
                            tint = BrandPrimaryLight
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DarkBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // 1. Preset Strategies Carousel
            PresetStrategySection(
                selectedPresetId = selectedPresetId,
                onSelectPreset = { preset ->
                    selectedPresetId = preset.id
                    selectedRuleType = preset.ruleType
                    param1 = preset.param1
                    param2 = preset.param2
                    thresholdValue = preset.thresholdValue
                    takeProfitPct = preset.takeProfitPct
                    stopLossPct = preset.stopLossPct
                    maxHoldBars = preset.maxHoldBars
                    exitOnOpposite = preset.exitOnOpposite
                    executeBacktest()
                }
            )

            // 2. Configuration & Parameter Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkSurfaceHighlight, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isConfigExpanded = !isConfigExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Tune,
                                contentDescription = null,
                                tint = BrandPrimaryLight,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "백테스트 조건 및 리스크 설정",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Icon(
                            imageVector = if (isConfigExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            tint = TextSecondary
                        )
                    }

                    AnimatedVisibility(
                        visible = isConfigExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Target Stock Chips
                            Column {
                                Text(
                                    text = "감시 대상 종목",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                val scrollState = rememberScrollState()
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(scrollState),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    stocks.take(8).forEach { stock ->
                                        val isSelected = stock.symbol == selectedStock.symbol
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSelected) BrandPrimary.copy(alpha = 0.25f) else DarkSurfaceVariant,
                                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, BrandPrimary) else null,
                                            modifier = Modifier.clickable {
                                                selectedStock = stock
                                                selectedPresetId = null
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = stock.name,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) BrandPrimaryLight else TextPrimary
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (stock.market == MarketType.US) "$${stock.currentPrice}" else "${stock.currentPrice.toInt()}원",
                                                    fontSize = 11.sp,
                                                    color = if (stock.isRising) StockRed else StockBlue
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Timeframe Selection
                            Column {
                                Text(
                                    text = "차트 주기 (Timeframe)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    ChartTimeframe.values().forEach { tf ->
                                        val isSelected = tf == selectedTimeframe
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSelected) BrandPrimary else DarkSurfaceVariant,
                                            modifier = Modifier
                                                .clickable {
                                                    selectedTimeframe = tf
                                                    selectedPresetId = null
                                                }
                                        ) {
                                            Text(
                                                text = tf.label,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.White else TextSecondary,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = DarkSurfaceHighlight)

                            // Strategy Entry Rule Selector
                            Column {
                                Text(
                                    text = "진입 전략 (Entry Strategy)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                val strategies = listOf(
                                    RuleType.MA_GOLDEN_CROSS to "5/20일 골든크로스",
                                    RuleType.RSI_OVERSOLD to "RSI 30 이하 과매도",
                                    RuleType.BOLLINGER_LOWER_TOUCH to "볼린저 밴드 하단 터치",
                                    RuleType.VOLUME_SURGE to "거래량 2배 폭증",
                                    RuleType.CHANGE_RATE_PLUNGE to "단기 -2.5% 급락 반등"
                                )

                                val rowScroll = rememberScrollState()
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rowScroll),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    strategies.forEach { (type, label) ->
                                        val isSelected = selectedRuleType == type
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSelected) BrandSecondary.copy(alpha = 0.2f) else DarkSurfaceVariant,
                                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, BrandSecondary) else null,
                                            modifier = Modifier.clickable {
                                                selectedRuleType = type
                                                selectedPresetId = null
                                            }
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) BrandSecondary else TextPrimary,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Dynamic Parameters
                            when (selectedRuleType) {
                                RuleType.MA_GOLDEN_CROSS, RuleType.MA_DEAD_CROSS -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("단기 이평: ${param1}일선", fontSize = 12.sp, color = IndicatorMA5)
                                            Slider(
                                                value = param1.toFloat(),
                                                onValueChange = { param1 = it.toInt(); selectedPresetId = null },
                                                valueRange = 3f..20f,
                                                steps = 17,
                                                colors = SliderDefaults.colors(thumbColor = IndicatorMA5, activeTrackColor = IndicatorMA5)
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("장기 이평: ${param2}일선", fontSize = 12.sp, color = IndicatorMA20)
                                            Slider(
                                                value = param2.toFloat(),
                                                onValueChange = { param2 = it.toInt(); selectedPresetId = null },
                                                valueRange = 10f..60f,
                                                steps = 50,
                                                colors = SliderDefaults.colors(thumbColor = IndicatorMA20, activeTrackColor = IndicatorMA20)
                                            )
                                        }
                                    }
                                }

                                RuleType.RSI_OVERSOLD, RuleType.RSI_OVERBOUGHT -> {
                                    Column {
                                        Text("RSI 기준값: ${thresholdValue.toInt()}", fontSize = 12.sp, color = IndicatorRSI)
                                        Slider(
                                            value = thresholdValue.toFloat(),
                                            onValueChange = { thresholdValue = it.toDouble(); selectedPresetId = null },
                                            valueRange = 15f..85f,
                                            steps = 70,
                                            colors = SliderDefaults.colors(thumbColor = IndicatorRSI, activeTrackColor = IndicatorRSI)
                                        )
                                    }
                                }

                                RuleType.VOLUME_SURGE -> {
                                    Column {
                                        Text("평균 대비 거래량 배수: %.1f배".format(thresholdValue), fontSize = 12.sp, color = AccentGold)
                                        Slider(
                                            value = thresholdValue.toFloat(),
                                            onValueChange = { thresholdValue = it.toDouble(); selectedPresetId = null },
                                            valueRange = 1.2f..5.0f,
                                            steps = 38,
                                            colors = SliderDefaults.colors(thumbColor = AccentGold, activeTrackColor = AccentGold)
                                        )
                                    }
                                }

                                RuleType.CHANGE_RATE_PLUNGE, RuleType.CHANGE_RATE_SURGE -> {
                                    Column {
                                        Text("등락률 기준값: %.1f%%".format(thresholdValue), fontSize = 12.sp, color = StockRed)
                                        Slider(
                                            value = thresholdValue.toFloat(),
                                            onValueChange = { thresholdValue = it.toDouble(); selectedPresetId = null },
                                            valueRange = 1.0f..10.0f,
                                            steps = 90,
                                            colors = SliderDefaults.colors(thumbColor = StockRed, activeTrackColor = StockRed)
                                        )
                                    }
                                }

                                else -> {}
                            }

                            HorizontalDivider(color = DarkSurfaceHighlight)

                            // Risk Management (Take Profit / Stop Loss / Max Hold)
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "리스크 관리 및 청산 조건 (Risk & Exit)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextSecondary
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("익절 목표 (TP)", fontSize = 12.sp, color = StockRed)
                                            Text("+%.1f%%".format(takeProfitPct), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StockRed)
                                        }
                                        Slider(
                                            value = takeProfitPct.toFloat(),
                                            onValueChange = { takeProfitPct = it.toDouble(); selectedPresetId = null },
                                            valueRange = 1.0f..15.0f,
                                            steps = 28,
                                            colors = SliderDefaults.colors(thumbColor = StockRed, activeTrackColor = StockRed)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("손절 한도 (SL)", fontSize = 12.sp, color = StockBlue)
                                            Text("-%.1f%%".format(stopLossPct), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StockBlue)
                                        }
                                        Slider(
                                            value = stopLossPct.toFloat(),
                                            onValueChange = { stopLossPct = it.toDouble(); selectedPresetId = null },
                                            valueRange = 0.5f..10.0f,
                                            steps = 19,
                                            colors = SliderDefaults.colors(thumbColor = StockBlue, activeTrackColor = StockBlue)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("반대 신호 발생 시 즉시 청산", fontSize = 13.sp, color = TextPrimary)
                                        Text("데드크로스 또는 과매수 도달 시 포지션 종료", fontSize = 11.sp, color = TextMuted)
                                    }
                                    Switch(
                                        checked = exitOnOpposite,
                                        onCheckedChange = { exitOnOpposite = it; selectedPresetId = null },
                                        colors = SwitchDefaults.colors(checkedThumbColor = BrandPrimary, checkedTrackColor = BrandPrimary.copy(alpha = 0.5f))
                                    )
                                }
                            }

                            // Run Button
                            Button(
                                onClick = { executeBacktest() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("run_backtest_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BrandPrimary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "백테스트 시뮬레이션 실행",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 3. Backtest Results Section
            backtestResult?.let { result ->
                BacktestResultDashboard(
                    result = result,
                    onDeployAlert = {
                        val alertRule = AlertRule(
                            name = "[백테스트 검증] ${result.stock.name} ${result.config.ruleType.title}",
                            scope = RuleScope.SPECIFIC,
                            targetSymbol = result.stock.symbol,
                            targetSymbolName = result.stock.name,
                            timeframe = result.config.timeframe,
                            ruleType = result.config.ruleType,
                            thresholdValue = result.config.thresholdValue,
                            param1 = result.config.param1,
                            param2 = result.config.param2,
                            cooldownMinutes = 15
                        )
                        onSaveRule(alertRule)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message = "전략이 실시간 알림 규칙으로 성공적으로 등록되었습니다!",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PresetStrategySection(
    selectedPresetId: String?,
    onSelectPreset: (StrategyPreset) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = AccentGold,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "인기 퀀트 전략 프리셋",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            Text(
                text = "원클릭 적용",
                fontSize = 11.sp,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        val scroll = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scroll),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BacktestEngine.PRESET_STRATEGIES.forEach { preset ->
                val isSelected = preset.id == selectedPresetId
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) BrandPrimary.copy(alpha = 0.2f) else DarkSurface,
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, BrandPrimary) else androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceHighlight),
                    modifier = Modifier
                        .width(220.dp)
                        .clickable { onSelectPreset(preset) }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = preset.iconEmoji, fontSize = 16.sp)
                            Text(
                                text = preset.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) BrandPrimaryLight else TextPrimary,
                                maxLines = 1
                            )
                        }
                        Text(
                            text = preset.description,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 2,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BacktestResultDashboard(
    result: BacktestResult,
    onDeployAlert: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

        // Main Performance Card
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, DarkSurfaceHighlight, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header & Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "시뮬레이션 분석 결과",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val isPositive = result.totalReturnPct >= 0
                            Text(
                                text = "${if (isPositive) "+" else ""}%.2f%%".format(result.totalReturnPct),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPositive) StockRed else StockBlue
                            )
                            Text(
                                text = "(단순보유: %+.2f%%)".format(result.benchmarkReturnPct),
                                fontSize = 12.sp,
                                color = TextMuted,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }

                    // Profit Amount Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (result.totalProfit >= 0) StockRed.copy(alpha = 0.15f) else StockBlue.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (result.stock.market == MarketType.US) {
                                "${if (result.totalProfit >= 0) "+" else ""}$%,.2f".format(result.totalProfit)
                            } else {
                                "${if (result.totalProfit >= 0) "+" else ""}$%,.0f원".format(result.totalProfit)
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (result.totalProfit >= 0) StockRed else StockBlue,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // KPI Grid (4 columns)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricBox(
                        title = "승률 (Win Rate)",
                        value = "%.1f%%".format(result.winRatePct),
                        subtitle = "${result.winningTrades}승 ${result.losingTrades}패",
                        valueColor = if (result.winRatePct >= 50.0) AccentGreen else StockBlue,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        title = "최대 낙폭 (MDD)",
                        value = "-%.1f%%".format(result.maxDrawdownPct),
                        subtitle = "자본 보존력",
                        valueColor = if (result.maxDrawdownPct <= 3.0) AccentGreen else StockRed,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricBox(
                        title = "수익 팩터 (PF)",
                        value = "%.2f".format(result.profitFactor),
                        subtitle = "총이익 / 총손실",
                        valueColor = if (result.profitFactor >= 1.5) AccentGold else TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        title = "총 매매 횟수",
                        value = "${result.totalTrades}회",
                        subtitle = "평균 %.1f봉 보유".format(result.avgHoldingBars),
                        valueColor = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Deploy Strategy Button
                Button(
                    onClick = onDeployAlert,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("deploy_alert_from_backtest_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkSurfaceVariant
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrandPrimaryLight)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AddAlert,
                        contentDescription = null,
                        tint = BrandPrimaryLight,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "이 검증된 전략으로 실시간 알림 등록",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandPrimaryLight
                    )
                }
            }
        }

        // Equity Curve Chart Card
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, DarkSurfaceHighlight, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "누적 자산 성장 곡선 (Equity Curve)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(BrandPrimary, CircleShape))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("전략 자산", fontSize = 11.sp, color = TextSecondary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(TextMuted, CircleShape))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("초기 자본", fontSize = 11.sp, color = TextMuted)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                EquityCurveCanvas(
                    equityCurve = result.equityCurve,
                    initialCapital = result.initialCapital,
                    trades = result.trades,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
            }
        }

        // Trade-by-Trade Logs
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, DarkSurfaceHighlight, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "매매 실행 내역 (${result.trades.size}건)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "수수료(0.015%) 반영",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (result.trades.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "설정된 기간 내 매매 시그널이 발생하지 않았습니다.\n진입 조건이나 기준값을 완화해 보세요.",
                            fontSize = 13.sp,
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    val dateFormat = SimpleDateFormat("MM.dd HH:mm", Locale.getDefault())
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        result.trades.reversed().forEach { trade ->
                            TradeLogRow(trade = trade, isUS = result.stock.market == MarketType.US, dateFormat = dateFormat)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricBox(
    title: String,
    value: String,
    subtitle: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = DarkSurfaceVariant,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(text = title, fontSize = 11.sp, color = TextSecondary)
            Text(
                text = value,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
            Text(text = subtitle, fontSize = 10.sp, color = TextMuted)
        }
    }
}

@Composable
private fun EquityCurveCanvas(
    equityCurve: List<EquityPoint>,
    initialCapital: Double,
    trades: List<BacktestTrade>,
    modifier: Modifier = Modifier
) {
    if (equityCurve.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("데이터 없음", fontSize = 12.sp, color = TextMuted)
        }
        return
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val minEquity = minOf(initialCapital * 0.98, equityCurve.minOfOrNull { it.equity } ?: initialCapital)
        val maxEquity = maxOf(initialCapital * 1.02, equityCurve.maxOfOrNull { it.equity } ?: initialCapital)
        val range = (maxEquity - minEquity).coerceAtLeast(1.0)

        // Baseline (Initial Capital) Y coordinate
        val baselineY = height - (((initialCapital - minEquity) / range) * height).toFloat()

        // Draw Baseline dotted line
        drawLine(
            color = Color.Gray.copy(alpha = 0.4f),
            start = Offset(0f, baselineY),
            end = Offset(width, baselineY),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        )

        // Path for Equity Line & Gradient fill
        val linePath = Path()
        val fillPath = Path()

        val stepX = width / (equityCurve.size - 1).coerceAtLeast(1)

        equityCurve.forEachIndexed { index, pt ->
            val x = index * stepX
            val y = height - (((pt.equity - minEquity) / range) * height).toFloat()

            if (index == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }

        fillPath.lineTo(width, height)
        fillPath.close()

        // Draw Area Fill Gradient
        val isFinalProfit = (equityCurve.lastOrNull()?.equity ?: initialCapital) >= initialCapital
        val gradientBrush = Brush.verticalGradient(
            colors = if (isFinalProfit) {
                listOf(BrandPrimary.copy(alpha = 0.35f), BrandPrimary.copy(alpha = 0.0f))
            } else {
                listOf(StockBlue.copy(alpha = 0.35f), StockBlue.copy(alpha = 0.0f))
            },
            startY = 0f,
            endY = height
        )
        drawPath(path = fillPath, brush = gradientBrush)

        // Draw Equity Line
        drawPath(
            path = linePath,
            color = if (isFinalProfit) BrandPrimaryLight else StockBlue,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw Trade Buy/Sell points
        trades.forEach { trade ->
            val entryPt = equityCurve.find { it.index == trade.entryIndex }
            if (entryPt != null) {
                val ex = entryPt.index * stepX
                val ey = height - (((entryPt.equity - minEquity) / range) * height).toFloat()
                drawCircle(color = AccentGreen, radius = 3.5.dp.toPx(), center = Offset(ex, ey))
            }

            val exitPt = equityCurve.find { it.index == trade.exitIndex }
            if (exitPt != null) {
                val xx = exitPt.index * stepX
                val xy = height - (((exitPt.equity - minEquity) / range) * height).toFloat()
                drawCircle(
                    color = if (trade.isWin) StockRed else StockBlue,
                    radius = 4.dp.toPx(),
                    center = Offset(xx, xy)
                )
            }
        }
    }
}

@Composable
private fun TradeLogRow(
    trade: BacktestTrade,
    isUS: Boolean,
    dateFormat: SimpleDateFormat
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = DarkSurfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Trade # and times
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(
                            if (trade.isWin) StockRed.copy(alpha = 0.2f) else StockBlue.copy(alpha = 0.2f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (trade.isWin) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                        contentDescription = null,
                        tint = if (trade.isWin) StockRed else StockBlue,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "#${trade.tradeId}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = DarkSurfaceHighlight
                        ) {
                            Text(
                                text = trade.exitReason.displayName,
                                fontSize = 10.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "진입: ${if (isUS) "$${trade.entryPrice}" else "${trade.entryPrice.toInt()}원"} → 청산: ${if (isUS) "$${trade.exitPrice}" else "${trade.exitPrice.toInt()}원"}",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            // Right: Profit rate & amount
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (trade.profitRatePct >= 0) "+" else ""}%.2f%%".format(trade.profitRatePct),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (trade.profitRatePct >= 0) StockRed else StockBlue
                )
                Text(
                    text = if (isUS) {
                        "${if (trade.netProfit >= 0) "+" else ""}$%.2f".format(trade.netProfit)
                    } else {
                        "${if (trade.netProfit >= 0) "+" else ""}$%,.0f원".format(trade.netProfit)
                    },
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }
    }
}
