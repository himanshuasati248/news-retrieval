package com.news.retrieval.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;


@Component
public class IntentStrategyResolver {

    private static final Logger log = LoggerFactory.getLogger(IntentStrategyResolver.class);

    private final Map<String, IntentFetchStrategy> strategyMap;

    public IntentStrategyResolver(List<IntentFetchStrategy> strategies) {
        this.strategyMap = strategies.stream()
                .collect(Collectors.toMap(IntentFetchStrategy::getIntent, Function.identity()));
        log.info("Registered intent fetch strategies: {}", strategyMap.keySet());
    }


    public Optional<IntentFetchStrategy> resolve(String intent) {
        return Optional.ofNullable(strategyMap.get(intent.toLowerCase()));
    }
}
