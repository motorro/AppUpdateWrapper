package com.motorro.appupdatewrapper.testapp

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
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

    // To pass 'activity result' as fake update manager does not start activities
    fun passActivityResult(requestCode: Int, resultCode: Int) {
        onActivityResult(requestCode, resultCode, null)
    }

    // Passes an activity result to wrapper to check for play-core interaction
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (updateWrapper.checkActivityResult(requestCode, resultCode)) {
            // Result handled and processed
            return
        }
        // Process your request codes
    }

    // AppUpdateView implementation
    override val activity: Activity get() = this
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
