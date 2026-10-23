package org.dpdns.bnai.assistant.data

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.dpdns.bnai.assistant.BuildConfig
import java.util.UUID

class AppRepository(private val api: ApiService, val tokenManager: TokenManager) {
    private fun newRequestId(): String = UUID.randomUUID().toString()
    
    private fun parseError(errBody: String?, defaultMsg: String): String {
        if (errBody == null) return defaultMsg
        return try { 
            val json = JsonParser.parseString(errBody).asJsonObject
            val errors = json.getAsJsonArray("errors")
            if (errors != null && errors.size() > 0) errors[0].asJsonObject.get("message")?.asString ?: defaultMsg else defaultMsg 
        } catch (e: Exception) { defaultMsg }
    }

    fun streamFastChat(message: String, projectId: String?, convId: String?, skillKey: String, ceoMode: Boolean, fileContext: String?): Flow<String> = callbackFlow {
        val client = OkHttpClient.Builder().build()
        val json = JsonObject().apply { 
            addProperty("message", message)
            addProperty("skill_key", skillKey)
            addProperty("ceo_mode", ceoMode)
            projectId?.let { addProperty("project_id", it) }
            convId?.let { addProperty("conversation_id", it) }
            fileContext?.let { addProperty("file_context", it) } 
        }
        
        val request = Request.Builder()
            .url("${BuildConfig.BASE_API_URL}api/v1/ai/chat/stream")
            .addHeader("Authorization", "Bearer ${tokenManager.getToken()}")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()
            
        val call = client.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) { close(e) }
            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) { close(Exception("HTTP ${response.code}")); return }
                val source = response.body?.source() ?: return
                try { 
                    while (!source.exhausted()) { 
                        val line = source.readUtf8Line() ?: break
                        if (line.startsWith("data: ") && line != "data: [DONE]") { 
                            try { 
                                val chunk = JsonParser.parseString(line.substring(6).trim()).asJsonObject
                                chunk.getAsJsonArray("choices")?.get(0)?.asJsonObject?.getAsJsonObject("delta")?.get("content")?.asString?.let { trySend(it) } 
                            } catch (e: Exception) {} 
                        } 
                    }
                    close() 
                } catch (e: Exception) { close(e) }
            }
        })
        awaitClose { call.cancel() }
    }

    suspend fun getHistory(): Result<List<ConversationUi>> = try { 
        val r = api.getHistory()
        if(r.isSuccessful) Result.success(r.body()?.conversations ?: emptyList()) else Result.failure(Exception("Failed")) 
    } catch(e:Exception) { Result.failure(e) }

    suspend fun getChatMessages(convId: String): Result<List<ChatMessageUi>> = try { 
        val r = api.getChatMessages(convId)
        if (r.isSuccessful && r.body() != null) { 
            val msgs = r.body()!!.getAsJsonArray("messages")
            Result.success(msgs?.map { 
                val o = it.asJsonObject
                ChatMessageUi(o.get("id")?.asString ?: UUID.randomUUID().toString(), o.get("role").asString, o.get("content").asString, "History") 
            } ?: emptyList()) 
        } else Result.failure(Exception("Failed")) 
    } catch(e:Exception) { Result.failure(e) }

    suspend fun sendFeedback(msgId: String, type: String): Result<Boolean> = try { 
        val r = api.sendFeedback(JsonObject().apply { addProperty("message_id", msgId); addProperty("feedback_type", type) })
        if (r.isSuccessful) Result.success(true) else Result.failure(Exception("Failed")) 
    } catch(e:Exception) { Result.failure(e) }

    suspend fun getSkills(): Result<List<DynamicSkill>> = try { 
        val r = api.getSkills()
        if(r.isSuccessful && r.body()?.status=="completed") Result.success(r.body()?.skills ?: emptyList()) else Result.failure(Exception("Failed")) 
    } catch(e:Exception) { Result.failure(e) }

    suspend fun getAiModels(): Result<List<AIModelConfig>> = try { 
        val r = api.getAiModels()
        if(r.isSuccessful && r.body()?.status=="completed") Result.success(r.body()?.models ?: emptyList()) else Result.failure(Exception("Failed")) 
    } catch(e:Exception) { Result.failure(e) }

    suspend fun updateAiModel(id: String? = null, name: String? = null, model: String? = null, priority: Int? = null, isEnabled: Boolean? = null, baseUrl: String? = null, apiKey: String? = null): Result<Boolean> = try { 
        val payload = JsonObject().apply { 
            if(id!=null)addProperty("id",id)
            if(name!=null)addProperty("provider_name",name)
            if(model!=null)addProperty("model_default",model)
            if(priority!=null)addProperty("priority",priority)
            if(isEnabled!=null)addProperty("is_enabled",isEnabled)
            if(baseUrl!=null)addProperty("base_url",baseUrl)
            if(apiKey!=null)addProperty("api_key",apiKey) 
        }
        val r = api.updateAiModel(payload)
        if(r.isSuccessful) Result.success(true) else Result.failure(Exception("Failed")) 
    } catch(e:Exception) { Result.failure(e) }

    suspend fun getPrompts(): Result<List<PromptConfig>> = try { 
        val r = api.getPrompts()
        if(r.isSuccessful && r.body()?.status=="completed") Result.success(r.body()?.prompts ?: emptyList()) else Result.failure(Exception("Failed")) 
    } catch(e:Exception) { Result.failure(e) }

    suspend fun updatePrompt(k:String, t:String): Result<Boolean> = try { 
        val r = api.updatePrompt(JsonObject().apply { addProperty("name",k); addProperty("content",t) })
        if(r.isSuccessful) Result.success(true) else Result.failure(Exception("Failed")) 
    } catch(e:Exception) { Result.failure(e) }

    suspend fun saveByoi(type:String, f1:String, f2:String, f3:String): Result<Boolean> = try { 
        val r = api.saveByoi(JsonObject().apply { addProperty("integration_type",type); addProperty("endpoint",f1); addProperty("api_key",f2); addProperty("model",f3) })
        if(r.isSuccessful) Result.success(true) else Result.failure(Exception("Failed")) 
    } catch(e:Exception) { Result.failure(e) }

    suspend fun signup(email:String, pass:String): Result<String> = try { 
        val r = api.signup(SignupRequest(email,pass))
        if(r.isSuccessful) Result.success("OK") else Result.failure(Exception(parseError(r.errorBody()?.string(),"Signup failed"))) 
    } catch(e:Exception) { Result.failure(e) }

    suspend fun verifyOtp(email:String, otp:String): Result<Boolean> = try { 
        val r = api.verifyOtp(VerifyOtpRequest(email,otp))
        if(r.isSuccessful) Result.success(true) else Result.failure(Exception(parseError(r.errorBody()?.string(),"Invalid OTP"))) 
    } catch(e:Exception) { Result.failure(e) }

    suspend fun logout() { 
        try { api.logout() } catch(e:Exception) {} finally { tokenManager.clearSession() } 
    }

    suspend fun login(email:String, pass:String): Result<Boolean> {
        return try { 
            val r = api.login(LoginRequest(email,pass))
            if(r.isSuccessful && r.body()!=null) { 
                val b=r.body()!!
                val jwt=b.token?:b.accessToken?:""
                if(jwt.isBlank()) {
                    Result.failure(Exception("No token"))
                } else {
                    tokenManager.saveSession(jwt, b.userId?:tokenManager.getUserId(), b.workspaceId?:tokenManager.getWorkspaceId(), email, b.role?:"normal_user")
                    Result.success(true) 
                }
            } else { 
                val e = r.errorBody()?.string()?:"{}"
                if(r.code()==403 && e.contains("verify")) Result.failure(Exception("UNVERIFIED_USER")) 
                else Result.failure(Exception(parseError(e,"Login failed"))) 
            } 
        } catch(e:Exception) { 
            Result.failure(e) 
        }
    }

    suspend fun sendFastChat(message:String, projectId:String?, convId:String?, skillKey:String, ceoMode:Boolean): Result<Pair<ChatMessageUi, String?>> = try { 
        val req = FastChatRequest(newRequestId(), message, tokenManager.getUserId(), tokenManager.getWorkspaceId(), projectId, convId, skillKey, ceoMode)
        val r = api.fastChat(req)
        if(r.isSuccessful && r.body()!=null) { 
            val b = r.body()!!
            Result.success(Pair(ChatMessageUi(b.requestId ?: newRequestId(), "assistant", b.reply ?: b.message ?: "OK", "Just now", b.verification?.status ?: "verified", b.provider ?: "groq"), b.conversationId)) 
        } else Result.failure(Exception(parseError(r.errorBody()?.string(), "Failed"))) 
    } catch(e:Exception) { Result.failure(e) }

    suspend fun executeGatewayIntent(intent:String, payloadObj:JsonObject, projectId:String?): Result<GatewayExecuteResponse> = try { 
        val req = GatewayExecuteRequest(newRequestId(), tokenManager.getUserId(), tokenManager.getWorkspaceId(), projectId, null, intent, "manual", payloadObj)
        val r = api.executeWorkflow(req)
        if(r.isSuccessful && r.body()!=null) Result.success(r.body()!!) else Result.failure(Exception("Failed")) 
    } catch(e:Exception) { Result.failure(e) }

    suspend fun saveWorkflowMemory(workflowJson:String, projectId:String?): Result<Boolean> = try { 
        val r = api.saveMemory(MemoryWriteRequest(scope="workflow", key="wf_${System.currentTimeMillis()}", content=workflowJson, category="generated", projectId=projectId))
        if(r.isSuccessful) Result.success(true) else Result.failure(Exception("Failed")) 
    } catch(e:Exception) { Result.failure(e) }

    suspend fun testWorkflow(workflowJson:JsonObject): Result<Boolean> = try { 
        val r = api.testWorkflow(JsonObject().apply { add("workflow_json",workflowJson) })
        if(r.isSuccessful && r.body()?.get("status")?.asString=="passed") Result.success(true) else Result.failure(Exception("Failed")) 
    } catch(e:Exception) { Result.failure(e) }

    suspend fun deployWorkflow(workflowJson:JsonObject): Result<String> = try { 
        val r = api.deployWorkflow(JsonObject().apply { add("workflow_json",workflowJson) })
        if(r.isSuccessful) Result.success("Deployed") else Result.failure(Exception("Failed")) 
    } catch(e:Exception) { Result.failure(e) }

    suspend fun fetchTasks(): Result<List<TaskItemUi>> = try { 
        val r = api.getTasks()
        if(r.isSuccessful && r.body()?.get("status")?.asString=="completed") { 
            Result.success(r.body()!!.getAsJsonArray("tasks")?.map{ 
                val o=it.asJsonObject
                TaskItemUi(o.get("id").asString, o.get("title").asString, "", o.get("status").asString, o.get("priority").asString, createdAt = o.get("created_at")?.asString ?: "", updatedAt = o.get("updated_at")?.asString ?: "") 
            } ?: emptyList()) 
        } else Result.failure(Exception("Failed")) 
    } catch(e:Exception) { Result.failure(e) }

    suspend fun fetchMemory(scope:String, projectId:String?): Result<List<MemoryItemUi>> = try { 
        val r = api.readMemory(JsonObject().apply { addProperty("scope",scope); if(projectId!=null)addProperty("project_id",projectId) })
        if(r.isSuccessful && r.body()?.get("status")?.asString=="completed") { 
            Result.success(r.body()!!.getAsJsonArray("memory_items")?.map{ 
                val o=it.asJsonObject
                MemoryItemUi(o.get("id").asString, o.get("scope").asString, o.get("key")?.asString, o.get("content").asString, o.get("category").asString, knowledgeStatus = o.get("knowledge_status")?.asString ?: "", createdAt = o.get("created_at")?.asString ?: "") 
            } ?: emptyList()) 
        } else Result.failure(Exception("Failed")) 
    } catch(e:Exception) { Result.failure(e) }

    suspend fun checkBackendHealth(): Boolean = try { api.checkHealth().isSuccessful } catch(e:Exception) { false }

    suspend fun sendGatewayChat(message: String, projectId: String?, convId: String?): Result<ChatMessageUi> {
        val reqId = newRequestId()
        return try {
            val payload = JsonObject().apply { addProperty("message", message) }
            val req = GatewayExecuteRequest(reqId, tokenManager.getUserId(), tokenManager.getWorkspaceId(), projectId, convId, "CHAT", "manual", payload)
            val resp = api.executeWorkflow(req)
            if (resp.isSuccessful && resp.body() != null) {
                val b = resp.body()!!
                if (b.status == "completed") {
                    val child = b.result?.getAsJsonObject("child_result")
                    val text = child?.get("assistant_message")?.asString ?: "Gateway Success (No msg)"
                    Result.success(ChatMessageUi(reqId, "assistant", text, "Just now", b.verification?.status ?: "verified", "n8n-gateway"))
                } else Result.failure(Exception(b.errors?.firstOrNull()?.message ?: "Gateway Error"))
            } else Result.failure(Exception(parseError(resp.errorBody()?.string(), "Gateway failed")))
        } catch (e: Exception) { Result.failure(e) }
    }
}
