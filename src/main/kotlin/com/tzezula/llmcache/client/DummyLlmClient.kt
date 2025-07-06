package com.tzezula.llmcache.client

import kotlinx.coroutines.delay
import org.springframework.stereotype.Component

@Component
class DummyLlmClient : LlmClient {

    override suspend fun summarize(prompt: String): String? {
        // Simulate LLM latency
        delay(1000)

        // Return a fake summary by converting the prompt to uppercase
        return prompt.uppercase()
    }
}
