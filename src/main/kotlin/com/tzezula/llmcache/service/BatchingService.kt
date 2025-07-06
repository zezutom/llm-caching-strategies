package com.tzezula.llmcache.service

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class BatchingService(
    // Reusing the coalescing service to handle LLM calls in the best possible way
    private val llm: CoalescingCacheService,
) : LlmService {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val channel = Channel<Pair<String, CompletableDeferred<String?>>>(Channel.Factory.UNLIMITED)

    init {
        CoroutineScope(Dispatchers.IO).launch {
            val buffer = mutableListOf<Pair<String, CompletableDeferred<String?>>>()
            while (isActive) {
                try {
                    // Wait for the first item in the channel
                    val first = channel.receive()
                    // Add the first item to the buffer
                    buffer.add(first)
                    // Collect more items arriving in the next 50 ms
                    withTimeoutOrNull(50) {
                        while (true) buffer.add(channel.receive())
                    }
                    // Process the batch of items
                    val prompts = buffer.map { it.first }
                    val results = llm.summarizeBatch(prompts)
                    // Complete the rest with the results
                    buffer.zip(results).forEach { (pair, result) ->
                        pair.second.complete(result)
                    }
                } catch (e: CancellationException) {
                    logger.info("BatchingLlmService cancelled: ${e.message}")
                    throw e
                } catch (e: Exception) {
                    logger.error("Error in BatchingLlmService: ${e.message}", e)
                }
            }
        }
    }

    override suspend fun summarize(prompt: String): String? {
        val deferred = CompletableDeferred<String?>()
        channel.send(prompt to deferred)
        return deferred.await()
    }
}
