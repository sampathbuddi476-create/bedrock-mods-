package com.example.service.exporter

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.BedrockMod
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object BedrockExporter {

    /**
     * Generates a 16x16 pixel-art PNG bitmap texture with distinct item/sword/block styling.
     */
    fun generatePixelArtTexture(name: String, primaryColorHex: String = "#38EF7D"): ByteArray {
        val bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val baseColor = try {
            Color.parseColor(primaryColorHex)
        } catch (_: Exception) {
            Color.rgb(56, 239, 125)
        }

        val highlightColor = Color.argb(
            255,
            minOf(255, Color.red(baseColor) + 60),
            minOf(255, Color.green(baseColor) + 60),
            minOf(255, Color.blue(baseColor) + 60)
        )
        val shadowColor = Color.argb(
            255,
            maxOf(0, Color.red(baseColor) - 60),
            maxOf(0, Color.green(baseColor) - 60),
            maxOf(0, Color.blue(baseColor) - 60)
        )
        val darkOutline = Color.rgb(20, 24, 30)

        val isSword = name.contains("sword", ignoreCase = true) || name.contains("blade", ignoreCase = true)
        val isPickaxe = name.contains("pick", ignoreCase = true) || name.contains("axe", ignoreCase = true)
        val isBlock = name.contains("ore", ignoreCase = true) || name.contains("block", ignoreCase = true)

        if (isBlock) {
            // Draw 16x16 ore block
            for (x in 0..15) {
                for (y in 0..15) {
                    val stoneShade = (110..150).random()
                    paint.color = Color.rgb(stoneShade, stoneShade, stoneShade)
                    canvas.drawPoint(x.toFloat(), y.toFloat(), paint)
                }
            }
            // Add gem veins
            val orePixels = listOf(
                Pair(4, 5), Pair(5, 5), Pair(5, 6),
                Pair(10, 4), Pair(11, 4), Pair(11, 5),
                Pair(7, 10), Pair(8, 10), Pair(8, 11), Pair(9, 11),
                Pair(3, 11), Pair(4, 12)
            )
            for (p in orePixels) {
                paint.color = highlightColor
                canvas.drawPoint(p.first.toFloat(), p.second.toFloat(), paint)
            }
        } else if (isSword) {
            // Diagonal sword blade from bottom-left to top-right
            val handlePixels = listOf(Pair(2, 13), Pair(3, 12), Pair(4, 11))
            val guardPixels = listOf(Pair(3, 10), Pair(4, 10), Pair(5, 10), Pair(5, 11), Pair(5, 12))
            val bladePixels = listOf(
                Pair(5, 9), Pair(6, 8), Pair(7, 7), Pair(8, 6),
                Pair(9, 5), Pair(10, 4), Pair(11, 3), Pair(12, 2), Pair(13, 2), Pair(12, 3)
            )
            val bladeCenter = listOf(
                Pair(6, 9), Pair(7, 8), Pair(8, 7), Pair(9, 6), Pair(10, 5), Pair(11, 4)
            )

            paint.color = Color.rgb(101, 67, 33) // wood handle
            handlePixels.forEach { canvas.drawPoint(it.first.toFloat(), it.second.toFloat(), paint) }

            paint.color = Color.rgb(70, 75, 80) // guard
            guardPixels.forEach { canvas.drawPoint(it.first.toFloat(), it.second.toFloat(), paint) }

            paint.color = baseColor
            bladePixels.forEach { canvas.drawPoint(it.first.toFloat(), it.second.toFloat(), paint) }

            paint.color = highlightColor
            bladeCenter.forEach { canvas.drawPoint(it.first.toFloat(), it.second.toFloat(), paint) }
        } else {
            // Cool mystical gem / wand / item emblem
            for (x in 3..12) {
                for (y in 3..12) {
                    val dist = kotlin.math.hypot((x - 7.5), (y - 7.5))
                    if (dist < 4.5) {
                        paint.color = if (dist < 2.0) highlightColor else baseColor
                        canvas.drawPoint(x.toFloat(), y.toFloat(), paint)
                    } else if (dist < 5.2) {
                        paint.color = darkOutline
                        canvas.drawPoint(x.toFloat(), y.toFloat(), paint)
                    }
                }
            }
        }

        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    /**
     * Builds and writes complete Behavior Pack & Resource Pack files into a target directory.
     */
    fun writeModToFolder(mod: BedrockMod, targetDir: File): Map<String, File> {
        val createdFiles = mutableMapOf<String, File>()

        val bpDir = File(targetDir, "BP_${sanitize(mod.name)}").apply { mkdirs() }
        val rpDir = File(targetDir, "RP_${sanitize(mod.name)}").apply { mkdirs() }

        // 1. Behavior Pack Manifest
        val hasScripts = mod.files.keys.any { it.endsWith(".js") || it.contains("scripts/") }
        val bpManifest = JSONObject().apply {
            put("format_version", 2)
            put("header", JSONObject().apply {
                put("name", "${mod.name} (Behavior)")
                put("description", mod.description)
                put("uuid", mod.bpUuidHeader)
                put("version", JSONArray(listOf(1, 0, 0)))
                put("min_engine_version", JSONArray(mod.minEngineVersion))
            })
            val modules = JSONArray()
            modules.put(JSONObject().apply {
                put("type", "data")
                put("uuid", mod.bpUuidModule)
                put("version", JSONArray(listOf(1, 0, 0)))
            })
            if (hasScripts) {
                modules.put(JSONObject().apply {
                    put("type", "script")
                    put("language", "javascript")
                    put("uuid", java.util.UUID.randomUUID().toString())
                    put("version", JSONArray(listOf(1, 0, 0)))
                    put("entry", "scripts/main.js")
                })
            }
            put("modules", modules)
            val deps = JSONArray()
            deps.put(JSONObject().apply {
                put("uuid", mod.rpUuidHeader)
                put("version", JSONArray(listOf(1, 0, 0)))
            })
            if (hasScripts) {
                deps.put(JSONObject().apply {
                    put("module_name", "@minecraft/server")
                    put("version", "1.11.0")
                })
            }
            put("dependencies", deps)
        }
        val bpManifestFile = File(bpDir, "manifest.json").apply { writeText(bpManifest.toString(2)) }
        createdFiles["BP/manifest.json"] = bpManifestFile

        // 2. Resource Pack Manifest
        val rpManifest = JSONObject().apply {
            put("format_version", 2)
            put("header", JSONObject().apply {
                put("name", "${mod.name} (Resource)")
                put("description", mod.description)
                put("uuid", mod.rpUuidHeader)
                put("version", JSONArray(listOf(1, 0, 0)))
                put("min_engine_version", JSONArray(mod.minEngineVersion))
            })
            val modules = JSONArray()
            modules.put(JSONObject().apply {
                put("type", "resources")
                put("uuid", mod.rpUuidModule)
                put("version", JSONArray(listOf(1, 0, 0)))
            })
            put("modules", modules)
        }
        val rpManifestFile = File(rpDir, "manifest.json").apply { writeText(rpManifest.toString(2)) }
        createdFiles["RP/manifest.json"] = rpManifestFile

        // 3. Write all custom files from the mod
        val itemTextureIds = mutableListOf<String>()
        mod.files.forEach { (relPath, content) ->
            val cleanPath = relPath.trim().replace("\\", "/")
            val fileDest = if (cleanPath.startsWith("rp/") || cleanPath.startsWith("resource_packs/")) {
                val sub = cleanPath.substringAfter("/").substringAfter("/")
                File(rpDir, sub)
            } else if (cleanPath.startsWith("bp/") || cleanPath.startsWith("behavior_packs/")) {
                val sub = cleanPath.substringAfter("/").substringAfter("/")
                File(bpDir, sub)
            } else if (cleanPath.endsWith(".js") || cleanPath.startsWith("scripts/")) {
                File(bpDir, if (cleanPath.startsWith("scripts/")) cleanPath else "scripts/$cleanPath")
            } else if (cleanPath.startsWith("textures/") || cleanPath.startsWith("texts/")) {
                File(rpDir, cleanPath)
            } else {
                // Default behavior file (items, entities, blocks, recipes, etc.)
                File(bpDir, cleanPath)
            }
            fileDest.parentFile?.mkdirs()
            fileDest.writeText(content)
            createdFiles[cleanPath] = fileDest

            if (cleanPath.contains("items/") && cleanPath.endsWith(".json")) {
                val itemId = cleanPath.substringAfterLast("/").substringBeforeLast(".json")
                itemTextureIds.add(itemId)
            }
        }

        // 4. Ensure textures & texture definitions exist
        val texturesItemsDir = File(rpDir, "textures/items").apply { mkdirs() }
        val itemTextureJson = JSONObject().apply {
            put("resource_pack_name", sanitize(mod.name))
            put("texture_name", "atlas.items")
            val textureData = JSONObject()
            if (itemTextureIds.isEmpty()) {
                itemTextureIds.add(sanitize(mod.name).lowercase())
            }
            itemTextureIds.forEach { id ->
                val pngBytes = generatePixelArtTexture(id, mod.primaryColorHex)
                File(texturesItemsDir, "$id.png").writeBytes(pngBytes)
                val texObj = JSONObject().apply {
                    put("textures", "textures/items/$id")
                }
                textureData.put(id, texObj)
            }
            put("texture_data", textureData)
        }
        File(rpDir, "textures/item_texture.json").apply { writeText(itemTextureJson.toString(2)) }

        // Pack icon
        val packIconBytes = generatePixelArtTexture(mod.name, mod.primaryColorHex)
        File(bpDir, "pack_icon.png").writeBytes(packIconBytes)
        File(rpDir, "pack_icon.png").writeBytes(packIconBytes)

        // Texts / lang
        val textsDir = File(rpDir, "texts").apply { mkdirs() }
        val langContent = buildString {
            appendLine("pack.name=${mod.name}")
            appendLine("pack.description=${mod.description}")
            itemTextureIds.forEach { id ->
                val formatted = id.replace("_", " ").split(" ")
                    .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
                appendLine("item.$id.name=$formatted")
            }
        }
        File(textsDir, "en_US.lang").writeText(langContent)

        return createdFiles
    }

    /**
     * Creates a standard .mcaddon file containing both the BP and RP.
     */
    fun createMcaddonFile(context: Context, mod: BedrockMod): File {
        val exportDir = File(context.cacheDir, "mc_exports").apply { mkdirs() }
        val zipFile = File(exportDir, "${sanitize(mod.name)}.mcaddon")
        if (zipFile.exists()) zipFile.delete()

        val tempModDir = File(context.cacheDir, "temp_pack_${System.currentTimeMillis()}").apply { mkdirs() }
        try {
            writeModToFolder(mod, tempModDir)

            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                zipDirectory(tempModDir, tempModDir.path, zos)
            }
        } finally {
            tempModDir.deleteRecursively()
        }

        return zipFile
    }

    private fun zipDirectory(folder: File, basePath: String, zos: ZipOutputStream) {
        val files = folder.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                zipDirectory(file, basePath, zos)
            } else {
                val entryPath = file.path.substring(basePath.length + 1).replace("\\", "/")
                zos.putNextEntry(ZipEntry(entryPath))
                file.inputStream().use { input ->
                    input.copyTo(zos)
                }
                zos.closeEntry()
            }
        }
    }

    /**
     * 1-Tap Open with Minecraft Bedrock (Direct Test Import).
     * If Minecraft Bedrock (com.mojang.minecraftpe) is installed, opens it immediately;
     * otherwise presents the system application chooser.
     */
    fun openInMinecraft(context: Context, mod: BedrockMod): Result<String> {
        return try {
            val mcaddon = createMcaddonFile(context, mod)
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                mcaddon
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/octet-stream")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Check if Minecraft is installed
            val mcPackage = "com.mojang.minecraftpe"
            val isMinecraftInstalled = try {
                context.packageManager.getPackageInfo(mcPackage, 0)
                true
            } catch (_: Exception) {
                false
            }

            if (isMinecraftInstalled) {
                intent.setPackage(mcPackage)
                context.startActivity(intent)
                Result.success("Launching Minecraft Bedrock to auto-import ${mod.name}!")
            } else {
                // Show generic chooser (allows opening with Minecraft or file managers)
                val chooser = Intent.createChooser(intent, "Open / Import Addon in Minecraft").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
                Result.success("Opened chooser for ${mod.name}. Select Minecraft Bedrock to import.")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Share .mcaddon file to other apps or save via Share Sheet.
     */
    fun shareModFile(context: Context, mod: BedrockMod): Intent {
        val mcaddon = createMcaddonFile(context, mod)
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            mcaddon
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "${mod.name} Bedrock Addon")
            putExtra(Intent.EXTRA_TEXT, "Here is the ${mod.name} Bedrock Edition addon created with Bedrock Studio!")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun sanitize(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9_]"), "_").trim('_')
    }
}
