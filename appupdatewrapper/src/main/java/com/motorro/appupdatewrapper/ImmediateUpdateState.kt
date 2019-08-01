package com.motorro.appupdatewrapper

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
            setUpdateState(Checking())
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
            stopped = false
            stateMachine.updateManager
                .appUpdateInfo
                .addOnSuccessListener {
                    if (!stopped) {
                        processUpdateInfo(it)
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
            setUpdateState(Initial())
        }

        /**
         * Transfers to failed state
         */
        private fun reportUpdateFailure(appUpdateException: AppUpdateException) {
            fail(appUpdateException)
        }

        /**
         * Starts update on success or transfers to failed state
         */
        private fun processUpdateInfo(appUpdateInfo: AppUpdateInfo) {
            when (appUpdateInfo.updateAvailability()) {
                UPDATE_AVAILABLE, DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> setUpdateState(Update(appUpdateInfo))
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
                stateMachine.updateManager.startUpdateFlowForResult(
                    updateInfo,
                    IMMEDIATE,
                    activity,
                    REQUEST_CODE_UPDATE
                )
                setUpdateState(Done())
            }
        }
    }
}