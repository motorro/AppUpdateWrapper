package com.motorro.appupdatewrapper.sample

import android.app.Activity
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.motorro.appupdatewrapper.AppUpdateView
import com.motorro.appupdatewrapper.AppUpdateWrapper
import com.motorro.appupdatewrapper.UpdateFlowBreaker
import com.motorro.appupdatewrapper.sample.databinding.ActivityMainBinding
import com.motorro.appupdatewrapper.startFlexibleUpdate

class MainActivity : AppCompatActivity(), AppUpdateView {
    /**
     * View
     */
    private lateinit var binding: ActivityMainBinding

    /**
     * Update flow
     */
    private lateinit var updateWrapper: AppUpdateWrapper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Starts flexible update flow
        updateWrapper = startFlexibleUpdate(
            AppUpdateManagerFactory.create(this.applicationContext),
            this,
            UpdateFlowBreaker.alwaysOn()
        )
    }

    /**
     * Returns hosting activity for update process
     * Call [AppUpdateState.checkActivityResult] in [Activity.onActivityResult] to
     * check update status
     * @see AppUpdateState.checkActivityResult
     */
    override val activity: Activity = this

    /**
     * Called when update is checking or downloading data
     * Subclasses may display some spinner at this point.
     * Remove spinner in:
     * - [updateReady]
     * - [updateComplete]
     * - [updateFailed]
     */
    override fun updateChecking() {
        log("updateChecking")
    }

    /**
     * Called when user accepts update download in play-core dialog and download starts.
     * Called in flexible flow
     */
    override fun updateDownloadStarts() {
        log("updateDownloadStarts")
    }

    /**
     * Called when installation activity launches
     * Calling this handler means the application will be restarted if update succeeds
     * Subclass may want to finish it's current activity at this point
     */
    override fun updateInstallUiVisible() {
        log("updateInstallUiVisible")
    }

    /**
     * No update available or update flow completed
     * Called in flexible flow
     */
    override fun updateComplete() {
        log("updateComplete")
    }

    /**
     * Reports update is downloaded and ready to be installed
     * When ready to proceed call [AppUpdateState.userConfirmedUpdate]
     * Called in flexible flow
     * @see AppUpdateState.userConfirmedUpdate
     * @see AppUpdateState.userCanceledUpdate
     */
    override fun updateReady() {
        log("Update ready...")
        Snackbar
            .make(binding.root,"An update has just been downloaded.", Snackbar.LENGTH_INDEFINITE)
            .setAction("Update") { updateWrapper.userConfirmedUpdate() }
            .setAction("Cancel") { updateWrapper.userCanceledUpdate() }
            .show()
    }

    /**
     * Critical update error occurred e.g. when immediate update was requested but failed to proceed
     */
    override fun updateFailed(e: Throwable) {
        log(e, "updateFailed")
    }

    /**
     * Notify user of some non-critical update error e.g. flexible update has failed but it is not critical for
     * general application flow.
     * Called in flexible flow
     */
    override fun nonCriticalUpdateError(e: Throwable) {
        log(e, "nonCriticalUpdateError")
    }

    /**
     * Debug output
     */
    private fun log(e: Throwable, message: String) {
        log("$message: $e")
    }

    /**
     * Debug output
     */
    private fun log(message: String) {
        binding.textView.append("\n--> $message")
    }
}