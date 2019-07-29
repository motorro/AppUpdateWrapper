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
sealed class ImmediateUpdate: AppUpdateState() {
    /**
     * Initial state
     */
    internal class Initial : AppUpdateState() {
        /**
         * Handles lifecycle `onStart`
         */
        override fun onStart() {
            super.onStart()
            host.setState(Checking())
        }
    }

    /**
     * Checks for update
     */
    internal class Checking: AppUpdateState() {
        /*
         * Set to true on [onStop] to prevent view interaction
         * as there is no way to abort task
         */
        private var stopped: Boolean = false

        override fun onStart() {
            super.onStart()
            host.updateManager
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
        }

        /**
         * Transfers to failed state
         */
        private fun reportUpdateFailure(appUpdateException: AppUpdateException) {
            host.setState(Failed(appUpdateException))
        }

        /**
         * Starts update on success or transfers to failed state
         */
        private fun processUpdateInfo(appUpdateInfo: AppUpdateInfo) {
            val state = when (appUpdateInfo.updateAvailability()) {
                UPDATE_AVAILABLE, DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> Update(appUpdateInfo)
                else -> Failed(AppUpdateException(ERROR_NO_IMMEDIATE_UPDATE))
            }
            host.setState(state)
        }
    }

    /**
     * Updates application
     * @param updateInfo Update info to start imeediate update
     */
    internal class Update(private val updateInfo: AppUpdateInfo): AppUpdateState() {
        /**
         * Handles lifecycle `onResume`
         */
        override fun onResume() {
            super.onResume()
            ifViewAvailable {
                host.updateManager.startUpdateFlowForResult(
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
    internal class Failed(private val error: AppUpdateException) : AppUpdateState() {
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
        private fun reportUpdateFailure() = ifViewAvailable {
            fail(error)
        }
    }
}