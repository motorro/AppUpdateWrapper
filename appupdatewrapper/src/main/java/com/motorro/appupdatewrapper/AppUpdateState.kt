package com.motorro.appupdatewrapper

/**
 * Application update state interface
 */
abstract class AppUpdateState {
    /**
     * Update host
     * @see AppUpdateHost.setState
     */
    internal lateinit var host: AppUpdateHost

    /**
     * Executes [block] if view available
     */
    protected fun ifViewAvailable(block: AppUpdateView.() -> Unit) {
        host.getView()?.block()
    }

    /**
     * Handles lifecycle `onStart`
     */
    internal open fun onStart() = Unit

    /**
     * Handles lifecycle `onStop`
     */
    internal open fun onStop() = Unit

    /**
     * Handles lifecycle `onPause`
     */
    internal open fun onPause() = Unit

    /**
     * Handles lifecycle `onResume`
     */
    internal open fun onResume() = Unit

    /**
     * Checks activity result and returns `true` if result is an update result and was handled
     * Use to check update activity result in [android.app.Activity.onActivityResult]
     */
    open fun checkActivityResult(requestCode: Int, resultCode: Int): Boolean = false

    /**
     * Cancels update installation
     * Call when update is downloaded and user cancelled app restart
     * Effective if update is called with [com.google.android.play.core.install.model.AppUpdateType.FLEXIBLE]
     */
    open fun userCanceledUpdate() = Unit

    /**
     * Completes update
     * Call when update is downloaded and user confirmed app restart
     * Effective if update is called with [com.google.android.play.core.install.model.AppUpdateType.FLEXIBLE]
     */
    open fun userConfirmedUpdate() = Unit
}