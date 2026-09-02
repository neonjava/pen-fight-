package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import com.example.model.*
import kotlin.math.*

@Composable
fun DeskArenaView(
    arena: ArenaType,
    pens: List<PenInstance>,
    obstacles: List<DeskObstacle>,
    currentTurnPlayerId: Int,
    isInteractivityEnabled: Boolean,
    isAiTurn: Boolean,
    particles: List<SparkParticle>,
    isTabletopFlippedPlayer2: Boolean,
    onPlayerFlick: (flickVx: Float, flickVy: Float, hitPointX: Float, hitPointY: Float, powerRatio: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var dragStartPos by remember { mutableStateOf<Offset?>(null) }
    var dragCurrentPos by remember { mutableStateOf<Offset?>(null) }
    var activeTouchPenId by remember { mutableIntStateOf(-1) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val canvasWidth = constraints.maxWidth.toFloat()
        val canvasHeight = constraints.maxHeight.toFloat()

        val scaleX = canvasWidth / PenPhysics.TABLE_WIDTH
        val scaleY = canvasHeight / PenPhysics.TABLE_HEIGHT
        val uniformScale = min(scaleX, scaleY)

        val offsetX = (canvasWidth - PenPhysics.TABLE_WIDTH * uniformScale) / 2f
        val offsetY = (canvasHeight - PenPhysics.TABLE_HEIGHT * uniformScale) / 2f

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("game_desk_canvas")
                .pointerInput(isInteractivityEnabled, currentTurnPlayerId, isAiTurn) {
                    if (!isInteractivityEnabled || isAiTurn) return@pointerInput

                    detectDragGestures(
                        onDragStart = { touchOffset ->
                            // Convert touch screen coords to virtual table coords
                            val vx = (touchOffset.x - offsetX) / uniformScale
                            val vy = (touchOffset.y - offsetY) / uniformScale

                            // Find if touch is near active player's pen
                            val currentPen = pens.firstOrNull { it.playerId == currentTurnPlayerId }
                            if (currentPen != null && !currentPen.isOffTable) {
                                val dist = sqrt((vx - currentPen.x).pow(2) + (vy - currentPen.y).pow(2))
                                // Generous touch target around the pen (within 220 virtual units)
                                if (dist < 220f) {
                                    activeTouchPenId = currentPen.playerId
                                    dragStartPos = Offset(currentPen.x, currentPen.y)
                                    dragCurrentPos = Offset(vx, vy)
                                }
                            }
                        },
                        onDrag = { change, _ ->
                            if (activeTouchPenId != -1) {
                                change.consume()
                                val vx = (change.position.x - offsetX) / uniformScale
                                val vy = (change.position.y - offsetY) / uniformScale
                                dragCurrentPos = Offset(vx, vy)
                            }
                        },
                        onDragEnd = {
                            if (activeTouchPenId != -1 && dragStartPos != null && dragCurrentPos != null) {
                                val start = dragStartPos!!
                                val curr = dragCurrentPos!!

                                // Pull-back sling vector: pull opposite to flick direction
                                val pullVectorX = start.x - curr.x
                                val pullVectorY = start.y - curr.y
                                val pullDist = sqrt(pullVectorX * pullVectorX + pullVectorY * pullVectorY)

                                if (pullDist > 15f) {
                                    // Power is proportional to pull distance
                                    val powerRatio = (pullDist / 260f).coerceIn(0.12f, 1.0f)
                                    val flickPower = powerRatio * PenPhysics.MAX_FLICK_FORCE
                                    val flickAngle = atan2(pullVectorY, pullVectorX)

                                    val flickVx = cos(flickAngle) * flickPower
                                    val flickVy = sin(flickAngle) * flickPower

                                    val activePen = pens.firstOrNull { it.playerId == activeTouchPenId }
                                    val hitX = activePen?.x ?: start.x
                                    val hitY = activePen?.y ?: start.y

                                    onPlayerFlick(flickVx, flickVy, hitX, hitY, powerRatio)
                                }
                            }
                            activeTouchPenId = -1
                            dragStartPos = null
                            dragCurrentPos = null
                        },
                        onDragCancel = {
                            activeTouchPenId = -1
                            dragStartPos = null
                            dragCurrentPos = null
                        }
                    )
                }
        ) {
            // Draw into virtual table coordinate space
            drawContext.canvas.save()
            drawContext.canvas.translate(offsetX, offsetY)
            drawContext.canvas.scale(uniformScale, uniformScale)

            // 1. Draw Void / Outside Floor Drop
            drawRect(
                color = Color(0xFF0F172A),
                topLeft = Offset(0f, 0f),
                size = Size(PenPhysics.TABLE_WIDTH, PenPhysics.TABLE_HEIGHT)
            )

            // 2. Draw Table Base & Surface with 3D Bevel Shadow
            drawDeskSurface(arena)

            // 3. Draw Table Obstacles
            for (obs in obstacles) {
                drawObstacle(obs)
            }

            // 4. Draw Pens
            for (pen in pens) {
                PenRenderer.drawPen(
                    scope = this,
                    pen = pen,
                    isCurrentTurn = (pen.playerId == currentTurnPlayerId && isInteractivityEnabled)
                )
            }

            // 5. Draw Aiming Slingshot & Trajectory Arrow
            if (activeTouchPenId != -1 && dragStartPos != null && dragCurrentPos != null) {
                val start = dragStartPos!!
                val curr = dragCurrentPos!!
                drawAimTrajectory(start, curr, currentTurnPlayerId)
            }

            // 6. Draw Particle Sparks
            ParticleSystem.drawParticles(this, particles)

            drawContext.canvas.restore()
        }
    }
}

