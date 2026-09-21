package com.example

import com.example.data.model.ChatSender
import com.example.data.model.ChatMessage
import com.example.data.model.ChatbotPersona
import com.example.data.model.GeminiAiModel
import com.example.data.remote.GeminiClient
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleUnitTest {
    @Test
    fun testEngineStatusVoiceQuery() = runBlocking {
        val result = GeminiClient.generateChatResponse(
            messages = listOf(
                ChatMessage(
                    id = "1",
                    sender = ChatSender.USER,
                    content = "What is my current engine status?"
                )
            ),
            latestUserPrompt = "What is my current engine status?",
            model = GeminiAiModel.FLASH,
            persona = ChatbotPersona.MECHANIC,
            telemetryContext = "Live Telemetry: RPM: 1850, Speed: 45 MPH, Coolant Temp: 195°F, Battery Voltage: 14.2V, Engine Load: 32%"
        )

        assertTrue(result.text.isNotEmpty())
    }

    @Test
    fun testVeoVideoGeneration() = runBlocking {
        val video = GeminiClient.generateVeoVideo(
            prompt = "DTC P0300 cylinder 3 misfire 3D CAD simulation",
            aspectRatio = com.example.data.model.VeoAspectRatio.LANDSCAPE_16_9
        )
        assertNotNull(video)
        assertEquals("veo-3.1-fast-generate-preview", video.model)
        assertEquals(com.example.data.model.VeoAspectRatio.LANDSCAPE_16_9, video.aspectRatio)
        assertEquals(com.example.data.model.VeoGenerationStatus.READY, video.status)
    }
}

