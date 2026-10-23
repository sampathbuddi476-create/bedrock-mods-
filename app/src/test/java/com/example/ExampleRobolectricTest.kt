package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.BedrockMod
import com.example.data.repository.OpenSourceModsCatalog
import com.example.data.repository.ModRepository
import com.example.service.exporter.BedrockExporter
import com.example.service.gemini.GeminiService
import com.example.service.validator.BedrockAddonValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Bedrock Studio", appName)
    }

    @Test
    fun `starter templates are loaded in repository`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = ModRepository(context)
        val mods = repo.mods.value
        assertTrue("Repository should preload starter templates", mods.isNotEmpty())
        val laserMod = mods.find { it.name.contains("Laser", ignoreCase = true) }
        assertNotNull(laserMod)
    }

    @Test
    fun `mcaddon zip package generation creates valid file`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val mod = BedrockMod(
            name = "Test Dagger",
            description = "Test sword for Bedrock",
            files = mapOf(
                "items/test_dagger.json" to "{\"format_version\":\"1.20.80\"}"
            )
        )
        val zipFile = BedrockExporter.createMcaddonFile(context, mod)
        assertTrue("mcaddon file should exist", zipFile.exists())
        assertTrue("mcaddon file should have nonzero size", zipFile.length() > 0)
        assertTrue("file extension should be .mcaddon", zipFile.name.endsWith(".mcaddon"))
    }

    @Test
    fun `mod response parser extracts code blocks into bedrock mod`() {
        val prompt = "Create plasma sword"
        val response = """
Here is your weapon:
```json // items/plasma_sword.json
{
  "format_version": "1.20.80",
  "minecraft:item": {
    "description": { "identifier": "studio:plasma_sword" }
  }
}
```
And the script hook:
```javascript // scripts/main.js
import { world } from "@minecraft/server";
```
        """.trimIndent()

        val parsedMod = GeminiService.parseModFromResponse(prompt, response)
        assertNotNull(parsedMod)
        assertEquals(2, parsedMod!!.files.size)
        assertTrue(parsedMod.files.containsKey("items/plasma_sword.json"))
        assertTrue(parsedMod.files.containsKey("scripts/main.js"))
    }

    @Test
    fun `validation layer detects invalid uuid and malformed manifest`() {
        val invalidMod = BedrockMod(
            name = "", // empty name
            description = "Invalid mod",
            bpUuidHeader = "not-a-valid-uuid",
            bpUuidModule = "duplicate-uuid",
            rpUuidHeader = "duplicate-uuid", // duplicate
            rpUuidModule = "also-invalid",
            minEngineVersion = emptyList(), // invalid version
            files = emptyMap() // empty files
        )

        val result = BedrockAddonValidator.validate(invalidMod)
        assertFalse("Mod should fail validation", result.isValid)
        assertTrue("Should contain errors", result.errors.isNotEmpty())

        // Auto-fix should repair the issues
        val fixedMod = BedrockAddonValidator.autoFix(invalidMod)
        assertEquals("Custom Bedrock Addon", fixedMod.name)
        assertTrue("UUID should be valid", fixedMod.bpUuidHeader.length == 36)
        assertEquals(listOf(1, 21, 0), fixedMod.minEngineVersion)
    }

    @Test
    fun `open source mods catalog contains verified addons ready for direct download`() {
        val mods = OpenSourceModsCatalog.curatedMods
        assertTrue("Open source mods catalog should have curated addons", mods.isNotEmpty())
        val weapons = mods.find { it.category == "Weapons" }
        assertNotNull("Weapons mod should exist", weapons)
        assertTrue("License should be open source", weapons!!.license.isNotEmpty())
        assertTrue("Should have files in template mod", weapons.templateMod.files.isNotEmpty())
    }
}
