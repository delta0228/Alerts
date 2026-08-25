package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoubleArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MarketType
import com.example.model.Stock
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkOledBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.StockBlue
import com.example.ui.theme.StockRed
import com.example.ui.theme.TabularOrderBook
import com.example.ui.theme.TabularPriceLarge
import com.example.ui.theme.TabularPriceMedium
import com.example.ui.theme.TabularRateBadge
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class OrderSide {
    BUY, SELL
}

data class OrderBookLevel(
    val price: Double,
    val volume: Int,
    val ratio: Float, // 0.0 ~ 1.0 gauge ratio
    val isAsk: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickOrderBottomSheet(
    stock: Stock,
    onDismiss: () -> Unit,
    onExecuteOrder: (stock: Stock, side: OrderSide, price: Double, quantity: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var selectedSide by remember { mutableStateOf(OrderSide.BUY) }
    var orderPrice by remember { mutableDoubleStateOf(stock.currentPrice) }
    var orderQuantity by remember { mutableIntStateOf(10) }
    var orderExecuted by remember { mutableStateOf(false) }

    // Mock realistic 5-depth order book relative to current price
    val orderBookLevels = remember(stock, orderPrice) {
        val basePrice = stock.currentPrice
        val step = if (stock.market == MarketType.US) 0.5 else if (basePrice > 100000) 500.0 else 100.0
        val asks = listOf(
            OrderBookLevel(basePrice + step * 5, 240, 0.45f, true),
            OrderBookLevel(basePrice + step * 4, 520, 0.75f, true),
            OrderBookLevel(basePrice + step * 3, 380, 0.60f, true),
            OrderBookLevel(basePrice + step * 2, 850, 0.95f, true),
            OrderBookLevel(basePrice + step * 1, 620, 0.80f, true)
        )
        val bids = listOf(
            OrderBookLevel(basePrice, 910, 1.0f, false),
            OrderBookLevel(basePrice - step * 1, 750, 0.85f, false),
            OrderBookLevel(basePrice - step * 2, 430, 0.55f, false),
            OrderBookLevel(basePrice - step * 3, 610, 0.70f, false),
            OrderBookLevel(basePrice - step * 4, 300, 0.40f, false)
        )
        asks to bids
    }

    val totalAmount = orderPrice * orderQuantity

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        scrimColor = Color.Black.copy(alpha = 0.75f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(DarkBorder)
            )
        },
        modifier = modifier.testTag("quick_order_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Header: Stock Info & Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stock.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = DarkSurfaceVariant
                        ) {
                            Text(
                                text = stock.symbol,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = NeonCyan,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "실시간 호가 기반 즉시 체결 주문",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                IconButton(
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                            onDismiss()
                        }
                    },
                    modifier = Modifier.size(32.dp).testTag("close_order_sheet_btn")
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "닫기", tint = TextMuted)
                }
            }

            // 2. Buy / Sell Tab Buttons (High-contrast Neon)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkOledBackground)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Buy Tab (Neon Green)
                val isBuy = selectedSide == OrderSide.BUY
                Surface(
                    onClick = {
                        selectedSide = OrderSide.BUY
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isBuy) NeonGreen else Color.Transparent,
                    modifier = Modifier.weight(1f).testTag("tab_buy")
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
                        Text(
                            text = "매수 (BUY)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isBuy) Color.Black else TextSecondary
                        )
                    }
                }

                // Sell Tab (Electric Red)
                val isSell = selectedSide == OrderSide.SELL
                Surface(
                    onClick = {
                        selectedSide = OrderSide.SELL
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSell) NeonRed else Color.Transparent,
                    modifier = Modifier.weight(1f).testTag("tab_sell")
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
                        Text(
                            text = "매도 (SELL)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isSell) Color.White else TextSecondary
                        )
                    }
                }
            }

            // 3. Mini Order Book (호가창 Depth Gauge)
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = DarkOledBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("호가 (매도잔량)", fontSize = 11.sp, color = TextMuted)
                        Text("수량 잔량 게이지", fontSize = 11.sp, color = TextMuted)
                        Text("호가 (매수잔량)", fontSize = 11.sp, color = TextMuted)
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    // Asks (매도호가 3개)
                    orderBookLevels.first.takeLast(3).forEach { ask ->
                        OrderBookRow(
                            level = ask,
                            isSelected = orderPrice == ask.price,
                            onSelectPrice = {
                                orderPrice = ask.price
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        )
                    }

                    // Divider / Current Price Marker
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(DarkSurfaceVariant, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("현재가", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            text = if (stock.market == MarketType.US) "$%,.2f".format(stock.currentPrice) else "%,.0f원".format(stock.currentPrice),
                            style = TabularPriceMedium,
                            color = if (stock.isRising) NeonGreen else NeonRed
                        )
                        Text(
                            text = "%+,.2f%%".format(stock.changeRate),
                            style = TabularRateBadge,
                            color = if (stock.isRising) NeonGreen else NeonRed
                        )
                    }

                    // Bids (매수호가 3개)
                    orderBookLevels.second.take(3).forEach { bid ->
                        OrderBookRow(
                            level = bid,
                            isSelected = orderPrice == bid.price,
                            onSelectPrice = {
                                orderPrice = bid.price
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        )
                    }
                }
            }

            // 4. Price & Quantity Stepper Control
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Price Stepper
                Card(
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("주문 단가", fontSize = 11.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    val step = if (stock.market == MarketType.US) 0.5 else if (orderPrice > 100000) 500.0 else 100.0
                                    orderPrice = (orderPrice - step).coerceAtLeast(1.0)
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "감소", tint = TextPrimary)
                            }
                            Text(
                                text = if (stock.market == MarketType.US) "$%,.2f".format(orderPrice) else "%,.0f".format(orderPrice),
                                style = TabularPriceMedium,
                                color = TextPrimary
                            )
                            IconButton(
                                onClick = {
                                    val step = if (stock.market == MarketType.US) 0.5 else if (orderPrice > 100000) 500.0 else 100.0
                                    orderPrice += step
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "증가", tint = TextPrimary)
                            }
                        }
                    }
                }

                // Quantity Stepper
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("주문 수량", fontSize = 11.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    orderQuantity = (orderQuantity - 5).coerceAtLeast(1)
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "수량 감소", tint = TextPrimary)
                            }
                            Text(
                                text = "${orderQuantity}주",
                                style = TabularPriceMedium,
                                color = TextPrimary
                            )
                            IconButton(
                                onClick = {
                                    orderQuantity += 5
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "수량 증가", tint = TextPrimary)
                            }
                        }
                    }
                }
            }

            // Quick Percentage Chips (10%, 25%, 50%, 100%)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(10 to "10%", 25 to "25%", 50 to "50%", 100 to "최대").forEach { (qty, label) ->
                    val isSelected = orderQuantity == qty
                    Surface(
                        onClick = {
                            orderQuantity = qty
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) NeonCyan.copy(alpha = 0.2f) else DarkSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) NeonCyan else DarkBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 6.dp)) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) NeonCyan else TextSecondary
                            )
                        }
                    }
                }
            }

            // Total Amount Display
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("총 체결 예상 금액", fontSize = 13.sp, color = TextSecondary)
                Text(
                    text = if (stock.market == MarketType.US) "$%,.2f".format(totalAmount) else "%,.0f원".format(totalAmount),
                    style = TabularPriceLarge,
                    color = if (selectedSide == OrderSide.BUY) NeonGreen else NeonRed
                )
            }

            // 5. 'Slide to Execute' (스와이프하여 즉시 주문) Interactive Slider
            if (!orderExecuted) {
                SlideToExecuteSlider(
                    side = selectedSide,
                    onExecute = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        orderExecuted = true
                        onExecuteOrder(stock, selectedSide, orderPrice, orderQuantity)
                        scope.launch {
                            delay(1200)
                            sheetState.hide()
                            onDismiss()
                        }
                    }
                )
            } else {
                // Success Confirmation State
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (selectedSide == OrderSide.BUY) NeonGreen.copy(alpha = 0.15f) else NeonRed.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedSide == OrderSide.BUY) NeonGreen else NeonRed),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (selectedSide == OrderSide.BUY) NeonGreen else NeonRed,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${stock.name} ${if (selectedSide == OrderSide.BUY) "매수" else "매도"} 체결 완료!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderBookRow(
    level: OrderBookLevel,
    isSelected: Boolean,
    onSelectPrice: () -> Unit
) {
    val bgGauageColor = if (level.isAsk) NeonRed.copy(alpha = 0.15f) else NeonGreen.copy(alpha = 0.15f)
    val textColor = if (level.isAsk) StockRed else StockBlue

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) DarkSurfaceVariant else Color.Transparent)
            .clickable { onSelectPrice() }
            .padding(vertical = 2.dp)
    ) {
        // Horizontal depth gauge bar fill
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(level.ratio)
                .align(if (level.isAsk) Alignment.CenterEnd else Alignment.CenterStart)
                .clip(RoundedCornerShape(3.dp))
                .background(bgGauageColor)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (level.isAsk) {
                Text(
                    text = "%,.0f".format(level.price),
                    style = TabularOrderBook,
                    color = textColor
                )
                Text(
                    text = "${level.volume}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted
                )
                Text("-", fontSize = 11.sp, color = Color.Transparent)
            } else {
                Text("-", fontSize = 11.sp, color = Color.Transparent)
                Text(
                    text = "${level.volume}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted
                )
                Text(
                    text = "%,.0f".format(level.price),
                    style = TabularOrderBook,
                    color = textColor
                )
            }
        }
    }
}

