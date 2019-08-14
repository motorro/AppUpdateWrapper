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

import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.install.model.AppUpdateType.FLEXIBLE
import com.google.android.play.core.install.model.AppUpdateType.IMMEDIATE
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

private const val APP_UPDATE_INFO_FORMAT = """Update info: 
    - available version code: %d
    - update availability: %s
    - install status: %s
    - update types allowed: %s"""

/**
 * Logs update info in human-readable format
 */
internal fun AppUpdateInfo.toLoggingString(): String = APP_UPDATE_INFO_FORMAT.format(
    availableVersionCode(),
    formatUpdateAvailability(),
    formatInstallStatus(),
    formatUpdateTypesAllowed()
)

/**
 * Returns a constant name for update availability
 */
private fun AppUpdateInfo.formatUpdateAvailability(): String = when(updateAvailability()) {
    UpdateAvailability.UNKNOWN -> "UNKNOWN"
    UpdateAvailability.UPDATE_NOT_AVAILABLE -> "UPDATE_NOT_AVAILABLE"
    UpdateAvailability.UPDATE_AVAILABLE -> "UPDATE_AVAILABLE"
    UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> "DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS"
    else -> "UNKNOWN UPDATE AVAILABILITY: ${updateAvailability()}"
}

/**
 * Returns a constant name for update availability
 */
private fun AppUpdateInfo.formatInstallStatus(): String = when(installStatus()) {
    InstallStatus.UNKNOWN -> "UNKNOWN"
    InstallStatus.REQUIRES_UI_INTENT -> "REQUIRES_UI_INTENT"
    InstallStatus.PENDING -> "PENDING"
    InstallStatus.DOWNLOADING -> "DOWNLOADING"
    InstallStatus.DOWNLOADED -> "DOWNLOADED"
    InstallStatus.INSTALLING -> "INSTALLING"
    InstallStatus.INSTALLED -> "INSTALLED"
    InstallStatus.FAILED -> "FAILED"
    InstallStatus.CANCELED -> "CANCELED"
    else -> "UNKNOWN INSTALL STATUS: ${installStatus()}"
}

private fun AppUpdateInfo.formatUpdateTypesAllowed(): String {
    var result = "NONE"
    if (isUpdateTypeAllowed(FLEXIBLE)) {
        result = "FLEXIBLE"
    }
    if (isUpdateTypeAllowed(IMMEDIATE)) {
        result += ", IMMEDIATE"
    }
    return result
}