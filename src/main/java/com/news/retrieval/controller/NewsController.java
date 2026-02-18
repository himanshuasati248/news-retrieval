package com.news.retrieval.controller;

import com.news.retrieval.dto.*;
import com.news.retrieval.exception.ErrorCode;
import com.news.retrieval.exception.NewsRetrievalException;
import com.news.retrieval.service.NewsService;
import com.news.retrieval.service.TrendingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/news")
@Tag(name = "News", description = "News article retrieval and search APIs")
@Slf4j
@AllArgsConstructor
public class NewsController {

    private final NewsService newsService;


    @PostMapping("/query")
    @Operation(
            summary = "Query news articles using natural language",
            description = "Processes a natural language query and returns relevant news articles."
    )
    public ResponseEntity<ApiResponse<List<NewsArticleResponse>>> queryNews(
            @Parameter(description = "Natural language query request", required = true)
            @RequestBody NewsQueryRequest request) {

        log.info("Natural language query request: {}", request);

        if (request.getQuery() == null || request.getQuery().isBlank()) {
            throw new NewsRetrievalException(ErrorCode.INVALID_QUERY);
        }

        List<NewsArticleResponse> articles = newsService.processNaturalLanguageQuery(request);

        ApiResponse<List<NewsArticleResponse>> response = ApiResponse.success(
                articles, articles.size(), request.getQuery());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/category")
    @Operation(
            summary = "Get news articles by category",
            description = "Retrieves news articles filtered by a specific category (e.g., 'technology', 'sports', 'politics')"
    )
    public ResponseEntity<ApiResponse<List<NewsArticleResponse>>> getByCategory(
            @Parameter(description = "Category name (e.g., 'technology', 'sports')", required = true)
            @RequestParam String category) {

        log.info("Category endpoint: category={}", category);

        if (category.isBlank()) {
            throw new NewsRetrievalException(ErrorCode.INVALID_CATEGORY);
        }

        List<NewsArticleResponse> enriched = newsService.fetchByCategory(category);

        ApiResponse<List<NewsArticleResponse>> response = ApiResponse.success(
                enriched, enriched.size(), "Articles retrieved for category: " + category);

        return ResponseEntity.ok(response);
    }


    @GetMapping("/score")
    @Operation(
            summary = "Get news articles by relevance score",
            description = "Retrieves news articles with a relevance score greater than or equal to the specified threshold"
    )
    public ResponseEntity<ApiResponse<List<NewsArticleResponse>>> getByScore(
            @Parameter(description = "Relevance score threshold (0.0 to 1.0)", example = "0.7")
            @RequestParam(defaultValue = "0.7") double threshold) {

        log.info("Score endpoint: threshold={}", threshold);

        if (threshold < 0 || threshold > 1) {
            throw new NewsRetrievalException(ErrorCode.INVALID_THRESHOLD);
        }

        List<NewsArticleResponse> enriched = newsService.fetchByScore(threshold);

        ApiResponse<List<NewsArticleResponse>> response = ApiResponse.success(
                enriched, enriched.size(), "Articles with relevance score >= " + threshold);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    @Operation(
            summary = "Search news articles by keyword",
            description = "Searches news articles using a keyword query string"
    )
    public ResponseEntity<ApiResponse<List<NewsArticleResponse>>> search(
            @Parameter(description = "Search query string", required = true)
            @RequestParam String query) {

        log.info("Search endpoint: query={}", query);

        if (query.isBlank()) {
            throw new NewsRetrievalException(ErrorCode.INVALID_SEARCH_QUERY);
        }

        List<NewsArticleResponse> enriched = newsService.fetchBySearch(query);

        ApiResponse<List<NewsArticleResponse>> response = ApiResponse.success(
                enriched, enriched.size(), "Search results for: " + query);

        return ResponseEntity.ok(response);
    }


    @GetMapping("/source")
    @Operation(
            summary = "Get news articles by source",
            description = "Retrieves news articles from a specific news source"
    )
    public ResponseEntity<ApiResponse<List<NewsArticleResponse>>> getBySource(
            @Parameter(description = "News source name", required = true)
            @RequestParam String source) {

        log.info("Source endpoint: source={}", source);

        if (source.isBlank()) {
            throw new NewsRetrievalException(ErrorCode.INVALID_SOURCE);
        }

        List<NewsArticleResponse> enriched = newsService.fetchBySource(source);

        ApiResponse<List<NewsArticleResponse>> response = ApiResponse.success(
                enriched, enriched.size(), "Articles from source: " + source);

        return ResponseEntity.ok(response);
    }


    @GetMapping("/nearby")
    @Operation(
            summary = "Get news articles near a location",
            description = "Retrieves news articles within a specified radius of a geographic location"
    )
    public ResponseEntity<ApiResponse<List<NewsArticleResponse>>> getNearby(
            @Parameter(description = "Latitude (-90 to 90)", required = true, example = "40.7128")
            @RequestParam double lat,
            @Parameter(description = "Longitude (-180 to 180)", required = true, example = "-74.0060")
            @RequestParam double lon) {

        log.info("Nearby endpoint: lat={}, lon={}", lat, lon);

        validateCoordinates(lat, lon);

        List<NewsArticleResponse> enriched = newsService.fetchNearby(lat, lon);

        ApiResponse<List<NewsArticleResponse>> response = ApiResponse.success(
                enriched, enriched.size(),
                String.format("Articles within of (%.4f, %.4f)", lat, lon));

        return ResponseEntity.ok(response);
    }


    private void validateCoordinates(double lat, double lon) {
        if (lat < -90 || lat > 90) {
            throw new NewsRetrievalException(ErrorCode.INVALID_LATITUDE);
        }
        if (lon < -180 || lon > 180) {
            throw new NewsRetrievalException(ErrorCode.INVALID_LONGITUDE);
        }
    }
}
