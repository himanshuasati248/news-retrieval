package com.news.retrieval.strategy;

import com.news.retrieval.dto.QueryResponse;
import com.news.retrieval.model.NewsArticle;

import java.util.List;

public interface IntentFetchStrategy {

    String getIntent();

    boolean supports(QueryResponse analysis);

    List<NewsArticle> fetch(QueryResponse analysis, String query, int limit);

    List<NewsArticle> rank(List<NewsArticle> articles, QueryResponse queryResponse, String originalQuery);
}
