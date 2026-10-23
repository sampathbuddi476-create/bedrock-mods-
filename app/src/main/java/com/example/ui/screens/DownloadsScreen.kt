package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.StudioViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class DownloadedPackItem(
    val file: File,
    val name: String,
    val sizeBytes: Long,
    val modifiedTime: Long,
    val extension: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(viewModel: StudioViewModel) {
    val context = LocalContext.current
    var downloadItems by remember { mutableStateOf<List<DownloadedPackItem>>(emptyList()) }
    var selectedFilter by remember { mutableStateOf("All") }

    fun refreshDownloads() {
        val exportDir = File(context.cacheDir, "mc_exports")
        val files = exportDir.listFiles()?.filter {
            it.isFile && (it.name.endsWith(".mcaddon") || it.name.endsWith(".mcpack") || it.name.endsWith(".zip"))
        }?.map { file ->
            DownloadedPackItem(
                file = file,
                name = file.name,
                sizeBytes = file.length(),
                modifiedTime = file.lastModified(),
                extension = file.extension
            )
        }?.sortedByDescending { it.modifiedTime } ?: emptyList()
        downloadItems = files
    }

    LaunchedEffect(Unit) {
        refreshDownloads()
    }

    val totalSizeBytes = downloadItems.sumOf { it.sizeBytes }
    val totalSizeMb = String.format(Locale.US, "%.2f MB", totalSizeBytes / (1024.0 * 1024.0))

    val filteredList = when (selectedFilter) {
        "MCADDON" -> downloadItems.filter { it.extension.equals("mcaddon", ignoreCase = true) }
        "MCPACK" -> downloadItems.filter { it.extension.equals("mcpack", ignoreCase = true) }
        else -> downloadItems
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDark)
    ) {
        // Downloads Top Bar
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(BedrockBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DownloadForOffline,
                                contentDescription = "Downloads",
                                tint = ObsidianDark,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Exported Downloads & Packs",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "${downloadItems.size} Addon files • $totalSizeMb storage",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = { refreshDownloads() },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ObsidianCard)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = DiamondCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Filter chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("All", "MCADDON", "MCPACK").forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BedrockBlue.copy(alpha = 0.2f),
                                selectedLabelColor = BedrockBlue,
                                containerColor = ObsidianCard,
                                labelColor = TextSecondary
                            ),
                            border = BorderStroke(1.dp, if (selectedFilter == filter) BedrockBlue else ObsidianBorder)
                        )
                    }
                }
            }
        }

        // Downloads List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredList, key = { it.file.absolutePath }) { item ->
                DownloadItemCard(
                    item = item,
                    onOpenInMinecraft = {
                        try {
                            val uri: Uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                item.file
                            )
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "application/octet-stream")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                setPackage("com.mojang.minecraftpe")
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            val uri: Uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                item.file
                            )
                            val chooser = Intent.createChooser(
                                Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "application/octet-stream")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                },
                                "Open in Minecraft"
                            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                            context.startActivity(chooser)
                        }
                    },
                    onShare = {
                        val uri: Uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            item.file
                        )
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/octet-stream"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            putExtra(Intent.EXTRA_SUBJECT, item.name)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Addon Pack"))
                    },
                    onDelete = {
                        item.file.delete()
                        refreshDownloads()
                    }
                )
            }

            if (filteredList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📥", fontSize = 42.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No downloaded addon packages yet", color = TextSecondary, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Download mods directly from the Open Source Library or export your own mods", color = TextMuted, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { viewModel.setScreen(AppScreen.MODS) },
                                colors = ButtonDefaults.buttonColors(containerColor = DiamondCyan, contentColor = ObsidianDark)
                            ) {
                                Text("Browse My Addons")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadItemCard(
    item: DownloadedPackItem,
    onOpenInMinecraft: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(item.modifiedTime) {
        val sdf = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
        sdf.format(Date(item.modifiedTime))
    }
    val sizeKb = item.sizeBytes / 1024

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("download_card_${item.name}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ObsidianSurface)
                            .border(1.dp, ObsidianBorder, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (item.extension == "mcaddon") "📦" else "📄", fontSize = 18.sp)
                    }

                    Column {
                        Text(
                            text = item.name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1
                        )
                        Text(
                            text = "$sizeKb KB • $dateStr",
                            fontSize = 10.sp,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = DiamondCyan.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = item.extension.uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = DiamondCyan,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenInMinecraft,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DiamondCyan,
                        contentColor = ObsidianDark
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("launch_mc_download_${item.name}")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("1-Tap Open in MC", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ObsidianSurface)
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = RedstoneRed, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
