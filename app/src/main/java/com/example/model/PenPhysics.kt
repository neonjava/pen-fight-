package com.example.model

import kotlin.math.*

class PenPhysics {
    companion object {
        const val TABLE_WIDTH = 1000f
        const val TABLE_HEIGHT = 1600f
        const val MARGIN_LEFT = 75f
        const val MARGIN_RIGHT = 925f
        const val MARGIN_TOP = 95f
        const val MARGIN_BOTTOM = 1505f

        const val MAX_FLICK_FORCE = 1800f
        const val MIN_FLICK_FORCE = 80f
    }

    data class CollisionEvent(
        val penA: PenInstance,
        val penB: PenInstance,
        val contactX: Float,
        val contactY: Float,
        val impactIntensity: Float
    )

    data class ObstacleCollisionEvent(
        val pen: PenInstance,
        val obstacle: DeskObstacle,
        val contactX: Float,
        val contactY: Float,
        val impactIntensity: Float
    )

    fun updatePhysics(
        dt: Float,
        pens: List<PenInstance>,
        obstacles: List<DeskObstacle>,
        arena: ArenaType,
        onPenCollision: (CollisionEvent) -> Unit,
        onObstacleCollision: (ObstacleCollisionEvent) -> Unit,
        onPenFall: (PenInstance) -> Unit
    ) {
        val subSteps = 6
        val subDt = dt / subSteps

        for (step in 0 until subSteps) {
            // 1. Move pens
            for (pen in pens) {
                if (pen.isOffTable) {
                    // Falling animation
                    pen.fallScale = (pen.fallScale - subDt * 1.8f).coerceAtLeast(0.3f)
                    pen.fallAlpha = (pen.fallAlpha - subDt * 2.2f).coerceAtLeast(0f)
                    pen.fallRotation += pen.angularVelocity * subDt * 2.5f
                    pen.x += pen.vx * subDt * 0.5f
                    pen.y += pen.vy * subDt * 0.5f
                    continue
                }

                val speed = sqrt(pen.vx * pen.vx + pen.vy * pen.vy)
                if (speed > 0.001f || abs(pen.angularVelocity) > 0.001f) {
                    pen.isMoving = true
                    pen.x += pen.vx * subDt
                    pen.y += pen.vy * subDt
                    pen.angle += pen.angularVelocity * subDt

                    // Surface friction
                    val frictionFactor = 1.35f * arena.friction * pen.config.style.frictionMultiplier
                    val linearDecay = (1.0f - frictionFactor * subDt).coerceIn(0.85f, 1.0f)
                    val angularDecay = (1.0f - frictionFactor * 1.6f * subDt).coerceIn(0.80f, 1.0f)

                    pen.vx *= linearDecay
                    pen.vy *= linearDecay
                    pen.angularVelocity *= angularDecay

                    if (speed < 3.0f && abs(pen.angularVelocity) < 0.08f) {
                        pen.vx = 0f
                        pen.vy = 0f
                        pen.angularVelocity = 0f
                        pen.isMoving = false
                    }
                } else {
                    pen.isMoving = false
                }

                // Check table boundaries (Falling off desk!)
                checkTableBounds(pen, onPenFall)
            }

            // 2. Pen vs Pen collisions
            for (i in 0 until pens.size) {
                for (j in i + 1 until pens.size) {
                    val p1 = pens[i]
                    val p2 = pens[j]
                    if (!p1.isOffTable && !p2.isOffTable) {
                        resolvePenPenCollision(p1, p2, arena, onPenCollision)
                    }
                }
            }

            // 3. Pen vs Obstacles
            for (pen in pens) {
                if (pen.isOffTable) continue
                for (obs in obstacles) {
                    resolvePenObstacleCollision(pen, obs, arena, onObstacleCollision)
                }
            }
        }
    }

