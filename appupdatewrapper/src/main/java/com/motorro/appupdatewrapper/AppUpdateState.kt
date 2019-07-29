package com.motorro.appupdatewrapper

/**
 * Application update state interface
 */
internal abstract class AppUpdateState: AppUpdateWrapper {
    /**
     * Update stateMachine
     * @see AppUpdateStateMachine.setUpdateState
     */
    internal lateinit var stateMachine: AppUpdateStateMachine

    /**
     * Executes [block] with update view
     */
    protected fun withUpdateView(block: AppUpdateView.() -> Unit) {
        stateMachine.view.block()
    }

    /**
     * Handles lifecycle `onStart`
     */
    open fun onStart() = Unit

    /**
     * Handles lifecycle `onStop`
     */
    open fun onStop() = Unit

    /**
     * Handles lifecycle `onPause`
     */
    open fun onPause() = Unit

    /**
     * Handles lifecycle `onResume`
     */
    open fun onResume() = Unit

    /**
     * Checks activity result and returns `true` if result is an update result and was handled
     * Use to check update activity result in [android.app.Activity.onActivityResult]
     */
    override fun checkActivityResult(requestCode: Int, resultCode: Int): Boolean = false

    /**
     * Cancels update installation
     * Call when update is downloaded and user cancelled app restart
     * Effective if update is called with [com.google.android.play.core.install.model.AppUpdateType.FLEXIBLE]
     */
    override fun userCanceledUpdate() = Unit

    /**
     * Completes update
     * Call when update is downloaded and user confirmed app restart
     * Effective if update is called with [com.google.android.play.core.install.model.AppUpdateType.FLEXIBLE]
     */
    override fun userConfirmedUpdate() = Unit
}

/**
 * Default update state that does nothing
 */
internal object NONE: AppUpdateState()