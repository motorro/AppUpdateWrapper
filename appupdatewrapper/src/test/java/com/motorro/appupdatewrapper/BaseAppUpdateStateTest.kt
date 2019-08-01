package com.motorro.appupdatewrapper

import android.app.Activity
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import org.junit.Before

internal abstract class BaseAppUpdateStateTest: TestAppTest() {
    protected lateinit var activity: Activity
    protected lateinit var view: AppUpdateView
    protected lateinit var stateMachine: AppUpdateStateMachine
    protected lateinit var updateManager: FakeAppUpdateManager
    protected lateinit var breaker: UpdateFlowBreaker

    @Before
    fun init() {
        activity = mock()
        view = mock {
            on { activity } doReturn activity
        }
        updateManager = spy(FakeAppUpdateManager(application))
        breaker = mock {
            on { isEnoughTimePassedSinceLatestCancel() } doReturn true
        }
        stateMachine = mock {
            on { view } doReturn view
            on { updateManager } doReturn updateManager
            on { flowBreaker } doReturn breaker
        }
    }

    protected fun <T: AppUpdateState> T.init() = this.apply {
        stateMachine = this@BaseAppUpdateStateTest.stateMachine
    }
}