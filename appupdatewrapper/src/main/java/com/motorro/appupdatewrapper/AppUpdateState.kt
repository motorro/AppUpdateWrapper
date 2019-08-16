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

import androidx.annotation.CallSuper
import androidx.annotation.VisibleForTesting
import com.google.android.play.core.appupdate.AppUpdateManager

/**
 * Application update state interface
 */
internal abstract class AppUpdateState: AppUpdateWrapper, Tagged {
    /**
     * Update stateMachine
     * @see AppUpdateStateMachine.setUpdateState
     */
    internal lateinit var stateMachine: AppUpdateStateMachine

    /**
     * Update manager
     */
    protected val updateManager: AppUpdateManager
        get() = stateMachine.updateManager

    /**
     * Executes [block] with update view
     */
    protected fun withUpdateView(block: AppUpdateView.() -> Unit) {
        stateMachine.view.block()
    }

    /**
     * Executes [block] if update flow is not broken by [AppUpdateStateMachine.flowBreaker]
     * Otherwise transfers to [Done]
     */
    protected inline fun ifNotBroken(block: () -> Unit) {
        if(false != stateMachine.flowBreaker.isEnoughTimePassedSinceLatestCancel()) {
            block()
        } else {
            timber.d("Update flow broken")
            complete()
        }
    }

    /**
     * Sets new update state
     */
    protected fun setUpdateState(state: AppUpdateState) {
        stateMachine.setUpdateState(state)
    }

    /**
     * Saves time user has explicitly cancelled update
     */
    protected fun markUserCancelTime() {
        stateMachine.flowBreaker.saveTimeCanceled()
    }

    /**
     * Sets a dummy state
     */
    protected open fun setNone() {
        setUpdateState(None())
    }

    /**
     * Reports update is complete
     */
    protected open fun complete() {
        setUpdateState(Done())
    }

    /**
     * Reports non-critical update error.
     * Ends-up in [AppUpdateView.updateComplete] calling [AppUpdateView.nonCriticalUpdateError]
     */
    protected open fun reportError(error: AppUpdateException) {
        setUpdateState(Error(error))
    }

    /**
     * Reports critical update error.
     * Ends-up in [AppUpdateView.updateFailed]
     */
    protected open fun fail(error: AppUpdateException) {
        setUpdateState(Failed(error))
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

    /**
     * Called by state-machine when state is being replaced
     */
    @CallSuper
    override fun cleanup() = Unit
}

/**
 * Default update state that does nothing
 */
internal class None: AppUpdateState()

/**
 * Completes the update sequence
 */
internal class Done: AppUpdateState(), Tagged {
    /**
     * Handles lifecycle `onResume`
     */
    override fun onResume() {
        super.onResume()
        timber.d("onResume")
        timber.d("Completing...")
        withUpdateView {
            updateComplete()
            setNone()
        }
    }
}

/**
 * Reports non-critical update error.
 * Ends-up in [AppUpdateView.updateComplete] calling [AppUpdateView.nonCriticalUpdateError]
 */
internal class Error(@VisibleForTesting val error: AppUpdateException) : AppUpdateState() {
    /**
     * Handles lifecycle `onResume`
     */
    override fun onResume() {
        super.onResume()
        timber.d("onResume")
        timber.w(error, "Application update failure: ")
        ifNotBroken {
            withUpdateView {
                nonCriticalUpdateError(error)
                complete()
            }
        }
    }
}

/**
 * Reports critical update error.
 * Ends-up in [AppUpdateView.updateFailed]
 */
internal class Failed(@VisibleForTesting val error: AppUpdateException) : AppUpdateState() {
    /**
     * Handles lifecycle `onResume`
     */
    override fun onResume() {
        super.onResume()
        timber.d("onResume")
        timber.w(error, "Application update failure: ")
        withUpdateView {
            updateFailed(error)
            setNone()
        }
    }
}

