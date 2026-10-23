package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.OpenSourceMod
import com.example.data.repository.OpenSourceModsCatalog
import com.example.ui.theme.*
import com.example.ui.viewmodel.StudioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenSourceLibraryScreen(viewModel: StudioViewModel) {
    val context = LocalContext.current
    val savedMods by viewModel.savedMods.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedLicense by remember { mutableStateOf("All") }
    var selectedModForDetails by remember { mutableStateOf<OpenSourceMod?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }

    val categories = remember {
        listOf("All", "Weapons", "Magic", "Tech", "Mobs", "Vehicles", "Survival", "World", "Decor", "Utilities")
    }

    val licenses = remember {
        listOf("All", "MIT", "Apache-2.0", "GPL-3.0", "CC-BY-4.0")
    }

    val allMods = remember { OpenSourceModsCatalog.curatedMods }

    val filteredMods = remember(searchQuery, selectedCategory, selectedLicense, allMods) {
        allMods.filter { mod ->
            val matchesCategory = selectedCategory == "All" || mod.category.equals(selectedCategory, ignoreCase = true)
            val matchesLicense = selectedLicense == "All" || mod.license.equals(selectedLicense, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                    mod.name.contains(searchQuery, ignoreCase = true) ||
                    mod.description.contains(searchQuery, ignoreCase = true) ||
                    mod.author.contains(searchQuery, ignoreCase = true) ||
                    mod.tags.any { it.contains(searchQuery, ignoreCase = true) }

            matchesCategory && matchesLicense && matchesSearch
        }
    }

    val savedModIds = remember(savedMods) { savedMods.map { it.id }.toSet() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDark)
    ) {
        // Top Header
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(EmeraldPrimary, DiamondCyan)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Open Source Mods",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = EmeraldPrimary.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = "NO ADS • DIRECT DL",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldPrimary,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Download community open source addons directly to app",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    // Action Buttons (Download All + Import)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilledTonalButton(
                            onClick = { viewModel.downloadAllStarterMods() },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = EmeraldPrimary.copy(alpha = 0.15f),
                                contentColor = EmeraldPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("download_all_mods_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("DL All (10)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        IconButton(
                            onClick = { showImportDialog = true },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ObsidianCard)
                                .border(1.dp, ObsidianBorder, RoundedCornerShape(8.dp))
                                .testTag("import_custom_mod_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Import Custom Mod",
                                tint = DiamondCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Search open source weapons, tech, bosses, biomes...",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = DiamondCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ObsidianDark,
                        unfocusedContainerColor = ObsidianDark,
                        focusedBorderColor = DiamondCyan,
                        unfocusedBorderColor = ObsidianBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("search_mods_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { cat ->
                        val isSelected = selectedCategory == cat
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) DiamondCyan else ObsidianCard,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) DiamondCyan else ObsidianBorder
                            ),
                            modifier = Modifier
                                .clickable { selectedCategory = cat }
                                .testTag("filter_cat_$cat")
                        ) {
                            Text(
                                text = cat,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.Black else TextPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // License Filter Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "License:",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(licenses) { lic ->
                            val isSelected = selectedLicense == lic
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) ObsidianLight else Color.Transparent,
                                border = BorderStroke(
                                    0.5.dp,
                                    if (isSelected) DiamondCyan.copy(alpha = 0.5f) else ObsidianBorder
                                ),
                                modifier = Modifier.clickable { selectedLicense = lic }
                            ) {
                                Text(
                                    text = lic,
                                    fontSize = 10.sp,
                                    color = if (isSelected) DiamondCyan else TextSecondary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Mod Count Info Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${filteredMods.size} open-source mods available",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${savedMods.size} downloaded to app",
                    fontSize = 11.sp,
                    color = EmeraldPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Mods List
        if (filteredMods.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No open source mods found",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Try clearing search filters or importing a custom mod",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("open_source_mods_list")
            ) {
                items(filteredMods, key = { it.id }) { modItem ->
                    val isDownloaded = savedModIds.contains(modItem.id) || savedModIds.contains(modItem.templateMod.id)

                    OpenSourceModCard(
                        mod = modItem,
                        isDownloaded = isDownloaded,
                        onDownload = { openInEditor ->
                            viewModel.downloadOpenSourceMod(modItem, openInEditorAfter = openInEditor)
                        },
                        onOpenEditor = {
                            val targetMod = savedMods.firstOrNull { it.id == modItem.id || it.name == modItem.name } ?: modItem.templateMod
                            viewModel.openModInEditor(targetMod)
                        },
                        onTestMinecraft = {
                            val targetMod = savedMods.firstOrNull { it.id == modItem.id || it.name == modItem.name } ?: modItem.templateMod
                            viewModel.testInMinecraft(targetMod)
                        },
                        onRemix = {
                            viewModel.remixOpenSourceMod(modItem)
                        },
                        onViewDetails = {
                            selectedModForDetails = modItem
                        }
                    )
                }
            }
        }
    }

    // Mod Details Dialog
    selectedModForDetails?.let { mod ->
        ModDetailDialog(
            mod = mod,
            isDownloaded = savedModIds.contains(mod.id) || savedModIds.contains(mod.templateMod.id),
            onDownload = {
                viewModel.downloadOpenSourceMod(mod)
                selectedModForDetails = null
            },
            onOpenEditor = {
                val targetMod = savedMods.firstOrNull { it.id == mod.id || it.name == mod.name } ?: mod.templateMod
                viewModel.openModInEditor(targetMod)
                selectedModForDetails = null
            },
            onTestMinecraft = {
                val targetMod = savedMods.firstOrNull { it.id == mod.id || it.name == mod.name } ?: mod.templateMod
                viewModel.testInMinecraft(targetMod)
                selectedModForDetails = null
            },
            onDismiss = { selectedModForDetails = null }
        )
    }

    // Import Custom Mod Dialog
    if (showImportDialog) {
        ImportCustomModDialog(
            onImport = { name, desc, cat, files ->
                viewModel.importCustomMod(name, desc, cat, files)
                showImportDialog = false
            },
            onDismiss = { showImportDialog = false }
        )
    }
}

