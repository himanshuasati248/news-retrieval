package com.news.retrieval.ingest;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.InputStream;

@Slf4j
public class S3DataSourceReader implements DataSourceReader {

    private final S3Client s3Client;
    private final String bucketName;
    private final String key;

    public S3DataSourceReader(S3Client s3Client, String bucketName, String key) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.key = key;
    }

    @Override
    public InputStream read() {
        log.info("Reading news data from S3 – bucket: {}, key: {}", bucketName, key);

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        try {
            return s3Client.getObject(request);
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format("Failed to read from S3 bucket=%s, key=%s: %s", bucketName, key, e.getMessage()), e);
        }
    }

    @Override
    public String sourceName() {
        return "s3";
    }
}
