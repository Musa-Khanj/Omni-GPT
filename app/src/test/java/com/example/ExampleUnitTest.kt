package com.example

import com.example.data.model.FactCheckResult
import com.example.data.model.GeneratedVideoItem
import com.example.data.model.GroundingSource
import com.example.data.model.VideoScene
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testVideoSceneSerializationAndModel() {
    val scenes = listOf(
      VideoScene(sceneNumber = 1, visualDescription = "Cinematic sunrise over Neo-Tokyo", cameraMotion = "Slow aerial zoom in", voiceover = "The morning of tomorrow begins now.", durationSeconds = 4),
      VideoScene(sceneNumber = 2, visualDescription = "Futuristic monorail speeding through clouds", cameraMotion = "Tracking shot", voiceover = "Seamless urban mobility.", durationSeconds = 5)
    )
    val videoItem = GeneratedVideoItem(
      title = "Neo-Tokyo Vision",
      script = "Sample script",
      scenes = scenes
    )
    assertEquals(2, videoItem.scenes.size)
    assertEquals(9, videoItem.scenes.sumOf { it.durationSeconds })
    assertEquals("Neo-Tokyo Vision", videoItem.title)
  }

  @Test
  fun testFactCheckModel() {
    val sources = listOf(
      GroundingSource(title = "NASA Exoplanet Archive", url = "https://exoplanets.nasa.gov"),
      GroundingSource(title = "Nature Astronomy", url = "https://nature.com/articles/exoplanets")
    )
    val result = FactCheckResult(
      claim = "Biosignature found on exoplanet K2-18b",
      verdict = "Partially Verified / Developing",
      summary = "Telescope data indicates preliminary spectral signatures requiring further observational validation.",
      keyPoints = listOf("Tentative atmospheric detection", "Needs follow-up spectroscopy"),
      sources = sources
    )
    assertEquals("Partially Verified / Developing", result.verdict)
    assertEquals(2, result.sources.size)
    assertTrue(result.sources.first().url.contains("nasa.gov"))
  }
}

