package com.rk.ai.mcp.engine

import ai.koog.agents.core.tools.ToolParameterType
import android.util.Log
import com.rk.ai.mcp.client.McpClientProvider
import com.rk.ai.mcp.model.McpRiskClass
import com.rk.ai.mcp.model.McpToolDescriptor
import com.rk.ai.tools.AiTool
import com.rk.ai.tools.AiToolKind
import com.rk.ai.tools.AiToolParameter
import com.rk.ai.tools.AiToolRegistry
import kotlinx.serialization.json.JsonObject
import org.json.JSONObject

object McpToolBridge {
    private const val TAG = "McpToolBridge"
    private val activeRegisteredTools = mutableMapOf<String, AiTool>()

    @Synchronized
    fun syncApprovedTools(serverId: String, approvedTools: List<McpToolDescriptor>) {
        // Unregister previously active tools for this server that are no longer approved
        val prefix = serverPrefix(serverId)
        val iterator = activeRegisteredTools.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.key.startsWith(prefix) && approvedTools.none { toAiToolName(serverId, it.name) == entry.key }) {
                AiToolRegistry.unregisterTool(entry.value)
                iterator.remove()
                Log.i(TAG, "Unregistered removed MCP tool ${entry.key}")
            }
        }

        // Register or update active tools
        approvedTools.forEach { descriptor ->
            val aiTool = createAiTool(descriptor)
            AiToolRegistry.registerTool(aiTool)
            activeRegisteredTools[aiTool.name] = aiTool
            Log.i(TAG, "Registered MCP tool ${aiTool.name} (${descriptor.riskClass})")
        }
    }

    @Synchronized
    fun unregisterServer(serverId: String) {
        val prefix = serverPrefix(serverId)
        val iterator = activeRegisteredTools.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.key.startsWith(prefix)) {
                AiToolRegistry.unregisterTool(entry.value)
                iterator.remove()
            }
        }
        Log.i(TAG, "Unregistered all MCP tools for server $serverId")
    }

    private fun serverPrefix(serverId: String): String {
        val safeServer = serverId.replace(Regex("[^A-Za-z0-9_]"), "_")
        return "mcp_${safeServer}_"
    }

    private fun toAiToolName(serverId: String, toolName: String): String {
        return "${serverPrefix(serverId)}$toolName"
    }

    private fun createAiTool(descriptor: McpToolDescriptor): AiTool {
        val registrationName = toAiToolName(descriptor.serverId, descriptor.name)
        val kind =
            when (descriptor.riskClass) {
                McpRiskClass.CLASS_A_SAFE -> AiToolKind.Read
                McpRiskClass.CLASS_B_SCOPED -> AiToolKind.Read
                McpRiskClass.CLASS_C_QUARANTINE -> {
                    val lower = descriptor.name.lowercase()
                    when {
                        lower.contains("write") || lower.contains("create") || lower.contains("delete") ->
                            AiToolKind.Write
                        lower.contains("shell") || lower.contains("exec") || lower.contains("terminal") ->
                            AiToolKind.Shell
                        lower.contains("http") || lower.contains("network") || lower.contains("curl") ->
                            AiToolKind.Network
                        else -> AiToolKind.Write
                    }
                }
            }

        val parameters = parseParameters(descriptor.inputSchemaJson)

        return AiTool(
            name = registrationName,
            description = "[MCP: ${descriptor.serverId}] ${descriptor.description}",
            kind = kind,
            parameters = parameters,
            isDestructive = kind != AiToolKind.Read,
            execute = { args: JsonObject ->
                val client =
                    McpClientProvider.getClient(descriptor.serverId)
                        ?: return@AiTool "Error: MCP server '${descriptor.serverId}' is not available."
                val jsonArgs = runCatching { JSONObject(args.toString()) }.getOrDefault(JSONObject())
                val result = client.callTool(descriptor.name, jsonArgs)
                result.getOrElse { e ->
                    Log.e(TAG, "Error executing MCP tool '${descriptor.name}'", e)
                    "Error executing MCP tool '${descriptor.name}': ${e.message}"
                }
            },
        )
    }

    private fun parseParameters(schemaJson: String): List<AiToolParameter> {
        val list = mutableListOf<AiToolParameter>()
        runCatching {
            val root = JSONObject(schemaJson)
            val properties = root.optJSONObject("properties") ?: return emptyList()
            val requiredArr = root.optJSONArray("required") ?: org.json.JSONArray()
            val requiredSet = mutableSetOf<String>()
            for (i in 0 until requiredArr.length()) {
                requiredSet.add(requiredArr.getString(i))
            }

            val keys = properties.keys()
            while (keys.hasNext()) {
                val paramName = keys.next()
                val propObj = properties.optJSONObject(paramName)
                val desc = propObj?.optString("description", "") ?: ""
                val typeStr = propObj?.optString("type", "string") ?: "string"
                val paramType =
                    when (typeStr) {
                        "integer",
                        "number" -> ToolParameterType.Integer
                        "boolean" -> ToolParameterType.Boolean
                        else -> ToolParameterType.String
                    }
                list.add(
                    AiToolParameter(
                        name = paramName,
                        description = desc,
                        type = paramType,
                        required = paramName in requiredSet,
                    )
                )
            }
        }
        return list
    }
}
