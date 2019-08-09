/*
 * Copyright 2019 Nikolai Kotchetkov (motorro).
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */

package com.motorro.appupdatewrapper

import android.content.SharedPreferences
import java.util.concurrent.TimeUnit

/**
 * Checks if user has already refused to install update and terminates update flow
 */
interface UpdateFlowBreaker: TimeCancelledStorage {
    /**
     * Checks if enough time has passed since user had explicitly cancelled update
     */
    fun isEnoughTimePassedSinceLatestCancel(): Boolean

    companion object {
        /**
         * Creates a dummy breaker that never interrupts callback
         */
        fun alwaysOn(): UpdateFlowBreaker = AlwaysOn

        /**
         * Creates a breaker that checks if given interval has passed since last time user had cancelled update
         * @param interval An interval between user cancels the update and the next time he is prompted
         * @param timeUnit Time-units the interval is measured in
         * @param storage Stores time cancelled
         */
        fun withInterval(interval: Long, timeUnit: TimeUnit, storage: TimeCancelledStorage): UpdateFlowBreaker =
            IntervalBreaker(interval, timeUnit, storage)

        /**
         * Creates a breaker to delay update for one day
         * @param storage Stores time cancelled
         */
        fun forOneDay(storage: TimeCancelledStorage): UpdateFlowBreaker =
            withInterval(1L, TimeUnit.DAYS, storage)

        /**
         * Creates a breaker to delay update for one day storing data in [SharedPreferences]
         * @param storage SharedPreferences instance
         */
        fun forOneDay(storage: SharedPreferences): UpdateFlowBreaker =
            withInterval(1L, TimeUnit.DAYS, TimeCancelledStorage.withPreferences(storage))
    }
}

/**
 * Always-on flow breaker
 */
internal object AlwaysOn: UpdateFlowBreaker {
    override fun isEnoughTimePassedSinceLatestCancel(): Boolean = true
    override fun getTimeCanceled(): Long = 0
    override fun saveTimeCanceled() = Unit
}

/**
 * Checks if given interval has passed since last time user had cancelled update
 * @param interval An interval between user cancels the update and the next time he is prompted
 * @param timeUnit Time-units the interval is measured in
 * @param storage Stores time cancelled
 * @param clock Time provider
 */
internal class IntervalBreaker(
    interval: Long,
    timeUnit: TimeUnit,
    private val storage: TimeCancelledStorage,
    private val clock: Clock = Clock.SYSTEM
): UpdateFlowBreaker, TimeCancelledStorage by storage {
    /**
     * [isEnoughTimePassedSinceLatestCancel] will return `true` after this interval since latest cancel
     */
    private val intervalMillis = timeUnit.toMillis(interval)

    /**
     * Checks if enough time has passed since user had explicitly cancelled update
     */
    override fun isEnoughTimePassedSinceLatestCancel(): Boolean {
        val timeCancelled = storage.getTimeCanceled()
        val currentTime = clock.getMillis()
        return currentTime - timeCancelled > intervalMillis
    }
}

/**
 * Stores time the update was cancelled
 */
interface TimeCancelledStorage {
    /**
     * Gets the latest time user has explicitly cancelled update (in milliseconds)
     */
    fun getTimeCanceled(): Long

    /**
     * Saves current time as the latest one user has explicitly cancelled update
     */
    fun saveTimeCanceled()

    companion object {
        /**
         * Creates [SharedPreferences] storage
         * @param storage SharedPreferences instance
         */
        fun withPreferences(storage: SharedPreferences): TimeCancelledStorage = WithPreferences(storage)
    }
}

/**
 * Stores time cancelled in shared preferences
 * @param storage SharedPreferences instance
 */
internal class WithPreferences(private val storage: SharedPreferences, private val clock: Clock = Clock.SYSTEM): TimeCancelledStorage {
    /**
     * Gets the latest time user has explicitly cancelled update
     */
    override fun getTimeCanceled(): Long = storage.getLong(LATEST_CANCEL_PROPERTY, 0)

    /**
     * Saves current time as the latest one user has explicitly cancelled update
     */
    override fun saveTimeCanceled() {
        storage
            .edit()
            .putLong(LATEST_CANCEL_PROPERTY, clock.getMillis())
            .apply()
    }
}
