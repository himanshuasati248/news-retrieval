package com.news.retrieval.util;

import com.news.retrieval.model.NewsArticle;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class NewUtils {

    private NewUtils() {}

    public static List<NewsArticle> deduplicateArticles(List<NewsArticle> articles) {
        if (articles == null || articles.isEmpty()) {
            return new ArrayList<>();
        }
        Set<NewsArticle> uniqueSet = new LinkedHashSet<>(articles);
        return new ArrayList<>(uniqueSet);
    }
}
