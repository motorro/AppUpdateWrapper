package com.motorro.appupdatewrapper

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.State.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nhaarman.mockitokotlin2.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class AppUpdateLifecycleStateMachineTest: TestAppTest() {
    private lateinit var lifecycle: Lifecycle
    private lateinit var stateMachine: AppUpdateLifecycleStateMachine
    private lateinit var state: AppUpdateState

    @Before
    fun init() {
        lifecycle = mock {
            on { currentState } doReturn DESTROYED
        }
        stateMachine = AppUpdateLifecycleStateMachine(lifecycle, mock(), mock(), mock())

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