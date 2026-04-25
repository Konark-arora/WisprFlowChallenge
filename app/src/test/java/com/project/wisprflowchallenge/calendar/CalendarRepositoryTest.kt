package com.project.wisprflowchallenge.calendar

import android.content.Context
import android.provider.CalendarContract
import androidx.test.core.app.ApplicationProvider
import com.project.wisprflowchallenge.model.CalendarEvent
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.util.*

@RunWith(RobolectricTestRunner::class)
class CalendarRepositoryTest {

    private lateinit var repository: CalendarRepository
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repository = CalendarRepository(context)
    }

    @Test
    fun `getPrimaryCalendarId returns null when no calendars exist`() {
        val id = repository.getPrimaryCalendarId()
        assertNull(id)
    }

    @Test
    fun `insertEvent returns false when no primary calendar found`() {
        val event = CalendarEvent("Test Event", 1000L, 2000L, "Desc", "Loc")
        val result = repository.insertEvent(event)
        assertFalse(result)
    }
}
