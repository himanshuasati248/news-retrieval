package com.news.retrieval.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;


@Service
@RequiredArgsConstructor
@Slf4j
public class RedisCacheService implements CacheService<Object> {

    private static final String CACHE_KEY_PREFIX = "trending:";
    private static final String LOCK_KEY_PREFIX = "trending:lock:";

    private static final Duration LOCK_TIMEOUT = Duration.ofSeconds(2);


    private final RedisTemplate<String, Object> redisTemplate;
    private final ThreadLocal<String> lockValueHolder = new ThreadLocal<>();

    @Override
    public Optional<Object> get(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            return Optional.ofNullable(value);
        } catch (Exception e) {
            log.error("Error retrieving cache key={}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<List<Object>> getList(String key) {
        String cahceKey = CACHE_KEY_PREFIX + key;
        try {
            Object value = redisTemplate.opsForValue().get(cahceKey);
            if (value instanceof List<?> list && !list.isEmpty()) {
                return Optional.of((List<Object>) list);
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error retrieving cache list key={}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void put(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
            log.debug("Cached value for key={} with TTL={}min", key, ttl.toMinutes());
        } catch (Exception e) {
            log.error("Error caching key={}: {}", key, e.getMessage());
        }
    }


    @Override
    public void evict(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.error("Error evicting cache key={}: {}", key, e.getMessage());
        }
    }

    @Override
    public boolean acquireLock(String lockKey, Duration lockTimeout) {
        try {
            String fullLockKey = LOCK_KEY_PREFIX + lockKey;
            String lockValue = UUID.randomUUID().toString();

            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(fullLockKey, lockValue, lockTimeout);

            if (Boolean.TRUE.equals(acquired)) {
                lockValueHolder.set(lockValue);
                log.debug("Acquired lock for key={}", lockKey);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("Error acquiring lock for key={}: {}", lockKey, e.getMessage());
            return false;
        }
    }

    @Override
    public void releaseLock(String lockKey) {
        try {
            String fullLockKey = LOCK_KEY_PREFIX + lockKey;
            String expectedValue = lockValueHolder.get();

            if (expectedValue != null) {
                Object currentValue = redisTemplate.opsForValue().get(fullLockKey);
                if (expectedValue.equals(currentValue)) {
                    redisTemplate.delete(fullLockKey);
                    log.debug("Released lock for key={}", lockKey);
                }
                lockValueHolder.remove();
            }
        } catch (Exception e) {
            log.error("Error releasing lock for key={}: {}", lockKey, e.getMessage());
        }
    }


    @Override
    public boolean updateWithLock(String value, List<Object> values, Duration ttl) {
        String cacheKey = CACHE_KEY_PREFIX + value;

        try {
            if (acquireLock(value, LOCK_TIMEOUT)) {
                try {
                    redisTemplate.opsForValue().set(cacheKey, values, ttl);
                    log.debug("Updated cache key={} with lock, {} items, TTL={}min", cacheKey, values.size(), ttl.toMinutes());
                    return true;
                } finally {
                    releaseLock(value);
                }
            }
            log.warn("Could not acquire lock for cache update, key={}", cacheKey);
            return false;
        } catch (Exception e) {
            log.error("Error updating cache with lock, key={}: {}", cacheKey, e.getMessage());
            return false;
        }
    }
}
