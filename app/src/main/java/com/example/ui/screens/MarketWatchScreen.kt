package com.example.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MarketType
import com.example.model.Stock
import com.example.ui.components.OrderSide
import com.example.ui.components.QuickOrderBottomSheet
import com.example.ui.components.StockListItem
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkOledBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.StockBlue
import com.example.ui.theme.StockRed
import com.example.ui.theme.TabularPriceLarge
import com.example.ui.theme.TabularPriceMedium
import com.example.ui.theme.TabularRateBadge
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun MarketWatchScreen(
    stocks: List<Stock>,
    isMonitoring: Boolean,
    onToggleMonitoring: () -> Unit,
    onSelectStock: (Stock) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onQuickAddAlert: (Stock) -> Unit,
    onOpenRuleBuilder: () -> Unit,
    onOpenConditionScanner: () -> Unit,
    onOpenBacktest: () -> Unit = {},
    onOpenDocs: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var activeQuickOrderStock by remember { mutableStateOf<Stock?>(null) }

    val tabCategories = listOf("전체", "코스피", "코스닥", "해외(US)", "관심종목")

    val filteredStocks = remember(stocks, selectedTabIndex, searchQuery) {
        stocks.filter { stock ->
            val matchesTab = when (selectedTabIndex) {
                0 -> true
                1 -> stock.market == MarketType.KOSPI
                2 -> stock.market == MarketType.KOSDAQ
                3 -> stock.market == MarketType.US
                4 -> stock.isFavorite
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                    stock.name.contains(searchQuery, ignoreCase = true) ||
                    stock.symbol.contains(searchQuery, ignoreCase = true)
            matchesTab && matchesSearch
        }
    }

    // Pulse animation for live monitoring dot
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        )
    )

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenRuleBuilder,
                containerColor = NeonCyan,
                contentColor = Color.Black,
                modifier = Modifier.testTag("fab_create_rule_main")
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "새 조건 알림 생성")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(DarkOledBackground)
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .testTag("market_watch_screen")
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 1. Top Status & Action Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Live Monitoring Badge
                Surface(
                    onClick = onToggleMonitoring,
                    shape = RoundedCornerShape(20.dp),
                    color = if (isMonitoring) NeonGreen.copy(alpha = 0.15f) else DarkSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isMonitoring) NeonGreen else DarkBorder),
                    modifier = Modifier.testTag("monitoring_status_badge")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (isMonitoring) NeonGreen.copy(alpha = pulseAlpha) else TextMuted,
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isMonitoring) "실시간 온디바이스 감시 중" else "시세 감시 일시정지",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isMonitoring) NeonGreen else TextMuted
                        )
                    }
                }

                // Right: Fast Action Buttons (Backtest, Scanner & Architecture Docs)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        onClick = onOpenBacktest,
                        shape = RoundedCornerShape(10.dp),
                        color = DarkSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier.testTag("open_backtest_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoGraph,
                                contentDescription = "백테스트",
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "백테스트",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }

                    Surface(
                        onClick = onOpenConditionScanner,
                        shape = RoundedCornerShape(10.dp),
                        color = DarkSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier.testTag("open_scanner_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = "조건 검색기",
                                tint = NeonAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "조건검색",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }

                    IconButton(
                        onClick = onOpenDocs,
                        modifier = Modifier.size(36.dp).testTag("open_docs_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Description,
                            contentDescription = "설계 명세",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Bento Grid Header (Total Asset & Real-time P&L + Market Heatmap Strip)
            BentoSummaryCard(
                totalAsset = "₩128,450,000",
                dailyPnl = "+₩3,420,000 (+2.73%)",
                isPnlUp = true,
                monitoredCount = stocks.size,
                onQuickTradeClick = {
                    activeQuickOrderStock = stocks.firstOrNull()
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Market Heatmap Intensity Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface)
                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MarketHeatmapTile(title = "KOSPI", value = "2,764.12", change = "+1.42%", isUp = true, modifier = Modifier.weight(1f))
                MarketHeatmapTile(title = "KOSDAQ", value = "872.45", change = "-0.85%", isUp = false, modifier = Modifier.weight(1f))
                MarketHeatmapTile(title = "NASDAQ", value = "17,842.1", change = "+2.14%", isUp = true, modifier = Modifier.weight(1f))
                MarketHeatmapTile(title = "S&P 500", value = "5,482.8", change = "+0.89%", isUp = true, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("종목명 또는 종목코드 검색 (예: 삼성전자, NVDA)", fontSize = 12.sp, color = TextMuted) },
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.Search, contentDescription = null, tint = TextMuted)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Filled.Clear, contentDescription = "Clear", tint = TextMuted)
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = DarkBorder,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface
                ),
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("stock_search_bar"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 5. Market Tabs
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = NeonCyan,
                indicator = { tabPositions ->
                    SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = NeonCyan
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                tabCategories.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTabIndex == index) NeonCyan else TextSecondary
                            )
                        },
                        modifier = Modifier.testTag("market_tab_$index")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 6. Stock list with Tabular Monospace & Sparklines
            if (filteredStocks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("검색 조건에 맞는 종목이 없습니다.", fontSize = 14.sp, color = TextMuted)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredStocks, key = { it.symbol }) { stock ->
                        StockListItem(
                            stock = stock,
                            onClick = { onSelectStock(stock) },
                            onToggleFavorite = { onToggleFavorite(stock.symbol) },
                            onQuickAddAlert = { onQuickAddAlert(stock) },
                            onQuickTrade = { activeQuickOrderStock = stock }
                        )
                    }
                }
            }
        }

        // Quick Order Bottom Sheet Modal
        activeQuickOrderStock?.let { stock ->
            QuickOrderBottomSheet(
                stock = stock,
                onDismiss = { activeQuickOrderStock = null },
                onExecuteOrder = { targetStock, side, price, qty ->
                    // Order executed callback
                }
            )
        }
    }
}

/**
 * Bento Grid Total Asset & Real-time P&L Card
 */
@Composable
private fun BentoSummaryCard(
    totalAsset: String,
    dailyPnl: String,
    isPnlUp: Boolean,
    monitoredCount: Int,
    onQuickTradeClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth().testTag("bento_summary_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("총 운용 평가 자산", fontSize = 11.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = totalAsset,
                        style = TabularPriceLarge,
                        color = TextPrimary
                    )
                }

                Surface(
                    onClick = onQuickTradeClick,
                    shape = RoundedCornerShape(10.dp),
                    color = NeonGreen.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(alpha = 0.4f)),
                    modifier = Modifier.testTag("bento_quick_trade_btn")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Bolt, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("퀵 매매", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurfaceVariant, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("당일 실현 손익: ", fontSize = 11.sp, color = TextMuted)
                    Text(
                        text = dailyPnl,
                        style = TabularRateBadge,
                        color = if (isPnlUp) NeonGreen else NeonRed
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("온디바이스 감시: ", fontSize = 11.sp, color = TextMuted)
                    Text(
                        text = "${monitoredCount}종목",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                }
            }
        }
    }
}

/**
 * Market Heatmap Mini Tile
 */
@Composable
private fun MarketHeatmapTile(
    title: String,
    value: String,
    change: String,
    isUp: Boolean,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isUp) NeonGreen.copy(alpha = 0.12f) else NeonRed.copy(alpha = 0.12f)
    val color = if (isUp) NeonGreen else NeonRed

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
            Text(text = value, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(text = change, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

