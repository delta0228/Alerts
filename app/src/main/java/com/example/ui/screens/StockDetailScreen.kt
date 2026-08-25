package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StockDataManager
import com.example.model.AlertRule
import com.example.model.ChartTimeframe
import com.example.model.MarketType
import com.example.model.Stock
import com.example.ui.components.CandlestickChart
import com.example.ui.theme.AccentGold
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandPrimaryLight
import com.example.ui.theme.IndicatorMA5
import com.example.ui.theme.IndicatorRSI
import com.example.ui.theme.StockBlue
import com.example.ui.theme.StockRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockDetailScreen(
    stock: Stock,
    dataManager: StockDataManager,
    activeRulesForStock: List<AlertRule>,
    onToggleRule: (Long, Boolean) -> Unit,
    onDeleteRule: (Long) -> Unit,
    onAddAlert: () -> Unit,
    onOpenBacktest: (Stock) -> Unit = {},
    onToggleFavorite: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTimeframe by remember { mutableStateOf(ChartTimeframe.M5) }
    val candles = remember(stock, selectedTimeframe) {
        dataManager.getCandles(stock.symbol, selectedTimeframe)
    }

    val latestCandle = candles.lastOrNull()
    val rsiVal = latestCandle?.indicators?.rsi14 ?: 50.0
    val ma5Val = latestCandle?.indicators?.ma5
    val ma20Val = latestCandle?.indicators?.ma20

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stock.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stock.symbol,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("stock_detail_back_btn")) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.testTag("detail_fav_btn")) {
                        Icon(
                            imageVector = if (stock.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "관심",
                            tint = if (stock.isFavorite) AccentGold else TextSecondary
                        )
                    }
                    IconButton(onClick = onAddAlert, modifier = Modifier.testTag("detail_add_alert_action_btn")) {
                        Icon(
                            imageVector = Icons.Filled.AddAlert,
                            contentDescription = "알림 생성",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
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
            // Live Price & Daily Stats Header
            Card(
                modifier = Modifier.fillMaxWidth().testTag("stock_price_header_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            val priceStr = if (stock.market == MarketType.US) "$%,.2f".format(stock.currentPrice) else "%,.0f원".format(stock.currentPrice)
                            Text(
                                text = priceStr,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val changePriceStr = if (stock.market == MarketType.US) "%+,.2f".format(stock.changePrice) else "%+,.0f".format(stock.changePrice)
                                Text(
                                    text = "$changePriceStr (%+,.2f%%)".format(stock.changeRate),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (stock.isRising) StockRed else if (stock.isFalling) StockBlue else TextSecondary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when (stock.market) {
                                MarketType.KOSPI -> Color(0xFF1E3A8A)
                                MarketType.KOSDAQ -> Color(0xFF065F46)
                                MarketType.US -> Color(0xFF581C87)
                                else -> Color(0xFF334155)
                            }
                        ) {
                            Text(
                                text = stock.market.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Grid stats (시가/고가/저가/거래량)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StatColumn(label = "시가", value = "%,.0f".format(stock.openPrice))
                        StatColumn(label = "고가", value = "%,.0f".format(stock.highPrice), color = StockRed)
                        StatColumn(label = "저가", value = "%,.0f".format(stock.lowPrice), color = StockBlue)
                        StatColumn(label = "거래량", value = "%,d".format(stock.volume))
                    }
                }
            }

            // Interactive Candlestick Chart
            CandlestickChart(
                stock = stock,
                candles = candles,
                selectedTimeframe = selectedTimeframe,
                onTimeframeSelected = { selectedTimeframe = it }
            )

            // Technical Indicator Diagnostic Card
            Card(
                modifier = Modifier.fillMaxWidth().testTag("indicator_diagnostic_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "기술적 지표 진단 요약",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // RSI Diagnostic
                        val rsiStatus = when {
                            rsiVal >= 70 -> "과열 (과매수)" to StockRed
                            rsiVal <= 30 -> "침체 (과매도)" to AccentGold
                            else -> "중립 구간" to TextSecondary
                        }
                        IndicatorDiagnosticItem(
                            title = "RSI(14)",
                            value = "%.1f".format(rsiVal),
                            status = rsiStatus.first,
                            statusColor = rsiStatus.second
                        )

                        // MA Trend Diagnostic
                        val trendStatus = if (ma5Val != null && ma20Val != null) {
                            if (ma5Val > ma20Val) "단기 상승세 (정배열)" to StockRed else "단기 조정세 (역배열)" to StockBlue
                        } else {
                            "이평선 분석 중" to TextSecondary
                        }
                        IndicatorDiagnosticItem(
                            title = "5/20 이평 배열",
                            value = if (ma5Val != null) "%,.0f / %,.0f".format(ma5Val, ma20Val ?: 0.0) else "-",
                            status = trendStatus.first,
                            statusColor = trendStatus.second
                        )
                    }
                }
            }

            // Active Alert Rules for this Stock
            Card(
                modifier = Modifier.fillMaxWidth().testTag("stock_active_rules_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "설정된 감시 알림 (${activeRulesForStock.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )

                        IconButton(
                            onClick = onAddAlert,
                            modifier = Modifier.size(28.dp).testTag("add_rule_for_stock_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AddAlert,
                                contentDescription = "추가",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (activeRulesForStock.isEmpty()) {
                        Text(
                            text = "이 종목에 등록된 알림이 없습니다. 상단 또는 아래 버튼을 눌러 목표가나 이평선 골든크로스 알림을 등록해보세요.",
                            fontSize = 12.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            activeRulesForStock.forEach { rule ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = rule.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "${rule.timeframe.label} | ${rule.formattedSummary()}",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Switch(
                                            checked = rule.isEnabled,
                                            onCheckedChange = { onToggleRule(rule.id, it) },
                                            modifier = Modifier.size(36.dp).testTag("rule_switch_${rule.id}")
                                        )

                                        IconButton(
                                            onClick = { onDeleteRule(rule.id) },
                                            modifier = Modifier.size(32.dp).testTag("delete_rule_btn_${rule.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Delete,
                                                contentDescription = "삭제",
                                                tint = TextMuted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quick Actions CTA Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { onOpenBacktest(stock) },
                    modifier = Modifier.weight(1f).height(48.dp).testTag("cta_backtest_btn"),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrandPrimaryLight)
                ) {
                    Icon(imageVector = Icons.Filled.AutoGraph, contentDescription = null, tint = BrandPrimaryLight, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("백테스트", color = BrandPrimaryLight, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onAddAlert,
                    modifier = Modifier.weight(1.4f).height(48.dp).testTag("cta_add_alert_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Filled.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("알림 만들기", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String, color: Color = TextPrimary) {
    Column {
        Text(text = label, fontSize = 11.sp, color = TextSecondary)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun IndicatorDiagnosticItem(
    title: String,
    value: String,
    status: String,
    statusColor: Color
) {
    Column {
        Text(text = title, fontSize = 11.sp, color = TextSecondary)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(text = status, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = statusColor)
    }
}
