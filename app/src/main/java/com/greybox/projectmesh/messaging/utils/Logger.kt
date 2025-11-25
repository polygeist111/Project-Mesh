package com.greybox.projectmesh.utils

import android.util.Log

/**
 * Centralized logging utility for the app.
 * Provides consistent logging with standardized tags and can be disabled in production.
 */

object Logger {
    internal const val TAG_PREFIX = "MeshChat_"
    private const val LOGGING_ENABLED = true

    internal fun buildTag(tag: String): String {
        return "$TAG_PREFIX$tag"
    }

    internal fun buildCriticalTag(tag: String): String {
        return "${TAG_PREFIX}${tag}_CRITICAL"
    }

    fun d(tag: String, message: String) {
        if (LOGGING_ENABLED) {
            Log.d(buildTag(tag), message)
        }
    }

    fun i(tag: String, message: String) {
        if (LOGGING_ENABLED) {
            Log.i(buildTag(tag), message)
        }
    }

    fun w(tag: String, message: String) {
        if (LOGGING_ENABLED) {
            Log.w(buildTag(tag), message)
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (LOGGING_ENABLED) {
            if (throwable != null) {
                Log.e(buildTag(tag), message, throwable)
            } else {
                Log.e(buildTag(tag), message)
            }
        }
    }

    // Log important events that should be visible even in production
    fun critical(tag: String, message: String, throwable: Throwable? = null) {
        val criticalTag = buildCriticalTag(tag)
        if (throwable != null) {
            Log.e(criticalTag, message, throwable)
        } else {
            Log.e(criticalTag, message)
        }
    }
}
