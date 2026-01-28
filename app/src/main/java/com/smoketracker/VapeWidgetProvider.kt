package com.smoketracker

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.widget.RemoteViews
import java.time.Duration
import java.time.LocalDateTime

class VapeWidgetProvider : AppWidgetProvider() {
    
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
        // Schedule periodic updates
        scheduleUpdates(context)
    }
    
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // Start periodic updates when first widget is added
        scheduleUpdates(context)
    }
    
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // Cancel updates when last widget is removed
        cancelUpdates(context)
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        when (intent.action) {
            ACTION_VAPE_CLICK -> {
                // Log a smoke
                val repository = SmokeRepository(context)
                repository.logSmoke()
                
                // Update all widgets
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, VapeWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                
                for (appWidgetId in appWidgetIds) {
                    updateWidget(context, appWidgetManager, appWidgetId)
                }
            }
            ACTION_WIDGET_UPDATE -> {
                // Just update the display
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, VapeWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                
                for (appWidgetId in appWidgetIds) {
                    updateWidget(context, appWidgetManager, appWidgetId)
                }
            }
        }
    }
    
    companion object {
        const val ACTION_VAPE_CLICK = "com.smoketracker.ACTION_VAPE_CLICK"
        const val ACTION_WIDGET_UPDATE = "com.smoketracker.ACTION_WIDGET_UPDATE"
        
        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val repository = SmokeRepository(context)
            val smokesToday = repository.getSmokesToday()
            val lastSmokeTime = repository.getLastSmokeTime()
            val timeSinceLastSmoke = calculateTimeSince(lastSmokeTime)
            
            val views = RemoteViews(context.packageName, R.layout.widget_vape)
            
            // Set the data
            views.setTextViewText(R.id.widget_count, smokesToday.toString())
            views.setTextViewText(R.id.widget_time, timeSinceLastSmoke)
            
            // Set click action to log smoke
            val vapeIntent = Intent(context, VapeWidgetProvider::class.java).apply {
                action = ACTION_VAPE_CLICK
            }
            val vapePendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                vapeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, vapePendingIntent)
            
            // Update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
        
        private fun calculateTimeSince(lastSmokeTime: LocalDateTime?): String {
            if (lastSmokeTime == null) {
                return "--:--"
            }
            
            val now = LocalDateTime.now()
            val duration = Duration.between(lastSmokeTime, now)
            
            val hours = duration.toHours()
            val minutes = duration.toMinutes() % 60
            
            return String.format("%02d:%02d", hours, minutes)
        }
        
        fun requestUpdate(context: Context) {
            val intent = Intent(context, VapeWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_UPDATE
            }
            context.sendBroadcast(intent)
        }
        
        private fun scheduleUpdates(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, VapeWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_UPDATE
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                1,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Update every minute
            alarmManager.setRepeating(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + 60000,
                60000,
                pendingIntent
            )
        }
        
        private fun cancelUpdates(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, VapeWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_UPDATE
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                1,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }
}
