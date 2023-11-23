/*
 * Copyright 2023 Nikolai Kotchetkov (motorro).
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

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager

/**
 * Exposes update contracts so the flow may complete
 * This is needed as Fake manager does not do anything with the contract
 */
class FakeUpdateManagerWithContract(private val context: Context) : FakeAppUpdateManager(context) {
    private lateinit var contract: ActivityResultLauncher<IntentSenderRequest>

    override fun startUpdateFlowForResult(
        appUpdateInfo: AppUpdateInfo,
        p1: ActivityResultLauncher<IntentSenderRequest>,
        options: AppUpdateOptions
    ): Boolean {
        contract = p1
        return super.startUpdateFlowForResult(appUpdateInfo, p1, options)
    }

    fun launchContract() {
        val intent = PendingIntent.getActivity(
            context,
            -1,
            Intent(),
            0
        )
        val request = IntentSenderRequest.Builder(intent.intentSender).build()
        contract.launch(request)
    }
}