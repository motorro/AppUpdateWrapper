package com.motorro.appupdatewrapper.sample

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
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
        // Starts flexible update flow that (if cancelled) will ask again tomorrow
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
     * Reports update is downloaded and ready to be installed
     * When ready to proceed call [AppUpdateState.userConfirmedUpdate]
     * Called in flexible flow
     * @see AppUpdateState.userConfirmedUpdate
     * @see AppUpdateState.userCanceledUpdate
     */
    override fun updateReady() {
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
        Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show()
    }
}