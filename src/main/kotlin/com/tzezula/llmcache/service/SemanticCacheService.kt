package com.tzezula.llmcache.service

import org.slf4j.LoggerFactory
import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.redis.RedisVectorStore
import org.springframework.stereotype.Service

/**
 * Summarizes the given prompt using the LLM client.
 * If a similar prompt exists in the semantic cache, it retrieves the summary from there.
 * Otherwise, it calls the LLM client to generate a new summary.
 */
@Service
class SemanticCacheService(
    // Handle LLM calls efficiently
    private val llm: CoalescingCacheService,
    // Semantic cache using Redis Vector Store
    private val vectorStore: RedisVectorStore,
) : LlmService {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override suspend fun summarize(prompt: String): String? {
        try {
            val req = SearchRequest.builder()
                .query(prompt)
                .topK(1) // Limit to top 1 result
                .similarityThreshold(0.85)
                .build()
            val hits = vectorStore.similaritySearch(req) ?: emptyList()
            if (hits.isNotEmpty()) {
                // If a similar prompt is found, return its summary
                return hits[0].metadata["summary"] as? String
            }
        } catch (e: Exception) {
            logger.error("Error searching in vector store: ${e.message}", e)
        }

        // If no similar prompt is found, run LLM and coalesce
        return llm.summarize(prompt)?.let { result ->
            // If LLM returns a summary, store it in the vector store
            storeSummary(prompt, result)
            result
        }
    }

    private fun storeSummary(prompt: String, summary: String) {
        try {
            vectorStore.add(
                listOf(
                    Document.builder()
                        .text(prompt)
                        .metadata(mapOf("summary" to summary))
                        .build()
                )
            )
        } catch (e: Exception) {
            logger.error("Error storing summary in vector store: ${e.message}", e)
        }
    }
}
