package com.example.puzzle_pieces.utils

import android.app.Activity
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowInsets

class MeasureScreen {
    fun getScreenSizeExcludingStatusBar(activity: Activity): Pair<Int, Int> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = activity.windowManager.currentWindowMetrics
            val insets = windowMetrics.windowInsets
                .getInsetsIgnoringVisibility(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            val bounds = windowMetrics.bounds
            val width = bounds.width()
            val height = bounds.height() - insets.top - insets.bottom // Trừ cả status và navigation bar
            Pair(width, height)
        } else {
            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels

            // Trừ cả status bar và navigation bar
            val statusBarId = activity.resources.getIdentifier("status_bar_height", "dimen", "android")
            val statusBarHeight = if (statusBarId > 0)
                activity.resources.getDimensionPixelSize(statusBarId) else 0

            val navBarId = activity.resources.getIdentifier("navigation_bar_height", "dimen", "android")
            val navBarHeight = if (navBarId > 0)
                activity.resources.getDimensionPixelSize(navBarId) else 0

            val usableHeight = screenHeight - statusBarHeight - navBarHeight
            Pair(screenWidth, usableHeight)
        }
    }

}