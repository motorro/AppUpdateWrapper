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

import android.os.Handler
import android.os.Looper.getMainLooper
import androidx.annotation.VisibleForTesting
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.motorro.appupdatewrapper.AppUpdateWrapper.Companion.USE_SAFE_LISTENERS

/**
 * Safely registers listener
 * Originally, updating a list of subscribers within event dispatching crashes AppUpdateManager dispatcher
 * with concurrent update exception.
 * See test files for explanation
 * @param listener Update state listener
 */
internal fun AppUpdateManager.registerListenerSafe(listener: InstallStateUpdatedListener) {
    if (USE_SAFE_LISTENERS) {
        doRegisterListenerSafe(listener)
    } else {
        registerListener(listener)
    }
}

/**
 * Registers listener in next loop
 */
@VisibleForTesting
internal fun AppUpdateManager.doRegisterListenerSafe(listener: InstallStateUpdatedListener) {
    Handler(getMainLooper()).post { registerListener(listener) }
}

/**
 * Safely unregisters listener
 * Originally, updating a list of subscribers within event dispatching crashes AppUpdateManager dispatcher
 * with concurrent update exception.
 * See test files for explanation
 * @param listener Update state listener
 */
internal fun AppUpdateManager.unregisterListenerSafe(listener: InstallStateUpdatedListener) {
    if (USE_SAFE_LISTENERS) {
        doUnregisterListenerSafe(listener)
    } else {
        unregisterListener(listener)
    }
}

/**
 * Unregisters listener in next loop
 */
@VisibleForTesting
internal fun AppUpdateManager.doUnregisterListenerSafe(listener: InstallStateUpdatedListener) {
    Handler(getMainLooper()).post { unregisterListener(listener) }
}
