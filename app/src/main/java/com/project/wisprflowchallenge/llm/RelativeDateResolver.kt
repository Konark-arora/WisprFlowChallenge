package com.project.wisprflowchallenge.llm

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Resolves all relative date/time expressions to absolute values
 * before the transcript is sent to the LLM.
 *
 * Handles: today, tomorrow, yesterday, day names ("monday", "next friday"),
 * "next week", "this week", and time shorthands.
 */
object RelativeDateResolver {

    fun resolve(text: String, now: Calendar = Calendar.getInstance()): String {
        var result = text

        // Time shorthands
        result = result.replace(Regex("\\bnoon\\b", RegexOption.IGNORE_CASE), "12:00")
        result = result.replace(Regex("\\bmidnight\\b", RegexOption.IGNORE_CASE), "00:00")
        result = result.replace(Regex("\\bmorning\\b", RegexOption.IGNORE_CASE), "09:00")
        result = result.replace(Regex("\\beverning\\b", RegexOption.IGNORE_CASE), "18:00")
        result = result.replace(Regex("\\bafternoon\\b", RegexOption.IGNORE_CASE), "14:00")

        // today / tomorrow / yesterday
        result = result.replace(
            Regex("\\btoday\\b", RegexOption.IGNORE_CASE),
            formatDate(now)
        )
        result = result.replace(
            Regex("\\btomorrow\\b", RegexOption.IGNORE_CASE),
            formatDate((now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) })
        )
        result = result.replace(
            Regex("\\byesterday\\b", RegexOption.IGNORE_CASE),
            formatDate((now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) })
        )

        // "next <weekday>" — always the weekday in the NEXT 7–13 days
        result = result.replace(
            Regex("\\bnext\\s+(monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b", RegexOption.IGNORE_CASE)
        ) { match ->
            val dayName = match.groupValues[1]
            formatDate(nextWeekday(now, dayName, forceNextWeek = true))
        }

        // "this <weekday>" — the upcoming occurrence within the current week
        result = result.replace(
            Regex("\\bthis\\s+(monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b", RegexOption.IGNORE_CASE)
        ) { match ->
            val dayName = match.groupValues[1]
            formatDate(nextWeekday(now, dayName, forceNextWeek = false))
        }

        // bare "<weekday>" — nearest future occurrence (same as "this")
        result = result.replace(
            Regex("\\b(monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b", RegexOption.IGNORE_CASE)
        ) { match ->
            val dayName = match.groupValues[1]
            formatDate(nextWeekday(now, dayName, forceNextWeek = false))
        }

        return result
    }

    /**
     * Returns the Calendar for the target weekday.
     *
     * @param forceNextWeek  true  → skip the current week entirely ("next Friday")
     *                       false → return the nearest future occurrence, today included
     */
    private fun nextWeekday(from: Calendar, dayName: String, forceNextWeek: Boolean): Calendar {
        val targetDow = dayNameToDayOfWeek(dayName)
        val cal = from.clone() as Calendar

        if (forceNextWeek) {
            // Jump to the same weekday next week
            cal.add(Calendar.WEEK_OF_YEAR, 1)
            cal.set(Calendar.DAY_OF_WEEK, targetDow)
        } else {
            // Move forward until we hit the target day (today counts)
            val startDow = cal.get(Calendar.DAY_OF_WEEK)
            var daysAhead = (targetDow - startDow + 7) % 7
            if (daysAhead == 0) daysAhead = 0  // today is fine for "this X"
            cal.add(Calendar.DAY_OF_YEAR, daysAhead)
        }

        return cal
    }

    private fun dayNameToDayOfWeek(name: String): Int = when (name.lowercase()) {
        "sunday"    -> Calendar.SUNDAY
        "monday"    -> Calendar.MONDAY
        "tuesday"   -> Calendar.TUESDAY
        "wednesday" -> Calendar.WEDNESDAY
        "thursday"  -> Calendar.THURSDAY
        "friday"    -> Calendar.FRIDAY
        "saturday"  -> Calendar.SATURDAY
        else        -> Calendar.MONDAY
    }

    private fun formatDate(cal: Calendar): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
}