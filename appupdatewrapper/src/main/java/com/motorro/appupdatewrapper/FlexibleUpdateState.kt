package com.motorro.appupdatewrapper

import android.app.Activity
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType.FLEXIBLE
import com.google.android.play.core.install.model.InstallErrorCode.ERROR_INSTALL_NOT_ALLOWED
import com.google.android.play.core.install.model.InstallErrorCode.ERROR_INSTALL_UNAVAILABLE
import com.google.android.play.core.install.model.InstallStatus.*
import com.google.android.play.core.install.model.UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
import com.google.android.play.core.install.model.UpdateAvailability.UPDATE_AVAILABLE
import com.motorro.appupdatewrapper.AppUpdateException.Companion.ERROR_UNKNOWN_UPDATE_RESULT
import com.motorro.appupdatewrapper.AppUpdateException.Companion.ERROR_UPDATE_FAILED
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
     * Transfers to update-checking state
     */
    protected fun checking() {
        stateMachine.setUpdateState(Checking())
    }

    /**
     * Transfers to update-consent state
     */
    protected fun updateConsent(appUpdateInfo: AppUpdateInfo) {
        stateMachine.setUpdateState(UpdateConsent(appUpdateInfo))
    }

    /**
     * Transfers to update consent check
     */
    protected fun updateConsentCheck() {
        stateMachine.setUpdateState(UpdateConsentCheck())
    }
    
    /**
     * Transfers to downloading state
     */
    protected fun downloading() {
        stateMachine.setUpdateState(Downloading())
    }

    /**
     * Transfers to install-consent state
     */
    protected fun installConsent() {
        stateMachine.setUpdateState(InstallConsent())
    }

    /**
     * Transfers to complete-update state
     */
    protected fun completeUpdate() {
        stateMachine.setUpdateState(CompleteUpdate())
    }

    /**
     * Saves time user has explicitly cancelled update
     */
    protected fun markUserCancelTime() {
        stateMachine.flowBreaker.saveTimeCanceled()
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
            checking()
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
            ifNotBroken {
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
                        REQUIRES_UI_INTENT -> updateConsent(appUpdateInfo)
                        PENDING, DOWNLOADING -> downloading()
                        DOWNLOADED -> installConsent()
                        INSTALLING -> completeUpdate()
                        else -> complete()
                    }
                    UPDATE_AVAILABLE -> updateConsent(appUpdateInfo)
                    else -> complete()
                }
            }
        }
    }

    /**
     * Opens update consent.
     * View should handle pass activity result to [checkActivityResult]
     * @param updateInfo Update info to start flexible update
     */
    internal class UpdateConsent(private val updateInfo: AppUpdateInfo): FlexibleUpdateState() {
        /**
         * Handles lifecycle `onResume`
         */
        override fun onResume() {
            super.onResume()
            ifNotBroken {
                if (false == updateInfo.isUpdateTypeAllowed(FLEXIBLE)) {
                    reportError(AppUpdateException(ERROR_UPDATE_TYPE_NOT_ALLOWED))
                } else withUpdateView {
                    stateMachine.updateManager.startUpdateFlowForResult(
                        updateInfo,
                        FLEXIBLE,
                        activity,
                        REQUEST_CODE_UPDATE
                    )
                    // As consent activity starts application looses focus.
                    // So we need to transfer to the next state to break popup cycle.
                    updateConsentCheck()
                }
            }
        }
    }

    /**
     * Checks for consent activity result
     */
    internal class UpdateConsentCheck: FlexibleUpdateState() {
        /**
         * Checks activity result and returns `true` if result is an update result and was handled
         * Use to check update activity result in [android.app.Activity.onActivityResult]
         */
        override fun checkActivityResult(requestCode: Int, resultCode: Int): Boolean {
            if (REQUEST_CODE_UPDATE != requestCode) {
                return false
            }
            when(resultCode) {
                Activity.RESULT_OK -> {
                    downloading()
                }
                Activity.RESULT_CANCELED -> {
                    markUserCancelTime()
                    complete()
                }
                else -> reportError(AppUpdateException(ERROR_UNKNOWN_UPDATE_RESULT))
            }
            return true
        }
    }


    /**
     * Watches for update download status
     */
    internal class Downloading(): FlexibleUpdateState() {
        /**
         * Update state listener
         */
        private val listener = InstallStateUpdatedListener { state ->
            when(state.installStatus()) {
                CANCELED, INSTALLED -> complete()
                DOWNLOADED -> installConsent()
                INSTALLING -> completeUpdate()
                FAILED -> reportError(
                    AppUpdateException(
                        when(state.installErrorCode()) {
                            ERROR_INSTALL_UNAVAILABLE, ERROR_INSTALL_NOT_ALLOWED -> ERROR_UPDATE_TYPE_NOT_ALLOWED
                            else -> ERROR_UPDATE_FAILED
                        }
                    )
                )
            }
        }

        /**
         * Handles lifecycle `onResume`
         */
        override fun onResume() {
            super.onResume()
            updateManager.registerListenerSafe(listener)
        }

        /**
         * Handles lifecycle `onPause`
         */
        override fun onPause() {
            super.onPause()
            // Switch back to checking so only the topmost activity handle installation progress.
            checking()
        }

        /**
         * Called by state-machine when state is being replaced
         */
        override fun cleanup() {
            super.cleanup()
            updateManager.unregisterListenerSafe(listener)
        }
    }

    /**
     * Instructs view to display update consent
     */
    internal class InstallConsent(): FlexibleUpdateState() {
        /**
         * Handles lifecycle `onResume`
         */
        override fun onResume() {
            super.onResume()
            withUpdateView {
                updateInstallUiVisible()
            }
        }
    }

    internal class CompleteUpdate(): FlexibleUpdateState() {

    }
}