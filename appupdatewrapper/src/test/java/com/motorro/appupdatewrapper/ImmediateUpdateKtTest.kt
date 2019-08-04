package com.motorro.appupdatewrapper

import androidx.test.core.app.ActivityScenario.launch
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import com.google.android.play.core.install.model.AppUpdateType
import com.motorro.appupdatewrapper.testapp.ImmediateUpdateActivity
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class ImmediateUpdateKtTest: TestAppTest() {
    @Test
    fun startsImmediateUpdateIfAvailable() {
        lateinit var updateManager: FakeAppUpdateManager
        val scenario = launch(ImmediateUpdateActivity::class.java)
        scenario.onActivity {
            updateManager = FakeAppUpdateManager(it).apply {
                setUpdateAvailable(100500)
                partiallyAllowedUpdateType = AppUpdateType.IMMEDIATE
            }
            it.startImmediateUpdate(updateManager, it)
        }

        assertTrue(updateManager.isImmediateFlowVisible)
    }

    @Test
    fun failsIfUpdateIsNotAvailable() {
        lateinit var updateManager: FakeAppUpdateManager
        val scenario = launch(ImmediateUpdateActivity::class.java)
        scenario.onActivity {
            updateManager = FakeAppUpdateManager(it).apply {
                setUpdateNotAvailable()
            }
            it.startImmediateUpdate(updateManager, it)
        }

        assertEquals(ImmediateUpdateActivity.RESULT_FAILURE, scenario.result.resultCode)
    }
}