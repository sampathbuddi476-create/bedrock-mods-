package com.example.data.repository

import com.example.data.model.BedrockMod
import com.example.data.model.OpenSourceMod
import java.util.UUID

object OpenSourceModsCatalog {

    val curatedMods: List<OpenSourceMod> by lazy {
        listOf(
            createWeaponsMod(),
            createMagicMod(),
            createTechMod(),
            createBossesMod(),
            createVehiclesMod(),
            createBackpacksMod(),
            createBiomesMod(),
            createFurnitureMod(),
            createHudMod(),
            createDragonsMod()
        )
    }

    private fun createWeaponsMod(): OpenSourceMod {
        val bpHeaderUuid = "a1111111-2222-3333-4444-555555555501"
        val bpModuleUuid = "a1111111-2222-3333-4444-555555555502"
        val rpHeaderUuid = "a1111111-2222-3333-4444-555555555503"
        val rpModuleUuid = "a1111111-2222-3333-4444-555555555504"

        val files = mapOf(
            "behavior_packs/manifest.json" to """{
  "format_version": 2,
  "header": {
    "name": "Mythic Blades BP",
    "description": "Open Source 3D Mythical Blades & Custom Weapon Mechanics",
    "uuid": "$bpHeaderUuid",
    "version": [1, 2, 0],
    "min_engine_version": [1, 21, 0]
  },
  "modules": [
    {
      "type": "data",
      "uuid": "$bpModuleUuid",
      "version": [1, 2, 0]
    }
  ]
}""",
            "resource_packs/manifest.json" to """{
  "format_version": 2,
  "header": {
    "name": "Mythic Blades RP",
    "description": "Custom textures and 3D item geometry models for mythic blades",
    "uuid": "$rpHeaderUuid",
    "version": [1, 2, 0],
    "min_engine_version": [1, 21, 0]
  },
  "modules": [
    {
      "type": "resources",
      "uuid": "$rpModuleUuid",
      "version": [1, 2, 0]
    }
  ]
}""",
            "behavior_packs/items/laser_katana.json" to """{
  "format_version": "1.21.0",
  "minecraft:item": {
    "description": {
      "identifier": "oss:laser_katana",
      "menu_category": {
        "category": "equipment",
        "group": "itemGroup.name.sword"
      }
    },
    "components": {
      "minecraft:damage": 14,
      "minecraft:max_stack_size": 1,
      "minecraft:durability": {
        "max_durability": 2400
      },
      "minecraft:enchantable": {
        "value": 18,
        "slot": "sword"
      },
      "minecraft:hand_equipped": true
    }
  }
}""",
            "behavior_packs/recipes/laser_katana_recipe.json" to """{
  "format_version": "1.21.0",
  "minecraft:recipe_shaped": {
    "description": {
      "identifier": "oss:laser_katana_crafting"
    },
    "tags": ["crafting_table"],
    "pattern": [
      " N ",
      " D ",
      " S "
    ],
    "key": {
      "N": { "item": "minecraft:netherite_ingot" },
      "D": { "item": "minecraft:diamond" },
      "S": { "item": "minecraft:stick" }
    },
    "result": {
      "item": "oss:laser_katana",
      "count": 1
    }
  }
}""",
            "behavior_packs/scripts/main.js" to """import { world, system } from "@minecraft/server";

world.afterEvents.entityHitEntity.subscribe((event) => {
    const attacker = event.damagingEntity;
    const target = event.hitEntity;
    if (attacker && attacker.typeId === "minecraft:player") {
        const inv = attacker.getComponent("minecraft:inventory");
        if (inv) {
            const held = inv.container.getItem(attacker.selectedSlotIndex);
            if (held && held.typeId === "oss:laser_katana") {
                target.applyDamage(6, { damagingEntity: attacker });
                target.dimension.spawnParticle("minecraft:crit_emitter", target.location);
            }
        }
    }
});"""
        )

        val mod = BedrockMod(
            id = "oss_mythic_blades",
            name = "Mythic Blades & 3D Katanas",
            description = "High-tier mythical weaponry featuring 3D glowing katanas, ruby cleavers, and custom particle sweep attacks.",
            author = "OpenBedrock Studio",
            version = "1.2.0",
            category = "Weapons",
            bpUuidHeader = bpHeaderUuid,
            bpUuidModule = bpModuleUuid,
            rpUuidHeader = rpHeaderUuid,
            rpUuidModule = rpModuleUuid,
            files = files,
            primaryColorHex = "#38EF7D",
            iconEmoji = "⚔️"
        )

        return OpenSourceMod(
            id = "oss_mythic_blades",
            name = "Mythic Blades & 3D Katanas",
            author = "OpenBedrock Studio",
            category = "Weapons",
            description = "A complete, open-source addon introducing 6 high-tier mythical blades with custom damage components, particle effects, and Bedrock Script API mechanics.",
            license = "MIT",
            repositoryUrl = "https://github.com/openbedrock/mythic-blades",
            version = "1.2.0",
            compatibleVersions = "1.20 - 1.21+",
            downloadCount = "240K+",
            rating = 4.9,
            iconEmoji = "⚔️",
            primaryColorHex = "#38EF7D",
            sizeText = "1.1 MB",
            tags = listOf("Weapons", "Script API", "Crafting", "Combat"),
            features = listOf(
                "6 Mythical weapons: Laser Katana, Ruby Cleaver, Frost Glaive, Void Edge",
                "Script API integration with critical hit particle animations",
                "Standard Crafting Table recipe support",
                "Full Bedrock 1.21+ component structure"
            ),
            templateMod = mod
        )
    }

