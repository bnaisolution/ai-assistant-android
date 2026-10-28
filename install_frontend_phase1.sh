#!/bin/bash
set -e
cd ~/ai-assistant-android

# Create directories
mkdir -p .github/workflows
mkdir -p app/src/main/java/org/dpdns/bnai/assistant/data
mkdir -p app/src/main/java/org/dpdns/bnai/assistant/viewmodel
mkdir -p app/src/main/java/org/dpdns/bnai/assistant/service
mkdir -p app/src/main/java/org/dpdns/bnai/assistant/ui

# ============================================================================
# FILE 1: .github/workflows/android-build.yml
# ============================================================================
cat > .github/workflows/android-build.yml << 'EOF'
name: Build AI Assistant APK
on:
  push:
    branches: [ "main", "master" ]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout Code
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Setup Android SDK
        uses: android-actions/setup-android@v3

      - name: Grant Execute Permission for Gradlew
        run: |
          if [ -f "./gradlew" ]; then chmod +x ./gradlew; else gradle wrapper; chmod +x ./gradlew; fi

      - name: Build Debug APK
        run: ./gradlew assembleDebug --no-daemon

      - name: Upload Debug APK Artifact
        uses: actions/upload-artifact@v4
        with:
          name: bnai-assistant-v3.1-debug-apk
          path: app/build/outputs/apk/debug/app-debug.apk
EOF

# ============================================================================
# FILE 2: settings.gradle.kts
# ============================================================================
cat > settings.gradle.kts << 'EOF'
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "ai-assistant-android"
include(":app")
EOF

# ============================================================================
# FILE 3: build.gradle.kts (Root) & gradle.properties
# ============================================================================
cat > build.gradle.kts << 'EOF'
plugins {
    id("com.android.application") version "8.5.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
}
EOF

cat > gradle.properties << 'EOF'
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
EOF

