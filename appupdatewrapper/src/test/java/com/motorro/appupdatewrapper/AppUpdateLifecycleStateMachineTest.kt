package com.motorro.appupdatewrapper

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.State.*
import androidx.lifecycle.LifecycleOwner
import com.nhaarman.mockito_kotlin.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class AppUpdateLifecycleStateMachineTest: TestAppTest() {
    private lateinit var lifecycle: Lifecycle
    private lateinit var stateMachine: AppUpdateLifecycleStateMachine
    private lateinit var state: AppUpdateState

    @Before
    fun init() {
        lifecycle = mock {
            on { currentState } doReturn DESTROYED
        }

        val lifecycleOwner: LifecycleOwner = mock {
            on { lifecycle } doReturn lifecycle
        }
        stateMachine = AppUpdateLifecycleStateMachine(lifecycleOwner, mock(), mock())

        state = spy()
    }

    @Test
    fun setsCurrentUpdateState() {
        stateMachine.setUpdateState(state)
        assertEquals(stateMachine, state.stateMachine)

        stateMachine.onStart()
        verify(state).onStart()
    }

    @Test
    fun doesNotCallSetStateLifecycleMethodsIfNotStartedAtLeast() {
        stateMachine.setUpdateState(state)
        verify(state, never()).onStart()
        verify(state, never()).onResume()
    }

    @Test
    fun callsStateOnStartIfLifecycleStarted() {
        whenever(lifecycle.currentState).thenReturn(STARTED)
        stateMachine.setUpdateState(state)
        verify(state).onStart()
        verify(state, never()).onResume()
    }

    @Test
    fun callsStateOnStartAndOnResumedIfLifecycleResumed() {
        whenever(lifecycle.currentState).thenReturn(RESUMED)
        stateMachine.setUpdateState(state)
        verify(state).onStart()
        verify(state).onResume()
    }
}