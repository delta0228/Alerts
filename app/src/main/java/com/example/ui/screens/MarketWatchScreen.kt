package com.example.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StockDataManager
import com.example.model.MarketType
import com.example.model.Stock
import com.example.ui.components.StockListItem
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.StockBlue
import com.example.ui.theme.StockRed
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
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_create_rule_main")
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "새 조건 알림 생성")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .testTag("market_watch_screen")
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Top Status & Action Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Live Monitoring Badge
                Surface(
                    onClick = onToggleMonitoring,
                    shape = RoundedCornerShape(20.dp),
                    color = if (isMonitoring) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
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
                                    if (isMonitoring) AccentGreen.copy(alpha = pulseAlpha) else TextMuted,
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isMonitoring) "실시간 시세 감시 중" else "시세 감시 일시정지",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isMonitoring) MaterialTheme.colorScheme.onPrimaryContainer else TextMuted
                        )
                    }
                }

                // Right: Fast Action Buttons (Backtest, Scanner & Architecture Docs)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        onClick = onOpenBacktest,
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.testTag("open_backtest_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoGraph,
                                contentDescription = "백테스트",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "백테스트",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Surface(
                        onClick = onOpenConditionScanner,
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.testTag("open_scanner_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = "조건 검색기",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "조건검색",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
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
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Market Index Ticker Strip
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth().testTag("market_index_strip")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MarketIndexItem(name = "코스피", value = "2,764.12", change = "+1.42%", isUp = true)
                    MarketIndexItem(name = "코스닥", value = "872.45", change = "-0.85%", isUp = false)
                    MarketIndexItem(name = "나스닥", value = "17,842.1", change = "+2.14%", isUp = true)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("종목명 또는 종목코드 검색 (예: 삼성전자, 005930)", fontSize = 12.sp) },
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
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("stock_search_bar"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Market Tabs
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = MaterialTheme.colorScheme.primary
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
                                color = if (selectedTabIndex == index) TextPrimary else TextSecondary
                            )
                        },
                        modifier = Modifier.testTag("market_tab_$index")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Stock list
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
                            onQuickAddAlert = { onQuickAddAlert(stock) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MarketIndexItem(name: String, value: String, change: String, isUp: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = name, fontSize = 11.sp, color = TextSecondary)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(text = change, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isUp) StockRed else StockBlue)
    }
}
