package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BedrockMod
import com.example.data.model.ModVersionSnapshot
import com.example.service.exporter.BedrockExporter
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.StudioViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModsListScreen(viewModel: StudioViewModel) {
    val mods by viewModel.savedMods.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    val context = LocalContext.current

    var pendingExportMod by remember { mutableStateOf<BedrockMod?>(null) }
    var versionHistoryMod by remember { mutableStateOf<BedrockMod?>(null) }
    var duplicateModTarget by remember { mutableStateOf<BedrockMod?>(null) }

    val safFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null && pendingExportMod != null) {
            viewModel.exportToDirectoryUri(uri, pendingExportMod!!)
            pendingExportMod = null
        }
    }

    val categories = listOf("All", "Weapons & Combat", "Bosses & Entities", "Blocks & Mining", "Minigames & Fun", "Custom Addon")

    val filteredMods = mods.filter { mod ->
        val matchesSearch = mod.name.contains(searchQuery, ignoreCase = true) ||
                mod.description.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == "All" || mod.category.contains(selectedCategory, ignoreCase = true)
        matchesSearch && matchesCategory
    }

    Scaffold(
        containerColor = ObsidianDark,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.setScreen(AppScreen.CHAT) },
                containerColor = DiamondCyan,
                contentColor = ObsidianDark,
                modifier = Modifier.testTag("create_mod_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create New Mod")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header Bar
            Surface(
                color = ObsidianSurface,
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Bedrock Addon Library",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "${mods.size} Addons Ready for Minecraft Bedrock Testing",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }

                        IconButton(
                            onClick = { viewModel.setScreen(AppScreen.CHAT) },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(ObsidianCard)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Studio",
                                tint = GeminiSparkleCyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Search input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search installed addons...", fontSize = 13.sp, color = TextMuted) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("mod_search_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BedrockBlue,
                            unfocusedBorderColor = ObsidianBorder,
                            focusedContainerColor = ObsidianDark,
                            unfocusedContainerColor = ObsidianDark,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )
                }
            }

            // Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BedrockBlue.copy(alpha = 0.2f),
                            selectedLabelColor = BedrockBlue,
                            containerColor = ObsidianCard,
                            labelColor = TextSecondary
                        ),
                        border = BorderStroke(1.dp, if (selectedCategory == cat) BedrockBlue else ObsidianBorder)
                    )
                }
            }

            // Addon Cards List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredMods, key = { it.id }) { mod ->
                    ModLibraryCard(
                        mod = mod,
                        onTestInMinecraft = { viewModel.testInMinecraft(mod) },
                        onExportToFolder = {
                            pendingExportMod = mod
                            safFolderLauncher.launch(null)
                        },
                        onEdit = { viewModel.openModInEditor(mod) },
                        onShare = {
                            val shareIntent = BedrockExporter.shareModFile(context, mod)
                            context.startActivity(Intent.createChooser(shareIntent, "Share Addon"))
                        },
                        onVersionHistory = { versionHistoryMod = mod },
                        onDuplicate = { duplicateModTarget = mod },
                        onDelete = { viewModel.deleteMod(mod.id) }
                    )
                }

                if (filteredMods.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📦", fontSize = 42.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No mods match your query", color = TextSecondary, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { viewModel.setScreen(AppScreen.CHAT) },
                                    colors = ButtonDefaults.buttonColors(containerColor = DiamondCyan, contentColor = ObsidianDark)
                                ) {
                                    Text("Ask Gemini to build one!")
                                }
                            }
                        }
                    }
                }
            }

            // Version History Dialog
            versionHistoryMod?.let { targetMod ->
                val freshMod = mods.find { it.id == targetMod.id } ?: targetMod
                ModVersionHistoryDialog(
                    mod = freshMod,
                    onCreateSnapshot = { note ->
                        viewModel.createVersionSnapshot(freshMod.id, note)
                    },
                    onRevertSnapshot = { index ->
                        viewModel.revertToSnapshot(freshMod.id, index)
                    },
                    onDismiss = { versionHistoryMod = null }
                )
            }

            // Duplicate Mod Dialog
            duplicateModTarget?.let { targetMod ->
                DuplicateModDialog(
                    mod = targetMod,
                    onConfirmDuplicate = { newName ->
                        viewModel.duplicateMod(targetMod.id, newName)
                        duplicateModTarget = null
                    },
                    onDismiss = { duplicateModTarget = null }
                )
            }
        }
    }
}

