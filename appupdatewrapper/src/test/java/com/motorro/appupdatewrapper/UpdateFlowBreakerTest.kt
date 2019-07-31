package com.motorro.appupdatewrapper

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class UpdateFlowBreakerTest: TestAppTest() {
    companion object {
        private const val INTERVAL = 1L
        private val UNITS = TimeUnit.DAYS
    }

    private lateinit var clock: Clock
    private lateinit var storage: TimeCancelledStorage
    private lateinit var intervalBreaker: IntervalBreaker

    @Before
    fun init() {
        clock = mock()
        storage = mock {
            on { getTimeCanceled() } doReturn 0
        }
        intervalBreaker = IntervalBreaker(INTERVAL, UNITS, storage, clock)
    }

    @Test
    fun permitsUiInteractionIfEnoughTimePassed() {
        whenever(clock.getMillis()).thenReturn(UNITS.toMillis(INTERVAL) + 1)
        assertTrue(intervalBreaker.isEnoughTimePassedSinceLatestCancel())
    }

    @Test
    fun prohibitsUiInteractionIfNotEnoughTimePassed() {
        whenever(clock.getMillis()).thenReturn(UNITS.toMillis(INTERVAL))
        assertFalse(intervalBreaker.isEnoughTimePassedSinceLatestCancel())
    }
}