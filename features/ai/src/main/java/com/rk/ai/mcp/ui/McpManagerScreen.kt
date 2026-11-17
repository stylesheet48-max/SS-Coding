package com.rk.ai.mcp.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rk.ai.mcp.engine.McpRegistry
import com.rk.ai.mcp.engine.McpStartupOrchestrator
import com.rk.ai.mcp.model.McpDiffResult
import com.rk.ai.mcp.model.McpRiskClass
import com.rk.ai.mcp.model.McpServerConfig
import com.rk.ai.mcp.model.McpServerStatus
import com.rk.ai.mcp.model.McpServerType
import com.rk.ai.mcp.model.McpToolDescriptor
import com.rk.ai.mcp.security.McpSecureVault
import com.rk.components.SettingsItem
import com.rk.components.compose.preferences.base.PreferenceGroup
import com.rk.components.compose.preferences.base.PreferenceLayout
import com.rk.resources.drawables

@Composable
fun McpManagerScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    val servers by McpRegistry.servers.collectAsState()
    val approvedTools by McpRegistry.approvedTools.collectAsState()
    val pendingApprovals by McpRegistry.pendingApprovals.collectAsState()
    val isRefreshing by McpStartupOrchestrator.isRefreshing.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedDiffForReview by remember { mutableStateOf<McpDiffResult?>(null) }
    var selectedServerForTools by remember { mutableStateOf<McpServerConfig?>(null) }
    var serverToDelete by remember { mutableStateOf<McpServerConfig?>(null) }

    val infiniteTransition = rememberInfiniteTransition(label = "refresh_rotation")
    val rotationAngle by
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(animation = tween(1000, easing = LinearEasing)),
            label = "angle",
        )

    PreferenceLayout(
        label = "MCP Management",
        modifier = modifier,
        actions = {
            IconButton(
                onClick = { McpStartupOrchestrator.refreshAll() },
                enabled = !isRefreshing,
            ) {
                Icon(
                    painter = painterResource(id = drawables.refresh),
                    contentDescription = "Refresh",
                    modifier = if (isRefreshing) Modifier.rotate(rotationAngle) else Modifier,
                )
            }
            IconButton(onClick = { showAddDialog = true }) {
                Icon(
                    painter = painterResource(id = drawables.add),
                    contentDescription = "Add MCP Server",
                )
            }
        },
    ) {
        // Pending Security Approvals (Quarantine Banner)
        if (pendingApprovals.isNotEmpty()) {
            PreferenceGroup(heading = "Security Quarantine (${pendingApprovals.size})") {
                pendingApprovals.forEach { diff ->
                    QuarantineWarningCard(
                        diff = diff,
                        onReview = { selectedDiffForReview = diff },
                    )
                }
            }
        }

        // Configured Servers Section
        PreferenceGroup(heading = "MCP Servers (${servers.size})") {
            if (servers.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "No MCP Servers Configured",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text =
                                "Connect Local or Remote MCP servers to expand AI coding tools with automated security gates.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(onClick = { showAddDialog = true }) {
                            Text("Add MCP Server", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                servers.forEach { server ->
                    val serverTools = approvedTools[server.id] ?: emptyList()
                    McpServerCard(
                        server = server,
                        toolCount = serverTools.size,
                        onToggleEnabled = { enabled -> McpRegistry.saveServer(server.copy(enabled = enabled)) },
                        onViewTools = { selectedServerForTools = server },
                        onDelete = { serverToDelete = server },
                    )
                }
            }
        }

        // Storage & Protocol Summary Group
        PreferenceGroup(heading = "Architecture & Storage Enclave") {
            SettingsItem(
                label = "Internal Storage Enclave",
                description = "context.filesDir/mcp/ (250 MB quota cap)",
                showSwitch = false,
                isEnabled = false,
            )
            SettingsItem(
                label = "Hardware Keystore Vault",
                description = "AES-256-GCM hardware-backed credential encryption",
                showSwitch = false,
                isEnabled = false,
            )
            SettingsItem(
                label = "Startup Update Protocol",
                description = "Non-blocking background check (5s timeout)",
                showSwitch = false,
                isEnabled = false,
            )
        }
    }

    // Add Server Dialog
    if (showAddDialog) {
        AddMcpServerDialog(
            onDismiss = { showAddDialog = false },
            onAddServer = { config, tokenSecret ->
                McpRegistry.saveServer(config)
                if (!tokenSecret.isNullOrBlank() && config.bearerTokenSecretKey != null) {
                    McpSecureVault.saveSecret(config.bearerTokenSecretKey, tokenSecret)
                }
                showAddDialog = false
                McpStartupOrchestrator.refreshAll()
            },
        )
    }

    // Quarantine Review Dialog
    selectedDiffForReview?.let { diff ->
        QuarantineReviewDialog(
            diff = diff,
            onDismiss = { selectedDiffForReview = null },
            onApprove = {
                McpStartupOrchestrator.approveQuarantinedServer(diff.serverId)
                selectedDiffForReview = null
            },
            onReject = {
                McpStartupOrchestrator.rejectQuarantinedServer(diff.serverId)
                selectedDiffForReview = null
            },
        )
    }

    // Server Tools Dialog
    selectedServerForTools?.let { server ->
        val tools = approvedTools[server.id] ?: emptyList()
        ServerToolsDialog(
            server = server,
            tools = tools,
            onDismiss = { selectedServerForTools = null },
        )
    }

    // Delete Confirmation Dialog
    serverToDelete?.let { server ->
        AlertDialog(
            onDismissRequest = { serverToDelete = null },
            title = { Text("Delete MCP Server?") },
            text = {
                Text(
                    "Are you sure you want to delete '${server.name}'? All associated cached tools and credentials will be removed."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        McpRegistry.removeServer(server.id)
                        serverToDelete = null
                    }
                ) {
                    Text("Delete", color = Color(0xFFEF4444))
                }
            },
            dismissButton = { TextButton(onClick = { serverToDelete = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun QuarantineWarningCard(
    diff: McpDiffResult,
    onReview: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFF59E0B)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = "Quarantine Alert",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Class C Permission Detected",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF92400E),
                    )
                    Text(
                        text = "Server: ${diff.serverId} (+${diff.addedTools.size} new tools)",
                        fontSize = 12.sp,
                        color = Color(0xFFB45309),
                    )
                }
            }
            TextButton(
                onClick = onReview,
                shape = RoundedCornerShape(8.dp),
            ) {
                Text("Review", fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
            }
        }
    }
}

