package com.news.retrieval.service;

import com.news.retrieval.cache.CacheService;
import com.news.retrieval.config.NewsProperties;
import com.news.retrieval.dto.TrendingArticleResponse;
import com.news.retrieval.model.NewsArticle;
import com.news.retrieval.model.TrendingScore;
import com.news.retrieval.model.UserArticleEvent;
import com.news.retrieval.repository.NewsArticleRepository;
import com.news.retrieval.repository.TrendingScoreRepository;
import com.news.retrieval.repository.UserEventRepository;
import com.news.retrieval.util.GeoUtils;
import com.news.retrieval.util.ResponseParserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrendingService {

    private final UserEventRepository userEventRepository;
    private final NewsArticleRepository articleRepository;
    private final TrendingScoreRepository trendingScoreRepository;
    private final NewsProperties newsProperties;
    private final CacheService<Object> cacheService;
    private final ResponseParserUtil responseParserUtil;

    private final static int SIMULATE_LAST_24_HOURS = 24;


    @Transactional
    public int simulateUserEvents(int count) {
        log.info("Simulating {} user events...", count);
        
        List<NewsArticle> allArticles = articleRepository.findAll();
        if (allArticles.isEmpty()) {
            log.warn("No articles found to simulate events for.");
            return 0;
        }

        Random random = new Random(42);
        UserArticleEvent.EventType[] eventTypes = UserArticleEvent.EventType.values();
        List<UserArticleEvent> events = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            NewsArticle article = allArticles.get(random.nextInt(allArticles.size()));
            UserArticleEvent.EventType eventType = eventTypes[random.nextInt(eventTypes.length)];
            LocalDateTime createdAt = LocalDateTime.now().minusHours(random.nextInt(SIMULATE_LAST_24_HOURS));
            
            double simulationRadiusKm = newsProperties.getRadiusKm();
            double latOffset = (random.nextDouble() - 0.5) * 2.0 * GeoUtils.latDeltaForRadius(simulationRadiusKm);
            double lonOffset = (random.nextDouble() - 0.5) * 2.0 * GeoUtils.lonDeltaForRadius(article.getLatitude(), simulationRadiusKm);
            double eventLat = article.getLatitude() + latOffset;
            double eventLon = article.getLongitude() + lonOffset;

            events.add(new UserArticleEvent(article.getId(), eventType, eventLat, eventLon, createdAt));
        }

        userEventRepository.saveAll(events);
        log.info("Simulated and saved {} user events.", events.size());
        return events.size();
    }


    @Scheduled(fixedRateString = "${news.trending.scheduler-interval-ms}")
    @Transactional
    public void computeTrendingScores() {
        log.info("Scheduler: Computing trending scores...");

        LocalDateTime cutoff = LocalDateTime.now().minusHours(SIMULATE_LAST_24_HOURS);
        List<UserArticleEvent> recentEvents = userEventRepository.findByCreatedAtAfter(cutoff);
        log.info("Scheduler: Found {} recent events since {}", recentEvents.size(), cutoff);

        if (recentEvents.isEmpty()) {
            log.info("Scheduler: No recent events to process.");
            return;
        }

        double radiusKm = newsProperties.getRadiusKm();
        Map<String, Map<String, Double>> geoCellScores = new HashMap<>();

        for (UserArticleEvent event : recentEvents) {
            Set<String> nearbyCells = GeoUtils.getGeoCellsWithinRadius(
                    event.getLatitude(),
                    event.getLongitude(),
                    radiusKm
            );

            double hoursSinceEvent = Duration.between(event.getCreatedAt(), LocalDateTime.now()).toHours();
            double recencyDecay = Math.exp(-hoursSinceEvent / SIMULATE_LAST_24_HOURS);
            double eventScore = event.getEventType().getWeight() * recencyDecay;

            for (String geoCell : nearbyCells) {
                geoCellScores
                        .computeIfAbsent(geoCell, k -> new HashMap<>())
                        .merge(event.getArticleId(), eventScore, Double::sum);
            }
        }

        int upsertCount = 0;
        Set<String> updatedGeoCells = new HashSet<>();

        for (Map.Entry<String, Map<String, Double>> cellEntry : geoCellScores.entrySet()) {
            String geoCell = cellEntry.getKey();
            Map<String, Double> articleScores = cellEntry.getValue();

            for (Map.Entry<String, Double> articleEntry : articleScores.entrySet()) {
                double roundedScore = BigDecimal.valueOf(articleEntry.getValue())
                        .setScale(3, RoundingMode.HALF_UP)
                        .doubleValue();
                trendingScoreRepository.upsertScore(geoCell, articleEntry.getKey(), roundedScore);
                upsertCount++;
            }
            updatedGeoCells.add(geoCell);
        }

        log.info("Scheduler: Upserted {} trending scores across {} geo-cells.", upsertCount, updatedGeoCells.size());

        int cacheUpdateCount = 0;
        long cacheTtlMinutes = newsProperties.getTrending().getCacheTtlMinutes();

        for (String geoCell : updatedGeoCells) {

            List<TrendingArticleResponse> trendingData = fetchTrendingDataFromDb(geoCell, newsProperties.getFetchRecordLimit());

            if (!trendingData.isEmpty()) {
                boolean updated = cacheService.updateWithLock(
                        geoCell,
                        (List<Object>) (List<?>) trendingData,
                        Duration.ofMinutes(cacheTtlMinutes));
                if (updated) {
                    cacheUpdateCount++;
                }
            }
        }

        log.info("Scheduler: Updated cache for {} geo-cells.", cacheUpdateCount);
    }


    public List<TrendingArticleResponse> getTrendingNearby(double lat, double lon, int limit) {
        return getTrendingNearby(lat, lon, newsProperties.getRadiusKm(), limit);
    }

    public List<TrendingArticleResponse> getTrendingNearby(double lat, double lon, double radiusKm, int limit) {
        Set<String> geoCells = GeoUtils.getGeoCellsWithinRadius(lat, lon, radiusKm);
        long cacheTtlMinutes = newsProperties.getTrending().getCacheTtlMinutes();

        Map<String, TrendingArticleResponse> aggregated = new HashMap<>();

        for (String geoCell : geoCells) {
            Optional<List<Object>> cachedOptional = cacheService.getList(geoCell);
            List<TrendingArticleResponse> cellData;

            if (cachedOptional.isPresent()) {
                log.debug("Cache HIT for geoCell={}", geoCell);
                cellData = convertCachedData(cachedOptional.get());
            } else {
                log.debug("Cache MISS for geoCell={}. Querying DB...", geoCell);
                cellData = fetchTrendingDataFromDb(geoCell, newsProperties.getFetchRecordLimit());

                if (!cellData.isEmpty()) {
                    cacheService.updateWithLock(geoCell, (List<Object>) (List<?>) cellData, Duration.ofMinutes(cacheTtlMinutes));
                    log.debug("Cached {} trending articles for geoCell={} with TTL={}min", cellData.size(), geoCell, cacheTtlMinutes);
                }
            }

            for (TrendingArticleResponse article : cellData) {
                aggregated.merge(article.getUrl(), article, (existing, newOne) ->
                        TrendingArticleResponse.builder()
                                .title(existing.getTitle())
                                .description(existing.getDescription())
                                .url(existing.getUrl())
                                .publicationDate(existing.getPublicationDate())
                                .sourceName(existing.getSourceName())
                                .category(existing.getCategory())
                                .trendingScore(existing.getTrendingScore() + newOne.getTrendingScore())
                                .build()
                );
            }
        }

        return aggregated.values().stream()
                .sorted(Comparator.comparingDouble(TrendingArticleResponse::getTrendingScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    private List<TrendingArticleResponse> convertCachedData(List<Object> cachedList) {
        return cachedList.stream()
                .map(obj -> responseParserUtil.convertValue(obj, TrendingArticleResponse.class))
                .collect(Collectors.toList());
    }

    private List<TrendingArticleResponse> fetchTrendingDataFromDb(String geoCell, int limit) {
        List<TrendingScore> scores = trendingScoreRepository.findTopByGeoCell(geoCell, limit);

        if (scores.isEmpty()) {
            return List.of();
        }

        List<String> articleIds = scores.stream()
                .map(TrendingScore::getArticleId)
                .toList();

        List<NewsArticle> articles = articleRepository.findAllByIdsWithCategories(articleIds);
        Map<String, NewsArticle> articleMap = articles.stream()
                .collect(Collectors.toMap(NewsArticle::getId, a -> a));

        List<TrendingArticleResponse> result = new ArrayList<>();
        for (TrendingScore ts : scores) {
            NewsArticle article = articleMap.get(ts.getArticleId());
            if (article == null) continue;

            TrendingArticleResponse response = TrendingArticleResponse.builder()
                    .title(article.getTitle())
                    .description(article.getDescription())
                    .url(article.getUrl())
                    .publicationDate(article.getPublicationDate())
                    .sourceName(article.getSourceName())
                    .category(article.getCategoryList())
                    .trendingScore(ts.getScore())
                    .build();

            result.add(response);
        }

        return result;
    }
}
