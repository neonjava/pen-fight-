package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.ArenaType
import com.example.model.PenInstance
import com.example.model.PenPhysics
import com.example.model.PenStyle
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read app name string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Duo Pen Fight", appName)
  }

  @Test
  fun `test pen physics flick and friction`() {
    val physics = PenPhysics()
    val pen = PenInstance(
      playerId = 1,
      x = 500f,
      y = 1000f,
      angle = 0f
    )

    physics.applyFlick(pen, flickVx = 0f, flickVy = -500f, hitPointX = 500f, hitPointY = 1000f)
    assertTrue("Pen should have negative Y velocity after flicking upward", pen.vy < 0)
    assertTrue(pen.isMoving)

    // Run physics update step
    physics.updatePhysics(
      dt = 0.016f,
      pens = listOf(pen),
      obstacles = emptyList(),
      arena = ArenaType.WOODEN_DESK,
      onPenCollision = {},
      onObstacleCollision = {},
      onPenFall = {}
    )

    assertTrue("Pen Y position should have moved upward", pen.y < 1000f)
  }

  @Test
  fun `test pen models have valid dimensions and mass`() {
    for (style in PenStyle.entries) {
      assertTrue(style.baseLength > 100f)
      assertTrue(style.baseWidth > 15f)
      assertTrue(style.mass > 0.5f)
      assertTrue(style.speedMultiplier > 0.5f)
    }
  }
}
