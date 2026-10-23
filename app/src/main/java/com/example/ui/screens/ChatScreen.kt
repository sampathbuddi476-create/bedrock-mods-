package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.example.data.model.BedrockMod
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSession
import com.example.data.model.GroundingSource
import com.example.service.exporter.BedrockExporter
import com.example.service.gemini.GeminiService
import com.example.ui.theme.*
import com.example.ui.viewmodel.StudioViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: StudioViewModel) {
    val messages by viewModel.chatMessages.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    val isHighThinking by viewModel.isHighThinkingEnabled.collectAsState()
    val isSearchGrounding by viewModel.isSearchGroundingEnabled.collectAsState()
    val chatSessions by viewModel.chatSessions.collectAsState()
    val activeSessionId by viewModel.activeSessionId.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var showHistorySheet by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // SAF folder picker for direct game folder export
    var pendingExportMod by remember { mutableStateOf<BedrockMod?>(null) }
    val safFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null && pendingExportMod != null) {
            viewModel.exportToDirectoryUri(uri, pendingExportMod!!)
            pendingExportMod = null
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDark)
    ) {
        // Gemini App Chat Switcher Header
        GeminiChatTopHeader(
            activeSession = chatSessions.firstOrNull { it.id == activeSessionId },
            onOpenHistory = { showHistorySheet = true },
            onNewChat = { viewModel.createNewChatSession() }
        )

        // Top Model & Tools Bar
        ModelControlBar(
            selectedModel = selectedModel,
            isHighThinking = isHighThinking,
            isSearchGrounding = isSearchGrounding,
            onSelectModel = { viewModel.setSelectedModel(it) },
            onToggleThinking = { viewModel.toggleHighThinking() },
            onToggleSearch = { viewModel.toggleSearchGrounding() },
            onOpenVideoDialog = { viewModel.setShowVideoDialog(true) }
        )

        // Chat Messages Thread
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatMessageItem(
                    message = msg,
                    onOpenEditor = { mod -> viewModel.openModInEditor(mod) },
                    onTestInMinecraft = { mod -> viewModel.testInMinecraft(mod) },
                    onExportToFolder = { mod ->
                        pendingExportMod = mod
                        safFolderLauncher.launch(null)
                    },
                    onShareMod = { mod ->
                        val shareIntent = BedrockExporter.shareModFile(context, mod)
                        context.startActivity(Intent.createChooser(shareIntent, "Share Addon"))
                    }
                )
            }

            if (isGenerating) {
                item {
                    GeneratingIndicator(
                        modelName = selectedModel,
                        isHighThinking = isHighThinking,
                        isSearchGrounding = isSearchGrounding
                    )
                }
            }
        }

        // Quick Suggestion Chips (when user is ready to explore)
        QuickPromptChips(
            onSelectPrompt = { prompt ->
                inputText = prompt
            }
        )

        // Chat Input Box
        ChatInputBar(
            inputText = inputText,
            isGenerating = isGenerating,
            onTextChanged = { inputText = it },
            onSend = {
                val text = inputText.trim()
                if (text.isNotEmpty()) {
                    viewModel.sendMessage(text)
                    inputText = ""
                }
            },
            onAttachVideo = {
                viewModel.setShowVideoDialog(true)
            }
        )
    }

    if (showHistorySheet) {
        GeminiChatHistorySheet(
            sessions = chatSessions,
            activeSessionId = activeSessionId,
            onSelectSession = {
                viewModel.switchChatSession(it)
                showHistorySheet = false
            },
            onNewChat = {
                viewModel.createNewChatSession()
                showHistorySheet = false
            },
            onDeleteSession = { viewModel.deleteChatSession(it) },
            onDismiss = { showHistorySheet = false }
        )
    }
}