private fun DrawScope.drawDeskSurface(arena: ArenaType) {
    val tableLeft = PenPhysics.MARGIN_LEFT
    val tableTop = PenPhysics.MARGIN_TOP
    val tableW = PenPhysics.MARGIN_RIGHT - PenPhysics.MARGIN_LEFT
    val tableH = PenPhysics.MARGIN_BOTTOM - PenPhysics.MARGIN_TOP

    // Desk 3D Drop Edge Shadow (at bottom & right)
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.5f),
        topLeft = Offset(tableLeft + 12f, tableTop + 16f),
        size = Size(tableW, tableH),
        cornerRadius = CornerRadius(24f, 24f)
    )

    // Desk Wooden/Surface Border Bevel
    drawRoundRect(
        color = Color(arena.borderColor),
        topLeft = Offset(tableLeft - 10f, tableTop - 10f),
        size = Size(tableW + 20f, tableH + 20f),
        cornerRadius = CornerRadius(26f, 26f)
    )

    // Main Desk Surface Canvas
    drawRoundRect(
        color = Color(arena.surfaceThemeColor),
        topLeft = Offset(tableLeft, tableTop),
        size = Size(tableW, tableH),
        cornerRadius = CornerRadius(18f, 18f)
    )

    // Surface details based on arena
    when (arena.gridOrPattern) {
        "WOOD" -> {
            // Wood plank grain lines & etched classroom doodles
            for (i in 1..7) {
                val y = tableTop + (tableH * i / 8f)
                drawLine(
                    color = Color(arena.surfaceAccentColor).copy(alpha = 0.35f),
                    start = Offset(tableLeft + 15f, y),
                    end = Offset(tableLeft + tableW - 15f, y),
                    strokeWidth = 2.5f
                )
            }

            // Vintage etched doodles / pen scratch marks
            drawLine(
                color = Color(arena.surfaceAccentColor).copy(alpha = 0.4f),
                start = Offset(tableLeft + 60f, tableTop + 120f),
                end = Offset(tableLeft + 140f, tableTop + 150f),
                strokeWidth = 1.5f
            )
            drawLine(
                color = Color(arena.surfaceAccentColor).copy(alpha = 0.3f),
                start = Offset(tableLeft + tableW - 120f, tableTop + tableH - 180f),
                end = Offset(tableLeft + tableW - 40f, tableTop + tableH - 140f),
                strokeWidth = 1.5f
            )
        }
        "GRID" -> {
            // Math Graph Paper Grid
            val gridStep = 40f
            var x = tableLeft + gridStep
            while (x < tableLeft + tableW) {
                drawLine(
                    color = Color(arena.surfaceAccentColor).copy(alpha = 0.22f),
                    start = Offset(x, tableTop),
                    end = Offset(x, tableTop + tableH),
                    strokeWidth = 1f
                )
                x += gridStep
            }
            var y = tableTop + gridStep
            while (y < tableTop + tableH) {
                drawLine(
                    color = Color(arena.surfaceAccentColor).copy(alpha = 0.22f),
                    start = Offset(tableLeft, y),
                    end = Offset(tableLeft + tableW, y),
                    strokeWidth = 1f
                )
                y += gridStep
            }
            // Margin Red Line (School notebook margin)
            drawLine(
                color = Color(0xFFEF4444).copy(alpha = 0.6f),
                start = Offset(tableLeft + 90f, tableTop),
                end = Offset(tableLeft + 90f, tableTop + tableH),
                strokeWidth = 2.5f
            )
        }
        "LAB" -> {
            // Science Lab Glowing Measurement Reticle & Hex Grid
            drawCircle(
                color = Color(arena.surfaceAccentColor).copy(alpha = 0.15f),
                radius = 180f,
                center = Offset(tableLeft + tableW / 2f, tableTop + tableH / 2f),
                style = Stroke(width = 2f)
            )
            drawLine(
                color = Color(arena.surfaceAccentColor).copy(alpha = 0.25f),
                start = Offset(tableLeft + tableW / 2f - 220f, tableTop + tableH / 2f),
                end = Offset(tableLeft + tableW / 2f + 220f, tableTop + tableH / 2f),
                strokeWidth = 1.5f
            )
        }
        "CAFETERIA" -> {
            // Tray Rim Groove
            drawRoundRect(
                color = Color(arena.surfaceAccentColor).copy(alpha = 0.25f),
                topLeft = Offset(tableLeft + 25f, tableTop + 25f),
                size = Size(tableW - 50f, tableH - 50f),
                cornerRadius = CornerRadius(16f, 16f),
                style = Stroke(width = 3f)
            )
        }
    }

    // Outer desk Danger Warning Edge Outline (Flashing caution dashes)
    drawRoundRect(
        color = Color(0xFFEF4444).copy(alpha = 0.35f),
        topLeft = Offset(tableLeft + 6f, tableTop + 6f),
        size = Size(tableW - 12f, tableH - 12f),
        cornerRadius = CornerRadius(14f, 14f),
        style = Stroke(
            width = 3f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 15f), 0f)
        )
    )
}

