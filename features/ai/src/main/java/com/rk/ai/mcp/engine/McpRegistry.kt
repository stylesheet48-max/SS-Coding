package com.rk.ai.mcp.engine

import android.content.Context
import android.util.Log
import com.rk.ai.mcp.model.McpDiffResult
import com.rk.ai.mcp.model.McpRiskClass
import com.rk.ai.mcp.model.McpServerConfig
import com.rk.ai.mcp.model.McpServerStatus
import com.rk.ai.mcp.model.McpToolDescriptor
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

object McpRegistry {
    private const val TAG = "McpRegistry"

    private lateinit var baseDir: File
    private lateinit var serversDir: File
    private lateinit var registryDir: File
    private lateinit var quarantineDir: File
    private lateinit var auditDir: File
    private lateinit var rollbackDir: File

    private val _servers = MutableStateFlow<List<McpServerConfig>>(emptyList())
    val servers: StateFlow<List<McpServerConfig>> = _servers.asStateFlow()

    private val _approvedTools = MutableStateFlow<Map<String, List<McpToolDescriptor>>>(emptyMap())
    val approvedTools: StateFlow<Map<String, List<McpToolDescriptor>>> = _approvedTools.asStateFlow()

    private val _pendingApprovals = MutableStateFlow<List<McpDiffResult>>(emptyList())
    val pendingApprovals: StateFlow<List<McpDiffResult>> = _pendingApprovals.asStateFlow()

    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        baseDir = File(context.filesDir, "mcp").apply { mkdirs() }
        serversDir = File(baseDir, "servers").apply { mkdirs() }
        registryDir = File(baseDir, "registry").apply { mkdirs() }
        quarantineDir = File(baseDir, "quarantine").apply { mkdirs() }
        auditDir = File(baseDir, "audit-logs").apply { mkdirs() }
        rollbackDir = File(baseDir, "rollback").apply { mkdirs() }