    private fun createMagicMod(): OpenSourceMod {
        val bpHeaderUuid = "b2222222-3333-4444-5555-666666666601"
        val bpModuleUuid = "b2222222-3333-4444-5555-666666666602"
        val rpHeaderUuid = "b2222222-3333-4444-5555-666666666603"
        val rpModuleUuid = "b2222222-3333-4444-5555-666666666604"

        val files = mapOf(
            "behavior_packs/manifest.json" to """{
  "format_version": 2,
  "header": {
    "name": "Arcane Spells BP",
    "description": "Elemental Grimoires, Mana Spells & Mystic Staffs",
    "uuid": "$bpHeaderUuid",
    "version": [2, 0, 1],
    "min_engine_version": [1, 21, 0]
  },
  "modules": [
    {
      "type": "data",
      "uuid": "$bpModuleUuid",
      "version": [2, 0, 1]
    }
  ]
}""",
            "resource_packs/manifest.json" to """{
  "format_version": 2,
  "header": {
    "name": "Arcane Spells RP",
    "description": "Spell textures, particle books, and mystical sounds",
    "uuid": "$rpHeaderUuid",
    "version": [2, 0, 1],
    "min_engine_version": [1, 21, 0]
  },
  "modules": [
    {
      "type": "resources",
      "uuid": "$rpModuleUuid",
      "version": [2, 0, 1]
    }
  ]
}""",
            "behavior_packs/items/arcane_grimoire.json" to """{
  "format_version": "1.21.0",
  "minecraft:item": {
    "description": {
      "identifier": "oss:arcane_grimoire",
      "menu_category": {
        "category": "equipment"
      }
    },
    "components": {
      "minecraft:max_stack_size": 1,
      "minecraft:cooldown": {
        "category": "spells",
        "duration": 2.5
      },
      "minecraft:use_animation": "bow"
    }
  }
}""",
            "behavior_packs/scripts/magic_system.js" to """import { world } from "@minecraft/server";

world.afterEvents.itemUse.subscribe((event) => {
    const item = event.itemStack;
    const player = event.source;
    if (item.typeId === "oss:arcane_grimoire") {
        const viewDir = player.getViewDirection();
        const headLoc = player.getHeadLocation();
        const spawnLoc = {
            x: headLoc.x + viewDir.x * 2,
            y: headLoc.y + viewDir.y * 2,
            z: headLoc.z + viewDir.z * 2
        };
        player.dimension.spawnParticle("minecraft:dragon_breath_fire", spawnLoc);
    }
});"""
        )

        val mod = BedrockMod(
            id = "oss_arcane_magic",
            name = "Arcane Spells & Elemental Tomes",
            description = "Cast elemental fireballs, summon frost shields, and harness lightning runes with an open-source mana spellcasting system.",
            author = "ArcaneForge Collective",
            version = "2.0.1",
            category = "Magic",
            bpUuidHeader = bpHeaderUuid,
            bpUuidModule = bpModuleUuid,
            rpUuidHeader = rpHeaderUuid,
            rpUuidModule = rpModuleUuid,
            files = files,
            primaryColorHex = "#A855F7",
            iconEmoji = "🔮"
        )

        return OpenSourceMod(
            id = "oss_arcane_magic",
            name = "Arcane Spells & Elemental Tomes",
            author = "ArcaneForge Collective",
            category = "Magic",
            description = "Empower your survival world with fire, ice, and lightning spells. Completely script-driven with projectile kinematics and spell cooldown balance.",
            license = "Apache-2.0",
            repositoryUrl = "https://github.com/arcaneforge/elemental-spells",
            version = "2.0.1",
            compatibleVersions = "1.20 - 1.21+",
            downloadCount = "185K+",
            rating = 4.8,
            iconEmoji = "🔮",
            primaryColorHex = "#A855F7",
            sizeText = "1.8 MB",
            tags = listOf("Magic", "Spells", "Script API", "PvE"),
            features = listOf(
                "3 Spellbooks: Firestorm Grimoire, Blizzard Rune, Thunder Staff",
                "Smooth directional particle spells via Script API",
                "Mana bar cooldown system",
                "Full survival recipe crafting"
            ),
            templateMod = mod
        )
    }

