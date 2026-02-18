package com.news.retrieval.repository;

import com.news.retrieval.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.Query;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    Optional<Category> findByNameIgnoreCase(String name);
    
    @Query("SELECT DISTINCT c.name FROM Category c")
    Set<String> findAllCategoryNames();
}
