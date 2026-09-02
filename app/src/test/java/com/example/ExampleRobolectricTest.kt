package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.components.ContentBlock
import com.example.ui.components.parseMarkdownBlocks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ChatGPT AI", appName)
  }

  @Test
  fun `test markdown block parsing with code and headers`() {
    val sampleText = """
      # Welcome to ChatGPT
      Here is an algorithm:
      ```python
      print("Hello World")
      ```
      - Point 1
      - Point 2
    """.trimIndent()

    val blocks = parseMarkdownBlocks(sampleText)
    assertTrue(blocks.any { it is ContentBlock.Header && it.text == "Welcome to ChatGPT" })
    assertTrue(blocks.any { it is ContentBlock.Code && it.language == "python" })
    assertTrue(blocks.any { it is ContentBlock.BulletItem })
  }
}
