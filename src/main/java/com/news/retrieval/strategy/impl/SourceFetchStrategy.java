package com.news.retrieval.strategy.impl;

import com.news.retrieval.dto.QueryResponse;
import com.news.retrieval.model.NewsArticle;
import com.news.retrieval.repository.NewsArticleRepository;
import com.news.retrieval.strategy.IntentFetchStrategy;
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
public class SourceFetchStrategy implements IntentFetchStrategy {

    private final NewsArticleRepository articleRepository;

    @Override
    public String getIntent() {
        return "source";
    }

    @Override
    public boolean supports(QueryResponse analysis) {
        return analysis.getSource() != null;
    }

    @Override
    public List<NewsArticle> fetch(QueryResponse analysis, String query, int limit) {
        log.debug("Fetching articles from source: {}", analysis.getSource());
        var articles = articleRepository.findBySource(analysis.getSource(), PageRequest.of(0, limit));
        log.info("Articles from source: {}", articles);
        return articles;
    }

    @Override
    public List<NewsArticle> rank(List<NewsArticle> articles, QueryResponse queryResponse, String originalQuery) {
        return articles.stream()
                .sorted(Comparator.comparing(NewsArticle::getPublicationDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }
}
