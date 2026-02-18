package com.news.retrieval.repository;

import com.news.retrieval.model.UserArticleEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserEventRepository extends JpaRepository<UserArticleEvent, Long> {

    List<UserArticleEvent> findByCreatedAtAfter(LocalDateTime since);
}
