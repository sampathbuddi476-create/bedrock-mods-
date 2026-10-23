package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BedrockMod
import com.example.service.exporter.BedrockExporter
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.StudioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModEditorScreen(viewModel: StudioViewModel) {
    val mod by viewModel.activeMod.collectAsState()
    val selectedPath by viewModel.selectedFilePath.collectAsState()
    val editorContent by viewModel.fileEditorContent.collectAsState()
    val context = LocalContext.current

    var activeTab by remember { mutableStateOf("EDITOR") } // "EDITOR", "PREVIEW_3D", "DEBUGGER"
    var showAddFileDialog by remember { mutableStateOf(false) }

    // Intercept back button to return to Mods screen
    BackHandler {
        viewModel.setScreen(AppScreen.MODS)
    }

    val safFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null && mod != null) {
            viewModel.exportToDirectoryUri(uri, mod!!)
        }
    }

    if (mod == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianDark),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No mod selected", color = TextSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.setScreen(AppScreen.MODS) },
                    colors = ButtonDefaults.buttonColors(containerColor = DiamondCyan, contentColor = ObsidianDark)
                ) {
                    Text("Select a Mod")
                }
            }
        }
        return
    }

    val activeMod = mod!!

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDark)
    ) {
        // Top Toolbar
        Surface(
            color = ObsidianSurface,
            border = BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.setScreen(AppScreen.MODS) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(activeMod.iconEmoji, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = activeMod.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = "UUID: ${activeMod.bpUuidHeader.take(8)}... • format 1.20.80",
                            fontSize = 10.sp,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Action buttons: Save & 1-Tap Test in Minecraft
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = { viewModel.saveCurrentEditorFile() },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ObsidianCard)
                            .testTag("save_file_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save File",
                            tint = EmeraldGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Button(
                        onClick = { viewModel.testInMinecraft(activeMod) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DiamondCyan,
                            contentColor = ObsidianDark
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("test_minecraft_editor_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Test in MC", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Studio Mode Tabs: Code Editor | 3D Visual Model | Live Validator Debugger
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ObsidianSurface)
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = activeTab == "EDITOR",
                onClick = { activeTab = "EDITOR" },
                label = { Text("Code Editor", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                leadingIcon = {
                    Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(14.dp))
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BedrockBlue.copy(alpha = 0.2f),
                    selectedLabelColor = BedrockBlue,
                    selectedLeadingIconColor = BedrockBlue
                ),
                border = BorderStroke(1.dp, if (activeTab == "EDITOR") BedrockBlue else ObsidianBorder),
                modifier = Modifier.testTag("tab_editor")
            )

            FilterChip(
                selected = activeTab == "PREVIEW_3D",
                onClick = { activeTab = "PREVIEW_3D" },
                label = { Text("3D Visual Model", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                leadingIcon = {
                    Icon(Icons.Default.ViewInAr, contentDescription = null, modifier = Modifier.size(14.dp))
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = DiamondCyan.copy(alpha = 0.2f),
                    selectedLabelColor = DiamondCyan,
                    selectedLeadingIconColor = DiamondCyan
                ),
                border = BorderStroke(1.dp, if (activeTab == "PREVIEW_3D") DiamondCyan else ObsidianBorder),
                modifier = Modifier.testTag("tab_preview_3d")
            )

            FilterChip(
                selected = activeTab == "DEBUGGER",
                onClick = { activeTab = "DEBUGGER" },
                label = { Text("Live Debugger", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                leadingIcon = {
                    Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(14.dp))
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EmeraldGreen.copy(alpha = 0.2f),
                    selectedLabelColor = EmeraldGreen,
                    selectedLeadingIconColor = EmeraldGreen
                ),
                border = BorderStroke(1.dp, if (activeTab == "DEBUGGER") EmeraldGreen else ObsidianBorder),
                modifier = Modifier.testTag("tab_debugger")
            )
        }

        when (activeTab) {
            "PREVIEW_3D" -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    BedrockVisualPreview3D(mod = activeMod)
                }
            }
            "DEBUGGER" -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    ValidationDebugPanel(
                        mod = activeMod,
                        onAutoFix = { viewModel.autoFixAndProceed() }
                    )
                }
            }
            else -> {
                Column(modifier = Modifier.weight(1f)) {
                    // File Tabs Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ObsidianCard)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        activeMod.files.keys.forEach { path ->
                            val isSelected = path == selectedPath
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) ObsidianDark else ObsidianSurface,
                                border = BorderStroke(1.dp, if (isSelected) BedrockBlue else ObsidianBorder),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.selectEditorFile(path) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = when {
                                            path.endsWith(".js") -> Icons.Default.Javascript
                                            path.contains("items/") -> Icons.Default.Hardware
                                            path.contains("entities/") -> Icons.Default.Pets
                                            path.contains("blocks/") -> Icons.Default.Widgets
                                            path.contains("recipes/") -> Icons.AutoMirrored.Filled.MenuBook
                                            else -> Icons.Default.Description
                                        },
                                        contentDescription = null,
                                        tint = if (isSelected) BedrockBlue else TextMuted,
                                        modifier = Modifier.size(14.dp)
                                    )

                                    Text(
                                        text = path.substringAfterLast("/"),
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) TextPrimary else TextSecondary,
                                        fontFamily = FontFamily.Monospace
                                    )

                                    if (activeMod.files.size > 1) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove file",
                                            tint = TextMuted,
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clickable { viewModel.deleteFileFromMod(path) }
                                        )
                                    }
                                }
                            }
                        }

                        // Add File Button
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ObsidianSurface,
                            border = BorderStroke(1.dp, DiamondCyan.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showAddFileDialog = true }
                                .testTag("add_file_tab_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add File",
                                    tint = DiamondCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text("New File", fontSize = 11.sp, color = DiamondCyan, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // Active File Path Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CodeBackground)
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = selectedPath ?: "No file open",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = BedrockBlue
                        )
                        Text(
                            text = "${editorContent.lines().size} lines",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }

                    // Code Editor View
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(CodeBackground)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Line Numbers Gutter
                            val lineCount = maxOf(1, editorContent.lines().size)
                            Column(
                                modifier = Modifier
                                    .background(ObsidianDark)
                                    .padding(horizontal = 8.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                for (i in 1..lineCount) {
                                    Text(
                                        text = "$i",
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = TextMuted,
                                        lineHeight = 18.sp
                                    )
                                }
                            }

                            // Code Input Area
                            BasicTextField(
                                value = editorContent,
                                onValueChange = { viewModel.updateEditorContent(it) },
                                textStyle = TextStyle(
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 18.sp
                                ),
                                cursorBrush = SolidColor(DiamondCyan),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(12.dp)
                                    .testTag("code_editor_text_field")
                            )
                        }
                    }
                }
            }
        }

        // Bottom Action Bar: SAF Export & Share
        Surface(
            color = ObsidianSurface,
            border = BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { safFolderLauncher.launch(null) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BedrockBlue),
                    border = BorderStroke(1.dp, BedrockBlue.copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export to Game Folder (SAF)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                IconButton(
                    onClick = {
                        val shareIntent = BedrockExporter.shareModFile(context, activeMod)
                        context.startActivity(Intent.createChooser(shareIntent, "Share Addon"))
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ObsidianCard)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }

    if (showAddFileDialog) {
        AddFileDialog(
            onDismiss = { showAddFileDialog = false },
            onAddFile = { fileName, template ->
                viewModel.addNewFileToMod(fileName, template)
                showAddFileDialog = false
            }
        )
    }
}

@Composable
fun AddFileDialog(
    onDismiss: () -> Unit,
    onAddFile: (String, String) -> Unit
) {
    var selectedType by remember { mutableStateOf("Item") }
    var fileNameInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Bedrock File", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Select Component Type:", fontSize = 12.sp, color = TextSecondary)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Item", "Block", "Entity", "Script (JS)", "Recipe").forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = {
                                selectedType = type
                                if (fileNameInput.isEmpty()) {
                                    fileNameInput = when (type) {
                                        "Item" -> "custom_item.json"
                                        "Block" -> "custom_block.json"
                                        "Entity" -> "custom_entity.json"
                                        "Script (JS)" -> "main.js"
                                        "Recipe" -> "custom_recipe.json"
                                        else -> "file.json"
                                    }
                                }
                            },
                            label = { Text(type, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BedrockBlue.copy(alpha = 0.2f),
                                selectedLabelColor = BedrockBlue
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = fileNameInput,
                    onValueChange = { fileNameInput = it },
                    label = { Text("File Name or Relative Path") },
                    placeholder = { Text("e.g., items/fire_sword.json") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rawName = fileNameInput.trim()
                    if (rawName.isNotEmpty()) {
                        val finalPath = when (selectedType) {
                            "Item" -> if (rawName.startsWith("items/")) rawName else "items/$rawName"
                            "Block" -> if (rawName.startsWith("blocks/")) rawName else "blocks/$rawName"
                            "Entity" -> if (rawName.startsWith("entities/")) rawName else "entities/$rawName"
                            "Script (JS)" -> if (rawName.startsWith("scripts/")) rawName else "scripts/$rawName"
                            "Recipe" -> if (rawName.startsWith("recipes/")) rawName else "recipes/$rawName"
                            else -> rawName
                        }
                        val template = when (selectedType) {
                            "Item" -> """
{
  "format_version": "1.20.80",
  "minecraft:item": {
    "description": { "identifier": "studio:${rawName.substringBeforeLast(".")}" },
    "components": {
      "minecraft:display_name": { "value": "Custom Item" },
      "minecraft:damage": 10
    }
  }
}
                            """.trimIndent()
                            "Script (JS)" -> """
import { world, system } from "@minecraft/server";

world.afterEvents.playerSpawn.subscribe((event) => {
    event.player.sendMessage("Welcome! Bedrock Mod Script active.");
});
                            """.trimIndent()
                            else -> "{\n  \"format_version\": \"1.20.80\"\n}"
                        }
                        onAddFile(finalPath, template)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DiamondCyan, contentColor = ObsidianDark)
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = ObsidianCard
    )
}
