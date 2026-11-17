package com.rk.ai.mcp.client

import android.util.Log
import com.rk.ai.mcp.model.McpServerConfig
import com.rk.ai.mcp.model.McpToolDescriptor
import com.rk.ai.mcp.protocol.McpJsonRpc
import com.rk.ai.mcp.security.McpSecureVault
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class RemoteSseMcpClient(val config: McpServerConfig) : McpClient {

    override val serverId: String
        get() = config.id

    private val idCounter = AtomicLong(1L)

    private val httpClient: OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

    override suspend fun connect(): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val initPayload = McpJsonRpc.buildInitializeRequest(idCounter.getAndIncrement())
                val responseText = executePost(initPayload)
                val json = JSONObject(responseText)
                if (json.has("error")) {
                    val err = json.getJSONObject("error")
                    throw IOException("MCP init failed: ${err.optString("message")}")
                }
                Log.i(TAG, "Connected to remote MCP server ${config.name} ($serverId)")
                Unit
            }

    override suspend fun listTools(): Result<List<McpToolDescriptor>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val listPayload = McpJsonRpc.buildToolsListRequest(idCounter.getAndIncrement())
                val responseText = executePost(listPayload)
                val tools = McpJsonRpc.parseToolsListResponse(serverId, responseText).getOrThrow()
                Log.i(TAG, "Fetched ${tools.size} tools from remote MCP server $serverId")
                tools
            }
        }

    override suspend fun callTool(name: String, arguments: JSONObject): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val callPayload = McpJsonRpc.buildToolCallRequest(idCounter.getAndIncrement(), name, arguments)
                val responseText = executePost(callPayload)
                McpJsonRpc.parseToolCallResponse(responseText).getOrThrow()
            }
        }

    override suspend fun disconnect() {
        // HTTP connection pool handles cleanup automatically
    }

    private fun executePost(jsonPayload: String): String {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val requestBody = jsonPayload.toRequestBody(mediaType)

        val requestBuilder =
            Request.Builder()
                .url(config.urlOrCommand)
                .post(requestBody)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")

        config.bearerTokenSecretKey?.let { secretKey ->
            val token = McpSecureVault.getSecret(secretKey)
            if (!token.isNullOrBlank()) {
                requestBuilder.header("Authorization", "Bearer $token")
            }
        }

        config.env.forEach { (k, v) -> requestBuilder.header("X-MCP-$k", v) }

        val response = httpClient.newCall(requestBuilder.build()).execute()
        response.use { resp ->
            if (!resp.isSuccessful) {
                throw IOException("HTTP ${resp.code}: ${resp.message}")
            }
            return resp.body?.string().orEmpty()
        }
    }

    companion object {
        private const val TAG = "RemoteSseMcpClient"
    }
}
