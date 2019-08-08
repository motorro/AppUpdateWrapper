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
import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.install.model.ActivityResult
import com.google.android.play.core.install.model.AppUpdateType.IMMEDIATE
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.motorro.appupdatewrapper.AppUpdateException.Companion.ERROR_NO_IMMEDIATE_UPDATE
import com.motorro.appupdatewrapper.AppUpdateException.Companion.ERROR_UPDATE_FAILED
import com.nhaarman.mockitokotlin2.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.LooperMode
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
internal class ImmediateUpdateStateTest: BaseAppUpdateStateTest() {

    @Test
    fun whenStartedSetsInitialState() {
        ImmediateUpdateState.start(stateMachine)
        verify(stateMachine).setUpdateState(check { assertTrue { it is ImmediateUpdateState.Initial } })
    }

    @Test
    fun initialStateStartsUpdateOnStart() {
        val state = ImmediateUpdateState.Initial().init()
        state.onStart()
        verify(stateMachine).setUpdateState(check { assertTrue { it is ImmediateUpdateState.Checking } })
    }

    @Test
    fun checkingStateWillCheckUpdateOnStart() {
        val state = ImmediateUpdateState.Checking().init()
        state.onStart()
        verify(view).updateChecking()
        verify(updateManager).appUpdateInfo
    }

    @Test
    fun checkingStateWillSetUpdateStateIfUpdateFound() {
        updateManager.setUpdateAvailable(100500)
        updateManager.partiallyAllowedUpdateType = IMMEDIATE
        val state = ImmediateUpdateState.Checking().init()
        state.onStart()
        verify(stateMachine).setUpdateState(check { assertTrue { it is ImmediateUpdateState.Update } })
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun checkingStateWillSetUpdateStateIfAlreadyUpdating() {
        updateManager.setUpdateAvailable(100500)
        updateManager.withInfo {
            startUpdateFlowForResult(it, IMMEDIATE, activity, 100)
            assertTrue(isImmediateFlowVisible)
            userAcceptsUpdate()

            val state = ImmediateUpdateState.Checking().init()
            state.onStart()
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            verify(stateMachine).setUpdateState(check { assertTrue { it is ImmediateUpdateState.Update } })
        }
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun checkingStateWillSetFailedStateIfUpdateCheckFails() {
        val error = RuntimeException("Update failed")
        val testTask = createTestInfoTask()
        val testUpdateManager: AppUpdateManager = mock {
            on { this.appUpdateInfo } doReturn testTask
        }
        whenever(stateMachine.updateManager).thenReturn(testUpdateManager)

        val state = ImmediateUpdateState.Checking().init()
        state.onStart()
        testTask.fail(error)
        argumentCaptor<AppUpdateState>().apply {
            verify(stateMachine).setUpdateState(capture())
            val newState = firstValue as Failed
            val stateError = newState.error
            assertEquals(ERROR_UPDATE_FAILED, stateError.message)
            assertEquals(error, stateError.cause)
        }
    }

    @Test
    fun checkingStateWillSetFailedStateIfUpdateNotAvailable() {
        updateManager.setUpdateNotAvailable()

        val state = ImmediateUpdateState.Checking().init()
        state.onStart()
        argumentCaptor<AppUpdateState>().apply {
            verify(stateMachine).setUpdateState(capture())
            val newState = firstValue as Failed
            val stateError = newState.error
            assertEquals(ERROR_NO_IMMEDIATE_UPDATE, stateError.message)
        }
    }

    @Test
    fun checkingStateWillNotProceedIfStoppedBeforeTaskCompletes() {
        val updateInfo = createUpdateInfo(
            UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS,
            InstallStatus.UNKNOWN
        )
        val testTask = createTestInfoTask()
        val testUpdateManager: AppUpdateManager = mock {
            on { this.appUpdateInfo } doReturn testTask
        }
        whenever(stateMachine.updateManager).thenReturn(testUpdateManager)

        val state = ImmediateUpdateState.Checking().init()
        state.onStart()
        state.onStop()
        state.cleanup()
        testTask.succeed(updateInfo)
        verify(stateMachine).setUpdateState(any<Done>())
        verify(stateMachine, never()).setUpdateState(any<ImmediateUpdateState.Update>())
        verify(stateMachine, never()).setUpdateState(any<Failed>())
    }

    @Test
    fun checkingStateWillNotProceedIfStoppedBeforeTaskFails() {
        val error = RuntimeException("Update failed")
        val testTask = createTestInfoTask()
        val testUpdateManager: AppUpdateManager = mock {
            on { this.appUpdateInfo } doReturn testTask
        }
        whenever(stateMachine.updateManager).thenReturn(testUpdateManager)

        val state = ImmediateUpdateState.Checking().init()
        state.onStart()
        state.onStop()
        state.cleanup()
        testTask.fail(error)
        verify(stateMachine).setUpdateState(any<Done>())
        verify(stateMachine, never()).setUpdateState(any<ImmediateUpdateState.Update>())
        verify(stateMachine, never()).setUpdateState(any<Failed>())
    }

    @Test
    fun updatingStateWillStartImmediateUpdateOnResume() {
        val updateInfo = createUpdateInfo(
            UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS,
            InstallStatus.UNKNOWN
        )

        val state = ImmediateUpdateState.Update(updateInfo).init()
        state.onResume()

        assertTrue(updateManager.isImmediateFlowVisible)
        verify(view).updateInstallUiVisible()
        verify(stateMachine).setUpdateState(check { assertTrue { it is ImmediateUpdateState.UpdateUiCheck } })
    }

    @Test
    fun updatingStateWillSetFailedStateIfUpdateTypeNotSupported() {
        val updateInfo = createUpdateInfo(
            UpdateAvailability.UNKNOWN,
            InstallStatus.UNKNOWN,
            immediateAvailable = false
        )

        val state = ImmediateUpdateState.Update(updateInfo).init()
        state.onResume()
        argumentCaptor<AppUpdateState>().apply {
            verify(stateMachine).setUpdateState(capture())
            val newState = firstValue as Failed
            val stateError = newState.error
            assertEquals(AppUpdateException.ERROR_UPDATE_TYPE_NOT_ALLOWED, stateError.message)
        }
    }

    @Test
    fun updateUiCheckStateWillCompleteIfUpdateSucceeds() {
        val state = ImmediateUpdateState.UpdateUiCheck().init()
        assertTrue(state.checkActivityResult(REQUEST_CODE_UPDATE, Activity.RESULT_OK))
        verify(stateMachine).setUpdateState(any<Done>())
    }

    @Test
    fun updateUiCheckStateWillNotHandleOtherRequests() {
        val state = ImmediateUpdateState.UpdateUiCheck().init()
        assertFalse(state.checkActivityResult(10, Activity.RESULT_OK))
        verify(stateMachine, never()).setUpdateState(any())
    }

    @Test
    fun updateUiCheckStateWillFailOnNotOkResult() {
        val state = ImmediateUpdateState.UpdateUiCheck().init()
        assertTrue(state.checkActivityResult(REQUEST_CODE_UPDATE, ActivityResult.RESULT_IN_APP_UPDATE_FAILED))
        verify(stateMachine, never()).setUpdateState(any<Done>())
        verify(stateMachine).setUpdateState(check {
            it as Failed
            assertEquals(ERROR_UPDATE_FAILED, it.error.message)
        })
    }
}