@Composable
fun GeminiChatTopHeader(
    activeSession: ChatSession?,
    onOpenHistory: () -> Unit,
    onNewChat: () -> Unit
) {
    Surface(
        color = Color(0xFF131924),
        border = BorderStroke(1.dp, Color(0xFF1F2937)),
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
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onOpenHistory,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ObsidianCard)
                        .testTag("chat_history_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Chat History",
                        tint = DiamondCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(
                    modifier = Modifier.clickable { onOpenHistory() }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Gemini Bedrock Studio",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = GeminiSparkleCyan,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = (activeSession?.title ?: "Mod Creation Chat").take(32),
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // New chat session button
            IconButton(
                onClick = onNewChat,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(ObsidianCard)
                    .testTag("new_chat_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Chat",
                    tint = DiamondCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiChatHistorySheet(
    sessions: List<ChatSession>,
    activeSessionId: String,
    onSelectSession: (String) -> Unit,
    onNewChat: () -> Unit,
    onDeleteSession: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextMuted) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxHeight(0.75f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = DiamondCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Gemini Chat History",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Text(
                    text = "${sessions.size} sessions",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Start New Chat Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, DiamondCyan.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onNewChat() }
                    .testTag("start_new_chat_card")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(DiamondCyan),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = ObsidianDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Start New Modding Chat",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Fresh context for a new addon or script idea",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "RECENT CONVERSATIONS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sessions, key = { it.id }) { session ->
                    val isActive = session.id == activeSessionId
                    val dateFormatted = remember(session.lastModified) {
                        val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                        sdf.format(Date(session.lastModified))
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isActive) Color(0xFF1E2838) else ObsidianCard,
                        border = BorderStroke(
                            1.dp,
                            if (isActive) DiamondCyan else ObsidianBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectSession(session.id) }
                            .testTag("session_item_${session.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (isActive) Icons.Default.ChatBubble else Icons.Default.ChatBubbleOutline,
                                    contentDescription = null,
                                    tint = if (isActive) DiamondCyan else TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = session.title,
                                            fontSize = 13.sp,
                                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (isActive) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = DiamondCyan.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = "ACTIVE",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = DiamondCyan,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = "$dateFormatted • ${session.messages.size} msgs",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            IconButton(
                                onClick = { onDeleteSession(session.id) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Delete",
                                    tint = TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModelControlBar(
    selectedModel: String,
    isHighThinking: Boolean,
    isSearchGrounding: Boolean,
    onSelectModel: (String) -> Unit,
    onToggleThinking: () -> Unit,
    onToggleSearch: () -> Unit,
    onOpenVideoDialog: () -> Unit
) {
    var showModelMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ObsidianSurface)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Model Selector Dropdown Button
        Box {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { showModelMenu = true }
                    .testTag("model_selector_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Model",
                        tint = GeminiSparkleCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = when (selectedModel) {
                            GeminiService.MODEL_PRO -> "Gemini 3.1 Pro"
                            GeminiService.MODEL_FLASH -> "Gemini 3.5 Flash"
                            else -> "Gemini 3.1 Lite"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = showModelMenu,
                onDismissRequest = { showModelMenu = false },
                modifier = Modifier.background(ObsidianCard)
            ) {
                DropdownMenuItem(
                    text = {
                        Column {
                            Text("Gemini 3.1 Pro (Preview)", color = TextPrimary, fontWeight = FontWeight.Bold)
                            Text("Complex addon code, High Thinking, Video Analysis", color = TextSecondary, fontSize = 11.sp)
                        }
                    },
                    onClick = {
                        onSelectModel(GeminiService.MODEL_PRO)
                        showModelMenu = false
                    }
                )
                DropdownMenuItem(
                    text = {
                        Column {
                            Text("Gemini 3.5 Flash", color = TextPrimary, fontWeight = FontWeight.Bold)
                            Text("Fast general tasks + Live Google Search Grounding", color = TextSecondary, fontSize = 11.sp)
                        }
                    },
                    onClick = {
                        onSelectModel(GeminiService.MODEL_FLASH)
                        showModelMenu = false
                    }
                )
                DropdownMenuItem(
                    text = {
                        Column {
                            Text("Gemini 3.1 Flash Lite", color = TextPrimary, fontWeight = FontWeight.Bold)
                            Text("Instant quick edits & JSON snippets", color = TextSecondary, fontSize = 11.sp)
                        }
                    },
                    onClick = {
                        onSelectModel(GeminiService.MODEL_LITE)
                        showModelMenu = false
                    }
                )
            }
        }

        // High Thinking Mode Toggle Chip
        FilterChip(
            selected = isHighThinking,
            onClick = onToggleThinking,
            label = {
                Text(
                    text = "High Thinking",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = "Thinking Mode",
                    modifier = Modifier.size(14.dp)
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = AmethystPurple.copy(alpha = 0.25f),
                selectedLabelColor = Color(0xFFD8B4FE),
                selectedLeadingIconColor = Color(0xFFD8B4FE),
                containerColor = ObsidianCard,
                labelColor = TextSecondary
            ),
            border = BorderStroke(1.dp, if (isHighThinking) AmethystPurple else ObsidianBorder),
            modifier = Modifier.testTag("thinking_mode_chip")
        )

        // Google Search Grounding Toggle Chip
        FilterChip(
            selected = isSearchGrounding,
            onClick = onToggleSearch,
            label = {
                Text(
                    text = "Search Grounding",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.TravelExplore,
                    contentDescription = "Search Grounding",
                    modifier = Modifier.size(14.dp)
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = BedrockBlue.copy(alpha = 0.2f),
                selectedLabelColor = BedrockBlue,
                selectedLeadingIconColor = BedrockBlue,
                containerColor = ObsidianCard,
                labelColor = TextSecondary
            ),
            border = BorderStroke(1.dp, if (isSearchGrounding) BedrockBlue else ObsidianBorder),
            modifier = Modifier.testTag("search_grounding_chip")
        )

        // Video Understanding Action Chip
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ObsidianCard,
            border = BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable { onOpenVideoDialog() }
                .testTag("analyze_video_button")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VideoLibrary,
                    contentDescription = "Video Analysis",
                    tint = DiamondCyan,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Analyze Video",
                    fontSize = 11.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    onOpenEditor: (BedrockMod) -> Unit,
    onTestInMinecraft: (BedrockMod) -> Unit,
    onExportToFolder: (BedrockMod) -> Unit,
    onShareMod: (BedrockMod) -> Unit
) {
    var isThinkingExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.role == "user") Alignment.End else Alignment.Start
    ) {
        // Message Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp, end = 4.dp)
        ) {
            if (message.role == "user") {
                Text("You", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(BedrockBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = ObsidianDark,
                        modifier = Modifier.size(12.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(GeminiSparkleStart, GeminiSparkleEnd))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
                Text("BedrockCraft AI", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DiamondCyan)
                Text(
                    text = "• ${message.modelUsed.replace("-preview", "")}",
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }
        }

        // Thinking Mode Expandable Box
        if (!message.thinkingProcess.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ObsidianSurface,
                border = BorderStroke(1.dp, AmethystPurple.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .padding(bottom = 8.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isThinkingExpanded = !isThinkingExpanded },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = AmethystPurple,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Thinking Process (High Reasoning)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AmethystPurple
                            )
                        }
                        Icon(
                            imageVector = if (isThinkingExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (isThinkingExpanded) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = message.thinkingProcess,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // Message Bubble
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.role == "user") 16.dp else 4.dp,
                bottomEnd = if (message.role == "user") 4.dp else 16.dp
            ),
            color = if (message.role == "user") Color(0xFF1E293B) else ObsidianCard,
            border = BorderStroke(
                1.dp,
                if (message.role == "user") BedrockBlue.copy(alpha = 0.3f) else ObsidianBorder
            ),
            modifier = Modifier.widthIn(max = 340.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Main Text
                Text(
                    text = message.content,
                    fontSize = 14.sp,
                    color = TextPrimary,
                    lineHeight = 20.sp
                )

                // Search Grounding Sources (if search grounded)
                if (message.groundingSources.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "🔍 Verified Bedrock Sources:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BedrockBlue
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        message.groundingSources.take(3).forEach { source ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = ObsidianSurface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Link,
                                        contentDescription = null,
                                        tint = BedrockBlue,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = source.title,
                                        fontSize = 10.sp,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // If this message generated an active Bedrock Mod, show the 1-Tap Action Card!
        if (message.generatedMod != null) {
            Spacer(modifier = Modifier.height(8.dp))
            GeneratedModActionCard(
                mod = message.generatedMod,
                onOpenEditor = { onOpenEditor(message.generatedMod) },
                onTestInMinecraft = { onTestInMinecraft(message.generatedMod) },
                onExportToFolder = { onExportToFolder(message.generatedMod) },
                onShareMod = { onShareMod(message.generatedMod) }
            )
        }
    }
}

@Composable
fun GeneratedModActionCard(
    mod: BedrockMod,
    onOpenEditor: () -> Unit,
    onTestInMinecraft: () -> Unit,
    onExportToFolder: () -> Unit,
    onShareMod: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(1.5.dp, DiamondCyan.copy(alpha = 0.7f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("generated_mod_card_${mod.name}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Mod Title & Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(mod.iconEmoji, fontSize = 24.sp)
                    Column {
                        Text(
                            text = mod.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${mod.files.size} Bedrock files generated • BP & RP",
                            fontSize = 11.sp,
                            color = DiamondCyan
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DiamondCyan.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "READY TO PLAY",
                        color = DiamondCyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // File items summary pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                mod.files.keys.forEach { path ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = ObsidianCard,
                        border = BorderStroke(1.dp, ObsidianBorder)
                    ) {
                        Text(
                            text = path,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Real-Time 3D Graphical Preview of Generated Block / Item
            BedrockVisualPreview3D(mod = mod)

            Spacer(modifier = Modifier.height(12.dp))

            // Primary 1-Tap Test in Minecraft Button
            Button(
                onClick = onTestInMinecraft,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DiamondCyan,
                    contentColor = ObsidianDark
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("test_in_minecraft_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "1-Tap Test in Minecraft Bedrock",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Secondary Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Direct Folder Export Button
                OutlinedButton(
                    onClick = onExportToFolder,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BedrockBlue),
                    border = BorderStroke(1.dp, BedrockBlue.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("export_to_folder_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Game Folder", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                // Inspect & Edit Code Button
                OutlinedButton(
                    onClick = onOpenEditor,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("inspect_code_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Inspect Files", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                // Share .mcaddon
                IconButton(
                    onClick = onShareMod,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ObsidianCard)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun QuickPromptChips(onSelectPrompt: (String) -> Unit) {
    val prompts = listOf(
        "🗡️ Frost Katana with Script API",
        "👹 Nether Titan Boss with boss bar",
        "💎 Ruby Ore & Ruby Armor set",
        "⚡ Magic Lightning Wand",
        "📦 Cosmic Lucky Block addon",
        "🔍 Bedrock 1.21 item components"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        prompts.forEach { text ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ObsidianSurface,
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSelectPrompt(text.substringAfter(" ")) }
            ) {
                Text(
                    text = text,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun GeneratingIndicator(
    modelName: String,
    isHighThinking: Boolean,
    isSearchGrounding: Boolean
) {
    Row(
        modifier = Modifier
            .padding(8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianCard)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CircularProgressIndicator(
            color = if (isHighThinking) AmethystPurple else DiamondCyan,
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp
        )
        Text(
            text = when {
                isHighThinking -> "Gemini Pro is deeply thinking & architecting Bedrock mod..."
                isSearchGrounding -> "Searching Bedrock documentation & synthesizing code..."
                else -> "Gemini is building your Bedrock addon files..."
            },
            fontSize = 12.sp,
            color = TextSecondary
        )
    }
}

@Composable
fun ChatInputBar(
    inputText: String,
    isGenerating: Boolean,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    onAttachVideo: () -> Unit
) {
    Surface(
        color = ObsidianSurface,
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Video concept attachment button
            IconButton(
                onClick = onAttachVideo,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(ObsidianCard)
                    .testTag("attach_video_button")
            ) {
                Icon(
                    imageVector = Icons.Default.VideoLibrary,
                    contentDescription = "Analyze Video",
                    tint = DiamondCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Input field
            OutlinedTextField(
                value = inputText,
                onValueChange = onTextChanged,
                placeholder = {
                    Text(
                        "Describe your mod or ask Bedrock docs...",
                        fontSize = 13.sp,
                        color = TextMuted
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp, max = 120.dp)
                    .testTag("chat_input_field"),
                shape = RoundedCornerShape(22.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BedrockBlue,
                    unfocusedBorderColor = ObsidianBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = ObsidianDark,
                    unfocusedContainerColor = ObsidianDark
                ),
                maxLines = 4
            )

            // Send Button
            IconButton(
                onClick = onSend,
                enabled = inputText.isNotBlank() && !isGenerating,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (inputText.isNotBlank() && !isGenerating) DiamondCyan else ObsidianCard
                    )
                    .testTag("send_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (inputText.isNotBlank() && !isGenerating) ObsidianDark else TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
