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

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event.*
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.google.android.play.core.appupdate.AppUpdateManager

/**
 * App update state machine
 */
internal interface AppUpdateStateMachine {
    /**
     * [AppUpdateManager] instance
     */
    val updateManager: AppUpdateManager

    /**
     * Terminates update flow if user has already cancelled an update
     */
    val flowBreaker: UpdateFlowBreaker

    /**
     * Update view
     */
    val view: AppUpdateView

    /**
     * Sets new update state
     */
    fun setUpdateState(newState: AppUpdateState)
}

/**
 * Manages state transition and component lifecycle
 * @param lifecycle Component lifecycle
 * @param updateManager AppUpdateManager instance
 * @param view Application update view interface
 * @param flowBreaker Terminates update flow if user has already cancelled an update
 */
internal class AppUpdateLifecycleStateMachine(
    private val lifecycle: Lifecycle,
    override val updateManager: AppUpdateManager,
    override val view: AppUpdateView,
    override val flowBreaker: UpdateFlowBreaker = UpdateFlowBreaker.alwaysOn()
): AppUpdateStateMachine, AppUpdateWrapper, LifecycleObserver, Tagged {
    /**
     * Current update state
     */
    @VisibleForTesting
    var currentUpdateState: AppUpdateState

    init {
        currentUpdateState = None()
        lifecycle.addObserver(this)
        timber.d("State machine initialized")
    }

    /**
     * Sets new update state
     */
    override fun setUpdateState(newState: AppUpdateState) {
        timber.d("Setting new state: %s", newState.javaClass.simpleName)
        currentUpdateState.cleanup()

        newState.stateMachine = this
        currentUpdateState = newState

        with(lifecycle.currentState) {
            if (isAtLeast(Lifecycle.State.STARTED)) {
                timber.d("Starting new state...")
                newState.onStart()
            }
            if (isAtLeast(Lifecycle.State.RESUMED)) {
                timber.d("Resuming new state...")
                newState.onResume()
            }
        }
    }

    @OnLifecycleEvent(ON_START)
    fun onStart() {
        currentUpdateState.onStart()
    }

    @OnLifecycleEvent(ON_RESUME)
    fun onResume() {
        currentUpdateState.onResume()
    }

    @OnLifecycleEvent(ON_RESUME)
    fun onPause() {
        currentUpdateState.onPause()
    }

    @OnLifecycleEvent(ON_STOP)
    fun onStop() {
        currentUpdateState.onStop()
    }

    /**
     * Checks activity result and returns `true` if result is an update result and was handled
     * Use to check update activity result in [android.app.Activity.onActivityResult]
     */
    override fun checkActivityResult(requestCode: Int, resultCode: Int): Boolean {
        timber.d("Processing activity result: requestCode(%d), resultCode(%d)", requestCode, resultCode)
        return currentUpdateState.checkActivityResult(requestCode, resultCode).also {
            timber.d("Activity result handled: %b", it)
        }
    }

    /**
     * Cancels update installation
     * Call when update is downloaded and user cancelled app restart
     * Effective if update is called with [com.google.android.play.core.install.model.AppUpdateType.FLEXIBLE]
     */
    override fun userCanceledUpdate() {
        currentUpdateState.userCanceledUpdate()
    }

    /**
     * Completes update
     * Call when update is downloaded and user confirmed app restart
     * Effective if update is called with [com.google.android.play.core.install.model.AppUpdateType.FLEXIBLE]
     */
    override fun userConfirmedUpdate() {
        currentUpdateState.userConfirmedUpdate()
    }

    /**
     * Stops update workflow and cleans-up
     */
    override fun cleanup() {
        lifecycle.removeObserver(this)
        currentUpdateState = None()
        timber.d("Cleaned-up!")
    }
}
