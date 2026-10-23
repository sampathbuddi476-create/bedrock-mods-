package com.example.data.repository

import android.content.Context
import com.example.data.model.BedrockMod
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSession
import com.example.data.model.GroundingSource
import com.example.data.model.ModVersionSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class ModRepository(private val context: Context) {

    private val _mods = MutableStateFlow<List<BedrockMod>>(emptyList())
    val mods: StateFlow<List<BedrockMod>> = _mods.asStateFlow()

    private val _chatSessions = MutableStateFlow<List<ChatSession>>(emptyList())
    val chatSessions: StateFlow<List<ChatSession>> = _chatSessions.asStateFlow()

    private val storageFile = File(context.filesDir, "saved_bedrock_mods.json")
    private val sessionsFile = File(context.filesDir, "chat_sessions.json")

    init {
        loadMods()
        loadChatSessions()
    }

    private fun loadMods() {
        val loaded = mutableListOf<BedrockMod>()
        if (storageFile.exists()) {
            try {
                val jsonStr = storageFile.readText()
                val jsonArray = JSONArray(jsonStr)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val filesMap = mutableMapOf<String, String>()
                    val filesObj = obj.optJSONObject("files")
                    if (filesObj != null) {
                        for (key in filesObj.keys()) {
                            filesMap[key] = filesObj.getString(key)
                        }
                    }

                    // Load version history snapshots
                    val snapshots = mutableListOf<ModVersionSnapshot>()
                    val historyArr = obj.optJSONArray("versionHistory")
                    if (historyArr != null) {
                        for (j in 0 until historyArr.length()) {
                            val snapObj = historyArr.getJSONObject(j)
                            val snapFiles = mutableMapOf<String, String>()
                            val snapFilesObj = snapObj.optJSONObject("files")
                            if (snapFilesObj != null) {
                                for (k in snapFilesObj.keys()) {
                                    snapFiles[k] = snapFilesObj.getString(k)
                                }
                            }
                            snapshots.add(
                                ModVersionSnapshot(
                                    versionTag = snapObj.optString("versionTag", "v1.0.0"),
                                    timestamp = snapObj.optLong("timestamp", System.currentTimeMillis()),
                                    changeNote = snapObj.optString("changeNote", "Checkpoint"),
                                    files = snapFiles
                                )
                            )
                        }
                    }

                    loaded.add(
                        BedrockMod(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            description = obj.getString("description"),
                            author = obj.optString("author", "Bedrock Studio"),
                            version = obj.optString("version", "1.0.0"),
                            category = obj.optString("category", "Custom Addon"),
                            bpUuidHeader = obj.optString("bpUuidHeader", UUID.randomUUID().toString()),
                            bpUuidModule = obj.optString("bpUuidModule", UUID.randomUUID().toString()),
                            rpUuidHeader = obj.optString("rpUuidHeader", UUID.randomUUID().toString()),
                            rpUuidModule = obj.optString("rpUuidModule", UUID.randomUUID().toString()),
                            files = filesMap,
                            primaryColorHex = obj.optString("primaryColorHex", "#38EF7D"),
                            iconEmoji = obj.optString("iconEmoji", "⚔️"),
                            versionHistory = snapshots,
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            lastModified = obj.optLong("lastModified", System.currentTimeMillis())
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (loaded.isEmpty()) {
            val starters = getStarterTemplates()
            _mods.value = starters
            saveToFile(starters)
        } else {
            _mods.value = loaded
        }
    }

    fun saveMod(mod: BedrockMod) {
        val current = _mods.value.toMutableList()
        val index = current.indexOfFirst { it.id == mod.id }
        if (index >= 0) {
            current[index] = mod.copy(lastModified = System.currentTimeMillis())
        } else {
            current.add(0, mod)
        }
        _mods.value = current
        saveToFile(current)
    }

    fun deleteMod(modId: String) {
        val current = _mods.value.filterNot { it.id == modId }
        _mods.value = current
        saveToFile(current)
    }

    fun getModById(id: String): BedrockMod? {
        return _mods.value.firstOrNull { it.id == id }
    }

    /**
     * Creates a named version checkpoint snapshot in the mod's version history.
     */
    fun createVersionSnapshot(modId: String, changeNote: String): BedrockMod? {
        val mod = getModById(modId) ?: return null
        val nextVersionNumber = "v1.0.${mod.versionHistory.size + 1}"
        val snapshot = ModVersionSnapshot(
            versionTag = nextVersionNumber,
            timestamp = System.currentTimeMillis(),
            changeNote = changeNote.ifBlank { "Version Checkpoint $nextVersionNumber" },
            files = mod.files.toMap()
        )
        val updatedHistory = mod.versionHistory + snapshot
        val updatedMod = mod.copy(
            version = nextVersionNumber,
            versionHistory = updatedHistory,
            lastModified = System.currentTimeMillis()
        )
        saveMod(updatedMod)
        return updatedMod
    }

    /**
     * Reverts a mod back to a previous version checkpoint snapshot.
     */
    fun revertToSnapshot(modId: String, snapshotIndex: Int): BedrockMod? {
        val mod = getModById(modId) ?: return null
        if (snapshotIndex < 0 || snapshotIndex >= mod.versionHistory.size) return null
        val snapshot = mod.versionHistory[snapshotIndex]
        val updatedMod = mod.copy(
            files = snapshot.files.toMap(),
            lastModified = System.currentTimeMillis()
        )
        saveMod(updatedMod)
        return updatedMod
    }

    /**
     * Clones/Duplicates a mod under a new name with brand new unique UUIDs.
     */
    fun duplicateMod(modId: String, newName: String): BedrockMod? {
        val original = getModById(modId) ?: return null
        val duplicated = original.copy(
            id = UUID.randomUUID().toString(),
            name = if (newName.isNotBlank()) newName else "${original.name} (Copy)",
            bpUuidHeader = UUID.randomUUID().toString(),
            bpUuidModule = UUID.randomUUID().toString(),
            rpUuidHeader = UUID.randomUUID().toString(),
            rpUuidModule = UUID.randomUUID().toString(),
            version = "1.0.0",
            versionHistory = emptyList(),
            createdAt = System.currentTimeMillis(),
            lastModified = System.currentTimeMillis()
        )
        saveMod(duplicated)
        return duplicated
    }

    private fun saveToFile(list: List<BedrockMod>) {
        try {
            val jsonArray = JSONArray()
            for (mod in list) {
                val obj = JSONObject().apply {
                    put("id", mod.id)
                    put("name", mod.name)
                    put("description", mod.description)
                    put("author", mod.author)
                    put("version", mod.version)
                    put("category", mod.category)
                    put("bpUuidHeader", mod.bpUuidHeader)
                    put("bpUuidModule", mod.bpUuidModule)
                    put("rpUuidHeader", mod.rpUuidHeader)
                    put("rpUuidModule", mod.rpUuidModule)
                    put("primaryColorHex", mod.primaryColorHex)
                    put("iconEmoji", mod.iconEmoji)
                    put("createdAt", mod.createdAt)
                    put("lastModified", mod.lastModified)

                    val filesObj = JSONObject()
                    mod.files.forEach { (k, v) -> filesObj.put(k, v) }
                    put("files", filesObj)

                    val historyArr = JSONArray()
                    mod.versionHistory.forEach { snap ->
                        val snapObj = JSONObject().apply {
                            put("versionTag", snap.versionTag)
                            put("timestamp", snap.timestamp)
                            put("changeNote", snap.changeNote)
                            val snapFilesObj = JSONObject()
                            snap.files.forEach { (k, v) -> snapFilesObj.put(k, v) }
                            put("files", snapFilesObj)
                        }
                        historyArr.put(snapObj)
                    }
                    put("versionHistory", historyArr)
                }
                jsonArray.put(obj)
            }
            storageFile.writeText(jsonArray.toString(2))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- Chat Sessions Management (Gemini-like multi-turn session persistence) ---

    private fun loadChatSessions() {
        val loaded = mutableListOf<ChatSession>()
        if (sessionsFile.exists()) {
            try {
                val jsonStr = sessionsFile.readText()
                val jsonArr = JSONArray(jsonStr)
                for (i in 0 until jsonArr.length()) {
                    val obj = jsonArr.getJSONObject(i)
                    val msgsList = mutableListOf<ChatMessage>()
                    val msgsArr = obj.optJSONArray("messages")
                    if (msgsArr != null) {
                        for (j in 0 until msgsArr.length()) {
                            val msgObj = msgsArr.getJSONObject(j)
                            msgsList.add(
                                ChatMessage(
                                    id = msgObj.optString("id", UUID.randomUUID().toString()),
                                    role = msgObj.optString("role", "user"),
                                    content = msgObj.optString("content", ""),
                                    timestamp = msgObj.optLong("timestamp", System.currentTimeMillis()),
                                    modelUsed = msgObj.optString("modelUsed", "gemini-3.5-flash")
                                )
                            )
                        }
                    }
                    loaded.add(
                        ChatSession(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            messages = msgsList,
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            lastModified = obj.optLong("lastModified", System.currentTimeMillis())
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        _chatSessions.value = loaded
    }

    fun saveChatSessions(sessions: List<ChatSession>) {
        _chatSessions.value = sessions
        try {
            val jsonArr = JSONArray()
            for (s in sessions) {
                val obj = JSONObject().apply {
                    put("id", s.id)
                    put("title", s.title)
                    put("createdAt", s.createdAt)
                    put("lastModified", s.lastModified)
                    val msgsArr = JSONArray()
                    s.messages.forEach { m ->
                        msgsArr.put(JSONObject().apply {
                            put("id", m.id)
                            put("role", m.role)
                            put("content", m.content)
                            put("timestamp", m.timestamp)
                            put("modelUsed", m.modelUsed)
                        })
                    }
                    put("messages", msgsArr)
                }
                jsonArr.put(obj)
            }
            sessionsFile.writeText(jsonArr.toString(2))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getStarterTemplates(): List<BedrockMod> {
        // Simple starter template requested by user: Ruby Dagger with custom item & recipe
        val rubyDaggerStarter = BedrockMod(
            name = "Ruby Dagger Starter Addon",
            description = "A lightweight beginner template with custom 1.20.80 item, 16x16 red crystal texture, and crafting table recipe.",
            category = "Weapons & Combat",
            iconEmoji = "🗡️",
            primaryColorHex = "#EF4444",
            files = mapOf(
                "items/ruby_dagger.json" to """
{
  "format_version": "1.20.80",
  "minecraft:item": {
    "description": {
      "identifier": "studio:ruby_dagger",
      "menu_category": {
        "category": "equipment",
        "group": "itemGroup.name.sword"
      }
    },
    "components": {
      "minecraft:icon": "ruby_dagger",
      "minecraft:display_name": { "value": "§c§lRuby Dagger" },
      "minecraft:hand_equipped": true,
      "minecraft:max_stack_size": 1,
      "minecraft:damage": 9,
      "minecraft:durability": {
        "max_durability": 850
      },
      "minecraft:can_destroy_in_creative": false,
      "minecraft:enchantable": {
        "value": 15,
        "slot": "sword"
      }
    }
  }
}
                """.trimIndent(),
                "recipes/ruby_dagger_recipe.json" to """
{
  "format_version": "1.20.80",
  "minecraft:recipe_shaped": {
    "description": {
      "identifier": "studio:ruby_dagger_recipe"
    },
    "tags": ["crafting_table"],
    "pattern": [
      " R",
      "S "
    ],
    "key": {
      "R": { "item": "minecraft:redstone" },
      "S": { "item": "minecraft:stick" }
    },
    "result": {
      "item": "studio:ruby_dagger",
      "count": 1
    }
  }
}
                """.trimIndent()
            )
        )

        val laserKatana = BedrockMod(
            name = "Hyper Laser Katana",
            description = "High-tech plasma weapon featuring custom particle trails and Bedrock Script API slash waves.",
            category = "Weapons & Combat",
            iconEmoji = "🗡️",
            primaryColorHex = "#00F2FE",
            files = mapOf(
                "items/laser_katana.json" to """
{
  "format_version": "1.20.80",
  "minecraft:item": {
    "description": {
      "identifier": "studio:laser_katana",
      "menu_category": {
        "category": "equipment",
        "group": "itemGroup.name.sword"
      }
    },
    "components": {
      "minecraft:icon": "laser_katana",
      "minecraft:display_name": { "value": "§b§lHyper Laser Katana" },
      "minecraft:hand_equipped": true,
      "minecraft:max_stack_size": 1,
      "minecraft:damage": 18,
      "minecraft:durability": {
        "max_durability": 2500
      },
      "minecraft:can_destroy_in_creative": false,
      "minecraft:enchantable": {
        "value": 25,
        "slot": "sword"
      }
    }
  }
}
                """.trimIndent(),
                "scripts/main.js" to """
import { world, system } from "@minecraft/server";

world.afterEvents.itemUse.subscribe((event) => {
    const item = event.itemStack;
    const player = event.source;
    if (item.typeId === "studio:laser_katana") {
        const viewDir = player.getViewDirection();
        const headLocation = player.getHeadLocation();

        for (let i = 1; i <= 8; i++) {
            const px = headLocation.x + (viewDir.x * i);
            const py = headLocation.y + (viewDir.y * i);
            const pz = headLocation.z + (viewDir.z * i);
            player.dimension.spawnParticle("minecraft:sonic_explosion", { x: px, y: py, z: pz });
        }
        player.dimension.playSound("beacon.activate", player.location, { pitch: 1.8, volume: 1.0 });
    }
});
                """.trimIndent(),
                "recipes/laser_katana_recipe.json" to """
{
  "format_version": "1.20.80",
  "minecraft:recipe_shaped": {
    "description": {
      "identifier": "studio:laser_katana_recipe"
    },
    "tags": ["crafting_table"],
    "pattern": [
      "  D",
      " N ",
      "R  "
    ],
    "key": {
      "D": { "item": "minecraft:diamond" },
      "N": { "item": "minecraft:netherite_ingot" },
      "R": { "item": "minecraft:redstone" }
    },
    "result": {
      "item": "studio:laser_katana",
      "count": 1
    }
  }
}
                """.trimIndent()
            )
        )

        val netherTitan = BedrockMod(
            name = "Nether Titan Boss",
            description = "Towering volcanic boss with seismic smash, custom boss bar, and Netherite Titan loot table.",
            category = "Bosses & Entities",
            iconEmoji = "👹",
            primaryColorHex = "#EF4444",
            files = mapOf(
                "entities/nether_titan.json" to """
{
  "format_version": "1.20.80",
  "minecraft:entity": {
    "description": {
      "identifier": "studio:nether_titan",
      "is_spawnable": true,
      "is_summonable": true,
      "runtime_identifier": "minecraft:iron_golem"
    },
    "components": {
      "minecraft:is_hidden_when_invisible": true,
      "minecraft:type_family": {
        "family": ["mob", "monster", "boss"]
      },
      "minecraft:health": {
        "value": 450,
        "max": 450
      },
      "minecraft:attack": {
        "damage": 22
      },
      "minecraft:fire_immune": true,
      "minecraft:knockback_resistance": {
        "value": 1.0
      },
      "minecraft:boss": {
        "name": "§c§lNether Titan",
        "should_darken_sky": true,
        "hud_range": 64
      },
      "minecraft:movement": {
        "value": 0.28
      },
      "minecraft:loot": {
        "table": "loot_tables/entities/nether_titan.json"
      }
    }
  }
}
                """.trimIndent(),
                "loot_tables/entities/nether_titan.json" to """
{
  "pools": [
    {
      "rolls": 1,
      "entries": [
        {
          "type": "item",
          "name": "minecraft:nether_star",
          "weight": 1
        },
        {
          "type": "item",
          "name": "minecraft:netherite_ingot",
          "weight": 2,
          "functions": [
            {
              "function": "set_count",
              "count": { "min": 2, "max": 5 }
            }
          ]
        }
      ]
    }
  ]
}
                """.trimIndent()
            )
        )

        val rubyOrePack = BedrockMod(
            name = "Ruby Gems & Tools",
            description = "Natural ruby ore generation, smelting recipes, and high-speed Ruby Pickaxe with glowing texture.",
            category = "Blocks & Mining",
            iconEmoji = "💎",
            primaryColorHex = "#F43F5E",
            files = mapOf(
                "blocks/ruby_ore.json" to """
{
  "format_version": "1.20.80",
  "minecraft:block": {
    "description": {
      "identifier": "studio:ruby_ore",
      "menu_category": {
        "category": "nature",
        "group": "itemGroup.name.ore"
      }
    },
    "components": {
      "minecraft:destructible_by_mining": {
        "seconds_to_destroy": 3.0
      },
      "minecraft:light_emission": 4,
      "minecraft:friction": 0.6
    }
  }
}
                """.trimIndent(),
                "items/ruby_pickaxe.json" to """
{
  "format_version": "1.20.80",
  "minecraft:item": {
    "description": {
      "identifier": "studio:ruby_pickaxe",
      "menu_category": {
        "category": "equipment",
        "group": "itemGroup.name.pickaxe"
      }
    },
    "components": {
      "minecraft:icon": "ruby_pickaxe",
      "minecraft:display_name": { "value": "§cRuby Pickaxe" },
      "minecraft:hand_equipped": true,
      "minecraft:damage": 7,
      "minecraft:durability": { "max_durability": 1800 },
      "minecraft:digger": {
        "use_efficiency": true,
        "destroy_speeds": [
          {
            "block": "minecraft:stone",
            "speed": 12
          },
          {
            "block": "minecraft:obsidian",
            "speed": 8
          }
        ]
      }
    }
  }
}
                """.trimIndent()
            )
        )

        return listOf(rubyDaggerStarter, laserKatana, netherTitan, rubyOrePack)
    }
}