private fun DrawScope.drawObstacle(obs: DeskObstacle) {
    val cx = obs.x
    val cy = obs.y
    val halfW = obs.width / 2f
    val halfH = obs.height / 2f

    rotate(obs.angle, Offset(cx, cy)) {
        // Shadow
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.25f),
            topLeft = Offset(cx - halfW + 4f, cy - halfH + 6f),
            size = Size(obs.width, obs.height),
            cornerRadius = CornerRadius(8f, 8f)
        )

        when (obs.kind) {
            ObstacleKind.ERASER -> {
                // White & Blue Vinyl Eraser
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(cx - halfW, cy - halfH),
                    size = Size(obs.width, obs.height),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                // Blue Cardboard Sleeve
                drawRoundRect(
                    color = Color(0xFF2563EB),
                    topLeft = Offset(cx - halfW * 0.4f, cy - halfH),
                    size = Size(obs.width * 0.7f, obs.height),
                    cornerRadius = CornerRadius(2f, 2f)
                )
            }
            ObstacleKind.RULER -> {
                // Wooden / Transparent Measuring Ruler
                drawRoundRect(
                    color = Color(0xFFFEF08A).copy(alpha = 0.9f),
                    topLeft = Offset(cx - halfW, cy - halfH),
                    size = Size(obs.width, obs.height),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                // Ruler tick marks
                val ticks = 16
                for (i in 0..ticks) {
                    val tx = cx - halfW + (obs.width * i / ticks.toFloat())
                    val tickLen = if (i % 4 == 0) halfH * 0.8f else halfH * 0.45f
                    drawLine(
                        color = Color(0xFF78350F),
                        start = Offset(tx, cy - halfH),
                        end = Offset(tx, cy - halfH + tickLen),
                        strokeWidth = 1.5f
                    )
                }
            }
            ObstacleKind.SHARPENER -> {
                // Metallic Wedge Sharpener
                drawRoundRect(
                    color = Color(0xFF94A3B8),
                    topLeft = Offset(cx - halfW, cy - halfH),
                    size = Size(obs.width, obs.height),
                    cornerRadius = CornerRadius(8f, 8f)
                )
                // Blade screw & opening
                drawCircle(
                    color = Color(0xFF334155),
                    radius = 6f,
                    center = Offset(cx, cy)
                )
            }
            ObstacleKind.PAPERCLIP -> {
                drawRoundRect(
                    color = Color(0xFFEC4899),
                    topLeft = Offset(cx - halfW, cy - halfH),
                    size = Size(obs.width, obs.height),
                    cornerRadius = CornerRadius(10f, 10f),
                    style = Stroke(width = 4f)
                )
            }
        }
    }
}

