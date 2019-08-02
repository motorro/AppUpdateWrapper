package com.motorro.appupdatewrapper.testapp

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import com.motorro.appupdatewrapper.AppUpdateView

/**
 * Basic immediate update activity with no UI interaction
 */
class ImmediateUpdateActivity : AppCompatActivity(), AppUpdateView {
    companion object {
        const val RESULT_SUCCESS = 10
        const val RESULT_FAILURE = 20
    }

    // AppUpdateView implementation
    override val activity: Activity get() = this
    override fun updateReady() = Unit
    override fun updateComplete() {
        setResult(RESULT_SUCCESS)
        finish()
    }
    override fun updateFailed(e: Throwable) {
        setResult(RESULT_FAILURE)
        finish()
    }
}