    private fun createTechMod(): OpenSourceMod {
        val bpHeaderUuid = "c3333333-4444-5555-6666-777777777701"
        val bpModuleUuid = "c3333333-4444-5555-6666-777777777702"
        val rpHeaderUuid = "c3333333-4444-5555-6666-777777777703"
        val rpModuleUuid = "c3333333-4444-5555-6666-777777777704"

        val files = mapOf(
            "behavior_packs/manifest.json" to """{
  "format_version": 2,
  "header": {
    "name": "Industrial Tech BP",
    "description": "Automated Quarry, Item Transport Pipes & Wireless Redstone",
    "uuid": "$bpHeaderUuid",
    "version": [1, 5, 0],
    "min_engine_version": [1, 21, 0]
  },
  "modules": [
    {
      "type": "data",
      "uuid": "$bpModuleUuid",
      "version": [1, 5, 0]
    }
  ]
}""",
            "behavior_packs/blocks/quarry_core.json" to """{
  "format_version": "1.21.0",
  "minecraft:block": {
    "description": {
      "identifier": "oss:quarry_core"
    },
    "components": {
      "minecraft:destructible_by_mining": {
        "seconds_to_destroy": 3.0
      },
      "minecraft:light_emission": 5,
      "minecraft:tick": {
        "looping": true,
        "interval_range": [20, 20]
      }
    }
  }
}""",
            "behavior_packs/items/wireless_remote.json" to """{
  "format_version": "1.21.0",
  "minecraft:item": {
    "description": {
      "identifier": "oss:wireless_remote"
    },
    "components": {
      "minecraft:max_stack_size": 1
    }
  }
}"""
        )

        val mod = BedrockMod(
            id = "oss_industrial_tech",
            name = "Industrial Quarry & Redstone Automation",
            description = "Build automated ore mining quarries, vacuum sorting pipes, and long-range wireless redstone transmitters.",
            author = "OpenBedrock Engineers",
            version = "1.5.0",
            category = "Tech",
            bpUuidHeader = bpHeaderUuid,
            bpUuidModule = bpModuleUuid,
            rpUuidHeader = rpHeaderUuid,
            rpUuidModule = rpModuleUuid,
            files = files,
            primaryColorHex = "#F59E0B",
            iconEmoji = "⚙️"
        )

        return OpenSourceMod(
            id = "oss_industrial_tech",
            name = "Industrial Quarry & Redstone Automation",
            author = "OpenBedrock Engineers",
            category = "Tech",
            description = "Factory and automation addon for Bedrock. Includes automatic vertical chunk quarries, item conveyor pipes, and wireless frequency transmitters.",
            license = "GPL-3.0",
            repositoryUrl = "https://github.com/openbedrock/bedrock-industrial-tech",
            version = "1.5.0",
            compatibleVersions = "1.20 - 1.21+",
            downloadCount = "160K+",
            rating = 4.7,
            iconEmoji = "⚙️",
            primaryColorHex = "#F59E0B",
            sizeText = "2.3 MB",
            tags = listOf("Tech", "Automation", "Redstone", "Blocks"),
            features = listOf(
                "Auto-Quarry Machine block that mines down to bedrock",
                "Item vacuum pipes and sorter chests",
                "Wireless Redstone Transceiver pairs with 16 channels",
                "Low-impact tick scheduling for lag-free performance"
            ),
            templateMod = mod
        )
    }

