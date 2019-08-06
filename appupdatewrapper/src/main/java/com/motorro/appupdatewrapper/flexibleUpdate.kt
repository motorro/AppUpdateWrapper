package com.motorro.appupdatewrapper

import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleOwner
import com.google.android.play.core.appupdate.AppUpdateManager

/**
 * Starts flexible update
 * Use to check for updates parallel to main application flow.
 *
 * If update found gets [AppUpdateView.activity] and starts play-core update consent on behalf of your activity.
 * Therefore you should pass an activity result to the [AppUpdateWrapper.checkActivityResult] for check.
 * Whenever the update is downloaded wrapper will call [AppUpdateView.updateReady]. At this point your view
 * should ask if user is ready to restart application.
 * Then call one of the continuation methods: [AppUpdateWrapper.userConfirmedUpdate] or [AppUpdateWrapper.userCanceledUpdate]
 *
 * * Calls [AppUpdateView.updateComplete] when update complete (but you may not receive it as your app is restarted
 * during update installation)
 * * Calls [AppUpdateView.updateFailed] if update is not available or failed to install
 * @receiver Lifecycle owner (activity or fragment)
 * @param appUpdateManager AppUpdateManager instance
 * @param view Application update UI interaction view
 * @param flowBreaker Terminates update flow if user has already cancelled an update. Use to not to bother user with
 * update popups
 * @see UpdateFlowBreaker.withInterval
 * @see UpdateFlowBreaker.forOneDay
 */
@MainThread
fun LifecycleOwner.startFlexibleUpdate(
    appUpdateManager: AppUpdateManager,
    view: AppUpdateView,
    flowBreaker: UpdateFlowBreaker
): AppUpdateWrapper = AppUpdateLifecycleStateMachine(lifecycle, appUpdateManager, view, flowBreaker).also {
    FlexibleUpdateState.start(it)
}