private fun DrawScope.drawAimTrajectory(
    start: Offset,
    curr: Offset,
    playerId: Int
) {
    val pullX = start.x - curr.x
    val pullY = start.y - curr.y
    val pullDist = sqrt(pullX * pullX + pullY * pullY)
    if (pullDist < 10f) return

    val powerRatio = (pullDist / 260f).coerceIn(0.1f, 1.0f)
    val angle = atan2(pullY, pullX)

    val teamColor = if (playerId == 1) Color(0xFF38BDF8) else Color(0xFFF43F5E)
    val powerColor = when {
        powerRatio > 0.75f -> Color(0xFFEF4444)
        powerRatio > 0.45f -> Color(0xFFF59E0B)
        else -> Color(0xFF10B981)
    }

    // 1. Draw Pull String (Elastic slingshot tension band)
    drawLine(
        color = teamColor.copy(alpha = 0.8f),
        start = start,
        end = curr,
        strokeWidth = 4f,
        cap = StrokeCap.Round
    )
    drawCircle(
        color = powerColor,
        radius = 12f + powerRatio * 8f,
        center = curr
    )

    // 2. Draw Forward Predictive Trajectory Arrow
    val arrowLen = (120f + powerRatio * 320f)
    val endX = start.x + cos(angle) * arrowLen
    val endY = start.y + sin(angle) * arrowLen

    // Trajectory Dotted Line
    drawLine(
        color = powerColor.copy(alpha = 0.9f),
        start = start,
        end = Offset(endX, endY),
        strokeWidth = 6f,
        cap = StrokeCap.Round,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f)
    )

    // Arrowhead Tip
    val arrowHeadSize = 24f + powerRatio * 10f
    val headAngle1 = angle + Math.PI.toFloat() * 0.85f
    val headAngle2 = angle - Math.PI.toFloat() * 0.85f

    val arrowPath = Path().apply {
        moveTo(endX, endY)
        lineTo(endX + cos(headAngle1) * arrowHeadSize, endY + sin(headAngle1) * arrowHeadSize)
        lineTo(endX + cos(headAngle2) * arrowHeadSize, endY + sin(headAngle2) * arrowHeadSize)
        close()
    }
    drawPath(arrowPath, color = powerColor)

    // 3. Power Meter Arc around anchor
    drawArc(
        color = powerColor,
        startAngle = -90f,
        sweepAngle = 360f * powerRatio,
        useCenter = false,
        topLeft = Offset(start.x - 45f, start.y - 45f),
        size = Size(90f, 90f),
        style = Stroke(width = 6f, cap = StrokeCap.Round)
    )
}
