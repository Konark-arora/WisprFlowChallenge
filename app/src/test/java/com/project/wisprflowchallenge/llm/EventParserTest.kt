package com.project.wisprflowchallenge.llm

import com.project.wisprflowchallenge.model.CalendarEvent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*

class EventParserTest {

    private lateinit var eventParser: EventParser
    private lateinit var fakeLlmProvider: FakeLlmProvider

    class FakeLlmProvider : LlmProvider {
        var nextResponse: String? = null
        var lastPrompt: String? = null

        override suspend fun prepare() {}

        override suspend fun generateContent(prompt: String): String? {
            lastPrompt = prompt
            return nextResponse
        }
    }

    @Before
    fun setUp() {
        fakeLlmProvider = FakeLlmProvider()
        eventParser = EventParser(fakeLlmProvider)
    }

    @Test
    fun `parse success with valid JSON`() = runBlocking {
        val transcript = "Lunch with Alice tomorrow at noon"
        fakeLlmProvider.nextResponse = """
            {
              "title": "Lunch with Alice",
              "date": "2024-07-16",
              "time": "12:00",
              "duration_minutes": 60,
              "location": "Cafe",
              "attendees": "Alice"
            }
        """.trimIndent()

        val result = eventParser.parse(transcript)

        assertEquals("Lunch with Alice", result.title)
        assertEquals("2024-07-16", result.dateIso)
        assertEquals("12:00", result.timeHhmm)
        assertEquals(60, result.durationMinutes)
        assertEquals("Cafe", result.location)
        assertEquals("Alice", result.attendees)
    }

    @Test
    fun `parse fallback when LLM returns null`() = runBlocking {
        val transcript = "lunch with Bob"
        fakeLlmProvider.nextResponse = null

        val result = eventParser.parse(transcript)

        // Based on fallbackParse logic, "with" takes precedence over "lunch"
        assertEquals("Meeting", result.title)
        assertEquals("09:00", result.timeHhmm)
        assertEquals("Bob", result.attendees)
    }

    @Test
    fun `parse fallback when LLM returns invalid JSON`() = runBlocking {
        val transcript = "call with Charlie"
        fakeLlmProvider.nextResponse = "Not a JSON"

        val result = eventParser.parse(transcript)

        // "with" takes precedence over "call"
        assertEquals("Meeting", result.title)
        assertEquals("Charlie", result.attendees)
    }

    @Test
    fun `toCalendarEvent converts ParseResult correctly`() {
        val result = EventParser.ParseResult(
            title = "Meeting",
            dateIso = "2024-07-15",
            timeHhmm = "14:30",
            durationMinutes = 90,
            location = "Office",
            attendees = "Dave"
        )

        val event = eventParser.toCalendarEvent(result)

        assertEquals("Meeting", event.title)
        assertEquals("Office", event.location)
        assertEquals("With: Dave", event.description)

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val expectedStart = sdf.parse("2024-07-15 14:30")!!.time
        assertEquals(expectedStart, event.startMillis)
        assertEquals(expectedStart + 90 * 60 * 1000L, event.endMillis)
    }
}
