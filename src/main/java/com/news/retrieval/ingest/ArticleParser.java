package com.news.retrieval.ingest;

import com.fasterxml.jackson.databind.JsonNode;
import com.news.retrieval.cache.CategoryCacheService;
import com.news.retrieval.model.Category;
import com.news.retrieval.model.NewsArticle;
import com.news.retrieval.repository.CategoryRepository;
import com.news.retrieval.util.ResponseParserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleParser {

    private final CategoryRepository categoryRepository;
    private final ResponseParserUtil responseParserUtil;
    private final CategoryCacheService categoryCacheService;

    private static final List<DateTimeFormatter> FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));

    public List<NewsArticle> parse(InputStream inputStream) {
        JsonNode rootNode = responseParserUtil.parseToJsonNode(inputStream);
        List<NewsArticle> articles = new ArrayList<>();

        Set<String> allCategoryNames = new LinkedHashSet<>();
        for (JsonNode node : rootNode) {
            allCategoryNames.addAll(extractCategoryNames(node));
        }

        Map<String, Category> categoryMap = resolveCategories(allCategoryNames);
        
        categoryCacheService.mergeCategories(allCategoryNames);

        for (JsonNode node : rootNode) {
            try {
                articles.add(mapToArticle(node, categoryMap));
            } catch (Exception e) {
                log.warn("Skipping malformed article: {}", e.getMessage());
            }
        }

        return articles;
    }


    private Set<String> extractCategoryNames(JsonNode node) {
        Set<String> names = new LinkedHashSet<>();
        JsonNode catNode = node.path("category");

        if (catNode.isMissingNode() || catNode.isNull()) {
            return names;
        }

        if (catNode.isArray()) {
            for (JsonNode c : catNode) {
                addIfValid(c.asText(null), names);
            }
        } else if (catNode.isTextual()) {
            String text = catNode.asText(null);
            if (text != null) {
                for (String part : text.split(",")) {
                    addIfValid(part, names);
                }
            }
        }

        return names;
    }

    private void addIfValid(String raw, Set<String> target) {
        if (raw != null) {
            String trimmed = raw.trim();
            if (!trimmed.isBlank()) {
                target.add(trimmed.toLowerCase());
            }
        }
    }

    private Map<String, Category> resolveCategories(Set<String> categoryNames) {
        Map<String, Category> map = new HashMap<>();

        for (String name : categoryNames) {
            Category category = categoryRepository.findByNameIgnoreCase(name)
                    .orElseGet(() -> categoryRepository.save(
                            Category.builder().name(name).build()));
            map.put(name.toLowerCase(), category);
        }

        return map;
    }

    private NewsArticle mapToArticle(JsonNode node, Map<String, Category> categoryMap) {
        NewsArticle article = new NewsArticle();

        article.setId(node.path("id").asText(null));
        article.setTitle(node.path("title").asText(null));
        article.setDescription(node.path("description").asText(null));
        article.setUrl(node.path("url").asText(null));
        article.setSourceName(node.path("source_name").asText(null));
        article.setRelevanceScore(node.path("relevance_score").asDouble(0.0));
        article.setLatitude(node.path("latitude").asDouble(0.0));
        article.setLongitude(node.path("longitude").asDouble(0.0));

        String dateStr = node.path("publication_date").asText(null);
        if (dateStr != null) {
            article.setPublicationDate(parseDate(dateStr));
        }

        Set<String> names = extractCategoryNames(node);
        Set<Category> articleCategories = new HashSet<>();
        for (String name : names) {
            Category cat = categoryMap.get(name.toLowerCase());
            if (cat != null) {
                articleCategories.add(cat);
            }
        }
        if (!articleCategories.isEmpty()) {
            article.addCategories(articleCategories);
        }

        return article;
    }

    private LocalDateTime parseDate(String dateStr) {
        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                return LocalDateTime.parse(dateStr, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        log.warn("Unable to parse date: {}", dateStr);
        return null;
    }
}
