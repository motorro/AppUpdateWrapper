package com.motorro.appupdatewrapper

import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
import com.google.android.play.core.install.model.UpdateAvailability.UPDATE_AVAILABLE
import com.motorro.appupdatewrapper.AppUpdateException.Companion.ERROR_NO_IMMEDIATE_UPDATE
import com.motorro.appupdatewrapper.AppUpdateException.Companion.ERROR_UPDATE_FAILED

/**
 * Immediate update flow
 */
internal sealed class ImmediateUpdate: AppUpdateState() {
    companion object {
        /**
         * Starts immediate update flow
         * @param stateMachine Application update stateMachine state-machine
         */
        fun start(stateMachine: AppUpdateStateMachine) {
            stateMachine.setUpdateState(Initial())
        }
    }

    /**
     * Initial state
     */
    internal class Initial : ImmediateUpdate() {
        /**
         * Handles lifecycle `onStart`
         */
        override fun onStart() {
            super.onStart()
            stateMachine.setUpdateState(Checking())
        }
    }

    /**
     * Checks for update
     */
    internal class Checking: ImmediateUpdate() {
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
            stopped = false
            stateMachine.updateManager
                .appUpdateInfo
                .addOnCompleteListener {
                    if (!stopped) {
                        processUpdateInfo(it.result)
                    }
                }
                .addOnFailureListener {
                    if (!stopped) {
                        reportUpdateFailure(AppUpdateException(ERROR_UPDATE_FAILED, it))
                    }
                }
        }

        /**
         * Handles lifecycle `onStop`
         */
        override fun onStop() {
            super.onStop()
            stopped = true
            stateMachine.setUpdateState(Initial())
        }

        /**
         * Transfers to failed state
         */
        private fun reportUpdateFailure(appUpdateException: AppUpdateException) {
            stateMachine.setUpdateState(Failed(appUpdateException))
        }

        /**
         * Starts update on success or transfers to failed state
         */
        private fun processUpdateInfo(appUpdateInfo: AppUpdateInfo) {
            val state = when (appUpdateInfo.updateAvailability()) {
                UPDATE_AVAILABLE, DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> Update(appUpdateInfo)
                else -> Failed(AppUpdateException(ERROR_NO_IMMEDIATE_UPDATE))
            }
            stateMachine.setUpdateState(state)
        }
    }

    /**
     * Updates application
     * @param updateInfo Update info to start imeediate update
     */
    internal class Update(private val updateInfo: AppUpdateInfo): ImmediateUpdate() {
        /**
         * Handles lifecycle `onResume`
         */
        override fun onResume() {
            super.onResume()
            withUpdateView {
                stateMachine.updateManager.startUpdateFlowForResult(
                    updateInfo,
                    AppUpdateType.IMMEDIATE,
                    activity,
                    REQUEST_CODE_UPDATE
                )
            }
        }
    }

    /**
     * Update failed
     */
    internal class Failed(private val error: AppUpdateException) : ImmediateUpdate() {
        /**
         * Handles lifecycle `onResume`
         */
        override fun onResume() {
            super.onResume()
            reportUpdateFailure()
        }

        /**
         * Notifies view error has occurred and resets update
         */
        private fun reportUpdateFailure() = withUpdateView {
            fail(error)
        }
    }
}