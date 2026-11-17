package com.rk.ai.mcp.security

import com.rk.ai.mcp.model.McpDiffResult
import com.rk.ai.mcp.model.McpRiskClass
import com.rk.ai.mcp.model.McpToolDescriptor

/**
 * Enforces the 3-tier security classification and permission gate for MCP tools and updates:
 * - Class A (Low Risk): Pure compute, formatting, read-only utilities -> Auto-Apply.
 * - Class B (Moderate Risk): Read-only filesystem / metadata inspection -> Auto-Apply with scope confinement.
 * - Class C (High Risk): Write, delete, shell, network, external execution -> Strictly Quarantined awaiting Owner Approval.
 */
object McpSecurityGate {

    private val CLASS_C_KEYWORDS = setOf(
        "write", "create", "delete", "remove", "unlink", "drop", "destroy",
        "exec", "execute", "shell", "bash", "sh", "cmd", "terminal", "run_command",
        "eval", "process", "socket", "network", "connect", "listen", "download",
        "upload", "install", "chmod", "chown", "sudo", "root"
    )

    private val CLASS_B_KEYWORDS = setOf(
        "read", "get", "fetch", "list", "search", "find", "grep", "cat", "view",
        "inspect", "status", "log", "diff", "git_status", "query", "check"
    )

    /**
     * Determines the risk class of a tool based on its name, description, and schema.
     */
    fun classifyTool(name: String, description: String, inputSchemaJson: String): McpRiskClass {
        val lowerName = name.lowercase()
        val lowerDesc = description.lowercase()
        val lowerSchema = inputSchemaJson.lowercase()

        // Check for Class C indicators
        if (CLASS_C_KEYWORDS.any { lowerName.contains(it) || lowerDesc.contains(it) }) {
            return McpRiskClass.CLASS_C_QUARANTINE
        }
        if (lowerSchema.contains("\"command\"") || lowerSchema.contains("\"script\"") ||
            lowerSchema.contains("\"path\"") && (lowerDesc.contains("write") || lowerDesc.contains("modify"))) {
            return McpRiskClass.CLASS_C_QUARANTINE
        }

        // Check for Class B indicators
        if (CLASS_B_KEYWORDS.any { lowerName.contains(it) || lowerDesc.contains(it) } ||
            lowerSchema.contains("\"path\"") || lowerSchema.contains("\"url\"")) {
            return McpRiskClass.CLASS_B_SCOPED
        }

        // Default to Class A if harmless
        return McpRiskClass.CLASS_A_SAFE
    }

    /**
     * Compares the current cached approved tools against freshly retrieved server tools.
     */
    fun evaluateDiff(
        serverId: String,
        currentTools: List<McpToolDescriptor>,
        freshTools: List<McpToolDescriptor>,
    ): McpDiffResult {
        val currentMap = currentTools.associateBy { it.name }
        val freshMap = freshTools.associateBy { it.name }

        val added = mutableListOf<McpToolDescriptor>()
        val modified = mutableListOf<McpToolDescriptor>()
        val removed = mutableListOf<String>()

        for (fresh in freshTools) {
            val old = currentMap[fresh.name]
            if (old == null) {
                added.add(fresh)
            } else if (old.hash != fresh.hash) {
                modified.add(fresh)
            }
        }

        for (curr in currentTools) {
            if (!freshMap.containsKey(curr.name)) {
                removed.add(curr.name)
            }
        }

        var maxRisk = McpRiskClass.CLASS_A_SAFE
        for (tool in added + modified) {
            if (tool.riskClass == McpRiskClass.CLASS_C_QUARANTINE) {
                maxRisk = McpRiskClass.CLASS_C_QUARANTINE
                break
            } else if (tool.riskClass == McpRiskClass.CLASS_B_SCOPED) {
                maxRisk = McpRiskClass.CLASS_B_SCOPED
            }
        }

        val requiresApproval = (added.isNotEmpty() || modified.isNotEmpty()) &&
                maxRisk == McpRiskClass.CLASS_C_QUARANTINE

        return McpDiffResult(
            serverId = serverId,
            addedTools = added,
            modifiedTools = modified,
            removedToolNames = removed,
            maxRiskClass = maxRisk,
            requiresApproval = requiresApproval,
        )
    }
}