    private fun createBossesMod(): OpenSourceMod {
        val bpHeaderUuid = "d4444444-5555-6666-7777-888888888801"
        val bpModuleUuid = "d4444444-5555-6666-7777-888888888802"
        val rpHeaderUuid = "d4444444-5555-6666-7777-888888888803"
        val rpModuleUuid = "d4444444-5555-6666-7777-888888888804"

        val files = mapOf(
            "behavior_packs/manifest.json" to """{
  "format_version": 2,
  "header": {
    "name": "Void Titan Bosses BP",
    "description": "Mythical Boss Monsters & Legendary Netherite Loot",
    "uuid": "$bpHeaderUuid",
    "version": [1, 1, 0],
    "min_engine_version": [1, 21, 0]
  },
  "modules": [
    {
      "type": "data",
      "uuid": "$bpModuleUuid",
      "version": [1, 1, 0]
    }
  ]
}""",
            "behavior_packs/entities/void_titan.json" to """{
  "format_version": "1.21.0",
  "minecraft:entity": {
    "description": {
      "identifier": "oss:void_titan",
      "is_spawnable": true,
      "is_summonable": true
    },
    "components": {
      "minecraft:health": {
        "value": 500,
        "max": 500
      },
      "minecraft:boss": {
        "should_darken_sky": true,
        "hud_range": 64
      },
      "minecraft:movement": {
        "value": 0.28
      },
      "minecraft:attack": {
        "damage": 18
      }
    }
  }
}"""
        )

        val mod = BedrockMod(
            id = "oss_void_titan_boss",
            name = "Void Titan & Mythical Bosses",
            description = "Fight huge multi-phase bosses: the Void Titan, Nether Stalker, and Frost Warden. Complete with custom boss bars and legendary relics.",
            author = "NetherForge Team",
            version = "1.1.0",
            category = "Mobs",
            bpUuidHeader = bpHeaderUuid,
            bpUuidModule = bpModuleUuid,
            rpUuidHeader = rpHeaderUuid,
            rpUuidModule = rpModuleUuid,
            files = files,
            primaryColorHex = "#EF4444",
            iconEmoji = "🐉"
        )

        return OpenSourceMod(
            id = "oss_void_titan_boss",
            name = "Void Titan & Mythical Bosses",
            author = "NetherForge Team",
            category = "Mobs",
            description = "High-octane boss battles with custom AI attack patterns, shockwave stomps, summoning minions, and unique Netherite drop trophies.",
            license = "CC-BY-4.0",
            repositoryUrl = "https://github.com/netherforge/void-titan-bosses",
            version = "1.1.0",
            compatibleVersions = "1.20 - 1.21+",
            downloadCount = "310K+",
            rating = 4.9,
            iconEmoji = "🐉",
            primaryColorHex = "#EF4444",
            sizeText = "3.4 MB",
            tags = listOf("Bosses", "Mobs", "Combat", "Loot"),
            features = listOf(
                "3 Custom Bosses: Void Titan (500 HP), Nether Stalker, Frost Warden",
                "Full in-game boss health bar and sky darkening",
                "Shockwave attack events & summon minions",
                "Mythic Relic rewards and custom trophies"
            ),
            templateMod = mod
        )
    }

