package com.project.wisprflowchallenge.calendar


import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import com.project.wisprflowchallenge.model.CalendarEvent
import java.util.TimeZone

/**
 * Writes events to the device's default calendar.
 *
 * CalendarContract is Android's built-in calendar database.
 * We insert directly rather than using an Intent so the user
 * stays in our app (no jumping to the Google Calendar app).
 */
class CalendarRepository(private val context: Context) {

    /**
     * Finds the ID of the user's primary calendar (the first one found).
     * Returns null if no calendars are set up on the device.
     */
    fun getPrimaryCalendarId(): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY
        )

        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            null, null, null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val isPrimary = cursor.getInt(1) == 1
                if (isPrimary) return id
            }
            // Fall back to first calendar if none marked primary
            cursor.moveToFirst()
            return if (cursor.count > 0) cursor.getLong(0) else null
        }
        return null
    }

    /**
     * Inserts the event into the calendar.
     * @return true if successful, false if something went wrong
     */
    fun insertEvent(event: CalendarEvent): Boolean {
        val calendarId = getPrimaryCalendarId() ?: return false

        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, event.title)
            put(CalendarContract.Events.DTSTART, event.startMillis)
            put(CalendarContract.Events.DTEND, event.endMillis)
            put(CalendarContract.Events.DESCRIPTION, event.description)
            put(CalendarContract.Events.EVENT_LOCATION, event.location)
            // Use the device's local timezone so "noon" means noon locally
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        }

        val uri = context.contentResolver.insert(
            CalendarContract.Events.CONTENT_URI,
            values
        )

        return uri != null  // null means insert failed
    }
}