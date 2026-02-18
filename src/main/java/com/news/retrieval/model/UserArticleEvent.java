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
@Table(name = "user_article_events", indexes = {
        @Index(name = "idx_uae_article_id", columnList = "article_id"),
        @Index(name = "idx_uae_created_at", columnList = "created_at"),
        @Index(name = "idx_uae_lat_lon", columnList = "latitude, longitude")
})
public class UserArticleEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "article_id", nullable = false)
    private String articleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Getter
    public enum EventType {
        VIEW(1.0),
        CLICK(3.0),
        SHARE(5.0);

        private final double weight;

        EventType(double weight) {
            this.weight = weight;
        }
    }

    public UserArticleEvent(String articleId, EventType eventType,
                            double latitude, double longitude, LocalDateTime createdAt) {
        this.articleId = articleId;
        this.eventType = eventType;
        this.latitude = latitude;
        this.longitude = longitude;
        this.createdAt = createdAt;
    }
}