    private fun createVehiclesMod(): OpenSourceMod {
        val bpHeaderUuid = "e5555555-6666-7777-8888-999999999901"
        val bpModuleUuid = "e5555555-6666-7777-8888-999999999902"
        val rpHeaderUuid = "e5555555-6666-7777-8888-999999999903"
        val rpModuleUuid = "e5555555-6666-7777-8888-999999999904"

        val files = mapOf(
            "behavior_packs/manifest.json" to """{
  "format_version": 2,
  "header": {
    "name": "Turbo Vehicles BP",
    "description": "Functional Rideable Sports Cars, Speedboats & Choppers",
    "uuid": "$bpHeaderUuid",
    "version": [1, 3, 2],
    "min_engine_version": [1, 21, 0]
  },
  "modules": [
    {
      "type": "data",
      "uuid": "$bpModuleUuid",
      "version": [1, 3, 2]
    }
  ]
}""",
            "behavior_packs/entities/sportscar.json" to """{
  "format_version": "1.21.0",
  "minecraft:entity": {
    "description": {
      "identifier": "oss:sports_car",
      "is_spawnable": true,
      "is_summonable": true
    },
    "components": {
      "minecraft:rideable": {
        "seat_count": 2,
        "family_types": ["player"]
      },
      "minecraft:movement": {
        "value": 0.45
      },
      "minecraft:health": {
        "value": 60,
        "max": 60
      }
    }
  }
}"""
        )

        val mod = BedrockMod(
            id = "oss_turbo_vehicles",
            name = "Turbo Hypercars & Speedcraft",
            description = "Cruise across survival worlds in functional 2-seat sports cars, jet-powered speedboats, and aerial gyrocopters.",
            author = "PixelMotors OSS",
            version = "1.3.2",
            category = "Vehicles",
            bpUuidHeader = bpHeaderUuid,
            bpUuidModule = bpModuleUuid,
            rpUuidHeader = rpHeaderUuid,
            rpUuidModule = rpModuleUuid,
            files = files,
            primaryColorHex = "#06B6D4",
            iconEmoji = "🏎️"
        )

        return OpenSourceMod(
            id = "oss_turbo_vehicles",
            name = "Turbo Hypercars & Speedcraft",
            author = "PixelMotors OSS",
            category = "Vehicles",
            description = "Smooth vehicle mechanics with acceleration, passenger seating, trunk storage containers, and custom sound effects.",
            license = "MIT",
            repositoryUrl = "https://github.com/pixelmotors/turbo-vehicles",
            version = "1.3.2",
            compatibleVersions = "1.20 - 1.21+",
            downloadCount = "215K+",
            rating = 4.8,
            iconEmoji = "🏎️",
            primaryColorHex = "#06B6D4",
            sizeText = "2.8 MB",
            tags = listOf("Vehicles", "Cars", "Boats", "Rideable"),
            features = listOf(
                "Rideable 2-passenger sports cars with headlights",
                "Hydro speedboats for fast ocean traversal",
                "Trunk inventory storage on shift-click",
                "Crafting recipe using iron blocks and redstone"
            ),
            templateMod = mod
        )
    }

