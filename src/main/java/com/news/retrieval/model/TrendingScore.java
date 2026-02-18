package com.news.retrieval.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "trending_scores", indexes = {
        @Index(name = "idx_ts_geo_cell", columnList = "geo_cell"),
        @Index(name = "idx_ts_geo_cell_score", columnList = "geo_cell, score")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_geo_cell_article", columnNames = {"geo_cell", "article_id"})
})
public class TrendingScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "geo_cell", nullable = false, length = 30)
    private String geoCell;

    @Column(name = "article_id", nullable = false, length = 64)
    private String articleId;

    @Column(nullable = false)
    private double score;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
