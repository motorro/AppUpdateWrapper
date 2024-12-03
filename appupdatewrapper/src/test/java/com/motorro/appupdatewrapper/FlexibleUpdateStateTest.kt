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
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.install.model.ActivityResult
import com.google.android.play.core.install.model.AppUpdateType.FLEXIBLE
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.LooperMode
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
internal class FlexibleUpdateStateTest: BaseAppUpdateStateTest() {
    @Test
    fun baseStateWillMarkCancellationTimeAndCompleteIfCancelled() {
        val state = FlexibleUpdateState.Initial().init()
        assertTrue(state.checkActivityResult(Activity.RESULT_CANCELED))
        verify(stateMachine.flowBreaker).saveTimeCanceled()
        verify(stateMachine).setUpdateState(any<Done>())
    }

    @Test
    fun whenStartedSetsInitialState() {
        FlexibleUpdateState.start(stateMachine)
        verify(stateMachine).setUpdateState(any<FlexibleUpdateState.Initial>())
    }

    @Test
    fun initialStateStartsUpdateOnStart() {
        val state = FlexibleUpdateState.Initial().init()
        state.onStart()
        verify(stateMachine).setUpdateState(any<FlexibleUpdateState.Checking>())
    }

    @Test
    fun checkingStateWillCheckUpdateOnStart() {
        val state = FlexibleUpdateState.Checking().init()
        state.onStart()
        verify(view).updateChecking()
        verify(updateManager).appUpdateInfo
    }

