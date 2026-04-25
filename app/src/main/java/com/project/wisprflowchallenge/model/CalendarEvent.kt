package com.project.wisprflowchallenge.model

data class CalendarEvent(
    val title: String,           // e.g. "Lunch with Sarah"
    val startMillis: Long,       // Unix ms — what CalendarContract needs
    val endMillis: Long,         // startMillis + durationMillis
    val description: String = "",
    val location: String = ""
)