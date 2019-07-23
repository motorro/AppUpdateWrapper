package com.motorro.appupdatewrapper

import androidx.lifecycle.Lifecycle
import com.google.android.play.core.appupdate.AppUpdateManager

/**
 * Wraps [AppUpdateManager] interaction into [Lifecycle]-aware component
 * - Starts update check on start
 * - Terminates on stop
 */
class AppUpdateWrapper(private val lifecycle: Lifecycle, private val appUpdateManager: AppUpdateManager) {
}