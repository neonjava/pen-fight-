package com.example.model

import kotlin.math.*
import kotlin.random.Random

class PenAiController {
    data class AiShotPlan(
        val flickVx: Float,
        val flickVy: Float,
        val hitPointX: Float,
        val hitPointY: Float,
        val powerRatio: Float
    )

    fun calculateBestShot(
        aiPen: PenInstance,
        targetPen: PenInstance,
        difficulty: AiDifficulty,
        arena: ArenaType
    ): AiShotPlan {
        // Target points on the opponent pen (tip, center, cap)
        val targets = listOf(
            Pair(targetPen.x, targetPen.y),
            Pair(targetPen.getTipX(), targetPen.getTipY()),
            Pair(targetPen.getCapX(), targetPen.getCapY())
        )

        // Choose strategic target: hitting the ends causes high spin torque!
        val chosenTarget = when (difficulty) {
            AiDifficulty.ROOKIE -> targets[0] // Simple center
            AiDifficulty.DESK_BATTLER -> targets.random()
            AiDifficulty.CLASSROOM_PRO, AiDifficulty.PEN_NINJA -> {
                // Find which target point pushes opponent closest to table edge
                targets.minByOrNull { (tx, ty) ->
                    val distToLeft = tx - PenPhysics.MARGIN_LEFT
                    val distToRight = PenPhysics.MARGIN_RIGHT - tx
                    val distToTop = ty - PenPhysics.MARGIN_TOP
                    val distToBottom = PenPhysics.MARGIN_BOTTOM - ty
                    minOf(distToLeft, distToRight, distToTop, distToBottom)
                } ?: targets[0]
            }
        }

        val dx = chosenTarget.first - aiPen.x
        val dy = chosenTarget.second - aiPen.y
        val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(10f)

        // Angle towards target with inaccuracy
        var aimAngle = atan2(dy, dx)
        val angleJitter = (1f - difficulty.accuracy) * 0.45f * (Random.nextFloat() * 2f - 1f)
        aimAngle += angleJitter

        // Distance-based power calculation
        val baseSpeed = sqrt(dist) * 38f * (1f + (arena.friction - 1f) * 0.25f)
        val powerJitter = 1f + (1f - difficulty.accuracy) * difficulty.powerError * (Random.nextFloat() * 2f - 1f)
        val calculatedPower = (baseSpeed * powerJitter).coerceIn(PenPhysics.MIN_FLICK_FORCE * 1.5f, PenPhysics.MAX_FLICK_FORCE * 0.85f)

        val flickVx = cos(aimAngle) * calculatedPower
        val flickVy = sin(aimAngle) * calculatedPower

        // Struck point on AI pen (center or cap for extra spin)
        val hitOffset = if (difficulty == AiDifficulty.PEN_NINJA) (Random.nextFloat() * 0.6f - 0.3f) else 0f
        val hitPointX = aiPen.x + cos(aiPen.angle + PI.toFloat() / 2f) * hitOffset * aiPen.width
        val hitPointY = aiPen.y + sin(aiPen.angle + PI.toFloat() / 2f) * hitOffset * aiPen.width

        val powerRatio = (calculatedPower / PenPhysics.MAX_FLICK_FORCE).coerceIn(0.1f, 1.0f)

        return AiShotPlan(
            flickVx = flickVx,
            flickVy = flickVy,
            hitPointX = hitPointX,
            hitPointY = hitPointY,
            powerRatio = powerRatio
        )
    }
}
