package com.news.retrieval.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "prompts")
public class PromptProperties {

    private String basePath = "prompts";
    private String queryAnalysisFile = "query-analysis.txt";
    private String articleSummaryFile = "article-summary.txt";
}
