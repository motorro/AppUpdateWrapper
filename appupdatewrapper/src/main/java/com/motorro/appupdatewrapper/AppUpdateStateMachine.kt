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

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.play.core.appupdate.AppUpdateManager
import com.motorro.appupdatewrapper.AppUpdateWrapper.Companion.REQUEST_KEY_UPDATE

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
     * Update request launcher
     */
    val launcher: ActivityResultLauncher<IntentSenderRequest>

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
): AppUpdateStateMachine, AppUpdateWrapper, DefaultLifecycleObserver, Tagged {
    /**
     * Current update state
     */
    @VisibleForTesting
    var currentUpdateState: AppUpdateState

    /**
     * Update request launcher
     */
    override lateinit var launcher: ActivityResultLauncher<IntentSenderRequest>
        private set

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

    override fun onStart(owner: LifecycleOwner) {
        launcher = view.resultContractRegistry.register(REQUEST_KEY_UPDATE, StartIntentSenderForResult()) {
            checkActivityResult(it.resultCode)
        }
        currentUpdateState.onStart()
    }

    override fun onResume(owner: LifecycleOwner) {
        currentUpdateState.onResume()
    }

    override fun onPause(owner: LifecycleOwner) {
        currentUpdateState.onPause()
    }

    override fun onStop(owner: LifecycleOwner) {
        currentUpdateState.onStop()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        launcher.unregister()
    }

    /**
     * Checks activity result and returns `true` if result is an update result and was handled
     * Use to check update activity result in [android.app.Activity.onActivityResult]
     */
    private fun checkActivityResult(resultCode: Int): Boolean {
        timber.d("Processing activity result: resultCode(%d)", resultCode)
        return currentUpdateState.checkActivityResult(resultCode).also {
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
        if (this::launcher.isInitialized) {
            launcher.unregister()
        }
        currentUpdateState = None()
        timber.d("Cleaned-up!")
    }
}
