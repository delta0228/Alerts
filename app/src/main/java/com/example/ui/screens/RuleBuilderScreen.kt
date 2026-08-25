package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StockDataManager
import com.example.model.AlertRule
import com.example.model.ChartTimeframe
import com.example.model.RuleCategory
import com.example.model.RuleScope
import com.example.model.RuleType
import com.example.model.Stock
import com.example.ui.theme.AccentGold
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandPrimaryLight
import com.example.ui.theme.StockRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RuleBuilderScreen(
    stocks: List<Stock>,
    dataManager: StockDataManager,
    initialStock: Stock? = null,
    initialRuleType: RuleType? = null,
    onSaveRule: (AlertRule) -> Unit,
    onOpenBacktest: (Stock?, RuleType?) -> Unit = { _, _ -> },
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var ruleName by remember {
        mutableStateOf(
            if (initialStock != null && initialRuleType != null) {
                "${initialStock.name} ${initialRuleType.title}"
            } else if (initialStock != null) {
                "${initialStock.name} 조건 알림"
            } else {
                "맞춤형 기술 지표 알림"
            }
        )
    }

    var selectedScope by remember {
        mutableStateOf(if (initialStock != null) RuleScope.SPECIFIC else RuleScope.ALL_KOSPI)
    }

    var selectedStock by remember { mutableStateOf(initialStock ?: stocks.firstOrNull()) }
    var selectedTimeframe by remember { mutableStateOf(ChartTimeframe.M5) }
    var selectedCategory by remember {
        mutableStateOf(initialRuleType?.category ?: RuleCategory.MOVING_AVERAGE)
    }
    var selectedRuleType by remember {
        mutableStateOf(initialRuleType ?: RuleType.MA_GOLDEN_CROSS)
    }

    var thresholdValue by remember {
        mutableDoubleStateOf(
            when (selectedRuleType) {
                RuleType.PRICE_ABOVE, RuleType.PRICE_BELOW -> (selectedStock?.currentPrice ?: 70000.0) * 1.05
                RuleType.CHANGE_RATE_SURGE, RuleType.CHANGE_RATE_PLUNGE -> 3.0
                RuleType.RSI_OVERSOLD -> 30.0
                RuleType.RSI_OVERBOUGHT -> 70.0
                RuleType.VOLUME_SURGE -> 2.0
                else -> 0.0
            }
        )
    }

    var param1 by remember { mutableIntStateOf(5) } // Fast MA
    var param2 by remember { mutableIntStateOf(20) } // Slow MA
    var cooldownMinutes by remember { mutableIntStateOf(30) }

    // Instant test results
    var testScanResults by remember { mutableStateOf<List<Pair<Stock, String>>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "알림 규칙 생성 (Rule Builder)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("rule_builder_back_btn")) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onOpenBacktest(selectedStock, selectedRuleType) },
                        modifier = Modifier.testTag("rule_builder_backtest_action_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoGraph,
                            contentDescription = "백테스트",
                            tint = BrandPrimaryLight
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            testScanResults = dataManager.runPresetScan(
                                ruleType = selectedRuleType,
                                threshold = thresholdValue,
                                param1 = param1,
                                param2 = param2
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.weight(1f).testTag("test_rule_btn")
                    ) {
                        Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null, tint = AccentGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("조건 테스트", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            val newRule = AlertRule(
                                name = ruleName.ifBlank { "${selectedStock?.name ?: selectedScope.displayName} ${selectedRuleType.title}" },
                                scope = selectedScope,
                                targetSymbol = if (selectedScope == RuleScope.SPECIFIC) selectedStock?.symbol ?: "" else "",
                                targetSymbolName = if (selectedScope == RuleScope.SPECIFIC) selectedStock?.name ?: "" else "",
                                timeframe = selectedTimeframe,
                                ruleType = selectedRuleType,
                                thresholdValue = thresholdValue,
                                param1 = param1,
                                param2 = param2,
                                cooldownMinutes = cooldownMinutes
                            )
                            onSaveRule(newRule)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1.3f).testTag("save_rule_submit_btn")
                    ) {
                        Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("규칙 저장 및 활성화", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Rule Name
            OutlinedTextField(
                value = ruleName,
                onValueChange = { ruleName = it },
                label = { Text("알림 규칙 이름") },
                placeholder = { Text("예: 삼성전자 5일선 골든크로스") },
                modifier = Modifier.fillMaxWidth().testTag("rule_name_input"),
                singleLine = true
            )

            // Step 1: Target Scope
            SectionCard(title = "1. 감시 대상 범위 (Target Scope)") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RuleScope.values().forEach { scope ->
                        FilterChip(
                            selected = selectedScope == scope,
                            onClick = { selectedScope = scope },
                            label = { Text(scope.displayName, fontSize = 12.sp) },
                            modifier = Modifier.testTag("scope_chip_${scope.name}")
                        )
                    }
                }

                if (selectedScope == RuleScope.SPECIFIC) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("종목 선택:", fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        stocks.forEach { stock ->
                            val isChosen = selectedStock?.symbol == stock.symbol
                            Surface(
                                onClick = {
                                    selectedStock = stock
                                    if (selectedRuleType == RuleType.PRICE_ABOVE || selectedRuleType == RuleType.PRICE_BELOW) {
                                        thresholdValue = stock.currentPrice
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isChosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.testTag("select_stock_${stock.symbol}")
                            ) {
                                Text(
                                    text = stock.name,
                                    fontSize = 11.sp,
                                    fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isChosen) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Step 2: Timeframe (감시 주기)
            SectionCard(title = "2. 감시 주기 (Monitoring Timeframe)") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ChartTimeframe.values().forEach { tf ->
                        val isSelected = selectedTimeframe == tf
                        Surface(
                            onClick = { selectedTimeframe = tf },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f).testTag("timeframe_btn_${tf.name}")
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                                Text(
                                    text = tf.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Step 3: Rule Category & Condition Type
            SectionCard(title = "3. 발동 조건 유형 (Trigger Condition)") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RuleCategory.values().forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = {
                                selectedCategory = cat
                                val firstInCat = RuleType.values().firstOrNull { it.category == cat }
                                if (firstInCat != null) selectedRuleType = firstInCat
                            },
                            label = { Text(cat.title, fontSize = 12.sp) },
                            modifier = Modifier.testTag("category_chip_${cat.name}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))

                // Condition types in selected category
                val availableTypes = RuleType.values().filter { it.category == selectedCategory }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    availableTypes.forEach { type ->
                        val isSelected = selectedRuleType == type
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedRuleType = type
                                    when (type) {
                                        RuleType.PRICE_ABOVE, RuleType.PRICE_BELOW -> {
                                            thresholdValue = (selectedStock?.currentPrice ?: 70000.0)
                                        }
                                        RuleType.CHANGE_RATE_SURGE, RuleType.CHANGE_RATE_PLUNGE -> thresholdValue = 3.0
                                        RuleType.RSI_OVERSOLD -> thresholdValue = 30.0
                                        RuleType.RSI_OVERBOUGHT -> thresholdValue = 70.0
                                        RuleType.VOLUME_SURGE -> thresholdValue = 2.0
                                        else -> {}
                                    }
                                }
                                .testTag("rule_type_card_${type.name}"),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = type.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else TextPrimary
                                    )
                                    Text(
                                        text = type.templateText,
                                        fontSize = 11.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else TextSecondary
                                    )
                                }
                                if (isSelected) {
                                    Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }

            // Step 4: Parameter fine-tuning
            SectionCard(title = "4. 조건 상세 값 설정 (Parameters)") {
                when (selectedRuleType) {
                    RuleType.PRICE_ABOVE, RuleType.PRICE_BELOW -> {
                        OutlinedTextField(
                            value = thresholdValue.toLong().toString(),
                            onValueChange = { thresholdValue = it.toDoubleOrNull() ?: thresholdValue },
                            label = { Text("목표 기준 가격 (원)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("price_threshold_input")
                        )
                    }
                    RuleType.CHANGE_RATE_SURGE, RuleType.CHANGE_RATE_PLUNGE -> {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("기준 등락률: ", fontSize = 13.sp, color = TextSecondary)
                                Text("%.1f%%".format(thresholdValue), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = StockRed)
                            }
                            Slider(
                                value = thresholdValue.toFloat(),
                                onValueChange = { thresholdValue = it.toDouble() },
                                valueRange = 1f..30f,
                                steps = 28,
                                modifier = Modifier.testTag("change_rate_slider")
                            )
                        }
                    }
                    RuleType.MA_GOLDEN_CROSS, RuleType.MA_DEAD_CROSS -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = param1.toString(),
                                onValueChange = { param1 = it.toIntOrNull() ?: param1 },
                                label = { Text("단기 이평 (봉)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f).testTag("ma_fast_input")
                            )
                            OutlinedTextField(
                                value = param2.toString(),
                                onValueChange = { param2 = it.toIntOrNull() ?: param2 },
                                label = { Text("장기 이평 (봉)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f).testTag("ma_slow_input")
                            )
                        }
                    }
                    RuleType.RSI_OVERSOLD, RuleType.RSI_OVERBOUGHT -> {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("RSI(14) 기준 임계값: ", fontSize = 13.sp, color = TextSecondary)
                                Text("%.0f".format(thresholdValue), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AccentGold)
                            }
                            Slider(
                                value = thresholdValue.toFloat(),
                                onValueChange = { thresholdValue = it.toDouble() },
                                valueRange = 10f..90f,
                                steps = 79,
                                modifier = Modifier.testTag("rsi_slider")
                            )
                        }
                    }
                    RuleType.VOLUME_SURGE -> {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("평균 거래량 대비 배율: ", fontSize = 13.sp, color = TextSecondary)
                                Text("%.1f배 폭증".format(thresholdValue), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BrandPrimary)
                            }
                            Slider(
                                value = thresholdValue.toFloat(),
                                onValueChange = { thresholdValue = it.toDouble() },
                                valueRange = 1.2f..10f,
                                steps = 44,
                                modifier = Modifier.testTag("volume_slider")
                            )
                        }
                    }
                    else -> {
                        Text("선택한 조건은 자동으로 캔들 차트의 볼린저밴드 수식에 따라 산출됩니다.", fontSize = 12.sp, color = TextSecondary)
                    }
                }
            }

            // Step 5: Cooldown configuration
            SectionCard(title = "5. 중복 알림 방지 쿨다운 (Cooldown)") {
                Text("동일한 조건이 연속으로 발동될 때 최소 알림 간격을 설정합니다.", fontSize = 11.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(5, 15, 30, 60, 1440).forEach { mins ->
                        val label = if (mins < 60) "${mins}분" else if (mins == 60) "1시간" else "1일"
                        val isSelected = cooldownMinutes == mins
                        Surface(
                            onClick = { cooldownMinutes = mins },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f).testTag("cooldown_chip_$mins")
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Instant Test Result Area
            if (testScanResults != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("test_scan_results_container"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Filled.Tune, contentDescription = null, tint = AccentGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "실시간 조건 테스트 결과: ${testScanResults!!.size}개 종목 포착",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (testScanResults!!.isEmpty()) {
                            Text("현재 시점에서 이 조건에 즉시 부합하는 종목이 없습니다. (시세 변동 시 자동 감지)", fontSize = 12.sp, color = TextMuted)
                        } else {
                            testScanResults!!.forEach { (stock, msg) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(stock.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text(msg, fontSize = 11.sp, color = StockRed)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}
