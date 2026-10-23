package com.example.data.model

import java.util.UUID

data class ModVersionSnapshot(
    val versionTag: String,
    val timestamp: Long = System.currentTimeMillis(),
    val changeNote: String,
    val files: Map<String, String>
)

data class BedrockMod(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val author: String = "Bedrock Studio Creator",
    val version: String = "1.0.0",
    val minEngineVersion: List<Int> = listOf(1, 21, 0),
    val category: String = "Custom Addon",
    val bpUuidHeader: String = UUID.randomUUID().toString(),
    val bpUuidModule: String = UUID.randomUUID().toString(),
    val rpUuidHeader: String = UUID.randomUUID().toString(),
    val rpUuidModule: String = UUID.randomUUID().toString(),
    // relative file path -> text content (e.g., "behavior_packs/items/laser_sword.json")
    val files: Map<String, String> = emptyMap(),
    val primaryColorHex: String = "#38EF7D",
    val iconEmoji: String = "⚔️",
    val versionHistory: List<ModVersionSnapshot> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis()
)

data class ModFileItem(
    val path: String,
    val name: String,
    val type: ModFileType,
    val isDirectory: Boolean = false,
    val content: String = ""
)

enum class ModFileType {
    BP_MANIFEST,
    RP_MANIFEST,
    ITEM_BEHAVIOR,
    ENTITY_BEHAVIOR,
    BLOCK_BEHAVIOR,
    RECIPE,
    SCRIPT_JS,
    ITEM_TEXTURE_DEF,
    TERRAIN_TEXTURE_DEF,
    LANG,
    GENERIC_JSON,
    GENERIC_TEXT
}

data class GroundingSource(
    val title: String,
    val url: String
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: String, // "user", "model", "system"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val modelUsed: String = "gemini-3.5-flash",
    val thinkingProcess: String? = null,
    val groundingSources: List<GroundingSource> = emptyList(),
    val generatedMod: BedrockMod? = null,
    val isVideoAnalysis: Boolean = false,
    val isThinkingExpanded: Boolean = false
)

data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val messages: List<ChatMessage> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis()
)
