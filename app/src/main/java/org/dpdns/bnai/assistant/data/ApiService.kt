package org.dpdns.bnai.assistant.data

import com.google.gson.JsonObject
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.dpdns.bnai.assistant.BuildConfig
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface ApiService {
    @POST("api/v1/auth/login") suspend fun login(@Body request: LoginRequest): Response<AuthResponse>
    @POST("api/v1/auth/signup") suspend fun signup(@Body request: SignupRequest): Response<JsonObject>
    @POST("api/v1/auth/verify-otp") suspend fun verifyOtp(@Body request: VerifyOtpRequest): Response<JsonObject>
    @POST("api/v1/auth/logout") suspend fun logout(): Response<JsonObject>
    
    @GET("api/v1/skills") suspend fun getSkills(): Response<SkillsResponse>
    @GET("api/v1/config/ai-models") suspend fun getAiModels(): Response<ModelsResponse>
    @POST("api/v1/config/ai-models/update") suspend fun updateAiModel(@Body request: JsonObject): Response<JsonObject>
    @GET("api/v1/config/prompts") suspend fun getPrompts(): Response<PromptsResponse>
    @POST("api/v1/config/prompts/update") suspend fun updatePrompt(@Body request: JsonObject): Response<JsonObject>
    @POST("api/v1/user/byoi") suspend fun saveByoi(@Body request: JsonObject): Response<JsonObject>

    @POST("api/v1/ai/chat") suspend fun fastChat(@Body request: FastChatRequest): Response<FastChatResponse>
    @POST("api/v1/ai/execute") suspend fun executeWorkflow(@Body request: GatewayExecuteRequest): Response<GatewayExecuteResponse>
    @GET("health") suspend fun checkHealth(): Response<JsonObject>
    
    @POST("api/v1/owner/action") suspend fun executeOwnerAction(@Body payload: JsonObject): Response<JsonObject>
    @POST("api/v1/memory/write") suspend fun saveMemory(@Body request: MemoryWriteRequest): Response<JsonObject>
    @POST("api/v1/n8n/test-workflow") suspend fun testWorkflow(@Body request: JsonObject): Response<JsonObject>
    @GET("api/v1/tasks/list") suspend fun getTasks(): Response<JsonObject>
    @POST("api/v1/memory/read") suspend fun readMemory(@Body payload: JsonObject): Response<JsonObject>
    @GET("api/v1/owner/dashboard") suspend fun getOwnerDashboard(): Response<JsonObject>
    @POST("api/v1/n8n/deploy-workflow") suspend fun deployWorkflow(@Body request: JsonObject): Response<JsonObject>

    companion object {
        fun create(tokenManager: TokenManager): ApiService {
            val authInterceptor = Interceptor { chain ->
                val builder = chain.request().newBuilder().addHeader("Accept", "application/json").addHeader("Content-Type", "application/json").addHeader("X-Client-Version", BuildConfig.VERSION_NAME)
                tokenManager.getToken()?.let { token -> if (token.isNotBlank()) builder.addHeader("Authorization", "Bearer $token") }
                chain.proceed(builder.build())
            }
            val client = OkHttpClient.Builder().addInterceptor(authInterceptor).addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }).connectTimeout(30, TimeUnit.SECONDS).readTimeout(190, TimeUnit.SECONDS).build()
            return Retrofit.Builder().baseUrl(BuildConfig.BASE_API_URL).client(client).addConverterFactory(GsonConverterFactory.create()).build().create(ApiService::class.java)
        }
    }
}
