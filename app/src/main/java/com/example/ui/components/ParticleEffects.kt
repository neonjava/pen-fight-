package com.example.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.model.SparkParticle
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

object ParticleSystem {

    fun spawnClashSparks(
        x: Float,
        y: Float,
        intensity: Float,
        count: Int = (12 * intensity).toInt().coerceIn(6, 24)
    ): List<SparkParticle> {
        val particles = mutableListOf<SparkParticle>()
        val colors = listOf(
            0xFFFBBF24, 0xFFF59E0B, 0xFFEF4444, 0xFF60A5FA, 0xFFFFFFFF
        )

        for (i in 0 until count) {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = (150f + Random.nextFloat() * 450f) * intensity
            val vx = cos(angle) * speed
            val vy = sin(angle) * speed
            val color = colors.random()
            val size = 3f + Random.nextFloat() * 6f

            particles.add(
                SparkParticle(
                    id = System.nanoTime() + i,
                    x = x,
                    y = y,
                    vx = vx,
                    vy = vy,
                    color = color,
                    size = size,
                    life = 1f,
                    maxLife = 0.35f + Random.nextFloat() * 0.25f
                )
            )
        }
        return particles
    }

    fun spawnConfetti(): List<SparkParticle> {
        val particles = mutableListOf<SparkParticle>()
        val colors = listOf(
            0xFFEF4444, 0xFF3B82F6, 0xFF10B981, 0xFFF59E0B, 0xFF8B5CF6, 0xFFEC4899
        )
        for (i in 0 until 60) {
            val x = 100f + Random.nextFloat() * 800f
            val y = 400f + Random.nextFloat() * 800f
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = 80f + Random.nextFloat() * 280f
            particles.add(
                SparkParticle(
                    id = System.nanoTime() + i,
                    x = x,
                    y = y,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed - 120f,
                    color = colors.random(),
                    size = 7f + Random.nextFloat() * 7f,
                    life = 1f,
                    maxLife = 1.2f
                )
            )
        }
        return particles
    }

    fun updateParticles(particles: MutableList<SparkParticle>, dt: Float) {
        val iter = particles.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            p.life -= dt / p.maxLife
            if (p.life <= 0f) {
                iter.remove()
            } else {
                p.x += p.vx * dt
                p.y += p.vy * dt
                p.vy += 280f * dt // gravity
                p.alpha = (p.life).coerceIn(0f, 1f)
            }
        }
    }

    fun drawParticles(scope: DrawScope, particles: List<SparkParticle>) {
        for (p in particles) {
            val color = Color(p.color).copy(alpha = p.alpha)
            scope.drawCircle(
                color = color,
                radius = p.size * p.life,
                center = Offset(p.x, p.y)
            )
        }
    }
}
