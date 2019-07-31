package com.motorro.appupdatewrapper

import android.app.Activity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.nhaarman.mockitokotlin2.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class ImmediateUpdateStateTest: TestAppTest() {
    private lateinit var activity: Activity
    private lateinit var view: AppUpdateView
    private lateinit var stateMachine: AppUpdateStateMachine
    private lateinit var updateManager: FakeAppUpdateManager

    @Before
    fun init() {
        activity = mock()
        view = mock {
            on { activity } doReturn activity
        }
        updateManager = spy(FakeAppUpdateManager(application))
        stateMachine = mock {
            on { view } doReturn view
            on { updateManager } doReturn updateManager
        }
    }

    private fun ImmediateUpdateState.init() = this.apply {
        stateMachine = this@ImmediateUpdateStateTest.stateMachine
    }

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
        verify(updateManager).appUpdateInfo
    }

    @Test
    fun checkingStateWillSetUpdateStateIfUpdateFound() {
        updateManager.setUpdateAvailable(100500)
        updateManager.partiallyAllowedUpdateType = AppUpdateType.IMMEDIATE
        val state = ImmediateUpdateState.Checking().init()
        state.onStart()
        verify(stateMachine).setUpdateState(check { assertTrue { it is ImmediateUpdateState.Update } })
    }

    @Test
    fun checkingStateWillSetUpdateStateIfAlreadyUpdating() {
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
        testTask.succeed(updateInfo)
        verify(stateMachine).setUpdateState(check { assertTrue { it is ImmediateUpdateState.Update } })
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
            val newState = firstValue as ImmediateUpdateState.Failed
            val stateError = newState.error
            assertEquals(AppUpdateException.ERROR_UPDATE_FAILED, stateError.message)
            assertEquals(error, stateError.cause)
        }
    }

    @Test
    fun checkingStateWillSetFailedStateIfUpdateTypeNotSupported() {
        val updateInfo = createUpdateInfo(
            UpdateAvailability.UNKNOWN,
            InstallStatus.UNKNOWN,
            immediateAvailable = false
        )
        val testTask = createTestInfoTask()
        val testUpdateManager: AppUpdateManager = mock {
            on { this.appUpdateInfo } doReturn testTask
        }
        whenever(stateMachine.updateManager).thenReturn(testUpdateManager)

        val state = ImmediateUpdateState.Checking().init()
        state.onStart()
        testTask.succeed(updateInfo)
        argumentCaptor<AppUpdateState>().apply {
            verify(stateMachine).setUpdateState(capture())
            val newState = firstValue as ImmediateUpdateState.Failed
            val stateError = newState.error
            assertEquals(AppUpdateException.ERROR_UPDATE_TYPE_NOT_ALLOWED, stateError.message)
        }
    }

    @Test
    fun checkingStateWillSetFailedStateIfUpdateNotAvailable() {
        val updateInfo = createUpdateInfo(
            UpdateAvailability.UPDATE_NOT_AVAILABLE,
            InstallStatus.UNKNOWN
        )
        val testTask = createTestInfoTask()
        val testUpdateManager: AppUpdateManager = mock {
            on { this.appUpdateInfo } doReturn testTask
        }
        whenever(stateMachine.updateManager).thenReturn(testUpdateManager)

        val state = ImmediateUpdateState.Checking().init()
        state.onStart()
        testTask.succeed(updateInfo)
        argumentCaptor<AppUpdateState>().apply {
            verify(stateMachine).setUpdateState(capture())
            val newState = firstValue as ImmediateUpdateState.Failed
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
        verify(stateMachine).setUpdateState(any<ImmediateUpdateState.Initial>())
        verify(stateMachine, never()).setUpdateState(any<ImmediateUpdateState.Update>())
        verify(stateMachine, never()).setUpdateState(any<ImmediateUpdateState.Failed>())
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
        verify(stateMachine).setUpdateState(any<ImmediateUpdateState.Initial>())
        verify(stateMachine, never()).setUpdateState(any<ImmediateUpdateState.Update>())
        verify(stateMachine, never()).setUpdateState(any<ImmediateUpdateState.Failed>())
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
        verify(stateMachine).setUpdateState(check { assertTrue { it is ImmediateUpdateState.Done } })
    }

    @Test
    fun doneStateWillCompleteViewOnResume() {
        val state = ImmediateUpdateState.Done().init()
        state.onResume()

        verify(view).updateComplete()
        verify(stateMachine).setUpdateState(check { assertTrue { NONE == it } })
    }

    @Test
    fun failedStateWillFailOnResume() {
        val error = AppUpdateException(AppUpdateException.ERROR_NO_IMMEDIATE_UPDATE)

        val state = ImmediateUpdateState.Failed(error).init()
        state.onResume()

        verify(view).updateFailed(error)
        verify(stateMachine).setUpdateState(check { assertTrue { NONE == it } })
    }
}