@Composable
private fun McpServerCard(
    server: McpServerConfig,
    toolCount: Int,
    onToggleEnabled: (Boolean) -> Unit,
    onViewTools: () -> Unit,
    onDelete: () -> Unit,
) {
    val accentColor =
        when (server.type) {
            McpServerType.REMOTE -> Color(0xFF2563EB) // Royal Blue
            McpServerType.LOCAL -> Color(0xFF7C3AED) // Deep Violet
        }

    val statusColor =
        when (server.status) {
            McpServerStatus.CONNECTED -> Color(0xFF10B981) // Green
            McpServerStatus.CONNECTING -> Color(0xFF2563EB) // Blue
            McpServerStatus.QUARANTINED -> Color(0xFFF59E0B) // Amber
            McpServerStatus.OFFLINE -> Color(0xFF64748B) // Slate Gray
            McpServerStatus.ERROR -> Color(0xFFEF4444) // Red
            McpServerStatus.DISCONNECTED -> Color(0xFF94A3B8)
        }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Squircle container
                    Box(
                        modifier =
                            Modifier.size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(accentColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (server.type == McpServerType.REMOTE) "R" else "L",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = accentColor,
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = server.name,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = server.urlOrCommand,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
                Switch(
                    checked = server.enabled,
                    onCheckedChange = onToggleEnabled,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Status Pill
                Row(
                    modifier =
                        Modifier.clip(RoundedCornerShape(12.dp))
                            .background(statusColor.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(statusColor))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${server.status.name} ($toolCount tools)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = statusColor,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onViewTools) { Text("Tools", fontSize = 13.sp) }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddMcpServerDialog(
    onDismiss: () -> Unit,
    onAddServer: (McpServerConfig, String?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(McpServerType.REMOTE) }
    var urlOrCommand by remember { mutableStateOf("") }
    var bearerToken by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add MCP Server", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Server Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Server Type", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = type == McpServerType.REMOTE,
                        onClick = { type = McpServerType.REMOTE },
                    )
                    Text("Remote (HTTP/SSE)", fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    RadioButton(
                        selected = type == McpServerType.LOCAL,
                        onClick = { type = McpServerType.LOCAL },
                    )
                    Text("Local (stdio)", fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = urlOrCommand,
                    onValueChange = { urlOrCommand = it },
                    label = {
                        Text(if (type == McpServerType.REMOTE) "Endpoint URL (https://...)" else "Command / Executable")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                if (type == McpServerType.REMOTE) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = bearerToken,
                        onValueChange = { bearerToken = it },
                        label = { Text("Bearer Token (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && urlOrCommand.isNotBlank(),
                onClick = {
                    val id = name.trim().lowercase().replace(Regex("[^a-z0-9_]"), "_")
                    val secretKey = if (bearerToken.isNotBlank()) "mcp_token_$id" else null
                    val config =
                        McpServerConfig(
                            id = id,
                            name = name.trim(),
                            type = type,
                            urlOrCommand = urlOrCommand.trim(),
                            bearerTokenSecretKey = secretKey,
                            enabled = true,
                        )
                    onAddServer(config, bearerToken.trim().ifBlank { null })
                },
            ) {
                Text("Add Server", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun QuarantineReviewDialog(
    diff: McpDiffResult,
    onDismiss: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Quarantine Review", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Text(
                    text =
                        "Server '${diff.serverId}' is introducing High-Risk (Class C) tools. Review and approve before execution is permitted.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))

                (diff.addedTools + diff.modifiedTools).forEach { tool ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = tool.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                )
                                Text(
                                    text = tool.riskClass.name,
                                    color = Color(0xFFEF4444),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = tool.description, fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onApprove) {
                Text("Approve & Unquarantine", color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onReject) { Text("Reject Update", color = Color(0xFFEF4444)) } },
    )
}

@Composable
private fun ServerToolsDialog(
    server: McpServerConfig,
    tools: List<McpToolDescriptor>,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${server.name} — Registered Tools (${tools.size})", fontWeight = FontWeight.Bold) },
        text = {
            if (tools.isEmpty()) {
                Text("No active tools registered for this server.")
            } else {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    tools.forEach { tool ->
                        val riskColor =
                            when (tool.riskClass) {
                                McpRiskClass.CLASS_A_SAFE -> Color(0xFF10B981)
                                McpRiskClass.CLASS_B_SCOPED -> Color(0xFF2563EB)
                                McpRiskClass.CLASS_C_QUARANTINE -> Color(0xFFEF4444)
                            }
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors =
                                CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = tool.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace,
                                    )
                                    Text(
                                        text = tool.riskClass.name,
                                        color = riskColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = tool.description,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
