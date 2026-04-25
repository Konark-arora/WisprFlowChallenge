package com.project.wisprflowchallenge.llm

interface LlmProvider {
    suspend fun prepare()
    suspend fun generateContent(prompt: String): String?
}
