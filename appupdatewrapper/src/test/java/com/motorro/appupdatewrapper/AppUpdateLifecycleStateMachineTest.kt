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

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.State.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nhaarman.mockitokotlin2.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
    fun cleansUpCurrentStateOnUpdate() {
        stateMachine.setUpdateState(state)
        stateMachine.setUpdateState(None())
        verify(state).cleanup()
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

    @Test
    fun initializes() {
        verify(lifecycle).addObserver(stateMachine)
        assertTrue { stateMachine.currentUpdateState is None}
    }

    @Test
    fun cleansUp() {
        stateMachine.setUpdateState(state)
        stateMachine.cleanup()
        verify(lifecycle).removeObserver(stateMachine)
        assertTrue { stateMachine.currentUpdateState is None}
    }
}