package com.motorro.appupdatewrapper

import androidx.lifecycle.LifecycleOwner
import com.google.android.play.core.appupdate.AppUpdateManager

/**
 * Starts immediate update
 * Use when your app version is deprecated somehow (say your API returns some `version not supported` error
 * and user needs to update the app immediately.
 *
 * * No user interaction supported
 * * Calls [AppUpdateView.updateComplete] when updateComplete (but you may not receive it as your app is restarted during update)
 * * Calls [AppUpdateView.updateFailed] if update is not available or failed to install
 * @receiver Lifecycle owner (activity or fragment)
 * @param appUpdateManager AppUpdateManager instance
 * @param view Application update UI interaction view
 */
fun LifecycleOwner.startImmediateUpdate(appUpdateManager: AppUpdateManager, view: AppUpdateView): AppUpdateWrapper =
    AppUpdateLifecycleStateMachine(lifecycle, appUpdateManager, view).also {
        ImmediateUpdateState.start(it)
    }
