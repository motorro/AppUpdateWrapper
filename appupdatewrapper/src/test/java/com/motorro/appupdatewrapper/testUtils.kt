package com.motorro.appupdatewrapper

import android.app.Activity
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager

/**
 * Executes [block] when update has already been triggered
 * Used to emulate [com.google.android.play.core.install.model.UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS]
 */
inline fun FakeAppUpdateManager.withUpdateAlreadyTriggered(updateType: Int, activity: Activity, crossinline block: () -> Unit) {
    appUpdateInfo.addOnSuccessListener {
        startUpdateFlowForResult(it, updateType, activity, 1050)
        userAcceptsUpdate()
        block()
    }
}