    @Test
    fun checkingStateWontRunIfBroken() {
        whenever(breaker.isEnoughTimePassedSinceLatestCancel()).thenReturn(false)
        val state = FlexibleUpdateState.Checking().init()
        state.onStart()
        verify(view, never()).updateChecking()
        verify(updateManager, never()).appUpdateInfo
        verify(stateMachine).setUpdateState(any<Done>())
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun checkingStateWillSetUpdateConsentStateIfUpdateFound() {
        updateManager.setUpdateAvailable(100500)
        val state = FlexibleUpdateState.Checking().init()
        state.onStart()
        shadowOf(getMainLooper()).idle()
        verify(stateMachine).setUpdateState(any<FlexibleUpdateState.UpdateConsent>())
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun checkingStateWillSetDownloadingIfPendingOrDownloading() {
        updateManager.setUpdateAvailable(100500)
        updateManager.withInfo {
            startUpdateFlowForResult(it, FLEXIBLE, activity, 100)
            assertTrue(isConfirmationDialogVisible)
            userAcceptsUpdate()

            val state1 = FlexibleUpdateState.Checking().init()
            state1.onStart()
            shadowOf(getMainLooper()).idle()
            verify(stateMachine).setUpdateState(any<FlexibleUpdateState.Downloading>())

            downloadStarts()
            val state2 = FlexibleUpdateState.Checking().init()
            state2.onStart()
            shadowOf(getMainLooper()).idle()
            verify(stateMachine, times(2)).setUpdateState(any<FlexibleUpdateState.Downloading>())
        }
        shadowOf(getMainLooper()).idle()
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun checkingStateWillSetInstallConsentWhenDownloaded() {
        updateManager.setUpdateAvailable(100500)
        updateManager.withInfo {
            startUpdateFlowForResult(it, FLEXIBLE, activity, 100)
            assertTrue(isConfirmationDialogVisible)
            userAcceptsUpdate()
            downloadStarts()
            downloadCompletes()

            val state = FlexibleUpdateState.Checking().init()
            state.onStart()
            shadowOf(getMainLooper()).idle()
            verify(stateMachine).setUpdateState(any<FlexibleUpdateState.InstallConsent>())
        }
        shadowOf(getMainLooper()).idle()
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun checkingStateWillSetCompleteUpdateWhenInstalling() {
        updateManager.setUpdateAvailable(100500)
        updateManager.withInfo {
            startUpdateFlowForResult(it, FLEXIBLE, activity, 100)
            assertTrue(isConfirmationDialogVisible)
            userAcceptsUpdate()
            downloadStarts()
            downloadCompletes()
            completeUpdate()

            val state = FlexibleUpdateState.Checking().init()
            state.onStart()
            shadowOf(getMainLooper()).idle()
            verify(stateMachine).setUpdateState(any<FlexibleUpdateState.CompleteUpdate>())
        }
        shadowOf(getMainLooper()).idle()
    }

    @Test
    fun checkingStateWillNotProceedIfStoppedBeforeTaskCompletes() {
        val updateInfo = mock<AppUpdateInfo> {
            on { updateAvailability() } doReturn UpdateAvailability.UPDATE_AVAILABLE
            on { installStatus() } doReturn InstallStatus.UNKNOWN
        }
        val testTask = createTestInfoTask()
        val testUpdateManager: AppUpdateManager = mock {
            on { this.appUpdateInfo } doReturn testTask
        }
        whenever(stateMachine.updateManager).thenReturn(testUpdateManager)

        val state = FlexibleUpdateState.Checking().init()
        state.onStart()
        state.onStop()
        state.cleanup()
        testTask.succeed(updateInfo)
        verify(stateMachine).setUpdateState(any<Done>())
        verify(stateMachine, never()).setUpdateState(any<FlexibleUpdateState.UpdateConsent>())
    }

    @Test
    fun checkingStateWillReportErrorIfUpdateCheckFails() {
        val error = RuntimeException("Update failed")
        val testTask = createTestInfoTask()
        val testUpdateManager: AppUpdateManager = mock {
            on { this.appUpdateInfo } doReturn testTask
        }
        whenever(stateMachine.updateManager).thenReturn(testUpdateManager)

        val state = FlexibleUpdateState.Checking().init()
        state.onStart()
        testTask.fail(error)
        argumentCaptor<AppUpdateState>().apply {
            verify(stateMachine).setUpdateState(capture())
            val newState = firstValue as Error
            val stateError = newState.error
            assertEquals(AppUpdateException.ERROR_UPDATE_CHECK_FAILED, stateError.message)
            assertEquals(error, stateError.cause)
        }
    }

    @Test
    fun checkingStateWillNotProceedIfStoppedBeforeTaskFails() {
        val error = RuntimeException("Update failed")
        val testTask = createTestInfoTask()
        val testUpdateManager: AppUpdateManager = mock {
            on { this.appUpdateInfo } doReturn testTask
        }
        whenever(stateMachine.updateManager).thenReturn(testUpdateManager)

        val state = FlexibleUpdateState.Checking().init()
        state.onStart()
        state.onStop()
        state.cleanup()
        testTask.fail(error)
        verify(stateMachine).setUpdateState(any<Done>())
        verify(stateMachine, never()).setUpdateState(any<Error>())
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun downloadingStateWillUpdateUpdateViewWhenDownloadStarts() {
        val state = FlexibleUpdateState.Downloading().init()
        state.onStart()
        shadowOf(getMainLooper()).idle()
        verify(view).updateDownloadStarts()
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun downloadingStateWillSubscribeEventsOnResume() {
        val state = FlexibleUpdateState.Downloading().init()
        state.onResume()
        shadowOf(getMainLooper()).idle()
        verify(updateManager).registerListener(any())
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun downloadingStateWillRemoveListenerOnClear() {
        val state = FlexibleUpdateState.Downloading().init()
        state.cleanup()
        shadowOf(getMainLooper()).idle()
        verify(updateManager).unregisterListener(any())
    }

    @Test
    fun downloadingStateWillSwitchToCheckingOnPause() {
        val state = FlexibleUpdateState.Downloading().init()
        state.onPause()
        verify(stateMachine).setUpdateState(any<FlexibleUpdateState.Checking>())
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun downloadingStateWillSetCompleteUpdateAndMarkCancellationTimeWhenCancelled() {
        updateManager.setUpdateAvailable(100500)
        updateManager.withInfo {
            startUpdateFlowForResult(it, FLEXIBLE, activity, 100)
            assertTrue(isConfirmationDialogVisible)
            userAcceptsUpdate()
            downloadStarts()

            val state = FlexibleUpdateState.Downloading().init()
            state.onResume()
            shadowOf(getMainLooper()).idle()

            userCancelsDownload()
            verify(stateMachine).setUpdateState(any<Done>())
            verify(breaker).saveTimeCanceled()
        }
        shadowOf(getMainLooper()).idle()
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun downloadingStateWillUpdateProgress() {
        updateManager.setUpdateAvailable(100500)
        updateManager.withInfo {
            startUpdateFlowForResult(it, FLEXIBLE, activity, 100)
            assertTrue(isConfirmationDialogVisible)
            userAcceptsUpdate()
            downloadStarts()
            setTotalBytesToDownload(100)

            val state = FlexibleUpdateState.Downloading().init()
            state.onResume()
            shadowOf(getMainLooper()).idle()

            setBytesDownloaded(50)
            verify(view).updateDownloadProgress(50, 100)
        }
        shadowOf(getMainLooper()).idle()
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun downloadingStateWillSetInstallConsentWhenDownloaded() {
        updateManager.setUpdateAvailable(100500)
        updateManager.withInfo {
            startUpdateFlowForResult(it, FLEXIBLE, activity, 100)
            assertTrue(isConfirmationDialogVisible)
            userAcceptsUpdate()
            downloadStarts()

            val state = FlexibleUpdateState.Downloading().init()
            state.onResume()
            shadowOf(getMainLooper()).idle()

            downloadCompletes()
            verify(stateMachine).setUpdateState(any<FlexibleUpdateState.InstallConsent>())
        }
        shadowOf(getMainLooper()).idle()
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun downloadingStateWillSetCompleteUpdateWhenInstalling() {
        updateManager.setUpdateAvailable(100500)
        updateManager.withInfo {
            startUpdateFlowForResult(it, FLEXIBLE, activity, 100)
            assertTrue(isConfirmationDialogVisible)
            userAcceptsUpdate()
            downloadStarts()

            val state = FlexibleUpdateState.Downloading().init()
            state.onResume()
            shadowOf(getMainLooper()).idle()

            downloadCompletes()
            completeUpdate()
            verify(stateMachine).setUpdateState(any<FlexibleUpdateState.CompleteUpdate>())
        }
        shadowOf(getMainLooper()).idle()
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun downloadingStateWillReportErrorOnError() {
        updateManager.setUpdateAvailable(100500)
        updateManager.withInfo {
            startUpdateFlowForResult(it, FLEXIBLE, activity, 100)
            assertTrue(isConfirmationDialogVisible)
            userAcceptsUpdate()
            downloadStarts()

            val state = FlexibleUpdateState.Downloading().init()
            state.onResume()
            shadowOf(getMainLooper()).idle()

            downloadFails()
            verify(stateMachine).setUpdateState(any<Error>())
        }
        shadowOf(getMainLooper()).idle()
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun updateConsentStateWillAskForInstallConsentOnResume() {
        updateManager.setUpdateAvailable(100500)
        updateManager.withInfo { info ->
            val state = FlexibleUpdateState.UpdateConsent(info).init()
            state.onResume()
            assertTrue(isConfirmationDialogVisible)
            verify(stateMachine).setUpdateState(any<FlexibleUpdateState.UpdateConsentCheck>())
            verify(breaker).isUpdateValuable(info)
        }
        shadowOf(getMainLooper()).idle()
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun updateConsentStateWillNotAskForInstallConsentIfBroken() {
        updateManager.setUpdateAvailable(100500)
        updateManager.withInfo { info ->
            whenever(breaker.isUpdateValuable(any())).thenReturn(false)
            val state = FlexibleUpdateState.UpdateConsent(info).init()
            state.onResume()
            assertFalse(isConfirmationDialogVisible)
            verify(stateMachine, never()).setUpdateState(any<FlexibleUpdateState.UpdateConsentCheck>())
            verify(breaker).isUpdateValuable(info)
        }
        shadowOf(getMainLooper()).idle()
    }

    @Test
    fun updateConsentCheckStateWillSetDownloadingIfConfirmed() {
        val state = FlexibleUpdateState.UpdateConsentCheck().init()
        assertTrue(state.checkActivityResult(Activity.RESULT_OK))
        verify(stateMachine).setUpdateState(any<FlexibleUpdateState.Downloading>())
    }

    @Test
    fun updateConsentCheckStateWillMarkCancellationTimeAndCompleteIfCancelled() {
        val state = FlexibleUpdateState.UpdateConsentCheck().init()
        assertTrue(state.checkActivityResult(Activity.RESULT_CANCELED))
        verify(stateMachine, never()).setUpdateState(any<FlexibleUpdateState.Downloading>())
        verify(stateMachine, never()).setUpdateState(any<Error>())
        verify(stateMachine.flowBreaker).saveTimeCanceled()
        verify(stateMachine).setUpdateState(any<Done>())
    }

    @Test
    fun updateConsentCheckStateWillReportUpdateError() {
        val state = FlexibleUpdateState.UpdateConsentCheck().init()
        assertTrue(state.checkActivityResult(ActivityResult.RESULT_IN_APP_UPDATE_FAILED))
        verify(stateMachine, never()).setUpdateState(any<Done>())
        verify(stateMachine).setUpdateState(check {
            it as Error
            assertEquals(AppUpdateException.ERROR_UPDATE_FAILED, it.error.message)
        })
    }

    @Test
    fun updateConsentCheckStateWillReportErrorOnUnknownResult() {
        val state = FlexibleUpdateState.UpdateConsentCheck().init()
        assertTrue(state.checkActivityResult(Activity.CONTEXT_RESTRICTED))
        verify(stateMachine, never()).setUpdateState(any<Done>())
        verify(stateMachine).setUpdateState(check {
            it as Error
            assertEquals(AppUpdateException.ERROR_UNKNOWN_UPDATE_RESULT, it.error.message)
        })
    }

    @Test
    fun installConsentStateWillCallUpdateReadyOnResumeAndSwitchToConsentCheck() {
        val state = FlexibleUpdateState.InstallConsent().init()
        state.onResume()
        verify(view).updateReady()
        verify(stateMachine).setUpdateState(any<FlexibleUpdateState.InstallConsentCheck>())
    }

    @Test
    fun installConsentStateWillCompleteIfBroken() {
        whenever(breaker.isEnoughTimePassedSinceLatestCancel()).thenReturn(false)
        val state = FlexibleUpdateState.InstallConsent().init()
        state.onResume()
        verify(view, never()).updateReady()
        verify(stateMachine).setUpdateState(any<Done>())
    }

    @Test
    fun installConsentCheckWillCompleteUpdateOnConfirm() {
        val state = FlexibleUpdateState.InstallConsentCheck().init()
        state.userConfirmedUpdate()
        verify(stateMachine).setUpdateState(any<FlexibleUpdateState.CompleteUpdate>())
    }

    @Test
    fun installConsentCheckWillMarkCancelTimeAndCompleteOnCancel() {
        val state = FlexibleUpdateState.InstallConsentCheck().init()
        state.userCanceledUpdate()
        verify(stateMachine, never()).setUpdateState(any<FlexibleUpdateState.CompleteUpdate>())
        verify(breaker).saveTimeCanceled()
        verify(stateMachine).setUpdateState(any<Done>())
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun completeUpdateStateWillCompleteUpdateOnStart() {
        updateManager.setUpdateAvailable(100500)
        updateManager.withInfo {
            startUpdateFlowForResult(it, FLEXIBLE, activity, 100)
            userAcceptsUpdate()
            downloadStarts()
            downloadCompletes()

            val state = FlexibleUpdateState.CompleteUpdate().init()
            state.onStart()
            shadowOf(getMainLooper()).idle()
            verify(updateManager).completeUpdate()
            verify(stateMachine).setUpdateState(any<Done>())
            verify(view).updateInstallUiVisible()
        }
        shadowOf(getMainLooper()).idle()
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun completeUpdateStateWillReportErrorIfUpdateFails() {
        updateManager.setUpdateAvailable(100500)
        updateManager.withInfo {
            val state = FlexibleUpdateState.CompleteUpdate().init()
            state.onStart()
            shadowOf(getMainLooper()).idle()
            verify(updateManager).completeUpdate()
            verify(stateMachine).setUpdateState(any<Error>())
        }
        shadowOf(getMainLooper()).idle()
    }

    @Test
    fun completeUpdateStateWillCompleteIfStoppedBeforeTaskCompletes() {
        val testTask = createTestInstallTask()
        val testUpdateManager: AppUpdateManager = mock {
            on { this.completeUpdate() } doReturn testTask
        }
        whenever(stateMachine.updateManager).thenReturn(testUpdateManager)

        val state = FlexibleUpdateState.CompleteUpdate().init()
        state.onStart()
        state.onStop()
        state.cleanup()
        testTask.succeed(null)
        verify(stateMachine).setUpdateState(any<Done>())
    }

    @Test
    fun completeUpdateStateWillNotProceedIfStoppedBeforeTaskFails() {
        val error = RuntimeException("Update failed")
        val testTask = createTestInstallTask()
        val testUpdateManager: AppUpdateManager = mock {
            on { this.completeUpdate() } doReturn testTask
        }
        whenever(stateMachine.updateManager).thenReturn(testUpdateManager)

        val state = FlexibleUpdateState.CompleteUpdate().init()
        state.onStart()
        state.onStop()
        state.cleanup()
        testTask.fail(error)
        verify(stateMachine).setUpdateState(any<Done>())
        verify(stateMachine, never()).setUpdateState(any<Error>())
    }
}