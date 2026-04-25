package com.project.wisprflowchallenge.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class VoiceRecognizer(private val context: Context) {

    private var recognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private var resultReceived = false

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText

    private val _finalText = MutableStateFlow<String?>(null)
    val finalText: StateFlow<String?> = _finalText

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun startListening() {
        mainHandler.post {
            Log.d("VoiceRecognizer", "startListening called on thread: ${Thread.currentThread().name}")

            destroyRecognizer()

            resultReceived = false
            _error.value = null
            _partialText.value = ""
            _finalText.value = null

            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                Log.e("VoiceRecognizer", "Speech recognition NOT available")
                _error.value = "Speech recognition not available on this device"
                return@post
            }

            recognizer = SpeechRecognizer.createSpeechRecognizer(context)

            if (recognizer == null) {
                Log.e("VoiceRecognizer", "Failed to create SpeechRecognizer")
                _error.value = "Failed to create speech recognizer"
                return@post
            }

            recognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(p: Bundle) {
                    Log.d("VoiceRecognizer", " onReadyForSpeech — mic is open")
                    _isListening.value = true
                }

                override fun onBeginningOfSpeech() {
                    Log.d("VoiceRecognizer", "onBeginningOfSpeech")
                }

                override fun onRmsChanged(v: Float) {}
                override fun onBufferReceived(b: ByteArray?) {}

                override fun onEndOfSpeech() {
                    Log.d("VoiceRecognizer", "onEndOfSpeech")
                    _isListening.value = false
                }

                override fun onError(error: Int) {
                    //  Ignore errors after result already received
                    if (resultReceived) {
                        Log.d("VoiceRecognizer", "Ignoring post-result error: $error")
                        return
                    }
                    val message = when (error) {
                        SpeechRecognizer.ERROR_AUDIO                    -> "Audio recording error"
                        SpeechRecognizer.ERROR_CLIENT                   -> "Client error — try restarting app"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission denied"
                        SpeechRecognizer.ERROR_NETWORK                  -> "Network error"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT          -> "Network timeout"
                        SpeechRecognizer.ERROR_NO_MATCH                 -> "No speech recognized — try again"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY          -> "Recognizer busy"
                        SpeechRecognizer.ERROR_SERVER                   -> "Server error"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT           -> "No speech detected — try again"
                        else                                            -> "Unknown error: $error"
                    }
                    Log.e("VoiceRecognizer", " onError: $error — $message")
                    _isListening.value = false
                    _error.value = message
                }

                override fun onResults(results: Bundle) {
                    val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""
                    Log.d("VoiceRecognizer", " onResults: $text")
                    resultReceived = true
                    _finalText.value = text
                    _isListening.value = false
                }

                override fun onPartialResults(partial: Bundle) {
                    val matches = partial.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: return
                    Log.d("VoiceRecognizer", "onPartialResults: $text")
                    _partialText.value = text
                }

                override fun onEvent(t: Int, p: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            try {
                recognizer?.startListening(intent)
                Log.d("VoiceRecognizer", "startListening() called on recognizer")
            } catch (e: Exception) {
                Log.e("VoiceRecognizer", "Exception calling startListening", e)
                _error.value = "Failed to start: ${e.message}"
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            Log.d("VoiceRecognizer", "stopListening called")
            // Only stop if we haven't received a result yet
            // If result already came in, recognizer stopped itself
            if (!resultReceived) {
                recognizer?.stopListening()
            }
        }
    }

    private fun destroyRecognizer() {
        recognizer?.destroy()
        recognizer = null
    }

    fun destroy() {
        mainHandler.post { destroyRecognizer() }
    }
}