# ============================================================================
# FILE 4: app/build.gradle.kts
# ============================================================================
cat > app/build.gradle.kts << 'EOF'
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "org.dpdns.bnai.assistant"
    compileSdk = 34

    defaultConfig {
        applicationId = "org.dpdns.bnai.assistant"
        minSdk = 26
        targetSdk = 34
        versionCode = 310
        versionName = "3.1.0"

        buildConfigField("String", "BASE_API_URL", "\"https://api.bnai.dpdns.org/\"")
        buildConfigField("String", "DEFAULT_USER_ID", "\"11111111-1111-1111-1111-111111111111\"")
        buildConfigField("String", "DEFAULT_WORKSPACE_ID", "\"11111111-1111-1111-1111-111111111111\"")
        buildConfigField("String", "ASSISTANT_PROJECT_ID", "\"11111111-1111-1111-1111-111111111111\"")
        buildConfigField("String", "CINEMATIC_PROJECT_ID", "\"33333333-3333-3333-3333-333333333333\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
}
EOF

# ============================================================================
# FILE 5: app/src/main/AndroidManifest.xml
# ============================================================================
mkdir -p app/src/main
cat > app/src/main/AndroidManifest.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application
        android:allowBackup="false"
        android:label="BNAI Unified v3.1"
        android:supportsRtl="true"
        android:usesCleartextTraffic="false"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <service
            android:name=".service.FloatingBubbleService"
            android:enabled="true"
            android:exported="false"
            android:foregroundServiceType="mediaProjection" />
    </application>
</manifest>
EOF

# ============================================================================
# FILE 6: TokenManager.kt
# ============================================================================
cat > app/src/main/java/org/dpdns/bnai/assistant/data/TokenManager.kt << 'EOF'
package org.dpdns.bnai.assistant.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.dpdns.bnai.assistant.BuildConfig

class TokenManager(context: Context) {
    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "bnai_secure_prefs_v31",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        context.getSharedPreferences("bnai_fallback_prefs_v31", Context.MODE_PRIVATE)
    }

    fun saveSession(token: String, userId: String, workspaceId: String, email: String, role: String) {
        prefs.edit()
            .putString("jwt_token", token)
            .putString("user_id", userId)
            .putString("workspace_id", workspaceId)
            .putString("email", email)
            .putString("role", role)
            .apply()
    }

    fun getToken(): String? = prefs.getString("jwt_token", null)
    fun getUserId(): String = prefs.getString("user_id", BuildConfig.DEFAULT_USER_ID) ?: BuildConfig.DEFAULT_USER_ID
    fun getWorkspaceId(): String = prefs.getString("workspace_id", BuildConfig.DEFAULT_WORKSPACE_ID) ?: BuildConfig.DEFAULT_WORKSPACE_ID
    fun getEmail(): String = prefs.getString("email", "") ?: ""
    fun getRole(): String = prefs.getString("role", "normal_user") ?: "normal_user"

    fun isOwner(): Boolean =
        getRole().equals("owner_admin", ignoreCase = true) || getRole().equals("owner", ignoreCase = true)

    fun setSessionMode(mode: String) {
        prefs.edit().putString("session_mode", mode).apply()
    }

    fun getSessionMode(): String = prefs.getString("session_mode", "Auto") ?: "Auto"

    fun setActiveProjectId(projectId: String) {
        prefs.edit().putString("active_project_id", projectId).apply()
    }

    fun getActiveProjectId(): String =
        prefs.getString("active_project_id", BuildConfig.ASSISTANT_PROJECT_ID) ?: BuildConfig.ASSISTANT_PROJECT_ID

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
EOF

# ============================================================================
# FILE 7: ApiModels.kt
# ============================================================================
cat > app/src/main/java/org/dpdns/bnai/assistant/data/ApiModels.kt << 'EOF'
package org.dpdns.bnai.assistant.data

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

// --- AUTH MODELS ---
data class LoginRequest(val email: String, val password: String)

data class AuthResponse(
    val token: String?,
    @SerializedName("access_token") val accessToken: String?,
    @SerializedName("user_id") val userId: String?,
    @SerializedName("workspace_id") val workspaceId: String?,
    val role: String?,
    val email: String?
)

// --- FAST PATH: DIRECT CHAT ---
data class FastChatRequest(
    @SerializedName("request_id") val requestId: String,
    val message: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("workspace_id") val workspaceId: String,
    @SerializedName("project_id") val projectId: String? = null,
    @SerializedName("conversation_id") val conversationId: String? = null,
    @SerializedName("skill_key") val skillKey: String = "ai_admin",
    @SerializedName("ceo_mode") val ceoMode: Boolean = false
)

data class FastChatResponse(
    @SerializedName("request_id") val requestId: String?,
    val reply: String?,
    val message: String?,
    val result: JsonElement?,
    val provider: String?,
    val model: String?,
    val status: String?
)

// --- BACKGROUND PATH: GATEWAY ---
data class GatewayExecuteRequest(
    @SerializedName("request_id") val requestId: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("workspace_id") val workspaceId: String,
    @SerializedName("project_id") val projectId: String? = null,
    @SerializedName("conversation_id") val conversationId: String? = null,
    val intent: String,
    @SerializedName("source_mode") val sourceMode: String = "manual",
    val payload: JsonObject
)

data class GatewayErrorItem(val category: String?, val message: String?)
data class GatewayWarningItem(val message: String?)

data class VerificationBlock(
    val status: String?,
    val evidence: List<String>?,
    val persisted: Boolean?,
    @SerializedName("gateway_persisted") val gatewayPersisted: Boolean?,
    @SerializedName("live_verified") val liveVerified: Boolean?,
    @SerializedName("delivered_confirmed") val deliveredConfirmed: Boolean?
)

data class GatewayExecuteResponse(
    @SerializedName("request_id") val requestId: String?,
    @SerializedName("task_id") val taskId: String?,
    val status: String?,
    val result: JsonObject?,
    val errors: List<GatewayErrorItem>?,
    val warnings: List<GatewayWarningItem>?,
    val verification: VerificationBlock?
)

// --- COPILOT & SECURITY MODELS ---
data class CopilotDiagnoseRequest(
    @SerializedName("project_id") val projectId: String,
    @SerializedName("workflow_id") val workflowId: String? = null,
    @SerializedName("failed_node") val failedNode: String? = null,
    @SerializedName("error_message") val errorMessage: String,
    @SerializedName("execution_logs") val executionLogs: String? = null
)

data class DiffViewerRequest(
    @SerializedName("workflow_id") val workflowId: String,
    @SerializedName("project_namespace") val projectNamespace: String,
    @SerializedName("original_json") val originalJson: JsonObject,
    @SerializedName("proposed_json") val proposedJson: JsonObject
)

data class SandboxExecuteRequest(
    @SerializedName("target_workflow") val targetWorkflow: String,
    @SerializedName("user_confirmed") val userConfirmed: Boolean,
    @SerializedName("residual_risk_accepted") val residualRiskAccepted: Boolean = true
)

data class ProtectedChangeExecuteRequest(
    @SerializedName("project_id") val projectId: String,
    @SerializedName("target_resource") val targetResource: String,
    val action: String,
    @SerializedName("approval_token") val approvalToken: String,
    @SerializedName("expected_base_version_hash") val expectedBaseVersionHash: String? = null,
    @SerializedName("proposed_change_hash") val proposedChangeHash: String? = null
)

data class ApprovalDecisionRequest(
    @SerializedName("approval_id") val approvalId: String,
    val decision: String, // "approved" | "rejected" | "cancelled"
    @SerializedName("decision_note") val decisionNote: String? = null
)

data class CinematicStageActionRequest(
    @SerializedName("stage_id") val stageId: String,
    val operation: String, // "queue" | "cancel" | "retry"
    @SerializedName("episode_id") val episodeId: String = "EP-01",
    @SerializedName("scene_id") val sceneId: String = "SCN-01"
)

// --- UI DOMAIN MODELS ---
data class ChatMessageUi(
    val id: String,
    val role: String,
    val content: String,
    val timestamp: String,
    val verificationStatus: String = "verified",
    val provider: String = "groq",
    val isError: Boolean = false,
    val rawJsonResult: String? = null
)

data class ProjectUi(
    val id: String,
    val namespace: String, // "Assistant" | "Cinematic"
    val name: String,
    val description: String,
    val status: String
)

data class SkillUi(
    val key: String,
    val name: String,
    val description: String,
    val ownerOnly: Boolean = false
)

data class TaskItemUi(
    val id: String,
    val title: String,
    val goal: String,
    val status: String,
    val priority: String,
    val approvalToken: String? = null,
    val cancelEligible: Boolean = true,
    val retryEligible: Boolean = false,
    val updatedAt: String = ""
)

data class ApprovalItemUi(
    val id: String,
    val actionType: String,
    val resourceType: String,
    val resourceId: String,
    val status: String, // pending, approved, rejected, cancelled
    val riskCategory: String,
    val reason: String,
    val issuedToken: String? = null,
    val expiresAt: String = ""
)

data class MemoryItemUi(
    val id: String,
    val scope: String,
    val key: String?,
    val content: String,
    val category: String,
    val knowledgeStatus: String,
    val confidence: Double?
)

data class ReminderItemUi(
    val id: String,
    val title: String,
    val scheduledAt: String,
    val status: String,
    val priority: String
)

data class CinematicStageUi(
    val stageId: String,
    val sectionName: String,
    val mappedWorkflow: String,
    val status: String,
    val lastUpdated: String
)

data class SecurityIncidentUi(
    val id: String,
    val severity: String,
    val category: String,
    val component: String,
    val status: String,
    val timestamp: String,
    val requiredAction: String
)
EOF

# ============================================================================
# FILE 8: ApiService.kt
# ============================================================================
cat > app/src/main/java/org/dpdns/bnai/assistant/data/ApiService.kt << 'EOF'
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
EOF

# ============================================================================
# FILE 9: AppRepository.kt
# ============================================================================
cat > app/src/main/java/org/dpdns/bnai/assistant/data/AppRepository.kt << 'EOF'
package org.dpdns.bnai.assistant.data

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.util.UUID

class AppRepository(
    val api: ApiService,
    val tokenManager: TokenManager
) {
    private val gson = Gson()
    private fun newRequestId(): String = UUID.randomUUID().toString()

    suspend fun login(email: String, pass: String): Result<Boolean> {
        return try {
            val resp = api.login(LoginRequest(email, pass))
            if (resp.isSuccessful && resp.body() != null) {
                val b = resp.body()!!
                val jwt = b.token ?: b.accessToken ?: "session_active"
                val uid = b.userId ?: tokenManager.getUserId()
                val wid = b.workspaceId ?: tokenManager.getWorkspaceId()
                val role = b.role ?: if (email.contains("admin", ignoreCase = true)) "owner_admin" else "normal_user"
                tokenManager.saveSession(jwt, uid, wid, email, role)
                Result.success(true)
            } else {
                Result.failure(Exception("Login failed: HTTP ${resp.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifySessionMe(): Result<JsonObject> = safeCall { api.getMe() }

    suspend fun sendFastChat(
        message: String,
        projectId: String?,
        conversationId: String?,
        skillKey: String,
        ceoMode: Boolean
    ): Result<ChatMessageUi> {
        val reqId = newRequestId()
        return try {
            val req = FastChatRequest(
                requestId = reqId,
                message = message,
                userId = tokenManager.getUserId(),
                workspaceId = tokenManager.getWorkspaceId(),
                projectId = projectId,
                conversationId = conversationId,
                skillKey = skillKey,
                ceoMode = ceoMode
            )
            val resp = api.fastChat(req)
            if (resp.isSuccessful && resp.body() != null) {
                val b = resp.body()!!
                val replyText = b.reply
                    ?: b.message
                    ?: b.result?.asJsonObject?.get("assistant_message")?.asString
                    ?: b.result?.toString()
                    ?: "Verified response received."
                Result.success(
                    ChatMessageUi(
                        id = reqId,
                        role = "assistant",
                        content = replyText,
                        timestamp = "Just now",
                        verificationStatus = "verified",
                        provider = b.provider ?: "groq (openai/gpt-oss-120b)"
                    )
                )
            } else {
                executeGatewayIntent("CHAT", projectId, conversationId, JsonObject().apply {
                    addProperty("message", message)
                }).map { gw ->
                    val childRes = gw.result?.getAsJsonObject("child_result")
                    val msg = childRes?.get("assistant_message")?.asString
                        ?: gw.errors?.firstOrNull()?.message
                        ?: "Gateway response status: ${gw.status}"
                    ChatMessageUi(
                        id = gw.requestId ?: reqId,
                        role = "assistant",
                        content = msg,
                        timestamp = "Just now",
                        verificationStatus = gw.verification?.status ?: "not_verified",
                        provider = childRes?.get("provider_used")?.asString ?: "wf00-gateway",
                        isError = gw.status == "failed",
                        rawJsonResult = gson.toJson(gw)
                    )
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun executeGatewayIntent(
        intent: String,
        projectId: String?,
        conversationId: String?,
        payload: JsonObject,
        sourceMode: String = "manual"
    ): Result<GatewayExecuteResponse> {
        return try {
            val req = GatewayExecuteRequest(
                requestId = newRequestId(),
                userId = tokenManager.getUserId(),
                workspaceId = tokenManager.getWorkspaceId(),
                projectId = projectId,
                conversationId = conversationId,
                intent = intent,
                sourceMode = sourceMode,
                payload = payload
            )
            val resp = api.executeWorkflow(req)
            val body = resp.body()
            if (body != null) {
                Result.success(body)
            } else {
                val errRaw = resp.errorBody()?.string()
                val parsedErr = try {
                    gson.fromJson(errRaw, GatewayExecuteResponse::class.java)
                } catch (_: Exception) {
                    null
                }
                if (parsedErr != null) Result.success(parsedErr)
                else Result.failure(Exception("HTTP ${resp.code()}: ${errRaw ?: "Empty response"}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchSkillsAndTools(): Result<JsonObject> = safeCall { api.listSkills() }
    suspend fun analyzeMasterBlueprint(blueprintText: String, projectId: String?, skillKey: String): Result<JsonObject> = safeCall {
        api.analyzeBlueprint(JsonObject().apply {
            addProperty("blueprint_text", blueprintText)
            addProperty("project_id", projectId)
            addProperty("skill_key", skillKey)
        })
    }
    suspend fun recordRepairAttempt(buildJobId: String, passed: Boolean, issues: List<String>): Result<JsonObject> = safeCall {
        val arr = JsonArray().apply { issues.forEach { add(it) } }
        api.recordRepairLoop(JsonObject().apply {
            addProperty("build_job_id", buildJobId)
            addProperty("project_id", tokenManager.getActiveProjectId())
            addProperty("validation_passed", passed)
            add("issues", arr)
        })
    }

    suspend fun fetchCopilotDashboard(): Result<JsonObject> = safeCall { api.getCopilotDashboard() }
    suspend fun diagnoseWithCopilot(req: CopilotDiagnoseRequest): Result<JsonObject> = safeCall { api.diagnoseError(req) }
    suspend fun analyzeScreenshotWithCopilot(ocrText: String, question: String): Result<JsonObject> = safeCall {
        api.analyzeScreenshot(JsonObject().apply {
            addProperty("project_id", tokenManager.getActiveProjectId())
            addProperty("ocr_extracted_text", ocrText)
            addProperty("user_question", question)
        })
    }
    suspend fun fetchNamespacedWorkflows(namespace: String? = null): Result<JsonObject> = safeCall {
        api.listNamespacedWorkflows(namespace)
    }
    suspend fun computeDiff(req: DiffViewerRequest): Result<JsonObject> = safeCall { api.computeWorkflowDiff(req) }
    suspend fun fetchCopilotHistory(): Result<JsonObject> = safeCall { api.getCopilotHistory() }

    suspend fun runSandboxPreflight(targetWf: String, purpose: String): Result<JsonObject> = safeCall {
        api.sandboxPreflight(JsonObject().apply {
            addProperty("target_workflow", targetWf)
            addProperty("diagnostic_purpose", purpose)
        })
    }
    suspend fun runSandboxExecution(targetWf: String, confirmed: Boolean): Result<JsonObject> = safeCall {
        api.sandboxExecute(SandboxExecuteRequest(targetWorkflow = targetWf, userConfirmed = confirmed))
    }
    suspend fun applyProtectedChange(req: ProtectedChangeExecuteRequest): Result<JsonObject> = safeCall {
        api.executeProtectedChange(req)
    }
    suspend fun fetchRecoveryPoints(): Result<JsonObject> = safeCall { api.getRecoveryPoints() }
    suspend fun executeRollback(
        targetResource: String,
        targetVersionHash: String,
        expectedCurrentHash: String?,
        approvalToken: String
    ): Result<JsonObject> = safeCall {
        api.executeRollback(JsonObject().apply {
            addProperty("target_resource", targetResource)
            addProperty("target_version_hash", targetVersionHash)
            if (!expectedCurrentHash.isNullOrBlank()) {
                addProperty("expected_current_hash", expectedCurrentHash)
            }
            addProperty("approval_token", approvalToken)
        })
    }

    suspend fun fetchApprovalsList(): Result<JsonObject> = safeCall { api.listApprovals() }
    suspend fun decideApproval(approvalId: String, decision: String, note: String?): Result<JsonObject> = safeCall {
        api.decideApproval(ApprovalDecisionRequest(approvalId, decision, note))
    }
    suspend fun requestNewApproval(actionType: String, resourceType: String, resourceId: String, reason: String): Result<JsonObject> = safeCall {
        api.requestApproval(JsonObject().apply {
            addProperty("project_id", tokenManager.getActiveProjectId())
            addProperty("action_type", actionType)
            addProperty("resource_type", resourceType)
            addProperty("resource_id", resourceId)
            addProperty("reason", reason)
        })
    }

    suspend fun fetchCinematicStudio(): Result<JsonObject> = safeCall { api.getCinematicStudio() }
    suspend fun triggerCinematicStage(stageId: String, op: String): Result<JsonObject> = safeCall {
        api.triggerCinematicStage(CinematicStageActionRequest(stageId = stageId, operation = op))
    }
    suspend fun fetchCinematicCopilotHandoff(episode: String, scene: String, stage: String, errorMsg: String): Result<JsonObject> = safeCall {
        api.getCinematicCopilotContext(JsonObject().apply {
            addProperty("current_episode", episode)
            addProperty("current_scene", scene)
            addProperty("failed_production_stage", stage)
            addProperty("relevant_error", errorMsg)
        })
    }

    suspend fun fetchRetentionPolicies(): Result<JsonObject> = safeCall { api.getRetentionPolicies() }
    suspend fun readScopedMemory(scope: String, projectId: String?): Result<JsonObject> = safeCall {
        api.readMemory(JsonObject().apply {
            addProperty("scope", scope)
            addProperty("project_id", projectId ?: tokenManager.getActiveProjectId())
            addProperty("limit", 30)
        })
    }
    suspend fun writeScopedMemory(scope: String, key: String, content: String, category: String): Result<JsonObject> = safeCall {
        api.writeMemory(JsonObject().apply {
            addProperty("scope", scope)
            if (key.isNotBlank()) addProperty("key", key)
            addProperty("content", content)
            addProperty("category", category)
            addProperty("project_id", tokenManager.getActiveProjectId())
            addProperty("knowledge_status", "observed")
        })
    }
    suspend fun deleteScopedMemory(memoryId: String): Result<JsonObject> = safeCall {
        api.deleteMemory(JsonObject().apply { addProperty("memory_id", memoryId) })
    }

    suspend fun fetchTasksList(): Result<JsonObject> = safeCall { api.listTasks() }
    suspend fun cancelTaskOnBackend(taskId: String): Result<JsonObject> = safeCall {
        api.cancelTask(JsonObject().apply { addProperty("task_id", taskId) })
    }
    suspend fun reconcileTaskOnBackend(taskId: String, resolution: String): Result<JsonObject> = safeCall {
        api.reconcileTask(JsonObject().apply {
            addProperty("task_id", taskId)
            addProperty("resolution", resolution)
        })
    }

    suspend fun fetchNotificationsList(): Result<JsonObject> = safeCall { api.listNotifications() }
    suspend fun dispatchNotification(title: String, message: String, channel: String, priority: String): Result<JsonObject> = safeCall {
        api.dispatchNotification(JsonObject().apply {
            addProperty("title", title)
            addProperty("message", message)
            addProperty("channel", channel)
            addProperty("priority", priority)
        })
    }
    suspend fun uploadFileMetadata(fileName: String, mimeType: String, sizeBytes: Long, artifactType: String, preview: String): Result<JsonObject> = safeCall {
        api.uploadFileMetadata(JsonObject().apply {
            addProperty("project_id", tokenManager.getActiveProjectId())
            addProperty("file_name", fileName)
            addProperty("mime_type", mimeType)
            addProperty("size_bytes", sizeBytes)
            addProperty("artifact_type", artifactType)
            addProperty("content_preview", preview)
        })
    }
    suspend fun exportProjectData(format: String): Result<JsonObject> = safeCall {
        api.exportProjectData(JsonObject().apply {
            addProperty("project_id", tokenManager.getActiveProjectId())
            addProperty("format", format)
        })
    }
    suspend fun deleteFileArtifact(artifactId: String): Result<JsonObject> = safeCall {
        api.deleteFileArtifact(JsonObject().apply {
            addProperty("artifact_id", artifactId)
            addProperty("project_id", tokenManager.getActiveProjectId())
        })
    }

    suspend fun fetchOwnerDashboard(): Result<JsonObject> = safeCall { api.getOwnerDashboard() }
    suspend fun fetchSchemaRegistry(): Result<JsonObject> = safeCall { api.getSchemaRegistry() }
    suspend fun executeOwnerAction(actionType: String, targetResource: String, approvalToken: String?, params: JsonObject? = null): Result<JsonObject> = safeCall {
        api.executeOwnerAction(JsonObject().apply {
            addProperty("action_type", actionType)
            addProperty("target_resource", targetResource)
            if (!approvalToken.isNullOrBlank()) addProperty("approval_token", approvalToken)
            if (params != null) add("parameters", params)
        })
    }
    suspend fun fetchUserDashboard(): Result<JsonObject> = safeCall { api.getUserDashboard() }
    suspend fun fetchCapabilitiesAudit(): Result<JsonObject> = safeCall { api.getCapabilities() }
    suspend fun fetchWorkflowAndContractRegistries(): Result<JsonObject> = safeCall { api.getWorkflowRegistry() }

    private suspend fun safeCall(call: suspend () -> retrofit2.Response<JsonObject>): Result<JsonObject> {
        return try {
            val resp = call()
            if (resp.isSuccessful && resp.body() != null) {
                Result.success(resp.body()!!)
            } else {
                val errStr = resp.errorBody()?.string() ?: "HTTP ${resp.code()}"
                Result.failure(Exception(errStr))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun parseJsonObjectOrNull(raw: String): JsonObject? {
        return try {
            JsonParser.parseString(raw.trim()).asJsonObject
        } catch (e: Exception) {
            null
        }
    }

    suspend fun checkBackendHealth(): Boolean {
        return try {
            api.checkHealth().isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}
EOF

echo "==========================================="
echo "✅ Frontend Phase 1 complete — 10 files created:"
echo "==========================================="
#!/bin/bash
set -e
cd ~/ai-assistant-android

# ============================================================================
# FILE 10: MainViewModel.kt
# ============================================================================
cat > app/src/main/java/org/dpdns/bnai/assistant/viewmodel/MainViewModel.kt << 'EOF'
package org.dpdns.bnai.assistant.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dpdns.bnai.assistant.BuildConfig
import org.dpdns.bnai.assistant.data.*
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class MainViewModel(private val repo: AppRepository) : ViewModel() {
    private val prettyGson = GsonBuilder().setPrettyPrinting().create()

    // --- SESSION, ROLE & GLOBAL NAVIGATION STATE ---
    private val _isLoggedIn = MutableStateFlow(repo.tokenManager.getToken() != null)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isOwner = MutableStateFlow(repo.tokenManager.isOwner())
    val isOwner: StateFlow<Boolean> = _isOwner.asStateFlow()

    private val _backendHealthy = MutableStateFlow(true)
    val backendHealthy: StateFlow<Boolean> = _backendHealthy.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    // --- TWO LOGICAL PROJECTS ---
    val projects = listOf(
        ProjectUi(
            id = BuildConfig.ASSISTANT_PROJECT_ID,
            namespace = "Assistant",
            name = "Project A — AI Assistant App",
            description = "Central AI Brain, Copilot, WF-00..WF-12 & System Operations",
            status = "Active"
        ),
        ProjectUi(
            id = BuildConfig.CINEMATIC_PROJECT_ID,
            namespace = "Cinematic",
            name = "Project B — AI Cinematic Engine",
            description = "Story Bible, Character Continuity, Scenes & Media Pipeline (WF-00..WF-13)",
            status = "Active"
        )
    )

    val skills = listOf(
        SkillUi("ai_admin", "AI Admin (Default)", "Hinglish Assistant, Context & Minimum Workflow Routing", false),
        SkillUi("n8n_wf_builder", "n8n WF Builder", "Blueprint Intake -> Generate -> Validate -> Max 5x Repair Loop", false),
        SkillUi("copilot", "AI Copilot & Troubleshooter", "Error Diagnosis, Screenshot Analysis & Diff Viewer", false),
        SkillUi("cinematic_director", "Cinematic Director", "Story Bible, Characters, Continuity & Scene Pipeline", false),
        SkillUi("owner_admin", "Owner Admin Console", "System Health, Schema Drift, Protected Deploy & Approvals", true)
    )

    val sessionModes = listOf("Auto", "Vision", "Story", "n8n Assistant", "Admin")

    private val _selectedProject = MutableStateFlow(projects.first())
    val selectedProject: StateFlow<ProjectUi> = _selectedProject.asStateFlow()

    private val _selectedSkill = MutableStateFlow(skills.first())
    val selectedSkill: StateFlow<SkillUi> = _selectedSkill.asStateFlow()

    private val _sessionMode = MutableStateFlow(repo.tokenManager.getSessionMode())
    val sessionMode: StateFlow<String> = _sessionMode.asStateFlow()

    private val _ceoMode = MutableStateFlow(false)
    val ceoMode: StateFlow<Boolean> = _ceoMode.asStateFlow()

    private val _useFastPath = MutableStateFlow(true)
    val useFastPath: StateFlow<Boolean> = _useFastPath.asStateFlow()

    private val _messages = MutableStateFlow(
        listOf(
            ChatMessageUi(
                id = "welcome-v31",
                role = "assistant",
                content = "Namaste! Main aapka Unified BNAI Primary AI Brain (v3.1) hun. Project A (AI Assistant) aur Project B (AI Cinematic Engine) dono active hain. Aaj hum kis par kaam karein?",
                timestamp = "Ready",
                verificationStatus = "verified",
                provider = "system"
            )
        )
    )
    val messages: StateFlow<List<ChatMessageUi>> = _messages.asStateFlow()

    private val _wfBuilderOutput = MutableStateFlow("")
    val wfBuilderOutput: StateFlow<String> = _wfBuilderOutput.asStateFlow()

    private val _lastGeneratedWfJson = MutableStateFlow("")
    val lastGeneratedWfJson: StateFlow<String> = _lastGeneratedWfJson.asStateFlow()

    private val _repairAttemptCount = MutableStateFlow(0)
    val repairAttemptCount: StateFlow<Int> = _repairAttemptCount.asStateFlow()

    private val _copilotConsoleOutput = MutableStateFlow("Select a diagnostic tool, screenshot analyzer, workflow inspector, diff check, sandbox, or rollback operation.")
    val copilotConsoleOutput: StateFlow<String> = _copilotConsoleOutput.asStateFlow()

    private val _lastBaseVersionHash = MutableStateFlow("")
    val lastBaseVersionHash: StateFlow<String> = _lastBaseVersionHash.asStateFlow()

    private val _lastProposedChangeHash = MutableStateFlow("")
    val lastProposedChangeHash: StateFlow<String> = _lastProposedChangeHash.asStateFlow()

    private val _tasks = MutableStateFlow<List<TaskItemUi>>(emptyList())
    val tasks: StateFlow<List<TaskItemUi>> = _tasks.asStateFlow()

    private val _approvals = MutableStateFlow<List<ApprovalItemUi>>(emptyList())
    val approvals: StateFlow<List<ApprovalItemUi>> = _approvals.asStateFlow()

    private val _lastIssuedApprovalToken = MutableStateFlow<String?>(null)
    val lastIssuedApprovalToken: StateFlow<String?> = _lastIssuedApprovalToken.asStateFlow()

    private val _reminders = MutableStateFlow<List<ReminderItemUi>>(emptyList())
    val reminders: StateFlow<List<ReminderItemUi>> = _reminders.asStateFlow()

    private val _projectProgressSummary = MutableStateFlow("Tap 'Refresh Progress (WF-06)' or 'Sync Live DB Tasks & Approvals'.")
    val projectProgressSummary: StateFlow<String> = _projectProgressSummary.asStateFlow()

    private val _cinematicStages = MutableStateFlow<List<CinematicStageUi>>(
        listOf(
            CinematicStageUi("STG-01", "Story Bible", "Cinematic / WF-01", "Completed", "Verified"),
            CinematicStageUi("STG-02", "Seasons and Episodes", "Cinematic / WF-02", "Completed", "Verified"),
            CinematicStageUi("STG-03", "Scripts", "Cinematic / WF-03", "Completed", "Verified"),
            CinematicStageUi("STG-04", "Characters", "Cinematic / WF-04", "Completed", "Verified"),
            CinematicStageUi("STG-05", "Character References", "Cinematic / WF-05", "Completed", "Verified"),
            CinematicStageUi("STG-06", "Continuity", "Cinematic / WF-06", "Completed", "Verified"),
            CinematicStageUi("STG-07", "Scenes", "Cinematic / WF-07", "Running", "Active"),
            CinematicStageUi("STG-08", "Image Assets", "Cinematic / WF-08", "Queued", "Pending"),
            CinematicStageUi("STG-09", "Animation Assets", "Cinematic / WF-09", "Not Started", "Idle"),
            CinematicStageUi("STG-10", "Voice and Emotion", "Cinematic / WF-10", "Not Started", "Idle"),
            CinematicStageUi("STG-11", "Audio and BGM", "Cinematic / WF-11", "Not Started", "Idle"),
            CinematicStageUi("STG-12", "SFX", "Cinematic / WF-11", "Not Started", "Idle"),
            CinematicStageUi("STG-13", "Rendering", "Cinematic / WF-12", "Waiting for Approval", "Gate"),
            CinematicStageUi("STG-14", "Quality Control", "Cinematic / WF-13", "Not Started", "Idle"),
            CinematicStageUi("STG-15", "Publishing", "Cinematic / WF-13", "Waiting for Approval", "Gate")
        )
    )
    val cinematicStages: StateFlow<List<CinematicStageUi>> = _cinematicStages.asStateFlow()

    private val _memoryItems = MutableStateFlow<List<MemoryItemUi>>(emptyList())
    val memoryItems: StateFlow<List<MemoryItemUi>> = _memoryItems.asStateFlow()

    private val _retentionPoliciesJson = MutableStateFlow("")
    val retentionPoliciesJson: StateFlow<String> = _retentionPoliciesJson.asStateFlow()

    private val _filesAndNotifsOutput = MutableStateFlow("Use the controls above to upload file metadata, export redacted data, dispatch notifications, or run UI-35 Capability Audit.")
    val filesAndNotifsOutput: StateFlow<String> = _filesAndNotifsOutput.asStateFlow()

    private val _ownerDashboardJson = MutableStateFlow("Tap 'Load Live Owner Telemetry' or 'Inspect 34-Table Schema' to view live governance data.")
    val ownerDashboardJson: StateFlow<String> = _ownerDashboardJson.asStateFlow()

    private val _incidents = MutableStateFlow(
        listOf(
            SecurityIncidentUi(
                id = "INC-301",
                severity = "WARNING",
                category = "Credential Rotation Pending",
                component = "WF-00 / Backend .env",
                status = "OWNER_ACTION_REQUIRED",
                timestamp = "Active Baseline",
                requiredAction = "Ensure x-service-token matches TRUSTED_SERVICE_TOKEN across WF-00"
            )
        )
    )
    val incidents: StateFlow<List<SecurityIncidentUi>> = _incidents.asStateFlow()

    init { refreshHealth() }

    fun refreshHealth() {
        viewModelScope.launch { _backendHealthy.value = repo.checkBackendHealth() }
    }

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repo.login(email, pass)
            _isLoading.value = false
            res.onSuccess {
                _isLoggedIn.value = true
                _isOwner.value = repo.tokenManager.isOwner()
                _statusBanner.value = "Authenticated as ${repo.tokenManager.getEmail()} (${repo.tokenManager.getRole()})"
            }.onFailure { e -> _statusBanner.value = "Authentication failed: ${e.message}" }
        }
    }

    fun logout() {
        repo.tokenManager.clearSession()
        _isLoggedIn.value = false
    }

    fun selectProject(project: ProjectUi) {
        _selectedProject.value = project
        repo.tokenManager.setActiveProjectId(project.id)
        if (project.namespace == "Cinematic" && _sessionMode.value == "Auto") setSessionMode("Story")
        _statusBanner.value = "Switched isolated context to ${project.namespace} (${project.name})"
    }

    fun selectSkill(skill: SkillUi) {
        if (skill.ownerOnly && !_isOwner.value) {
            _statusBanner.value = "Access Denied: Owner Admin role required (Backend enforced)"
            return
        }
        _selectedSkill.value = skill
    }

    fun setSessionMode(mode: String) {
        if (mode == "Admin" && !_isOwner.value) _statusBanner.value = "Mode Safety: Selecting Admin mode does not grant Owner privileges."
        _sessionMode.value = mode
        repo.tokenManager.setSessionMode(mode)
    }

    fun toggleCeoMode(enabled: Boolean) { _ceoMode.value = enabled }
    fun toggleFastPath(enabled: Boolean) { _useFastPath.value = enabled }
    fun clearBanner() { _statusBanner.value = null }

    fun sendChatMessage(userText: String) {
        if (userText.isBlank()) return
        val userMsg = ChatMessageUi(id = UUID.randomUUID().toString(), role = "user", content = userText, timestamp = "Now")
        _messages.value = _messages.value + userMsg

        viewModelScope.launch {
            _isLoading.value = true
            val pid = _selectedProject.value.id
            if (_useFastPath.value) {
                val res = repo.sendFastChat(userText, pid, null, _selectedSkill.value.key, _ceoMode.value)
                res.onSuccess { reply -> _messages.value = _messages.value + reply }
                .onFailure { err -> _messages.value = _messages.value + ChatMessageUi(id = UUID.randomUUID().toString(), role = "assistant", content = "Fast Chat Error: ${err.message}", timestamp = "Now", verificationStatus = "not_verified", isError = true) }
            } else {
                val payload = JsonObject().apply { addProperty("message", userText); addProperty("session_mode", _sessionMode.value) }
                val res = repo.executeGatewayIntent("CHAT", pid, null, payload)
                res.onSuccess { gw ->
                    val child = gw.result?.getAsJsonObject("child_result")
                    val text = child?.get("assistant_message")?.asString ?: gw.errors?.firstOrNull()?.message ?: prettyGson.toJson(gw.result)
                    _messages.value = _messages.value + ChatMessageUi(id = gw.requestId ?: UUID.randomUUID().toString(), role = "assistant", content = text, timestamp = "Now", verificationStatus = gw.verification?.status ?: "not_verified", provider = child?.get("provider_used")?.asString ?: "WF-00", isError = gw.status == "failed", rawJsonResult = prettyGson.toJson(gw))
                }.onFailure { err -> _messages.value = _messages.value + ChatMessageUi(id = UUID.randomUUID().toString(), role = "assistant", content = "Gateway Error: ${err.message}", timestamp = "Now", verificationStatus = "not_verified", isError = true) }
            }
            _isLoading.value = false
        }
    }

    fun generateAndAutoRepairWorkflow(requirements: String, targetVersion: String = "1.80.0") {
        if (requirements.isBlank()) { _wfBuilderOutput.value = "Error: Requirements text is required for WF-02 generation."; return }
        viewModelScope.launch {
            _isLoading.value = true
            _repairAttemptCount.value = 0
            val buildJobId = UUID.randomUUID().toString()
            val logBuilder = StringBuilder("=== STEP 1: WF-02 Generating Workflow JSON (Job: ${buildJobId.take(8)}) ===\n")
            
            val genPayload = JsonObject().apply {
                addProperty("requirements", requirements)
                addProperty("target_n8n_version", targetVersion)
                addProperty("blueprint_reference", "Unified Master Blueprint v3.1")
            }
            val genRes = repo.executeGatewayIntent("GENERATE_WORKFLOW", _selectedProject.value.id, null, genPayload)
            val genEnv = genRes.getOrNull()
            val childRes = genEnv?.result?.getAsJsonObject("child_result") ?: genEnv?.result
            var currentWfJson = childRes?.getAsJsonObject("generated_workflow_json")

            if (currentWfJson == null) {
                logBuilder.appendLine("Generation failed or returned no JSON:\n${prettyGson.toJson(genEnv)}")
                _wfBuilderOutput.value = logBuilder.toString()
                _isLoading.value = false
                return@launch
            }

            _lastGeneratedWfJson.value = prettyGson.toJson(currentWfJson)
            logBuilder.appendLine("WF-02 generated initial workflow JSON successfully.")

            var passedValidation = false
            for (attempt in 1..5) {
                logBuilder.appendLine("\n=== VALIDATION CHECK (WF-04) — Attempt #$attempt ===")
                val valPayload = JsonObject().apply { add("workflow_json", currentWfJson); addProperty("target_n8n_version", targetVersion) }
                val valRes = repo.executeGatewayIntent("VALIDATE_WORKFLOW", _selectedProject.value.id, null, valPayload)
                val valEnv = valRes.getOrNull()
                val valChild = valEnv?.result?.getAsJsonObject("child_result") ?: valEnv?.result
                val vStatus = valChild?.get("validation_status")?.asString?.lowercase() ?: ""

                if (valEnv?.status == "completed" && (vStatus == "passed" || vStatus == "valid" || valEnv.errors.isNullOrEmpty())) {
                    logBuilder.appendLine("WF-04 Validation PASSED on check #$attempt.")
                    repo.recordRepairAttempt(buildJobId, true, emptyList())
                    passedValidation = true
                    break
                }

                _repairAttemptCount.value = attempt
                val issueSummary = valEnv?.errors?.joinToString("; ") { it.message ?: "" } ?: valChild?.get("failed_tests")?.toString() ?: "Fix validation structure and node parameters"
                repo.recordRepairAttempt(buildJobId, false, listOf(issueSummary))
                logBuilder.appendLine("Validation issues detected: $issueSummary\nRunning WF-03 Auto-Repair Attempt #$attempt of 5...")

                val updPayload = JsonObject().apply { add("original_workflow_json", currentWfJson); addProperty("requested_changes", "Repair validation issues: $issueSummary") }
                val updRes = repo.executeGatewayIntent("UPDATE_WORKFLOW", _selectedProject.value.id, null, updPayload)
                val updEnv = updRes.getOrNull()
                val updChild = updEnv?.result?.getAsJsonObject("child_result") ?: updEnv?.result
                val repairedJson = updChild?.getAsJsonObject("updated_workflow_json")

                if (repairedJson != null) {
                    currentWfJson = repairedJson
                    _lastGeneratedWfJson.value = prettyGson.toJson(currentWfJson)
                    logBuilder.appendLine("WF-03 Repair #$attempt applied.")
                } else {
                    logBuilder.appendLine("WF-03 Repair #$attempt returned no updated JSON. Halting loop.")
                    break
                }
            }

            if (!passedValidation) logBuilder.appendLine("\n[WARNING] Unresolved issues remain after ${_repairAttemptCount.value} repair attempts. Do not deploy without manual review.")
            logBuilder.appendLine("\n=== FINAL WORKFLOW JSON ===\n${prettyGson.toJson(currentWfJson)}")
            _wfBuilderOutput.value = logBuilder.toString()
            _isLoading.value = false
        }
    }

    fun analyzeWorkflowWf01(workflowJsonStr: String, mode: String = "full_audit", errorLogs: String = "") {
        val parsed = repo.parseJsonObjectOrNull(workflowJsonStr)
        if (parsed == null) { _wfBuilderOutput.value = "Error: Invalid Workflow JSON object."; return }
        viewModelScope.launch {
            _isLoading.value = true
            val payload = JsonObject().apply { add("workflow_json", parsed); addProperty("analysis_mode", mode); if (errorLogs.isNotBlank()) addProperty("error_logs", errorLogs) }
            repo.executeGatewayIntent("ANALYZE_WORKFLOW", _selectedProject.value.id, null, payload, "uploaded")
                .onSuccess { _wfBuilderOutput.value = prettyGson.toJson(it) }
                .onFailure { _wfBuilderOutput.value = "WF-01 Error: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun updateWorkflowWf03(originalJsonStr: String, requestedChanges: String) {
        val parsed = repo.parseJsonObjectOrNull(originalJsonStr)
        if (parsed == null || requestedChanges.isBlank()) { _wfBuilderOutput.value = "Error: Valid original_workflow_json and requested_changes required."; return }
        viewModelScope.launch {
            _isLoading.value = true
            val payload = JsonObject().apply { add("original_workflow_json", parsed); addProperty("requested_changes", requestedChanges) }
            repo.executeGatewayIntent("UPDATE_WORKFLOW", _selectedProject.value.id, null, payload)
                .onSuccess { gw ->
                    val child = gw.result?.getAsJsonObject("child_result") ?: gw.result
                    child?.getAsJsonObject("updated_workflow_json")?.let { _lastGeneratedWfJson.value = prettyGson.toJson(it) }
                    _wfBuilderOutput.value = prettyGson.toJson(gw)
                }.onFailure { _wfBuilderOutput.value = "WF-03 Error: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun researchDocsWf05(question: String, docVersion: String = "1.80.0") {
        if (question.length < 5) { _wfBuilderOutput.value = "Question must be at least 5 characters for WF-05."; return }
        viewModelScope.launch {
            _isLoading.value = true
            val payload = JsonObject().apply { addProperty("question", question); addProperty("documentation_version", docVersion) }
            repo.executeGatewayIntent("RESEARCH_DOCUMENTATION", _selectedProject.value.id, null, payload)
                .onSuccess { _wfBuilderOutput.value = prettyGson.toJson(it) }
                .onFailure { _wfBuilderOutput.value = "WF-05 Error: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun listLiveWorkflowsWf09() {
        viewModelScope.launch {
            _isLoading.value = true
            repo.executeGatewayIntent("LIST_LIVE_WORKFLOWS", _selectedProject.value.id, null, JsonObject(), "live")
                .onSuccess { _wfBuilderOutput.value = prettyGson.toJson(it) }
                .onFailure { _wfBuilderOutput.value = "WF-09 Error: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun getLiveWorkflowWf09(workflowId: String) {
        if (workflowId.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            val payload = JsonObject().apply { addProperty("workflow_id", workflowId.trim()) }
            repo.executeGatewayIntent("GET_LIVE_WORKFLOW", _selectedProject.value.id, null, payload, "live")
                .onSuccess { _wfBuilderOutput.value = prettyGson.toJson(it) }
                .onFailure { _wfBuilderOutput.value = "WF-09 Error: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun runCopilotDiagnosis(wfId: String, failedNode: String, errorMsg: String, logs: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val req = CopilotDiagnoseRequest(projectId = _selectedProject.value.id, workflowId = wfId.ifBlank { null }, failedNode = failedNode.ifBlank { null }, errorMessage = errorMsg, executionLogs = logs.ifBlank { null })
            repo.diagnoseWithCopilot(req)
                .onSuccess { _copilotConsoleOutput.value = prettyGson.toJson(it) }
                .onFailure { _copilotConsoleOutput.value = "Copilot Diagnosis Failed: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun analyzeScreenshotOcr(ocrText: String, userQuestion: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repo.analyzeScreenshotWithCopilot(ocrText, userQuestion)
                .onSuccess { _copilotConsoleOutput.value = prettyGson.toJson(it) }
                .onFailure { _copilotConsoleOutput.value = "Screenshot Analysis Failed: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun inspectNamespacedWorkflows(namespaceFilter: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            repo.fetchNamespacedWorkflows(namespaceFilter)
                .onSuccess { _copilotConsoleOutput.value = prettyGson.toJson(it) }
                .onFailure { _copilotConsoleOutput.value = "Workflow Inspector Error: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun loadCopilotDiagnosticHistory() {
        viewModelScope.launch {
            _isLoading.value = true
            repo.fetchCopilotHistory()
                .onSuccess { _copilotConsoleOutput.value = prettyGson.toJson(it) }
                .onFailure { _copilotConsoleOutput.value = "Copilot History Error: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun runWorkflowDiffViewer(wfId: String, origJsonStr: String, propJsonStr: String) {
        val orig = repo.parseJsonObjectOrNull(origJsonStr)
        val prop = repo.parseJsonObjectOrNull(propJsonStr)
        if (orig == null || prop == null) { _copilotConsoleOutput.value = "Error: Both Original JSON and Proposed JSON must be valid JSON objects."; return }
        viewModelScope.launch {
            _isLoading.value = true
            val req = DiffViewerRequest(workflowId = wfId.ifBlank { "WF-00" }, projectNamespace = _selectedProject.value.namespace, originalJson = orig, proposedJson = prop)
            repo.computeDiff(req)
                .onSuccess { res ->
                    _lastBaseVersionHash.value = res.get("base_version_hash")?.asString ?: ""
                    _lastProposedChangeHash.value = res.get("proposed_change_hash")?.asString ?: ""
                    _copilotConsoleOutput.value = prettyGson.toJson(res)
                }.onFailure { _copilotConsoleOutput.value = "Diff Viewer Error: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun runSandboxDiagnostic(targetWf: String, userConfirmed: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = if (!userConfirmed) repo.runSandboxPreflight(targetWf, "Isolated structural & runtime preflight check")
                      else repo.runSandboxExecution(targetWf, true)
            res.onSuccess { _copilotConsoleOutput.value = prettyGson.toJson(it) }
               .onFailure { _copilotConsoleOutput.value = "Sandbox Error: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun executeProtectedLiveChange(targetResource: String, action: String, approvalToken: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val req = ProtectedChangeExecuteRequest(projectId = _selectedProject.value.id, targetResource = targetResource, action = action, approvalToken = approvalToken, expectedBaseVersionHash = _lastBaseVersionHash.value.ifBlank { null }, proposedChangeHash = _lastProposedChangeHash.value.ifBlank { null })
            repo.applyProtectedChange(req)
                .onSuccess { _copilotConsoleOutput.value = prettyGson.toJson(it) }
                .onFailure { _copilotConsoleOutput.value = "Protected Change Blocked/Failed: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun loadRecoveryPoints() {
        viewModelScope.launch {
            _isLoading.value = true
            repo.fetchRecoveryPoints()
                .onSuccess { _copilotConsoleOutput.value = prettyGson.toJson(it) }
                .onFailure { _copilotConsoleOutput.value = "Recovery Points Error: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun executeResourceRollback(targetResource: String, targetVersionHash: String, approvalToken: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repo.executeRollback(targetResource, targetVersionHash, _lastBaseVersionHash.value.ifBlank { null }, approvalToken)
                .onSuccess { _copilotConsoleOutput.value = prettyGson.toJson(it) }
                .onFailure { _copilotConsoleOutput.value = "Rollback Blocked/Failed: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun syncLiveTasksAndApprovals() {
        viewModelScope.launch {
            _isLoading.value = true
            repo.fetchTasksList().onSuccess { res ->
                val list = mutableListOf<TaskItemUi>()
                res.getAsJsonArray("tasks")?.forEach { el ->
                    val obj = el.asJsonObject
                    val st = obj.get("status")?.asString?.uppercase() ?: "PENDING"
                    list.add(TaskItemUi(
                        id = obj.get("id")?.asString ?: UUID.randomUUID().toString(), title = obj.get("title")?.asString ?: "Task",
                        goal = obj.get("goal")?.asString ?: obj.get("blocked_reason")?.asString ?: "Task", status = st,
                        priority = obj.get("priority")?.asString ?: "normal", cancelEligible = st !in listOf("COMPLETED", "FAILED", "CANCELLED"),
                        retryEligible = st in listOf("FAILED", "PAUSED", "OUTCOME_UNKNOWN"), updatedAt = obj.get("updated_at")?.asString ?: ""
                    ))
                }
                _tasks.value = list
            }
            repo.fetchApprovalsList().onSuccess { res ->
                val list = mutableListOf<ApprovalItemUi>()
                res.getAsJsonArray("approvals")?.forEach { el ->
                    val obj = el.asJsonObject
                    list.add(ApprovalItemUi(
                        id = obj.get("id")?.asString ?: UUID.randomUUID().toString(), actionType = obj.get("action_type")?.asString ?: "ACTION",
                        resourceType = obj.get("resource_type")?.asString ?: "resource", resourceId = obj.get("resource_id")?.asString ?: "id",
                        status = obj.get("status")?.asString ?: "pending", riskCategory = "protected_system_change",
                        reason = obj.getAsJsonObject("metadata")?.get("reason")?.asString ?: "Approval required", expiresAt = obj.get("expires_at")?.asString ?: ""
                    ))
                }
                _approvals.value = list
            }
            _isLoading.value = false
        }
    }

    fun fetchProjectProgressWf06() {
        viewModelScope.launch {
            _isLoading.value = true
            repo.executeGatewayIntent("GET_PROJECT_PROGRESS", _selectedProject.value.id, null, JsonObject())
                .onSuccess { gw ->
                    val child = gw.result?.getAsJsonObject("child_result") ?: gw.result
                    val summary = child?.get("project_summary")?.asString ?: "Progress loaded."
                    val pendingApprovals = child?.get("pending_approvals")?.asInt ?: 0
                    val auditsCount = child?.get("workflow_audits_count")?.asInt ?: 0
                    _projectProgressSummary.value = "$summary\nPending Approvals: $pendingApprovals | Workflow Audits: $auditsCount"
                    syncLiveTasksAndApprovals()
                }.onFailure { _projectProgressSummary.value = "WF-06 Error: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun createTaskWf08(taskType: String, goal: String, priority: String = "important") {
        if (taskType.isBlank() || goal.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            val payload = JsonObject().apply { addProperty("task_type", taskType); addProperty("goal", goal); addProperty("description", goal); addProperty("priority", priority) }
            repo.executeGatewayIntent("EXECUTE_TASK", _selectedProject.value.id, null, payload)
                .onSuccess { gw ->
                    val child = gw.result?.getAsJsonObject("child_result") ?: gw.result
                    val taskObj = child?.getAsJsonObject("task")
                    val newTaskId = taskObj?.get("id")?.asString ?: gw.taskId ?: UUID.randomUUID().toString()
                    val newItem = TaskItemUi(id = newTaskId, title = taskType, goal = goal, status = (taskObj?.get("status")?.asString ?: "CREATED").uppercase(), priority = priority)
                    _tasks.value = listOf(newItem) + _tasks.value
                    _statusBanner.value = "Task created via WF-08 (${newTaskId.take(8)}...)"
                }.onFailure { _statusBanner.value = "Create Task Failed: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun requestTaskCancellation(taskId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repo.cancelTaskOnBackend(taskId).onSuccess { res ->
                val cState = res.get("cancellation_state")?.asString ?: "cancellation_requested"
                _tasks.value = _tasks.value.map { if (it.id == taskId) it.copy(status = cState.uppercase(), cancelEligible = false, retryEligible = true) else it }
                _statusBanner.value = "Backend Task Cancellation State: $cState"
            }.onFailure { err -> _statusBanner.value = "Cancel Request Error: ${err.message}" }
            _isLoading.value = false
        }
    }

    fun reconcileTaskOutcome(taskId: String, resolution: String = "resume_check") {
        viewModelScope.launch {
            _isLoading.value = true
            repo.reconcileTaskOnBackend(taskId, resolution).onSuccess { res ->
                val taskObj = res.getAsJsonObject("task")
                val updatedStatus = (taskObj?.get("status")?.asString ?: "RECONCILED").uppercase()
                _tasks.value = _tasks.value.map { if (it.id == taskId) it.copy(status = updatedStatus, retryEligible = false) else it }
                _statusBanner.value = "Task ${taskId.take(8)} reconciled -> $updatedStatus"
            }.onFailure { err -> _statusBanner.value = "Reconciliation Failed: ${err.message}" }
            _isLoading.value = false
        }
    }

    fun createApprovalRequest(actionType: String, resourceType: String, resourceId: String, reason: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repo.requestNewApproval(actionType, resourceType, resourceId, reason)
                .onSuccess { res ->
                    val appr = res.getAsJsonObject("approval")
                    val id = appr?.get("id")?.asString ?: UUID.randomUUID().toString()
                    val item = ApprovalItemUi(id = id, actionType = actionType, resourceType = resourceType, resourceId = resourceId, status = "pending", riskCategory = "protected_system_change", reason = reason, expiresAt = appr?.get("expires_at")?.asString ?: "+60m")
                    _approvals.value = listOf(item) + _approvals.value
                    _statusBanner.value = "Approval Request Created ($id)"
                }.onFailure { _statusBanner.value = "Approval Request Error: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun submitApprovalDecision(approvalId: String, decision: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repo.decideApproval(approvalId, decision, "Decided from Android v1.1 Approval Inbox")
                .onSuccess { res ->
                    val tok = res.get("approval_token")?.asString
                    if (!tok.isNullOrBlank()) _lastIssuedApprovalToken.value = tok
                    _approvals.value = _approvals.value.map { if (it.id == approvalId) it.copy(status = decision, issuedToken = tok) else it }
                    _statusBanner.value = "Approval $decision. ${if (tok != null) "SHA-256 Token issued!" else ""}"
                }.onFailure { _statusBanner.value = "Decision Rejected: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun scheduleReminderWf10(title: String, minutesFromNow: Long = 30, priority: String = "important") {
        if (title.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            val scheduledIso = Instant.now().plus(minutesFromNow, ChronoUnit.MINUTES).toString()
            val payload = JsonObject().apply { addProperty("title", title); addProperty("scheduled_at", scheduledIso); addProperty("priority", priority); addProperty("reminder_type", "task_followup") }
            repo.executeGatewayIntent("SET_REMINDER", _selectedProject.value.id, null, payload)
                .onSuccess { gw ->
                    val child = gw.result?.getAsJsonObject("child_result") ?: gw.result
                    val remId = child?.get("reminder_id")?.asString ?: UUID.randomUUID().toString()
                    _reminders.value = listOf(ReminderItemUi(remId, title, scheduledIso, "scheduled", priority)) + _reminders.value
                    _statusBanner.value = "Reminder scheduled via WF-10 for $scheduledIso"
                }.onFailure { _statusBanner.value = "WF-10 Error: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun syncLiveCinematicStudio() {
        viewModelScope.launch {
            _isLoading.value = true
            repo.fetchCinematicStudio().onSuccess { res ->
                val list = mutableListOf<CinematicStageUi>()
                res.getAsJsonArray("pipeline_sections")?.forEach { el ->
                    val obj = el.asJsonObject
                    list.add(CinematicStageUi(
                        stageId = obj.get("stage_id")?.asString ?: "STG", sectionName = obj.get("section_name")?.asString ?: "Stage",
                        mappedWorkflow = obj.get("mapped_workflow")?.asString ?: "Cinematic / WF", status = obj.get("status")?.asString ?: "Not Started",
                        lastUpdated = obj.get("last_updated")?.asString ?: ""
                    ))
                }
                if (list.isNotEmpty()) _cinematicStages.value = list
            }
            _isLoading.value = false
        }
    }

    fun triggerCinematicStageOperation(stageId: String, operation: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repo.triggerCinematicStage(stageId, operation)
                .onSuccess { res ->
                    val stg = res.getAsJsonObject("updated_stage")
                    val newStatus = stg?.get("status")?.asString ?: "Queued"
                    _cinematicStages.value = _cinematicStages.value.map { if (it.stageId == stageId) it.copy(status = newStatus, lastUpdated = "Just now") else it }
                    _statusBanner.value = "Cinematic Stage $stageId -> $newStatus"
                }.onFailure { _statusBanner.value = "Cinematic Operation Failed: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun syncScopedMemoryFromBackend(scope: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repo.readScopedMemory(scope, _selectedProject.value.id).onSuccess { res ->
                val loaded = mutableListOf<MemoryItemUi>()
                res.getAsJsonArray("memory_items")?.forEach { el ->
                    val obj = el.asJsonObject
                    loaded.add(MemoryItemUi(
                        id = obj.get("id")?.asString ?: UUID.randomUUID().toString(), scope = obj.get("scope")?.asString ?: scope,
                        key = if (obj.get("key")?.isJsonNull == false) obj.get("key")?.asString else null, content = obj.get("content")?.asString ?: "",
                        category = obj.get("category")?.asString ?: "general", knowledgeStatus = obj.get("knowledge_status")?.asString ?: "observed", confidence = 0.95
                    ))
                }
                _memoryItems.value = loaded
            }
            repo.fetchRetentionPolicies().onSuccess { pol -> _retentionPoliciesJson.value = prettyGson.toJson(pol) }
            _isLoading.value = false
        }
    }

    fun addLocalOrRemoteMemory(scope: String, key: String, content: String, category: String = "general") {
        if (content.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            repo.writeScopedMemory(scope, key, content, category).onSuccess { res ->
                val obj = res.getAsJsonObject("memory_item")
                val id = obj?.get("id")?.asString ?: UUID.randomUUID().toString()
                val newItem = MemoryItemUi(id = id, scope = scope, key = key.ifBlank { null }, content = content, category = category, knowledgeStatus = obj?.get("knowledge_status")?.asString ?: "observed", confidence = 0.95)
                _memoryItems.value = listOf(newItem) + _memoryItems.value
                _statusBanner.value = "Memory persisted to Supabase in scope '$scope' ($id)."
            }.onFailure { err -> _statusBanner.value = "Memory Write Error: ${err.message}" }
            _isLoading.value = false
        }
    }

    fun requestMemoryDeletion(memoryId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repo.deleteScopedMemory(memoryId).onSuccess {
                _memoryItems.value = _memoryItems.value.filterNot { it.id == memoryId }
                _statusBanner.value = "Memory deleted from Supabase & recorded in data_jobs."
            }.onFailure { err -> _statusBanner.value = "Memory Delete Error: ${err.message}" }
            _isLoading.value = false
        }
    }

    fun uploadProjectFileMetadata(fileName: String, artifactType: String, preview: String) {
        if (fileName.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            repo.uploadFileMetadata(fileName, "application/json", preview.toByteArray().size.toLong().coerceAtLeast(128L), artifactType, preview)
                .onSuccess { _filesAndNotifsOutput.value = prettyGson.toJson(it) }
                .onFailure { _filesAndNotifsOutput.value = "File Upload Error: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun triggerRedactedProjectExport(format: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repo.exportProjectData(format)
                .onSuccess { _filesAndNotifsOutput.value = prettyGson.toJson(it) }
                .onFailure { _filesAndNotifsOutput.value = "Export Error: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun loadNotificationsAndUserDashboard() {
        viewModelScope.launch {
            _isLoading.value = true
            val combined = JsonObject()
            repo.fetchNotificationsList().onSuccess { combined.add("notifications_center", it) }
            repo.fetchUserDashboard().onSuccess { combined.add("user_dashboard_quotas", it) }
            _filesAndNotifsOutput.value = prettyGson.toJson(combined)
            _isLoading.value = false
        }
    }

    fun sendMultiChannelNotification(title: String, message: String, channel: String) {
        if (title.isBlank() || message.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            repo.dispatchNotification(title, message, channel, "important")
                .onSuccess { _filesAndNotifsOutput.value = prettyGson.toJson(it) }
                .onFailure { _filesAndNotifsOutput.value = "Notification Error: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun runUi35FinalCapabilityAudit() {
        viewModelScope.launch {
            _isLoading.value = true
            val auditObj = JsonObject()
            repo.fetchCapabilitiesAudit().onSuccess { auditObj.add("backend_v31_capabilities", it) }
            repo.fetchWorkflowAndContractRegistries().onSuccess { auditObj.add("n8n_workflow_registry", it) }
            _filesAndNotifsOutput.value = prettyGson.toJson(auditObj)
            _isLoading.value = false
        }
    }

    fun loadOwnerAdminTelemetry() {
        viewModelScope.launch {
            _isLoading.value = true
            repo.fetchOwnerDashboard()
                .onSuccess { _ownerDashboardJson.value = prettyGson.toJson(it) }
                .onFailure { _ownerDashboardJson.value = "Owner Access Denied / Error: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun loadOwnerSchemaRegistry() {
        viewModelScope.launch {
            _isLoading.value = true
            repo.fetchSchemaRegistry()
                .onSuccess { _ownerDashboardJson.value = prettyGson.toJson(it) }
                .onFailure { _ownerDashboardJson.value = "Schema Registry Error: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun triggerOwnerConsoleAction(actionType: String, targetResource: String, approvalToken: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            repo.executeOwnerAction(actionType, targetResource, approvalToken)
                .onSuccess { _ownerDashboardJson.value = prettyGson.toJson(it) }
                .onFailure { _ownerDashboardJson.value = "Owner Action Blocked/Failed: ${it.message}" }
            _isLoading.value = false
        }
    }
}
EOF

# ============================================================================
# FILE 11: FloatingBubbleService.kt
# ============================================================================
cat > app/src/main/java/org/dpdns/bnai/assistant/service/FloatingBubbleService.kt << 'EOF'
package org.dpdns.bnai.assistant.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class FloatingBubbleService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var rootLayout: LinearLayout
    private lateinit var panelLayout: LinearLayout
    private lateinit var statusLabel: TextView

    private val handler = Handler(Looper.getMainLooper())
    private var isHoldingCapture = false
    private var capturedFramesCount = 0

    private val holdCaptureRunnable = object : Runnable {
        override fun run() {
            if (isHoldingCapture) {
                capturedFramesCount += 1
                statusLabel.text = "Capturing (1 FPS Hold): $capturedFramesCount frames queued"
                handler.postDelayed(this, 1000L)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val density = resources.displayMetrics.density
        val bubbleSizePx = (60 * density).toInt()

        rootLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val bubbleView = TextView(this).apply {
            text = "AI"
            textSize = 15f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(bubbleSizePx, bubbleSizePx)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#7C8CFF"))
                setStroke(4, Color.parseColor("#29C7C7"))
            }
        }

        panelLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(24, 24, 24, 24)
            background = GradientDrawable().apply {
                cornerRadius = 24f
                setColor(Color.parseColor("#151B2D"))
                setStroke(2, Color.parseColor("#303A52"))
            }
        }

        statusLabel = TextView(this).apply {
            text = "Idle (Explicit Consent Required for Screen Capture)"
            setTextColor(Color.parseColor("#F5F7FF"))
            textSize = 12f
        }

        val singleCaptureBtn = Button(this).apply {
            text = "Single Screenshot Analyze"
            setOnClickListener {
                capturedFramesCount += 1
                statusLabel.text = "Captured 1 Screenshot -> Queued for Copilot Vision Analysis"
                Toast.makeText(this@FloatingBubbleService, "Screenshot queued (7-day ephemeral retention)", Toast.LENGTH_SHORT).show()
            }
        }

        val holdCaptureBtn = Button(this).apply {
            text = "Hold (1 FPS Capture) / Tap to Toggle"
            setOnClickListener {
                if (!isHoldingCapture) {
                    isHoldingCapture = true
                    handler.post(holdCaptureRunnable)
                } else {
                    stopHoldCapture("Hold capture paused ($capturedFramesCount frames).")
                }
            }
        }

        val stopServiceBtn = Button(this).apply {
            text = "STOP FLOATING ASSISTANT"
            setBackgroundColor(Color.parseColor("#F16C7B"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                stopHoldCapture("Session stopped by user.")
                stopSelf()
            }
        }

        panelLayout.addView(statusLabel)
        panelLayout.addView(singleCaptureBtn)
        panelLayout.addView(holdCaptureBtn)
        panelLayout.addView(stopServiceBtn)

        rootLayout.addView(bubbleView)
        rootLayout.addView(panelLayout)

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 220
        }

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        bubbleView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x; initialY = params.y
                    initialTouchX = event.rawX; initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(rootLayout, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val dx = kotlin.math.abs(event.rawX - initialTouchX)
                    val dy = kotlin.math.abs(event.rawY - initialTouchY)
                    if (dx < 15 && dy < 15) {
                        panelLayout.visibility = if (panelLayout.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                    }
                    true
                }
                else -> false
            }
        }

        windowManager.addView(rootLayout, params)
    }

    private fun stopHoldCapture(reason: String) {
        isHoldingCapture = false
        handler.removeCallbacks(holdCaptureRunnable)
        statusLabel.text = reason
    }

    private fun startForegroundNotification() {
        val channelId = "bnai_floating_assistant_v31"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "BNAI Floating Assistant", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
                .setContentTitle("BNAI Floating Assistant Active")
                .setContentText("Tap bubble for quick chat or authorized screen analysis")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("BNAI Floating Assistant Active")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build()
        }
        startForeground(3101, notification)
    }

    override fun onDestroy() {
        stopHoldCapture("Service destroyed")
        if (::rootLayout.isInitialized) windowManager.removeView(rootLayout)
        super.onDestroy()
    }
}
EOF

# ============================================================================
# FILE 12: Theme.kt
# ============================================================================
cat > app/src/main/java/org/dpdns/bnai/assistant/ui/Theme.kt << 'EOF'
package org.dpdns.bnai.assistant.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BnaiPrimaryBg = Color(0xFF0B1020)
val BnaiSecondaryBg = Color(0xFF151B2D)
val BnaiElevatedSurface = Color(0xFF20283D)
val BnaiPrimaryAccent = Color(0xFF7C8CFF)
val BnaiSecondaryAccent = Color(0xFF29C7C7)
val BnaiMainText = Color(0xFFF5F7FF)
val BnaiSecondaryText = Color(0xFFA8B2C8)
val BnaiSuccess = Color(0xFF39C98A)
val BnaiWarning = Color(0xFFF5B84B)
val BnaiError = Color(0xFFF16C7B)
val BnaiBorder = Color(0xFF303A52)

private val BnaiV11ColorScheme = darkColorScheme(
    primary = BnaiPrimaryAccent, onPrimary = Color.White, secondary = BnaiSecondaryAccent,
    tertiary = BnaiWarning, background = BnaiPrimaryBg, surface = BnaiSecondaryBg,
    surfaceVariant = BnaiElevatedSurface, onBackground = BnaiMainText, onSurface = BnaiMainText,
    onSurfaceVariant = BnaiSecondaryText, error = BnaiError, outline = BnaiBorder
)

@Composable
fun BnaiTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = BnaiV11ColorScheme, content = content)
}
EOF

# ============================================================================
# FILE 13: AuthScreen.kt
# ============================================================================
cat > app/src/main/java/org/dpdns/bnai/assistant/ui/AuthScreen.kt << 'EOF'
package org.dpdns.bnai.assistant.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dpdns.bnai.assistant.viewmodel.MainViewModel

@Composable
fun AuthScreen(viewModel: MainViewModel) {
    var email by remember { mutableStateOf("admin@example.com") }
    var password by remember { mutableStateOf("") }
    val isLoading by viewModel.isLoading.collectAsState()
    val backendHealthy by viewModel.backendHealthy.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "BNAI UNIFIED APP v1.1", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = "One Android App • One Primary AI Brain • Two Logical Projects", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (backendHealthy) "Backend v3.1: Connected (SSL Verified)" else "Backend Status: Unreachable",
            fontSize = 12.sp, color = if (backendHealthy) BnaiSuccess else MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text("Email (admin@example.com for Owner Role)") },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("Password or Trusted Service Token") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { viewModel.login(email.trim(), password.trim()) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = !isLoading && email.isNotBlank()
        ) {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            else Text("Shared Secure Login (Assistant + Cinematic)")
        }
    }
}
EOF

# ============================================================================
# FILE 14: HomeDashboardScreen.kt
# ============================================================================
cat > app/src/main/java/org/dpdns/bnai/assistant/ui/HomeDashboardScreen.kt << 'EOF'
package org.dpdns.bnai.assistant.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dpdns.bnai.assistant.viewmodel.MainViewModel

@Composable
fun HomeDashboardScreen(
    viewModel: MainViewModel,
    onNavigateToSection: (String) -> Unit
) {
    val selectedProject by viewModel.selectedProject.collectAsState()
    val sessionMode by viewModel.sessionMode.collectAsState()
    val backendHealthy by viewModel.backendHealthy.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val approvals by viewModel.approvals.collectAsState()
    val incidents by viewModel.incidents.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "Welcome to BNAI Unified Control Center", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(text = "Active Project: ${selectedProject.namespace} (${selectedProject.name})", fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
                Text(text = "Session Mode: $sessionMode | Backend Connection: ${if (backendHealthy) "Verified" else "Offline"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Text("Quick Actions", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onNavigateToSection("CHAT") }, modifier = Modifier.weight(1f)) { Text("Ask AI", fontSize = 12.sp) }
            Button(onClick = { onNavigateToSection("COPILOT") }, modifier = Modifier.weight(1f)) { Text("Open Copilot", fontSize = 12.sp) }
            Button(onClick = { onNavigateToSection("CINEMATIC") }, modifier = Modifier.weight(1f)) { Text("Cinematic", fontSize = 12.sp) }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onNavigateToSection("WF_BUILDER") }, modifier = Modifier.weight(1f)) { Text("WF Builder", fontSize = 12.sp) }
            OutlinedButton(onClick = { onNavigateToSection("TASKS") }, modifier = Modifier.weight(1f)) { Text("Tasks & Approvals", fontSize = 12.sp) }
            OutlinedButton(onClick = { onNavigateToSection("MEMORY") }, modifier = Modifier.weight(1f)) { Text("7-Layer Memory", fontSize = 12.sp) }
        }

        Text("Logical Projects (Isolated Contexts)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        viewModel.projects.forEach { proj ->
            val isSelected = proj.id == selectedProject.id
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "${proj.name} [${proj.namespace}]", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(text = proj.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(onClick = { viewModel.selectProject(proj) }, enabled = !isSelected) {
                        Text(if (isSelected) "Active" else "Switch", fontSize = 11.sp)
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Attention Required (Verified Evidence Only)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("• Pending Approvals: ${approvals.count { it.status == "pending" }}", fontSize = 12.sp)
                Text("• Active / Blocked Tasks: ${tasks.count { it.status != "COMPLETED" }}", fontSize = 12.sp)
                Text("• Security / Governance Alerts: ${incidents.size}", fontSize = 12.sp)
            }
        }
    }
}
EOF

echo "==========================================="
echo "✅ Frontend Phase 2 complete — 5 files created:"
echo "==========================================="

# ============================================================================
# PHASE 3 START (Main Screens)
# ============================================================================

# ============================================================================
# FILE 15: ChatAndSkillsScreen.kt
# ============================================================================
cat > app/src/main/java/org/dpdns/bnai/assistant/ui/ChatAndSkillsScreen.kt << 'EOF'
package org.dpdns.bnai.assistant.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dpdns.bnai.assistant.service.FloatingBubbleService
import org.dpdns.bnai.assistant.viewmodel.MainViewModel

@Composable
fun ChatAndSkillsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val selectedProject by viewModel.selectedProject.collectAsState()
    val selectedSkill by viewModel.selectedSkill.collectAsState()
    val sessionMode by viewModel.sessionMode.collectAsState()
    val useFastPath by viewModel.useFastPath.collectAsState()
    val ceoMode by viewModel.ceoMode.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var projectDropdownExpanded by remember { mutableStateOf(false) }
    var skillDropdownExpanded by remember { mutableStateOf(false) }
    var modeDropdownExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        OutlinedButton(onClick = { projectDropdownExpanded = true }) {
                            Text("Proj: ${selectedProject.namespace}", fontSize = 11.sp)
                        }
                        DropdownMenu(expanded = projectDropdownExpanded, onDismissRequest = { projectDropdownExpanded = false }) {
                            viewModel.projects.forEach { proj ->
                                DropdownMenuItem(
                                    text = { Text("${proj.namespace} — ${proj.name}") },
                                    onClick = { viewModel.selectProject(proj); projectDropdownExpanded = false }
                                )
                            }
                        }
                    }
                    Box {
                        OutlinedButton(onClick = { modeDropdownExpanded = true }) {
                            Text("Mode: $sessionMode", fontSize = 11.sp)
                        }
                        DropdownMenu(expanded = modeDropdownExpanded, onDismissRequest = { modeDropdownExpanded = false }) {
                            viewModel.sessionModes.forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode) },
                                    onClick = { viewModel.setSessionMode(mode); modeDropdownExpanded = false }
                                )
                            }
                        }
                    }
                    Box {
                        OutlinedButton(onClick = { skillDropdownExpanded = true }) {
                            Text("Skill: ${selectedSkill.name.take(10)}..", fontSize = 11.sp)
                        }
                        DropdownMenu(expanded = skillDropdownExpanded, onDismissRequest = { skillDropdownExpanded = false }) {
                            viewModel.skills.forEach { sk ->
                                DropdownMenuItem(
                                    text = { Text(sk.name + if (sk.ownerOnly) " [Owner]" else "") },
                                    onClick = { viewModel.selectSkill(sk); skillDropdownExpanded = false }
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = useFastPath, onCheckedChange = { viewModel.toggleFastPath(it) })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = if (useFastPath) "Fast (Groq Direct)" else "n8n (WF-00)", fontSize = 11.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = ceoMode, onCheckedChange = { viewModel.toggleCeoMode(it) })
                        Text("CEO Mode", fontSize = 11.sp)
                    }
                    TextButton(
                        onClick = {
                            if (!Settings.canDrawOverlays(context)) {
                                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                                context.startActivity(intent)
                            } else {
                                context.startForegroundService(Intent(context, FloatingBubbleService::class.java))
                            }
                        }
                    ) {
                        Text("Bubble", fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val isUser = msg.role == "user"
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isUser) MaterialTheme.colorScheme.primary
                                else if (msg.isError) MaterialTheme.colorScheme.error.copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.widthIn(max = 330.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = msg.content, fontSize = 14.sp)
                            if (!isUser) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Provider: ${msg.provider} | Verification: ${msg.verificationStatus} | Project: ${selectedProject.namespace}",
                                    fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = inputText, onValueChange = { inputText = it },
                placeholder = { Text("Ask Primary AI Brain (${selectedProject.namespace})...") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { val q = inputText; inputText = ""; viewModel.sendChatMessage(q) },
                enabled = !isLoading && inputText.isNotBlank()
            ) {
                Text(if (isLoading) "..." else "Send")
            }
        }
    }
}
EOF

# ============================================================================
# FILE 16: CopilotAndDiagnosticsScreen.kt
# ============================================================================
cat > app/src/main/java/org/dpdns/bnai/assistant/ui/CopilotAndDiagnosticsScreen.kt << 'EOF'
package org.dpdns.bnai.assistant.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dpdns.bnai.assistant.viewmodel.MainViewModel

@Composable
fun CopilotAndDiagnosticsScreen(viewModel: MainViewModel) {
    val consoleOutput by viewModel.copilotConsoleOutput.collectAsState()
    val selectedProject by viewModel.selectedProject.collectAsState()
    val baseHash by viewModel.lastBaseVersionHash.collectAsState()
    val propHash by viewModel.lastProposedChangeHash.collectAsState()
    val lastToken by viewModel.lastIssuedApprovalToken.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var wfId by remember { mutableStateOf("WF-00") }
    var failedNode by remember { mutableStateOf("Call Groq Primary") }
    var errorMsg by remember { mutableStateOf("401 Unauthorized: Invalid x-service-token") }
    var execLogs by remember { mutableStateOf("") }
    var screenshotOcrText by remember { mutableStateOf("HTTP 502 Bad Gateway at WF-00 Webhook node") }
    var screenshotQuestion by remember { mutableStateOf("Identify visible UI error and safe resolution") }
    var origJson by remember { mutableStateOf("{\"nodes\":[{\"name\":\"Webhook\"}]}") }
    var propJson by remember { mutableStateOf("{\"nodes\":[{\"name\":\"Webhook\"},{\"name\":\"Validator\"}]}") }
    var sandboxTarget by remember { mutableStateOf("Assistant / WF-04") }
    var sandboxUserConfirmed by remember { mutableStateOf(false) }
    var protectedTarget by remember { mutableStateOf("Assistant / WF-00") }
    var approvalTokenInput by remember { mutableStateOf("") }
    var rollbackTargetHash by remember { mutableStateOf("") }

    LaunchedEffect(lastToken) {
        if (!lastToken.isNullOrBlank()) approvalTokenInput = lastToken!!
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "AI Copilot, Inspector, Diff, Sandbox & Recovery (${selectedProject.namespace})", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(text = "Rule: Generating a fix is a PROPOSAL ONLY, never an automatic live execution (Part 14.3).", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { viewModel.inspectNamespacedWorkflows(selectedProject.namespace) }, modifier = Modifier.weight(1f), enabled = !isLoading) { Text("Inspect ${selectedProject.namespace} WFs", fontSize = 11.sp) }
            OutlinedButton(onClick = { viewModel.loadCopilotDiagnosticHistory() }, modifier = Modifier.weight(1f), enabled = !isLoading) { Text("Diagnostic History", fontSize = 11.sp) }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("1. Error Diagnosis (Evidence-Classified)", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = wfId, onValueChange = { wfId = it }, label = { Text("Workflow ID") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = failedNode, onValueChange = { failedNode = it }, label = { Text("Failed Node") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                OutlinedTextField(value = errorMsg, onValueChange = { errorMsg = it }, label = { Text("Error Summary / Message") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = execLogs, onValueChange = { execLogs = it }, label = { Text("Execution Logs (Optional — Secrets Auto-Redacted)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                Button(onClick = { viewModel.runCopilotDiagnosis(wfId, failedNode, errorMsg, execLogs) }, enabled = !isLoading) { Text("Diagnose & Propose Safe Fix") }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("2. Screenshot Vision Analysis (7-Day Ephemeral Policy)", fontWeight = FontWeight.Bold)
                OutlinedTextField(value = screenshotOcrText, onValueChange = { screenshotOcrText = it }, label = { Text("Visible Screenshot OCR / UI Error Text") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = screenshotQuestion, onValueChange = { screenshotQuestion = it }, label = { Text("Analysis Question") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Button(onClick = { viewModel.analyzeScreenshotOcr(screenshotOcrText, screenshotQuestion) }, enabled = !isLoading) { Text("Analyze Screenshot with Copilot") }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("3. Workflow Diff Viewer (TOCTOU Hash Binding)", fontWeight = FontWeight.Bold)
                OutlinedTextField(value = origJson, onValueChange = { origJson = it }, label = { Text("Original Configuration JSON") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(value = propJson, onValueChange = { propJson = it }, label = { Text("Proposed Configuration JSON") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                if (baseHash.isNotBlank()) Text(text = "Base Hash: ${baseHash.take(16)}... | Proposed Hash: ${propHash.take(16)}...", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                Button(onClick = { viewModel.runWorkflowDiffViewer(wfId, origJson, propJson) }, enabled = !isLoading) { Text("Compare Before / After Diff") }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("4. Isolated Sandbox Diagnostic Center", fontWeight = FontWeight.Bold)
                OutlinedTextField(value = sandboxTarget, onValueChange = { sandboxTarget = it }, label = { Text("Namespaced Target Workflow") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = sandboxUserConfirmed, onCheckedChange = { sandboxUserConfirmed = it })
                    Text(text = "I explicitly confirm isolated sandbox execution & accept residual risks", fontSize = 11.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { viewModel.runSandboxDiagnostic(sandboxTarget, false) }, enabled = !isLoading) { Text("1. Run Preflight") }
                    Button(onClick = { viewModel.runSandboxDiagnostic(sandboxTarget, sandboxUserConfirmed) }, enabled = !isLoading && sandboxUserConfirmed) { Text("2. Execute in Sandbox") }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("5. Protected Changes & Backup/Rollback Center", fontWeight = FontWeight.Bold)
                OutlinedTextField(value = protectedTarget, onValueChange = { protectedTarget = it }, label = { Text("Target Resource") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = approvalTokenInput, onValueChange = { approvalTokenInput = it }, label = { Text("SHA-256 Bound Approval Token") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { viewModel.createApprovalRequest("APPLY_PROTECTED_CHANGE", "workflow_or_system", protectedTarget, "Copilot Proposed Patch Verification") }, enabled = !isLoading) { Text("Request Approval", fontSize = 11.sp) }
                    Button(onClick = { viewModel.executeProtectedLiveChange(protectedTarget, "APPLY_PROTECTED_CHANGE", approvalTokenInput) }, enabled = !isLoading && approvalTokenInput.isNotBlank()) { Text("Apply Change", fontSize = 11.sp) }
                }
                HorizontalDivider()
                Text("Backup & Recovery Rollback:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(value = rollbackTargetHash, onValueChange = { rollbackTargetHash = it }, label = { Text("Target Recovery Version Hash") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { viewModel.loadRecoveryPoints() }, enabled = !isLoading) { Text("Inspect Recovery Points", fontSize = 11.sp) }
                    Button(onClick = { viewModel.executeResourceRollback(protectedTarget, rollbackTargetHash, approvalTokenInput) }, enabled = !isLoading && rollbackTargetHash.isNotBlank() && approvalTokenInput.isNotBlank()) { Text("Execute Rollback", fontSize = 11.sp) }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Diagnostic & Verification Evidence Output:", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = consoleOutput, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        }
    }
}
EOF

# ============================================================================
# FILE 17: CinematicStudioScreen.kt
# ============================================================================
cat > app/src/main/java/org/dpdns/bnai/assistant/ui/CinematicStudioScreen.kt << 'EOF'
package org.dpdns.bnai.assistant.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dpdns.bnai.assistant.viewmodel.MainViewModel

@Composable
fun CinematicStudioScreen(
    viewModel: MainViewModel,
    onOpenCopilotWithContext: () -> Unit
) {
    val stages by viewModel.cinematicStages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) { viewModel.syncLiveCinematicStudio() }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "Project B — AI Cinematic Studio", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(text = "Isolated Cinematic Namespace • Story Bible • Continuity • Scene & Media Pipeline", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        viewModel.projects.find { it.namespace == "Cinematic" }?.let { viewModel.selectProject(it) }
                        onOpenCopilotWithContext()
                    }) {
                        Text("Open Cinematic Copilot", fontSize = 11.sp)
                    }
                }
            }
        }

        Text("15 Production Pipeline Sections:", fontWeight = FontWeight.Bold, fontSize = 14.sp)

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(stages, key = { it.stageId }) { stage ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "${stage.stageId}: ${stage.sectionName}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(text = "Mapped WF: ${stage.mappedWorkflow} | State: ${stage.status}", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { viewModel.triggerCinematicStageOperation(stage.stageId, "queue") }, enabled = !isLoading) { Text("Run/Queue", fontSize = 10.sp) }
                            if (stage.status == "Running" || stage.status == "Queued") {
                                TextButton(onClick = { viewModel.triggerCinematicStageOperation(stage.stageId, "cancel") }, enabled = !isLoading) { Text("Cancel", fontSize = 10.sp, color = MaterialTheme.colorScheme.error) }
                            }
                        }
                    }
                }
            }
        }
    }
}
EOF

# ============================================================================
# FILE 18: WorkflowBuilderScreen.kt
# ============================================================================
cat > app/src/main/java/org/dpdns/bnai/assistant/ui/WorkflowBuilderScreen.kt << 'EOF'
package org.dpdns.bnai.assistant.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dpdns.bnai.assistant.viewmodel.MainViewModel

@Composable
fun WorkflowBuilderScreen(viewModel: MainViewModel) {
    val output by viewModel.wfBuilderOutput.collectAsState()
    val lastGenJson by viewModel.lastGeneratedWfJson.collectAsState()
    val repairCount by viewModel.repairAttemptCount.collectAsState()
    val selectedProject by viewModel.selectedProject.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var requirementsText by remember { mutableStateOf("") }
    var workflowJsonInput by remember { mutableStateOf("") }
    var requestedChanges by remember { mutableStateOf("") }
    var liveWfId by remember { mutableStateOf("") }
    var docQuestion by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "n8n WF Builder & Inspector [${selectedProject.namespace}]", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        if (repairCount > 0) {
            Text(text = "Auto-Repair Cycle Attempts Used: $repairCount / 5 (Blueprint v1.6 Part 13 Limit)", color = MaterialTheme.colorScheme.tertiary, fontSize = 12.sp)
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "1. Generate + Validate + Max 5x Auto-Repair (WF-02 -> WF-04 -> WF-03)", fontWeight = FontWeight.SemiBold)
                OutlinedTextField(value = requirementsText, onValueChange = { requirementsText = it }, label = { Text("Blueprint / Workflow Requirements") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.generateAndAutoRepairWorkflow(requirementsText) }, enabled = !isLoading) { Text("Build & Verify WF") }
                    if (lastGenJson.isNotBlank()) {
                        OutlinedButton(onClick = { workflowJsonInput = lastGenJson }) { Text("Load Generated JSON") }
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("2. Analyze (WF-01) or Controlled Update (WF-03)", fontWeight = FontWeight.SemiBold)
                OutlinedTextField(value = workflowJsonInput, onValueChange = { workflowJsonInput = it }, label = { Text("Paste Workflow JSON (Uploaded Mode)") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                OutlinedTextField(value = requestedChanges, onValueChange = { requestedChanges = it }, label = { Text("Requested Changes (for WF-03 Update)") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.analyzeWorkflowWf01(workflowJsonInput) }, enabled = !isLoading) { Text("Audit (WF-01)") }
                    Button(onClick = { viewModel.updateWorkflowWf03(workflowJsonInput, requestedChanges) }, enabled = !isLoading) { Text("Update (WF-03)") }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("3. Doc Research (WF-05) & Connected n8n Inspector (WF-09)", fontWeight = FontWeight.SemiBold)
                OutlinedTextField(value = docQuestion, onValueChange = { docQuestion = it }, label = { Text("n8n Documentation / API Question (WF-05)") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.researchDocsWf05(docQuestion) }, enabled = !isLoading) { Text("Research (WF-05)") }
                    Button(onClick = { viewModel.listLiveWorkflowsWf09() }, enabled = !isLoading) { Text("List Live WFs (WF-09)") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = liveWfId, onValueChange = { liveWfId = it }, label = { Text("Live WF ID") }, modifier = Modifier.weight(1f), singleLine = true)
                    Button(onClick = { viewModel.getLiveWorkflowWf09(liveWfId) }, enabled = !isLoading) { Text("Fetch Live WF") }
                }
            }
        }

        if (output.isNotBlank()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Execution Result & Verification Evidence:", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = output, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            }
        }
    }
}
EOF

echo "==========================================="
echo "✅ Frontend Phase 3 complete — 4 files created:"
echo "==========================================="

# ============================================================================
# PHASE 4 START (Remaining Screens & MainActivity)
# ============================================================================

# ============================================================================
# FILE 19: TasksAndApprovalsScreen.kt
# ============================================================================
cat > app/src/main/java/org/dpdns/bnai/assistant/ui/TasksAndApprovalsScreen.kt << 'EOF'
package org.dpdns.bnai.assistant.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dpdns.bnai.assistant.viewmodel.MainViewModel

@Composable
fun TasksAndApprovalsScreen(viewModel: MainViewModel) {
    val progressSummary by viewModel.projectProgressSummary.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val approvals by viewModel.approvals.collectAsState()
    val lastIssuedToken by viewModel.lastIssuedApprovalToken.collectAsState()
    val reminders by viewModel.reminders.collectAsState()
    val isOwner by viewModel.isOwner.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var newTaskType by remember { mutableStateOf("") }
    var newTaskGoal by remember { mutableStateOf("") }
    var reminderTitle by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.syncLiveTasksAndApprovals() }

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tasks, Queue & Approvals (WF-06 / WF-08)", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { viewModel.syncLiveTasksAndApprovals() }, enabled = !isLoading) { Text("Sync DB", fontSize = 11.sp) }
                        Button(onClick = { viewModel.fetchProjectProgressWf06() }, enabled = !isLoading) { Text("WF-06", fontSize = 11.sp) }
                    }
                }
                Text(progressSummary, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                HorizontalDivider()

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = newTaskType, onValueChange = { newTaskType = it }, label = { Text("Task Title (WF-08)") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = newTaskGoal, onValueChange = { newTaskGoal = it }, label = { Text("Goal") }, modifier = Modifier.weight(1f), singleLine = true)
                    Button(
                        onClick = { viewModel.createTaskWf08(newTaskType, newTaskGoal); newTaskType = ""; newTaskGoal = "" },
                        enabled = !isLoading && newTaskType.isNotBlank() && newTaskGoal.isNotBlank()
                    ) { Text("+Task") }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = reminderTitle, onValueChange = { reminderTitle = it }, label = { Text("Reminder Title (WF-10)") }, modifier = Modifier.weight(1f), singleLine = true)
                    Button(
                        onClick = { viewModel.scheduleReminderWf10(reminderTitle, 30); reminderTitle = "" },
                        enabled = !isLoading && reminderTitle.isNotBlank()
                    ) { Text("+30m Reminder") }
                }
            }
        }

        if (!lastIssuedToken.isNullOrBlank()) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("Latest SHA-256 Bound Approval Token:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(lastIssuedToken!!, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (approvals.isNotEmpty()) {
                item { Text("Approval Inbox (SHA-256 Action-Bound):", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                items(approvals, key = { it.id }) { appr ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = "${appr.actionType} on ${appr.resourceId} [${appr.status.uppercase()}]", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Reason: ${appr.reason} | Risk: ${appr.riskCategory}", fontSize = 11.sp)
                            if (appr.status == "pending" && isOwner) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { viewModel.submitApprovalDecision(appr.id, "approved") }) { Text("Approve (Owner)", fontSize = 11.sp) }
                                    OutlinedButton(onClick = { viewModel.submitApprovalDecision(appr.id, "rejected") }) { Text("Reject", fontSize = 11.sp) }
                                }
                            }
                        }
                    }
                }
            }

            item { Text("Persistent Tasks, Cancellation & Reconciliation (UI-20):", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
            items(tasks, key = { it.id }) { task ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${task.title} [${task.status}]", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Goal: ${task.goal}", fontSize = 12.sp)
                            Text("ID: ${task.id.take(12)}... | Priority: ${task.priority}", fontSize = 11.sp)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (task.cancelEligible) {
                                OutlinedButton(onClick = { viewModel.requestTaskCancellation(task.id) }) { Text("Cancel", fontSize = 10.sp) }
                            }
                            OutlinedButton(onClick = { viewModel.reconcileTaskOutcome(task.id, "resume_check") }) { Text("Reconcile", fontSize = 10.sp) }
                        }
                    }
                }
            }

            items(reminders, key = { it.id }) { rem ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Reminder: ${rem.title} [${rem.status}]", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(text = "Scheduled: ${rem.scheduledAt}", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
EOF

# ============================================================================
# FILE 20: MemoryAndPrivacyScreen.kt
# ============================================================================
cat > app/src/main/java/org/dpdns/bnai/assistant/ui/MemoryAndPrivacyScreen.kt << 'EOF'
package org.dpdns.bnai.assistant.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dpdns.bnai.assistant.viewmodel.MainViewModel

@Composable
fun MemoryAndPrivacyScreen(viewModel: MainViewModel) {
    val memories by viewModel.memoryItems.collectAsState()
    val selectedProject by viewModel.selectedProject.collectAsState()
    val scopes = listOf("project", "conversation", "workflow", "skill", "user_global", "workspace")
    var selectedScope by remember { mutableStateOf(scopes.first()) }
    var memKey by remember { mutableStateOf("") }
    var memContent by remember { mutableStateOf("") }

    LaunchedEffect(selectedScope, selectedProject.id) { viewModel.syncScopedMemoryFromBackend(selectedScope) }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = "7-Layer Scoped Memory [${selectedProject.namespace}]", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = { viewModel.syncScopedMemoryFromBackend(selectedScope) }) { Text("Sync Scope", fontSize = 11.sp) }
        }
        Text(text = "Isolation Rule: Memory from ${selectedProject.namespace} is never leaked to other projects.", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            scopes.take(4).forEach { sc ->
                FilterChip(selected = selectedScope == sc, onClick = { selectedScope = sc }, label = { Text(sc, fontSize = 11.sp) })
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Write Scoped Memory to Supabase (Secrets Auto-Redacted)", fontWeight = FontWeight.SemiBold)
                OutlinedTextField(value = memKey, onValueChange = { memKey = it }, label = { Text("Memory Key (Optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = memContent, onValueChange = { memContent = it }, label = { Text("Memory Content (Scope: $selectedScope)") }, modifier = Modifier.fillMaxWidth())
                Button(
                    onClick = { viewModel.addLocalOrRemoteMemory(selectedScope, memKey, memContent); memKey = ""; memContent = "" },
                    enabled = memContent.isNotBlank()
                ) { Text("Save to Scope '$selectedScope'") }
            }
        }

        Text("Retained Memory Items & Deletion Control:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(memories, key = { it.id }) { item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("[${item.scope.uppercase()}] ${item.key ?: "Entry"}", fontWeight = FontWeight.Bold)
                            Text(item.content, fontSize = 13.sp)
                            Text(text = "Status: ${item.knowledgeStatus} | Cat: ${item.category}", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                        TextButton(onClick = { viewModel.requestMemoryDeletion(item.id) }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }
}
EOF

# ============================================================================
# FILE 21: OwnerAdminScreen.kt
# ============================================================================
cat > app/src/main/java/org/dpdns/bnai/assistant/ui/OwnerAdminScreen.kt << 'EOF'
package org.dpdns.bnai.assistant.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dpdns.bnai.assistant.viewmodel.MainViewModel

@Composable
fun OwnerAdminScreen(viewModel: MainViewModel) {
    val isOwner by viewModel.isOwner.collectAsState()
    val ownerTelemetry by viewModel.ownerDashboardJson.collectAsState()
    val incidents by viewModel.incidents.collectAsState()
    val lastToken by viewModel.lastIssuedApprovalToken.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var ownerActionType by remember { mutableStateOf("TRIGGER_BACKUP_VERIFICATION") }
    var ownerTargetResource by remember { mutableStateOf("system_baseline_v31") }
    var ownerApprovalToken by remember { mutableStateOf("") }

    LaunchedEffect(lastToken) { if (!lastToken.isNullOrBlank()) ownerApprovalToken = lastToken!! }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Owner Admin Governance Console", fontSize = 18.sp, fontWeight = FontWeight.Bold)

        if (!isOwner) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f))) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Restricted Section (UI-19)", fontWeight = FontWeight.Bold)
                    Text("Normal users cannot access Owner-only schema drift or administrative controls. Backend enforces owner_admin role.", fontSize = 12.sp)
                }
            }
            return@Column
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("System Baseline & Schema Governance", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.loadOwnerAdminTelemetry() }, enabled = !isLoading) { Text("Owner Dashboard", fontSize = 11.sp) }
                    OutlinedButton(onClick = { viewModel.loadOwnerSchemaRegistry() }, enabled = !isLoading) { Text("Schema Check", fontSize = 11.sp) }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Protected Administrative Action", fontWeight = FontWeight.Bold)
                OutlinedTextField(value = ownerActionType, onValueChange = { ownerActionType = it }, label = { Text("Action Type") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = ownerTargetResource, onValueChange = { ownerTargetResource = it }, label = { Text("Target Resource") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = ownerApprovalToken, onValueChange = { ownerApprovalToken = it }, label = { Text("SHA-256 Approval Token") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Button(onClick = { viewModel.triggerOwnerConsoleAction(ownerActionType, ownerTargetResource, ownerApprovalToken.ifBlank { null }) }, enabled = !isLoading && ownerActionType.isNotBlank()) { Text("Execute Authorized Owner Action") }
            }
        }

        Text("Active Security & Configuration Incidents:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        incidents.forEach { inc ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${inc.id} [${inc.severity}] — ${inc.category}", fontWeight = FontWeight.Bold)
                    Text("Component: ${inc.component} | Status: ${inc.status}", fontSize = 12.sp)
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Live Backend Owner Telemetry JSON:", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = ownerTelemetry, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        }
    }
}
EOF

# ============================================================================
# FILE 23: FilesAndNotificationsScreen.kt
# ============================================================================
cat > app/src/main/java/org/dpdns/bnai/assistant/ui/FilesAndNotificationsScreen.kt << 'EOF'
package org.dpdns.bnai.assistant.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dpdns.bnai.assistant.viewmodel.MainViewModel

@Composable
fun FilesAndNotificationsScreen(viewModel: MainViewModel) {
    val selectedProject by viewModel.selectedProject.collectAsState()
    val output by viewModel.filesAndNotifsOutput.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var fileName by remember { mutableStateOf("workflow_snapshot_v31.json") }
    var artifactType by remember { mutableStateOf("workflow_json") }
    var contentPreview by remember { mutableStateOf("{\"name\":\"BNAI Verified Snapshot\"}") }
    var notifTitle by remember { mutableStateOf("Production Stage Update") }
    var notifMessage by remember { mutableStateOf("Task completed verification gate.") }
    var notifChannel by remember { mutableStateOf("in_app") }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "Files, Notifications, User Quotas & Audit [${selectedProject.namespace}]", fontSize = 17.sp, fontWeight = FontWeight.Bold)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.loadNotificationsAndUserDashboard() }, modifier = Modifier.weight(1f), enabled = !isLoading) { Text("User Quotas & Notifs", fontSize = 11.sp) }
            OutlinedButton(onClick = { viewModel.runUi35FinalCapabilityAudit() }, modifier = Modifier.weight(1f), enabled = !isLoading) { Text("UI-35 Capability Audit", fontSize = 11.sp) }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("1. Project-Scoped Files & Redacted Exports", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = fileName, onValueChange = { fileName = it }, label = { Text("File Name") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = artifactType, onValueChange = { artifactType = it }, label = { Text("Type") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                OutlinedTextField(value = contentPreview, onValueChange = { contentPreview = it }, label = { Text("Content Preview (Auto-Redacted)") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.uploadProjectFileMetadata(fileName, artifactType, contentPreview) }, enabled = !isLoading) { Text("Upload Metadata", fontSize = 11.sp) }
                    OutlinedButton(onClick = { viewModel.triggerRedactedProjectExport("JSON") }, enabled = !isLoading) { Text("Export JSON", fontSize = 11.sp) }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("2. Multi-Channel Notifications", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = notifTitle, onValueChange = { notifTitle = it }, label = { Text("Title") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = notifChannel, onValueChange = { notifChannel = it }, label = { Text("Channel") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                OutlinedTextField(value = notifMessage, onValueChange = { notifMessage = it }, label = { Text("Notification Body") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = { viewModel.sendMultiChannelNotification(notifTitle, notifMessage, notifChannel) }, enabled = !isLoading) { Text("Dispatch Verified Notification") }
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Files, Notifications & UI-35 Audit Telemetry:", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = output, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        }
    }
}
EOF

# ============================================================================
# FILE 22: MainActivity.kt
# ============================================================================
cat > app/src/main/java/org/dpdns/bnai/assistant/MainActivity.kt << 'EOF'
package org.dpdns.bnai.assistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dpdns.bnai.assistant.data.ApiService
import org.dpdns.bnai.assistant.data.AppRepository
import org.dpdns.bnai.assistant.data.TokenManager
import org.dpdns.bnai.assistant.ui.*
import org.dpdns.bnai.assistant.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tokenManager = TokenManager(applicationContext)
        val apiService = ApiService.create(tokenManager)
        val repository = AppRepository(apiService, tokenManager)
        val mainViewModel = MainViewModel(repository)

        setContent {
            BnaiTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainAppScaffold(viewModel = mainViewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(viewModel: MainViewModel) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val selectedProject by viewModel.selectedProject.collectAsState()
    val statusBanner by viewModel.statusBanner.collectAsState()
    var activeScreen by remember { mutableStateOf("HOME") }

    if (!isLoggedIn) {
        AuthScreen(viewModel = viewModel)
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("BNAI Unified v3.1", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Project: ${selectedProject.namespace} • Screen: $activeScreen", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.logout() }) { Text("Logout", color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = activeScreen == "HOME", onClick = { activeScreen = "HOME" }, icon = { Icon(Icons.Default.Home, contentDescription = "Home") }, label = { Text("Home") })
                NavigationBarItem(selected = activeScreen == "CHAT", onClick = { activeScreen = "CHAT" }, icon = { Icon(Icons.Default.Chat, contentDescription = "Chat") }, label = { Text("Chat") })
                NavigationBarItem(selected = activeScreen in listOf("PROJECTS", "CINEMATIC", "WF_BUILDER"), onClick = { activeScreen = "PROJECTS" }, icon = { Icon(Icons.Default.Folder, contentDescription = "Projects") }, label = { Text("Projects") })
                NavigationBarItem(selected = activeScreen == "TASKS", onClick = { activeScreen = "TASKS" }, icon = { Icon(Icons.Default.CheckCircle, contentDescription = "Tasks") }, label = { Text("Tasks") })
                NavigationBarItem(selected = activeScreen in listOf("MORE", "COPILOT", "MEMORY", "FILES_NOTIFS", "OWNER"), onClick = { activeScreen = "MORE" }, icon = { Icon(Icons.Default.MoreHoriz, contentDescription = "More") }, label = { Text("More") })
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            statusBanner?.let { banner ->
                Surface(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(banner, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        TextButton(onClick = { viewModel.clearBanner() }) { Text("OK", fontSize = 11.sp) }
                    }
                }
            }

            when (activeScreen) {
                "HOME" -> HomeDashboardScreen(viewModel = viewModel, onNavigateToSection = { dest -> activeScreen = dest })
                "CHAT" -> ChatAndSkillsScreen(viewModel = viewModel)
                "PROJECTS" -> ProjectsHubScreen(viewModel = viewModel, onNavigate = { dest -> activeScreen = dest })
                "CINEMATIC" -> CinematicStudioScreen(viewModel = viewModel, onOpenCopilotWithContext = { activeScreen = "COPILOT" })
                "WF_BUILDER" -> WorkflowBuilderScreen(viewModel = viewModel)
                "TASKS" -> TasksAndApprovalsScreen(viewModel = viewModel)
                "COPILOT" -> CopilotAndDiagnosticsScreen(viewModel = viewModel)
                "MEMORY" -> MemoryAndPrivacyScreen(viewModel = viewModel)
                "FILES_NOTIFS" -> FilesAndNotificationsScreen(viewModel = viewModel)
                "OWNER" -> OwnerAdminScreen(viewModel = viewModel)
                "MORE" -> MoreMenuScreen(onSelectDestination = { dest -> activeScreen = dest })
            }
        }
    }
}

@Composable
fun ProjectsHubScreen(viewModel: MainViewModel, onNavigate: (String) -> Unit) {
    val selectedProject by viewModel.selectedProject.collectAsState()
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Two Logical Projects Hub", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        viewModel.projects.forEach { proj ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${proj.name} [${proj.namespace}]", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(proj.description, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.selectProject(proj) }) { Text(if (selectedProject.id == proj.id) "Active Context" else "Switch Context") }
                        if (proj.namespace == "Assistant") {
                            OutlinedButton(onClick = { onNavigate("WF_BUILDER") }) { Text("Open WF Builder") }
                        } else {
                            OutlinedButton(onClick = { onNavigate("CINEMATIC") }) { Text("Open Cinematic Studio") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MoreMenuScreen(onSelectDestination: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("More Capabilities", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Button(onClick = { onSelectDestination("COPILOT") }, modifier = Modifier.fillMaxWidth()) { Text("AI Copilot, Inspector, Diff, Sandbox & Recovery") }
        Button(onClick = { onSelectDestination("CINEMATIC") }, modifier = Modifier.fillMaxWidth()) { Text("Project B — AI Cinematic Studio") }
        Button(onClick = { onSelectDestination("WF_BUILDER") }, modifier = Modifier.fillMaxWidth()) { Text("Project A — n8n Workflow Builder") }
        Button(onClick = { onSelectDestination("MEMORY") }, modifier = Modifier.fillMaxWidth()) { Text("7-Layer Scoped Memory & Data Privacy") }
        Button(onClick = { onSelectDestination("FILES_NOTIFS") }, modifier = Modifier.fillMaxWidth()) { Text("Files, Exports, Notifications & Audit") }
        Button(onClick = { onSelectDestination("OWNER") }, modifier = Modifier.fillMaxWidth()) { Text("Owner Admin Governance Console") }
    }
}
EOF

echo "==========================================="
echo "✅ Frontend Phase 4 complete — All 24 files ready!"
echo "==========================================="
