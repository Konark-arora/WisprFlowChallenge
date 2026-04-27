package com.project.wisprflowchallenge.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.wisprflowchallenge.calendar.CalendarRepository
import com.project.wisprflowchallenge.llm.EventParser
import com.project.wisprflowchallenge.llm.LlmProvider
import com.project.wisprflowchallenge.model.CalendarEvent
import com.project.wisprflowchallenge.speech.VoiceRecognizer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class CalendarViewModel(
    private val voiceRecognizer: VoiceRecognizer,
    private var eventParser: EventParser,
    private val calendarRepo: CalendarRepository
) : ViewModel() {

    sealed class UiState {
        object Initializing : UiState()
        data class Downloading(val progress: Int) : UiState()
        object Idle : UiState()
        data class Listening(val partialText: String) : UiState()
        data class Processing(val transcript: String) : UiState()
        data class Confirming(val event: CalendarEvent) : UiState()
        data class Done(val event: CalendarEvent) : UiState()
        data class Error(val message: String) : UiState()
        data class Unavailable(val reason: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Initializing)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var initJob: Job? = null

    init {
        viewModelScope.launch {
            voiceRecognizer.partialText.collect { partial ->
                if (_uiState.value is UiState.Listening) {
                    _uiState.value = UiState.Listening(partial)
                }
            }
        }
        viewModelScope.launch {
            voiceRecognizer.finalText
                .filterNotNull()
                .filter { it.isNotEmpty() }
                .collect { processTranscript(it) }
        }
        viewModelScope.launch {
            voiceRecognizer.error
                .filterNotNull()
                .filter { it.isNotEmpty() }
                .collect { errorMessage ->
                    Log.e("CalendarViewModel", "VoiceRecognizer error: $errorMessage")
                    _uiState.value = UiState.Error(errorMessage)
                    // Auto-reset to Idle after 2 seconds so user can try again
                    delay(2000)
                    if (_uiState.value is UiState.Error) {
                        _uiState.value = UiState.Idle
                    }
                }
        }
    }

    // ✅ Does NOT call initializeModel — caller handles that
    fun setEventParser(newEventParser: EventParser) {
        this.eventParser = newEventParser
    }

    fun initializeModel(fallbacks: List<LlmProvider> = emptyList()) {
        initJob?.cancel()

        initJob = viewModelScope.launch {
            _uiState.value = UiState.Initializing
            val providers = mutableListOf(eventParser.provider()) + fallbacks
            Log.d("CalendarViewModel", "initializeModel: trying ${providers.size} providers: " +
                    providers.map { it::class.simpleName })

            var success = false
            for (provider in providers) {
                if (!isActive) {
                    Log.d("CalendarViewModel", "initializeModel: job cancelled")
                    return@launch
                }
                try {
                    Log.d("CalendarViewModel", "Trying: ${provider::class.simpleName}")
                    eventParser = EventParser(provider)
                    eventParser.prepare()
                    _uiState.value = UiState.Idle
                    Log.d("CalendarViewModel", " ${provider::class.simpleName} succeeded → Idle")
                    success = true
                    break
                } catch (e: Throwable) {
                    Log.e("CalendarViewModel", "Provider ${provider::class.simpleName} failed", e)
                }
            }

            if (!success) {
                Log.e("CalendarViewModel", "All providers failed")
                _uiState.value = UiState.Unavailable(
                    "No AI model available. Check your internet connection."
                )
            }
        }
    }

    fun startListening() {
        Log.d("CalendarViewModel", "startListening() — state: ${_uiState.value}")
        _uiState.value = UiState.Listening("")
        voiceRecognizer.startListening()
    }

    fun stopListening() {
        Log.d("CalendarViewModel", "stopListening() — state: ${_uiState.value}")
        voiceRecognizer.stopListening()
    }

    fun updateDownloadProgress(progress: Int) {
        _uiState.value = UiState.Downloading(progress)
    }

    fun onDownloadError(message: String) {
        Log.e("CalendarViewModel", "onDownloadError: $message")
        _uiState.value = UiState.Unavailable(message)
    }

    private fun normalizeTranscript(input: String): String {
        return input
            .lowercase()
            .replace(Regex("\\b(uh|um|like|you know)\\b"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun processTranscript(transcript: String) {
        viewModelScope.launch {
            Log.d("CalendarViewModel", "processTranscript: \"$transcript\"")
            _uiState.value = UiState.Processing(transcript)
            try {
                val parsed = eventParser.parse(normalizeTranscript(transcript))
                if (parsed == null) {
                    Log.w("CalendarViewModel", "parse() returned null")
                    _uiState.value = UiState.Error("Couldn't understand that. Try again!")
                    delay(2000)
                    if (_uiState.value is UiState.Error) _uiState.value = UiState.Idle
                    return@launch
                }
                val event = eventParser.toCalendarEvent(parsed)
                Log.d("CalendarViewModel", "Parsed event: $event")
                _uiState.value = UiState.Confirming(event)
            } catch (e: Throwable) {
                Log.e("CalendarViewModel", "processTranscript failed", e)
                _uiState.value = UiState.Error("AI error: ${e.message}")
                delay(2000)
                if (_uiState.value is UiState.Error) _uiState.value = UiState.Idle
            }
        }
    }

    fun confirmEvent(event: CalendarEvent) {
        Log.d("CalendarViewModel", "confirmEvent: $event")
        val success = calendarRepo.insertEvent(event)
        if (success) {
            _uiState.value = UiState.Done(event)
            viewModelScope.launch {
                delay(2000)
                if (_uiState.value is UiState.Done) {
                    _uiState.value = UiState.Idle
                }
            }
        } else {
            _uiState.value = UiState.Error("Couldn't save to calendar. Check permissions.")
        }
    }

    fun dismiss() {
        Log.d("CalendarViewModel", "dismiss() → Idle")
        _uiState.value = UiState.Idle
    }

    override fun onCleared() {
        Log.d("CalendarViewModel", "onCleared")
        voiceRecognizer.destroy()
    }
}