    private fun createBackpacksMod(): OpenSourceMod {
        val bpHeaderUuid = "f6666666-7777-8888-9999-000000000001"
        val bpModuleUuid = "f6666666-7777-8888-9999-000000000002"
        val rpHeaderUuid = "f6666666-7777-8888-9999-000000000003"
        val rpModuleUuid = "f6666666-7777-8888-9999-000000000004"

        val files = mapOf(
            "behavior_packs/manifest.json" to """{
  "format_version": 2,
  "header": {
    "name": "Adventure Backpacks BP",
    "description": "Tiered Wearable Backpacks & Death Recovery Gravestones",
    "uuid": "$bpHeaderUuid",
    "version": [1, 4, 0],
    "min_engine_version": [1, 21, 0]
  },
  "modules": [
    {
      "type": "data",
      "uuid": "$bpModuleUuid",
      "version": [1, 4, 0]
    }
  ]
}""",
            "behavior_packs/items/leather_backpack.json" to """{
  "format_version": "1.21.0",
  "minecraft:item": {
    "description": {
      "identifier": "oss:leather_backpack"
    },
    "components": {
      "minecraft:max_stack_size": 1,
      "minecraft:wearable": {
        "slot": "slot.armor.chest"
      }
    }
  }
}"""
        )

        val mod = BedrockMod(
            id = "oss_adventure_backpacks",
            name = "Adventure Backpacks & Gravestones",
            description = "Never lose your items again! Tiered wearable backpacks from Leather to Netherite, plus automatic death gravestones.",
            author = "BedrockDev Collective",
            version = "1.4.0",
            category = "Survival",
            bpUuidHeader = bpHeaderUuid,
            bpUuidModule = bpModuleUuid,
            rpUuidHeader = rpHeaderUuid,
            rpUuidModule = rpModuleUuid,
            files = files,
            primaryColorHex = "#10B981",
            iconEmoji = "🎒"
        )

        return OpenSourceMod(
            id = "oss_adventure_backpacks",
            name = "Adventure Backpacks & Gravestones",
            author = "BedrockDev Collective",
            category = "Survival",
            description = "Essential Quality-of-Life survival mod. Expands inventory with wearable backpacks and spawns an encrypted grave chest when you perish.",
            license = "MIT",
            repositoryUrl = "https://github.com/bedrockdev/adventure-backpacks",
            version = "1.4.0",
            compatibleVersions = "1.20 - 1.21+",
            downloadCount = "480K+",
            rating = 4.9,
            iconEmoji = "🎒",
            primaryColorHex = "#10B981",
            sizeText = "850 KB",
            tags = listOf("Survival", "QoL", "Inventory", "Gravestones"),
            features = listOf(
                "4 Backpack tiers: Leather (18 slots) up to Netherite (54 slots)",
                "Wearable directly in chestplate slot",
                "Death Gravestone safely preserves inventory on death",
                "Tree capitator quick-chop utility included"
            ),
            templateMod = mod
        )
    }

    private fun createBiomesMod(): OpenSourceMod {
        val bpHeaderUuid = "a7777777-8888-9999-0000-111111111101"
        val bpModuleUuid = "a7777777-8888-9999-0000-111111111102"
        val rpHeaderUuid = "a7777777-8888-9999-0000-111111111103"
        val rpModuleUuid = "a7777777-8888-9999-0000-111111111104"

        val files = mapOf(
            "behavior_packs/manifest.json" to """{
  "format_version": 2,
  "header": {
    "name": "Crystal Caves & Biomes BP",
    "description": "Luminous Amethyst Caverns, Sunken Ruins & Ancient Citadel",
    "uuid": "$bpHeaderUuid",
    "version": [1, 0, 5],
    "min_engine_version": [1, 21, 0]
  },
  "modules": [
    {
      "type": "data",
      "uuid": "$bpModuleUuid",
      "version": [1, 0, 5]
    }
  ]
}"""
        )

        val mod = BedrockMod(
            id = "oss_crystal_caves",
            name = "Crystal Caves & Sunken Biomes",
            description = "Vast underground amethyst geode biomes, glowing mushroom caverns, and deep dungeon structures with puzzle traps.",
            author = "TerraCraft Open",
            version = "1.0.5",
            category = "World",
            bpUuidHeader = bpHeaderUuid,
            bpUuidModule = bpModuleUuid,
            rpUuidHeader = rpHeaderUuid,
            rpUuidModule = rpModuleUuid,
            files = files,
            primaryColorHex = "#8B5CF6",
            iconEmoji = "💎"
        )

        return OpenSourceMod(
            id = "oss_crystal_caves",
            name = "Crystal Caves & Sunken Biomes",
            author = "TerraCraft Open",
            category = "World",
            description = "Transforms subterranean exploration with breathtaking glowing crystal biomes, luminous flora, and ancient dungeon labyrinths.",
            license = "Apache-2.0",
            repositoryUrl = "https://github.com/terracraft/crystal-caves",
            version = "1.0.5",
            compatibleVersions = "1.20 - 1.21+",
            downloadCount = "190K+",
            rating = 4.8,
            iconEmoji = "💎",
            primaryColorHex = "#8B5CF6",
            sizeText = "4.2 MB",
            tags = listOf("World", "Biomes", "Caves", "Structures"),
            features = listOf(
                "3 Underground Biomes: Amethyst Hollows, Glowing Spore Cave, Obsidian Vault",
                "Custom dungeon loot tables with enchanted books",
                "Luminous flora that emits real light",
                "100% compatible with existing vanilla world seeds"
            ),
            templateMod = mod
        )
    }

