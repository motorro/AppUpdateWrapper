package com.motorro.appupdatewrapper

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.play.core.appupdate.AppUpdateManager

/**
 * App update view host
 */
abstract class AppUpdateHost {
    /**
     * AppUpdateManager instance
     */
    abstract val updateManager: AppUpdateManager

    /**
     * Get update view
     */
    abstract fun getView(): AppUpdateView?

    protected abstract val lifecycleOwner: LifecycleOwner

    protected abstract var currentState: AppUpdateState

    /**
     * Sets new update state
     */
    internal fun setState(newState: AppUpdateState) {
        newState.host = this
        currentState = newState

        with(lifecycleOwner.lifecycle.currentState) {
            if (isAtLeast(Lifecycle.State.STARTED)) {
                newState.onStart()
            }
            if (isAtLeast(Lifecycle.State.RESUMED)) {
                newState.onResume()
            }
        }
    }
}