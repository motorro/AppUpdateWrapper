package com.motorro.appupdatewrapper

import androidx.lifecycle.Lifecycle
import com.google.android.play.core.appupdate.AppUpdateManager

/**
 * Wraps [AppUpdateManager] interaction
 */
interface AppUpdateWrapper {
    /**
     * Checks activity result and returns `true` if result is an update result and was handled
     * Use to check update activity result in [android.app.Activity.onActivityResult]
     */
    fun checkActivityResult(requestCode: Int, resultCode: Int): Boolean = false

    /**
     * Cancels update installation
     * Call when update is downloaded and user cancelled app restart
     * Effective if update is called with [com.google.android.play.core.install.model.AppUpdateType.FLEXIBLE]
     */
    fun userCanceledUpdate() = Unit

    /**
     * Completes update
     * Call when update is downloaded and user confirmed app restart
     * Effective if update is called with [com.google.android.play.core.install.model.AppUpdateType.FLEXIBLE]
     */
    fun userConfirmedUpdate() = Unit
}