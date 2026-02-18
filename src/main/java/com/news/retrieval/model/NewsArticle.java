package com.news.retrieval.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@ToString
@Table(name = "news_articles", indexes = {
        @Index(name = "idx_source_name", columnList = "source_name"),
        @Index(name = "idx_relevance_score", columnList = "relevance_score"),
        @Index(name = "idx_publication_date", columnList = "publication_date"),
        @Index(name = "idx_lat_lon", columnList = "latitude, longitude")
})
public class NewsArticle {

    @Id
    @Column(length = 64)
    private String id;

    @Column(columnDefinition = "TEXT")
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 2048)
    private String url;

    @Column(name = "publication_date")
    private LocalDateTime publicationDate;

    @Column(name = "source_name")
    private String sourceName;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "news_article_categories",
            joinColumns = @JoinColumn(name = "article_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    @Builder.Default
    private Set<Category> categories = new HashSet<>();

    @Column(name = "relevance_score")
    private double relevanceScore;

    private double latitude;
    private double longitude;

    public List<String> getCategoryList() {
        if (categories == null || categories.isEmpty()) {
            return List.of();
        }
        return categories.stream()
                .map(Category::getName)
                .collect(Collectors.toList());
    }

    public void addCategories(Set<Category> categoriesToAdd) {
        if (categories == null) {
            categories = new HashSet<>();
        }
        categories.addAll(categoriesToAdd);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NewsArticle that = (NewsArticle) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