        reloadAll()
        initialized = true
        Log.i(TAG, "McpRegistry initialized at ${baseDir.absolutePath}")
    }

    @Synchronized
    fun reloadAll() {
        // Load servers
        val serverList = mutableListOf<McpServerConfig>()
        serversDir.listFiles()?.forEach { file ->
            if (file.name.endsWith(".json")) {
                runCatching {
                    val content = file.readText(Charsets.UTF_8)
                    val json = JSONObject(content)
                    serverList.add(McpServerConfig.fromJson(json))
                }
                    .onFailure { Log.e(TAG, "Failed reading server config ${file.name}", it) }
            }
        }
        _servers.value = serverList

        // Load approved tools
        val toolsMap = mutableMapOf<String, List<McpToolDescriptor>>()
        registryDir.listFiles()?.forEach { file ->
            if (file.name.endsWith("_tools.json")) {
                val serverId = file.name.removeSuffix("_tools.json")
                runCatching {
                    val arr = JSONArray(file.readText(Charsets.UTF_8))
                    val list = mutableListOf<McpToolDescriptor>()
                    for (i in 0 until arr.length()) {
                        list.add(McpToolDescriptor.fromJson(arr.getJSONObject(i)))
                    }
                    toolsMap[serverId] = list
                }
                    .onFailure { Log.e(TAG, "Failed reading tools for $serverId", it) }
            }
        }
        _approvedTools.value = toolsMap

        // Load pending quarantine diffs
        val pendingList = mutableListOf<McpDiffResult>()
        quarantineDir.listFiles()?.forEach { file ->
            if (file.name.endsWith("_pending.json")) {
                val serverId = file.name.removeSuffix("_pending.json")
                runCatching {
                    val json = JSONObject(file.readText(Charsets.UTF_8))
                    val added = mutableListOf<McpToolDescriptor>()
                    val addedArr = json.optJSONArray("added")
                    if (addedArr != null) {
                        for (i in 0 until addedArr.length()) {
                            added.add(McpToolDescriptor.fromJson(addedArr.getJSONObject(i)))
                        }
                    }
                    val modified = mutableListOf<McpToolDescriptor>()
                    val modArr = json.optJSONArray("modified")
                    if (modArr != null) {
                        for (i in 0 until modArr.length()) {
                            modified.add(McpToolDescriptor.fromJson(modArr.getJSONObject(i)))
                        }
                    }
                    val removed = mutableListOf<String>()
                    val remArr = json.optJSONArray("removed")
                    if (remArr != null) {
                        for (i in 0 until remArr.length()) {
                            removed.add(remArr.getString(i))
                        }
                    }
                    val riskStr = json.optString("maxRiskClass", McpRiskClass.CLASS_C_QUARANTINE.name)
                    val maxRisk = runCatching {
                        McpRiskClass.valueOf(riskStr)
                    }.getOrDefault(McpRiskClass.CLASS_C_QUARANTINE)

                    pendingList.add(
                        McpDiffResult(
                            serverId = serverId,
                            addedTools = added,
                            modifiedTools = modified,
                            removedToolNames = removed,
                            maxRiskClass = maxRisk,
                            requiresApproval = true,
                        )
                    )
                }
                    .onFailure { Log.e(TAG, "Failed reading quarantine diff for $serverId", it) }
            }
        }
        _pendingApprovals.value = pendingList
    }

    @Synchronized
    fun saveServer(config: McpServerConfig) {
        val file = File(serversDir, "${config.id}.json")
        file.writeText(config.toJson().toString(2), Charsets.UTF_8)
        val list = _servers.value.filter { it.id != config.id } + config
        _servers.value = list
        logAudit("SERVER_SAVED", "Saved configuration for server ${config.name} (${config.id})")
    }

    @Synchronized
    fun removeServer(serverId: String) {
        File(serversDir, "$serverId.json").delete()
        File(registryDir, "${serverId}_tools.json").delete()
        File(quarantineDir, "${serverId}_pending.json").delete()
        File(rollbackDir, "${serverId}_rollback.json").delete()

        _servers.value = _servers.value.filter { it.id != serverId }
        val newMap = _approvedTools.value.toMutableMap().apply { remove(serverId) }
        _approvedTools.value = newMap
        _pendingApprovals.value = _pendingApprovals.value.filter { it.serverId != serverId }
        logAudit("SERVER_REMOVED", "Removed server $serverId and all associated tool entries")
    }

    @Synchronized
    fun updateServerStatus(
        serverId: String,
        status: McpServerStatus,
        message: String? = null,
        fingerprint: String? = null,
    ) {
        val current = _servers.value.firstOrNull { it.id == serverId } ?: return
        val updated =
            current.copy(
                status = status,
                statusMessage = message,
                lastChecked = System.currentTimeMillis(),
                fingerprint = fingerprint ?: current.fingerprint,
            )
        saveServer(updated)
    }

    @Synchronized
    fun saveApprovedTools(serverId: String, tools: List<McpToolDescriptor>) {
        val file = File(registryDir, "${serverId}_tools.json")
        val arr = JSONArray()
        tools.forEach { arr.put(it.toJson()) }
        file.writeText(arr.toString(2), Charsets.UTF_8)

        val newMap = _approvedTools.value.toMutableMap()
        newMap[serverId] = tools
        _approvedTools.value = newMap
        logAudit("TOOLS_APPROVED", "Updated approved tools for $serverId (Count: ${tools.size})")
    }

    @Synchronized
    fun getApprovedTools(serverId: String): List<McpToolDescriptor> {
        return _approvedTools.value[serverId] ?: emptyList()
    }

    @Synchronized
    fun saveQuarantinedDiff(diff: McpDiffResult) {
        val file = File(quarantineDir, "${diff.serverId}_pending.json")
        val json = JSONObject()
        val addedArr = JSONArray()
        diff.addedTools.forEach { addedArr.put(it.toJson()) }
        json.put("added", addedArr)

        val modArr = JSONArray()
        diff.modifiedTools.forEach { modArr.put(it.toJson()) }
        json.put("modified", modArr)

        val remArr = JSONArray()
        diff.removedToolNames.forEach { remArr.put(it) }
        json.put("removed", remArr)

        json.put("maxRiskClass", diff.maxRiskClass.name)
        json.put("requiresApproval", true)

        file.writeText(json.toString(2), Charsets.UTF_8)
        val list = _pendingApprovals.value.filter { it.serverId != diff.serverId } + diff
        _pendingApprovals.value = list

        updateServerStatus(diff.serverId, McpServerStatus.QUARANTINED, "New Class C permissions require owner approval")
        logAudit("QUARANTINE_TRIGGERED", "Quarantined updates for ${diff.serverId} due to Class C risk")
    }

    @Synchronized
    fun clearQuarantinedDiff(serverId: String) {
        File(quarantineDir, "${serverId}_pending.json").delete()
        _pendingApprovals.value = _pendingApprovals.value.filter { it.serverId != serverId }
    }

    @Synchronized
    fun createRollbackSnapshot(serverId: String) {
        val toolsFile = File(registryDir, "${serverId}_tools.json")
        if (toolsFile.exists()) {
            val rollbackFile = File(rollbackDir, "${serverId}_rollback.json")
            toolsFile.copyTo(rollbackFile, overwrite = true)
        }
    }

    @Synchronized
    fun restoreRollbackSnapshot(serverId: String): Boolean {
        val rollbackFile = File(rollbackDir, "${serverId}_rollback.json")
        if (!rollbackFile.exists()) return false

        val toolsFile = File(registryDir, "${serverId}_tools.json")
        rollbackFile.copyTo(toolsFile, overwrite = true)
        reloadAll()
        logAudit("ROLLBACK_RESTORED", "Restored rollback snapshot for server $serverId")
        return true
    }

    fun logAudit(action: String, details: String) {
        runCatching {
            val logFile = File(auditDir, "events.log")
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            logFile.appendText("[$timestamp] [$action] $details\n", Charsets.UTF_8)
        }
    }
}
