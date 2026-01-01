package com.dev.mandadito.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dev.mandadito.R
import com.dev.mandadito.data.local.database.MandaditoDatabase
import com.dev.mandadito.data.local.entities.NotificationEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "inventory_channel"
        const val CHANNEL_NAME = "Notificaciones de Inventario"
        const val CHANNEL_DESC = "Alertas sobre stock y pedidos"

        const val LOW_STOCK_ID = 1
        const val OUT_OF_STOCK_ID = 2
        const val PENDING_ORDERS_ID = 3
    }

    private val database = MandaditoDatabase.getDatabase(context)
    private val notificationDao = database.notificationDao()
    private val sharedPrefs = SharedPreferenHelper(context)

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESC
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showLowStockNotification(lowStockItems: List<String>) {
        if (lowStockItems.isEmpty()) return

        val title = "Stock Bajo"
        val text = "Productos con stock bajo: ${lowStockItems.take(3).joinToString(", ")}${if(lowStockItems.size > 3) "..." else ""}"

        // Mostrar notificación en el dispositivo
        showNotification(LOW_STOCK_ID, title, text)

        // Guardar en base de datos
        saveNotificationToDatabase(
            type = "LOW_STOCK",
            title = title,
            message = "Productos con stock bajo: ${lowStockItems.joinToString(", ")}",
            isPush = true
        )

        android.util.Log.d("NotificationHelper", "Notificación de stock bajo guardada: ${lowStockItems.size} productos")
    }

    fun showOutOfStockNotification(outOfStockItems: List<String>) {
        if (outOfStockItems.isEmpty()) return
        val title = "Sin Stock"
        val text = "Productos agotados: ${outOfStockItems.take(3).joinToString(", ")}${if(outOfStockItems.size > 3) "..." else ""}"
        showNotification(OUT_OF_STOCK_ID, title, text)

        saveNotificationToDatabase(
            type = "OUT_OF_STOCK",
            title = title,
            message = "Productos: ${outOfStockItems.joinToString(", ")}",
            isPush = true
        )
    }

    fun showPendingOrdersNotification(pendingCount: Int) {
        if (pendingCount == 0) return
        val title = "Pedidos Pendientes"
        val text = "Tienes $pendingCount pedidos pendientes por atender."
        showNotification(PENDING_ORDERS_ID, title, text)

        saveNotificationToDatabase(
            type = "NEW_ORDER",
            title = title,
            message = text,
            isPush = true
        )
    }

    private fun saveNotificationToDatabase(
        type: String,
        title: String,
        message: String,
        isPush: Boolean
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = sharedPrefs.getUserId() ?: return@launch

                val notification = NotificationEntity(
                    userId = userId,
                    type = type,
                    title = title,
                    message = message,
                    timestamp = System.currentTimeMillis(),
                    isRead = false,
                    isPush = isPush
                )

                notificationDao.insertNotification(notification)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showNotification(id: Int, title: String, text: String) {
        try {
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)

            with(NotificationManagerCompat.from(context)) {
                notify(id, builder.build())
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}