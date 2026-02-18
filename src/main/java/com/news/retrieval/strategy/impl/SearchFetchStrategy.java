package com.news.retrieval.strategy.impl;

import com.news.retrieval.dto.QueryResponse;
import com.news.retrieval.model.NewsArticle;
import com.news.retrieval.repository.NewsArticleRepository;
import com.news.retrieval.strategy.IntentFetchStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class SearchFetchStrategy implements IntentFetchStrategy {

    private final NewsArticleRepository articleRepository;

    @Override
    public String getIntent() {
        return "search";
    }

    @Override
    public boolean supports(QueryResponse analysis) {
        return true;
    }

    @Override
    public List<NewsArticle> fetch(QueryResponse analysis, String query, int limit) {
        String searchQuery = analysis.getSearchQuery() != null ? analysis.getSearchQuery() : query;
        log.debug("Searching articles for: {}", searchQuery);

        ArrayList<NewsArticle> newsArticles = new ArrayList<>();
        try {
            List<String> ids = articleRepository.fullTextSearchIds(searchQuery, limit, 0);
            if (!ids.isEmpty()) {
                newsArticles.addAll(articleRepository.findAllByIdsWithCategories(ids));
                log.info("Articles found Based on query : {}", newsArticles);
                return newsArticles;
            }
        } catch (Exception e) {
            log.warn("Full-text search failed, falling back to LIKE search: {}", e.getMessage());
        }
         newsArticles.addAll(fallbackLikeSearch(searchQuery, limit));
         log.info("Articles found Based on query but based on fallback : {}", newsArticles);
         return newsArticles;
    }

    @Override
    public List<NewsArticle> rank(List<NewsArticle> articles, QueryResponse queryResponse, String originalQuery) {
        String queryLower = originalQuery.toLowerCase();
        return articles.stream()
                .sorted(Comparator.comparingDouble((NewsArticle a) -> {
                    double textScore = computeTextMatchScore(a, queryLower);
                    double relevance = a.getRelevanceScore();
                    return -(textScore * 0.6 + relevance * 0.4);
                }))
                .collect(Collectors.toList());
    }

    private List<NewsArticle> fallbackLikeSearch(String searchQuery, int limit) {
        String[] terms = searchQuery.split("\\s+");
        Set<NewsArticle> combined = new LinkedHashSet<>();
        Pageable pageable = PageRequest.of(0, limit);
        for (String term : terms) {
            if (term.length() < 2) continue;
            combined.addAll(articleRepository.searchByTerm(term, pageable));
        }
        return combined.stream().limit(limit).collect(Collectors.toList());
    }

    private double computeTextMatchScore(NewsArticle article, String queryLower) {
        double score = 0.0;
        String[] queryTerms = queryLower.split("\\s+");
        String titleLower = article.getTitle() != null ? article.getTitle().toLowerCase() : "";
        String descLower = article.getDescription() != null ? article.getDescription().toLowerCase() : "";

        for (String term : queryTerms) {
            if (term.length() < 2) continue;
            if (titleLower.contains(term)) score += 3.0;
            if (descLower.contains(term)) score += 1.0;
        }

        if (titleLower.contains(queryLower)) score += 5.0;
        if (descLower.contains(queryLower)) score += 2.0;

        return score / Math.max(queryTerms.length, 1);
    }
}
