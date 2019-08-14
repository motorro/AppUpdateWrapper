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

import android.app.Activity
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.install.model.AppUpdateType.IMMEDIATE
import com.google.android.play.core.install.model.UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
import com.google.android.play.core.install.model.UpdateAvailability.UPDATE_AVAILABLE
import com.motorro.appupdatewrapper.AppUpdateException.Companion.ERROR_NO_IMMEDIATE_UPDATE
import com.motorro.appupdatewrapper.AppUpdateException.Companion.ERROR_UPDATE_FAILED
import com.motorro.appupdatewrapper.AppUpdateException.Companion.ERROR_UPDATE_TYPE_NOT_ALLOWED

/**
 * Immediate update flow
 */
internal sealed class ImmediateUpdateState: AppUpdateState() {
    companion object {
        /**
         * Starts immediate update flow
         * @param stateMachine Application update stateMachine state-machine
         */
        fun start(stateMachine: AppUpdateStateMachine) {
            stateMachine.setUpdateState(Initial())
        }

        /**
         * Forces immediate update start
         * @param stateMachine Application update stateMachine state-machine
         * @param appUpdateInfo Application update info
         * @see FlexibleUpdateState.Checking
         */
        fun doUpdate(stateMachine: AppUpdateStateMachine, appUpdateInfo: AppUpdateInfo) {
            stateMachine.setUpdateState(Update(appUpdateInfo))
        }
    }

    /**
     * Transfers to checking state
     */
    protected fun checking() {
        setUpdateState(Checking())
    }

    /**
     * Transfers to update state
     */
    protected fun update(appUpdateInfo: AppUpdateInfo) {
        setUpdateState(Update(appUpdateInfo))
    }

    /**
     * Transfers to update ui check
     */
    protected fun updateUiCheck() {
        setUpdateState(UpdateUiCheck())
    }

    /**
     * Initial state
     */
    internal class Initial : ImmediateUpdateState() {
        /**
         * Handles lifecycle `onStart`
         */
        override fun onStart() {
            super.onStart()
            checking()
        }
    }

    /**
     * Checks for update
     */
    internal class Checking: ImmediateUpdateState() {
        /*
         * Set to true on [onStop] to prevent view interaction
         * as there is no way to abort task
         */
        private var stopped: Boolean = false

        /**
         * Handles lifecycle `onStart`
         */
        override fun onStart() {
            super.onStart()
            withUpdateView {
                updateChecking()
            }
            updateManager
                .appUpdateInfo
                .addOnSuccessListener {
                    if (!stopped) {
                        processUpdateInfo(it)
                    }
                }
                .addOnFailureListener {
                    if (!stopped) {
                        reportUpdateCheckFailure(it)
                    }
                }
        }

        /**
         * Called by state-machine when state is being replaced
         */
        override fun cleanup() {
            super.cleanup()
            stopped = true
        }

        /**
         * Handles lifecycle `onStop`
         */
        override fun onStop() {
            super.onStop()
            complete()
        }

        /**
         * Transfers to failed state
         */
        private fun reportUpdateCheckFailure(appUpdateException: Throwable) {
            fail(AppUpdateException(ERROR_UPDATE_FAILED, appUpdateException))
        }

        /**
         * Starts update on success or transfers to failed state
         */
        private fun processUpdateInfo(appUpdateInfo: AppUpdateInfo) {
            when (appUpdateInfo.updateAvailability()) {
                UPDATE_AVAILABLE, DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> update(appUpdateInfo)
                else -> fail(AppUpdateException(ERROR_NO_IMMEDIATE_UPDATE))
            }
        }
    }

    /**
     * Updates application
     * @param updateInfo Update info to start imeediate update
     */
    internal class Update(private val updateInfo: AppUpdateInfo): ImmediateUpdateState() {
        /**
         * Handles lifecycle `onResume`
         */
        override fun onResume() {
            super.onResume()
            if (false == updateInfo.isUpdateTypeAllowed(IMMEDIATE)) {
                fail(AppUpdateException(ERROR_UPDATE_TYPE_NOT_ALLOWED))
            } else withUpdateView {
                updateManager.startUpdateFlowForResult(
                    updateInfo,
                    IMMEDIATE,
                    activity,
                    REQUEST_CODE_UPDATE
                )
                updateInstallUiVisible()
                updateUiCheck()
            }
        }
    }

    /**
     * Checks for update ui errors
     */
    internal class UpdateUiCheck: ImmediateUpdateState() {
        /**
         * Checks activity result and returns `true` if result is an update result and was handled
         * Use to check update activity result in [android.app.Activity.onActivityResult]
         */
        override fun checkActivityResult(requestCode: Int, resultCode: Int): Boolean {
            if (REQUEST_CODE_UPDATE != requestCode) {
                return false
            }

            if (Activity.RESULT_OK == resultCode) {
                complete()
            } else {
                fail(AppUpdateException(ERROR_UPDATE_FAILED))
            }

            return true
        }
    }
}