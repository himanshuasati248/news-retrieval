package com.news.retrieval.controller;

import com.news.retrieval.dto.ApiResponse;
import com.news.retrieval.dto.TrendingArticleResponse;
import com.news.retrieval.exception.ErrorCode;
import com.news.retrieval.exception.NewsRetrievalException;
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
@Tag(name = "Trending", description = "News article retrieval and search APIs")
@Slf4j
@AllArgsConstructor
public class TrendingController {

    private final TrendingService trendingService;

    @GetMapping("/trending")
    @Operation(
            summary = "Get trending news articles near a location",
            description = "Returns top trending articles within a geo-cell derived from the user's location. " +
                    "Uses precomputed trending scores and Redis caching per geo-cell."
    )
    public ResponseEntity<ApiResponse<List<TrendingArticleResponse>>> getTrending(
            @Parameter(description = "Latitude (-90 to 90)", required = true, example = "40.7128")
            @RequestParam double lat,
            @Parameter(description = "Longitude (-180 to 180)", required = true, example = "-74.0060")
            @RequestParam double lon,
            @Parameter(description = "Maximum number of articles to return (default: 10, max: 50)", example = "10")
            @RequestParam(defaultValue = "10") int limit) {

        log.info("Trending endpoint: lat={}, lon={}, limit={}", lat, lon, limit);

        validateCoordinates(lat, lon);

        List<TrendingArticleResponse> trending = trendingService.getTrendingNearby(lat, lon, limit);

        ApiResponse<List<TrendingArticleResponse>> response = ApiResponse.success(
                trending, trending.size(),
                String.format("Trending articles near (%.4f, %.4f)", lat, lon));

        return ResponseEntity.ok(response);
    }


    @PostMapping("/trending/simulate")
    @Operation(
            summary = "Simulate user interaction events",
            description = "Generates simulated user events (view, click, share) tied to existing articles " +
                    "for seeding the trending system. Use this to populate user_article_events before " +
                    "the scheduler computes trending scores."
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> simulateEvents(
            @Parameter(description = "Number of events to simulate (default: 5000)", example = "5000")
            @RequestParam(defaultValue = "100") int count) {

        log.info("Simulate events endpoint: count={}", count);

        int simulated = trendingService.simulateUserEvents(count);

        Map<String, Object> result = Map.of("events_simulated", simulated);

        return ResponseEntity.ok(
                ApiResponse.success(result, simulated,
                        "Successfully simulated " + simulated + " user events."));
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
