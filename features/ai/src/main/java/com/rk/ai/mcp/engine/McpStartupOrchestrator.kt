package com.rk.ai.mcp.engine

import android.app.Application
import android.util.Log
import com.rk.ai.mcp.client.McpClientProvider
import com.rk.ai.mcp.model.McpServerConfig
import com.rk.ai.mcp.model.McpServerStatus
import com.rk.ai.mcp.security.McpHasher
import com.rk.ai.mcp.security.McpSecureVault
import com.rk.ai.mcp.security.McpSecurityGate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object McpStartupOrchestrator {
    private const val TAG = "McpStartupOrchestrator"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun start(application: Application) {
        scope.launch {
            Log.i(TAG, "Starting background MCP initialization and update engine")
            // Step 1: Initialize vault and registry
            McpSecureVault.init(application)
            McpRegistry.init(application)

            // Step 2: Immediately bridge existing approved tools so offline or instant usage is ready
            val servers = McpRegistry.servers.value
            servers.forEach { server ->
                val cachedApproved = McpRegistry.getApprovedTools(server.id)
                if (cachedApproved.isNotEmpty() && server.enabled) {
                    McpToolBridge.syncApprovedTools(server.id, cachedApproved)
                }
            }

            // Step 3: Check for updates across enabled servers in background
            checkAllServers(servers)
        }
    }

    fun refreshAll() {
        scope.launch {
            val servers = McpRegistry.servers.value
            checkAllServers(servers)
        }
    }

    suspend fun refreshServer(serverId: String) = withContext(Dispatchers.IO) {
        val server = McpRegistry.servers.value.firstOrNull { it.id == serverId } ?: return@withContext
        checkServer(server)
    }

    private suspend fun checkAllServers(servers: List<McpServerConfig>) {
        if (_isRefreshing.value) return
        _isRefreshing.value = true
        try {
            servers.filter { it.enabled }.forEach { server ->
                runCatching {
                    checkServer(server)
                }.onFailure { e ->
                    Log.e(TAG, "Failed update check for server ${server.id}", e)
                    McpRegistry.updateServerStatus(server.id, McpServerStatus.ERROR, e.message)
                }
            }
        } finally {
            _isRefreshing.value = false
        }
    }

    private suspend fun checkServer(server: McpServerConfig) {
        McpRegistry.updateServerStatus(server.id, McpServerStatus.CONNECTING, "Connecting to MCP server...")
        val client = McpClientProvider.getClient(server)

        // Handshake & Ping
        val connectResult = client.connect()
        if (connectResult.isFailure) {
            val err = connectResult.exceptionOrNull()?.message ?: "Server unreachable"
            Log.w(TAG, "Server ${server.id} connection failed: $err")
            McpRegistry.updateServerStatus(server.id, McpServerStatus.OFFLINE, "Offline: $err")
            return
        }

        // Fetch Tools
        val toolsResult = client.listTools()
        if (toolsResult.isFailure) {
            val err = toolsResult.exceptionOrNull()?.message ?: "Failed listing tools"
            Log.w(TAG, "Server ${server.id} listTools failed: $err")
            McpRegistry.updateServerStatus(server.id, McpServerStatus.ERROR, err)
            return
        }

        val freshTools = toolsResult.getOrThrow()
        val currentApproved = McpRegistry.getApprovedTools(server.id)

        // Security gate classification & diff evaluation
        val diff = McpSecurityGate.evaluateDiff(server.id, currentApproved, freshTools)
        val manifestFingerprint = McpHasher.hashServerManifest(freshTools.map { it.name to it.hash })

        if (diff.requiresApproval) {
            // High risk Class C changes detected: quarantine and do not auto-apply
            Log.w(TAG, "Server ${server.id} has Class C changes requiring Owner Approval")
            McpRegistry.saveQuarantinedDiff(diff)
        } else {
            // Safe Class A or Class B changes: auto-apply with atomic snapshot
            McpRegistry.createRollbackSnapshot(server.id)
            McpRegistry.saveApprovedTools(server.id, freshTools)
            McpToolBridge.syncApprovedTools(server.id, freshTools)
            McpRegistry.clearQuarantinedDiff(server.id)
            McpRegistry.updateServerStatus(
                server.id,
                McpServerStatus.CONNECTED,
                "Connected (${freshTools.size} tools)",
                manifestFingerprint
            )
            Log.i(TAG, "Server ${server.id} auto-updated successfully with ${freshTools.size} tools")
        }
    }

    fun approveQuarantinedServer(serverId: String) {
        scope.launch {
            val pending = McpRegistry.pendingApprovals.value.firstOrNull { it.serverId == serverId } ?: return@launch
            val server = McpRegistry.servers.value.firstOrNull { it.id == serverId } ?: return@launch
            val client = McpClientProvider.getClient(server)

            val freshTools = client.listTools().getOrNull() ?: (pending.addedTools + pending.modifiedTools)
            McpRegistry.createRollbackSnapshot(serverId)
            McpRegistry.saveApprovedTools(serverId, freshTools)
            McpToolBridge.syncApprovedTools(serverId, freshTools)
            McpRegistry.clearQuarantinedDiff(serverId)
            McpRegistry.updateServerStatus(serverId, McpServerStatus.CONNECTED, "Owner Approved (${freshTools.size} tools)")
            McpRegistry.logAudit("MANUAL_APPROVAL", "Owner approved Class C tools for server $serverId")
        }
    }

    fun rejectQuarantinedServer(serverId: String) {
        scope.launch {
            McpRegistry.clearQuarantinedDiff(serverId)
            val current = McpRegistry.getApprovedTools(serverId)
            McpToolBridge.syncApprovedTools(serverId, current)
            McpRegistry.updateServerStatus(serverId, McpServerStatus.CONNECTED, "Update Rejected (Using prior approved tools)")
            McpRegistry.logAudit("MANUAL_REJECTION", "Owner rejected Class C update for server $serverId")
        }
    }
}
