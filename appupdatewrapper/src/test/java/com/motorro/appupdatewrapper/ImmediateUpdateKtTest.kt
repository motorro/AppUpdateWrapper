package com.motorro.appupdatewrapper

import android.app.Activity
import androidx.test.core.app.ActivityScenario.launch
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import com.google.android.play.core.install.model.AppUpdateType
import com.motorro.appupdatewrapper.testapp.TestUpdateActivity
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class ImmediateUpdateKtTest: TestAppTest() {
    @Test
    fun startsImmediateUpdateIfAvailable() {
        lateinit var updateManager: FakeAppUpdateManager
        val scenario = launch(TestUpdateActivity::class.java)
        scenario.onActivity {
            updateManager = FakeAppUpdateManager(it).apply {
                setUpdateAvailable(100500)
                partiallyAllowedUpdateType = AppUpdateType.IMMEDIATE
            }
            it.updateWrapper = it.startImmediateUpdate(updateManager, it)

            assertTrue(updateManager.isImmediateFlowVisible)
            // Emulate update success
            it.passActivityResult(REQUEST_CODE_UPDATE, Activity.RESULT_OK)

            assertEquals(TestUpdateActivity.RESULT_SUCCESS, scenario.result.resultCode)
        }
    }

    @Test
    fun failsIfUpdateIsNotAvailable() {
        lateinit var updateManager: FakeAppUpdateManager
        val scenario = launch(TestUpdateActivity::class.java)
        scenario.onActivity {
            updateManager = FakeAppUpdateManager(it).apply {
                setUpdateNotAvailable()
            }
            it.startImmediateUpdate(updateManager, it)
        }

        assertEquals(TestUpdateActivity.RESULT_FAILURE, scenario.result.resultCode)
    }
}