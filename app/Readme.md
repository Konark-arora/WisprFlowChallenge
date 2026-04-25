# Voice Calendar Event Parser (Android)

An Android application that converts natural language voice or text input into structured calendar events using on-device AI (Gemini Nano / Edge AI Core).

The goal of this project is to make scheduling faster, more intuitive, and hands-free by turning simple voice commands into structured calendar entries.

---

## Features

- Convert natural language input into calendar events
- AI-powered parsing of time, date, and intent
- Automatic handling of phrases like "tomorrow", "noon", "next Friday"
- Extracts attendees from conversational input
- Smart defaults for missing fields (time, duration, etc.)
- Robust fallback parsing when AI output is incomplete
- Clean Android architecture in Kotlin

---

## Example Inputs

set with alex tomorrow at noon  
lunch with team friday at 1pm  
doctor appointment tomorrow

---

## How It Works

1. User enters or speaks a natural language command
2. Input is normalized (removes noise and filler words)
3. Prompt is sent to on-device LLM, fallback as google cloud if device don't have on-device AI
4. AI extracts structured JSON:
    - title
    - date
    - time
    - duration
    - attendees
    - location
5. Fallback logic is applied if AI output is incomplete or invalid
6. Final event is converted into a `CalendarEvent`

---

## Tech Stack

- Kotlin
- Android SDK
- Jetpack Components
- Gemini Nano / Edge AI Core (LLM)
- JSONObject parsing
- SimpleDateFormat / Calendar APIs

---

## Project Structure

com.project.wisprflowchallenge  
│  
├── llm  
│   └── EventParser.kt        (core parsing logic: LLM + fallback)  
│  
├── model  
│   └── CalendarEvent.kt      (event data model)  
│  
└── ui  
└── MainActivity.kt       (UI layer)

---

## Key Design Decisions

- Hybrid parsing approach combining LLM and deterministic fallback
- System does not depend on perfect AI output
- Time and date handled defensively to avoid incorrect scheduling
- Optimized for low latency voice-to-event creation
- Always returns a usable result even on failure

---

## Why This Project

This project demonstrates how AI can reduce friction in everyday productivity workflows by converting unstructured human intent into structured actions.

It focuses on:
- practical AI integration on Android
- reliability over perfect model output
- real-world UX for voice-driven interaction

---

## Author

Built as an Android + AI experiment focused on productivity and on-device intelligence.