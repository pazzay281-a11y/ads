package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppPreferences
import com.example.data.BubbleClickAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Ad Reset Floating", appName)
  }

  @Test
  fun `test preferences defaults and updates`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = AppPreferences(context)

    assertEquals(58, prefs.bubbleSizeDp.value)
    assertEquals(BubbleClickAction.DIRECT_RESET, prefs.clickAction.value)

    prefs.setBubbleSize(65)
    assertEquals(65, prefs.bubbleSizeDp.value)

    prefs.recordResetAction(note = "Test Reset")
    assertEquals(1, prefs.resetCount.value)
    assertTrue(prefs.history.value.isNotEmpty())
  }
}
