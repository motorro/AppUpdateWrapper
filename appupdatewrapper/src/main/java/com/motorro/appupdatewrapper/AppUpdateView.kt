package com.motorro.appupdatewrapper

import android.app.Activity

/**
 * Application update view that performs user interaction.
 */
interface AppUpdateView {
    /**
     * Returns hosting activity for update process
     * Call [AppUpdateState.checkActivityResult] in [Activity.onActivityResult] to
     * check update status
     * @see AppUpdateState.checkActivityResult
     */
    val activity: Activity

    /**
     * Reports update is downloaded and ready to be installed
     * When ready to proceed call [AppUpdateState.userConfirmedUpdate]
     * @see AppUpdateState.userConfirmedUpdate
     * @see AppUpdateState.userCanceledUpdate
     */
    fun updateReady()

    /**
     * No update available - complete
     */
    fun complete()

    /**
     * Update critical error occurred e.g. when immediate update was requested but failed to proceed
     */
    fun fail(e: Throwable)

    /**
     * Notify user of update check error or just [complete]
     */
    fun reportError(e: Throwable)
}
