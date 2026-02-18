package com.news.retrieval.strategy.impl;

import com.news.retrieval.config.NewsProperties;
import com.news.retrieval.dto.QueryResponse;
import com.news.retrieval.model.NewsArticle;
import com.news.retrieval.repository.NewsArticleRepository;
import com.news.retrieval.strategy.IntentFetchStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScoreFetchStrategy implements IntentFetchStrategy {

    private final NewsProperties newsProperties;
    private final NewsArticleRepository articleRepository;

    @Override
    public String getIntent() {
        return "score";
    }

    @Override
    public boolean supports(QueryResponse analysis) {
        return true;
    }

    @Override
    public List<NewsArticle> fetch(QueryResponse analysis, String query, int limit) {
        double threshold = newsProperties.getThresholdValue();
        if (StringUtils.hasText(query)) {
            threshold = Double.parseDouble(query);
        }
        log.info("Fetching articles with score >= {}", threshold);
        var article = articleRepository.findByScoreAboveThreshold(threshold, PageRequest.of(0, limit));
        log.info("Article found Based on score strategy: {}", article);
        return article;
    }

    @Override
    public List<NewsArticle> rank(List<NewsArticle> articles, QueryResponse queryResponse, String originalQuery) {
        return articles.stream()
                .sorted(Comparator.comparingDouble(NewsArticle::getRelevanceScore).reversed())
                .collect(Collectors.toList());
    }
}
