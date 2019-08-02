package com.motorro.appupdatewrapper

import android.app.Activity
import androidx.lifecycle.Lifecycle.State.RESUMED

/**
 * Application update view that performs user interaction.
 * [AppUpdateWrapper] works as a presenter for [AppUpdateView] calling methods
 * appropriate for concrete update flow.
 * All view methods are called within [RESUMED] state
 * to make your view stack happy.
 * The update process may end in calling one of two methods:
 * * [updateComplete] - update check has completed without **critical** errors (although [nonCriticalUpdateError] may be
 *   called during the workflow)
 * * [updateFailed] - a critical error has occurred during the update. Say the immediate update fails and the application
 *   should be terminated
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
     * Called when update is checking or downloading data
     * Subclasses may display some spinner at this point.
     * Remove spinner in:
     * - [updateReady]
     * - [updateComplete]
     * - [updateFailed]
     */
    fun updateChecking() = Unit

    /**
     * Reports update is downloaded and ready to be installed
     * When ready to proceed call [AppUpdateState.userConfirmedUpdate]
     * @see AppUpdateState.userConfirmedUpdate
     * @see AppUpdateState.userCanceledUpdate
     */
    fun updateReady()

    /**
     * Called when installation activity launches
     * Calling this handler means the application will be restarted if update succeeds
     * Subclass may want to finish it's current activity at this point
     */
    fun updateInstallUiVisible() = Unit

    /**
     * No update available or update flow completed
     */
    fun updateComplete() = Unit

    /**
     * Critical update error occurred e.g. when immediate update was requested but failed to proceed
     */
    fun updateFailed(e: Throwable)

    /**
     * Notify user of some non-critical update error e.g. flexible update has failed but it is not critical for
     * general application flow.
     */
    fun nonCriticalUpdateError(e: Throwable) = Unit
}
