package com.motorro.appupdatewrapper

import android.app.Activity
import android.os.Looper.getMainLooper
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.AppUpdateType.FLEXIBLE
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.nhaarman.mockitokotlin2.*
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
    fun checkingStateWillSetUpdateConsentStateIfUpdateFound() {
        updateManager.setUpdateAvailable(100500)
        updateManager.partiallyAllowedUpdateType = FLEXIBLE
        val state = FlexibleUpdateState.Checking().init()
        state.onStart()
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
        val updateInfo = createUpdateInfo(
            UpdateAvailability.UPDATE_AVAILABLE,
            InstallStatus.UNKNOWN
        )
        val testTask = createTestInfoTask()
        val testUpdateManager: AppUpdateManager = mock {
            on { this.appUpdateInfo } doReturn testTask
        }
        whenever(stateMachine.updateManager).thenReturn(testUpdateManager)

        val state = FlexibleUpdateState.Checking().init()
        state.onStart()
        state.onStop()
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
        testTask.fail(error)
        verify(stateMachine).setUpdateState(any<Done>())
        verify(stateMachine, never()).setUpdateState(any<Error>())
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
    fun downloadingStateWillSetCompleteUpdateWhenCancelled() {
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
        updateManager.withInfo {
            val state = FlexibleUpdateState.UpdateConsent(it).init()
            state.onResume()
            assertTrue(isConfirmationDialogVisible)
            verify(stateMachine).setUpdateState(any<FlexibleUpdateState.UpdateConsentCheck>())
        }
        shadowOf(getMainLooper()).idle()
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun updateConsentStateWillNotAskForInstallConsentIfBroken() {
        updateManager.setUpdateAvailable(100500)
        updateManager.withInfo {
            whenever(breaker.isEnoughTimePassedSinceLatestCancel()).thenReturn(false)
            val state = FlexibleUpdateState.UpdateConsent(it).init()
            state.onResume()
            assertFalse(isConfirmationDialogVisible)
            verify(stateMachine, never()).setUpdateState(any<FlexibleUpdateState.UpdateConsentCheck>())
        }
        shadowOf(getMainLooper()).idle()
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun updateConsentStateWillReportErrorIfUpdateIsNotCompatible() {
        updateManager.setUpdateAvailable(100500)
        updateManager.partiallyAllowedUpdateType = AppUpdateType.IMMEDIATE
        updateManager.withInfo {
            val state = FlexibleUpdateState.UpdateConsent(it).init()
            state.onResume()
            assertFalse(isConfirmationDialogVisible)
            verify(stateMachine, never()).setUpdateState(any<FlexibleUpdateState.UpdateConsentCheck>())
            verify(stateMachine).setUpdateState(check { newState -> 
                val error = (newState as Error).error
                assertEquals(AppUpdateException.ERROR_UPDATE_TYPE_NOT_ALLOWED, error.message)
            })
        }
        shadowOf(getMainLooper()).idle()
    }

    @Test
    fun updateConsentCheckStateWillSetDownloadingIfConfirmed() {
        val state = FlexibleUpdateState.UpdateConsentCheck().init()
        assertTrue(state.checkActivityResult(REQUEST_CODE_UPDATE, Activity.RESULT_OK))
        verify(stateMachine).setUpdateState(any<FlexibleUpdateState.Downloading>())
    }

    @Test
    fun updateConsentCheckStateWillMarkCancellationTimeAndCompleteIfCancelled() {
        val state = FlexibleUpdateState.UpdateConsentCheck().init()
        assertTrue(state.checkActivityResult(REQUEST_CODE_UPDATE, Activity.RESULT_CANCELED))
        verify(stateMachine, never()).setUpdateState(any<FlexibleUpdateState.Downloading>())
        verify(stateMachine, never()).setUpdateState(any<Error>())
        verify(stateMachine.flowBreaker).saveTimeCanceled()
        verify(stateMachine).setUpdateState(any<Done>())
    }

    @Test
    fun updateConsentCheckStateWillNotHandleOtherRequests() {
        val state = FlexibleUpdateState.UpdateConsentCheck().init()
        assertFalse(state.checkActivityResult(10, Activity.RESULT_OK))
        verify(stateMachine, never()).setUpdateState(any())
    }

    @Test
    fun updateConsentCheckStateWillReportErrorOnUnknownResult() {
        val state = FlexibleUpdateState.UpdateConsentCheck().init()
        assertTrue(state.checkActivityResult(REQUEST_CODE_UPDATE, Activity.RESULT_FIRST_USER))
        verify(stateMachine, never()).setUpdateState(any<Done>())
        verify(stateMachine).setUpdateState(check {
            it as Error
            assertEquals(AppUpdateException.ERROR_UNKNOWN_UPDATE_RESULT, it.error.message)
        })
    }
}