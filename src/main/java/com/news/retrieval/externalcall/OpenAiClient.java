package com.news.retrieval.externalcall;

import com.news.retrieval.config.OpenAiProperties;
import com.news.retrieval.exception.ErrorCode;
import com.news.retrieval.exception.NewsRetrievalException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles all external HTTP communication with the OpenAI API.
 */
@Component
@RequiredArgsConstructor
public class OpenAiClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiClient.class);

    private final OpenAiProperties openAiProperties;

    private WebClient webClient;

    @PostConstruct
    public void init() {
        this.webClient = WebClient.builder()
                .baseUrl(openAiProperties.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + openAiProperties.getApiKey())
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();
        log.info("OpenAI WebClient initialized with base URL: {}", openAiProperties.getBaseUrl());
    }


    public String chatCompletion(String systemPrompt, String userMessage) {
        try {
            Map<String, Object> requestBody = buildRequestBody(systemPrompt, userMessage);

//            return webClient.post()
//                    .uri(uriBuilder -> uriBuilder
//                            .path("/openai/deployments/{deployment}/chat/completions")
//                            .queryParam("api-version", "2024-02-15-preview")
//                            .build(openAiProperties.getModel()))
//                    .bodyValue(requestBody)
//                    .retrieve()
//                    .bodyToMono(String.class)
//                    .block();
            return webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("LLM API returned error status {}: {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new NewsRetrievalException(ErrorCode.LLM_COMMUNICATION_ERROR,
                    "LLM API error (HTTP " + e.getStatusCode().value() + "): " + e.getResponseBodyAsString(), e);
        } catch (WebClientRequestException e) {
            log.error("Failed to connect to LLM API: {}", e.getMessage(), e);
            throw new NewsRetrievalException(ErrorCode.LLM_COMMUNICATION_ERROR,
                    "Failed to connect to LLM API: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error during LLM API call: {}", e.getMessage(), e);
            throw new NewsRetrievalException(ErrorCode.LLM_COMMUNICATION_ERROR,
                    "Unexpected error during LLM API call: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> buildRequestBody(String systemPrompt, String userMessage) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("temperature", openAiProperties.getTemp());
        body.put("model", openAiProperties.getModel());
        body.put("max_tokens", 500);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userMessage));
        body.put("messages", messages);

        return body;
    }

}
