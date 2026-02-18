package com.news.retrieval.cache;

import java.time.Duration;
import java.util.List;
import java.util.Optional;


public interface CacheService<T> {

    Optional<T> get(String key);

    Optional<List<T>> getList(String key);

    void put(String key, T value, Duration ttl);

    void evict(String key);

    boolean acquireLock(String lockKey, Duration lockTimeout);

    void releaseLock(String lockKey);

    boolean updateWithLock(String value, List<T> values, Duration ttl);
}
