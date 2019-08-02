package com.motorro.appupdatewrapper

import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.install.model.AppUpdateType.FLEXIBLE
import com.google.android.play.core.install.model.InstallStatus.*
import com.google.android.play.core.install.model.UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
import com.google.android.play.core.install.model.UpdateAvailability.UPDATE_AVAILABLE
import com.motorro.appupdatewrapper.AppUpdateException.Companion.ERROR_UPDATE_TYPE_NOT_ALLOWED

/**
 * Flexible update flow
 */
internal sealed class FlexibleUpdateState(): AppUpdateState() {
    companion object {
        /**
         * Starts flexible update flow
         * @param stateMachine Application update stateMachine state-machine
         */
        fun start(stateMachine: AppUpdateStateMachine) {
            stateMachine.setUpdateState(Initial())
        }
    }

    /**
     * Initial state
     */
    internal class Initial() : FlexibleUpdateState() {
        /**
         * Handles lifecycle `onStart`
         */
        override fun onStart() {
            super.onStart()
            ifNotBroken {
                setUpdateState(Checking())
            }
        }
    }

    /**
     * Checks for update
     */
    internal class Checking(): FlexibleUpdateState() {
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
                        reportUpdateCheckFailure(it)
                    }
                }
        }

        /**
         * Handles lifecycle `onStop`
         */
        override fun onStop() {
            super.onStop()
            stopped = true
            complete()
        }

        /**
         * Transfers to failed state
         */
        private fun reportUpdateCheckFailure(appUpdateException: Throwable) {
            reportError(AppUpdateException(AppUpdateException.ERROR_UPDATE_CHECK_FAILED, appUpdateException))
        }

        /**
         * Starts update on success or transfers to failed state
         */
        private fun processUpdateInfo(appUpdateInfo: AppUpdateInfo) {
            with(appUpdateInfo) {
                when (updateAvailability()) {
                    DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> when (installStatus()) {
                        REQUIRES_UI_INTENT -> setUpdateState(UpdateConsent(appUpdateInfo))
                        PENDING, DOWNLOADING -> setUpdateState(Downloading(appUpdateInfo))
                        DOWNLOADED -> setUpdateState(InstallConsent(appUpdateInfo))
                        INSTALLING -> setUpdateState(CompleteUpdate())
                        else -> complete()
                    }
                    UPDATE_AVAILABLE -> setUpdateState(UpdateConsent(appUpdateInfo))
                    else -> complete()
                }
            }
        }
    }

    internal class Downloading(private val updateInfo: AppUpdateInfo): FlexibleUpdateState() {

    }

    /**
     * Opens update consent
     * @param updateInfo Update info to start flexible update
     */
    internal class UpdateConsent(private val updateInfo: AppUpdateInfo): FlexibleUpdateState() {
        /**
         * Handles lifecycle `onResume`
         */
        override fun onResume() {
            super.onResume()
            if (false == updateInfo.isUpdateTypeAllowed(FLEXIBLE)) {
                reportError(AppUpdateException(ERROR_UPDATE_TYPE_NOT_ALLOWED))
            } else withUpdateView {
                stateMachine.updateManager.startUpdateFlowForResult(
                    updateInfo,
                    FLEXIBLE,
                    activity,
                    REQUEST_CODE_UPDATE
                )
                complete()
            }
        }
    }

    internal class InstallConsent(private val updateInfo: AppUpdateInfo): FlexibleUpdateState() {

    }

    internal class CompleteUpdate(): FlexibleUpdateState() {

    }
}