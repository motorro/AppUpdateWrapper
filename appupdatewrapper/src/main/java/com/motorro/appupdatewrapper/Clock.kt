package com.motorro.appupdatewrapper

/**
 * Time provider
 */
interface Clock {
    /**
     * Current milliseconds value
     */
    fun getMillis(): Long

    /**
     * System clock
     */
    object SYSTEM: Clock {
        override fun getMillis(): Long = System.currentTimeMillis()
    }
}