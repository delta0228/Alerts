package com.example.ui.screens

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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AlertHistory
import com.example.model.Stock
import com.example.ui.theme.AccentGold
import com.example.ui.theme.StockBlue
import com.example.ui.theme.StockRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AlertHistoryScreen(
    histories: List<AlertHistory>,
    stocks: List<Stock>,
    onSelectStock: (Stock) -> Unit,
    onMarkAsRead: (Long) -> Unit,
    onMarkAllAsRead: () -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .testTag("alert_history_screen")
    ) {
        // Top Header & Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "발동 알림 내역",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                val unread = histories.count { !it.isRead }
                Text(
                    text = "총 ${histories.size}건 (미확인 ${unread}건)",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (histories.any { !it.isRead }) {
                    IconButton(onClick = onMarkAllAsRead, modifier = Modifier.size(36.dp).testTag("mark_all_read_btn")) {
                        Icon(imageVector = Icons.Filled.DoneAll, contentDescription = "모두 읽음", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                if (histories.isNotEmpty()) {
                    IconButton(onClick = onClearAll, modifier = Modifier.size(36.dp).testTag("clear_history_btn")) {
                        Icon(imageVector = Icons.Filled.DeleteSweep, contentDescription = "전체 삭제", tint = TextMuted)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (histories.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsNone,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("아직 발동된 알림 내역이 없습니다.", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("설정한 조건이 충족되면 여기에 실시간 기록됩니다.", fontSize = 12.sp, color = TextSecondary)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(histories, key = { it.id }) { alert ->
                    val dateStr = SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.KOREA).format(Date(alert.timestamp))
                    val relatedStock = stocks.find { it.symbol == alert.symbol }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onMarkAsRead(alert.id)
                                if (relatedStock != null) onSelectStock(relatedStock)
                            }
                            .testTag("history_item_${alert.id}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (!alert.isRead) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (!alert.isRead) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(StockRed, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = alert.stockName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = AccentGold.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = alert.ruleName,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AccentGold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = dateStr,
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = alert.message,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "발동가: %,.0f원".format(alert.triggeredPrice),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "(%+,.2f%%)".format(alert.changeRate),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (alert.changeRate >= 0) StockRed else StockBlue
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "차트 보기",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
