package com.tzezula.llmcache.service

import io.mockk.Ordering
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.redis.RedisVectorStore

class SemanticCacheServiceTest {
    private lateinit var vectorStore: RedisVectorStore
    private lateinit var llm: CoalescingCacheService
    private lateinit var service: SemanticCacheService

    @BeforeEach
    fun setUp() {
        vectorStore = mockk<RedisVectorStore>()
        llm = mockk<CoalescingCacheService>()
        service = SemanticCacheService(llm, vectorStore)
    }

    @Test
    fun `semantic hit skips LLM`() = runTest {
        val prompt = "This is a test prompt"
        val cachedSummary = "Cached summary for: $prompt"
        val doc = Document.builder()
            .text(prompt)
            .metadata(mapOf("summary" to cachedSummary))
            .build()

        val req = SearchRequest.builder()
            .query(prompt)
            .topK(1) // Limit to top 1 result
            .similarityThreshold(0.85)
            .build()

        coEvery { vectorStore.similaritySearch(req) } returns listOf(doc)

        val result = service.summarize(prompt)
        assert(result == cachedSummary) {
            "Expected cached summary '$cachedSummary', but got '$result'"
        }

        // Verify that the vector store was queried and LLM was not called
        coVerify(exactly = 1) {
            vectorStore.similaritySearch(req)
        }
        coVerify(exactly = 0) {
            llm.summarize(prompt)
        }
    }

    @Test
    fun `cache miss calls LLM`() = runTest {
        val prompt = "New question!"
        val summary = "LLM summary for: $prompt"

        coEvery { vectorStore.similaritySearch(any<SearchRequest>()) } returns emptyList()
        coEvery { llm.summarize(prompt) } returns summary
        coEvery { vectorStore.add(any<List<Document>>()) } returns Unit

        val result = service.summarize(prompt)

        // Assert that the result matches the expected summary
        assert(result == summary) {
            "Expected LLM summary '$summary', but got '$result'"
        }
        // Verify that the vector store was queried, LLM was called, and the summary was stored
        coVerify(ordering = Ordering.ORDERED) {
            vectorStore.similaritySearch(any<SearchRequest>())
            llm.summarize(prompt)
            vectorStore.add(match { docs ->
                docs.size == 1 && docs[0].text == prompt && docs[0].metadata["summary"] == summary
            })
        }
    }
}
