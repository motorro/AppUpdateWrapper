package com.motorro.appupdatewrapper

import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.install.model.AppUpdateType.IMMEDIATE
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.nhaarman.mockitokotlin2.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.LooperMode
import kotlin.test.assertEquals
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
            assertEquals(AppUpdateException.ERROR_UPDATE_FAILED, stateError.message)
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
            assertEquals(AppUpdateException.ERROR_NO_IMMEDIATE_UPDATE, stateError.message)
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
        verify(stateMachine).setUpdateState(check { assertTrue { it is Done } })
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
}