@Composable
fun OpenSourceModCard(
    mod: OpenSourceMod,
    isDownloaded: Boolean,
    onDownload: (openInEditor: Boolean) -> Unit,
    onOpenEditor: () -> Unit,
    onTestMinecraft: () -> Unit,
    onRemix: () -> Unit,
    onViewDetails: () -> Unit
) {
    val accentColor = remember(mod.primaryColorHex) {
        try {
            Color(android.graphics.Color.parseColor(mod.primaryColorHex))
        } catch (e: Exception) {
            DiamondCyan
        }
    }

    Surface(
        color = ObsidianCard,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.dp,
            if (isDownloaded) EmeraldPrimary.copy(alpha = 0.5f) else ObsidianBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mod_card_${mod.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Emoji icon, Title, Author, License & Downloaded Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentColor.copy(alpha = 0.15f))
                            .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = mod.iconEmoji, fontSize = 22.sp)
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = mod.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "by ${mod.author}",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )

                            Text(text = "•", fontSize = 10.sp, color = TextSecondary)

                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = ObsidianLight,
                                border = BorderStroke(0.5.dp, ObsidianBorder)
                            ) {
                                Text(
                                    text = mod.category,
                                    fontSize = 10.sp,
                                    color = DiamondCyan,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Badges (License + Downloaded status)
                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isDownloaded) EmeraldPrimary.copy(alpha = 0.2f) else ObsidianLight,
                        border = BorderStroke(
                            1.dp,
                            if (isDownloaded) EmeraldPrimary else ObsidianBorder
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            if (isDownloaded) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Downloaded",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = DiamondCyan,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = mod.license,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DiamondCyan
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "v${mod.version} • ${mod.sizeText}",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Description
            Text(
                text = mod.description,
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tags row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(mod.tags) { tag ->
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = ObsidianDark,
                        border = BorderStroke(0.5.dp, ObsidianBorder)
                    ) {
                        Text(
                            text = "#$tag",
                            fontSize = 9.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Secondary actions (Details & Remix)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onViewDetails,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, ObsidianBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("details_mod_${mod.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = DiamondCyan
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Details", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = onRemix,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, ObsidianBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("remix_mod_${mod.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = Color(0xFFA855F7)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Remix AI", fontSize = 11.sp)
                    }
                }

                // Primary Action Button (Download vs Open)
                if (isDownloaded) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilledTonalButton(
                            onClick = onOpenEditor,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = ObsidianLight,
                                contentColor = TextPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("edit_mod_${mod.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = DiamondCyan
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Editor", fontSize = 11.sp)
                        }

                        Button(
                            onClick = onTestMinecraft,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("test_mc_${mod.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color.Black
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Minecraft",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = { onDownload(false) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DiamondCyan),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("download_mod_${mod.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color.Black
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Download to App",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModDetailDialog(
    mod: OpenSourceMod,
    isDownloaded: Boolean,
    onDownload: () -> Unit,
    onOpenEditor: () -> Unit,
    onTestMinecraft: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = ObsidianCard,
            border = BorderStroke(1.dp, DiamondCyan.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                // Title and Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(text = mod.iconEmoji, fontSize = 28.sp)
                        Column {
                            Text(
                                text = mod.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "v${mod.version} • ${mod.category} • License: ${mod.license}",
                                fontSize = 11.sp,
                                color = DiamondCyan
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Metadata Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ObsidianDark,
                        border = BorderStroke(0.5.dp, ObsidianBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Author", fontSize = 10.sp, color = TextSecondary)
                            Text(mod.author, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ObsidianDark,
                        border = BorderStroke(0.5.dp, ObsidianBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Compatibility", fontSize = 10.sp, color = TextSecondary)
                            Text(mod.compatibleVersions, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ObsidianDark,
                        border = BorderStroke(0.5.dp, ObsidianBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Rating", fontSize = 10.sp, color = TextSecondary)
                            Text("★ ${mod.rating}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFBBF24))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Description
                Text(
                    text = "About this Mod:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = mod.description,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Key Features
                if (mod.features.isNotEmpty()) {
                    Text(
                        text = "Included Features:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    mod.features.forEach { feat ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier
                                    .size(14.dp)
                                    .padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = feat, fontSize = 11.sp, color = TextPrimary)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // File Structure summary
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ObsidianDark,
                    border = BorderStroke(0.5.dp, ObsidianBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Package Structure", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DiamondCyan)
                            Text("${mod.templateMod.files.size} JSON/Script files", fontSize = 10.sp, color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "BP UUID: ${mod.templateMod.bpUuidHeader.take(8)}... • RP UUID: ${mod.templateMod.rpUuidHeader.take(8)}...",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Dialog Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isDownloaded) {
                        OutlinedButton(
                            onClick = onOpenEditor,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, ObsidianBorder)
                        ) {
                            Text("Open in Editor", fontSize = 12.sp, color = TextPrimary)
                        }

                        Button(
                            onClick = onTestMinecraft,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Text("Launch in MC", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    } else {
                        Button(
                            onClick = onDownload,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DiamondCyan)
                        ) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Download Mod to App Now", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImportCustomModDialog(
    onImport: (name: String, desc: String, category: String, files: Map<String, String>) -> Unit,
    onDismiss: () -> Unit
) {
    var modName by remember { mutableStateOf("") }
    var modDesc by remember { mutableStateOf("") }
    var modCategory by remember { mutableStateOf("Weapons") }
    var modJson by remember {
        mutableStateOf("""{
  "format_version": "1.21.0",
  "minecraft:item": {
    "description": {
      "identifier": "custom:my_item"
    },
    "components": {
      "minecraft:damage": 10,
      "minecraft:max_stack_size": 1
    }
  }
}""")
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = ObsidianCard,
            border = BorderStroke(1.dp, DiamondCyan),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Import Custom Mod",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = modName,
                    onValueChange = { modName = it },
                    label = { Text("Mod Name") },
                    placeholder = { Text("e.g. Plasma Blaster") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = modDesc,
                    onValueChange = { modDesc = it },
                    label = { Text("Description") },
                    placeholder = { Text("e.g. Custom energy weapon with projectile blast") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("Bedrock Component JSON Definition:", fontSize = 11.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = modJson,
                    onValueChange = { modJson = it },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (modName.isNotBlank()) {
                            val files = mapOf(
                                "items/${modName.lowercase().replace(" ", "_")}.json" to modJson,
                                "behavior_packs/manifest.json" to """{
  "format_version": 2,
  "header": {
    "name": "$modName BP",
    "description": "$modDesc",
    "uuid": "${java.util.UUID.randomUUID()}",
    "version": [1, 0, 0],
    "min_engine_version": [1, 21, 0]
  },
  "modules": [
    {
      "type": "data",
      "uuid": "${java.util.UUID.randomUUID()}",
      "version": [1, 0, 0]
    }
  ]
}"""
                            )
                            onImport(modName, modDesc, modCategory, files)
                        }
                    },
                    enabled = modName.isNotBlank(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DiamondCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Import & Download to App", fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
        }
    }
}