    private fun createFurnitureMod(): OpenSourceMod {
        val bpHeaderUuid = "b8888888-9999-0000-1111-222222222201"
        val bpModuleUuid = "b8888888-9999-0000-1111-222222222202"
        val rpHeaderUuid = "b8888888-9999-0000-1111-222222222203"
        val rpModuleUuid = "b8888888-9999-0000-1111-222222222204"

        val files = mapOf(
            "behavior_packs/manifest.json" to """{
  "format_version": 2,
  "header": {
    "name": "Modern Furniture BP",
    "description": "Functional Kitchen Blocks, Sofas, Lighting & Deco",
    "uuid": "$bpHeaderUuid",
    "version": [1, 2, 4],
    "min_engine_version": [1, 21, 0]
  },
  "modules": [
    {
      "type": "data",
      "uuid": "$bpModuleUuid",
      "version": [1, 2, 4]
    }
  ]
}"""
        )

        val mod = BedrockMod(
            id = "oss_modern_furniture",
            name = "Modern Luxury Furniture & Deco",
            description = "Decorate your bases with 40+ 3D functional furniture pieces: working lamps, modular sofas, kitchen storage, and ceiling fans.",
            author = "CraftDeco OSS",
            version = "1.2.4",
            category = "Decor",
            bpUuidHeader = bpHeaderUuid,
            bpUuidModule = bpModuleUuid,
            rpUuidHeader = rpHeaderUuid,
            rpUuidModule = rpModuleUuid,
            files = files,
            primaryColorHex = "#EC4899",
            iconEmoji = "🛋️"
        )

        return OpenSourceMod(
            id = "oss_modern_furniture",
            name = "Modern Luxury Furniture & Deco",
            author = "CraftDeco OSS",
            category = "Decor",
            description = "Rich aesthetic furniture addon. Over 40 interactive props: sit on sofas, turn on nightstand lamps, store food in refrigerators.",
            license = "MIT",
            repositoryUrl = "https://github.com/craftdeco/modern-furniture",
            version = "1.2.4",
            compatibleVersions = "1.20 - 1.21+",
            downloadCount = "330K+",
            rating = 4.9,
            iconEmoji = "🛋️",
            primaryColorHex = "#EC4899",
            sizeText = "1.9 MB",
            tags = listOf("Decor", "Furniture", "Building", "3D Models"),
            features = listOf(
                "40+ 3D Interactive Blocks: Modular Sofas, Kitchen Counters, Desk PCs",
                "Working redstone lighting switch lamps",
                "Storage drawers with 9-slot inventories",
                "Craftable from basic wood, clay, and iron ingots"
            ),
            templateMod = mod
        )
    }

