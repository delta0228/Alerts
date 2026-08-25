package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AlertRule
import com.example.model.RuleScope
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkOledBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RulesListScreen(
    rules: List<AlertRule>,
    onToggleRule: (Long, Boolean) -> Unit,
    onDeleteRule: (Long) -> Unit,
    onAddNewRule: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        containerColor = DarkOledBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddNewRule,
                containerColor = NeonCyan,
                contentColor = Color.Black,
                modifier = Modifier.testTag("fab_add_rule")
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "규칙 추가")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(DarkOledBackground)
                .padding(innerPadding)
                .padding(16.dp)
                .testTag("rules_list_screen")
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "온디바이스 감시 규칙 관리",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "등록된 ${rules.size}개의 온디바이스 알림 규칙",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DarkSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                ) {
                    val activeCount = rules.count { it.isEnabled }
                    Text(
                        text = "${activeCount}개 활성",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = NeonCyan,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (rules.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsOff,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("등록된 알림 규칙이 없습니다.", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("골든크로스, 목표가 돌파, RSI 과매도 조건을 만들어보세요.", fontSize = 12.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onAddNewRule,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            modifier = Modifier.testTag("empty_add_rule_btn")
                        ) {
                            Text("첫 번째 조건 알림 생성", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(rules, key = { it.id }) { rule ->
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("rule_card_${rule.id}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = when (rule.scope) {
                                                RuleScope.SPECIFIC -> NeonCyan.copy(alpha = 0.2f)
                                                RuleScope.ALL_KOSPI -> Color(0xFF1E3A8A)
                                                RuleScope.ALL_KOSDAQ -> Color(0xFF065F46)
                                                RuleScope.ALL_US -> Color(0xFF581C87)
                                                RuleScope.FAVORITES -> NeonAmber.copy(alpha = 0.2f)
                                            },
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                when (rule.scope) {
                                                    RuleScope.SPECIFIC -> NeonCyan
                                                    RuleScope.FAVORITES -> NeonAmber
                                                    else -> DarkBorder
                                                }
                                            )
                                        ) {
                                            Text(
                                                text = if (rule.scope == RuleScope.SPECIFIC && rule.targetSymbolName.isNotBlank()) rule.targetSymbolName else rule.scope.displayName,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (rule.scope == RuleScope.SPECIFIC) NeonCyan else Color.White,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = rule.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = TextPrimary
                                        )
                                    }

                                    Switch(
                                        checked = rule.isEnabled,
                                        onCheckedChange = { onToggleRule(rule.id, it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.Black,
                                            checkedTrackColor = NeonCyan,
                                            uncheckedThumbColor = TextMuted,
                                            uncheckedTrackColor = DarkSurfaceVariant
                                        ),
                                        modifier = Modifier.testTag("rule_manage_switch_${rule.id}")
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "조건: ${rule.formattedSummary()}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = NeonGreen
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "주기: ${rule.timeframe.label} | 쿨다운: ${rule.cooldownMinutes}분",
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = TextSecondary
                                        )
                                    }

                                    IconButton(
                                        onClick = { onDeleteRule(rule.id) },
                                        modifier = Modifier.size(32.dp).testTag("delete_rule_manage_btn_${rule.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "삭제",
                                            tint = TextMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                if (rule.lastTriggeredAt > 0) {
                                    val dateStr = SimpleDateFormat("MM/dd HH:mm", Locale.KOREA).format(Date(rule.lastTriggeredAt))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Filled.AlarmOn, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("최근 발동: $dateStr", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextMuted)
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
