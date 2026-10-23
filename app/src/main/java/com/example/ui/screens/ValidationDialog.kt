package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BedrockMod
import com.example.service.validator.IssueSeverity
import com.example.service.validator.ValidationResult
import com.example.ui.theme.*

@Composable
fun ValidationDialog(
    mod: BedrockMod,
    validationResult: ValidationResult,
    onAutoFixAndProceed: () -> Unit,
    onDismiss: () -> Unit,
    onOpenEditor: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (validationResult.isValid) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (validationResult.isValid) EmeraldGreen else RedstoneRed,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = if (validationResult.isValid) "Bedrock Validation Passed" else "Addon Validation Notice",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Standard Mojang Manifest & Pack Requirements",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (validationResult.errors.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = RedstoneRed.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, RedstoneRed.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Found ${validationResult.errors.size} error(s) that must be resolved for Minecraft Bedrock to load this pack properly.",
                            color = RedstoneRed,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Issues list
                    items(validationResult.issues) { issue ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ObsidianSurface,
                            border = BorderStroke(
                                1.dp,
                                when (issue.severity) {
                                    IssueSeverity.ERROR -> RedstoneRed.copy(alpha = 0.5f)
                                    IssueSeverity.WARNING -> GoldYellow.copy(alpha = 0.5f)
                                    IssueSeverity.INFO -> BedrockBlue.copy(alpha = 0.3f)
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = issue.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = issue.description,
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    lineHeight = 15.sp
                                )
                                Text(
                                    text = "Target: ${issue.fileTarget}",
                                    fontSize = 10.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }

                    // Passed checks list
                    if (validationResult.passedChecks.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Passed Checks:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                        }
                        items(validationResult.passedChecks) { check ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(12.dp))
                                Text(check, fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAutoFixAndProceed,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DiamondCyan,
                    contentColor = ObsidianDark
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("autofix_and_export_button")
            ) {
                Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (validationResult.errors.isNotEmpty()) "Auto-Fix & Export" else "Proceed with Export",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onOpenEditor) {
                    Text("Inspect in Editor", color = BedrockBlue, fontSize = 12.sp)
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = TextSecondary, fontSize = 12.sp)
                }
            }
        },
        containerColor = ObsidianCard
    )
}
