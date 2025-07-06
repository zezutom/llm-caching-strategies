package com.tzezula.llmcache.client

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LlmLatencyTest {
    private lateinit var llmClient: LlmClient

    @BeforeEach
    fun setUp() {
        llmClient = DummyLlmClient()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `llm takes more than 900ms each call()`() = runTest {
        val n = 5
        val startTime = currentTime
        repeat(n) {
            llmClient.summarize("This is a test prompt $it")
        }
        val totalTime = currentTime - startTime
        assert(totalTime > n * 900) {
            "Expected total time for $n calls to be more than ${n * 900}ms, but was $totalTime ms"
        }
    }
}
