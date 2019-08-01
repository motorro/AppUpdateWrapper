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