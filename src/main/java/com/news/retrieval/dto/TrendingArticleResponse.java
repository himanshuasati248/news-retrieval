package com.news.retrieval.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrendingArticleResponse {

    private String title;
    private String description;
    private String url;
    @JsonProperty("publication_date")
    private LocalDateTime publicationDate;
    @JsonProperty("source_name")
    private String sourceName;
    private List<String> category;
    @JsonProperty("trending_score")
    private double trendingScore;
}
