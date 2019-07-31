package com.motorro.appupdatewrapper

import android.app.Application
import android.os.Build
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import org.robolectric.annotation.Config

/**
 * Test class for robolectric with [TestApplication].
 */
@Config(
        application = TestApplication::class,
        sdk = [Build.VERSION_CODES.P]
)
open class TestAppTest{
    /**
     * Application instance
     */
    val application: Application
        get() = getApplicationContext<TestApplication>()
}