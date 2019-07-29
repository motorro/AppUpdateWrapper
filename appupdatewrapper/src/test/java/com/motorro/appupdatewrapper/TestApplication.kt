package com.motorro.appupdatewrapper

import android.app.Application
import org.robolectric.TestLifecycleApplication
import java.lang.reflect.Method

/**
 * Test application
 */
class TestApplication: Application(), TestLifecycleApplication {
    override fun beforeTest(method: Method) {

    }

    override fun prepareTest(test: Any) {

    }

    override fun afterTest(method: Method) {

    }
}