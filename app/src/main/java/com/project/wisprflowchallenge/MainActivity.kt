package com.project.wisprflowchallenge

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import com.google.ai.edge.aicore.DownloadCallback
import com.google.ai.edge.aicore.DownloadConfig
import com.google.ai.edge.aicore.GenerativeModel
import com.google.ai.edge.aicore.generationConfig
import com.project.wisprflowchallenge.Constants.GEMINI_API_KEY
import com.project.wisprflowchallenge.llm.AICoreProvider
import com.project.wisprflowchallenge.llm.EventParser
import com.project.wisprflowchallenge.llm.LlmProvider
import com.project.wisprflowchallenge.llm.MediaPipeProvider
import com.project.wisprflowchallenge.llm.GeminiCloudProvider
import com.google.ai.client.generativeai.GenerativeModel as CloudGenerativeModel
import com.project.wisprflowchallenge.calendar.CalendarRepository
import com.project.wisprflowchallenge.screens.HomeScreen
import com.project.wisprflowchallenge.speech.VoiceRecognizer
import com.project.wisprflowchallenge.viewmodels.CalendarViewModel

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_CALENDAR,
                Manifest.permission.WRITE_CALENDAR
            )
        )

        val voiceRecognizer = VoiceRecognizer(applicationContext)
        val calendarRepo    = CalendarRepository(applicationContext)

        val viewModel = androidx.lifecycle.ViewModelProvider(
            this,
            object : androidx.lifecycle.ViewModelProvider.Factory {
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return CalendarViewModel(
                        voiceRecognizer,
                        EventParser(object : LlmProvider {
                            override suspend fun prepare() {}
                            override suspend fun generateContent(prompt: String): String? = null
                        }),
                        calendarRepo
                    ) as T
                }
            }
        )[CalendarViewModel::class.java]

        // ── Provider 1: AICore (Gemini Nano) ──────────────────────────────
        val aicoreProvider = try {
            val generativeModel = GenerativeModel(
                generationConfig = generationConfig {
                    context = applicationContext
                    temperature = 0.1f
                    topK = 16
                },
                downloadConfig = DownloadConfig(object : DownloadCallback {
                    override fun onDownloadStarted(bytesToDownload: Long) {
                        viewModel.updateDownloadProgress(0)
                    }
                    override fun onDownloadProgress(totalBytesDownloaded: Long) {
                        viewModel.updateDownloadProgress(50)
                    }
                    override fun onDownloadCompleted() {
                        Log.d("MainActivity", "AICore model download completed")
                    }
                    override fun onDownloadFailed(
                        s: String,
                        e: com.google.ai.edge.aicore.GenerativeAIException
                    ) {
                        viewModel.onDownloadError("Download failed: $s")
                    }
                    override fun onDownloadDidNotStart(
                        e: com.google.ai.edge.aicore.GenerativeAIException
                    ) {}
                    override fun onDownloadPending() {
                        viewModel.updateDownloadProgress(0)
                    }
                })
            )
            AICoreProvider(generativeModel)
        } catch (t: Throwable) {
            Log.e("MainActivity", "AICoreProvider failed to instantiate", t)
            null
        }

        // ── Provider 2: Gemini Cloud ───────────────────────────────────────
        val cloudProvider = try {
            if (GEMINI_API_KEY.isNotEmpty()) {
                val cloudModel = CloudGenerativeModel(
                    modelName = "gemini-2.5-flash",
                    apiKey = GEMINI_API_KEY
                )
                GeminiCloudProvider(cloudModel)
            } else {
                Log.w("MainActivity", "No Gemini API key provided")
                null
            }
        } catch (t: Throwable) {
            Log.e("MainActivity", "GeminiCloudProvider failed to instantiate", t)
            null
        }

        // ── Provider 3: MediaPipe (local model file) ───────────────────────
        val mediaPipeProvider = try {
            MediaPipeProvider(applicationContext, "model.tflite")
        } catch (t: Throwable) {
            Log.e("MainActivity", "MediaPipeProvider failed to instantiate", t)
            null
        }

        // ── Build ordered provider list ────────────────────────────────────
        val providers = listOfNotNull(aicoreProvider, cloudProvider, mediaPipeProvider)
        Log.d("MainActivity", "Available providers: ${providers.map { it::class.simpleName }}")

        if (providers.isEmpty()) {
            Log.e("MainActivity", "No providers available!")
        }

        val initialProvider = providers.firstOrNull() ?: object : LlmProvider {
            override suspend fun prepare() {
                throw IllegalStateException("No LLM providers available")
            }
            override suspend fun generateContent(prompt: String): String? = null
        }

        val fallbacks = if (providers.size > 1) providers.drop(1) else emptyList()
        Log.d("MainActivity", "Primary: ${initialProvider::class.simpleName}, " +
                "Fallbacks: ${fallbacks.map { it::class.simpleName }}")

        viewModel.setEventParser(EventParser(initialProvider))
        viewModel.initializeModel(fallbacks = fallbacks)

        setContent {
            MaterialTheme {
                HomeScreen(viewModel)
            }
        }
    }
}