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

import com.google.android.play.core.appupdate.AppUpdateManager
import com.motorro.appupdatewrapper.AppUpdateWrapper.Companion.REQUEST_KEY_UPDATE

/**
 * Wraps [AppUpdateManager] interaction.
 * The update wrapper is designed to be a single-use object. It carries out the workflow using host
 * [androidx.lifecycle.Lifecycle] and terminates in either [AppUpdateView.updateComplete] or
 * [AppUpdateView.updateFailed].
 * [AppUpdateManager] pops up activities-for-result from time to time. That is why [AppUpdateView.resultContractRegistry].
 * The library registers the contract itself. If you need to change contract key - set [REQUEST_KEY_UPDATE]
 * to the desired one
 */
interface AppUpdateWrapper {
    companion object {
        /**
         * Originally, updating a list of subscribers within event dispatching crashes AppUpdateManager with concurrent
         * update exception. If your application uses several listeners simultaneously (like you have multiple activity
         * setup) or you encounter such an exception - set this value to true
         * @see AppUpdateManager.registerListener
         * @see AppUpdateManager.unregisterListener
         */
        var USE_SAFE_LISTENERS = false

        /**
         * The request key wrapper uses to register [AppUpdateManager] contract
         */
        var REQUEST_KEY_UPDATE = REQUEST_KEY_UPDATE_DEFAULT
    }

    /**
     * Cancels update installation
     * Call when update is downloaded and user cancelled app restart
     * Effective if update is called with [com.google.android.play.core.install.model.AppUpdateType.FLEXIBLE]
     */
    fun userCanceledUpdate()

    /**
     * Completes update
     * Call when update is downloaded and user confirmed app restart
     * Effective if update is called with [com.google.android.play.core.install.model.AppUpdateType.FLEXIBLE]
     */
    fun userConfirmedUpdate()

    /**
     * Stops update workflow and cleans-up
     */
    fun cleanup()
}