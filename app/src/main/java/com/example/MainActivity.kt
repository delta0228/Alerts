package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.RuleType
import com.example.model.Stock
import com.example.ui.Screen
import com.example.ui.StockAlertViewModel
import com.example.ui.components.QuickAlertBanner
import com.example.ui.screens.AlertHistoryScreen
import com.example.ui.screens.ArchitectureDocsDialog
import com.example.ui.screens.BacktestScreen
import com.example.ui.screens.ConditionScannerScreen
import com.example.ui.screens.MarketWatchScreen
import com.example.ui.screens.RuleBuilderScreen
import com.example.ui.screens.RulesListScreen
import com.example.ui.screens.StockDetailScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: StockAlertViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme(darkTheme = true) {
                StockAlertApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun StockAlertApp(viewModel: StockAlertViewModel) {
    val context = LocalContext.current

    // Request Android 13+ Notification permission
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val stocks by viewModel.stocks.collectAsStateWithLifecycle()
    val isMonitoring by viewModel.isMonitoring.collectAsStateWithLifecycle()
    val latestAlert by viewModel.latestAlert.collectAsStateWithLifecycle()
    val allRules by viewModel.allRules.collectAsStateWithLifecycle()
    val allHistories by viewModel.allHistories.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadCount.collectAsStateWithLifecycle()
    val showDocs by viewModel.showArchitectureDocs.collectAsStateWithLifecycle()

    val showBottomBar = currentScreen !is Screen.StockDetail && currentScreen !is Screen.RuleBuilder

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = com.example.ui.theme.DarkSurface,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("main_bottom_nav_bar")
                ) {
                    NavigationBarItem(
                        selected = currentScreen is Screen.MarketWatch,
                        onClick = { viewModel.navigateTo(Screen.MarketWatch) },
                        icon = { Icon(imageVector = Icons.Filled.ShowChart, contentDescription = "시세") },
                        label = { Text("시세") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = com.example.ui.theme.NeonCyan,
                            unselectedIconColor = com.example.ui.theme.TextMuted,
                            unselectedTextColor = com.example.ui.theme.TextMuted,
                            indicatorColor = com.example.ui.theme.NeonCyan
                        ),
                        modifier = Modifier.testTag("nav_item_market")
                    )

                    NavigationBarItem(
                        selected = currentScreen is Screen.Backtest,
                        onClick = { viewModel.navigateTo(Screen.Backtest()) },
                        icon = { Icon(imageVector = Icons.Filled.AutoGraph, contentDescription = "백테스트") },
                        label = { Text("백테스트") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = com.example.ui.theme.NeonCyan,
                            unselectedIconColor = com.example.ui.theme.TextMuted,
                            unselectedTextColor = com.example.ui.theme.TextMuted,
                            indicatorColor = com.example.ui.theme.NeonCyan
                        ),
                        modifier = Modifier.testTag("nav_item_backtest")
                    )

                    NavigationBarItem(
                        selected = currentScreen is Screen.ConditionScanner,
                        onClick = { viewModel.navigateTo(Screen.ConditionScanner) },
                        icon = { Icon(imageVector = Icons.Filled.Bolt, contentDescription = "조건검색") },
                        label = { Text("조건검색") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = com.example.ui.theme.NeonCyan,
                            unselectedIconColor = com.example.ui.theme.TextMuted,
                            unselectedTextColor = com.example.ui.theme.TextMuted,
                            indicatorColor = com.example.ui.theme.NeonCyan
                        ),
                        modifier = Modifier.testTag("nav_item_scanner")
                    )

                    NavigationBarItem(
                        selected = currentScreen is Screen.RulesList,
                        onClick = { viewModel.navigateTo(Screen.RulesList) },
                        icon = { Icon(imageVector = Icons.Filled.ListAlt, contentDescription = "규칙관리") },
                        label = { Text("규칙관리") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = com.example.ui.theme.NeonCyan,
                            unselectedIconColor = com.example.ui.theme.TextMuted,
                            unselectedTextColor = com.example.ui.theme.TextMuted,
                            indicatorColor = com.example.ui.theme.NeonCyan
                        ),
                        modifier = Modifier.testTag("nav_item_rules")
                    )

                    NavigationBarItem(
                        selected = currentScreen is Screen.AlertHistoryList,
                        onClick = { viewModel.navigateTo(Screen.AlertHistoryList) },
                        icon = {
                            if (unreadCount > 0) {
                                BadgedBox(badge = { Badge { Text("$unreadCount") } }) {
                                    Icon(imageVector = Icons.Filled.Notifications, contentDescription = "알림내역")
                                }
                            } else {
                                Icon(imageVector = Icons.Filled.Notifications, contentDescription = "알림내역")
                            }
                        },
                        label = { Text("알림내역") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = com.example.ui.theme.NeonCyan,
                            unselectedIconColor = com.example.ui.theme.TextMuted,
                            unselectedTextColor = com.example.ui.theme.TextMuted,
                            indicatorColor = com.example.ui.theme.NeonCyan
                        ),
                        modifier = Modifier.testTag("nav_item_history")
                    )
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    is Screen.MarketWatch -> {
                        MarketWatchScreen(
                            stocks = stocks,
                            isMonitoring = isMonitoring,
                            onToggleMonitoring = { viewModel.toggleMonitoring() },
                            onSelectStock = { viewModel.navigateTo(Screen.StockDetail(it)) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onQuickAddAlert = { stock ->
                                viewModel.navigateTo(Screen.RuleBuilder(stock = stock))
                            },
                            onOpenRuleBuilder = {
                                viewModel.navigateTo(Screen.RuleBuilder())
                            },
                            onOpenConditionScanner = {
                                viewModel.navigateTo(Screen.ConditionScanner)
                            },
                            onOpenBacktest = {
                                viewModel.navigateTo(Screen.Backtest())
                            },
                            onOpenDocs = { viewModel.openArchitectureDocs() }
                        )
                    }

                    is Screen.Backtest -> {
                        BacktestScreen(
                            stocks = stocks,
                            dataManager = viewModel.dataManager,
                            initialStock = screen.stock,
                            initialRuleType = screen.ruleType,
                            onSaveRule = { rule -> viewModel.saveRule(rule) },
                            onNavigateBack = { viewModel.navigateTo(Screen.MarketWatch) }
                        )
                    }

                    is Screen.ConditionScanner -> {
                        ConditionScannerScreen(
                            dataManager = viewModel.dataManager,
                            onSelectStock = { viewModel.navigateTo(Screen.StockDetail(it)) },
                            onAddAlertForRule = { stock, ruleType ->
                                viewModel.navigateTo(Screen.RuleBuilder(stock = stock, ruleType = ruleType))
                            }
                        )
                    }

                    is Screen.RulesList -> {
                        RulesListScreen(
                            rules = allRules,
                            onToggleRule = { id, enabled -> viewModel.toggleRule(id, enabled) },
                            onDeleteRule = { id -> viewModel.deleteRule(id) },
                            onAddNewRule = { viewModel.navigateTo(Screen.RuleBuilder()) }
                        )
                    }

                    is Screen.AlertHistoryList -> {
                        AlertHistoryScreen(
                            histories = allHistories,
                            stocks = stocks,
                            onSelectStock = { viewModel.navigateTo(Screen.StockDetail(it)) },
                            onMarkAsRead = { viewModel.markHistoryAsRead(it) },
                            onMarkAllAsRead = { viewModel.markAllHistoriesAsRead() },
                            onClearAll = { viewModel.clearAllHistories() }
                        )
                    }

                    is Screen.StockDetail -> {
                        // Get live version of stock
                        val liveStock = stocks.find { it.symbol == screen.stock.symbol } ?: screen.stock
                        val rulesForStock = allRules.filter {
                            it.targetSymbol == liveStock.symbol || it.scope.name.startsWith("ALL")
                        }
                        StockDetailScreen(
                            stock = liveStock,
                            dataManager = viewModel.dataManager,
                            activeRulesForStock = rulesForStock,
                            onToggleRule = { id, enabled -> viewModel.toggleRule(id, enabled) },
                            onDeleteRule = { id -> viewModel.deleteRule(id) },
                            onAddAlert = { viewModel.navigateTo(Screen.RuleBuilder(stock = liveStock)) },
                            onOpenBacktest = { stock -> viewModel.navigateTo(Screen.Backtest(stock = stock)) },
                            onToggleFavorite = { viewModel.toggleFavorite(liveStock.symbol) },
                            onNavigateBack = { viewModel.navigateTo(Screen.MarketWatch) }
                        )
                    }

                    is Screen.RuleBuilder -> {
                        RuleBuilderScreen(
                            stocks = stocks,
                            dataManager = viewModel.dataManager,
                            initialStock = screen.stock,
                            initialRuleType = screen.ruleType,
                            onSaveRule = { rule -> viewModel.saveRule(rule) },
                            onOpenBacktest = { stock, ruleType ->
                                viewModel.navigateTo(Screen.Backtest(stock = stock, ruleType = ruleType))
                            },
                            onNavigateBack = { viewModel.navigateTo(Screen.MarketWatch) }
                        )
                    }
                }
            }

            // In-app alert notification banner overlay
            QuickAlertBanner(
                alert = latestAlert,
                onDismiss = { viewModel.dismissBanner() },
                onClick = { alert ->
                    viewModel.dismissBanner()
                    viewModel.markHistoryAsRead(alert.id)
                    val stock = stocks.find { it.symbol == alert.symbol }
                    if (stock != null) {
                        viewModel.navigateTo(Screen.StockDetail(stock))
                    }
                }
            )

            // Architecture & Specifications Dialog
            if (showDocs) {
                ArchitectureDocsDialog(onDismiss = { viewModel.closeArchitectureDocs() })
            }
        }
    }
}
