package com.motorro.appupdatewrapper

import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleOwner
import com.google.android.play.core.appupdate.AppUpdateManager

/**
 * Starts immediate update
 * Use when your app version is deprecated somehow (say your API returns some `version not supported` error
 * and user needs to update the app immediately.
 *
 * * No user interaction supported

 * * Calls [AppUpdateView.updateComplete] when update complete (but you may not receive it as your app is restarted
 * during update installation)
 * * Calls [AppUpdateView.updateFailed] if update is not available or failed to install
 * @receiver Lifecycle owner (activity or fragment)
 * @param appUpdateManager AppUpdateManager instance
 * @param view Application update UI interaction view
 */
@MainThread
fun LifecycleOwner.startImmediateUpdate(appUpdateManager: AppUpdateManager, view: AppUpdateView): AppUpdateWrapper =
    AppUpdateLifecycleStateMachine(lifecycle, appUpdateManager, view).also {
        ImmediateUpdateState.start(it)
    }
