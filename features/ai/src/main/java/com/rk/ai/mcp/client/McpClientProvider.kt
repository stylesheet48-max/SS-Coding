package com.rk.ai.mcp.client

import com.rk.ai.mcp.engine.McpRegistry
import com.rk.ai.mcp.model.McpServerConfig
import com.rk.ai.mcp.model.McpServerType
import java.util.concurrent.ConcurrentHashMap

object McpClientProvider {
    private val clientCache = ConcurrentHashMap<String, McpClient>()

    fun getClient(config: McpServerConfig): McpClient {
        return clientCache.computeIfAbsent(config.id) {
            when (config.type) {
                McpServerType.REMOTE -> RemoteSseMcpClient(config)
                McpServerType.LOCAL -> LocalProcessMcpClient(config)
            }
        }
    }

    fun getClient(serverId: String): McpClient? {
        val cached = clientCache[serverId]
        if (cached != null) return cached
        val config = McpRegistry.servers.value.firstOrNull { it.id == serverId } ?: return null
        return getClient(config)
    }

    suspend fun removeClient(serverId: String) {
        val client = clientCache.remove(serverId)
        client?.disconnect()
    }
}
