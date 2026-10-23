package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BedrockMod
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

enum class PreviewMode {
    ISOMETRIC_3D,
    PIXEL_TEXTURE,
    HANDHELD_IN_GAME
}

/**
 * High-performance 3D visual preview component that renders an isometric 3D block or weapon/item
 * with realistic directional lighting, embedded gem veins, interactive drag-to-rotate,
 * and particle shine.
 */
@Composable
fun BedrockVisualPreview3D(
    mod: BedrockMod,
    modifier: Modifier = Modifier
) {
    var previewMode by remember { mutableStateOf(PreviewMode.ISOMETRIC_3D) }
    var rotationY by remember { mutableFloatStateOf(0f) }
    var rotationX by remember { mutableFloatStateOf(0f) }
    var isAutoSpin by remember { mutableStateOf(true) }

    // Auto-spin animation
    val infiniteTransition = rememberInfiniteTransition(label = "spin_transition")
    val animatedAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin_angle"
    )

    val currentRotationY = if (isAutoSpin) animatedAngle + rotationY else rotationY

    // Determine type from files
    val isBlock = mod.files.keys.any { it.contains("blocks/") || it.contains("ore") } ||
            mod.name.contains("block", ignoreCase = true) || mod.name.contains("ore", ignoreCase = true)
    val isSword = mod.files.keys.any { it.contains("items/") } || !isBlock

    // Primary item/block accent color
    val accentColor = remember(mod.primaryColorHex) {
        try {
            Color(android.graphics.Color.parseColor(mod.primaryColorHex))
        } catch (_: Exception) {
            DiamondCyan
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("visual_preview_3d_${mod.name}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Bar
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
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isBlock) Icons.Default.ViewInAr else Icons.Default.PrecisionManufacturing,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text(
                            text = if (isBlock) "3D Isometric Block Visualizer" else "3D Item Model Preview",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${if (isBlock) "Custom Block Geometry" else "Handheld Item"} • Drag to Rotate",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Auto-spin toggle & View selector
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { isAutoSpin = !isAutoSpin },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isAutoSpin) Icons.Default.PauseCircleOutline else Icons.Default.PlayCircleOutline,
                            contentDescription = "Toggle Spin",
                            tint = if (isAutoSpin) DiamondCyan else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ObsidianSurface,
                        border = BorderStroke(1.dp, ObsidianBorder)
                    ) {
                        Row(modifier = Modifier.padding(2.dp)) {
                            IconButton(
                                onClick = { previewMode = PreviewMode.ISOMETRIC_3D },
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (previewMode == PreviewMode.ISOMETRIC_3D) ObsidianCard else Color.Transparent)
                            ) {
                                Icon(
                                    Icons.Default.ViewInAr,
                                    contentDescription = "3D",
                                    tint = if (previewMode == PreviewMode.ISOMETRIC_3D) DiamondCyan else TextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            IconButton(
                                onClick = { previewMode = PreviewMode.PIXEL_TEXTURE },
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (previewMode == PreviewMode.PIXEL_TEXTURE) ObsidianCard else Color.Transparent)
                            ) {
                                Icon(
                                    Icons.Default.GridOn,
                                    contentDescription = "Pixel Texture",
                                    tint = if (previewMode == PreviewMode.PIXEL_TEXTURE) DiamondCyan else TextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main 3D Canvas Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.radialGradient(
                            listOf(
                                accentColor.copy(alpha = 0.15f),
                                ObsidianDark
                            )
                        )
                    )
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
                    .pointerInput(Unit) {
                        detectDragGestures { _, dragAmount ->
                            isAutoSpin = false
                            rotationY += dragAmount.x * 0.5f
                            rotationX += dragAmount.y * 0.5f
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                when (previewMode) {
                    PreviewMode.ISOMETRIC_3D -> {
                        if (isBlock) {
                            RenderIsometricBlock3D(
                                accentColor = accentColor,
                                blockName = mod.name,
                                angleY = currentRotationY,
                                angleX = rotationX
                            )
                        } else {
                            RenderIsometricWeapon3D(
                                accentColor = accentColor,
                                weaponName = mod.name,
                                angleY = currentRotationY,
                                angleX = rotationX
                            )
                        }
                    }
                    PreviewMode.PIXEL_TEXTURE -> {
                        RenderPixelTextureGrid(
                            accentColor = accentColor,
                            itemName = mod.name,
                            isBlock = isBlock
                        )
                    }
                    PreviewMode.HANDHELD_IN_GAME -> {
                        RenderIsometricWeapon3D(
                            accentColor = accentColor,
                            weaponName = mod.name,
                            angleY = 45f,
                            angleX = 15f
                        )
                    }
                }

                // Live status badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = ObsidianDark.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = if (isBlock) "16x16 Cube • Isometric Mesh" else "Handheld 3D Extrusion",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                // Stats badge (Damage / Light)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = ObsidianDark.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = if (isBlock) "Emit Light: 8 • Resistance: 3.0" else "Attack: +18 • Durability: 2500",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

/**
 * Draws an isometric 3D block with top face, left face, right face,
 * accurate lighting shading, and embedded ore gems.
 */
@Composable
fun RenderIsometricBlock3D(
    accentColor: Color,
    blockName: String,
    angleY: Float,
    angleX: Float
) {
    Canvas(modifier = Modifier.size(160.dp)) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        val rad = Math.toRadians(angleY.toDouble()).toFloat()
        val tilt = Math.toRadians((angleX * 0.3f).toDouble()).toFloat()

        val scale = 52f

        // Isometric 3D Cube vertices projection
        val cosA = cos(rad)
        val sinA = sin(rad)

        // Base points for isometric top, left, right faces
        val topCenter = Offset(centerX, centerY - scale * 0.9f)
        val rightCorner = Offset(centerX + scale * 1.05f * (0.866f * cosA - 0.5f * sinA), centerY - scale * 0.35f + tilt * 10f)
        val leftCorner = Offset(centerX - scale * 1.05f * (0.866f * cosA + 0.5f * sinA), centerY - scale * 0.35f + tilt * 10f)
        val centerCorner = Offset(centerX, centerY + scale * 0.2f)

        val bottomCenter = Offset(centerX, centerY + scale * 1.15f)
        val bottomRight = Offset(rightCorner.x, rightCorner.y + scale * 0.95f)
        val bottomLeft = Offset(leftCorner.x, leftCorner.y + scale * 0.95f)

        // Shading colors
        val stoneBase = Color(0xFF334155)
        val topColor = Color(0xFF64748B)   // Brightest face (light from above)
        val rightColor = Color(0xFF475569) // Medium face
        val leftColor = Color(0xFF1E293B)  // Shadow face

        // 1. Draw Left Face
        val leftPath = Path().apply {
            moveTo(leftCorner.x, leftCorner.y)
            lineTo(centerCorner.x, centerCorner.y)
            lineTo(bottomCenter.x, bottomCenter.y)
            lineTo(bottomLeft.x, bottomLeft.y)
            close()
        }
        drawPath(leftPath, leftColor)
        drawPath(leftPath, Color.Black.copy(alpha = 0.5f), style = Stroke(width = 1.5f))

        // 2. Draw Right Face
        val rightPath = Path().apply {
            moveTo(centerCorner.x, centerCorner.y)
            lineTo(rightCorner.x, rightCorner.y)
            lineTo(bottomRight.x, bottomRight.y)
            lineTo(bottomCenter.x, bottomCenter.y)
            close()
        }
        drawPath(rightPath, rightColor)
        drawPath(rightPath, Color.Black.copy(alpha = 0.5f), style = Stroke(width = 1.5f))

        // 3. Draw Top Face
        val topPath = Path().apply {
            moveTo(topCenter.x, topCenter.y)
            lineTo(rightCorner.x, rightCorner.y)
            lineTo(centerCorner.x, centerCorner.y)
            lineTo(leftCorner.x, leftCorner.y)
            close()
        }
        drawPath(topPath, topColor)
        drawPath(topPath, Color.Black.copy(alpha = 0.5f), style = Stroke(width = 1.5f))

        // 4. Draw Embedded Gem Veins on Top Face
        drawCircle(
            color = accentColor,
            radius = 6f,
            center = Offset((topCenter.x + centerCorner.x) / 2f - 4f, (topCenter.y + centerCorner.y) / 2f + 2f)
        )
        drawCircle(
            color = Color.White,
            radius = 2.5f,
            center = Offset((topCenter.x + centerCorner.x) / 2f - 5f, (topCenter.y + centerCorner.y) / 2f + 1f)
        )

        // 5. Gem Veins on Right Face
        drawCircle(
            color = accentColor,
            radius = 7f,
            center = Offset((centerCorner.x + rightCorner.x) / 2f, (centerCorner.y + bottomRight.y) / 2f - 6f)
        )
        drawCircle(
            color = accentColor.copy(alpha = 0.6f),
            radius = 4f,
            center = Offset((centerCorner.x + rightCorner.x) / 2f + 8f, (centerCorner.y + bottomRight.y) / 2f + 4f)
        )

        // 6. Gem Veins on Left Face
        drawCircle(
            color = accentColor.copy(alpha = 0.8f),
            radius = 6f,
            center = Offset((leftCorner.x + centerCorner.x) / 2f, (leftCorner.y + bottomCenter.y) / 2f)
        )
    }
}

/**
 * Draws an isometric 3D weapon / item model with animated glow particles and rotation.
 */
@Composable
fun RenderIsometricWeapon3D(
    accentColor: Color,
    weaponName: String,
    angleY: Float,
    angleX: Float
) {
    Canvas(modifier = Modifier.size(160.dp)) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        val rad = Math.toRadians(angleY.toDouble()).toFloat()
        val cosA = cos(rad)
        val sinA = sin(rad)

        // Blade diagonal vector
        val bladeLength = 65f
        val bladeWidth = 14f * cosA

        val handleStart = Offset(centerX - 35f, centerY + 35f)
        val guardCenter = Offset(centerX - 15f, centerY + 15f)
        val tip = Offset(centerX + bladeLength * 0.7f, centerY - bladeLength * 0.7f)

        // Glowing outer aura
        drawCircle(
            brush = Brush.radialGradient(
                listOf(accentColor.copy(alpha = 0.35f), Color.Transparent),
                center = Offset(centerX + 15f, centerY - 15f),
                radius = 65f
            ),
            radius = 65f,
            center = Offset(centerX + 15f, centerY - 15f)
        )

        // Handle (Wood & Gold)
        drawLine(
            color = Color(0xFF78350F),
            start = handleStart,
            end = guardCenter,
            strokeWidth = 7f
        )
        drawCircle(
            color = Color(0xFFF59E0B),
            radius = 5f,
            center = handleStart
        )

        // Crossguard
        val guardPerpX = 14f
        val guardPerpY = 14f
        drawLine(
            color = Color(0xFF475569),
            start = Offset(guardCenter.x - guardPerpX, guardCenter.y - guardPerpY),
            end = Offset(guardCenter.x + guardPerpX, guardCenter.y + guardPerpY),
            strokeWidth = 6f
        )

        // 3D Extruded Blade (Polygon Path)
        val bladeLeft = Path().apply {
            moveTo(guardCenter.x, guardCenter.y)
            lineTo(guardCenter.x - bladeWidth * 0.5f, guardCenter.y - bladeWidth * 0.5f)
            lineTo(tip.x - 3f, tip.y - 3f)
            lineTo(tip.x, tip.y)
            close()
        }
        val bladeRight = Path().apply {
            moveTo(guardCenter.x, guardCenter.y)
            lineTo(guardCenter.x + bladeWidth * 0.5f, guardCenter.y + bladeWidth * 0.5f)
            lineTo(tip.x + 3f, tip.y + 3f)
            lineTo(tip.x, tip.y)
            close()
        }

        drawPath(bladeLeft, accentColor.copy(alpha = 0.85f))
        drawPath(bladeRight, accentColor)
        drawPath(bladeLeft, Color.White.copy(alpha = 0.5f), style = Stroke(width = 1f))

        // Center spine shine
        drawLine(
            color = Color.White,
            start = guardCenter,
            end = tip,
            strokeWidth = 2.5f
        )

        // Energy sparkles
        val particleOffsets = listOf(
            Offset(tip.x - 8f, tip.y - 12f),
            Offset(tip.x + 10f, tip.y + 4f),
            Offset(guardCenter.x + 20f, guardCenter.y - 20f),
            Offset(guardCenter.x - 10f, guardCenter.y - 25f)
        )
        particleOffsets.forEach { pos ->
            drawCircle(color = accentColor, radius = 2.5f, center = pos)
            drawCircle(color = Color.White, radius = 1.2f, center = pos)
        }
    }
}

/**
 * 16x16 Pixel Art Texture Grid Inspection View.
 */
@Composable
fun RenderPixelTextureGrid(
    accentColor: Color,
    itemName: String,
    isBlock: Boolean
) {
    Canvas(modifier = Modifier.size(150.dp)) {
        val cellSize = size.width / 16f

        // Draw 16x16 pixel grid
        for (x in 0 until 16) {
            for (y in 0 until 16) {
                val rectOffset = Offset(x * cellSize, y * cellSize)
                val isBorder = x == 0 || y == 0 || x == 15 || y == 15

                val pixelColor = if (isBlock) {
                    val isGem = (x in 4..6 && y in 5..7) || (x in 10..12 && y in 9..11) || (x in 7..9 && y in 2..4)
                    when {
                        isGem -> if ((x + y) % 2 == 0) accentColor else Color.White
                        isBorder -> Color(0xFF1E293B)
                        (x + y) % 3 == 0 -> Color(0xFF475569)
                        else -> Color(0xFF334155)
                    }
                } else {
                    // Sword diagonal
                    val isDiagonal = (x == y || x == y - 1 || x == y + 1) && x in 3..14
                    val isHandle = (x in 1..3 && y in 1..3)
                    val isGuard = (x == 4 && y in 2..6) || (y == 4 && x in 2..6)
                    when {
                        isDiagonal -> if (x == y) Color.White else accentColor
                        isGuard -> Color(0xFF64748B)
                        isHandle -> Color(0xFF78350F)
                        else -> Color(0xFF0F172A).copy(alpha = 0.3f)
                    }
                }

                drawRect(
                    color = pixelColor,
                    topLeft = rectOffset,
                    size = androidx.compose.ui.geometry.Size(cellSize - 0.5f, cellSize - 0.5f),
                    style = Fill
                )
            }
        }
    }
}
