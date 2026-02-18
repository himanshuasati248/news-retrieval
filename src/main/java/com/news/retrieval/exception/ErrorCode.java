package com.news.retrieval.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // 400 - Bad Request
    INVALID_QUERY("NEWS-400-001", HttpStatus.BAD_REQUEST, "Query parameter is required."),
    INVALID_CATEGORY("NEWS-400-002", HttpStatus.BAD_REQUEST, "Category parameter cannot be empty."),
    INVALID_THRESHOLD("NEWS-400-003", HttpStatus.BAD_REQUEST, "Threshold must be between 0 and 1."),
    INVALID_SEARCH_QUERY("NEWS-400-004", HttpStatus.BAD_REQUEST, "Search query cannot be empty."),
    INVALID_SOURCE("NEWS-400-005", HttpStatus.BAD_REQUEST, "Source parameter cannot be empty."),
    INVALID_LATITUDE("NEWS-400-006", HttpStatus.BAD_REQUEST, "Latitude must be between -90 and 90."),
    INVALID_LONGITUDE("NEWS-400-007", HttpStatus.BAD_REQUEST, "Longitude must be between -180 and 180."),
    MISSING_PARAMETER("NEWS-400-008", HttpStatus.BAD_REQUEST, "A required parameter is missing."),
    TYPE_MISMATCH("NEWS-400-009", HttpStatus.BAD_REQUEST, "Parameter type mismatch."),

    // 404 - Not Found
    ARTICLE_NOT_FOUND("NEWS-404-001", HttpStatus.NOT_FOUND, "The requested article was not found."),
    CATEGORY_NOT_FOUND("NEWS-404-002", HttpStatus.NOT_FOUND, "The specified category does not exist."),
    STRATEGY_NOT_FOUND("NEWS-404-003", HttpStatus.NOT_FOUND, "No strategy found for the given intent."),

    // 500 - Internal Server Error
    INGESTION_FAILED("NEWS-500-001", HttpStatus.INTERNAL_SERVER_ERROR, "Data ingestion failed."),
    LLM_SERVICE_ERROR("NEWS-500-002", HttpStatus.INTERNAL_SERVER_ERROR, "LLM service encountered an error."),
    INTERNAL_ERROR("NEWS-500-003", HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred. Please try again later."),
    LLM_RESPONSE_PARSE_ERROR("NEWS-500-004", HttpStatus.INTERNAL_SERVER_ERROR, "Failed to parse LLM response."),
    LLM_PROMPT_LOAD_ERROR("NEWS-500-005", HttpStatus.INTERNAL_SERVER_ERROR, "Failed to load prompt templates."),
    LLM_COMMUNICATION_ERROR("NEWS-500-006", HttpStatus.INTERNAL_SERVER_ERROR, "Failed to communicate with LLM service.");

    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(String code, HttpStatus httpStatus, String defaultMessage) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
