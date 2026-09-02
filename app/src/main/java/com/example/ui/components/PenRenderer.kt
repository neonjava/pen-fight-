package com.example.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import com.example.model.PenInstance
import com.example.model.PenStyle
import kotlin.math.cos
import kotlin.math.sin

object PenRenderer {

    fun drawPen(
        scope: DrawScope,
        pen: PenInstance,
        isCurrentTurn: Boolean
    ) {
        val alpha = pen.fallAlpha
        if (alpha <= 0.01f) return

        val cx = pen.x
        val cy = pen.y
        val angleDeg = Math.toDegrees(pen.angle.toDouble()).toFloat() + pen.fallRotation
        val length = pen.length
        val width = pen.width

        scope.scale(pen.fallScale, pen.fallScale, Offset(cx, cy)) {
            // 1. Draw Drop Shadow on table (offset by light angle)
            if (!pen.isOffTable) {
                scope.rotate(angleDeg, Offset(cx + 8f, cy + 12f)) {
                    drawPenBody(
                        scope = this,
                        cx = cx + 8f,
                        cy = cy + 12f,
                        length = length,
                        width = width,
                        style = pen.config.style,
                        bodyColor = Color.Black.copy(alpha = 0.22f),
                        capColor = Color.Black.copy(alpha = 0.22f),
                        gripColor = Color.Black.copy(alpha = 0.22f),
                        nibColor = Color.Black.copy(alpha = 0.22f),
                        isShadow = true
                    )
                }
            }

            // 2. Draw Glow / Active Indicator for current turn
            if (isCurrentTurn && !pen.isOffTable) {
                val auraColor = if (pen.playerId == 1) Color(0xFF38BDF8).copy(alpha = 0.35f) else Color(0xFFF43F5E).copy(alpha = 0.35f)
                scope.drawCircle(
                    color = auraColor,
                    radius = (length / 2f) + 18f,
                    center = Offset(cx, cy)
                )
            }

            // 3. Draw Actual Pen with Rotated Perspective
            scope.rotate(angleDeg, Offset(cx, cy)) {
                val bodyCol = Color(pen.config.bodyColor).copy(alpha = alpha)
                val capCol = Color(pen.config.capColor).copy(alpha = alpha)
                val gripCol = Color(pen.config.gripColor).copy(alpha = alpha)
                val nibCol = Color(pen.config.nibColor).copy(alpha = alpha)

                drawPenBody(
                    scope = this,
                    cx = cx,
                    cy = cy,
                    length = length,
                    width = width,
                    style = pen.config.style,
                    bodyColor = bodyCol,
                    capColor = capCol,
                    gripColor = gripCol,
                    nibColor = nibCol,
                    isShadow = false
                )
            }
        }
    }

