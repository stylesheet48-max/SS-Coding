package com.rk.ai.mcp.security

import java.security.MessageDigest
import org.json.JSONArray
import org.json.JSONObject

object McpHasher {

    /** Computes a deterministic canonical SHA-256 hash for an MCP tool descriptor. */
    fun hashTool(name: String, description: String, inputSchemaJson: String): String {
        val canonicalSchema = canonicalizeJson(inputSchemaJson)
        val payload = "name:$name|desc:$description|schema:$canonicalSchema"
        return sha256Hex(payload)
    }

    /** Computes a canonical SHA-256 fingerprint for a list of tools representing a server manifest. */
    fun hashServerManifest(tools: List<Pair<String, String>>): String {
        // Sorted by tool name
        val sorted = tools.sortedBy { it.first }
        val payload = sorted.joinToString(";") { "${it.first}:${it.second}" }
        return sha256Hex(payload)
    }

    fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /** Converts a JSON string into a sorted, deterministic canonical JSON string. */
    fun canonicalizeJson(jsonStr: String): String {
        val trimmed = jsonStr.trim()
        if (trimmed.isEmpty() || trimmed == "{}") return "{}"
        return runCatching {
                when {
                    trimmed.startsWith("{") -> {
                        val obj = JSONObject(trimmed)
                        canonicalizeJsonObject(obj)
                    }
                    trimmed.startsWith("[") -> {
                        val arr = JSONArray(trimmed)
                        canonicalizeJsonArray(arr)
                    }
                    else -> trimmed
                }
            }
            .getOrDefault(trimmed)
    }

    private fun canonicalizeJsonObject(obj: JSONObject): String {
        val keys = mutableListOf<String>()
        val it = obj.keys()
        while (it.hasNext()) {
            keys.add(it.next())
        }
        keys.sort()
        val pairs = keys.map { key ->
            val value = obj.get(key)
            val canonicalValue =
                when (value) {
                    is JSONObject -> canonicalizeJsonObject(value)
                    is JSONArray -> canonicalizeJsonArray(value)
                    is String -> JSONObject.quote(value)
                    else -> value.toString()
                }
            "\"$key\":$canonicalValue"
        }
        return "{" + pairs.joinToString(",") + "}"
    }

    private fun canonicalizeJsonArray(arr: JSONArray): String {
        val items = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            val value = arr.get(i)
            val canonicalValue =
                when (value) {
                    is JSONObject -> canonicalizeJsonObject(value)
                    is JSONArray -> canonicalizeJsonArray(value)
                    is String -> JSONObject.quote(value)
                    else -> value.toString()
                }
            items.add(canonicalValue)
        }
        return "[" + items.joinToString(",") + "]"
    }
}
