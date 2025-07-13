package com.tzezula.llmcache.service

import com.tzezula.llmcache.client.LlmClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CoalescingCacheServiceTest {
    private lateinit var llmClient: LlmClient
    private lateinit var cacheService: CoalescingCacheService

    @BeforeEach
    fun setUp() {
        llmClient = mockk<LlmClient>()
        cacheService = CoalescingCacheService(llmClient)
    }

    @Test
    fun `coalescing only invokes once`() = runTest {
        val prompt = "This is a test prompt"
        coEvery { llmClient.summarize(prompt) } coAnswers {
            // Simulate LLM latency
            delay(1000)
            "Summary for: $prompt"
        }

        // Fire off 5 concurrent calls to summarize
        coroutineScope {
            repeat(5) {
                launch {
                    cacheService.summarize(prompt)
                }
            }
        }

        // The coalescing cache should only invoke the LLM once
        coVerify(exactly = 1) {
            llmClient.summarize(prompt)
        }
    }
}
