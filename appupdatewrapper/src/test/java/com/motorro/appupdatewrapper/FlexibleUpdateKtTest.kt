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
import androidx.test.core.app.ActivityScenario.launch
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import com.motorro.appupdatewrapper.AppUpdateWrapper.Companion.REQUEST_CODE_UPDATE
import com.motorro.appupdatewrapper.testapp.TestUpdateActivity
import com.nhaarman.mockitokotlin2.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.LooperMode
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class FlexibleUpdateKtTest: TestAppTest() {

    private lateinit var flowBreaker: UpdateFlowBreaker

    @Before
    fun init() {
        flowBreaker = mock {
            on { isEnoughTimePassedSinceLatestCancel() } doReturn true
            on { isUpdateValuable(any()) } doReturn true
        }
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun startsFlexibleUpdateIfAvailable() {
        lateinit var updateManager: FakeAppUpdateManager
        val scenario = launch(TestUpdateActivity::class.java)
        scenario.onActivity {
            updateManager = FakeAppUpdateManager(it).apply {
                setUpdateAvailable(100500)
            }
            it.updateWrapper = it.startFlexibleUpdate(updateManager, it, flowBreaker)
            shadowOf(getMainLooper()).idle()
            assertTrue(updateManager.isConfirmationDialogVisible)

            // Emulate update is accepted
            updateManager.userAcceptsUpdate()
            it.passActivityResult(REQUEST_CODE_UPDATE, Activity.RESULT_OK)
            shadowOf(getMainLooper()).idle()

            updateManager.downloadStarts()
            updateManager.downloadCompletes()
            shadowOf(getMainLooper()).idle()

            // Install is auto-accepted in [TestUpdateActivity.updateReady]

            assertEquals(TestUpdateActivity.RESULT_SUCCESS, scenario.result.resultCode)
        }
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun cancelsUpdateOnUserReject() {
        lateinit var updateManager: FakeAppUpdateManager
        val scenario = launch(TestUpdateActivity::class.java)
        scenario.onActivity {
            updateManager = FakeAppUpdateManager(it).apply {
                setUpdateAvailable(100500)
            }
            it.updateWrapper = it.startFlexibleUpdate(updateManager, it, flowBreaker)
            shadowOf(getMainLooper()).idle()
            assertTrue(updateManager.isConfirmationDialogVisible)

            // Emulate update is rejected
            updateManager.userRejectsUpdate()
            it.passActivityResult(REQUEST_CODE_UPDATE, Activity.RESULT_CANCELED)
            shadowOf(getMainLooper()).idle()

            verify(flowBreaker).saveTimeCanceled()

            assertEquals(TestUpdateActivity.RESULT_SUCCESS, scenario.result.resultCode)
        }
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun willNotAskUpdateConsentIfAlreadyCancelled() {
        whenever(flowBreaker.isEnoughTimePassedSinceLatestCancel()).thenReturn(false)
        lateinit var updateManager: FakeAppUpdateManager
        val scenario = launch(TestUpdateActivity::class.java)
        scenario.onActivity {
            updateManager = FakeAppUpdateManager(it).apply {
                setUpdateAvailable(100500)
            }
            it.updateWrapper = it.startFlexibleUpdate(updateManager, it, flowBreaker)
            shadowOf(getMainLooper()).idle()
            assertFalse(updateManager.isConfirmationDialogVisible)

            assertEquals(TestUpdateActivity.RESULT_SUCCESS, scenario.result.resultCode)
        }
    }
}