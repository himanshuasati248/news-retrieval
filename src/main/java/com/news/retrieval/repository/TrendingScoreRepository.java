package com.news.retrieval.repository;

import com.news.retrieval.model.TrendingScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrendingScoreRepository extends JpaRepository<TrendingScore, Long> {

    @Query("""
        SELECT ts FROM TrendingScore ts
        WHERE ts.geoCell = :geoCell
        ORDER BY ts.score DESC
        LIMIT :limit
    """)
    List<TrendingScore> findTopByGeoCell(@Param("geoCell") String geoCell,
                                         @Param("limit") int limit);

    @Modifying
    @Query(value = """
        INSERT INTO trending_scores (geo_cell, article_id, score, updated_at)
        VALUES (:geoCell, :articleId, :score, NOW())
        ON CONFLICT ON CONSTRAINT uk_geo_cell_article
        DO UPDATE SET score = :score, updated_at = NOW()
    """, nativeQuery = true)
    void upsertScore(@Param("geoCell") String geoCell,
                     @Param("articleId") String articleId,
                     @Param("score") double score);

}
