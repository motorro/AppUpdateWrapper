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

import android.app.Activity
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import com.nhaarman.mockitokotlin2.any
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
            on { isUpdateValuable(any()) } doReturn true
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