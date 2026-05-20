@file:Suppress("DEPRECATION")

package com.msu.mfalocker
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Handler
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.io.File

class ListenerService : Service() {
    private lateinit var lockedAppList : ArrayList<String>
    private lateinit var lastApp: File
    private lateinit var lockTypeStore: LockTypeStore
    private var pendingReset = false

    private fun createNotificationChannel() {
        val channel = NotificationChannel("foreground", "Foreground Services", NotificationManager.IMPORTANCE_LOW).apply {
            description = "To inform the user that the app is running in the background."
        }
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "foreground")
            .setContentTitle("Notification Listener")
            .setContentText("Monitoring notifications")
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setOngoing(true)
            .build()
    }

    @SuppressLint("ForegroundServiceType", "HardwareIds")
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)

        lockTypeStore = LockTypeStore(filesDir)
        val lockedFile = File(filesDir, "locked.txt")
        lockedAppList = if (lockedFile.exists() && lockedFile.readText().isNotEmpty())
            ArrayList(lockedFile.readText().split(","))
        else ArrayList()
        lastApp = File(filesDir, "lastApp.txt")
        lastApp.writeText("none") // always reset on service (re)start
        checkForegroundApp()
    }

    private fun checkForegroundApp() {
        val usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val currentTime = System.currentTimeMillis()

        Handler().postDelayed({ checkForegroundApp() }, 500)

        // Reload locked list on every tick so changes from MainActivity are picked up
        val lockedFile = File(filesDir, "locked.txt")
        if (lockedFile.exists()) {
            val text = lockedFile.readText()
            lockedAppList = if (text.isNotEmpty()) ArrayList(text.split(",")) else ArrayList()
        }

        val usageStatsList: List<UsageStats> = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, currentTime - 1000, currentTime)

        if (usageStatsList.isNotEmpty()) {
            val recentUsageStats = usageStatsList.maxByOrNull { it.lastTimeUsed }
            val pkg = recentUsageStats?.packageName

            if (recentUsageStats != null) {
                // Don't interfere while our own locker UI is in the foreground
                if (pkg == packageName) return

                if (pkg != null && lockedAppList.contains(pkg)) {
                    pendingReset = false
                    if (lastApp.readText() != pkg) {
                        val lockType = lockTypeStore.resolveEffectiveLockType(pkg)
                        val intent = Intent(this, LockActivity::class.java)
                            .putExtra(LockActivity.EXTRA_PACKAGE_NAME, pkg)
                            .putExtra(LockActivity.EXTRA_LOCK_TYPE, lockType.name)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        startActivity(intent)
                    }
                }
                // Only reset lastApp when a clearly different, stable app comes to foreground.
                // Do NOT reset during transient system UI / recents / our own package —
                // these appear briefly during unlock transitions and would re-trigger the lock.
                else if (pkg != null
                    && !isLauncher(pkg)
                    && pkg != packageName
                    && !isTransientSystemPackage(pkg)
                    && pkg != lastApp.readText()
                ) {
                    if (pendingReset) {
                        lastApp.writeText("none")
                        pendingReset = false
                    } else {
                        pendingReset = true
                    }
                }
            }
            else lastApp.writeText("none")
        }
    }

    private fun isLauncher(pkg: String): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = packageManager.resolveActivity(intent, 0)
        val defaultLauncher = resolveInfo?.activityInfo?.packageName
        return pkg == defaultLauncher
    }

    /**
     * Returns true for transient system packages that briefly appear during
     * app-to-app transitions (recents, system UI, etc.) and should NOT cause
     * lastApp to be reset — otherwise the unlock guard is cleared mid-transition
     * and the lock fires again immediately.
     */
    private fun isTransientSystemPackage(pkg: String): Boolean {
        return pkg == "com.android.systemui"
            || pkg == "com.android.launcher"
            || pkg.startsWith("com.android.launcher")
            || pkg == "com.google.android.apps.nexuslauncher"
            || pkg == "com.sec.android.app.launcher"   // Samsung
            || pkg == "com.miui.home"                  // Xiaomi
            || pkg == "com.huawei.android.launcher"    // Huawei
            || pkg == "com.oppo.launcher"              // OPPO
            || pkg == "com.vivo.launcher"              // Vivo
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSelf()
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
}