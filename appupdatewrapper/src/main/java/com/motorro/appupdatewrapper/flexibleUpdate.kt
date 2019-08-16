/*
 * Copyright 2019 Nikolai Kotchetkov (motorro).
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */

package com.motorro.appupdatewrapper

import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleOwner
import com.google.android.play.core.appupdate.AppUpdateManager
import timber.log.Timber

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
    Timber.tag(trimmedTag("$LIBRARY_LOG_PREFIX:startImmediateUpdate")).d("Starting FLEXIBLE update flow...")
    FlexibleUpdateState.start(it)
}
