package com.news.retrieval.ingest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;

import java.io.InputStream;


@Slf4j
public class FileDataSourceReader implements DataSourceReader {

    private final Resource resource;

    public FileDataSourceReader(Resource resource) {
        if (resource == null || !resource.exists()) {
            throw new IllegalArgumentException("Data file resource does not exist.");
        }
        this.resource = resource;
    }

    @Override
    public InputStream read() {
        try {
            log.info("Reading news data from file: {}", resource.getDescription());
            return resource.getInputStream();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read data file: " + e.getMessage(), e);
        }
    }

    @Override
    public String sourceName() {
        return "file";
    }
}
