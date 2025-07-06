package com.tzezula.llmcache.service

import com.github.benmanes.caffeine.cache.Caffeine
import com.tzezula.llmcache.client.LlmClient
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

/**
 * A naive cache service that caches summaries for prompts.
 * It doesn't handle concurrent requests for the same prompt,
 * which means if multiple requests for the same prompt are made,
 * each request will invoke the LLM client independently.
 */
@Service
class NaiveCacheService(
    private val llmClient: LlmClient
) : LlmService {
    private val cache = Caffeine.newBuilder()
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build<String, String>()

    override suspend fun summarize(prompt: String): String? {
        // Use a simple hash function to create a unique key for the prompt
        val hash = hash(prompt)

        // Check if the summary is already in the cache
        cache.getIfPresent(hash)?.let {
            return it
        }
        // If not in cache, call the LLM client to get the summary
        return llmClient.summarize(prompt).also { summary ->
            cache.put(hash, summary)
        }
    }
}
