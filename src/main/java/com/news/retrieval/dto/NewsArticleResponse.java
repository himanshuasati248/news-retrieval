package com.news.retrieval.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.news.retrieval.model.NewsArticle;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NewsArticleResponse{

    private String title;
    private String description;
    private String url;
    private LocalDateTime publicationDate;
    private String sourceName;
    private List<String> category;
    private double relevanceScore;
    private String llmSummary;
    private double latitude;
    private double longitude;


    public static NewsArticleResponse fromEntity(NewsArticle article) {
        return NewsArticleResponse.builder()
                .title(article.getTitle())
                .description(article.getDescription())
                .url(article.getUrl())
                .publicationDate(article.getPublicationDate())
                .sourceName(article.getSourceName())
                .category(article.getCategoryList())
                .relevanceScore(article.getRelevanceScore())
                .latitude(article.getLatitude())
                .longitude(article.getLongitude())
                .build();
    }
}
