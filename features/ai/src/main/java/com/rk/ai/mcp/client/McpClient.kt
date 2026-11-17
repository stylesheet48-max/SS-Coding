package com.rk.ai.mcp.client

import com.rk.ai.mcp.model.McpToolDescriptor
import org.json.JSONObject

interface McpClient {
    val serverId: String
    suspend fun connect(): Result<Unit>
    suspend fun listTools(): Result<List<McpToolDescriptor>>
    suspend fun callTool(name: String, arguments: JSONObject): Result<String>
    suspend fun disconnect()
}
