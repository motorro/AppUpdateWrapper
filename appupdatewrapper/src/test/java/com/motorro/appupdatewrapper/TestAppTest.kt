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

import android.app.Application
import android.os.Build
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import org.junit.BeforeClass
import org.robolectric.annotation.Config
import timber.log.Timber

/**
 * Test class for robolectric with [TestApplication].
 */
@Config(
        application = TestApplication::class,
        sdk = [Build.VERSION_CODES.P]
)
open class TestAppTest{
    companion object {
        @BeforeClass
        @JvmStatic
        fun installTimber() {
            // Just to test we don't crash in logging
            if (0 == Timber.treeCount) {
                Timber.plant(Timber.DebugTree())
            }
        }
    }

    /**
     * Application instance
     */
    val application: Application
        get() = getApplicationContext<TestApplication>()
}