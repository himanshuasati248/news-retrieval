package com.news.retrieval.strategy.impl;

import com.news.retrieval.config.NewsProperties;
import com.news.retrieval.dto.QueryResponse;
import com.news.retrieval.model.NewsArticle;
import com.news.retrieval.repository.NewsArticleRepository;
import com.news.retrieval.strategy.IntentFetchStrategy;
import com.news.retrieval.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class NearbyFetchStrategy implements IntentFetchStrategy {

    private final NewsProperties newsProperties;
    private final NewsArticleRepository articleRepository;

    @Override
    public String getIntent() {
        return "nearby";
    }

    @Override
    public boolean supports(QueryResponse analysis) {
        return analysis.getLatitude() != null && analysis.getLongitude() != null;
    }

    @Override
    public List<NewsArticle> fetch(QueryResponse analysis, String query, int limit) {
        double lat = analysis.getLatitude();
        double lon = analysis.getLongitude();
        double radius = newsProperties.getRadiusKm();

        log.info("Fetching articles near ({}, {}) within {}km", lat, lon, radius);

        double latDelta = GeoUtils.latDeltaForRadius(radius);
        double lonDelta = GeoUtils.lonDeltaForRadius(lat, radius);

        var article = articleRepository.findWithinBoundingBox(
                lat - latDelta, lat + latDelta,
                lon - lonDelta, lon + lonDelta,
                PageRequest.of(0, limit));
        log.info("Nearby Article {}", article);
        return article;
    }

    @Override
    public List<NewsArticle> rank(List<NewsArticle> articles, QueryResponse queryResponse, String originalQuery) {
        double lat = queryResponse.getLatitude() != null ? queryResponse.getLatitude() : 0;
        double lon = queryResponse.getLongitude() != null ? queryResponse.getLongitude() : 0;

        return articles.stream()
                .sorted(Comparator.comparingDouble(a ->
                        GeoUtils.haversineDistance(lat, lon, a.getLatitude(), a.getLongitude())))
                .collect(Collectors.toList());
    }
}
