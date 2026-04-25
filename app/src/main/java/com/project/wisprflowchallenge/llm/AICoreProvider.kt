package com.project.wisprflowchallenge.llm

import com.google.ai.edge.aicore.GenerativeModel

class AICoreProvider(private val model: GenerativeModel) : LlmProvider {
    override suspend fun prepare() {
        model.prepareInferenceEngine()
    }

    override suspend fun generateContent(prompt: String): String? {
        return model.generateContent(prompt).text
    }
}
