package com.news.retrieval.service;

import com.news.retrieval.config.NewsProperties;
import com.news.retrieval.exception.ErrorCode;
import com.news.retrieval.exception.NewsRetrievalException;
import com.news.retrieval.model.NewsArticle;
import com.news.retrieval.repository.NewsArticleRepository;
import com.news.retrieval.ingest.ArticleParser;
import com.news.retrieval.ingest.DataSourceReader;
import com.news.retrieval.ingest.DataSourceReaderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


@Service
@Slf4j
@RequiredArgsConstructor
public class DataIngestService {

    private final NewsArticleRepository articleRepository;
    private final NewsProperties newsProperties;
    private final ArticleParser articleParser;
    private final DataSourceReaderFactory readerFactory;


    @Transactional
    public int ingest() {
        log.info("Ingesting data from configured source: {}", newsProperties.getDataSource());
        DataSourceReader reader = readerFactory.createFromConfig();
        return ingestFrom(reader);
    }

    protected int ingestFrom(DataSourceReader reader) {
        try (InputStream inputStream = reader.read()) {
            List<NewsArticle> articles = articleParser.parse(inputStream);
            return saveBatched(articles);
        } catch (Exception e) {
            log.error("Ingestion failed for source [{}]", reader.sourceName(), e);
            throw new NewsRetrievalException(ErrorCode.INGESTION_FAILED,
                    "Ingestion failed for source [" + reader.sourceName() + "]: " + e.getMessage(), e);
        }
    }

    private int saveBatched(List<NewsArticle> articles) {
        int totalSaved = 0;
        int batchSize = newsProperties.getBatchSize();
        List<NewsArticle> batch = new ArrayList<>(newsProperties.getBatchSize());

        for (NewsArticle article : articles) {
            batch.add(article);
            if (batch.size() >= batchSize) {
                articleRepository.saveAll(batch);
                totalSaved += batch.size();
                log.debug("Saved batch of {} articles.", batch.size());
                batch.clear();
            }
        }

        if (!batch.isEmpty()) {
            articleRepository.saveAll(batch);
            totalSaved += batch.size();
        }

        log.info("Successfully ingested {} news articles.", totalSaved);
        return totalSaved;
    }

}
