package com.example.service.gemini

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.BedrockMod
import com.example.data.model.ChatMessage
import com.example.data.model.GroundingSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"

    val MODEL_PRO = "gemini-3.1-pro-preview"
    val MODEL_FLASH = "gemini-3.5-flash"
    val MODEL_LITE = "gemini-3.1-flash-lite-preview"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private const val SYSTEM_PROMPT = """
You are BedrockCraft AI, an elite Minecraft Bedrock Edition Modding Engineer & Addon Architect.
You specialize in Minecraft Bedrock (v1.20 - v1.21+):
- Behavior Packs (BP): items, entities, blocks, recipes, loot tables, trade tables.
- Resource Packs (RP): manifests, textures, models, attachables, sound_definitions, lang files.
- Bedrock Script API (@minecraft/server): modern event hooks (world.afterEvents, system.runInterval, customComponents, itemUse, entityHit).
- Always ensure valid UUIDs, format_version 1.20.80 or 1.21.0, and proper Mojang schema compliance.

When the user asks you to create or modify a mod/addon:
1. Provide an inspiring explanation of what the mod does and how it plays in Bedrock Edition.
2. Put each file in a distinct Markdown code block with the relative file path in the first line comment or tag, like:
```json // items/hyper_blade.json
{
  "format_version": "1.20.80",
  "minecraft:item": { ... }
}
```
```javascript // scripts/main.js
import { world, system } from "@minecraft/server";
...
```
3. When searching or grounding, provide verified Bedrock documentation and component names.
"""

    /**
     * Executes a chat turn with full multi-turn history, optional thinking, search grounding, or video input.
     */
    suspend fun sendChat(
        history: List<ChatMessage>,
        userPrompt: String,
        modelName: String = MODEL_FLASH,
        enableHighThinking: Boolean = false,
        enableSearchGrounding: Boolean = false,
        videoBase64: String? = null,
        videoMimeType: String = "video/mp4"
    ): ChatResponseResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext ChatResponseResult(
                text = "⚠️ Gemini API Key not configured. Please add your key in the AI Studio Secrets panel.\n\nYou can still use the built-in Bedrock Mod Builder templates and export mods directly to Minecraft!",
                generatedMod = null,
                groundingSources = emptyList(),
                thinking = null
            )
        }

        try {
            val root = JSONObject()

            // System instruction
            val systemInstruction = JSONObject().apply {
                val partsArray = JSONArray().apply {
                    put(JSONObject().apply { put("text", SYSTEM_PROMPT) })
                }
                put("parts", partsArray)
            }
            root.put("systemInstruction", systemInstruction)

            // Conversation history contents
            val contentsArray = JSONArray()
            // Append past history turns (limit to last 10 to keep within token bounds)
            val recentHistory = history.takeLast(10)
            for (msg in recentHistory) {
                if (msg.role == "system") continue
                val role = if (msg.role == "user") "user" else "model"
                val turnObj = JSONObject().apply {
                    put("role", role)
                    val parts = JSONArray().apply {
                        put(JSONObject().apply { put("text", msg.content) })
                    }
                    put("parts", parts)
                }
                contentsArray.put(turnObj)
            }

            // Current user turn
            val currentTurn = JSONObject().apply {
                put("role", "user")
                val parts = JSONArray()
                // If video attachment provided
                if (!videoBase64.isNullOrEmpty()) {
                    val inlineData = JSONObject().apply {
                        put("mimeType", videoMimeType)
                        put("data", videoBase64)
                    }
                    parts.put(JSONObject().apply { put("inlineData", inlineData) })
                }
                parts.put(JSONObject().apply { put("text", userPrompt) })
                put("parts", parts)
            }
            contentsArray.put(currentTurn)
            root.put("contents", contentsArray)

            // Generation config
            val genConfig = JSONObject()
            if (enableHighThinking && modelName == MODEL_PRO) {
                val thinkingConfig = JSONObject().apply {
                    put("thinkingLevel", "HIGH")
                }
                genConfig.put("thinkingConfig", thinkingConfig)
                // Note: Do NOT set maxOutputTokens when using thinking
            } else {
                genConfig.put("temperature", 0.7)
            }
            root.put("generationConfig", genConfig)

            // Search Grounding Tools (applicable for gemini-3.5-flash)
            if (enableSearchGrounding) {
                val toolsArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("googleSearch", JSONObject())
                    })
                }
                root.put("tools", toolsArray)
            }

            val requestBody = root.toString().toRequestBody("application/json".toMediaType())
            val targetUrl = "$BASE_URL$modelName:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(targetUrl)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val rawResponse = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini API error ($modelName): ${response.code} $rawResponse")
                return@withContext ChatResponseResult(
                    text = "Gemini API Error (${response.code}): $rawResponse",
                    generatedMod = null,
                    groundingSources = emptyList(),
                    thinking = null
                )
            }

            val respJson = JSONObject(rawResponse)
            val candidates = respJson.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val contentObj = candidate?.optJSONObject("content")
            val parts = contentObj?.optJSONArray("parts")

            var responseText = ""
            var thinkingText: String? = null

            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val part = parts.optJSONObject(i) ?: continue
                    if (part.optBoolean("thought", false)) {
                        thinkingText = part.optString("text")
                    } else if (part.has("text")) {
                        responseText += part.optString("text")
                    }
                }
            }

            // Extract Google Search Grounding sources
            val sources = mutableListOf<GroundingSource>()
            val groundingMetadata = candidate?.optJSONObject("groundingMetadata")
            if (groundingMetadata != null) {
                val searchChunks = groundingMetadata.optJSONArray("groundingChunks")
                if (searchChunks != null) {
                    for (i in 0 until searchChunks.length()) {
                        val chunk = searchChunks.optJSONObject(i) ?: continue
                        val web = chunk.optJSONObject("web")
                        if (web != null) {
                            val title = web.optString("title", "Bedrock Source")
                            val uri = web.optString("uri", "")
                            if (uri.isNotEmpty()) {
                                sources.add(GroundingSource(title = title, url = uri))
                            }
                        }
                    }
                }
            }

            // Attempt to parse out Bedrock files from response text
            val detectedMod = parseModFromResponse(userPrompt, responseText)

            ChatResponseResult(
                text = responseText.ifEmpty { "Received empty response from $modelName." },
                generatedMod = detectedMod,
                groundingSources = sources,
                thinking = thinkingText
            )
        } catch (e: Exception) {
            Log.e(TAG, "Chat request exception", e)
            ChatResponseResult(
                text = "Error connecting to Gemini: ${e.localizedMessage ?: e.message}",
                generatedMod = null,
                groundingSources = emptyList(),
                thinking = null
            )
        }
    }

    /**
     * Parses Markdown code blocks in the response to assemble a ready-to-test BedrockMod project.
     */
    fun parseModFromResponse(userPrompt: String, responseText: String): BedrockMod? {
        val files = mutableMapOf<String, String>()
        val codeBlockRegex = Regex("```(?:[a-zA-Z0-9_-]+)?(?:[ /]+([^\\n\\r]+))?\\r?\\n([\\s\\S]*?)```")

        val matches = codeBlockRegex.findAll(responseText).toList()
        if (matches.isEmpty()) return null

        var fileIndex = 1
        var modName = "Custom Addon"
        var emoji = "⚡"

        for (match in matches) {
            val header = match.groups[1]?.value?.trim() ?: ""
            val code = match.groups[2]?.value?.trim() ?: ""

            // Extract file path from header comment like "// items/laser_sword.json" or "items/sword.json"
            var cleanPath = header.removePrefix("//").removePrefix("/*").removePrefix("#").trim()
            if (cleanPath.isEmpty() || !cleanPath.contains(".")) {
                if (code.contains("\"minecraft:item\"")) {
                    cleanPath = "items/custom_item_$fileIndex.json"
                } else if (code.contains("\"minecraft:entity\"")) {
                    cleanPath = "entities/custom_entity_$fileIndex.json"
                } else if (code.contains("\"minecraft:block\"")) {
                    cleanPath = "blocks/custom_block_$fileIndex.json"
                } else if (code.contains("\"minecraft:recipe_shaped\"") || code.contains("\"minecraft:recipe_shapeless\"")) {
                    cleanPath = "recipes/custom_recipe_$fileIndex.json"
                } else if (code.contains("@minecraft/server") || code.contains("world.afterEvents")) {
                    cleanPath = "scripts/main.js"
                } else {
                    cleanPath = "files/file_$fileIndex.json"
                }
            }

            files[cleanPath] = code
            fileIndex++
        }

        if (files.isEmpty()) return null

        // Derive name from prompt or files
        val firstItemPath = files.keys.firstOrNull { it.contains("items/") || it.contains("entities/") || it.contains("blocks/") }
        if (firstItemPath != null) {
            val raw = firstItemPath.substringAfterLast("/").substringBeforeLast(".")
            modName = raw.replace("_", " ").split(" ")
                .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
            if (firstItemPath.contains("items/")) emoji = "⚔️"
            if (firstItemPath.contains("entities/")) emoji = "🐉"
            if (firstItemPath.contains("blocks/")) emoji = "🧱"
        } else {
            modName = userPrompt.take(24).trim().replaceFirstChar { it.uppercase() }
        }

        return BedrockMod(
            name = modName,
            description = "Generated with Gemini from prompt: \"${userPrompt.take(60)}\"",
            files = files,
            iconEmoji = emoji,
            primaryColorHex = "#38EF7D"
        )
    }
}

data class ChatResponseResult(
    val text: String,
    val generatedMod: BedrockMod?,
    val groundingSources: List<GroundingSource>,
    val thinking: String?
)
