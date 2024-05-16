package com.greybox.projectmesh.debug

import android.R.layout
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.FrameLayout
import android.widget.PopupWindow
import com.google.gson.Gson
import org.xml.sax.helpers.DefaultHandler
import java.lang.Exception
import java.lang.Thread.UncaughtExceptionHandler
import kotlin.system.exitProcess


class CrashHandler(private val context: Context, private val defaultHandler: UncaughtExceptionHandler, private val activityToBeLaunched: Class<*>) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            launchActivity(context,activityToBeLaunched,throwable)
            exitProcess(status = 1)
        } catch (e: Exception)
        {
            defaultHandler.uncaughtException(thread,throwable)
        }
    }

    private fun launchActivity(applicationContext: Context, activity: Class<*>, exception: Throwable)
    {
        val crashIntent = Intent(applicationContext, activity).also {
            it.putExtra("CrashData", Gson().toJson(exception))
            Log.e("Project Mesh Error","Error: ",exception);
        }

        crashIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        crashIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        applicationContext.startActivity(crashIntent)
    }

    companion object {
        fun init(applicationContext: Context, activityToBeLaunched: Class<*>)
        {
            val handler = CrashHandler(applicationContext,Thread.getDefaultUncaughtExceptionHandler() as UncaughtExceptionHandler, activityToBeLaunched)
            Thread.setDefaultUncaughtExceptionHandler(handler)
        }

        fun getThrowableFromIntent(intent: Intent): Throwable?
        {
            return try {
                Gson().fromJson(intent.getStringExtra("CrashData"), Throwable::class.java)
            }
            catch (e: Exception) {
                Log.e("CrashHandler","getThrowableFromIntent: ",e);
                null
            }

        }
    }
}
