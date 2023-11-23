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

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultRegistry
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
     * Returns result contract registry
     * Wrapper will register an activity result contract to listen to update state
     * Pass [ComponentActivity.activityResultRegistry] or other registry to it
     */
    val resultContractRegistry: ActivityResultRegistry

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
     * Called when user accepts update download in play-core dialog and download starts.
     * Called in flexible flow
     */
    fun updateDownloadStarts() = Unit

    /**
     * Reports update is downloaded and ready to be installed
     * When ready to proceed call [AppUpdateState.userConfirmedUpdate]
     * Called in flexible flow
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
     * Called in flexible flow
     */
    fun updateComplete() = Unit

    /**
     * Critical update error occurred e.g. when immediate update was requested but failed to proceed
     */
    fun updateFailed(e: Throwable)

    /**
     * Notify user of some non-critical update error e.g. flexible update has failed but it is not critical for
     * general application flow.
     * Called in flexible flow
     */
    fun nonCriticalUpdateError(e: Throwable) = Unit
}
