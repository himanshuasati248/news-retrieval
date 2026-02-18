package com.news.retrieval.service;

import com.news.retrieval.cache.CategoryCacheService;
import com.news.retrieval.config.PromptProperties;
import com.news.retrieval.dto.QueryResponse;
import com.news.retrieval.exception.ErrorCode;
import com.news.retrieval.exception.NewsRetrievalException;
import com.news.retrieval.externalcall.OpenAiClient;
import com.news.retrieval.util.ResponseParserUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
@Slf4j
public class LlmService {

    private static final String CATEGORIES_PLACEHOLDER = "{{AVAILABLE_CATEGORIES}}";

    private final OpenAiClient openAiClient;
    private final ResponseParserUtil responseParserUtil;
    private final PromptProperties promptProperties;
    private final CategoryCacheService categoryCacheService;

    private String queryAnalysisPromptTemplate;
    private String articleSummaryPrompt;

    @PostConstruct
    public void loadPrompts() {
        try {
            Path basePath = Path.of(promptProperties.getBasePath());

            Path queryAnalysisPath = basePath.resolve(promptProperties.getQueryAnalysisFile());
            Path articleSummaryPath = basePath.resolve(promptProperties.getArticleSummaryFile());

            this.queryAnalysisPromptTemplate = Files.readString(queryAnalysisPath);
            this.articleSummaryPrompt = Files.readString(articleSummaryPath);

            log.info("Loaded prompt templates from '{}': query-analysis ({} chars), article-summary ({} chars)",
                    basePath, queryAnalysisPromptTemplate.length(), articleSummaryPrompt.length());
        } catch (IOException e) {
            log.error("Failed to load prompt templates", e);
            throw new NewsRetrievalException(ErrorCode.LLM_PROMPT_LOAD_ERROR,
                    "Failed to load prompt templates: " + e.getMessage(), e);
        }
    }
    
    public QueryResponse analyzeQuery(String query) {
        try {
            String prompt = buildQueryAnalysisPrompt();
            String responseBody = openAiClient.chatCompletion(prompt, query);
            String content = responseParserUtil.extractContent(responseBody);
            content = content.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();

            QueryResponse analysis = responseParserUtil.parseJson(content, QueryResponse.class);

            log.info("LLM Query Analysis: {}", analysis);
            return analysis;
        } catch (NewsRetrievalException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to analyze query via LLM: {}", query, e);
            throw new NewsRetrievalException(ErrorCode.LLM_SERVICE_ERROR,
                    "Failed to analyze query: " + e.getMessage(), e);
        }
    }

    public String generateSummary(String title, String description) {
        try {
            String userMessage = String.format("Title: %s\nDescription: %s", title,
                    description != null ? description : "No description available");

            String responseBody = openAiClient.chatCompletion(articleSummaryPrompt, userMessage);
            return responseParserUtil.extractContent(responseBody);
        } catch (NewsRetrievalException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to generate summary for article: {}", title, e);
            throw new NewsRetrievalException(ErrorCode.LLM_SERVICE_ERROR,
                    "Failed to generate summary for article: " + title, e);
        }
    }

    private String buildQueryAnalysisPrompt() {
        String categories = categoryCacheService.getCategoriesAsCommaSeparated();
        return queryAnalysisPromptTemplate.replace(CATEGORIES_PLACEHOLDER, categories);
    }

}