    private fun checkTableBounds(pen: PenInstance, onPenFall: (PenInstance) -> Unit) {
        if (pen.isOffTable) return

        val tipX = pen.getTipX()
        val tipY = pen.getTipY()
        val capX = pen.getCapX()
        val capY = pen.getCapY()
        val centerX = pen.x
        val centerY = pen.y

        var offPoints = 0
        if (tipX < MARGIN_LEFT || tipX > MARGIN_RIGHT || tipY < MARGIN_TOP || tipY > MARGIN_BOTTOM) offPoints++
        if (capX < MARGIN_LEFT || capX > MARGIN_RIGHT || capY < MARGIN_TOP || capY > MARGIN_BOTTOM) offPoints++
        if (centerX < MARGIN_LEFT || centerX > MARGIN_RIGHT || centerY < MARGIN_TOP || centerY > MARGIN_BOTTOM) offPoints += 2

        // If center of mass is off or both ends are off
        if (offPoints >= 2) {
            pen.isOffTable = true
            pen.angularVelocity += if (pen.vx > 0) 3.5f else -3.5f
            onPenFall(pen)
        }
    }

    private fun resolvePenPenCollision(
        p1: PenInstance,
        p2: PenInstance,
        arena: ArenaType,
        onCollision: (CollisionEvent) -> Unit
    ) {
        // Line segment for P1
        val p1TipX = p1.getTipX()
        val p1TipY = p1.getTipY()
        val p1CapX = p1.getCapX()
        val p1CapY = p1.getCapY()
        val r1 = p1.width / 2f

        // Line segment for P2
        val p2TipX = p2.getTipX()
        val p2TipY = p2.getTipY()
        val p2CapX = p2.getCapX()
        val p2CapY = p2.getCapY()
        val r2 = p2.width / 2f

        // Find closest points between line segments
        val (closest1, closest2) = closestPointsBetweenSegments(
            p1CapX, p1CapY, p1TipX, p1TipY,
            p2CapX, p2CapY, p2TipX, p2TipY
        )

        val dx = closest2.first - closest1.first
        val dy = closest2.second - closest1.second
        val dist = sqrt(dx * dx + dy * dy)
        val minDist = r1 + r2

        if (dist < minDist && dist > 0.0001f) {
            val nx = dx / dist
            val ny = dy / dist

            val overlap = minDist - dist
            // Positional correction
            p1.x -= nx * overlap * 0.5f
            p1.y -= ny * overlap * 0.5f
            p2.x += nx * overlap * 0.5f
            p2.y += ny * overlap * 0.5f

            // Contact point in table coordinates
            val cx = (closest1.first + closest2.first) * 0.5f
            val cy = (closest1.second + closest2.second) * 0.5f

            // Lever arms from centers of mass
            val rx1 = cx - p1.x
            val ry1 = cy - p1.y
            val rx2 = cx - p2.x
            val ry2 = cy - p2.y

            // Velocity of contact points
            val vp1x = p1.vx - p1.angularVelocity * ry1
            val vp1y = p1.vy + p1.angularVelocity * rx1
            val vp2x = p2.vx - p2.angularVelocity * ry2
            val vp2y = p2.vy + p2.angularVelocity * rx2

            val relVx = vp1x - vp2x
            val relVy = vp1y - vp2y
            val velAlongNormal = relVx * nx + relVy * ny

            // Only resolve if moving towards each other
            if (velAlongNormal > 0) {
                val e = arena.elasticity * 0.92f

                // Cross product terms (r x n)
                val r1CrossN = rx1 * ny - ry1 * nx
                val r2CrossN = rx2 * ny - ry2 * nx

                val invMass1 = 1f / p1.mass
                val invMass2 = 1f / p2.mass
                val invI1 = 1f / p1.momentOfInertia
                val invI2 = 1f / p2.momentOfInertia

                val denominator = invMass1 + invMass2 + (r1CrossN * r1CrossN * invI1) + (r2CrossN * r2CrossN * invI2)
                val impulseMag = ((1f + e) * velAlongNormal) / denominator

                val impulseX = nx * impulseMag
                val impulseY = ny * impulseMag

                // Apply linear impulse
                p1.vx -= impulseX * invMass1
                p1.vy -= impulseY * invMass1
                p2.vx += impulseX * invMass2
                p2.vy += impulseY * invMass2

                // Apply angular torque
                p1.angularVelocity -= (r1CrossN * impulseMag * invI1) * p1.config.style.spinMultiplier
                p2.angularVelocity += (r2CrossN * impulseMag * invI2) * p2.config.style.spinMultiplier

                // Tangential friction impulse
                val tx = -ny
                val ty = nx
                val velAlongTangent = relVx * tx + relVy * ty
                val r1CrossT = rx1 * ty - ry1 * tx
                val r2CrossT = rx2 * ty - ry2 * tx
                val denomT = invMass1 + invMass2 + (r1CrossT * r1CrossT * invI1) + (r2CrossT * r2CrossT * invI2)
                val frictionMu = 0.38f
                val maxFrictionImpulse = impulseMag * frictionMu
                val frictionImpulseMag = (velAlongTangent / denomT).coerceIn(-maxFrictionImpulse, maxFrictionImpulse)

                p1.vx -= tx * frictionImpulseMag * invMass1
                p1.vy -= ty * frictionImpulseMag * invMass1
                p2.vx += tx * frictionImpulseMag * invMass2
                p2.vy += ty * frictionImpulseMag * invMass2

                p1.angularVelocity -= (r1CrossT * frictionImpulseMag * invI1)
                p2.angularVelocity += (r2CrossT * frictionImpulseMag * invI2)

                p1.isMoving = true
                p2.isMoving = true
                p1.consecutiveHits++
                p2.consecutiveHits++

                val intensity = (velAlongNormal / 600f).coerceIn(0.2f, 1.0f)
                onCollision(CollisionEvent(p1, p2, cx, cy, intensity))
            }
        }
    }

