package com.msu.mfalocker

import android.view.View
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test for task 5.4:
 * AppAdaptor sets systemBadge VISIBLE for system apps and GONE for user apps.
 *
 * Uses ActivityScenarioRule to get a real Activity context for view inflation.
 */
// Feature: system-app-lock
@RunWith(AndroidJUnit4::class)
class AppAdaptorSystemBadgeTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    /**
     * 5.4a: systemBadge is VISIBLE when app.isSystem == true
     */
    @Test
    fun systemBadge_isVisible_forSystemApp() {
        activityRule.scenario.onActivity { activity ->
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val icon = context.getDrawable(android.R.drawable.sym_def_app_icon)!!

            val systemApp = MainActivity.App(
                appName = "Settings",
                packageName = "com.android.settings",
                icon = icon,
                status = "Unlocked",
                isSystem = true
            )

            val appList = arrayListOf(systemApp)
            val lockedList = arrayListOf<String>()
            val adaptor = AppAdaptor(activity, appList, "", lockedList, null)

            // Attach to a real RecyclerView so ViewHolder can be created and bound
            val recyclerView = androidx.recyclerview.widget.RecyclerView(activity)
            recyclerView.layoutManager =
                androidx.recyclerview.widget.LinearLayoutManager(activity)
            recyclerView.adapter = adaptor

            // Force measure/layout so ViewHolder is created and bind() is called
            recyclerView.measure(
                View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(2400, View.MeasureSpec.EXACTLY)
            )
            recyclerView.layout(0, 0, 1080, 2400)

            val holder = recyclerView.findViewHolderForAdapterPosition(0)
            val badge = holder!!.itemView.findViewById<View>(R.id.systemBadge)
            assertEquals(View.VISIBLE, badge.visibility)
        }
    }

    /**
     * 5.4b: systemBadge is GONE when app.isSystem == false
     */
    @Test
    fun systemBadge_isGone_forUserApp() {
        activityRule.scenario.onActivity { activity ->
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val icon = context.getDrawable(android.R.drawable.sym_def_app_icon)!!

            val userApp = MainActivity.App(
                appName = "WhatsApp",
                packageName = "com.whatsapp",
                icon = icon,
                status = "Unlocked",
                isSystem = false
            )

            val appList = arrayListOf(userApp)
            val lockedList = arrayListOf<String>()
            val adaptor = AppAdaptor(activity, appList, "", lockedList, null)

            val recyclerView = androidx.recyclerview.widget.RecyclerView(activity)
            recyclerView.layoutManager =
                androidx.recyclerview.widget.LinearLayoutManager(activity)
            recyclerView.adapter = adaptor

            recyclerView.measure(
                View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(2400, View.MeasureSpec.EXACTLY)
            )
            recyclerView.layout(0, 0, 1080, 2400)

            val holder = recyclerView.findViewHolderForAdapterPosition(0)
            val badge = holder!!.itemView.findViewById<View>(R.id.systemBadge)
            assertEquals(View.GONE, badge.visibility)
        }
    }
}
