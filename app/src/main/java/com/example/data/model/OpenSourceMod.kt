package com.example.data.model

data class OpenSourceMod(
    val id: String,
    val name: String,
    val author: String,
    val category: String,
    val description: String,
    val license: String,
    val repositoryUrl: String,
    val version: String = "1.2.0",
    val compatibleVersions: String = "1.20 - 1.21+",
    val downloadCount: String = "120K+",
    val rating: Double = 4.9,
    val iconEmoji: String = "⚔️",
    val primaryColorHex: String = "#38EF7D",
    val sizeText: String = "1.2 MB",
    val tags: List<String> = emptyList(),
    val features: List<String> = emptyList(),
    val templateMod: BedrockMod
)
