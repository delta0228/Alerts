package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StockDataManager
import com.example.model.MarketType
import com.example.model.RuleType
import com.example.model.Stock
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkOledBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.IndicatorMA5
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.StockBlue
import com.example.ui.theme.StockRed
import com.example.ui.theme.TabularRateBadge
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

data class ScanPreset(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val iconColor: Color,
    val ruleType: RuleType,
    val threshold: Double,
    val param1: Int = 5,
    val param2: Int = 20
)

@Composable
fun ConditionScannerScreen(
    dataManager: StockDataManager,
    onSelectStock: (Stock) -> Unit,
    onAddAlertForRule: (Stock, RuleType) -> Unit,
    modifier: Modifier = Modifier
) {
    val presets = remember {
        listOf(
            ScanPreset("p1", "골든크로스 발생", "5일 이평선이 20일 이평선을 상향 돌파한 종목", Icons.Filled.TrendingUp, NeonGreen, RuleType.MA_GOLDEN_CROSS, 0.0, 5, 20),
            ScanPreset("p2", "RSI 30 이하 과매도", "단기 과매도 구간으로 기술적 반등 유력 종목", Icons.Filled.Bolt, NeonAmber, RuleType.RSI_OVERSOLD, 35.0),
            ScanPreset("p3", "거래량 2배 폭증", "최근 20일 평균 대비 거래량이 200% 이상 급증", Icons.Filled.ElectricBolt, NeonCyan, RuleType.VOLUME_SURGE, 1.8),
            ScanPreset("p4", "볼린저밴드 하단 터치", "하단 밴드 지지선에서 저가 매수세 유입 구간", Icons.Filled.FilterAlt, NeonCyan, RuleType.BOLLINGER_LOWER_TOUCH, 0.0),
            ScanPreset("p5", "당일 +2.5% 이상 급등", "장중 강한 모멘텀으로 전일 대비 급등 중인 종목", Icons.Filled.TrendingUp, NeonGreen, RuleType.CHANGE_RATE_SURGE, 2.0),
            ScanPreset("p6", "데드크로스 경고", "5일선이 20일선을 하향 이탈한 하락 전환 종목", Icons.Filled.Warning, NeonRed, RuleType.MA_DEAD_CROSS, 0.0, 5, 20)
        )
    }

    var selectedPreset by remember { mutableStateOf(presets.first()) }
    var scanResults by remember { mutableStateOf<List<Pair<Stock, String>>>(emptyList()) }

    // Run initial scan
    remember(selectedPreset) {
        scanResults = dataManager.runPresetScan(
            selectedPreset.ruleType,
            selectedPreset.threshold,
            selectedPreset.param1,
            selectedPreset.param2
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkOledBackground)
            .padding(16.dp)
            .testTag("condition_scanner_screen")
    ) {
        // Header
        Text(
            text = "실시간 온디바이스 조건 검색기",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "기술적 지표 및 가격 돌파 조건을 만족하는 종목을 기기 내부에서 실시간 탐색합니다.",
            fontSize = 12.sp,
            color = TextSecondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        // Presets horizontal list
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            items(presets) { preset ->
                val isSelected = preset.id == selectedPreset.id
                Card(
                    modifier = Modifier
                        .width(180.dp)
                        .clickable {
                            selectedPreset = preset
                            scanResults = dataManager.runPresetScan(
                                preset.ruleType,
                                preset.threshold,
                                preset.param1,
                                preset.param2
                            )
                        }
                        .testTag("preset_card_${preset.id}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) DarkSurfaceVariant else DarkSurface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) NeonCyan else DarkBorder
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = preset.icon,
                                contentDescription = null,
                                tint = preset.iconColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = preset.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isSelected) NeonCyan else TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = preset.description,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 2
                        )
                    }
                }
            }
        }

        // Action & Scan Status Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "포착 결과: ",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                Text(
                    text = "${scanResults.size}개 종목",
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
            }

            Button(
                onClick = {
                    scanResults = dataManager.runPresetScan(
                        selectedPreset.ruleType,
                        selectedPreset.threshold,
                        selectedPreset.param1,
                        selectedPreset.param2
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                modifier = Modifier.testTag("refresh_scan_btn")
            ) {
                Icon(imageVector = Icons.Filled.Search, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("실시간 재스캔", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        // Matched Stock List
        if (scanResults.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "현재 조건에 부합하는 종목이 없습니다.",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "시세가 변동되면 자동으로 조건이 재검사됩니다.",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(scanResults) { (stock, message) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectStock(stock) }
                            .testTag("scan_result_item_${stock.symbol}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = stock.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = DarkSurfaceVariant
                                    ) {
                                        Text(
                                            text = stock.market.name,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = NeonCyan,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = message,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = NeonGreen
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "%,.0f원".format(stock.currentPrice),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "%+,.2f%%".format(stock.changeRate),
                                    style = TabularRateBadge,
                                    color = if (stock.isRising) NeonGreen else NeonRed
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Surface(
                                    onClick = { onAddAlertForRule(stock, selectedPreset.ruleType) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = NeonCyan
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(imageVector = Icons.Filled.NotificationsActive, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("알림 등록", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