@Composable
fun ModLibraryCard(
    mod: BedrockMod,
    onTestInMinecraft: () -> Unit,
    onExportToFolder: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onVersionHistory: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mod_card_${mod.name}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ObsidianSurface)
                            .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(mod.iconEmoji, fontSize = 22.sp)
                    }

                    Column {
                        Text(
                            text = mod.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${mod.category} • v${mod.version}",
                                fontSize = 11.sp,
                                color = DiamondCyan
                            )
                            if (mod.versionHistory.isNotEmpty()) {
                                Text(
                                    text = " (${mod.versionHistory.size} versions)",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                // Delete menu
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = mod.description,
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // File items preview badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = ObsidianSurface,
                    border = BorderStroke(1.dp, ObsidianBorder)
                ) {
                    Text(
                        text = "📦 ${mod.files.size} Addon Files",
                        fontSize = 10.sp,
                        color = BedrockBlue,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (mod.files.keys.any { it.endsWith(".js") }) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = EmeraldGreen.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "⚡ Script API Active",
                            fontSize = 10.sp,
                            color = EmeraldGreen,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 1-Tap Open with Minecraft Bedrock (Direct Test)
            Button(
                onClick = onTestInMinecraft,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DiamondCyan,
                    contentColor = ObsidianDark
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .testTag("launch_mc_button_${mod.name}")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "1-Tap Launch & Test in Minecraft",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sub-actions row with Code Editor, Game Folder, Version History, Clone, Share
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = onExportToFolder,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BedrockBlue),
                    border = BorderStroke(1.dp, BedrockBlue.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Game Folder", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                ) {
                    Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Code Editor", fontSize = 11.sp)
                }

                IconButton(
                    onClick = onVersionHistory,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ObsidianSurface)
                        .testTag("version_history_button_${mod.name}")
                ) {
                    Icon(Icons.Default.History, contentDescription = "Version History", tint = DiamondCyan, modifier = Modifier.size(16.dp))
                }

                IconButton(
                    onClick = onDuplicate,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ObsidianSurface)
                        .testTag("duplicate_button_${mod.name}")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = BedrockBlue, modifier = Modifier.size(16.dp))
                }

                IconButton(
                    onClick = onShare,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ObsidianSurface)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun ModVersionHistoryDialog(
    mod: BedrockMod,
    onCreateSnapshot: (String) -> Unit,
    onRevertSnapshot: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var checkpointNote by remember { mutableStateOf("") }
    var isAddingNote by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianCard,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.History, contentDescription = null, tint = DiamondCyan)
                Column {
                    Text("Version History & Checkpoints", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("${mod.name} • Current: ${mod.version}", fontSize = 12.sp, color = DiamondCyan)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                Text(
                    text = "Track, save, and revert mod file versions before editing or sharing.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (!isAddingNote) {
                    Button(
                        onClick = { isAddingNote = true },
                        colors = ButtonDefaults.buttonColors(containerColor = BedrockBlue, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(36.dp)
                    ) {
                        Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save New Checkpoint Snapshot", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ObsidianSurface, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        OutlinedTextField(
                            value = checkpointNote,
                            onValueChange = { checkpointNote = it },
                            placeholder = { Text("Note e.g. 'Added freeze effect to sword'", fontSize = 11.sp, color = TextMuted) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DiamondCyan,
                                unfocusedBorderColor = ObsidianBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = {
                                    onCreateSnapshot(checkpointNote.ifBlank { "Manual Checkpoint" })
                                    checkpointNote = ""
                                    isAddingNote = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DiamondCyan, contentColor = ObsidianDark),
                                modifier = Modifier.weight(1f).height(32.dp)
                            ) {
                                Text("Save", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { isAddingNote = false },
                                modifier = Modifier.weight(1f).height(32.dp)
                            ) {
                                Text("Cancel", fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("SNAPSHOT TIMELINE (${mod.versionHistory.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))

                if (mod.versionHistory.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No previous snapshots yet.\nClick 'Save New Checkpoint' to create one!", color = TextMuted, fontSize = 12.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(mod.versionHistory.size) { index ->
                            val snapshot = mod.versionHistory[index]
                            val dateStr = remember(snapshot.timestamp) {
                                val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
                                sdf.format(Date(snapshot.timestamp))
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ObsidianSurface,
                                border = BorderStroke(1.dp, ObsidianBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(snapshot.versionTag, fontWeight = FontWeight.Bold, color = DiamondCyan, fontSize = 12.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(dateStr, color = TextMuted, fontSize = 10.sp)
                                        }
                                        Text(snapshot.changeNote, color = TextPrimary, fontSize = 11.sp, maxLines = 1)
                                        Text("${snapshot.files.size} files captured", color = TextSecondary, fontSize = 10.sp)
                                    }

                                    Button(
                                        onClick = {
                                            onRevertSnapshot(index)
                                            onDismiss()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = BedrockBlue, contentColor = Color.White),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("Revert", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = DiamondCyan)
            }
        }
    )
}

@Composable
fun DuplicateModDialog(
    mod: BedrockMod,
    onConfirmDuplicate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember { mutableStateOf("${mod.name} (Copy)") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianCard,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = BedrockBlue)
                Text("Duplicate Addon", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        },
        text = {
            Column {
                Text(
                    text = "Create an independent duplicate of \"${mod.name}\". A new set of Bedrock UUIDs will be generated so both addons can be installed side-by-side in Minecraft Bedrock.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("New Mod Name", fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DiamondCyan,
                        unfocusedBorderColor = ObsidianBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmDuplicate(newName) },
                colors = ButtonDefaults.buttonColors(containerColor = DiamondCyan, contentColor = ObsidianDark),
                enabled = newName.isNotBlank()
            ) {
                Text("Duplicate", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
