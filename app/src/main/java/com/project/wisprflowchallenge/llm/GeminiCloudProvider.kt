package com.project.wisprflowchallenge.llm

import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiCloudProvider(private val model: GenerativeModel) : LlmProvider {
    override suspend fun prepare() {
        // Cloud models don't need local preparation like on-device models
    }

    override suspend fun generateContent(prompt: String): String? = withContext(Dispatchers.IO) {
        try {
            model.generateContent(prompt).text
        } catch (e: Exception) {
            val message = e.message ?: ""
            when {
                message.contains("QuotaExceeded") -> {
                    android.util.Log.e("GeminiCloud", "Quota exceeded — switch model or enable billing")
                    throw Exception("AI service quota exceeded. Please try again later.")
                }
                message.contains("API_KEY") || message.contains("403") -> {
                    throw Exception("Invalid API key.")
                }
                else -> {
                    android.util.Log.e("GeminiCloud", "FAILED: ${e::class.simpleName}: ${message}")
                    null
                }
            }
        }
    }
}
