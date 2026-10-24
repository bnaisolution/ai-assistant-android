package org.dpdns.bnai.assistant.crash

import android.content.Context
import android.os.Build
import org.dpdns.bnai.assistant.data.CrashApiService
import org.dpdns.bnai.assistant.data.CrashReportRequest
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.PrintWriter
import java.io.StringWriter

class CrashHandler(private val context: Context) : Thread.UncaughtExceptionHandler {
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val stackTrace = sw.toString()

        saveCrashToPrefs(throwable.message ?: "Unknown", stackTrace)
        sendCrashToServer(throwable.message ?: "Unknown", stackTrace)

        defaultHandler?.uncaughtException(thread, throwable)
    }

    private fun saveCrashToPrefs(message: String, stackTrace: String) {
        val prefs = context.getSharedPreferences("crash_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("last_crash", message + "
" + stackTrace).apply()
    }

    private fun sendCrashToServer(message: String, stackTrace: String) {
        // ASSUMPTION: Replace with your actual backend base URL
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.yourdomain.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val api = retrofit.create(CrashApiService::class.java)

        val request = CrashReportRequest(
            error_type = "AndroidCrash",
            error_message = message,
            stack_trace = stackTrace,
            app_version = "1.0.0",
            device_info = Build.MODEL
        )

        try {
            api.sendCrashReport(request).enqueue(object : retrofit2.Callback<Void> {
                override fun onResponse(call: retrofit2.Call<Void>, response: retrofit2.Response<Void>) {}
                override fun onFailure(call: retrofit2.Call<Void>, t: Throwable) {}
            })
        } catch (e: Exception) {}
    }
}
