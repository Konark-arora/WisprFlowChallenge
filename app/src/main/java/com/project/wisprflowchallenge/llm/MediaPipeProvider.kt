package com.project.wisprflowchallenge.llm

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class MediaPipeProvider(private val context: Context, private val modelAssetName: String) : LlmProvider {

    private var llmInference: LlmInference? = null

    override suspend fun prepare() = withContext(Dispatchers.IO) {
        if (llmInference == null) {
            try {
                val modelFile = File(context.cacheDir, modelAssetName)
                if (!modelFile.exists()) {
                    Log.d("MediaPipeProvider", "Copying model from assets...")
                    context.assets.open("models/$modelAssetName").use { inputStream ->
                        FileOutputStream(modelFile).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                }
                
                llmInference = LlmInference.createFromOptions(
                    context,
                    LlmInference.LlmInferenceOptions.builder()
                        .setModelPath(modelFile.absolutePath)
                        .build()
                )
            } catch (e: Exception) {
                Log.e("MediaPipeProvider", "MediaPipe initialization failed: ${e.message}")
                throw e
            }
        }
    }

    override suspend fun generateContent(prompt: String): String? {
        return try {
            llmInference?.generateResponse(prompt)
        } catch (e: Exception) {
            Log.e("MediaPipeProvider", "Inference failed: ${e.message}")
            null
        }
    }
}
