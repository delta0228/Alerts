package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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

@Composable
fun ArchitectureDocsDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 680.dp)
                .testTag("architecture_docs_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.AccountTree,
                            contentDescription = "Architecture",
                            tint = NeonCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "시스템 아키텍처 & 설계 명세",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp).testTag("close_docs_dialog_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "닫기",
                            tint = TextSecondary
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = DarkBorder
                )

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Section 1: Recommended Tech Stack
                    DocSectionCard(
                        icon = Icons.Filled.Dns,
                        iconTint = NeonCyan,
                        title = "1. 추천 시스템 아키텍처 & 기술 스택"
                    ) {
                        Text(
                            text = "• 클라이언트 (모바일): Android Kotlin + Jetpack Compose (M3), Room Local DB, Coroutines & Flow, WorkManager / Foreground Service\n" +
                                    "• 백엔드 (서버): Spring Boot (Kotlin) 또는 FastAPI (Python) - 마이크로서비스 구조\n" +
                                    "• 데이터 수집 파이프라인: 한국투자증권 Open API / 키움 OpenAPI / Yahoo Finance / WebSocket 실시간 체결가 수신\n" +
                                    "• 시계열 DB: TimescaleDB (PostgreSQL 기반) 또는 RedisTimeSeries (밀리초 단위 캔들 캐싱)\n" +
                                    "• 메시지 큐 & 스케줄러: Apache Kafka (실시간 틱 스트림 분배), Celery / Quartz Scheduler / Redis Streams\n" +
                                    "• 푸시 알림 서버: Firebase Cloud Messaging (FCM) + APNs (대량 토큰 병렬 발송)",
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = TextSecondary
                        )
                    }

                    // Section 2: Core Data Models & Schemas
                    DocSectionCard(
                        icon = Icons.Filled.Storage,
                        iconTint = NeonAmber,
                        title = "2. 핵심 데이터 모델 및 스키마 설계"
                    ) {
                        CodeBlock(
                            code = """-- AlertRule (조건 검색 규칙 테이블)
CREATE TABLE alert_rules (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    rule_name VARCHAR(100) NOT NULL,
    scope VARCHAR(20) NOT NULL, -- SPECIFIC, KOSPI, KOSDAQ, US, FAVORITES
    target_symbol VARCHAR(20),
    timeframe VARCHAR(10) NOT NULL, -- 1M, 5M, 15M, 1H, 1D
    rule_type VARCHAR(30) NOT NULL, -- GOLDEN_CROSS, RSI_OVERSOLD 등
    threshold_value DOUBLE PRECISION,
    param1 INT DEFAULT 5,  -- 단기 이평
    param2 INT DEFAULT 20, -- 장기 이평
    cooldown_minutes INT DEFAULT 30,
    is_enabled BOOLEAN DEFAULT TRUE,
    last_triggered_at BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- AlertHistory (발동 이력 테이블)
CREATE TABLE alert_histories (
    id BIGSERIAL PRIMARY KEY,
    rule_id BIGINT REFERENCES alert_rules(id) ON DELETE CASCADE,
    symbol VARCHAR(20) NOT NULL,
    stock_name VARCHAR(100) NOT NULL,
    triggered_price DOUBLE PRECISION NOT NULL,
    change_rate DOUBLE PRECISION NOT NULL,
    message TEXT NOT NULL,
    timestamp BIGINT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE
);"""
                        )
                    }

                    // Section 3: Rule Evaluation & Indicator Formulas
                    DocSectionCard(
                        icon = Icons.Filled.Code,
                        iconTint = NeonGreen,
                        title = "3. 온디바이스 지표 계산 및 조건 검사 엔진"
                    ) {
                        Text(
                            text = "1. 골든크로스 / 데드크로스 검증:\n" +
                                    "   이전 봉(t-1)에서 MA_fast ≤ MA_slow 이고, 현재 봉(t)에서 MA_fast > MA_slow 일 때 골든크로스 판정.\n\n" +
                                    "2. RSI(14) 계산:\n" +
                                    "   Wilder's Smoothing을 적용한 14봉 평균 상승분(AvgGain)과 평균 하락분(AvgLoss) 계산. RS = AvgGain / AvgLoss, RSI = 100 - (100 / (1 + RS)).\n\n" +
                                    "3. 볼린저 밴드 (20, 2):\n" +
                                    "   20일 SMA ± 2 * 표준편차(StdDev). 하단선 접촉 시 반등, 상단선 돌파 시 모멘텀 포착.\n\n" +
                                    "4. 쿨다운 타임(Cooldown Engine):\n" +
                                    "   (현재시간 - lastTriggeredAt) < cooldownMinutes * 60 * 1000 인 경우 알림 중복 발송을 자동 억제.",
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = TextSecondary
                        )
                    }

                    // Section 4: Development Roadmap Guidelines
                    DocSectionCard(
                        icon = Icons.Filled.Schedule,
                        iconTint = NeonRed,
                        title = "4. 단계별 구현 가이드라인"
                    ) {
                        Text(
                            text = "Phase 1: 데이터 파이프라인 구축 (OpenAPI 연동 및 OHLCV 캔들 집계 엔진)\n" +
                                    "Phase 2: 온디바이스 기술적 보조지표 연산 엔진 (SMA, EMA, RSI, MACD, Bollinger Bands)\n" +
                                    "Phase 3: 룰 빌더 UI 및 룰 평가 엔진 (단일 종목 및 전시장 멀티스캔)\n" +
                                    "Phase 4: 백그라운드 워커 및 푸시 알림 (WorkManager, Notification Channel, FCM)\n" +
                                    "Phase 5: 인터랙티브 캔들 차트 및 스크러빙/툴팁 최적화 (Canvas GPU 렌더링)",
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    modifier = Modifier.fillMaxWidth().testTag("confirm_docs_btn")
                ) {
                    Text("확인 및 닫기", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DocSectionCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun CodeBlock(code: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = DarkOledBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = code,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            color = Color(0xFFE2E8F0),
            modifier = Modifier.padding(10.dp)
        )
    }
}
