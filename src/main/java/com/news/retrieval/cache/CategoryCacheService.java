package com.news.retrieval.cache;

import com.news.retrieval.repository.CategoryRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryCacheService {

    private static final String CATEGORIES_CACHE_KEY = "categories:all";

    private final RedisTemplate<String, Object> redisTemplate;
    private final CategoryRepository categoryRepository;

    @PostConstruct
    public void initializeCache() {
        Set<String> categories = categoryRepository.findAllCategoryNames();
        if (!categories.isEmpty()) {
            redisTemplate.opsForSet().add(CATEGORIES_CACHE_KEY, categories.toArray());
            log.info("Initialized category cache with {} categories from database", categories.size());
        } else {
            log.info("No categories found in database during initialization");
        }
    }

    public Set<String> getAllCategories() {
        try {
            Set<Object> cached = redisTemplate.opsForSet().members(CATEGORIES_CACHE_KEY);
            if (cached != null && !cached.isEmpty()) {
                return cached.stream()
                        .map(Object::toString)
                        .collect(Collectors.toSet());
            }
            
            Set<String> dbCategories = categoryRepository.findAllCategoryNames();
            if (!dbCategories.isEmpty()) {
                redisTemplate.opsForSet().add(CATEGORIES_CACHE_KEY, dbCategories.toArray());
                log.debug("Loaded {} categories from database into cache", dbCategories.size());
            }
            return dbCategories;
        } catch (Exception e) {
            log.error("Error retrieving categories from cache, falling back to DB: {}", e.getMessage());
            return categoryRepository.findAllCategoryNames();
        }
    }

    public void mergeCategories(Collection<String> newCategories) {
        if (newCategories == null || newCategories.isEmpty()) {
            return;
        }
        
        try {
            Set<String> normalizedCategories = newCategories.stream()
                    .map(String::toLowerCase)
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.toSet());
            
            if (!normalizedCategories.isEmpty()) {
                redisTemplate.opsForSet().add(CATEGORIES_CACHE_KEY, normalizedCategories.toArray());
                log.debug("Merged {} new categories into cache", normalizedCategories.size());
            }
        } catch (Exception e) {
            log.error("Error merging categories into cache: {}", e.getMessage());
        }
    }

    public String getCategoriesAsCommaSeparated() {
        Set<String> categories = getAllCategories();
        if (categories.isEmpty()) {
            return "";
        }
        return categories.stream()
                .sorted()
                .collect(Collectors.joining(", "));
    }
}
