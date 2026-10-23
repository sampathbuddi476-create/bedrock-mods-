package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.StudioViewModel

@Composable
fun VideoAnalysisDialog(viewModel: StudioViewModel) {
    var videoUrlInput by remember { mutableStateOf("") }
    var userPromptInput by remember { mutableStateOf("Analyze the weapons and entity attacks shown in this video and generate a complete Bedrock Addon.") }
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var selectedVideoName by remember { mutableStateOf<String?>(null) }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedVideoUri = uri
            selectedVideoName = uri.lastPathSegment ?: "video.mp4"
        }
    }

    AlertDialog(
        onDismissRequest = { viewModel.setShowVideoDialog(false) },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VideoLibrary,
                    contentDescription = null,
                    tint = DiamondCyan,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Video Understanding",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ObsidianSurface,
                    border = BorderStroke(1.dp, AmethystPurple.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "Powered by Gemini 3.1 Pro Preview. Analyzes video mechanics, mob behaviors, and weapon animations to generate a playable Minecraft Bedrock Addon.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                // Video URL Input
                OutlinedTextField(
                    value = videoUrlInput,
                    onValueChange = { videoUrlInput = it },
                    label = { Text("Video / Concept URL (optional)") },
                    placeholder = { Text("https://youtube.com/watch?v=... or clip URL") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("video_url_input"),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                // Pick Local Video Button
                OutlinedButton(
                    onClick = { videoPickerLauncher.launch("video/*") },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BedrockBlue),
                    border = BorderStroke(1.dp, BedrockBlue.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (selectedVideoUri != null) Icons.Default.PlayCircleOutline else Icons.Default.VideoFile,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = selectedVideoName ?: "Or select video from device",
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }

                // Prompt description
                OutlinedTextField(
                    value = userPromptInput,
                    onValueChange = { userPromptInput = it },
                    label = { Text("Extraction Instructions") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .testTag("video_prompt_input"),
                    shape = RoundedCornerShape(10.dp),
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.analyzeVideoConcept(
                        prompt = userPromptInput,
                        videoUri = selectedVideoUri,
                        videoUrl = videoUrlInput.ifBlank { null }
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = DiamondCyan,
                    contentColor = ObsidianDark
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("start_video_analysis_button")
            ) {
                Text("Analyze with Gemini Pro", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.setShowVideoDialog(false) }) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = ObsidianCard
    )
}
