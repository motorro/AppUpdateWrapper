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

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nhaarman.mockitokotlin2.*
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
internal class AppUpdateStateTest: BaseAppUpdateStateTest() {

    @Test
    fun executesIfNotBroken() {
        var executed = false
        whenever(breaker.isEnoughTimePassedSinceLatestCancel()).thenReturn(true)
        val state = object : AppUpdateState() {
            fun check() {
                ifNotBroken { executed = true }
            }
        }.init()
        state.check()
        assertTrue(executed)
    }

    @Test
    fun setsDoneIfBroken() {
        var executed = false
        whenever(breaker.isEnoughTimePassedSinceLatestCancel()).thenReturn(false)
        val state = object : AppUpdateState() {
            fun check() {
                ifNotBroken { executed = true }
            }
        }.init()
        state.check()
        assertFalse(executed)
        verify(stateMachine).setUpdateState(check { assertTrue { it is Done } })
    }

    @Test
    fun doneStateWillCompleteViewOnResume() {
        val state = Done().init()
        state.onResume()

        verify(view).updateComplete()
        verify(stateMachine).setUpdateState(any<None>())
    }

    @Test
    fun errorStateWillReportErrorOnResume() {
        val error = AppUpdateException(AppUpdateException.ERROR_NO_IMMEDIATE_UPDATE)

        val state = Error(error).init()
        state.onResume()

        verify(view).nonCriticalUpdateError(error)
        verify(stateMachine).setUpdateState(any<Done>())
    }    

    @Test
    fun errorStateWillNotBotherIfBroken() {
        whenever(breaker.isEnoughTimePassedSinceLatestCancel()).thenReturn(false)
        val error = AppUpdateException(AppUpdateException.ERROR_NO_IMMEDIATE_UPDATE)

        val state = Error(error).init()
        state.onResume()

        verify(view, never()).updateFailed(error)
        verify(stateMachine).setUpdateState(any<Done>())
    }

    @Test
    fun failedStateWillFailOnResume() {
        val error = AppUpdateException(AppUpdateException.ERROR_NO_IMMEDIATE_UPDATE)

        val state = Failed(error).init()
        state.onResume()

        verify(view).updateFailed(error)
        verify(stateMachine).setUpdateState(any<None>())
    }    
}