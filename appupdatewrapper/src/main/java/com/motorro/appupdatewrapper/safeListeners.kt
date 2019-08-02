package com.motorro.appupdatewrapper

import android.os.Handler
import android.os.Looper
import android.os.Looper.*
import androidx.annotation.VisibleForTesting
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.motorro.appupdatewrapper.AppUpdateWrapper.Companion.USE_SAFE_LISTENERS

/**
 * Safely registers listener
 * Originally, updating a list of subscribers within event dispatching crashes AppUpdateManager dispatcher
 * with concurrent update exception.
 * See test files for explanation
 * @param listener Update state listener
 */
internal fun AppUpdateManager.registerListenerSafe(listener: InstallStateUpdatedListener) {
    if (USE_SAFE_LISTENERS) {
        doRegisterListenerSafe(listener)
    } else {
        registerListener(listener)
    }
}

/**
 * Registers listener in next loop
 */
@VisibleForTesting
internal fun AppUpdateManager.doRegisterListenerSafe(listener: InstallStateUpdatedListener) {
    Handler(getMainLooper()).post { registerListener(listener) }
}

/**
 * Safely unregisters listener
 * Originally, updating a list of subscribers within event dispatching crashes AppUpdateManager dispatcher
 * with concurrent update exception.
 * See test files for explanation
 * @param listener Update state listener
 */
internal fun AppUpdateManager.unregisterListenerSafe(listener: InstallStateUpdatedListener) {
    if (USE_SAFE_LISTENERS) {
        doUnregisterListenerSafe(listener)
    } else {
        unregisterListener(listener)
    }
}

/**
 * Unregisters listener in next loop
 */
@VisibleForTesting
internal fun AppUpdateManager.doUnregisterListenerSafe(listener: InstallStateUpdatedListener) {
    Handler(getMainLooper()).post { unregisterListener(listener) }
}
