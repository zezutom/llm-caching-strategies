package com.tzezula.llmcache.service

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BatchingServiceTest {
    private lateinit var llm: CoalescingCacheService
    private lateinit var service: BatchingService

    @BeforeEach
    fun setUp() {
        llm = mockk<CoalescingCacheService>()
        service = BatchingService(llm)
    }

    @Test
    fun `batch multiple requests in the same window`() = runTest {
        val prompts =  List(5) { "Request $it" }
        coEvery { llm.summarizeBatch(any()) } coAnswers {
            firstArg<List<String>>().map { "Summary for: $it" }
        }

        coroutineScope {
            // Fire off multiple requests in quick succession
            prompts.forEach { prompt ->
                launch {
                    service.summarize(prompt)
                }
            }
        }
        // Verify that the LLM was called only once with a batch of requests
        coVerify(exactly = 1) { llm.summarizeBatch(prompts) }
    }
}
