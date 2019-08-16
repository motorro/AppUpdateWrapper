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
import androidx.test.core.app.ActivityScenario.launch
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import com.google.android.play.core.install.model.AppUpdateType
import com.motorro.appupdatewrapper.testapp.TestUpdateActivity
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class ImmediateUpdateKtTest: TestAppTest() {
    @Test
    fun startsImmediateUpdateIfAvailable() {
        lateinit var updateManager: FakeAppUpdateManager
        val scenario = launch(TestUpdateActivity::class.java)
        scenario.onActivity {
            updateManager = FakeAppUpdateManager(it).apply {
                setUpdateAvailable(100500)
                partiallyAllowedUpdateType = AppUpdateType.IMMEDIATE
            }
            it.updateWrapper = it.startImmediateUpdate(updateManager, it)

            assertTrue(updateManager.isImmediateFlowVisible)
            // Emulate update success
            it.passActivityResult(REQUEST_CODE_UPDATE, Activity.RESULT_OK)

            assertEquals(TestUpdateActivity.RESULT_SUCCESS, scenario.result.resultCode)
        }
    }

    @Test
    fun failsIfUpdateIsNotAvailable() {
        lateinit var updateManager: FakeAppUpdateManager
        val scenario = launch(TestUpdateActivity::class.java)
        scenario.onActivity {
            updateManager = FakeAppUpdateManager(it).apply {
                setUpdateNotAvailable()
            }
            it.startImmediateUpdate(updateManager, it)
        }

        assertEquals(TestUpdateActivity.RESULT_FAILURE, scenario.result.resultCode)
    }
}