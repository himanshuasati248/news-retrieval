package com.news.retrieval.controller;

import com.news.retrieval.dto.ApiResponse;
import com.news.retrieval.service.DataIngestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("/api/v1/ingest")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Data Ingestion", description = "API for ingesting news data from configured sources")
public class DataIngestController {

    private final DataIngestService dataIngestService;

    @PostMapping
    @Operation(
            summary = "Ingest news data",
            description = "Ingests news articles from the configured data source."
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> ingest() {
        log.info("Ingest endpoint called – reading from configuration");

        int articlesIngested = dataIngestService.ingest();

        Map<String, Object> result = Map.of("articles_ingested", articlesIngested);

        return ResponseEntity.ok(
                ApiResponse.success(result, articlesIngested,
                        "Successfully ingested " + articlesIngested + " articles."));
    }
}
