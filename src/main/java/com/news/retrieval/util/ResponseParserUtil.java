package com.news.retrieval.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.news.retrieval.exception.ErrorCode;
import com.news.retrieval.exception.NewsRetrievalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class ResponseParserUtil {

    private final ObjectMapper objectMapper;

    public ResponseParserUtil() {
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public String extractContent(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (JsonProcessingException e) {
            throw new NewsRetrievalException(ErrorCode.LLM_RESPONSE_PARSE_ERROR,
                    "Failed to parse OpenAI response", e);
        }
    }

    public <T> T parseJson(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new NewsRetrievalException(ErrorCode.LLM_RESPONSE_PARSE_ERROR,
                    "Failed to parse JSON into " + type.getSimpleName() + ": " + json, e);
        }
    }

    public JsonNode parseToJsonNode(InputStream inputStream) {
        try {
            return objectMapper.readTree(inputStream);
        } catch (IOException e) {
            throw new NewsRetrievalException(ErrorCode.LLM_RESPONSE_PARSE_ERROR,
                    "Failed to parse JSON from input stream", e);
        }
    }

    public <T> T convertValue(Object fromValue, Class<T> toValueType) {
        return objectMapper.convertValue(fromValue, toValueType);
    }

}