    private fun drawPenBody(
        scope: DrawScope,
        cx: Float,
        cy: Float,
        length: Float,
        width: Float,
        style: PenStyle,
        bodyColor: Color,
        capColor: Color,
        gripColor: Color,
        nibColor: Color,
        isShadow: Boolean
    ) {
        val halfL = length / 2f
        val halfW = width / 2f

        // Pen orientation: Tip is at (+halfL, 0), Cap is at (-halfL, 0) in local rotated space
        val capX = cx - halfL
        val tipX = cx + halfL

        when (style) {
            PenStyle.BALLPOINT -> {
                // Main barrel (Hexagonal/Round translucent body)
                val barrelL = length * 0.65f
                val barrelX = capX + length * 0.15f

                scope.drawRoundRect(
                    color = bodyColor,
                    topLeft = Offset(barrelX, cy - halfW),
                    size = Size(barrelL, width),
                    cornerRadius = CornerRadius(width * 0.2f, width * 0.2f)
                )

                if (!isShadow) {
                    // Highlight reflection stripe
                    scope.drawLine(
                        color = Color.White.copy(alpha = 0.35f),
                        start = Offset(barrelX + 10f, cy - halfW * 0.45f),
                        end = Offset(barrelX + barrelL - 10f, cy - halfW * 0.45f),
                        strokeWidth = width * 0.22f,
                        cap = StrokeCap.Round
                    )
                }

                // Cap at back end
                val backCapL = length * 0.15f
                scope.drawRoundRect(
                    color = capColor,
                    topLeft = Offset(capX, cy - halfW * 0.85f),
                    size = Size(backCapL, width * 0.85f * 2f),
                    cornerRadius = CornerRadius(4f, 4f)
                )

                // Front Grip + Cone + Tip
                val gripL = length * 0.14f
                val gripX = barrelX + barrelL
                scope.drawRoundRect(
                    color = gripColor,
                    topLeft = Offset(gripX, cy - halfW * 0.9f),
                    size = Size(gripL, width * 1.8f),
                    cornerRadius = CornerRadius(2f, 2f)
                )

                // Metallic Cone & Ball Nib
                val conePath = Path().apply {
                    moveTo(gripX + gripL, cy - halfW * 0.75f)
                    lineTo(tipX - 6f, cy - 2.5f)
                    lineTo(tipX, cy)
                    lineTo(tipX - 6f, cy + 2.5f)
                    lineTo(gripX + gripL, cy + halfW * 0.75f)
                    close()
                }
                scope.drawPath(conePath, color = nibColor)
            }

            PenStyle.FOUNTAIN_PEN -> {
                // Luxurious Metallic Fountain Pen
                val barrelL = length * 0.58f
                val barrelX = capX + length * 0.18f

                scope.drawRoundRect(
                    color = bodyColor,
                    topLeft = Offset(barrelX, cy - halfW),
                    size = Size(barrelL, width),
                    cornerRadius = CornerRadius(width * 0.35f, width * 0.35f)
                )

                // Gold Cap at back
                val capL = length * 0.18f
                scope.drawRoundRect(
                    color = capColor,
                    topLeft = Offset(capX, cy - halfW * 1.05f),
                    size = Size(capL, width * 1.05f * 2f),
                    cornerRadius = CornerRadius(6f, 6f)
                )

                // Golden Clip
                if (!isShadow) {
                    scope.drawRoundRect(
                        color = nibColor,
                        topLeft = Offset(capX + 4f, cy - halfW * 1.25f),
                        size = Size(capL * 0.85f, width * 0.25f),
                        cornerRadius = CornerRadius(2f, 2f)
                    )
                }

                // Grip Section
                val gripL = length * 0.12f
                val gripX = barrelX + barrelL
                scope.drawRect(
                    color = gripColor,
                    topLeft = Offset(gripX, cy - halfW * 0.8f),
                    size = Size(gripL, width * 1.6f)
                )

                // Royal Gold Nib
                val nibPath = Path().apply {
                    moveTo(gripX + gripL, cy - halfW * 0.75f)
                    cubicTo(
                        gripX + gripL + length * 0.06f, cy - halfW * 0.85f,
                        tipX - 4f, cy - 1.5f,
                        tipX, cy
                    )
                    cubicTo(
                        tipX - 4f, cy + 1.5f,
                        gripX + gripL + length * 0.06f, cy + halfW * 0.85f,
                        gripX + gripL, cy + halfW * 0.75f
                    )
                    close()
                }
                scope.drawPath(nibPath, color = nibColor)

                if (!isShadow) {
                    // Nib breather hole & slit
                    scope.drawCircle(
                        color = Color.Black.copy(alpha = 0.5f),
                        radius = 2.5f,
                        center = Offset(gripX + gripL + length * 0.05f, cy)
                    )
                    scope.drawLine(
                        color = Color.Black.copy(alpha = 0.5f),
                        start = Offset(gripX + gripL + length * 0.05f, cy),
                        end = Offset(tipX - 2f, cy),
                        strokeWidth = 1.5f
                    )
                }
            }

            PenStyle.GEL_CLICKER -> {
                // Clicker Button at back
                val clickerL = length * 0.08f
                scope.drawRoundRect(
                    color = capColor,
                    topLeft = Offset(capX, cy - halfW * 0.5f),
                    size = Size(clickerL, width),
                    cornerRadius = CornerRadius(2f, 2f)
                )

                // Main barrel
                val barrelL = length * 0.65f
                val barrelX = capX + clickerL
                scope.drawRoundRect(
                    color = bodyColor,
                    topLeft = Offset(barrelX, cy - halfW),
                    size = Size(barrelL, width),
                    cornerRadius = CornerRadius(4f, 4f)
                )

                // Rubberized textured grip
                val gripL = length * 0.18f
                val gripX = barrelX + barrelL
                scope.drawRoundRect(
                    color = gripColor,
                    topLeft = Offset(gripX, cy - halfW * 0.95f),
                    size = Size(gripL, width * 1.9f),
                    cornerRadius = CornerRadius(3f, 3f)
                )

                if (!isShadow) {
                    // Grip ridges
                    for (i in 1..4) {
                        val gx = gripX + (gripL * i / 5f)
                        scope.drawLine(
                            color = Color.Black.copy(alpha = 0.25f),
                            start = Offset(gx, cy - halfW * 0.85f),
                            end = Offset(gx, cy + halfW * 0.85f),
                            strokeWidth = 1.5f
                        )
                    }
                }

                // Retractable tip cone
                val tipConePath = Path().apply {
                    moveTo(gripX + gripL, cy - halfW * 0.7f)
                    lineTo(tipX - 4f, cy - 2f)
                    lineTo(tipX, cy)
                    lineTo(tipX - 4f, cy + 2f)
                    lineTo(gripX + gripL, cy + halfW * 0.7f)
                    close()
                }
                scope.drawPath(tipConePath, color = nibColor)
            }

            PenStyle.HIGHLIGHTER -> {
                // Chubby flat body
                val bodyL = length * 0.72f
                val bodyX = capX + length * 0.08f
                scope.drawRoundRect(
                    color = bodyColor,
                    topLeft = Offset(bodyX, cy - halfW),
                    size = Size(bodyL, width),
                    cornerRadius = CornerRadius(width * 0.25f, width * 0.25f)
                )

                // Cap
                val capL = length * 0.2f
                val capStartX = bodyX + bodyL - capL * 0.3f
                scope.drawRoundRect(
                    color = capColor,
                    topLeft = Offset(capStartX, cy - halfW * 1.08f),
                    size = Size(capL, width * 1.08f * 2f),
                    cornerRadius = CornerRadius(width * 0.3f, width * 0.3f)
                )

                // Chisel Felt Tip
                val chiselPath = Path().apply {
                    val nx = capStartX + capL
                    moveTo(nx, cy - halfW * 0.6f)
                    lineTo(tipX, cy - halfW * 0.8f)
                    lineTo(tipX, cy + halfW * 0.4f)
                    lineTo(nx, cy + halfW * 0.6f)
                    close()
                }
                scope.drawPath(chiselPath, color = nibColor)
            }

            PenStyle.FINELINER -> {
                // Sleek, needle-point technical pen
                val bodyL = length * 0.78f
                val bodyX = capX + length * 0.06f
                scope.drawRoundRect(
                    color = bodyColor,
                    topLeft = Offset(bodyX, cy - halfW),
                    size = Size(bodyL, width),
                    cornerRadius = CornerRadius(2f, 2f)
                )

                // Metal needle sleeve & micro tip
                val sleeveL = length * 0.12f
                val sleeveX = bodyX + bodyL
                scope.drawRect(
                    color = nibColor,
                    topLeft = Offset(sleeveX, cy - 3.5f),
                    size = Size(sleeveL, 7f)
                )
                // Black tip
                scope.drawCircle(
                    color = Color.Black,
                    radius = 2.5f,
                    center = Offset(tipX, cy)
                )
            }

            PenStyle.CHUNKY_4COLOR -> {
                // Wide multicolor barrel with 4 slider buttons
                val bodyL = length * 0.68f
                val bodyX = capX + length * 0.16f
                scope.drawRoundRect(
                    color = bodyColor,
                    topLeft = Offset(bodyX, cy - halfW),
                    size = Size(bodyL, width),
                    cornerRadius = CornerRadius(width * 0.4f, width * 0.4f)
                )

                // 4 Color Clicker Sliders at top
                if (!isShadow) {
                    val colors = listOf(Color.Blue, Color.Red, Color.Black, Color(0xFF16A34A))
                    for ((idx, col) in colors.withIndex()) {
                        val sy = cy - halfW * 0.8f + (idx * halfW * 0.5f)
                        scope.drawRoundRect(
                            color = col,
                            topLeft = Offset(capX + 4f, sy),
                            size = Size(length * 0.12f, 5f),
                            cornerRadius = CornerRadius(2f, 2f)
                        )
                    }
                }

                // Front Cone & Tip
                val coneL = length * 0.14f
                val coneX = bodyX + bodyL
                val conePath = Path().apply {
                    moveTo(coneX, cy - halfW * 0.9f)
                    lineTo(tipX, cy)
                    lineTo(coneX, cy + halfW * 0.9f)
                    close()
                }
                scope.drawPath(conePath, color = nibColor)
            }
        }
    }
}
