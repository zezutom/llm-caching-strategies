package com.tzezula.llmcache.service

interface LlmService {
    /**
     * Summarizes a single prompt using the LLM.
     *
     * @param prompt The text to summarize.
     * @return The summary of the prompt, or null if the summarization failed.
     */
    suspend fun summarize(prompt: String): String?

    /**
     * Summarizes a batch of prompts using the LLM.
     *
     * @param prompts A list of prompts to summarize.
     * @return A list of summaries returned by the LLM, corresponding to the input prompts.
     * Prompts that could not be summarized will not be included in the map.
     */
    suspend fun summarizeBatch(prompts: List<String>): List<String> {
        // Not supported by default, can be overridden by implementations
        throw NotImplementedError("Batch summarization is not supported.")
    }
}
