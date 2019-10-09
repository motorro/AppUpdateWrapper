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
import android.os.Looper.getMainLooper
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.nhaarman.mockitokotlin2.mock
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.LooperMode
import kotlin.test.assertFailsWith

/**
 * Originally, updating a list of subscribers within event dispatching crashes AppUpdateManager dispatcher
 * with concurrent update exception.
 * TODO: Remove the duct tape as soon as original library becomes friendly to multiple subscribers
 */
@RunWith(AndroidJUnit4::class)
class SafeListenersKtTest: TestAppTest() {
    private lateinit var activity: Activity
    private lateinit var updateManager: FakeAppUpdateManager

    @Before
    fun init() {
        activity = mock()
        updateManager = FakeAppUpdateManager(application)
    }

    private inline fun buildTest(crossinline action: FakeAppUpdateManager.(InstallStateUpdatedListener) -> Unit) {
        updateManager.setUpdateAvailable(100500)
        updateManager.withInfo {
            startUpdateFlowForResult(it, AppUpdateType.FLEXIBLE, activity, 100)
            assertTrue(isConfirmationDialogVisible)
            userAcceptsUpdate()
            downloadStarts()

            (1..2).map {
                registerListener(object: InstallStateUpdatedListener {
                    override fun onStateUpdate(p0: InstallState?) {
                        action(this)
                    }
                })
            }

            downloadCompletes()
            shadowOf(getMainLooper()).idle()
        }
        shadowOf(getMainLooper()).idle()
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun registeringWithinEventDispatchWillNotCrash() {
        buildTest {
            registerListener { }
        }
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun safelyRegisteringWithinEventDispatchWillNotCrash() {
        buildTest {
            doRegisterListenerSafe(InstallStateUpdatedListener { })
        }
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun unregisteringWithinEventDispatchWillNotCrash() {
        buildTest {
            unregisterListener(it)
        }
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun safelyUnregisteringWithinEventDispatchWillNotCrash() {
        buildTest {
            doUnregisterListenerSafe(it)
        }
    }
}