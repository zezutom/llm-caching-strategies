package com.tzezula.llmcache.config

import io.github.ollama4j.OllamaAPI
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class LlmConfig {

    @Bean
    fun ollamaApi(): OllamaAPI {
        val ollamaApi = OllamaAPI("http://localhost:11434")
        ollamaApi.setVerbose(true) // Enable verbose logging for debugging
        ollamaApi.setRequestTimeoutSeconds(120) // Set a longer timeout for requests
        return ollamaApi
    }
}
