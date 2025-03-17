package com.greybox.projectmesh.utils

import android.util.Log

/**
 * Centralized logging utility for the app.
 * Provides consistent logging with standardized tags and can be disabled in production.
 */

object Logger {
    private const val LOGGING_ENABLED = true
    private const val TAG_PREFIX = "MeshChat_"

    fun d(tag: String, message: String) {
        if (LOGGING_ENABLED) {
            Log.d("$TAG_PREFIX$tag", message)
        }
    }

    fun i(tag: String, message: String) {
        if (LOGGING_ENABLED) {
            Log.i("$TAG_PREFIX$tag", message)
        }
    }

    fun w(tag: String, message: String) {
        if (LOGGING_ENABLED) {
            Log.w("$TAG_PREFIX$tag", message)
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (LOGGING_ENABLED) {
            if (throwable != null) {
                Log.e("$TAG_PREFIX$tag", message, throwable)
            } else {
                Log.e("$TAG_PREFIX$tag", message)
            }
        }
    }

    // Log important events that should be visible even in production
    fun critical(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e("$TAG_PREFIX${tag}_CRITICAL", message, throwable)
        } else {
            Log.e("$TAG_PREFIX${tag}_CRITICAL", message)
        }
    }
}