package com.news.retrieval.repository;

import com.news.retrieval.model.NewsArticle;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NewsArticleRepository extends JpaRepository<NewsArticle, String> {

    @Query("SELECT DISTINCT a FROM NewsArticle a JOIN FETCH a.categories c " +
            "WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :category, '%'))")
    List<NewsArticle> findByCategory(@Param("category") String category, Pageable pageable);

    @Query("SELECT DISTINCT a FROM NewsArticle a LEFT JOIN FETCH " +
            "a.categories WHERE a.relevanceScore >= :threshold")
    List<NewsArticle> findByScoreAboveThreshold(@Param("threshold") double threshold, Pageable pageable);

    @Query("SELECT DISTINCT a FROM NewsArticle a LEFT JOIN FETCH" +
            " a.categories WHERE LOWER(a.sourceName) LIKE LOWER(CONCAT('%', :source, '%'))")
    List<NewsArticle> findBySource(@Param("source") String source, Pageable pageable);

    @Query("SELECT DISTINCT a FROM NewsArticle a LEFT JOIN FETCH" +
            " a.categories WHERE LOWER(a.title) LIKE LOWER(CONCAT('%', :term, '%')) OR LOWER(a.description) LIKE LOWER(CONCAT('%', :term, '%'))")
    List<NewsArticle> searchByTerm(@Param("term") String term, Pageable pageable);


    @Query(value = "SELECT a.id FROM news_articles a WHERE " +
            "to_tsvector('english', COALESCE(a.title,'') || ' ' || COALESCE(a.description,''))" +
            " @@ to_tsquery('english', regexp_replace(trim(:query), '\\s+', ' | ', 'g')) " +
            "ORDER BY ts_rank(to_tsvector('english', COALESCE(a.title,'') || ' ' || COALESCE(a.description,'')), " +
            "to_tsquery('english', regexp_replace(trim(:query), '\\s+', ' | ', 'g'))) DESC " +
            "LIMIT :limit OFFSET :offset",
            nativeQuery = true)
    List<String> fullTextSearchIds(@Param("query") String query, 
                                   @Param("limit") int limit, 
                                   @Param("offset") int offset);

    @Query("SELECT DISTINCT a FROM NewsArticle a LEFT JOIN FETCH a.categories WHERE a.id IN :ids")
    List<NewsArticle> findAllByIdsWithCategories(@Param("ids") List<String> ids);


    @Query("SELECT DISTINCT a FROM NewsArticle a LEFT JOIN " +
            "FETCH a.categories WHERE a.latitude BETWEEN :minLat AND :maxLat AND a.longitude BETWEEN :minLon AND :maxLon")
    List<NewsArticle> findWithinBoundingBox(@Param("minLat") double minLat, @Param("maxLat") double maxLat,
                                            @Param("minLon") double minLon, @Param("maxLon") double maxLon,
                                            Pageable pageable);
}
