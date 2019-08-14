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
import androidx.annotation.CallSuper
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.ActivityResult.RESULT_IN_APP_UPDATE_FAILED
import com.google.android.play.core.install.model.AppUpdateType.FLEXIBLE
import com.google.android.play.core.install.model.InstallErrorCode.ERROR_INSTALL_NOT_ALLOWED
import com.google.android.play.core.install.model.InstallErrorCode.ERROR_INSTALL_UNAVAILABLE
import com.google.android.play.core.install.model.InstallStatus.*
import com.google.android.play.core.install.model.UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
import com.google.android.play.core.install.model.UpdateAvailability.UPDATE_AVAILABLE
import com.motorro.appupdatewrapper.AppUpdateException.Companion.ERROR_UNKNOWN_UPDATE_RESULT
import com.motorro.appupdatewrapper.AppUpdateException.Companion.ERROR_UPDATE_FAILED
import com.motorro.appupdatewrapper.AppUpdateException.Companion.ERROR_UPDATE_TYPE_NOT_ALLOWED
import timber.log.Timber

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
        setUpdateState(Checking())
    }

    /**
     * Transfers to update-consent state
     */
    protected fun updateConsent(appUpdateInfo: AppUpdateInfo) {
        setUpdateState(UpdateConsent(appUpdateInfo))
    }

    /**
     * Transfers to update consent check
     */
    protected fun updateConsentCheck() {
        setUpdateState(UpdateConsentCheck())
    }


    /**
     * Transfers to install consent check
     */
    protected fun installConsentCheck() {
        setUpdateState(InstallConsentCheck())
    }

    /**
     * Transfers to downloading state
     */
    protected fun downloading() {
        setUpdateState(Downloading())
    }

    /**
     * Transfers to install-consent state
     */
    protected fun installConsent() {
        setUpdateState(InstallConsent())
    }

    /**
     * Transfers to complete-update state
     */
    protected fun completeUpdate() {
        setUpdateState(CompleteUpdate())
    }

    /**
     * According to:
     * https://developer.android.com/reference/android/app/Activity.html#onActivityResult(int,%2520int,%2520android.content.Intent)
     * `onActivityResult` will be called before `onResume` thus saving explicit cancellation before any UI interaction
     * takes place. This may prevent download consent popup if activity was recreated during consent display
     */
    @CallSuper
    override fun checkActivityResult(requestCode: Int, resultCode: Int): Boolean =
        if (REQUEST_CODE_UPDATE == requestCode && Activity.RESULT_CANCELED == resultCode) {
            Timber.d("Update download cancelled")
            markUserCancelTime()
            complete()
            true
        } else {
            false
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
            ifNotBroken {
                withUpdateView {
                    updateChecking()
                }
                Timber.i("Getting application update info for FLEXIBLE update...")
                updateManager
                    .appUpdateInfo
                    .addOnSuccessListener {
                        Timber.i("Application update info: %s", it.toLoggingString())
                        if (!stopped) {
                            processUpdateInfo(it)
                        }
                    }
                    .addOnFailureListener {
                        Timber.w(it, "Error getting application update info: ")
                        if (!stopped) {
                            reportUpdateCheckFailure(it)
                        }
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
            Timber.d("Reporting update error due to update check...")
            reportError(AppUpdateException(AppUpdateException.ERROR_UPDATE_CHECK_FAILED, appUpdateException))
        }

        /**
         * Starts update on success or transfers to failed state
         */
        private fun processUpdateInfo(appUpdateInfo: AppUpdateInfo) {
            Timber.d("Evaluating update info...")
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
                    Timber.d("Update type FLEXIBLE is not allowed!")
                    reportError(AppUpdateException(ERROR_UPDATE_TYPE_NOT_ALLOWED))
                } else withUpdateView {
                    // As consent activity starts current activity looses focus.
                    // So we need to transfer to the next state to break popup cycle.
                    updateConsentCheck()

                    Timber.d("Asking for installation consent...")
                    stateMachine.updateManager.startUpdateFlowForResult(
                        updateInfo,
                        FLEXIBLE,
                        activity,
                        REQUEST_CODE_UPDATE
                    )
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
        override fun checkActivityResult(requestCode: Int, resultCode: Int): Boolean = when {
            super.checkActivityResult(requestCode, resultCode) -> true
            REQUEST_CODE_UPDATE != requestCode -> false
            else -> {
                when(resultCode) {
                    Activity.RESULT_OK -> {
                        Timber.d("User accepted update")
                        downloading()
                    }
                    RESULT_IN_APP_UPDATE_FAILED -> {
                        Timber.d("Reporting update error due to play-core UI error...")
                        reportError(AppUpdateException(ERROR_UPDATE_FAILED))
                    }
                    else -> {
                        Timber.w("Failing due to unknown play-core UI result...")
                        reportError(AppUpdateException(ERROR_UNKNOWN_UPDATE_RESULT))
                    }
                }
                true
            }
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
            Timber.d("Install state updated: %s", formatInstallStatus(state.installStatus()))
            when(state.installStatus()) {
                INSTALLED -> complete()
                CANCELED -> {
                    markUserCancelTime()
                    complete()
                }
                DOWNLOADED -> installConsent()
                INSTALLING -> completeUpdate()
                FAILED -> {
                    val errorCode = state.installErrorCode()
                    Timber.d("Install error code: %s", formatInstallErrorCode(errorCode))
                    reportError(
                        AppUpdateException(
                            when(state.installErrorCode()) {
                                ERROR_INSTALL_UNAVAILABLE, ERROR_INSTALL_NOT_ALLOWED -> ERROR_UPDATE_TYPE_NOT_ALLOWED
                                else -> ERROR_UPDATE_FAILED
                            }
                        )
                    )
                }
            }
        }

        /**
         * Handles lifecycle `onResume`
         */
        override fun onResume() {
            super.onResume()
            Timber.d("Registering to installation state updates...")
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
            Timber.d("Unregistering from installation state updates...")
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
            ifNotBroken {
                Timber.d("Getting installation consent...")
                withUpdateView {
                    // As consent activity starts current activity looses focus.
                    // So we need to transfer to the next state to break popup cycle.
                    installConsentCheck()
                    updateReady()
                }
            }
        }

    }

    /**
     * Listens to install consent results
     */
    internal class InstallConsentCheck: FlexibleUpdateState() {
        /**
         * Completes update
         * Call when update is downloaded and user confirmed app restart
         * Effective if update is called with [com.google.android.play.core.install.model.AppUpdateType.FLEXIBLE]
         */
        override fun userConfirmedUpdate() {
            completeUpdate()
        }

        /**
         * Cancels update installation
         * Call when update is downloaded and user cancelled app restart
         * Effective if update is called with [com.google.android.play.core.install.model.AppUpdateType.FLEXIBLE]
         */
        override fun userCanceledUpdate() {
            markUserCancelTime()
            complete()
        }
    }

    /**
     * Completes flexible update
     */
    internal class CompleteUpdate(): FlexibleUpdateState() {
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
            Timber.d("Starting play-core update installer for FLEXIBLE state...")
            updateManager
                .completeUpdate()
                .addOnSuccessListener {
                    Timber.d("Update installation complete")
                    if (!stopped) {
                        complete()
                    }
                }
                .addOnFailureListener {
                    Timber.d("Reporting update error due to installation failure...")
                    if (!stopped) {
                        reportError(AppUpdateException(ERROR_UPDATE_FAILED, it))
                    }
                }
            withUpdateView {
                updateInstallUiVisible()
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
    }
}