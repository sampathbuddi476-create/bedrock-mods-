package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.StudioViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: StudioViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BedrockStudioTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: StudioViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val statusBanner by viewModel.statusBanner.collectAsState()
    val showVideoDialog by viewModel.showVideoDialog.collectAsState()
    val showValidationDialog by viewModel.showValidationDialog.collectAsState()
    val validationResult by viewModel.validationResult.collectAsState()
    val validationPendingMod by viewModel.validationPendingMod.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ObsidianDark,
        bottomBar = {
            StudioBottomNavBar(
                currentScreen = currentScreen,
                onNavigate = { viewModel.setScreen(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                AppScreen.LIBRARY -> OpenSourceLibraryScreen(viewModel = viewModel)
                AppScreen.MODS -> ModsListScreen(viewModel = viewModel)
                AppScreen.CHAT -> ChatScreen(viewModel = viewModel)
                AppScreen.EDITOR -> ModEditorScreen(viewModel = viewModel)
                AppScreen.DOWNLOADS -> DownloadsScreen(viewModel = viewModel)
            }

            // Top Status / Notification Banner
            AnimatedVisibility(
                visible = statusBanner != null,
                enter = slideInVertically { -it },
                exit = slideOutVertically { -it },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(12.dp)
            ) {
                statusBanner?.let { msg ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ObsidianCard,
                        border = BorderStroke(1.dp, DiamondCyan.copy(alpha = 0.6f)),
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .clickable { viewModel.clearStatusBanner() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = DiamondCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = msg,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Video understanding modal
            if (showVideoDialog) {
                VideoAnalysisDialog(viewModel = viewModel)
            }

            // Bedrock Addon Manifest & Structure Validation Dialog
            if (showValidationDialog && validationPendingMod != null && validationResult != null) {
                ValidationDialog(
                    mod = validationPendingMod!!,
                    validationResult = validationResult!!,
                    onAutoFixAndProceed = { viewModel.autoFixAndProceed() },
                    onDismiss = { viewModel.dismissValidationDialog() },
                    onOpenEditor = {
                        viewModel.openModInEditor(validationPendingMod!!)
                        viewModel.dismissValidationDialog()
                    }
                )
            }
        }
    }
}

@Composable
fun StudioBottomNavBar(
    currentScreen: AppScreen,
    onNavigate: (AppScreen) -> Unit
) {
    NavigationBar(
        containerColor = ObsidianSurface,
        tonalElevation = 8.dp,
        modifier = Modifier.testTag("bottom_nav_bar")
    ) {
        // Open Source Mods Library (first / default tab)
        NavigationBarItem(
            selected = currentScreen == AppScreen.LIBRARY,
            onClick = { onNavigate(AppScreen.LIBRARY) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Explore,
                    contentDescription = "Open Source Mods Library",
                    modifier = Modifier.size(20.dp)
                )
            },
            label = { Text("OSS Library", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DiamondCyan,
                selectedTextColor = DiamondCyan,
                indicatorColor = ObsidianCard,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            ),
            modifier = Modifier.testTag("nav_item_library")
        )

        NavigationBarItem(
            selected = currentScreen == AppScreen.MODS,
            onClick = { onNavigate(AppScreen.MODS) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Widgets,
                    contentDescription = "My Addons",
                    modifier = Modifier.size(20.dp)
                )
            },
            label = { Text("My Addons", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DiamondCyan,
                selectedTextColor = DiamondCyan,
                indicatorColor = ObsidianCard,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            ),
            modifier = Modifier.testTag("nav_item_mods")
        )

        // AI Chat tab
        NavigationBarItem(
            selected = currentScreen == AppScreen.CHAT,
            onClick = { onNavigate(AppScreen.CHAT) },
            icon = {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "AI Chat",
                    modifier = Modifier.size(20.dp)
                )
            },
            label = { Text("AI Chat", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DiamondCyan,
                selectedTextColor = DiamondCyan,
                indicatorColor = ObsidianCard,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            ),
            modifier = Modifier.testTag("nav_item_chat")
        )

        NavigationBarItem(
            selected = currentScreen == AppScreen.DOWNLOADS,
            onClick = { onNavigate(AppScreen.DOWNLOADS) },
            icon = {
                Icon(
                    imageVector = Icons.Default.DownloadForOffline,
                    contentDescription = "Downloads",
                    modifier = Modifier.size(20.dp)
                )
            },
            label = { Text("Downloads", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DiamondCyan,
                selectedTextColor = DiamondCyan,
                indicatorColor = ObsidianCard,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            ),
            modifier = Modifier.testTag("nav_item_downloads")
        )
    }
}
