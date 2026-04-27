package com.project.wisprflowchallenge.llm


import com.project.wisprflowchallenge.model.CalendarEvent
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * Uses Gemini Nano (on-device) to extract structured event data
 * from a free-form voice transcript.
 */
class EventParser(private var provider: LlmProvider) {

    fun provider(): LlmProvider = provider

    /**
     * Pre-warms the generative model. This can trigger a model download
     * if the model is missing but configured via DownloadConfig.
     */
    suspend fun prepare() {
        provider.prepare()
    }

    data class ParseResult(
        val title: String,
        val dateIso: String,     // "2024-07-15"
        val timeHhmm: String,    // "12:00" (24h)
        val durationMinutes: Int,
        val location: String,
        val attendees: String    // comma-separated names
    )

    suspend fun parse(transcript: String): ParseResult {
        val normalized = normalizeTranscript(transcript)
        val resolved = RelativeDateResolver.resolve(normalized)
        val today = SimpleDateFormat("yyyy-MM-dd EEEE", Locale.US).format(Date())

        val prompt = """
            Today is $today.
            All relative date references have already been resolved to absolute YYYY-MM-DD 
            dates in the transcript. Preserve them exactly — do NOT reinterpret them.
        
            Extract a calendar event from this voice transcript.
            The transcript may be informal, incomplete, or missing words.
        
            Return ONLY valid JSON with no markdown, no code fences, no explanation.
            Start your response with { and end with }.
        
            Transcript: "$resolved"
        
            JSON format:
            {
              "title": "string",
              "date": "YYYY-MM-DD",
              "time": "HH:MM",
              "duration_minutes": number,
              "location": "string",
              "attendees": "string"
            }
        
            Rules:
            - title: short descriptive name only (e.g. "Meeting", "Lunch", "Call") — NEVER copy the raw transcript as the title
            - date: if a YYYY-MM-DD appears in the transcript, copy it exactly
            - date: if no YYYY-MM-DD found, use ${todayDate()}
            - time: extract from transcript (e.g. "6:00" → "06:00", "3pm" → "15:00")
            - time: if no time mentioned → "09:00"
            - duration_minutes: if not mentioned → 60
            - Use 24-hour time format
            - Never return empty strings for date or time
            - Always return valid JSON (no trailing commas)
        """.trimIndent()

        android.util.Log.d("EventParser", "Original: $transcript")
        android.util.Log.d("EventParser", "Normalized: $normalized")
        android.util.Log.d("EventParser", "Resolved: $resolved")

        return try {
            val raw = provider.generateContent(prompt)

            android.util.Log.d("EventParser", "LLM raw output before: $raw")

            if (raw.isNullOrEmpty()) {
                return fallbackParse(transcript)
            }

            // Log raw output during development to see what the LLM actually returns
            android.util.Log.d("EventParser", "LLM raw output: $raw")

            val clean = extractJson(raw)
            android.util.Log.d("EventParser", "Extracted JSON: $clean")

            val json = JSONObject(clean)

            val title = json.optString("title").takeIf { it.isNotBlank() } ?: inferTitle(transcript)
            val date = json.optString("date").takeIf { isValidDate(it) } ?: todayDate()
            val time = json.optString("time").takeIf { isValidTime(it) } ?: "09:00"
            val duration = json.optInt("duration_minutes").takeIf { it > 0 } ?: 60
            val location = json.optString("location")
            val attendees = json.optString("attendees").takeIf { it.isNotBlank() }
                ?: extractAttendees(transcript)

            ParseResult(
                title = title,
                dateIso = date,
                timeHhmm = time,
                durationMinutes = duration,
                location = location,
                attendees = attendees
            )

        } catch (e: Exception) {
            android.util.Log.e("EventParser", "Parse failed, using fallback. Error: ${e.message}, Raw was: ${provider.generateContent(prompt)}")
            fallbackParse(transcript)
        }
    }

    // Only lowercase filler words, not the whole transcript
    private fun normalizeTranscript(input: String): String {
        return input
            .replace(Regex("\\b(uh|um|like|you know)\\b", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun isValidDate(value: String): Boolean {
        return try {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).also { it.isLenient = false }.parse(value)
            Regex("\\d{4}-\\d{2}-\\d{2}").matches(value)
        } catch (e: Exception) { false }
    }

    private fun isValidTime(value: String): Boolean {
        return Regex("^([01]?\\d|2[0-3]):[0-5]\\d$").matches(value)
    }

    private fun extractJson(raw: String): String {
        val start = raw.indexOf("{")
        val end = raw.lastIndexOf("}")
        if (start == -1 || end == -1) throw IllegalArgumentException("No JSON")
        return raw.substring(start, end + 1)
    }

    private fun fallbackParse(text: String): ParseResult {
        val attendees = extractAttendees(text)

        return ParseResult(
            title = inferTitle(text),
            dateIso = todayDate(),
            timeHhmm = if (text.contains("noon")) "12:00" else "09:00",
            durationMinutes = 60,
            location = "",
            attendees = attendees
        )
    }

    private fun extractAttendees(text: String): String {
        val match = Regex("with ([a-zA-Z]+)").find(text)
        return match?.groupValues?.get(1) ?: ""
    }

    private fun inferTitle(text: String): String {
        return when {
            text.contains("with") -> "Meeting"
            text.contains("call") -> "Call"
            text.contains("lunch") -> "Lunch"
            else -> text.take(30)
        }
    }

    private fun todayDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    /**
     * Converts the LLM output into a CalendarEvent with real Unix timestamps.
     */
    fun toCalendarEvent(result: ParseResult): CalendarEvent {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val startDate = sdf.parse("${result.dateIso} ${result.timeHhmm}") ?: Date()
        val startMillis = startDate.time
        val endMillis = startMillis + (result.durationMinutes * 60 * 1000L)

        return CalendarEvent(
            title = result.title,
            startMillis = startMillis,
            endMillis = endMillis,
            description = if (result.attendees.isNotEmpty()) "With: ${result.attendees}" else "",
            location = result.location
        )
    }
}