    private fun resolvePenObstacleCollision(
        pen: PenInstance,
        obs: DeskObstacle,
        arena: ArenaType,
        onCollision: (ObstacleCollisionEvent) -> Unit
    ) {
        val penTipX = pen.getTipX()
        val penTipY = pen.getTipY()
        val penCapX = pen.getCapX()
        val penCapY = pen.getCapY()
        val penRadius = pen.width / 2f

        // Approximate obstacle as bounding box with corner radius
        val halfW = obs.width / 2f
        val halfH = obs.height / 2f

        // Sample points along pen spine
        val samples = 7
        for (s in 0..samples) {
            val t = s.toFloat() / samples
            val px = penCapX + t * (penTipX - penCapX)
            val py = penCapY + t * (penTipY - penCapY)

            val relX = px - obs.x
            val relY = py - obs.y

            // Nearest point on obstacle box
            val nearestX = relX.coerceIn(-halfW, halfW)
            val nearestY = relY.coerceIn(-halfH, halfH)

            val diffX = relX - nearestX
            val diffY = relY - nearestY
            val d = sqrt(diffX * diffX + diffY * diffY)

            if (d < penRadius) {
                val nx = if (d > 0.0001f) diffX / d else 0f
                val ny = if (d > 0.0001f) diffY / d else if (relY > 0) 1f else -1f

                val overlap = penRadius - d
                pen.x += nx * overlap
                pen.y += ny * overlap

                val rx = (obs.x + nearestX) - pen.x
                val ry = (obs.y + nearestY) - pen.y

                val vpx = pen.vx - pen.angularVelocity * ry
                val vpy = pen.vy + pen.angularVelocity * rx
                val velNorm = vpx * nx + vpy * ny

                if (velNorm < 0) {
                    val e = arena.elasticity * 0.85f
                    val rCrossN = rx * ny - ry * nx
                    val invMass = 1f / pen.mass
                    val invI = 1f / pen.momentOfInertia
                    val denom = invMass + (rCrossN * rCrossN * invI)
                    val j = - (1f + e) * velNorm / denom

                    pen.vx += nx * j * invMass
                    pen.vy += ny * j * invMass
                    pen.angularVelocity += (rCrossN * j * invI) * 0.8f
                    pen.isMoving = true

                    val intensity = (abs(velNorm) / 500f).coerceIn(0.2f, 1.0f)
                    onCollision(ObstacleCollisionEvent(pen, obs, obs.x + nearestX, obs.y + nearestY, intensity))
                }
                break
            }
        }
    }

