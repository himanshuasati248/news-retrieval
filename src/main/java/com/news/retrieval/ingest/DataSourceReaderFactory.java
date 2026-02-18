package com.news.retrieval.ingest;

import com.news.retrieval.config.NewsProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;


@Slf4j
@Component
public class DataSourceReaderFactory {

    private final NewsProperties newsProperties;
    private S3Client s3Client;

    public DataSourceReaderFactory(NewsProperties newsProperties) {
        this.newsProperties = newsProperties;
    }

    @Autowired(required = false)
    public void setS3Client(S3Client s3Client) {
        this.s3Client = s3Client;
    }


    public DataSourceReader createFromConfig() {
        String source = newsProperties.getDataSource();
        return switch (source.toLowerCase()) {
            case "s3" -> new S3DataSourceReader(
                    s3Client,
                    newsProperties.getS3().getBucketName(),
                    newsProperties.getS3().getKey());
            case "file" -> new FileDataSourceReader(newsProperties.getDataFile());
            default -> throw new IllegalArgumentException(
                    "Unsupported news.data-source: '" + source + "'. Supported values: file, s3.");
        };
    }

}
