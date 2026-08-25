package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.model.AlertHistory

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "stock_alerts_channel"
        const val CHANNEL_NAME = "주식 조건 알림 (Stock Alert)"
        const val CHANNEL_DESC = "사용자가 설정한 기술적 지표 및 가격 도달 알림"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                setShowBadge(true)
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendStockAlertNotification(alert: AlertHistory) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_SYMBOL", alert.symbol)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            alert.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val priceStr = "%,.0f원".format(alert.triggeredPrice)
        val rateStr = "%+,.2f%%".format(alert.changeRate)
        val title = "[${alert.stockName}] ${alert.ruleName} 감지!"
        val content = "$priceStr ($rateStr) | ${alert.message}"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(alert.id.toInt(), builder.build())
        } catch (e: SecurityException) {
            // Permission not granted on Android 13+ yet
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
