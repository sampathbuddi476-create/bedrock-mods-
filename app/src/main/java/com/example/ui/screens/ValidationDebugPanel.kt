package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BedrockMod
import com.example.service.validator.BedrockAddonValidator
import com.example.service.validator.IssueSeverity
import com.example.service.validator.ValidationResult
import com.example.ui.theme.*

@Composable
fun ValidationDebugPanel(
    mod: BedrockMod,
    onAutoFix: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Re-evaluates in real-time whenever mod content changes!
    val validationResult = remember(mod) {
        BedrockAddonValidator.validate(mod)
    }

    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf("All") }

    val filteredIssues = when (selectedFilter) {
        "Errors" -> validationResult.issues.filter { it.severity == IssueSeverity.ERROR }
        "Warnings" -> validationResult.issues.filter { it.severity == IssueSeverity.WARNING }
        else -> validationResult.issues
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        border = BorderStroke(
            1.dp,
            if (validationResult.isValid) EmeraldGreen.copy(alpha = 0.5f) else RedstoneRed.copy(alpha = 0.5f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("validation_debug_panel")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Health Score & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (validationResult.isValid) EmeraldGreen.copy(alpha = 0.2f) else RedstoneRed.copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (validationResult.isValid) Icons.Default.CheckCircle else Icons.Default.BugReport,
                            contentDescription = "Debugger",
                            tint = if (validationResult.isValid) EmeraldGreen else RedstoneRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Real-Time Manifest Debugger",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (validationResult.isValid) EmeraldGreen.copy(alpha = 0.2f) else RedstoneRed.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = if (validationResult.isValid) "PASS" else "${validationResult.errors.size} ISSUES",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (validationResult.isValid) EmeraldGreen else RedstoneRed,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = if (validationResult.isValid)
                                "100% Mojang Bedrock Specification Compliant"
                            else
                                "Schema discrepancies detected in manifest or components",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle Panel",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    // Quick Filter Pills & Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = selectedFilter == "All",
                                onClick = { selectedFilter = "All" },
                                label = { Text("All (${validationResult.issues.size})", fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BedrockBlue.copy(alpha = 0.2f),
                                    selectedLabelColor = BedrockBlue
                                )
                            )
                            if (validationResult.errors.isNotEmpty()) {
                                FilterChip(
                                    selected = selectedFilter == "Errors",
                                    onClick = { selectedFilter = "Errors" },
                                    label = { Text("Errors (${validationResult.errors.size})", fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = RedstoneRed.copy(alpha = 0.2f),
                                        selectedLabelColor = RedstoneRed
                                    )
                                )
                            }
                            if (validationResult.warnings.isNotEmpty()) {
                                FilterChip(
                                    selected = selectedFilter == "Warnings",
                                    onClick = { selectedFilter = "Warnings" },
                                    label = { Text("Warnings (${validationResult.warnings.size})", fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GoldYellow.copy(alpha = 0.2f),
                                        selectedLabelColor = GoldYellow
                                    )
                                )
                            }
                        }

                        // Copy Diagnostic Report
                        IconButton(
                            onClick = {
                                val report = buildString {
                                    appendLine("=== Bedrock Studio Manifest Diagnostic Report ===")
                                    appendLine("Mod: ${mod.name}")
                                    appendLine("Status: ${if (validationResult.isValid) "VALID" else "INVALID"}")
                                    appendLine("UUID BP: ${mod.bpUuidHeader}")
                                    appendLine("UUID RP: ${mod.rpUuidHeader}")
                                    appendLine("Min Engine: ${mod.minEngineVersion}")
                                    appendLine("\nIssues (${validationResult.issues.size}):")
                                    validationResult.issues.forEach { issue ->
                                        appendLine("[${issue.severity}] ${issue.title} - ${issue.fileTarget}: ${issue.description}")
                                    }
                                }
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Bedrock Diagnostic", report))
                                Toast.makeText(context, "Diagnostic report copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Report", tint = TextMuted, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Issues and passed checks list
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        filteredIssues.forEach { issue ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ObsidianSurface,
                                border = BorderStroke(
                                    1.dp,
                                    when (issue.severity) {
                                        IssueSeverity.ERROR -> RedstoneRed.copy(alpha = 0.4f)
                                        IssueSeverity.WARNING -> GoldYellow.copy(alpha = 0.4f)
                                        IssueSeverity.INFO -> BedrockBlue.copy(alpha = 0.4f)
                                    }
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = when (issue.severity) {
                                            IssueSeverity.ERROR -> Icons.Default.Cancel
                                            IssueSeverity.WARNING -> Icons.Default.WarningAmber
                                            IssueSeverity.INFO -> Icons.Default.Info
                                        },
                                        contentDescription = null,
                                        tint = when (issue.severity) {
                                            IssueSeverity.ERROR -> RedstoneRed
                                            IssueSeverity.WARNING -> GoldYellow
                                            IssueSeverity.INFO -> BedrockBlue
                                        },
                                        modifier = Modifier
                                            .size(16.dp)
                                            .padding(top = 2.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = issue.title,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = issue.description,
                                            fontSize = 10.sp,
                                            color = TextSecondary,
                                            lineHeight = 14.sp
                                        )
                                        Text(
                                            text = "File: ${issue.fileTarget}",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = TextMuted
                                        )
                                    }
                                }
                            }
                        }

                        // Passed checks
                        if (validationResult.passedChecks.isNotEmpty()) {
                            validationResult.passedChecks.take(3).forEach { check ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(12.dp))
                                    Text(check, fontSize = 10.sp, color = TextSecondary)
                                }
                            }
                        }
                    }

                    // Auto-fix button if issues are found
                    if (!validationResult.isValid) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onAutoFix,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DiamondCyan,
                                contentColor = ObsidianDark
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .testTag("autofix_manifest_button")
                        ) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Auto-Repair Manifest & UUIDs", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
