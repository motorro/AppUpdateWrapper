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
import com.google.android.play.core.appupdate.AppUpdateInfo
import java.util.concurrent.TimeUnit

/**
 * Checks if user has already refused to install update and terminates update flow
 */
interface UpdateFlowBreaker: TimeCancelledStorage {
    /**
     * Checks if enough time has passed since user had explicitly cancelled update
     */
    fun isEnoughTimePassedSinceLatestCancel(): Boolean

    /**
     * An extra point to check if update available is of any value to user
     * For example you may implement extra logic on update priority level (set from play market) or
     * staleness. Called _instead_ of [isEnoughTimePassedSinceLatestCancel] when processing
     * available update so you may want to combine both time of last offer and update value at the
     * same time.
     * @param updateInfo
     */
    fun isUpdateValuable(updateInfo: AppUpdateInfo): Boolean

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

        /**
         * Wraps flow breaker with an update value check function [valueCheck] which is called
         * when update info gets available. Combined with last update offer time you may decide
         * if to ask again or even cancel the flexible flow and switch to immediate
         * @receiver A flow breaker that checks the last time update UI was displayed
         * @param valueCheck A function that is supplied with time check result and update info
         */
        fun UpdateFlowBreaker.withUpdateValueCheck(valueCheck: (Boolean, AppUpdateInfo) -> Boolean): UpdateFlowBreaker =
            ValueCheckBreaker(this, valueCheck)
    }
}

/**
 * Always-on flow breaker
 */
internal object AlwaysOn: UpdateFlowBreaker {
    override fun isEnoughTimePassedSinceLatestCancel(): Boolean = true
    override fun isUpdateValuable(updateInfo: AppUpdateInfo): Boolean = true
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
): UpdateFlowBreaker, TimeCancelledStorage by storage, Tagged {
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
        return (currentTime - timeCancelled > intervalMillis).also {
            timber.d(
                "Last time cancelled: %d, Current time: %d, Enough time passed: %s",
                timeCancelled,
                currentTime,
                if(it) "yes" else "no"
            )
        }
    }

    /**
     * This breaker processes last time the prompt was displayed only so we just delegate check
     * to common method.
     * @param updateInfo
     */
    override fun isUpdateValuable(updateInfo: AppUpdateInfo): Boolean = isEnoughTimePassedSinceLatestCancel()
}

/**
 * Wraps around another [UpdateFlowBreaker] that checks for time intervals
 * delegating value check to [valueCheck] block
 * @param intervalBreaker Interval breaker who's [isEnoughTimePassedSinceLatestCancel] is called
 * @param valueCheck Update value check function
 */
internal class ValueCheckBreaker(
    private val intervalBreaker: UpdateFlowBreaker,
    private val valueCheck: (Boolean, AppUpdateInfo) -> Boolean
): UpdateFlowBreaker by intervalBreaker{
    /**
     * Gets time check from [intervalBreaker] and passes check result along with update info to
     * [valueCheck]
     * @param updateInfo
     */
    override fun isUpdateValuable(updateInfo: AppUpdateInfo): Boolean =
        valueCheck(isEnoughTimePassedSinceLatestCancel(), updateInfo)
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
 * @param clock Time provider
 */
internal class WithPreferences(
    private val storage: SharedPreferences,
    private val clock: Clock = Clock.SYSTEM
): TimeCancelledStorage, Tagged {
    /**
     * Gets the latest time user has explicitly cancelled update
     */
    override fun getTimeCanceled(): Long = storage.getLong(LATEST_CANCEL_PROPERTY, 0)

    /**
     * Saves current time as the latest one user has explicitly cancelled update
     */
    override fun saveTimeCanceled() {
        val currentTime = clock.getMillis()
        timber.d("Saving time cancelled: %d", currentTime)
        storage
            .edit()
            .putLong(LATEST_CANCEL_PROPERTY, currentTime)
            .apply()
    }
}
