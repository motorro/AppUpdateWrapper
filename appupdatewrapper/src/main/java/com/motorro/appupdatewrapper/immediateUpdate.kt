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
        Timber.d("Starting immediate update flow...")
        ImmediateUpdateState.start(it)
    }
