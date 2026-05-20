package com.msu.mfalocker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Start the foreground monitoring service on boot.
            // No notification-listener check needed — ListenerService uses UsageStatsManager,
            // not the NotificationListenerService API.
            val serviceIntent = Intent(context, ListenerService::class.java)
            context.startService(serviceIntent)
        }
    }
}