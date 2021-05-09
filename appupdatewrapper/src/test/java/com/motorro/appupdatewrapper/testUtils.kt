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

import android.os.Handler
import android.os.Looper.getMainLooper
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import com.google.android.play.core.tasks.OnFailureListener
import com.google.android.play.core.tasks.OnSuccessListener
import com.google.android.play.core.tasks.Task
import com.nhaarman.mockitokotlin2.spy
import org.robolectric.annotation.LooperMode
import java.util.*

/**
 * Detects Windows OS
 */
fun isRunningWindows() = true == System.getProperty("os.name")?.lowercase(Locale.US)?.contains("windows")

/**
 * A task that may [succeed] or [fail] on demand
 */
abstract class TestTask<T>: Task<T>() {
    private lateinit var onSuccessListener: OnSuccessListener<in T>
    private lateinit var onFailureListener: OnFailureListener

    override fun addOnSuccessListener(p0: OnSuccessListener<in T>): Task<T> {
        onSuccessListener = p0
        return this
    }

    override fun addOnFailureListener(p0: OnFailureListener): Task<T> {
        onFailureListener = p0
        return this
    }

    /**
     * Calls success listener with [result]
     */
    fun succeed(result: T?) {
        onSuccessListener.onSuccess(result)
    }

    /**
     * Calls failure listener with [error]
     */
    fun fail(error: Exception) {
        onFailureListener.onFailure(error)
    }
}

/**
 * A task to check update info
 */
abstract class TestUpdateInfoTask: TestTask<AppUpdateInfo>()

/**
 * A task to install update
 */
abstract class TestInstallTask: TestTask<Void>()

/**
 * Creates a test info task
 */
fun createTestInfoTask(): TestUpdateInfoTask = spy()

/**
 * Creates a test install task
 */
fun createTestInstallTask(): TestInstallTask = spy()

/**
 * Requests update info and executes [block] when returned
 * Requires `@LooperMode(LooperMode.Mode.PAUSED)`
 * @see LooperMode
 */
inline fun FakeAppUpdateManager.withInfo(crossinline block: FakeAppUpdateManager.(AppUpdateInfo) -> Unit) {
    appUpdateInfo
        .addOnSuccessListener {
            Handler(getMainLooper()).post { block(it) }
        }
        .addOnFailureListener { throw IllegalStateException("Unexpected update check error") }
}
