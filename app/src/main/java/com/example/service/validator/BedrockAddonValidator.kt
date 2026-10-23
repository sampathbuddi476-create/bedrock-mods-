package com.example.service.validator

import com.example.data.model.BedrockMod
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class IssueSeverity {
    ERROR,   // Blocks export
    WARNING, // Advisory notice
    INFO     // Good practice info
}

data class ValidationIssue(
    val severity: IssueSeverity,
    val title: String,
    val description: String,
    val fileTarget: String = "manifest.json",
    val canAutoFix: Boolean = true
)

data class ValidationResult(
    val isValid: Boolean,
    val issues: List<ValidationIssue>,
    val passedChecks: List<String>
) {
    val errors: List<ValidationIssue> get() = issues.filter { it.severity == IssueSeverity.ERROR }
    val warnings: List<ValidationIssue> get() = issues.filter { it.severity == IssueSeverity.WARNING }
}

object BedrockAddonValidator {

    private val UUID_REGEX = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

    /**
     * Validates an entire Bedrock mod for Mojang standard manifest and file structure requirements.
     */
    fun validate(mod: BedrockMod): ValidationResult {
        val issues = mutableListOf<ValidationIssue>()
        val passedChecks = mutableListOf<String>()

        // 1. Mod Name & Description Check
        if (mod.name.isBlank()) {
            issues.add(
                ValidationIssue(
                    severity = IssueSeverity.ERROR,
                    title = "Missing Addon Name",
                    description = "The addon must have a non-empty name for the Minecraft manifest header.",
                    fileTarget = "manifest.json"
                )
            )
        } else {
            passedChecks.add("Addon name defined: \"${mod.name}\"")
        }

        // 2. UUID Validation
        val uuids = listOf(
            "BP Header" to mod.bpUuidHeader,
            "BP Module" to mod.bpUuidModule,
            "RP Header" to mod.rpUuidHeader,
            "RP Module" to mod.rpUuidModule
        )

        val seenUuids = mutableSetOf<String>()
        var uuidErrors = false
        for ((label, uuid) in uuids) {
            if (!UUID_REGEX.matches(uuid)) {
                uuidErrors = true
                issues.add(
                    ValidationIssue(
                        severity = IssueSeverity.ERROR,
                        title = "Invalid $label UUID",
                        description = "UUID \"$uuid\" does not adhere to RFC 4122 format. Minecraft requires standard 8-4-4-4-12 hex UUIDs.",
                        fileTarget = "manifest.json"
                    )
                )
            } else if (seenUuids.contains(uuid.lowercase())) {
                uuidErrors = true
                issues.add(
                    ValidationIssue(
                        severity = IssueSeverity.ERROR,
                        title = "Duplicate UUID ($label)",
                        description = "Minecraft Bedrock requires distinct UUIDs for the header and modules. \"$uuid\" is reused.",
                        fileTarget = "manifest.json"
                    )
                )
            } else {
                seenUuids.add(uuid.lowercase())
            }
        }
        if (!uuidErrors) {
            passedChecks.add("All 4 Pack UUIDs are valid and distinct RFC 4122 identifiers")
        }

        // 3. Minimum Engine Version
        if (mod.minEngineVersion.size < 3 || mod.minEngineVersion[0] < 1) {
            issues.add(
                ValidationIssue(
                    severity = IssueSeverity.ERROR,
                    title = "Invalid min_engine_version",
                    description = "min_engine_version must be a 3-part integer array such as [1, 21, 0].",
                    fileTarget = "manifest.json"
                )
            )
        } else {
            passedChecks.add("min_engine_version is [${mod.minEngineVersion.joinToString(", ")}]")
        }

        // 4. File Structure & Component Checks
        if (mod.files.isEmpty()) {
            issues.add(
                ValidationIssue(
                    severity = IssueSeverity.ERROR,
                    title = "No Addon Content Files",
                    description = "The addon contains no component, entity, script, or item definitions.",
                    fileTarget = "root"
                )
            )
        } else {
            passedChecks.add("Addon contains ${mod.files.size} definition file(s)")
        }

        var hasItem = false
        var hasEntity = false
        var hasBlock = false
        var hasScript = false

        for ((path, content) in mod.files) {
            val cleanPath = path.trim().replace("\\", "/")

            // Check JSON syntax
            if (cleanPath.endsWith(".json")) {
                try {
                    val json = JSONObject(content)

                    if (cleanPath.contains("items/")) {
                        hasItem = true
                        val itemObj = json.optJSONObject("minecraft:item")
                        if (itemObj == null) {
                            issues.add(
                                ValidationIssue(
                                    severity = IssueSeverity.WARNING,
                                    title = "Missing minecraft:item component in $cleanPath",
                                    description = "Bedrock item files should declare \"minecraft:item\" root object.",
                                    fileTarget = cleanPath
                                )
                            )
                        } else {
                            val desc = itemObj.optJSONObject("description")
                            val id = desc?.optString("identifier")
                            if (id.isNullOrBlank() || !id.contains(":")) {
                                issues.add(
                                    ValidationIssue(
                                        severity = IssueSeverity.WARNING,
                                        title = "Item identifier should include namespace in $cleanPath",
                                        description = "Recommended format is \"namespace:item_name\" (e.g., \"studio:sword\").",
                                        fileTarget = cleanPath
                                    )
                                )
                            }
                        }
                    }

                    if (cleanPath.contains("entities/")) {
                        hasEntity = true
                        val entityObj = json.optJSONObject("minecraft:entity")
                        if (entityObj == null) {
                            issues.add(
                                ValidationIssue(
                                    severity = IssueSeverity.WARNING,
                                    title = "Missing minecraft:entity in $cleanPath",
                                    description = "Bedrock entity files should declare \"minecraft:entity\" root object.",
                                    fileTarget = cleanPath
                                )
                            )
                        }
                    }

                    if (cleanPath.contains("blocks/")) {
                        hasBlock = true
                        val blockObj = json.optJSONObject("minecraft:block")
                        if (blockObj == null) {
                            issues.add(
                                ValidationIssue(
                                    severity = IssueSeverity.WARNING,
                                    title = "Missing minecraft:block in $cleanPath",
                                    description = "Bedrock block files should declare \"minecraft:block\" root object.",
                                    fileTarget = cleanPath
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    issues.add(
                        ValidationIssue(
                            severity = IssueSeverity.ERROR,
                            title = "Malformed JSON in $cleanPath",
                            description = "JSON parse error: ${e.message}",
                            fileTarget = cleanPath,
                            canAutoFix = false
                        )
                    )
                }
            } else if (cleanPath.endsWith(".js") || cleanPath.startsWith("scripts/")) {
                hasScript = true
                if (!content.contains("@minecraft/server") && !content.contains("world.")) {
                    issues.add(
                        ValidationIssue(
                            severity = IssueSeverity.INFO,
                            title = "Script API Hook Notice in $cleanPath",
                            description = "Make sure your script imports '@minecraft/server' to interact with the game world.",
                            fileTarget = cleanPath
                        )
                    )
                }
            }
        }

        if (hasItem) passedChecks.add("Custom items verified")
        if (hasEntity) passedChecks.add("Custom entities verified")
        if (hasBlock) passedChecks.add("Custom blocks verified")
        if (hasScript) passedChecks.add("Bedrock Script API module verified")

        val hasErrors = issues.any { it.severity == IssueSeverity.ERROR }
        return ValidationResult(
            isValid = !hasErrors,
            issues = issues,
            passedChecks = passedChecks
        )
    }

    /**
     * Automatically repairs common manifest issues (fixes invalid or duplicate UUIDs,
     * repairs min_engine_version, normalizes missing names).
     */
    fun autoFix(mod: BedrockMod): BedrockMod {
        var fixedBpHeader = mod.bpUuidHeader
        var fixedBpModule = mod.bpUuidModule
        var fixedRpHeader = mod.rpUuidHeader
        var fixedRpModule = mod.rpUuidModule

        val used = mutableSetOf<String>()

        fun validOrNew(uuid: String): String {
            return if (UUID_REGEX.matches(uuid) && !used.contains(uuid.lowercase())) {
                used.add(uuid.lowercase())
                uuid
            } else {
                val newUuid = UUID.randomUUID().toString()
                used.add(newUuid.lowercase())
                newUuid
            }
        }

        fixedBpHeader = validOrNew(fixedBpHeader)
        fixedBpModule = validOrNew(fixedBpModule)
        fixedRpHeader = validOrNew(fixedRpHeader)
        fixedRpModule = validOrNew(fixedRpModule)

        val fixedEngine = if (mod.minEngineVersion.size < 3 || mod.minEngineVersion[0] < 1) {
            listOf(1, 21, 0)
        } else {
            mod.minEngineVersion
        }

        val fixedName = if (mod.name.isBlank()) "Custom Bedrock Addon" else mod.name
        val fixedDesc = if (mod.description.isBlank()) "Created with Bedrock Studio" else mod.description

        return mod.copy(
            name = fixedName,
            description = fixedDesc,
            bpUuidHeader = fixedBpHeader,
            bpUuidModule = fixedBpModule,
            rpUuidHeader = fixedRpHeader,
            rpUuidModule = fixedRpModule,
            minEngineVersion = fixedEngine,
            lastModified = System.currentTimeMillis()
        )
    }
}
