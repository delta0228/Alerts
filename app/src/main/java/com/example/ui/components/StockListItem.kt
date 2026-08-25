package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MarketType
import com.example.model.Stock
import com.example.ui.theme.AccentGold
import com.example.ui.theme.StockBlue
import com.example.ui.theme.StockRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun StockListItem(
    stock: Stock,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onQuickAddAlert: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("stock_item_${stock.symbol}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Favorite star & Name/Symbol/Market
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(36.dp).testTag("fav_btn_${stock.symbol}")
                ) {
                    Icon(
                        imageVector = if (stock.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "관심종목 토글",
                        tint = if (stock.isFavorite) AccentGold else TextMuted
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stock.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = when (stock.market) {
                                MarketType.KOSPI -> Color(0xFF1E3A8A)
                                MarketType.KOSDAQ -> Color(0xFF065F46)
                                MarketType.US -> Color(0xFF581C87)
                                else -> Color(0xFF334155)
                            }
                        ) {
                            Text(
                                text = stock.market.name,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stock.symbol,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Middle: Mini Sparkline
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(28.dp)
                    .padding(horizontal = 4.dp)
            ) {
                val isUp = stock.isRising
                Canvas(modifier = Modifier.fillMaxWidth().height(28.dp)) {
                    val path = Path()
                    val w = size.width
                    val h = size.height
                    if (isUp) {
                        path.moveTo(0f, h * 0.8f)
                        path.lineTo(w * 0.3f, h * 0.6f)
                        path.lineTo(w * 0.6f, h * 0.7f)
                        path.lineTo(w, h * 0.15f)
                    } else {
                        path.moveTo(0f, h * 0.2f)
                        path.lineTo(w * 0.3f, h * 0.4f)
                        path.lineTo(w * 0.6f, h * 0.3f)
                        path.lineTo(w, h * 0.85f)
                    }
                    drawPath(
                        path = path,
                        color = if (isUp) StockRed else StockBlue,
                        style = Stroke(width = 2f)
                    )
                }
            }

            // Right: Price, Change Rate Badge, Quick Alert button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    val priceText = if (stock.market == MarketType.US) {
                        "$%,.2f".format(stock.currentPrice)
                    } else {
                        "%,.0f원".format(stock.currentPrice)
                    }
                    Text(
                        text = priceText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    val changeText = "%+,.2f%%".format(stock.changeRate)
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (stock.isRising) StockRed.copy(alpha = 0.15f) else if (stock.isFalling) StockBlue.copy(alpha = 0.15f) else Color.Transparent
                    ) {
                        Text(
                            text = changeText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (stock.isRising) StockRed else if (stock.isFalling) StockBlue else TextSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = onQuickAddAlert,
                    modifier = Modifier.size(32.dp).testTag("quick_alert_btn_${stock.symbol}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsActive,
                        contentDescription = "알림 설정",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
