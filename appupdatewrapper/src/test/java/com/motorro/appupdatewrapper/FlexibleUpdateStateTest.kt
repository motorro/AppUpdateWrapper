package com.motorro.appupdatewrapper

import android.os.Looper.getMainLooper
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.install.model.AppUpdateType.FLEXIBLE
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.nhaarman.mockitokotlin2.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.LooperMode
import kotlin.test.assertEquals
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
    fun initialStateWontRunIfBroken() {
        whenever(breaker.isEnoughTimePassedSinceLatestCancel()).thenReturn(false)
        val state = FlexibleUpdateState.Initial().init()
        state.onStart()
        verify(stateMachine, never()).setUpdateState(any<FlexibleUpdateState.Checking>())
        verify(stateMachine).setUpdateState(any<Done>())
    }

    @Test
    fun checkingStateWillCheckUpdateOnStart() {
        val state = FlexibleUpdateState.Checking().init()
        state.onStart()
        verify(updateManager).appUpdateInfo
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
        verify(stateMachine).setUpdateState(any<FlexibleUpdateState.Initial>())
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
        verify(stateMachine).setUpdateState(any<FlexibleUpdateState.Initial>())
        verify(stateMachine, never()).setUpdateState(any<Error>())
    }
}