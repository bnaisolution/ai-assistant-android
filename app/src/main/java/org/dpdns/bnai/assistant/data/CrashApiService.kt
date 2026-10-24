package org.dpdns.bnai.assistant.data

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

data class CrashReportRequest(
    val error_type: String,
    val error_message: String,
    val stack_trace: String,
    val app_version: String,
    val device_info: String
)

interface CrashApiService {
    @POST("api/system/crash-report")
    fun sendCrashReport(@Body request: CrashReportRequest): Call<Void>
}
