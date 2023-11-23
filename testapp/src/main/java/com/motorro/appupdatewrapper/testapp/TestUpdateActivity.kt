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

package com.motorro.appupdatewrapper.testapp

import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import com.motorro.appupdatewrapper.AppUpdateView
import com.motorro.appupdatewrapper.AppUpdateWrapper

/**
 * Basic update activity
 */
class TestUpdateActivity : AppCompatActivity(), AppUpdateView {
    companion object {
        const val RESULT_SUCCESS = 10
        const val RESULT_FAILURE = 20
    }

    lateinit var updateWrapper: AppUpdateWrapper
    private lateinit var resultRegistry: ActivityResultRegistry

    // To pass 'activity result' as fake update manager does not start activities
    fun createTestRegistry(resultCode: Int) {
        resultRegistry = object : ActivityResultRegistry() {
            override fun <I, O> onLaunch(
                requestCode: Int,
                contract: ActivityResultContract<I, O>,
                input: I,
                options: ActivityOptionsCompat?
            ) {
                dispatchResult(requestCode, ActivityResult(resultCode, null))
            }
        }
    }

    // AppUpdateView implementation
    override val resultContractRegistry: ActivityResultRegistry get() = resultRegistry
    override fun updateReady() {
        updateWrapper.userConfirmedUpdate()
    }
    override fun updateComplete() {
        setResult(RESULT_SUCCESS)
        finish()
    }
    override fun updateFailed(e: Throwable) {
        setResult(RESULT_FAILURE)
        finish()
    }
}
