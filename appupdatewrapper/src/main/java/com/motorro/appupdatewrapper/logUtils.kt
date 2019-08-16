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

import android.os.Build
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.install.model.AppUpdateType.FLEXIBLE
import com.google.android.play.core.install.model.AppUpdateType.IMMEDIATE
import com.google.android.play.core.install.model.InstallErrorCode
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import timber.log.Timber

/**
 * Trims tag to fit
 */
internal fun trimmedTag(tag: String): String = if (tag.length <= 23  || Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
    tag
} else {
    tag.substring(0, 23)
}

/**
 * Creates logging tag
 */
internal interface Tagged {
    /**
     * Returns common tag prefix
     */
    fun getTagPrefix(): String = ""

    /**
     * Returns logging tag
     */
    fun getTag(): String = trimmedTag("$LIBRARY_LOG_PREFIX:${getTagPrefix()}:${javaClass.simpleName}")
}

/**
 * Sets [Timber.tag] and returns tree
 */
internal val Tagged.timber: Timber.Tree
    get() = Timber.tag(getTag())

/**
 * AppUpdateInfo logging format
 */
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
 * Returns a constant name for update status
 */
internal fun formatInstallStatus(status: Int): String = when(status) {
    InstallStatus.UNKNOWN -> "UNKNOWN"
    InstallStatus.REQUIRES_UI_INTENT -> "REQUIRES_UI_INTENT"
    InstallStatus.PENDING -> "PENDING"
    InstallStatus.DOWNLOADING -> "DOWNLOADING"
    InstallStatus.DOWNLOADED -> "DOWNLOADED"
    InstallStatus.INSTALLING -> "INSTALLING"
    InstallStatus.INSTALLED -> "INSTALLED"
    InstallStatus.FAILED -> "FAILED"
    InstallStatus.CANCELED -> "CANCELED"
    else -> "UNKNOWN INSTALL STATUS: $status"
}

/**
 * Returns a constant name for installation error
 */
internal fun formatInstallErrorCode(code: Int): String = when(code) {
    InstallErrorCode.NO_ERROR -> "NO_ERROR"
    InstallErrorCode.NO_ERROR_PARTIALLY_ALLOWED -> "NO_ERROR_PARTIALLY_ALLOWED"
    InstallErrorCode.ERROR_UNKNOWN -> "ERROR_UNKNOWN"
    InstallErrorCode.ERROR_API_NOT_AVAILABLE -> "ERROR_API_NOT_AVAILABLE"
    InstallErrorCode.ERROR_INVALID_REQUEST -> "ERROR_INVALID_REQUEST"
    InstallErrorCode.ERROR_INSTALL_UNAVAILABLE -> "ERROR_INSTALL_UNAVAILABLE"
    InstallErrorCode.ERROR_INSTALL_NOT_ALLOWED -> "ERROR_INSTALL_UNAVAILABLE"
    InstallErrorCode.ERROR_DOWNLOAD_NOT_PRESENT -> "ERROR_DOWNLOAD_NOT_PRESENT"
    InstallErrorCode.ERROR_INTERNAL_ERROR -> "ERROR_INTERNAL_ERROR"
    else -> "UNKNOWN INSTALL ERROR: $code"
}

/**
 * Returns a constant name for update status
 */
private fun AppUpdateInfo.formatInstallStatus(): String = formatInstallStatus(installStatus())

/**
 * Retrieves allowed update types
 */
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