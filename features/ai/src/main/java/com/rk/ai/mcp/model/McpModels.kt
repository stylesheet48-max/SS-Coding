package com.rk.ai.mcp.model

import org.json.JSONArray
import org.json.JSONObject

enum class McpServerType {
    LOCAL,
    REMOTE,
}

enum class McpServerStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    OFFLINE,
    ERROR,
    QUARANTINED,
}

enum class McpRiskClass {
    CLASS_A_SAFE,
    CLASS_B_SCOPED,
    CLASS_C_QUARANTINE,
}

data class McpServerConfig(
    val id: String,
    val name: String,
    val type: McpServerType,
    val urlOrCommand: String,
    val args: List<String> = emptyList(),
    val env: Map<String, String> = emptyMap(),
    val bearerTokenSecretKey: String? = null,
    val enabled: Boolean = true,
    val lastChecked: Long = 0L,
    val status: McpServerStatus = McpServerStatus.DISCONNECTED,
    val statusMessage: String? = null,
    val fingerprint: String? = null,
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("name", name)
        json.put("type", type.name)
        json.put("urlOrCommand", urlOrCommand)
        val argsArr = JSONArray()
        args.forEach { argsArr.put(it) }
        json.put("args", argsArr)
        val envObj = JSONObject()
        env.forEach { (k, v) -> envObj.put(k, v) }
        json.put("env", envObj)
        json.put("bearerTokenSecretKey", bearerTokenSecretKey ?: JSONObject.NULL)
        json.put("enabled", enabled)
        json.put("lastChecked", lastChecked)
        json.put("status", status.name)
        json.put("statusMessage", statusMessage ?: JSONObject.NULL)
        json.put("fingerprint", fingerprint ?: JSONObject.NULL)
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): McpServerConfig {
            val id = json.getString("id")
            val name = json.optString("name", id)
            val typeStr = json.optString("type", McpServerType.REMOTE.name)
            val type = runCatching { McpServerType.valueOf(typeStr) }.getOrDefault(McpServerType.REMOTE)
            val urlOrCommand = json.optString("urlOrCommand", "")

            val args = mutableListOf<String>()
            val argsArr = json.optJSONArray("args")
            if (argsArr != null) {
                for (i in 0 until argsArr.length()) {
                    args.add(argsArr.getString(i))
                }
            }

            val env = mutableMapOf<String, String>()
            val envObj = json.optJSONObject("env")
            if (envObj != null) {
                val keys = envObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    env[k] = envObj.getString(k)
                }
            }

            val bearerKey = if (json.isNull("bearerTokenSecretKey")) null else json.optString("bearerTokenSecretKey")
            val enabled = json.optBoolean("enabled", true)
            val lastChecked = json.optLong("lastChecked", 0L)
            val statusStr = json.optString("status", McpServerStatus.DISCONNECTED.name)
            val status = runCatching { McpServerStatus.valueOf(statusStr) }.getOrDefault(McpServerStatus.DISCONNECTED)
            val statusMessage = if (json.isNull("statusMessage")) null else json.optString("statusMessage")
            val fingerprint = if (json.isNull("fingerprint")) null else json.optString("fingerprint")

            return McpServerConfig(
                id = id,
                name = name,
                type = type,
                urlOrCommand = urlOrCommand,
                args = args,
                env = env,
                bearerTokenSecretKey = bearerKey,
                enabled = enabled,
                lastChecked = lastChecked,
                status = status,
                statusMessage = statusMessage,
                fingerprint = fingerprint,
            )
        }
    }
}

data class McpToolDescriptor(
    val name: String,
    val description: String,
    val serverId: String,
    val inputSchemaJson: String = "{}",
    val riskClass: McpRiskClass = McpRiskClass.CLASS_A_SAFE,
    val hash: String = "",
    val isQuarantined: Boolean = false,
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("name", name)
        json.put("description", description)
        json.put("serverId", serverId)
        json.put("inputSchemaJson", inputSchemaJson)
        json.put("riskClass", riskClass.name)
        json.put("hash", hash)
        json.put("isQuarantined", isQuarantined)
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): McpToolDescriptor {
            val name = json.getString("name")
            val description = json.optString("description", "")
            val serverId = json.optString("serverId", "")
            val inputSchemaJson = json.optString("inputSchemaJson", "{}")
            val riskStr = json.optString("riskClass", McpRiskClass.CLASS_A_SAFE.name)
            val riskClass = runCatching { McpRiskClass.valueOf(riskStr) }.getOrDefault(McpRiskClass.CLASS_A_SAFE)
            val hash = json.optString("hash", "")
            val isQuarantined = json.optBoolean("isQuarantined", false)

            return McpToolDescriptor(
                name = name,
                description = description,
                serverId = serverId,
                inputSchemaJson = inputSchemaJson,
                riskClass = riskClass,
                hash = hash,
                isQuarantined = isQuarantined,
            )
        }
    }
}

data class McpDiffResult(
    val serverId: String,
    val addedTools: List<McpToolDescriptor>,
    val modifiedTools: List<McpToolDescriptor>,
    val removedToolNames: List<String>,
    val maxRiskClass: McpRiskClass,
    val requiresApproval: Boolean,
)