/**
 * Slide-to-Execute interactive drag bar
 */
@Composable
fun SlideToExecuteSlider(
    side: OrderSide,
    onExecute: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    val primaryColor = if (side == OrderSide.BUY) NeonGreen else NeonRed
    val actionText = if (side == OrderSide.BUY) "밀어서 즉시 매수 실행" else "밀어서 즉시 매도 실행"

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(DarkOledBackground)
            .border(1.dp, primaryColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .testTag("slide_to_execute_container")
    ) {
        val widthPx = with(density) { (maxWidth - 58.dp).toPx() }

        // Slider Track Background Gradient
        val progress = (offsetX / widthPx).coerceIn(0f, 1f)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.1f), primaryColor.copy(alpha = 0.4f))
                    )
                )
        )

        // Center Hint Label
        Text(
            text = actionText,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = TextPrimary.copy(alpha = (1f - progress * 0.8f).coerceAtLeast(0.2f)),
            modifier = Modifier.align(Alignment.Center)
        )

        // Draggable Handle Pill
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .size(56.dp)
                .padding(4.dp)
                .clip(CircleShape)
                .background(primaryColor)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        val newOffset = (offsetX + delta).coerceIn(0f, widthPx)
                        if (newOffset != offsetX) {
                            offsetX = newOffset
                        }
                    },
                    onDragStopped = {
                        if (offsetX >= widthPx * 0.85f) {
                            offsetX = widthPx
                            onExecute()
                        } else {
                            offsetX = 0f
                        }
                    }
                )
                .testTag("slide_handle_pill"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.DoubleArrow,
                contentDescription = "밀어서 주문",
                tint = if (side == OrderSide.BUY) Color.Black else Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
