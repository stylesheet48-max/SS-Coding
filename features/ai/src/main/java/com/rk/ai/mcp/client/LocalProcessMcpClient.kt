package com.rk.ai.mcp.client

import android.util.Log
import com.rk.ai.mcp.model.McpServerConfig
import com.rk.ai.mcp.model.McpToolDescriptor
import com.rk.ai.mcp.protocol.McpJsonRpc
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

class LocalProcessMcpClient(
    val config: McpServerConfig,
    private val workingDirectory: File? = null,
) : McpClient {

    override val serverId: String
        get() = config.id

    private val idCounter = AtomicLong(1L)
    private val processMutex = Mutex()
    private var process: Process? = null
    private var reader: BufferedReader? = null
    private var writer: BufferedWriter? = null
    private var idleJob: Job? = null

    override suspend fun connect(): Result<Unit> =
        withContext(Dispatchers.IO) {
            processMutex.withLock {
                runCatching {
                    ensureProcessRunningLocked()
                    val initPayload = McpJsonRpc.buildInitializeRequest(idCounter.getAndIncrement())
                    val responseLine = sendAndReceiveLocked(initPayload)
                    val json = JSONObject(responseLine)
                    if (json.has("error")) {
                        val err = json.getJSONObject("error")
                        throw IllegalStateException("Local MCP init error: ${err.optString("message")}")
                    }
                    Log.i(TAG, "Connected to local MCP server ${config.name} ($serverId)")
                    Unit
                }
            }
        }

    override suspend fun listTools(): Result<List<McpToolDescriptor>> =
        withContext(Dispatchers.IO) {
            processMutex.withLock {
                runCatching {
                    ensureProcessRunningLocked()
                    val listPayload = McpJsonRpc.buildToolsListRequest(idCounter.getAndIncrement())
                    val responseLine = sendAndReceiveLocked(listPayload)
                    val tools = McpJsonRpc.parseToolsListResponse(serverId, responseLine).getOrThrow()
                    Log.i(TAG, "Fetched ${tools.size} tools from local MCP server $serverId")
                    tools
                }
            }
        }

    override suspend fun callTool(name: String, arguments: JSONObject): Result<String> =
        withContext(Dispatchers.IO) {
            processMutex.withLock {
                runCatching {
                    ensureProcessRunningLocked()
                    val callPayload = McpJsonRpc.buildToolCallRequest(idCounter.getAndIncrement(), name, arguments)
                    val responseLine = sendAndReceiveLocked(callPayload)
                    McpJsonRpc.parseToolCallResponse(responseLine).getOrThrow()
                }
            }
        }

    override suspend fun disconnect() =
        withContext(Dispatchers.IO) { processMutex.withLock { terminateProcessLocked() } }

    private fun ensureProcessRunningLocked() {
        if (process != null && isProcessAlive(process)) {
            return
        }

        terminateProcessLocked()

        val commandList = mutableListOf<String>()
        commandList.add(config.urlOrCommand)
        commandList.addAll(config.args)

        val processBuilder = ProcessBuilder(commandList)
        workingDirectory?.let { if (it.exists()) processBuilder.directory(it) }

        val env = processBuilder.environment()
        config.env.forEach { (k, v) -> env[k] = v }

        val proc = processBuilder.start()
        process = proc
        reader = BufferedReader(InputStreamReader(proc.inputStream, Charsets.UTF_8))
        writer = BufferedWriter(OutputStreamWriter(proc.outputStream, Charsets.UTF_8))

        Log.i(TAG, "Started local MCP process for server $serverId: ${commandList.joinToString(" ")}")
    }

    private fun sendAndReceiveLocked(jsonPayload: String): String {
        val w = writer ?: throw IllegalStateException("Process output stream not available")
        val r = reader ?: throw IllegalStateException("Process input stream not available")

        w.write(jsonPayload.replace("\n", " "))
        w.write("\n")
        w.flush()

        val line = r.readLine() ?: throw IllegalStateException("Process closed stream prematurely")
        return line
    }

    private fun isProcessAlive(p: Process?): Boolean {
        if (p == null) return false
        return runCatching {
                p.exitValue()
                false
            }
            .getOrDefault(true)
    }

    private fun terminateProcessLocked() {
        idleJob?.cancel()
        idleJob = null

        runCatching { writer?.close() }
        runCatching { reader?.close() }
        writer = null
        reader = null

        process?.let { p -> runCatching { p.destroy() } }
        process = null
    }

    companion object {
        private const val TAG = "LocalProcessMcpClient"
    }
}
