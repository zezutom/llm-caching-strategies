package com.tzezula.llmcache.client

interface LlmClient {
    suspend fun summarize(prompt: String): String?
}