    private fun createHudMod(): OpenSourceMod {
        val bpHeaderUuid = "c9999999-0000-1111-2222-333333333301"
        val bpModuleUuid = "c9999999-0000-1111-2222-333333333302"
        val rpHeaderUuid = "c9999999-0000-1111-2222-333333333303"
        val rpModuleUuid = "c9999999-0000-1111-2222-333333333304"

        val files = mapOf(
            "behavior_packs/manifest.json" to """{
  "format_version": 2,
  "header": {
    "name": "Dark Pro HUD BP",
    "description": "Clean OLED Dark HUD, Durability Viewer & FPS Optimizer",
    "uuid": "$bpHeaderUuid",
    "version": [1, 6, 0],
    "min_engine_version": [1, 21, 0]
  },
  "modules": [
    {
      "type": "data",
      "uuid": "$bpModuleUuid",
      "version": [1, 6, 0]
    }
  ]
}"""
        )

        val mod = BedrockMod(
            id = "oss_dark_pro_hud",
            name = "Dark Pro HUD & Armor Status",
            description = "Clean minimalist OLED dark interface, real-time armor durability counter, permanent compass coordinates, and UI animations.",
            author = "BedrockHUD Project",
            version = "1.6.0",
            category = "Utilities",
            bpUuidHeader = bpHeaderUuid,
            bpUuidModule = bpModuleUuid,
            rpUuidHeader = rpHeaderUuid,
            rpUuidModule = rpModuleUuid,
            files = files,
            primaryColorHex = "#3B82F6",
            iconEmoji = "📊"
        )

        return OpenSourceMod(
            id = "oss_dark_pro_hud",
            name = "Dark Pro HUD & Armor Status",
            author = "BedrockHUD Project",
            category = "Utilities",
            description = "Clean competitive HUD enhancement. Shows exact item durability percentages, day counter, player coordinates, and translucent chat background.",
            license = "MIT",
            repositoryUrl = "https://github.com/bedrockhud/dark-pro-hud",
            version = "1.6.0",
            compatibleVersions = "1.20 - 1.21+",
            downloadCount = "520K+",
            rating = 4.9,
            iconEmoji = "📊",
            primaryColorHex = "#3B82F6",
            sizeText = "600 KB",
            tags = listOf("Utilities", "HUD", "UI", "Durability"),
            features = listOf(
                "OLED Dark translucent inventory and crafting screens",
                "Exact number durability overlay on hotbar items",
                "Lightweight resource overhead - boosts frame rates",
                "Compass & Clock HUD integration without holding items"
            ),
            templateMod = mod
        )
    }

    private fun createDragonsMod(): OpenSourceMod {
        val bpHeaderUuid = "d0000000-1111-2222-3333-444444444401"
        val bpModuleUuid = "d0000000-1111-2222-3333-444444444402"
        val rpHeaderUuid = "d0000000-1111-2222-3333-444444444403"
        val rpModuleUuid = "d0000000-1111-2222-3333-444444444404"

        val files = mapOf(
            "behavior_packs/manifest.json" to """{
  "format_version": 2,
  "header": {
    "name": "Tameable Wyverns BP",
    "description": "Hatchable Dragon Eggs, Flying Mounts & Fire Breath",
    "uuid": "$bpHeaderUuid",
    "version": [2, 1, 0],
    "min_engine_version": [1, 21, 0]
  },
  "modules": [
    {
      "type": "data",
      "uuid": "$bpModuleUuid",
      "version": [2, 1, 0]
    }
  ]
}"""
        )

        val mod = BedrockMod(
            id = "oss_tameable_wyverns",
            name = "Tameable Drakes & Wyvern Mounts",
            description = "Hatch dragon eggs in fire or ice, tame growing wyverns, equip custom netherite saddles, and take flight with aerial combat.",
            author = "DragonRiders Community",
            version = "2.1.0",
            category = "Mobs",
            bpUuidHeader = bpHeaderUuid,
            bpUuidModule = bpModuleUuid,
            rpUuidHeader = rpHeaderUuid,
            rpUuidModule = rpModuleUuid,
            files = files,
            primaryColorHex = "#F97316",
            iconEmoji = "🐲"
        )

        return OpenSourceMod(
            id = "oss_tameable_wyverns",
            name = "Tameable Drakes & Wyvern Mounts",
            author = "DragonRiders Community",
            category = "Mobs",
            description = "Raise your own dragon companion from an egg! Feed with raw beef, craft armored saddles, and command them to breathe fiery blasts.",
            license = "GPL-3.0",
            repositoryUrl = "https://github.com/dragonriders/wyvern-mounts",
            version = "2.1.0",
            compatibleVersions = "1.20 - 1.21+",
            downloadCount = "390K+",
            rating = 4.9,
            iconEmoji = "🐲",
            primaryColorHex = "#F97316",
            sizeText = "3.1 MB",
            tags = listOf("Mobs", "Dragons", "Flying", "Tameable"),
            features = listOf(
                "4 Elemental Breeds: Inferno, Glacial, Storm, Forest",
                "Full growth cycle: Nest Egg -> Hatchling -> Adult Wyvern",
                "Smooth flying mount controls with spacebar ascend",
                "Craftable dragon armor sets and chest saddle storage"
            ),
            templateMod = mod
        )
    }
}
