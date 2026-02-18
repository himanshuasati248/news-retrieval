package com.news.retrieval.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private int totalResults;
    private String query;
    private String intent;
    private List<String> entities;
    private LocalDateTime timestamp = LocalDateTime.now();
    private T articles;
    private Integer page;

    public static <T> ApiResponse<T> success(T data, int totalResults, String query) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setMessage("Query processed successfully.");
        response.setTotalResults(totalResults);
        response.setArticles(data);
        response.setQuery(query);
        return response;
    }

    public static <T> ApiResponse<T> error(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setMessage(message);
        response.setTotalResults(0);
        return response;
    }
}
