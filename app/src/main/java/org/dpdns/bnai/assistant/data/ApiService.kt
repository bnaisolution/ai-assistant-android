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
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface ApiService {
    @GET("health")
    suspend fun checkHealth(): Response<JsonObject>

    @GET("api/v1/capabilities")
    suspend fun getCapabilities(): Response<JsonObject>

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @GET("api/v1/auth/me")
    suspend fun getMe(): Response<JsonObject>

    @POST("api/v1/ai/chat")
    suspend fun fastChat(@Body request: FastChatRequest): Response<FastChatResponse>

    @POST("api/v1/ai/execute")
    suspend fun executeWorkflow(@Body request: GatewayExecuteRequest): Response<GatewayExecuteResponse>

    @GET("api/v1/skills/list")
    suspend fun listSkills(): Response<JsonObject>

    @POST("api/v1/skills/analyze-blueprint")
    suspend fun analyzeBlueprint(@Body body: JsonObject): Response<JsonObject>

    @POST("api/v1/skills/repair-loop/record")
    suspend fun recordRepairLoop(@Body body: JsonObject): Response<JsonObject>

    @GET("api/v1/copilot/dashboard")
    suspend fun getCopilotDashboard(): Response<JsonObject>

    @POST("api/v1/copilot/diagnose")
    suspend fun diagnoseError(@Body request: CopilotDiagnoseRequest): Response<JsonObject>

    @POST("api/v1/copilot/screenshot-analyze")
    suspend fun analyzeScreenshot(@Body body: JsonObject): Response<JsonObject>

    @GET("api/v1/copilot/workflows")
    suspend fun listNamespacedWorkflows(@Query("namespace") namespace: String? = null): Response<JsonObject>

    @POST("api/v1/copilot/diff-viewer")
    suspend fun computeWorkflowDiff(@Body request: DiffViewerRequest): Response<JsonObject>

    @GET("api/v1/copilot/history")
    suspend fun getCopilotHistory(): Response<JsonObject>

    @POST("api/v1/trusted-controller/sandbox/preflight")
    suspend fun sandboxPreflight(@Body body: JsonObject): Response<JsonObject>

    @POST("api/v1/trusted-controller/sandbox/execute")
    suspend fun sandboxExecute(@Body request: SandboxExecuteRequest): Response<JsonObject>

    @POST("api/v1/trusted-controller/protected-changes/execute")
    suspend fun executeProtectedChange(@Body request: ProtectedChangeExecuteRequest): Response<JsonObject>

    @GET("api/v1/trusted-controller/recovery/points")
    suspend fun getRecoveryPoints(): Response<JsonObject>

    @POST("api/v1/trusted-controller/recovery/rollback")
    suspend fun executeRollback(@Body body: JsonObject): Response<JsonObject>

    @GET("api/v1/approvals/list")
    suspend fun listApprovals(): Response<JsonObject>

    @POST("api/v1/approvals/request")
    suspend fun requestApproval(@Body body: JsonObject): Response<JsonObject>

    @POST("api/v1/approvals/decide")
    suspend fun decideApproval(@Body request: ApprovalDecisionRequest): Response<JsonObject>

    @GET("api/v1/cinematic/studio")
    suspend fun getCinematicStudio(): Response<JsonObject>

    @POST("api/v1/cinematic/stage-action")
    suspend fun triggerCinematicStage(@Body request: CinematicStageActionRequest): Response<JsonObject>

    @POST("api/v1/cinematic/copilot-context")
    suspend fun getCinematicCopilotContext(@Body body: JsonObject): Response<JsonObject>

    @GET("api/v1/memory/retention-policies")
    suspend fun getRetentionPolicies(): Response<JsonObject>

    @POST("api/v1/memory/read")
    suspend fun readMemory(@Body body: JsonObject): Response<JsonObject>

    @POST("api/v1/memory/write")
    suspend fun writeMemory(@Body body: JsonObject): Response<JsonObject>

    @POST("api/v1/memory/delete")
    suspend fun deleteMemory(@Body body: JsonObject): Response<JsonObject>

    @GET("api/v1/tasks/list")
    suspend fun listTasks(): Response<JsonObject>

    @POST("api/v1/tasks/cancel")
    suspend fun cancelTask(@Body body: JsonObject): Response<JsonObject>

    @POST("api/v1/tasks/reconcile")
    suspend fun reconcileTask(@Body body: JsonObject): Response<JsonObject>

    @GET("api/v1/notifications/list")
    suspend fun listNotifications(): Response<JsonObject>

    @POST("api/v1/notifications/dispatch")
    suspend fun dispatchNotification(@Body body: JsonObject): Response<JsonObject>

    @POST("api/v1/files/upload-metadata")
    suspend fun uploadFileMetadata(@Body body: JsonObject): Response<JsonObject>

    @POST("api/v1/files/export")
    suspend fun exportProjectData(@Body body: JsonObject): Response<JsonObject>

    @POST("api/v1/files/delete")
    suspend fun deleteFileArtifact(@Body body: JsonObject): Response<JsonObject>

    @GET("api/v1/n8n/registry")
    suspend fun getWorkflowRegistry(): Response<JsonObject>

    @GET("api/v1/n8n/contracts")
    suspend fun getContractRegistry(): Response<JsonObject>

    @GET("api/v1/owner/dashboard")
    suspend fun getOwnerDashboard(): Response<JsonObject>

    @GET("api/v1/owner/schema-registry")
    suspend fun getSchemaRegistry(): Response<JsonObject>

    @POST("api/v1/owner/action")
    suspend fun executeOwnerAction(@Body payload: JsonObject): Response<JsonObject>

    @GET("api/v1/user/dashboard")
    suspend fun getUserDashboard(): Response<JsonObject>

    companion object {
        fun create(tokenManager: TokenManager): ApiService {
            val authInterceptor = Interceptor { chain ->
                val builder = chain.request().newBuilder()
                    .addHeader("Accept", "application/json")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("X-Client-Version", BuildConfig.VERSION_NAME)
                    .addHeader("X-Session-Mode", tokenManager.getSessionMode())
                    .addHeader("X-Project-Id", tokenManager.getActiveProjectId())

                tokenManager.getToken()?.let { token ->
                    if (token.isNotBlank()) {
                        builder.addHeader("Authorization", "Bearer $token")
                    }
                }
                chain.proceed(builder.build())
            }

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(190, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(BuildConfig.BASE_API_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }
    }
}
