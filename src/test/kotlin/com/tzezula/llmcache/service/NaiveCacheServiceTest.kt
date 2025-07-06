package com.tzezula.llmcache.service

import com.tzezula.llmcache.client.LlmClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class NaiveCacheServiceTest {
    private lateinit var llmClient: LlmClient
    private lateinit var cacheService: NaiveCacheService

    @BeforeEach
    fun setUp() {
        llmClient = mockk<LlmClient>()
        cacheService = NaiveCacheService(llmClient)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `naive cache invokes LLM for each concurrent duplicate`() = runTest {
        val prompt = "This is a test prompt"
        coEvery { llmClient.summarize(prompt) } coAnswers {
            // Simulate LLM latency
            delay(1000)
            "Summary for: $prompt"
        }

        // Fire off 3 concurrent calls to summarize
        coroutineScope {
            repeat(3) {
                launch {
                    cacheService.summarize(prompt)
                }
            }
        }

        // The naive cache didn't prevent multiple calls to the LLM
        coVerify(exactly = 3) {
            llmClient.summarize(prompt)
        }
    }
}
