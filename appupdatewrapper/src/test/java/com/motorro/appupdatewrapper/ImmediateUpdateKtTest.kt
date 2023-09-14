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
import android.os.Looper.getMainLooper
import androidx.test.core.app.ActivityScenario.launchActivityForResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import com.motorro.appupdatewrapper.AppUpdateWrapper.Companion.REQUEST_CODE_UPDATE
import com.motorro.appupdatewrapper.testapp.TestUpdateActivity
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.LooperMode
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class ImmediateUpdateKtTest: TestAppTest() {
    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun startsImmediateUpdateIfAvailable() {
        lateinit var updateManager: FakeAppUpdateManager
        val scenario = launchActivityForResult(TestUpdateActivity::class.java)
        scenario.onActivity {
            updateManager = FakeAppUpdateManager(it).apply {
                setUpdateAvailable(100500)
            }
            it.updateWrapper = it.startImmediateUpdate(updateManager, it)
            shadowOf(getMainLooper()).idle()

            assertTrue(updateManager.isImmediateFlowVisible)
            // Emulate update success
            it.passActivityResult(REQUEST_CODE_UPDATE, Activity.RESULT_OK)

            assertEquals(TestUpdateActivity.RESULT_SUCCESS, scenario.result.resultCode)
        }
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun failsIfUpdateIsNotAvailable() {
        lateinit var updateManager: FakeAppUpdateManager
        val scenario = launchActivityForResult(TestUpdateActivity::class.java)
        scenario.onActivity {
            updateManager = FakeAppUpdateManager(it).apply {
                setUpdateNotAvailable()
            }
            it.startImmediateUpdate(updateManager, it)
            shadowOf(getMainLooper()).idle()
        }

        assertEquals(TestUpdateActivity.RESULT_FAILURE, scenario.result.resultCode)
    }
}