    private fun closestPointsBetweenSegments(
        p1x: Float, p1y: Float, q1x: Float, q1y: Float,
        p2x: Float, p2y: Float, q2x: Float, q2y: Float
    ): Pair<Pair<Float, Float>, Pair<Float, Float>> {
        val d1x = q1x - p1x
        val d1y = q1y - p1y
        val d2x = q2x - p2x
        val d2y = q2y - p2y
        val rx = p1x - p2x
        val ry = p1y - p2y

        val a = d1x * d1x + d1y * d1y
        val e = d2x * d2x + d2y * d2y
        val f = d2x * ry - d2y * rx

        var s: Float
        var t: Float

        if (a <= 0.0001f && e <= 0.0001f) {
            return Pair(Pair(p1x, p1y), Pair(p2x, p2y))
        }

        if (a <= 0.0001f) {
            s = 0f
            t = (d2x * -rx + d2y * -ry) / e
            t = t.coerceIn(0f, 1f)
        } else {
            val c = d1x * rx + d1y * ry
            if (e <= 0.0001f) {
                t = 0f
                s = (-c / a).coerceIn(0f, 1f)
            } else {
                val b = d1x * d2x + d1y * d2y
                val denom = a * e - b * b

                s = if (denom != 0f) {
                    ((b * (d2x * -rx + d2y * -ry) - c * e) / denom).coerceIn(0f, 1f)
                } else {
                    0f
                }

                t = (b * s + (d2x * -rx + d2y * -ry)) / e
                if (t < 0f) {
                    t = 0f
                    s = (-c / a).coerceIn(0f, 1f)
                } else if (t > 1f) {
                    t = 1f
                    s = ((b - c) / a).coerceIn(0f, 1f)
                }
            }
        }

        val c1x = p1x + d1x * s
        val c1y = p1y + d1y * s
        val c2x = p2x + d2x * t
        val c2y = p2y + d2y * t

        return Pair(Pair(c1x, c1y), Pair(c2x, c2y))
    }

    /**
     * Apply flick impulse to a pen.
     * dragStartX / dragStartY: Touch anchor point on the table.
     * dragEndX / dragEndY: Release point.
     * flickOrigin: Where on the pen the flick was struck (allows torque/spin!).
     */
    fun applyFlick(
        pen: PenInstance,
        flickVx: Float,
        flickVy: Float,
        hitPointX: Float = pen.x,
        hitPointY: Float = pen.y
    ) {
        val speed = sqrt(flickVx * flickVx + flickVy * flickVy)
        val clampedSpeed = speed.coerceIn(MIN_FLICK_FORCE, MAX_FLICK_FORCE) * pen.config.style.speedMultiplier
        val scale = if (speed > 0) clampedSpeed / speed else 1f

        val finalVx = flickVx * scale
        val finalVy = flickVy * scale

        // Calculate lever arm from center of mass for spin!
        val rx = hitPointX - pen.x
        val ry = hitPointY - pen.y
        val torque = (rx * finalVy - ry * finalVx) / (pen.momentOfInertia * 0.035f)

        pen.vx += finalVx / pen.mass
        pen.vy += finalVy / pen.mass
        pen.angularVelocity += torque * pen.config.style.spinMultiplier
        pen.isMoving = true
    }
}
