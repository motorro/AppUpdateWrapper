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
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.LooperMode
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class LogUtilsKtTest: TestAppTest() {
    private lateinit var activity: Activity
    private lateinit var updateManager: FakeAppUpdateManager

    @Before
    fun init() {
        activity = mock()
        updateManager = spy(FakeAppUpdateManager(application))
    }

    @Test
    fun createsLoggingTag() {
        class Test: Tagged {
            override fun getTagPrefix(): String = "Internal"
        }
        assertEquals("$LIBRARY_LOG_PREFIX:Internal:createsLoggingTag\$Test", Test().getTag())
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun formatsUpdateInfo() {
        updateManager.setUpdateAvailable(100500)
        updateManager.withInfo {
            assertEquals(
                """
                    Update info: 
                        - available version code: 100500
                        - update availability: UPDATE_AVAILABLE
                        - install status: UNKNOWN
                        - update types allowed: FLEXIBLE, IMMEDIATE
                """.trimIndent(),
                it.format()
            )
        }
        shadowOf(getMainLooper()).idle()
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun formatsUpdateTypesAllowedWhenNothingAllowed() {
        updateManager.setUpdateNotAvailable()
        updateManager.withInfo {
            assertEquals("NONE", it.formatUpdateTypesAllowed())
        }
        shadowOf(getMainLooper()).idle()
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun formatsUpdateTypesAllowedWhenFlexibleAllowed() {
        updateManager.setUpdateAvailable(100500)
        updateManager.partiallyAllowedUpdateType = AppUpdateType.FLEXIBLE
        updateManager.withInfo {
            assertEquals("FLEXIBLE", it.formatUpdateTypesAllowed())
        }
        shadowOf(getMainLooper()).idle()
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun formatsUpdateTypesAllowedWhenImmediateAllowed() {
        updateManager.setUpdateAvailable(100500)
        updateManager.partiallyAllowedUpdateType = AppUpdateType.IMMEDIATE
        updateManager.withInfo {
            assertEquals("IMMEDIATE", it.formatUpdateTypesAllowed())
        }
        shadowOf(getMainLooper()).idle()
    }
}