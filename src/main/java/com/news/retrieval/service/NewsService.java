package com.news.retrieval.service;

import com.news.retrieval.config.NewsProperties;
import com.news.retrieval.dto.NewsArticleResponse;
import com.news.retrieval.dto.NewsQueryRequest;
import com.news.retrieval.dto.QueryResponse;
import com.news.retrieval.exception.ErrorCode;
import com.news.retrieval.exception.NewsRetrievalException;
import com.news.retrieval.model.NewsArticle;
import com.news.retrieval.strategy.IntentFetchStrategy;
import com.news.retrieval.strategy.IntentStrategyResolver;
import com.news.retrieval.util.NewUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PreDestroy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsService {

    private static final int LLM_THREAD_POOL_SIZE = 5;
    private final ExecutorService llmExecutor = Executors.newFixedThreadPool(LLM_THREAD_POOL_SIZE);

    private final LlmService llmService;
    private final IntentStrategyResolver intentStrategyResolver;
    private final NewsProperties newsProperties;

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down LLM executor service...");
        llmExecutor.shutdown();
        try {
            if (!llmExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                llmExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            llmExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Transactional(readOnly = true)
    public List<NewsArticleResponse> processNaturalLanguageQuery(NewsQueryRequest request) {
        String query = request.getQuery();
        int limit = newsProperties.getFetchRecordLimit();

        QueryResponse queryResponse = llmService.analyzeQuery(query);

        log.info("LLM response: {}", queryResponse);

        queryResponse.setLongitude(request.getLongitude());
        queryResponse.setLatitude(request.getLatitude());

        List<NewsArticle> collected = new ArrayList<>();
        List<String> intents = queryResponse.getIntents();

        if (intents == null || intents.isEmpty()) {
            intents = List.of("search");
        }

        IntentFetchStrategy primaryStrategy = null;
        for (String intent : intents) {
            var strategyOpt = intentStrategyResolver.resolve(intent);
            if (strategyOpt.isPresent()) {
                IntentFetchStrategy strategy = strategyOpt.get();
                if (strategy.supports(queryResponse)) {
                    collected.addAll(strategy.fetch(queryResponse, query, limit));
                    if (primaryStrategy == null) {
                        primaryStrategy = strategy;
                    }
                }
            } else {
                log.warn("No strategy found for intent: {}", intent);
            }
        }

        List<NewsArticle> articles = NewUtils.deduplicateArticles(collected);

        if (primaryStrategy != null) {
            articles = primaryStrategy.rank(articles, queryResponse, query);
        }

        List<NewsArticle> topArticles = articles.stream().limit(limit).collect(Collectors.toList());
        return enrichWithSummariesParallel(topArticles);
    }

    @Transactional(readOnly = true)
    public List<NewsArticleResponse> fetchByCategory(String category) {
        QueryResponse queryResponse = new QueryResponse();
        queryResponse.setCategory(category);
        List<NewsArticle> articles = fetchAndRankByIntent("category", queryResponse, category);
        return enrichWithSummariesParallel(articles);
    }

    @Transactional(readOnly = true)
    public List<NewsArticleResponse> fetchByScore(double threshold) {
        QueryResponse queryResponse = new QueryResponse();
        List<NewsArticle> articles = fetchAndRankByIntent("score", queryResponse, String.valueOf(threshold));
        return enrichWithSummariesParallel(articles);
    }

    @Transactional(readOnly = true)
    public List<NewsArticleResponse> fetchBySearch(String query) {
        QueryResponse queryResponse = new QueryResponse();
        queryResponse.setSearchQuery(query);
        List<NewsArticle> articles = fetchAndRankByIntent("search", queryResponse, query);
        return enrichWithSummariesParallel(articles);
    }

    @Transactional(readOnly = true)
    public List<NewsArticleResponse> fetchBySource(String source) {
        QueryResponse queryResponse = new QueryResponse();
        queryResponse.setSource(source);
        List<NewsArticle> articles = fetchAndRankByIntent("source", queryResponse, source);
        return enrichWithSummariesParallel(articles);
    }

    @Transactional(readOnly = true)
    public List<NewsArticleResponse> fetchNearby(double lat, double lon) {
        QueryResponse queryResponse = new QueryResponse();
        queryResponse.setLatitude(lat);
        queryResponse.setLongitude(lon);
        List<NewsArticle> articles = fetchAndRankByIntent("nearby", queryResponse, "");
        return enrichWithSummariesParallel(articles);
    }

    private List<NewsArticle> fetchAndRankByIntent(String intent, QueryResponse queryResponse, String query) {
        IntentFetchStrategy strategy = intentStrategyResolver.resolve(intent)
                .orElseThrow(() -> new NewsRetrievalException(ErrorCode.STRATEGY_NOT_FOUND,
                        "No strategy found for intent: " + intent));

        int limit = newsProperties.getFetchRecordLimit();
        List<NewsArticle> articles = strategy.fetch(queryResponse, query, limit);
        return strategy.rank(articles, queryResponse, query);
    }

    private List<NewsArticleResponse> enrichWithSummariesParallel(List<NewsArticle> articles) {
        if (articles == null || articles.isEmpty()) {
            return new ArrayList<>();
        }

        List<CompletableFuture<NewsArticleResponse>> futures = articles.stream()
                .map(article -> CompletableFuture.supplyAsync(() -> {
                    NewsArticleResponse response = NewsArticleResponse.fromEntity(article);
                    try {
                        String summary = llmService.generateSummary(article.getTitle(), article.getDescription());
                        response.setLlmSummary(summary);
                    } catch (Exception e) {
                        log.warn("Failed to generate summary for article: {}", article.getId(), e);
                        response.setLlmSummary(null);
                    }
                    return response;
                }, llmExecutor))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }
}
