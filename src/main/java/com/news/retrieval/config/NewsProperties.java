package com.news.retrieval.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "news")
public class NewsProperties {

    private Resource dataFile;
    private double radiusKm;
    private double thresholdValue;
    private Trending trending = new Trending();
    private S3 s3 = new S3();
    private String dataSource;
    private int batchSize;
    private int fetchRecordLimit;

    @Getter
    @Setter
    public static class Trending {
        private long schedulerIntervalMs = 600000;
        private long cacheTtlMinutes = 10;
    }

    @Getter
    @Setter
    public static class S3 {
        private String bucketName;
        private String key;
        private String region = "us-east-1";
    }
}
