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
        lateinit var updateManager: FakeUpdateManagerWithContract
        val scenario = launchActivityForResult(TestUpdateActivity::class.java)
        scenario.onActivity { activity ->
            updateManager = FakeUpdateManagerWithContract(activity).apply {
                setUpdateAvailable(100500)
            }
            // Emulate update is accepted
            activity.createTestRegistry(Activity.RESULT_OK)
            activity.updateWrapper = activity.startFlexibleUpdate(updateManager, activity, flowBreaker)
            shadowOf(getMainLooper()).idle()
            assertTrue(updateManager.isConfirmationDialogVisible)

            // Emulate update is accepted
            updateManager.userAcceptsUpdate()
            updateManager.launchContract()
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
        lateinit var updateManager: FakeUpdateManagerWithContract
        val scenario = launchActivityForResult(TestUpdateActivity::class.java)
        scenario.onActivity { activity ->
            updateManager = FakeUpdateManagerWithContract(activity).apply {
                setUpdateAvailable(100500)
            }
            // Emulate update is rejected
            activity.createTestRegistry(Activity.RESULT_CANCELED)
            activity.updateWrapper = activity.startFlexibleUpdate(updateManager, activity, flowBreaker)
            shadowOf(getMainLooper()).idle()
            assertTrue(updateManager.isConfirmationDialogVisible)

            updateManager.userRejectsUpdate()
            updateManager.launchContract()
            shadowOf(getMainLooper()).idle()

            verify(flowBreaker).saveTimeCanceled()

            assertEquals(TestUpdateActivity.RESULT_SUCCESS, scenario.result.resultCode)
        }
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun willNotAskUpdateConsentIfAlreadyCancelled() {
        whenever(flowBreaker.isEnoughTimePassedSinceLatestCancel()).thenReturn(false)
        lateinit var updateManager: FakeUpdateManagerWithContract
        val scenario = launchActivityForResult(TestUpdateActivity::class.java)
        scenario.onActivity { activity ->
            updateManager = FakeUpdateManagerWithContract(activity).apply {
                setUpdateAvailable(100500)
            }
            // Emulate update is accepted
            activity.createTestRegistry(Activity.RESULT_OK)
            activity.updateWrapper = activity.startFlexibleUpdate(updateManager, activity, flowBreaker)
            shadowOf(getMainLooper()).idle()
            assertFalse(updateManager.isConfirmationDialogVisible)

            assertEquals(TestUpdateActivity.RESULT_SUCCESS, scenario.result.resultCode)
        }
    }
}