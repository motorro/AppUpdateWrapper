package com.motorro.appupdatewrapper

import android.app.Activity
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import com.google.android.play.core.install.model.AppUpdateType
import com.nhaarman.mockito_kotlin.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ImmediateUpdateTest: TestAppTest() {
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
        updateManager = FakeAppUpdateManager(application)
        stateMachine = mock {
            on { view } doReturn view
            on { updateManager } doReturn updateManager
        }
    }

    private fun ImmediateUpdate.init() = this.apply {
        stateMachine = this@ImmediateUpdateTest.stateMachine
    }

    @Test
    fun whenStartedSetsInitialState() {
        ImmediateUpdate.start(stateMachine)
        verify(stateMachine).setUpdateState(check { it is ImmediateUpdate.Initial })
    }

    @Test
    fun initialStateStartsUpdateOnStart() {
        val state = ImmediateUpdate.Initial().init()
        state.onStart()
        verify(stateMachine).setUpdateState(check { it is ImmediateUpdate.Checking })
    }

    @Test
    fun checkingStateWillCheckUpdateOnStart() {
        val state = ImmediateUpdate.Checking().init()
        state.onStart()
        verify(updateManager).appUpdateInfo
    }

    @Test
    fun checkingStateWillSetUpdateStateIfUpdateFound() {
        updateManager.setUpdateAvailable(100500)
        updateManager.partiallyAllowedUpdateType = AppUpdateType.IMMEDIATE
        val state = ImmediateUpdate.Checking().init()
        state.onStart()
        verify(stateMachine).setUpdateState(check { it is ImmediateUpdate.Update })
    }

    @Test
    fun checkingStateWillSetUpdateStateIfAlreadyUpdating() {
        updateManager.setUpdateAvailable(100500)
        updateManager.partiallyAllowedUpdateType = AppUpdateType.IMMEDIATE

        val state1 = ImmediateUpdate.Checking().apply {
            stateMachine = mock { on { updateManager } doReturn updateManager }
        }
        state1.onStart()
        updateManager.userAcceptsUpdate()
        updateManager.downloadStarts()

        val state2 = ImmediateUpdate.Checking().init()
        state2.onStart()
        verify(stateMachine).setUpdateState(check { it is ImmediateUpdate.Update })
    }

}