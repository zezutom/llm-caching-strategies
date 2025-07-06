package com.tzezula.llmcache.service

import com.tzezula.llmcache.client.LlmClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

/**
 * A cache service that coalesces requests for the same prompt.
 * In other words, if multiple requests for the same prompt are made,
 * it will only process the first request and the others will await the result.
 */
@Service
class CoalescingCacheService(
    private val llmClient: LlmClient,
) : LlmService {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val inFlight = ConcurrentHashMap<String, CompletableDeferred<String?>>()
    private val mutex = Mutex()

    override suspend fun summarize(prompt: String): String? {
        val hash = hash(prompt)

        inFlight[hash]?.let { deferred ->
            // If there's already a request in flight, wait for it to complete
            return deferred.await()
        }

        return mutex.withLock {
            // Double-check if another request was added while we were waiting for the lock
            inFlight[hash]?.let { deferred ->
                return deferred.await()
            }

            // Create a new CompletableDeferred to hold the result of the LLM call
            val deferred = CompletableDeferred<String?>()
            inFlight[hash] = deferred

            try {
                // Launch the LLM call asynchronously
                llmClient.summarize(prompt).also { summary ->
                    // Complete the deferred with the result
                    // A null value indicates no summary was generated
                    deferred.complete(summary)
                }
            } catch (e: Exception) {
                deferred.completeExceptionally(e)
                // Log the error and return null
                logger.error("Error summarizing prompt: $prompt", e)
                null
            } finally {
                // Clean up the in-flight map
                inFlight.remove(hash)
            }
        }
    }
}
