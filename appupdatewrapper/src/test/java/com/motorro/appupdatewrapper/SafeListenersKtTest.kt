package com.motorro.appupdatewrapper

import android.app.Activity
import android.os.Looper
import android.os.Looper.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.nhaarman.mockitokotlin2.mock
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.*
import org.robolectric.annotation.LooperMode
import kotlin.test.assertFailsWith

/**
 * Originally, updating a list of subscribers within event dispatching crashes AppUpdateManager dispatcher
 * with concurrent update exception.
 * TODO: Remove the duct tape as soon as original library becomes friendly to multiple subscribers
 */
@RunWith(AndroidJUnit4::class)
class SafeListenersKtTest: TestAppTest() {
    private lateinit var activity: Activity
    private lateinit var updateManager: FakeAppUpdateManager

    @Before
    fun init() {
        activity = mock()
        updateManager = FakeAppUpdateManager(application)
    }

    private inline fun buildTest(crossinline action: FakeAppUpdateManager.(InstallStateUpdatedListener) -> Unit) {
        updateManager.setUpdateAvailable(100500)
        updateManager.withInfo {
            startUpdateFlowForResult(it, AppUpdateType.FLEXIBLE, activity, 100)
            assertTrue(isConfirmationDialogVisible)
            userAcceptsUpdate()
            downloadStarts()

            (1..2).map {
                registerListener(object: InstallStateUpdatedListener {
                    override fun onStateUpdate(p0: InstallState?) {
                        action(this)
                    }
                })
            }

            downloadCompletes()
            shadowOf(getMainLooper()).idle()
        }
        shadowOf(getMainLooper()).idle()
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun registeringWithinEventDispatchWillCrash() {
        assertFailsWith<ConcurrentModificationException> {
            buildTest {
                registerListener { }
            }
        }
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun safelyRegisteringWithinEventDispatchWillNotCrash() {
        buildTest {
            doRegisterListenerSafe(InstallStateUpdatedListener { })
        }
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun unregisteringWithinEventDispatchWillCrash() {
        assertFailsWith<ConcurrentModificationException> {
            buildTest {
                unregisterListener(it)
            }
        }
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun safelyUnregisteringWithinEventDispatchWillNotCrash() {
        buildTest {
            doUnregisterListenerSafe(it)
        }
    }
}