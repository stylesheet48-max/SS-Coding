package com.rk.ai.mcp.protocol

import com.rk.ai.mcp.model.McpToolDescriptor
import com.rk.ai.mcp.security.McpHasher
import com.rk.ai.mcp.security.McpSecurityGate
import org.json.JSONArray
import org.json.JSONObject

object McpJsonRpc {
    const val JSONRPC_VERSION = "2.0"

    fun buildInitializeRequest(requestId: Long = 1L): String {
        val root = JSONObject()
        root.put("jsonrpc", JSONRPC_VERSION)
        root.put("id", requestId)
        root.put("method", "initialize")

        val params = JSONObject()
        params.put("protocolVersion", "2024-11-05")

        val clientInfo = JSONObject()
        clientInfo.put("name", "SS coding")
        clientInfo.put("version", "1.0.3")
        params.put("clientInfo", clientInfo)

        val capabilities = JSONObject()
        capabilities.put("tools", JSONObject())
        params.put("capabilities", capabilities)

        root.put("params", params)
        return root.toString()
    }

    fun buildToolsListRequest(requestId: Long = 2L): String {
        val root = JSONObject()
        root.put("jsonrpc", JSONRPC_VERSION)
        root.put("id", requestId)
        root.put("method", "tools/list")
        root.put("params", JSONObject())
        return root.toString()
    }

    fun buildToolCallRequest(requestId: Long, toolName: String, arguments: JSONObject): String {
        val root = JSONObject()
        root.put("jsonrpc", JSONRPC_VERSION)
        root.put("id", requestId)
        root.put("method", "tools/call")

        val params = JSONObject()
        params.put("name", toolName)
        params.put("arguments", arguments)
        root.put("params", params)

        return root.toString()
    }

    fun buildPingRequest(requestId: Long): String {
        val root = JSONObject()
        root.put("jsonrpc", JSONRPC_VERSION)
        root.put("id", requestId)
        root.put("method", "ping")
        return root.toString()
    }

    fun parseToolsListResponse(serverId: String, responseJson: String): Result<List<McpToolDescriptor>> {
        return runCatching {
            val root = JSONObject(responseJson)
            if (root.has("error")) {
                val err = root.getJSONObject("error")
                throw IllegalStateException("MCP JSON-RPC Error ${err.optInt("code")}: ${err.optString("message")}")
            }
            val result = root.optJSONObject("result") ?: throw IllegalStateException("Missing 'result' in response")
            val toolsArr = result.optJSONArray("tools") ?: JSONArray()
            val list = mutableListOf<McpToolDescriptor>()

            for (i in 0 until toolsArr.length()) {
                val toolObj = toolsArr.getJSONObject(i)
                val name = toolObj.getString("name")
                val desc = toolObj.optString("description", "")
                val schemaObj = toolObj.optJSONObject("inputSchema") ?: JSONObject()
                val schemaJson = schemaObj.toString()
                val riskClass = McpSecurityGate.classifyTool(name, desc, schemaJson)
                val hash = McpHasher.hashTool(name, desc, schemaJson)

                list.add(
                    McpToolDescriptor(
                        name = name,
                        description = desc,
                        serverId = serverId,
                        inputSchemaJson = schemaJson,
                        riskClass = riskClass,
                        hash = hash,
                        isQuarantined = false,
                    )
                )
            }
            list
        }
    }

    fun parseToolCallResponse(responseJson: String): Result<String> {
        return runCatching {
            val root = JSONObject(responseJson)
            if (root.has("error")) {
                val err = root.getJSONObject("error")
                throw IllegalStateException("Tool call error ${err.optInt("code")}: ${err.optString("message")}")
            }
            val result = root.optJSONObject("result") ?: throw IllegalStateException("Missing result in tool response")
            val isError = result.optBoolean("isError", false)

            val contentArr = result.optJSONArray("content")
            val textBuilder = StringBuilder()
            if (contentArr != null) {
                for (i in 0 until contentArr.length()) {
                    val item = contentArr.getJSONObject(i)
                    if (item.optString("type") == "text") {
                        textBuilder.append(item.optString("text")).append("\n")
                    } else {
                        textBuilder.append(item.toString()).append("\n")
                    }
                }
            } else if (result.has("text")) {
                textBuilder.append(result.optString("text"))
            } else {
                textBuilder.append(result.toString())
            }

            val text = textBuilder.toString().trim()
            if (isError) {
                "Error: $text"
            } else {
                text
            }
        }
    }
}
