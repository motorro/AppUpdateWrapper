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
import androidx.lifecycle.Lifecycle.State.INITIALIZED
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class AppUpdateLifecycleStateMachineTest: TestAppTest() {
    private lateinit var lifecycleOwner: TestLifecycleOwner
    private lateinit var stateMachine: AppUpdateLifecycleStateMachine
    private lateinit var state: AppUpdateState

    @Before
    fun init() {
        lifecycleOwner = TestLifecycleOwner(INITIALIZED)
        stateMachine = AppUpdateLifecycleStateMachine(lifecycleOwner.lifecycle, mock(), mock(), mock())

        state = spy()
    }

    @Test
    fun setsCurrentUpdateState() {
        stateMachine.setUpdateState(state)
        assertEquals(stateMachine, state.stateMachine)

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
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
    fun followsLifecycle() {
        stateMachine.setUpdateState(state)

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        verify(state).onStart()
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        verify(state).onResume()
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        verify(state).onPause()
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        verify(state).onStop()
    }

    @Test
    fun initializes() {
        assertEquals(1, lifecycleOwner.observerCount)
        assertTrue { stateMachine.currentUpdateState is None}
    }

    @Test
    fun cleansUp() {
        stateMachine.setUpdateState(state)
        stateMachine.cleanup()
        assertEquals(0, lifecycleOwner.observerCount)
        assertTrue { stateMachine.currentUpdateState is None }
    }
}